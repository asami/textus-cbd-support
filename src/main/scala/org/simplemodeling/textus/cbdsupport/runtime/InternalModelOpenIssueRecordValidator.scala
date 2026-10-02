package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Path

import scala.util.control.NonFatal

import org.goldenport.Consequence

/*
 * @since   Sep. 28, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Admits one captured open-issue ledger against its one captured semantic realization. */
private[runtime] object InternalModelOpenIssueRecordValidator {
  def validate(projectRoot: Path): Consequence[InternalModelOpenIssueAdmission] = {
    try {
      InternalModelPackageValidator.verifiedOpenIssueRecords(projectRoot).flatMap(validateVerified)
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model open-issue record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }
  }

  private[runtime] def validateVerified(handoff: InternalModelVerifiedOpenIssuePackage): Consequence[InternalModelOpenIssueAdmission] = {
    try {
      _captured_paths(handoff).fold(Consequence.operationInvalid, _ => InternalModelSemanticRealizationValidator.validateVerified(handoff.realizationpackage).flatMap { realization =>
        (for {
          ledger <- InternalModelOpenIssueRecordCodec.decode(handoff.openissue)
          admission <- _admission(handoff, realization, ledger)
        } yield admission).fold(Consequence.operationInvalid, Consequence.success)
      })
    } catch {
      case NonFatal(error) => Consequence.operationInvalid(s"internal-model open-issue record validation failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }
  }

  private def _admission(
    handoff: InternalModelVerifiedOpenIssuePackage,
    realization: InternalModelSemanticRealization,
    ledger: InternalModelOpenIssueLedger
  ): Either[String, InternalModelOpenIssueAdmission] = {
    val selected = handoff.realizationpackage.realization
    for {
      _ <- Either.cond(ledger.scope == realization.scope, (), "open-issue ledger scope does not equal selected realization scope")
      _ <- Either.cond(handoff.openissue.dependencies.contains(selected.reference), (), "selected open-issue must depend on the exact selected realization artifact reference")
      _ <- Either.cond(ledger.basis.realizationArtifactReference == selected.reference, (), "open-issue ledger basis artifact reference does not equal selected realization artifact reference")
      _ <- Either.cond(ledger.basis.realizationReference == realization.realizationReference, (), "open-issue ledger basis logical reference does not equal selected realization reference")
      _ <- _first_failure(ledger.issues.iterator.map(issue => _issue_admission(issue, realization)))
    } yield InternalModelOpenIssueAdmission(ledger, realization)
  }

  private def _issue_admission(
    issue: InternalModelOpenIssueRecord,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    val conditionids = realization.conditions.map(_.conditionId).toSet
    val targets = issue.affectedTargets.toSet
    val targetconditions =
      if issue.affectedTargets.isEmpty then realization.conditions.map(_.conditionId).toSet
      else realization.conditions.collect {
        case condition if targets.contains(InternalModelSemanticTarget(condition.affectedKind, condition.affectedIdentity)) => condition.conditionId
      }.toSet
    val evidenceconditions = issue.consideredEvidence.flatMap(_.conditionIds).toSet
    for {
      _ <- _first_failure(issue.affectedTargets.iterator.map(target => _target_exists(target, realization, issue.issueReference.recordId.value)))
      _ <- Either.cond(issue.realizationConditionIds.forall(conditionids.contains), (), s"open-issue record ${issue.issueReference.recordId.value} names an unknown current realization condition")
      _ <- Either.cond(targetconditions.subsetOf(issue.realizationConditionIds.toSet), (), s"open-issue record ${issue.issueReference.recordId.value} hides a required affected-target or scope-wide condition")
      _ <- Either.cond(evidenceconditions.subsetOf(issue.realizationConditionIds.toSet), (), s"open-issue record ${issue.issueReference.recordId.value} omits an evidence condition from its realization condition set")
      _ <- _first_failure(issue.consideredEvidence.iterator.map(evidence => _evidence(evidence, issue.issueReference.recordId.value, realization)))
      _ <- _first_failure(issue.options.iterator.map(option => _option(option, issue)))
    } yield ()
  }

  private def _target_exists(
    target: InternalModelSemanticTarget,
    realization: InternalModelSemanticRealization,
    issueidentity: String
  ): Either[String, Unit] = {
    target.semanticIdentityKind match {
      case "element" => Either.cond(realization.elements.exists(_.identity == target.semanticIdentity), (), s"open-issue record $issueidentity targets an unknown or cross-scope element")
      case "relationship" => Either.cond(realization.relationships.exists(_.identity == target.semanticIdentity), (), s"open-issue record $issueidentity targets an unknown or cross-scope relationship")
      case _ => Left(s"open-issue record $issueidentity target has an invalid semantic identity kind")
    }
  }

  private def _evidence(
    evidence: InternalModelOpenIssueEvidence,
    issueidentity: String,
    realization: InternalModelSemanticRealization
  ): Either[String, Unit] = {
    evidence.kind match {
      case InternalModelOpenIssueEvidenceKind.RealizationSource =>
        for {
          referenceid <- evidence.sourceReferenceId.toRight(s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} lacks a realization source reference")
          reference <- realization.sourceReferences.find(_.referenceId == referenceid).toRight(s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} names an unknown current source reference")
          _ <- Either.cond(reference.source == evidence.source, (), s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} source does not equal its admitted realization reference")
          expectedconditions = realization.conditions.filter(_.sourceReferenceId == referenceid).map(_.conditionId)
          _ <- Either.cond(evidence.conditionIds == expectedconditions, (), s"open-issue record $issueidentity evidence ${evidence.evidenceIdentity} hides or changes source-reference conditions")
        } yield ()
      case InternalModelOpenIssueEvidenceKind.ExternalHuman |
          InternalModelOpenIssueEvidenceKind.ProviderProposal |
          InternalModelOpenIssueEvidenceKind.ExternalOther =>
        Either.cond(evidence.sourceReferenceId.isEmpty && evidence.conditionIds.isEmpty, (), s"open-issue record $issueidentity external evidence must not link realization sources or conditions")
    }
  }

  private def _option(
    option: InternalModelOpenIssueOption,
    issue: InternalModelOpenIssueRecord
  ): Either[String, Unit] = {
    val evidenceids = issue.consideredEvidence.map(_.evidenceIdentity).toSet
    Either.cond(
      option.evidenceIds.forall(evidenceids.contains),
      (),
      s"open-issue record ${issue.issueReference.recordId.value} option ${option.optionIdentity} links evidence outside its owning issue"
    )
  }

  private def _first_failure(values: Iterator[Either[String, Unit]]): Either[String, Unit] = {
    values.collectFirst { case Left(reason) => reason } match {
      case Some(reason) => Left(reason)
      case None => Right(())
    }
  }

  private def _captured_paths(handoff: InternalModelVerifiedOpenIssuePackage): Either[String, Unit] =
    for {
      _ <- Either.cond(handoff != null && handoff.openissue != null && handoff.realizationpackage != null, (), "open-issue capture, selected issue and realization package must be present")
      selected = handoff.realizationpackage
      _ <- Either.cond(selected.realization != null && selected.sourcesnapshots != null, (), "captured realization and source-snapshot collection must be present")
      _ <- Either.cond(selected.realization.path != null && !selected.realization.path.isBlank, (), "captured realization path must be present")
      _ <- selected.sourcesnapshots.foldLeft[Either[String, Unit]](Right(())) { (result, snapshot) =>
        result.flatMap(_ => Either.cond(snapshot != null && snapshot.path != null && !snapshot.path.isBlank, (), "captured source-snapshot entry and path must be present"))
      }
    } yield ()
}
