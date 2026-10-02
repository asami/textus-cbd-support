package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/**
 * Admits attributable command observations and independently refreshed semantic owners.
 *
 * @since   Oct.  2, 2026
 * @version Oct.  3, 2026
 */
private[runtime] object InternalModelCmlChangePostValidation {
  import InternalModelCmlChangeApplication.ApplicationReport

  enum Disposition {
    case Incomplete, Failed, ReprojectedPendingAcceptance
  }
  enum CommandOutcome {
    case Missing(reason: String)
    case Indeterminate(reason: String)
    case Terminal(exitcode: Int, stdout: String, stderr: String)
  }
  final case class Finding(level: String, code: String, message: String, path: String, line: Int)
  final case class CommandObservation(commandreference: InternalModelRecordReference,
    applicationreference: InternalModelRecordReference, projectroot: String, targetid: String,
    projectrelativepath: String, sourceauthority: String, sourceidentity: String, sourcerevision: String,
    runtimeversion: String, argv: Vector[String], outcome: CommandOutcome)
  final case class TargetBaseline(targetid: String, snapshotreference: InternalModelArtifactReference)
  final case class OwnerEvidence(packagereference: InternalModelPackageReference, scope: InternalModelSemanticScope,
    realizationartifactreference: InternalModelArtifactReference, realizationreference: InternalModelRecordReference,
    continuityartifactreference: InternalModelArtifactReference, continuityreference: InternalModelRecordReference,
    targetbaselines: Vector[TargetBaseline], livesources: Map[InternalModelArtifactReference, InternalModelPackageFreshnessInput])
  final case class Request(application: ApplicationReport, postvalidationreference: InternalModelRecordReference,
    commands: Vector[CommandObservation], owner: Option[OwnerEvidence])
  enum ProblemKind {
    case ApplicationNotComplete, CommandMissing, CommandIndeterminate, CommandBindingMismatch, CommandFailed
    case CommandOutputMalformed, OwnerMissing, OwnerBindingMismatch, SourceNotFresh, MappingNotRetained
  }
  final case class Problem(kind: ProblemKind, dimension: String, targetid: Option[String], diagnostic: String)
  final case class Report(postvalidationreference: InternalModelRecordReference, application: ApplicationReport,
    commands: Vector[CommandObservation], findings: Vector[Finding], owner: Option[OwnerEvidence],
    disposition: Disposition, problems: Vector[Problem], freshness: Option[InternalModelPackageFreshnessReport],
    continuity: Option[InternalModelProjectionContinuity])

  private val _runtime_version = "0.3.3-SNAPSHOT"
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _incomplete_kinds = Set(ProblemKind.CommandMissing, ProblemKind.CommandIndeterminate, ProblemKind.OwnerMissing)

  /** Executes no command or write; observations are caller admission, not authentication. */
  def validate(projectRoot: Path, request: Request): Consequence[Report] = {
    val structural = try _structural(projectRoot, request) catch {
      case NonFatal(_) => Left("post-validation root or typed graph is malformed")
    }
    structural.fold(Consequence.operationInvalid, _ => {
      val targets = request.application.gate.plan.toVector.flatMap(_.targets)
      val (commandproblems, findings) = _commands(projectRoot, request, targets)
      val applicationproblems = Option.when(!_application_complete(request.application))(
        _problem(ProblemKind.ApplicationNotComplete, "application", "actual all-target application is not complete")).toVector
      val referenceproblems = Option.when(request.postvalidationreference == request.application.applicationreference ||
        request.commands.exists(_.commandreference == request.postvalidationreference))(
        _problem(ProblemKind.CommandBindingMismatch, "postvalidationreference", "post-validation reference must be independent")).toVector
      val problems = applicationproblems ++ referenceproblems ++ commandproblems ++
        request.owner.fold(Vector(_problem(ProblemKind.OwnerMissing, "owner", "independently refreshed owner evidence is missing")))(
          owner => _owner_selectors(request.application, owner, targets))
      request.owner match {
        case Some(owner) if applicationproblems.isEmpty => _owners(projectRoot, request, owner, targets, findings, problems)
        case _ => Consequence.success(_report(request, findings, problems, None, None))
      }
    })
  }

  private def _structural(projectroot: Path, request: Request): Either[String, Unit] = {
    for {
      _ <- Either.cond(projectroot != null, (), "post-validation project root is missing")
      _ <- _graph(request)
      _ <- Either.cond(projectroot.isAbsolute && projectroot.normalize == projectroot, (), "post-validation root must be normalized and absolute")
      _ <- request.commands.foldLeft[Either[String, Unit]](Right(())) { (result, command) =>
        result.flatMap { _ =>
          val strings = Vector(command.projectroot, command.targetid, command.projectrelativepath, command.sourceauthority,
            command.sourceidentity, command.sourcerevision, command.runtimeversion) ++ command.argv
          for {
            _ <- Either.cond(strings.forall(_nonblank), (), "command binding values must be nonblank valid Unicode")
            _ <- Either.cond(InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(command.projectrelativepath), (), "command target path is unsafe")
            _ <- Either.cond(Path.of(command.projectroot).isAbsolute && Path.of(command.projectroot).normalize.toString == command.projectroot,
              (), "command root must be normalized and absolute")
            _ <- command.outcome match {
              case CommandOutcome.Missing(reason) => Either.cond(_nonblank(reason), (), "missing command requires a reason")
              case CommandOutcome.Indeterminate(reason) => Either.cond(_nonblank(reason), (), "indeterminate command requires a reason")
              case _: CommandOutcome.Terminal => Right(())
            }
          } yield ()
        }
      }
      _ <- request.owner.fold[Either[String, Unit]](Right(())) { owner =>
        for {
          _ <- Either.cond(owner.targetbaselines.forall(entry => _nonblank(entry.targetid)), (), "owner target identities must be nonblank")
          _ <- owner.livesources.values.foldLeft[Either[String, Unit]](Right(())) { (result, input) =>
            result.flatMap { _ => input match {
              case InternalModelPackageFreshnessInput.CmlObserved(authority, identity, revision, path) =>
                Either.cond(_nonblank(authority) && _nonblank(identity) && revision.forall(_nonblank) &&
                  InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(path), (), "owner CML live input is malformed")
              case InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(authority, identity, revision, _, path)) =>
                Either.cond(_nonblank(authority) && _nonblank(identity) && revision.forall(_nonblank) && path.forall(_nonblank), (), "owner source input is malformed")
              case InternalModelPackageFreshnessInput.SourceObservation(observation) => _graph(observation)
            }}
          }
        } yield ()
      }
      _ <- request.application.gate.plan.toVector.flatMap(_.targets).foldLeft[Either[String, Unit]](Right(())) { (result, target) =>
        result.flatMap(_ => Either.cond(_nonblank(target.target.targetId) && _nonblank(target.nextsourcerevision) &&
          InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(target.target.projectRelativePath), (), "application target graph is malformed"))
      }
    } yield ()
  }

  /** Reference factories inspect opaque values inside the structural traversal. */
  private def _graph(value: Any): Either[String, Unit] = value match {
    case null => Left("post-validation graph contains null")
    case text: String => Either.cond(_unicode(text), (), "post-validation graph contains malformed Unicode")
    case reference: InternalModelRecordReference => for {
      _ <- InternalModelRecordId.from(reference.recordId.value)
      _ <- Either.cond(_nonblank(reference.recordId.value), (), "record identity must be nonblank valid Unicode")
      _ <- InternalModelRecordRevision.from(reference.recordRevision.value)
    } yield ()
    case reference: InternalModelArtifactReference => for {
      _ <- InternalModelArtifactId.from(reference.artifactId.value)
      _ <- InternalModelArtifactRevision.from(reference.artifactRevision.value)
      _ <- Either.cond(reference.role != null, (), "artifact role is missing")
    } yield ()
    case reference: InternalModelPackageReference => for {
      _ <- InternalModelPackageId.from(reference.packageId.value)
      _ <- InternalModelProjectToken.from(reference.projectNamespace.value)
      _ <- InternalModelProjectToken.from(reference.projectId.value)
    } yield ()
    case scope: InternalModelSemanticScope => Either.cond(Vector(scope.componentIdentity, scope.projectionContextIdentity,
      scope.selectedUseCaseElementIdentity).forall(_nonblank), (), "semantic scope must be nonblank valid Unicode")
    case values: Iterable[?] => values.foldLeft[Either[String, Unit]](Right(()))((result, item) => result.flatMap(_ => _graph(item)))
    case product: Product => product.productIterator.foldLeft[Either[String, Unit]](Right(()))((result, item) => result.flatMap(_ => _graph(item)))
    case _ => Right(())
  }

  private def _application_complete(application: ApplicationReport): Boolean = {
    val targets = application.gate.plan.toVector.flatMap(_.targets)
    application.disposition == InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation &&
      application.gate.eligibility == InternalModelCmlChangeEligibility.EligibleForSkillApplication && targets.nonEmpty &&
      targets.map(_.target.targetId).distinct.size == targets.size && application.writes.exists { writes =>
        writes.failure.isEmpty && writes.cleanupfailures.isEmpty && writes.outcomes.size == targets.size &&
          writes.outcomes.zip(targets).forall { case (outcome, target) =>
            outcome.targetid == target.target.targetId && outcome.projectrelativepath == target.target.projectRelativePath &&
              outcome.disposition == NativeCmlFileWriter.Disposition.Applied && outcome.failure.isEmpty &&
              outcome.byteswritten == target.payload.proposedRawBytes.size.toLong && outcome.mayhavechanged
          }
      }
  }

  private def _commands(projectroot: Path, request: Request, targets: Vector[InternalModelCmlApplicationTarget]): (Vector[Problem], Vector[Finding]) = {
    val problems = Vector.newBuilder[Problem]
    val findings = Vector.newBuilder[Finding]
    val commands = request.commands
    if (commands.map(_.commandreference).distinct.size != commands.size || commands.map(_.targetid).distinct.size != commands.size)
      problems += _problem(ProblemKind.CommandBindingMismatch, "commands.unique", "command references and target observations must be unique")
    targets.filterNot(target => commands.exists(_.targetid == target.target.targetId)).foreach { target =>
      problems += _problem(ProblemKind.CommandMissing, "commands.target", "required target command observation is missing", Some(target.target.targetId))
    }
    commands.foreach { command =>
      targets.find(_.target.targetId == command.targetid) match {
        case None => problems += _problem(ProblemKind.CommandBindingMismatch, "command.targetid", "command names an unknown applied target", Some(command.targetid))
        case Some(target) =>
          val argv = Vector("--runtime", _runtime_version, "lint", "cml", projectroot.resolve(target.target.projectRelativePath).normalize.toString, "--format", "json")
          val checks = Vector(
            "applicationreference" -> (command.applicationreference == request.application.applicationreference),
            "commandreference" -> (command.commandreference != request.application.applicationreference),
            "projectroot" -> (command.projectroot == projectroot.toString && request.application.gate.plan.exists(_.authority.projectroot == projectroot.toString)),
            "projectrelativepath" -> (command.projectrelativepath == target.target.projectRelativePath),
            "sourceauthority" -> (command.sourceauthority == target.target.source.authority),
            "sourceidentity" -> (command.sourceidentity == target.target.source.identity),
            "sourcerevision" -> (command.sourcerevision == target.nextsourcerevision),
            "runtimeversion" -> (command.runtimeversion == _runtime_version), "argv" -> (command.argv == argv))
          checks.collect { case (dimension, false) => _problem(ProblemKind.CommandBindingMismatch, "command." + dimension,
            "actual command binding differs from the exact applied target", Some(command.targetid)) }.foreach(problems += _)
      }
      command.outcome match {
        case CommandOutcome.Missing(reason) => problems += _problem(ProblemKind.CommandMissing, "command.outcome", reason, Some(command.targetid))
        case CommandOutcome.Indeterminate(reason) => problems += _problem(ProblemKind.CommandIndeterminate, "command.outcome", reason, Some(command.targetid))
        case CommandOutcome.Terminal(exitcode, stdout, _) =>
          if (exitcode != 0) problems += _problem(ProblemKind.CommandFailed, "command.exitcode", s"actual command exit code is $exitcode", Some(command.targetid))
          _findings(stdout) match {
            case Left(reason) => problems += _problem(ProblemKind.CommandOutputMalformed, "command.stdout", reason, Some(command.targetid))
            case Right(observed) =>
              findings ++= observed
              observed.filter(_.level == "FAIL").foreach(finding => problems += _problem(ProblemKind.CommandFailed,
                "command.finding", finding.code + ": " + finding.message, Some(command.targetid)))
          }
      }
    }
    (problems.result(), findings.result())
  }

  private def _findings(stdout: String): Either[String, Vector[Finding]] = for {
    json <- _json_parser.parse(stdout).left.map(_ => "Cozy stdout must be actual finding JSON without duplicate members")
    root <- json.asObject.toRight("Cozy finding root must be an object")
    _ <- Either.cond(root.keys.toSet == Set("findings"), (), "Cozy finding root must contain only findings")
    values <- root("findings").flatMap(_.asArray).toRight("Cozy findings must be an array")
    findings <- values.foldLeft[Either[String, Vector[Finding]]](Right(Vector.empty)) { (result, value) => for {
      collected <- result
      fields <- value.asObject.toRight("Cozy finding must be an object")
      _ <- Either.cond(fields.keys.toSet == Set("level", "code", "message", "path", "line"), (), "Cozy finding fields must be closed")
      level <- _finding_string(fields, "level")
      _ <- Either.cond(Set("FAIL", "WARN").contains(level), (), "Cozy finding level must be FAIL or WARN")
      code <- _finding_string(fields, "code")
      message <- _finding_string(fields, "message")
      path <- _finding_string(fields, "path")
      line <- fields("line").flatMap(_.asNumber).flatMap(_.toInt).filter(_ > 0).toRight("Cozy finding line must be a positive Int")
    } yield collected :+ Finding(level, code, message, path, line) }
  } yield findings

  private def _finding_string(fields: JsonObject, key: String): Either[String, String] =
    fields(key).flatMap(_.asString).filter(_nonblank).toRight(s"Cozy finding $key must be nonblank valid Unicode")

  private def _owner_selectors(application: ApplicationReport, owner: OwnerEvidence, targets: Vector[InternalModelCmlApplicationTarget]): Vector[Problem] = {
    val plan = application.gate.plan
    val checks = plan.toVector.flatMap { selected => Vector(
      "package" -> (owner.packagereference == selected.packagereference), "scope" -> (owner.scope == selected.scope),
      "realizationartifactreference" -> (owner.realizationartifactreference.role == InternalModelArtifactRole.Realization && owner.realizationartifactreference != selected.realizationartifactreference),
      "realizationreference" -> (owner.realizationreference != selected.realizationreference),
      "continuityartifactreference" -> (owner.continuityartifactreference.role == InternalModelArtifactRole.Projection && owner.continuityartifactreference != selected.continuityartifactreference),
      "continuityreference" -> (owner.continuityreference != selected.continuityreference)) }
    val problems = checks.collect { case (dimension, false) => _problem(ProblemKind.OwnerBindingMismatch, "owner." + dimension,
      "independent owner selector must bind the original scope and explicit refreshed reference") }
    val baselines = owner.targetbaselines
    val unique = Option.when(baselines.map(_.targetid).distinct.size != baselines.size ||
      baselines.map(_.snapshotreference).distinct.size != baselines.size)(
      _problem(ProblemKind.OwnerBindingMismatch, "owner.targetbaselines.unique", "target snapshot selectors must be unique")).toVector
    val missing = targets.filterNot(target => baselines.exists(_.targetid == target.target.targetId)).map(target =>
      _problem(ProblemKind.OwnerMissing, "owner.targetbaseline", "independent refreshed target snapshot is missing", Some(target.target.targetId)))
    val binding = baselines.flatMap { baseline => targets.find(_.target.targetId == baseline.targetid) match {
      case None => Vector(_problem(ProblemKind.OwnerBindingMismatch, "owner.targetbaseline", "owner names an unknown target", Some(baseline.targetid)))
      case Some(target) => Option.when(baseline.snapshotreference.role != InternalModelArtifactRole.SourceSnapshot ||
        baseline.snapshotreference == target.target.baselineArtifactReference)(
        _problem(ProblemKind.OwnerBindingMismatch, "owner.targetbaseline", "target requires an explicit new source-snapshot reference", Some(baseline.targetid))).toVector
    }}
    problems ++ unique ++ missing ++ binding
  }

  private def _owners(projectroot: Path, request: Request, owner: OwnerEvidence, targets: Vector[InternalModelCmlApplicationTarget],
    findings: Vector[Finding], problems: Vector[Problem]): Consequence[Report] = {
    InternalModelPackageValidator.verifiedContinuation(projectroot).flatMap { handoff =>
      val carrier = handoff.continuityPackage
      val checks = Vector("package" -> (handoff.packageContext.reference == owner.packagereference),
        "realizationartifactreference" -> (carrier.realizationpackage.realization.reference == owner.realizationartifactreference),
        "continuityartifactreference" -> (carrier.projection.reference == owner.continuityartifactreference))
      val selectedproblems = checks.collect { case (dimension, false) => _problem(ProblemKind.OwnerBindingMismatch,
        "current." + dimension, "actual rooted carrier differs from independently selected owner evidence") }
      val snapshots = carrier.realizationpackage.sourcesnapshots
      val known = snapshots.map(_.reference).toSet
      val unknown = owner.livesources.keySet -- known
      val keyproblems = Option.when(unknown.nonEmpty)(_problem(ProblemKind.OwnerBindingMismatch, "owner.livesources.reference",
        "live source keys contain unknown, stale or wrong-role full references")).toVector
      val targetproblems = owner.targetbaselines.flatMap { baseline =>
        val snapshotproblems = Option.when(!known.contains(baseline.snapshotreference))(_problem(ProblemKind.OwnerBindingMismatch,
          "current.targetbaseline", "selected new target baseline is not in the actual carrier", Some(baseline.targetid))).toVector
        val inputproblems = targets.find(_.target.targetId == baseline.targetid).toVector.flatMap { target =>
          owner.livesources.get(baseline.snapshotreference) match {
            case None => Vector(_problem(ProblemKind.OwnerMissing, "owner.livecml", "required source-owner current CML request is missing", Some(baseline.targetid)))
            case Some(InternalModelPackageFreshnessInput.CmlObserved(authority, identity, revision, path)) =>
              val mismatch = Option.when(authority != target.target.source.authority || identity != target.target.source.identity ||
                path != target.target.projectRelativePath || revision.exists(_ != target.nextsourcerevision))(
                _problem(ProblemKind.OwnerBindingMismatch, "owner.livecml", "owner CML request differs from applied ownership/path/next revision", Some(baseline.targetid))).toVector
              mismatch ++ Option.when(revision.isEmpty)(_problem(ProblemKind.OwnerMissing, "owner.livecml.revision",
                "independent current CML source revision is missing", Some(baseline.targetid))).toVector
            case Some(_) => Vector(_problem(ProblemKind.OwnerBindingMismatch, "owner.livecml", "caller source bytes cannot substitute for actual CML observation", Some(baseline.targetid)))
          }
        }
        snapshotproblems ++ inputproblems
      }
      val currentproblems = problems ++ selectedproblems ++ keyproblems ++ targetproblems
      if (selectedproblems.nonEmpty || keyproblems.nonEmpty) Consequence.success(_report(request, findings, currentproblems, None, None))
      else InternalModelPackageFreshness.check(projectroot, owner.livesources).flatMap { freshness =>
        val selectedbaselines = owner.targetbaselines.map(_.snapshotreference).toSet
        val freshproblems = freshness.entries.flatMap { entry =>
          val required = selectedbaselines.contains(entry.reference) || snapshots.exists(snapshot => snapshot.reference == entry.reference && snapshot.required)
          entry.result match {
            case InternalModelPackageFreshnessResult.MissingBaseline if !required => Vector.empty
            case InternalModelPackageFreshnessResult.MissingBaseline => Vector(_problem(ProblemKind.OwnerMissing, "freshness.baseline", "required snapshot is unavailable"))
            case InternalModelPackageFreshnessResult.Compared(report) if report.status == InternalModelSnapshotFreshnessStatus.Unchanged && report.missingDimensionNames.isEmpty => Vector.empty
            case InternalModelPackageFreshnessResult.Compared(report) =>
              val missingrevision = report.status == InternalModelSnapshotFreshnessStatus.Incomplete &&
                report.changedDimensionNames == Vector("source.revision") && report.missingDimensionNames.contains("observed.source.revision")
              val missing = Option.when(report.missingDimensionNames.nonEmpty || Set(InternalModelSnapshotFreshnessStatus.Incomplete,
                InternalModelSnapshotFreshnessStatus.Unavailable).contains(report.status))(
                _problem(ProblemKind.OwnerMissing, "freshness." + entry.reference.artifactId.value, "required current source has missing evidence: " + report.missingDimensionNames.mkString(","))).toVector
              val failed = Option.when((report.changedDimensionNames.nonEmpty && !missingrevision) || Set(InternalModelSnapshotFreshnessStatus.Changed,
                InternalModelSnapshotFreshnessStatus.Unauthorized, InternalModelSnapshotFreshnessStatus.Malformed,
                InternalModelSnapshotFreshnessStatus.AmbiguousOrConflicting).contains(report.status))(
                _problem(ProblemKind.SourceNotFresh, "freshness." + entry.reference.artifactId.value, report.reason.getOrElse("required source is not unchanged"))).toVector
              missing ++ failed
          }
        }
        val allproblems = currentproblems ++ freshproblems
        InternalModelProjectionContinuityValidator.validateVerified(carrier).map { continuity =>
          val logicalchecks = Vector("realizationreference" -> (continuity.realization.realizationReference == owner.realizationreference),
            "continuityreference" -> (continuity.binding.bindingReference == owner.continuityreference),
            "scope" -> (continuity.realization.scope == owner.scope && continuity.binding.scope == InternalModelProjectionBindingScope(
              owner.scope.componentIdentity, owner.scope.projectionContextIdentity, owner.scope.selectedUseCaseElementIdentity)))
          val logicalproblems = logicalchecks.collect { case (dimension, false) => _problem(ProblemKind.OwnerBindingMismatch,
            "current." + dimension, "actual semantic owner differs from independent refreshed selector") }
          val mappingproblems = _mappings(request.application, continuity.realization)
          val finalproblems = allproblems ++ logicalproblems ++ mappingproblems
          _report(request, findings, finalproblems, Some(freshness), Option.when(finalproblems.isEmpty)(continuity))
        }
      }
    }
  }

  private def _mappings(application: ApplicationReport, current: InternalModelSemanticRealization): Vector[Problem] = {
    val original = application.gate.continuation.state.continuity.realization
    def _source_retained_(oldid: String, newid: String): Boolean = {
      val old = original.sourceReferences.find(_.referenceId == oldid)
      val next = current.sourceReferences.find(_.referenceId == newid)
      old.exists(prior => next.exists(value => prior.referenceId == value.referenceId && prior.source.authority == value.source.authority &&
        prior.source.identity == value.source.identity && prior.source.locator == value.source.locator &&
        prior.sourceAnchor == value.sourceAnchor && prior.target == value.target))
    }
    def _assertions_retained_(ids: Vector[String], before: Vector[InternalModelSemanticAssertion], after: Vector[InternalModelSemanticAssertion],
      mapping: InternalModelCandidateCmlMapping): Boolean = ids.forall { id =>
      before.find(_.assertionId == id).exists { prior => after.find(_.assertionId == id).exists { next =>
        next.semanticIdentityKind == mapping.semanticIdentityKind && next.semanticIdentity == mapping.semanticIdentity &&
          prior.semanticIdentityKind == next.semanticIdentityKind && prior.semanticIdentity == next.semanticIdentity &&
          prior.content == next.content && prior.association == next.association && prior.conditionIds == next.conditionIds &&
          _source_retained_(prior.sourceReferenceId, next.sourceReferenceId)
      }}
    }
    application.gate.plan.toVector.flatMap(_.targets).flatMap { target => target.target.mappings.flatMap { mapping =>
      val links = mapping.semanticIdentityKind match {
        case "element" => current.elements.find(_.identity == mapping.semanticIdentity).map(value =>
          (value.canonicalAssertionIds, value.enrichmentAssertionIds, value.conditionIds))
        case "relationship" => current.relationships.find(_.identity == mapping.semanticIdentity).map(value =>
          (value.canonicalAssertionIds, value.enrichmentAssertionIds, value.conditionIds))
        case _ => None
      }
      val retained = links.exists { case (canonical, enrichment, conditions) =>
        mapping.canonicalAssertionIds.forall(canonical.contains) && mapping.enrichmentAssertionIds.forall(enrichment.contains) &&
          mapping.conditionIds.forall(conditions.contains)
      } && _assertions_retained_(mapping.canonicalAssertionIds, original.canonicalAssertions, current.canonicalAssertions, mapping) &&
        _assertions_retained_(mapping.enrichmentAssertionIds, original.enrichmentAssertions, current.enrichmentAssertions, mapping) &&
        mapping.conditionIds.forall { id => original.conditions.find(_.conditionId == id).exists { prior =>
          current.conditions.find(_.conditionId == id).exists { next => prior.kind == next.kind &&
            prior.affectedKind == next.affectedKind && next.affectedKind == mapping.semanticIdentityKind &&
            prior.affectedIdentity == next.affectedIdentity && next.affectedIdentity == mapping.semanticIdentity &&
            prior.detail == next.detail && _source_retained_(prior.sourceReferenceId, next.sourceReferenceId) }
        }}
      Option.when(!retained)(_problem(ProblemKind.MappingNotRetained, "mapping." + mapping.mappingId,
        "mapped exact identity, assertion lanes, attribution or full conditions were not retained", Some(target.target.targetId))).toVector
    }}
  }

  private def _report(request: Request, findings: Vector[Finding], problems: Vector[Problem],
    freshness: Option[InternalModelPackageFreshnessReport], continuity: Option[InternalModelProjectionContinuity]): Report = {
    val disposition = if (problems.exists(problem => !_incomplete_kinds.contains(problem.kind))) Disposition.Failed
      else if (problems.nonEmpty) Disposition.Incomplete else Disposition.ReprojectedPendingAcceptance
    Report(request.postvalidationreference, request.application, request.commands, findings, request.owner,
      disposition, problems, freshness, continuity)
  }
  private def _problem(kind: ProblemKind, dimension: String, diagnostic: String, targetid: Option[String] = None): Problem =
    Problem(kind, dimension, targetid, diagnostic)
  private def _unicode(text: String): Boolean = text != null && text.codePoints().toArray.forall(code => code < 0xd800 || code > 0xdfff)
  private def _nonblank(text: String): Boolean = _unicode(text) &&
    text.codePoints().toArray.exists(code => !Character.isWhitespace(code) && !Character.isSpaceChar(code) && code != 0x85)
}
