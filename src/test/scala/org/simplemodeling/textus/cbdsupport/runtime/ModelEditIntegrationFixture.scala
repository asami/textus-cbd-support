package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.util.control.NonFatal
import io.circe.Json
import io.circe.syntax.*
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.record.io.RecordEncoder
import ModelEditSession.*
import ModelEditCmlProjection.given
import ModelEditSourceProjectionCodec.given
import ModelEditCandidateCodec.given

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Native-owned effectful preparation. It supplies no product review or human decision. */
private[runtime] object ModelEditIntegrationFixture {
  import ModelEditProjectionSpecFixture.*
  final case class Preparation(root: Path, original: Vector[Byte], afterPreparation: Vector[Byte],
    basis: ModelEditSourceProjection.Publication, reread: ModelEditSourceProjection.Publication,
    session: ModelEditSession, missingTicketObserved: Boolean, missingTicketCleared: Boolean,
    view: ModelEditViewProjection.View, confirmation: ModelEditConfirmation.Prepared,
    rendering: ModelEditSourceProjection.Rendering, reproduced: ModelEditSourceProjection.Publication,
    producerEntriesPresent: Boolean, sharedActor: Boolean, phase10: ModelEditPhase10SpecFixture.Prepared)

  val specificationPath: Path = Path.of("src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/ModelEditServiceSpec.scala")
  val specificationArtifact: InternalModelArtifactReference = ModelEditSpecFixture.artifact("phase12-existing-specification", role = InternalModelArtifactRole.Validation)
  def sourceSelection(publication: ModelEditSourceProjection.Publication): ModelEditSourceProjection.Selection =
    selection(publication).copy(owners = Vector(ModelEditSourceProjection.OwnerBinding("example/order",
      RepositoryProjectReference(record("asami/textus-cbd-support").recordId, record("textus-cbd-support").recordId))))
  def editSession(publication: ModelEditSourceProjection.Publication): ModelEditSession =
    ModelEditSession(record("session"), scope, ModelEditSourceProjection.importCandidate(sourceSelection(publication), record("candidate"), scope).toOption.get,
      SessionState.Active)

  def prepare(producerRoot: Path, outputRoot: Path): Preparation = {
    Files.createDirectories(outputRoot)
    val root = Files.createTempDirectory(outputRoot, "actual-")
    val input = root.resolve("models/order.cml")
    Files.createDirectories(input.getParent)
    Files.copy(Path.of("src/test/resources/model-edit-producer/model/model-edit-source.cml"), input)
    val original = Files.readAllBytes(input).toVector
    Files.readAllBytes(specificationPath) // Actual existing Specification observation precedes host resolution admission.
    val classpath = Files.readString(producerRoot.resolve("target/cozy.d/runtime-classpath.txt")).trim
    val entriespresent = classpath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator), -1).toVector
      .forall(value => value.nonEmpty && Files.exists(Path.of(value)))
    val basistext = runProducer(classpath, root, "produce-basis", Vector("produce", input.toString, source.owner, source.identity, source.revision))
    val basis = ModelEditSourceProjectionCodec.decodePublication(basistext).toOption.get
    val publicationpath = root.resolve("publication.json")
    Files.writeString(publicationpath, basistext)
    val reread = ModelEditSourceProjectionCodec.decodePublication(runProducer(classpath, root, "read-basis", Vector("read", publicationpath.toString))).toOption.get
    val originalrequest = io.circe.parser.parse(Files.readString(Path.of("src/test/resources/model-edit-producer/requests/model-edit-production.json")))
      .toOption.get.as[ModelEditCmlProjection.Request].toOption.get
    val request = originalrequest.copy(addedRelationships = originalrequest.addedRelationships.map { edge => edge.to match {
      case ModelEditSourceProjection.Endpoint.SpecificationTarget(value) => edge.copy(to = ModelEditSourceProjection.Endpoint.SpecificationTarget(
        value.copy(identity = "org.simplemodeling.textus.cbdsupport.runtime.ModelEditServiceSpec", sourceRevision = "stage12-07-spec-source-1")))
      case _ => edge
    } })
    val proposed = basis.copy(source = request.source, document = basis.document.copy(coverage = request.coverage,
      elements = basis.document.elements ++ request.addedElements, relationships = basis.document.relationships ++ request.addedRelationships))
    val desired = editSession(proposed).candidate
    val initial = editSession(basis)
    val actor = initial.candidate.actors.head
    val goal = desired.goals.head
    val directcontext = ModelEditPalette.Context(ModelEditPalette.Surface.ActorGoal, initial.scope, Subject.Actor(actor.semanticTargetId))
    val directoperation = ModelEditSpecFixture.operationFor(initial, Command.AddGoal(actor, goal.reference.semanticTargetId, "Draft repeat purchase intent", Some(goal.reference)))
    val direct = ModelEditModelMutation.applyOperation(initial, directcontext, directoperation, record("candidate", 2),
      ModelEditSpecFixture.authorization(initial), Vector.empty, sourceSelection(basis), Map.empty).toOption.get.session
    val conversationcontext = ModelEditPalette.Context(ModelEditPalette.Surface.ActorGoal, direct.scope, Subject.Goal(goal.reference.semanticTargetId))
    val intent = ModelEditConversationalAdapter.Request("Refine the provisional repeat purchase goal", ModelEditConversationalAdapter.Intent.RefineGoal(goal.intent, Some(goal.reference)))
    val proposal = ModelEditConversationalAdapter.propose(direct, conversationcontext, intent, record("conversation-goal"), record("candidate", 3),
      goal.attribution, ModelEditSpecFixture.authorization(direct), Vector.empty).toOption.get
    val refined = ModelEditConversationalAdapter.applyProposal(direct, proposal, ModelEditSpecFixture.authorization(direct), Vector.empty, sourceSelection(basis), Map.empty).toOption.get.session
    val linked = ModelEditSpecFixture.applyCommand(refined, Command.LinkGoalUseCase(desired.goalLinks.head))
    val policy = FindingPolicy(record("e2e-project-policy"), FindingKind.values.map(kind => kind -> FindingDisposition(Severity.Warning, false)).toMap)
    // The source alias maps explicitly to this repository. The existing Specification is captured; Ticket evidence is provisional only.
    val resolutions = Vector(ResolutionEvidence(ExternalReference.Specification(desired.specificationLinks.head.specification), ResolutionState.Resolved, snapshot, desired.specificationLinks.head.attribution),
      ResolutionEvidence(ExternalReference.Ticket(desired.ticketLinks.head.ticket), ResolutionState.Resolved, snapshot, desired.ticketLinks.head.attribution))
    val application = ModelEditApplication.create(linked, sourceSelection(basis), Vector(ModelEditAuthorization.Binding("codex", true, Some(ModelEditSpecFixture.authorization(linked)))),
      policy, resolutions, Vector.empty, Vector.empty).toOption.get
    val host = ModelEditExternalOperationsSpec.host(Some(application), "codex")
    given ExecutionContext = host.context
    def _apply_(command: Command, subject: Subject): ModelEditSession = {
      val current = ModelEditApplicationSpecFixture.successfulValue(application.capture())
      val successor = record("candidate", current.candidate.reference.recordRevision.value + 1)
      val context = ModelEditPalette.Context(ModelEditPalette.Surface.EventStorming, current.scope, subject)
      val request = ModelEditOperationJson.EditRequest(ModelEditOperationJson.Selection(current.reference, current.candidate.reference), context,
        ModelEditSpecFixture.operationFor(current, command), successor)
      ModelEditExternalOperationsSpec.invoke("applyModelEdit", ModelEditOperationJson.edit(request), host)
      ModelEditApplicationSpecFixture.successfulValue(application.capture())
    }
    val slice = desired.slices.head
    val created = _apply_(Command.CreateUseCaseSlice(slice.copy(title = "Draft repeat order Slice")), Subject.UseCase(slice.useCaseIdentity))
    val refinedslice = slice.copy(reference = slice.reference.copy(recordRevision = InternalModelRecordRevision.from(2).toOption.get))
    _apply_(Command.RefineUseCaseSlice(created.candidate.slices.head.reference, refinedslice), Subject.Slice(slice.identity))
    val specified = _apply_(Command.LinkSliceSpecification(desired.specificationLinks.head), Subject.Slice(slice.identity))
    val missing = ModelEditReview.reviewSource(specified, policy, sourceSelection(basis), resolutions, Vector.empty, Vector.empty).toOption.get
    _apply_(Command.LinkSliceTicket(desired.ticketLinks.head), Subject.Slice(slice.identity))
    _apply_(Command.UnlinkSliceTicket(desired.ticketLinks.head), Subject.Relationship(desired.ticketLinks.head.reference))
    val edited = _apply_(Command.LinkSliceTicket(desired.ticketLinks.head), Subject.Slice(slice.identity))
    val cleared = ModelEditReview.reviewSource(edited, policy, sourceSelection(basis), resolutions, Vector.empty, Vector.empty).toOption.get
    val bindings = sourceSelection(proposed).bindings.map(value => if (value.semanticIdentity == slice.identity) value.copy(recordReference = refinedslice.reference) else value)
    val plan = ModelEditCmlProjection.prepare(sourceSelection(basis), edited, request.source, proposed.document, bindings).toOption.get
    val candidatepath = root.resolve("candidate-v2.json")
    Files.writeString(candidatepath, ModelEditCmlProjection.encodeRequest(plan))
    val rendering = ModelEditSourceProjectionCodec.decodeRendering(runProducer(classpath, root, "render-candidate", Vector("render-candidate-v2", input.toString,
      source.owner, source.identity, source.revision, candidatepath.toString))).toOption.get
    val proposedpath = root.resolve("candidate.cml")
    Files.writeString(proposedpath, rendering.text)
    val reproduced = ModelEditSourceProjectionCodec.decodePublication(runProducer(classpath, root, "reproduce-candidate", Vector("produce", proposedpath.toString,
      request.source.owner, request.source.identity, request.source.revision))).toOption.get
    val confirmed = ModelEditCmlProjection.confirm(plan, rendering, reproduced).toOption.get
    val planned = completeDiff(ModelEditPhase10SpecFixture.prepare(confirmed, edited, original).toOption.get, basis, reproduced)
    writePackage(root, planned, edited, original)
    val admitted = InternalModelSemanticDiffValidator.validate(root).toOption.get
    val phase10 = planned.copy(candidate = admitted.candidateAdmission, diff = admitted)
    Files.createDirectories(root.resolve("review-materials"))
    Files.copy(specificationPath, root.resolve("review-materials/ModelEditServiceSpec.scala"))
    inspectThroughOperations(root, edited, sourceSelection(basis), policy, resolutions, phase10)
    val paths = phase10.candidate.packageContext.artifacts.map(value => Path.of("").toAbsolutePath.relativize(root.resolve("src/main/internal-model").resolve(value.path)).toString) :+
      Path.of("").toAbsolutePath.relativize(proposedpath).toString
    val reviewpaths = paths ++ Vector("codex-getModelEditView.json", "codex-getModelEditDiff.json", "codex-getModelEditTraces.json", "codex-getModelEditReview.json",
      "chatgpt-getModelEditView.json", "candidate.html", "semantic-diff.json", "confirmation.json").map(name => Path.of("").toAbsolutePath.relativize(root.resolve(name)).toString)
    val confirmation = ModelEditConfirmation.prepare(edited, confirmed, phase10.mapping, phase10.diff, record("phase12-product-subject"),
      ModelEditSpecFixture.artifact("phase12-product-review", role = InternalModelArtifactRole.Validation), record("phase12-product-review"), reviewpaths, Vector(specificationArtifact)).toOption.get
    Files.writeString(root.resolve("review-request.json"), RecordEncoder.json(confirmation.reviewRequest.toRecord()))
    val view = ModelEditViewProjection.projectConfirmed(edited, confirmed, Map.empty).toOption.get
    Files.writeString(root.resolve("candidate.html"), "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><title>Exact Model Edit candidate</title><body>" +
      ModelEditWebView.confirmation(view) + "<p>Ticket integration is provisional. Live provider existence is not established.</p></body></html>")
    Files.writeString(root.resolve("semantic-diff.json"), ModelEditOperationJson.semanticDiff(phase10.diff.diff).spaces2 + "\n")
    val editentry = phase10.candidate.packageContext.artifacts.find(_.reference.artifactId.value == "model-edit-session").get
    val capturedsession = ModelEditSessionCodec.decodeCaptured(InternalModelVerifiedProjection(editentry.reference, editentry.path,
      editentry.required, editentry.dependencies, Files.readAllBytes(root.resolve("src/main/internal-model").resolve(editentry.path)).toVector)).toOption.get
    require(capturedsession == edited, "closed captured edit payload must preserve the exact actual shared candidate and trace history")
    val after = Files.readAllBytes(input).toVector
    val shared = view.actorGoals.actors.exists(_.reference == actor) && view.eventStorming.nodes.exists(_.identity == actor.semanticTargetId)
    val result = Preparation(root, original, after, basis, reread, edited,
      missing.findings.exists(_.kind == FindingKind.SliceWithoutTicket), !cleared.findings.exists(_.kind == FindingKind.SliceWithoutTicket),
      view, confirmation, rendering, reproduced, entriespresent, shared, phase10)
    writeConfirmation(result)
    result
  }

  def inspectThroughOperations(root: Path, session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    policy: FindingPolicy, resolutions: Vector[ResolutionEvidence], prepared: ModelEditPhase10SpecFixture.Prepared): Unit = {
    val grants = Vector("codex", "chatgpt").map(principal => ModelEditAuthorization.Binding(principal, true, Some(ModelEditSpecFixture.authorization(session))))
    val application = ModelEditApplication.createWithDiff(session, source, grants, policy, resolutions, Vector.empty, Vector.empty,
      prepared.mapping, prepared.diff).toOption.get
    Vector("codex", "chatgpt").foreach { principal =>
      val host = ModelEditExternalOperationsSpec.host(Some(application), principal)
      Vector("getModelEditView", "getModelEditDiff", "getModelEditTraces", "getModelEditReview").foreach { operation =>
        val response = ModelEditExternalOperationsSpec.invoke(operation,
          ModelEditOperationJson.selection(ModelEditOperationJson.Selection(session.reference, session.candidate.reference)), host)
        require(response.getString("status").contains("OK"), "ordinary authorized inspection must retain the exact admitted candidate")
        Files.writeString(root.resolve(principal + "-" + operation + ".json"), response.getString("resultJson").get)
      }
    }
  }

  /** Every added element/edge appears, rather than inheriting the helper's Goal-only diff. */
  def completeDiff(prepared: ModelEditPhase10SpecFixture.Prepared, basis: ModelEditSourceProjection.Publication,
    proposed: ModelEditSourceProjection.Publication): ModelEditPhase10SpecFixture.Prepared = {
    val target = prepared.diff.diff.targets.head
    val template = target.entries.head.entry
    val changed = proposed.document.elements.filterNot(value => basis.document.elements.contains(value)).map(value =>
      ("element", value.ref.identity, value.kind, value.asJson.spaces2)) ++ proposed.document.relationships.filterNot(value => basis.document.relationships.contains(value)).map(value =>
      ("relationship", value.ref.identity, value.kind, value.asJson.spaces2))
    val entries = changed.map { case (kind, identity, vocabulary, description) =>
      val mapping = prepared.candidate.projection.targets.head.mappings.find(value => value.semanticIdentityKind == kind && value.semanticIdentity == identity).get
      InternalModelSemanticDiffMappedEntry(template.copy(id = "phase12-diff-" + identity, category = vocabulary,
        subject = identity, before = None, after = Some(description), relationship = "Explicit accumulated candidate creation"), mapping.mappingId, kind)
    }.sortBy(_.entry.id)
    val diff = prepared.diff.copy(diff = prepared.diff.diff.copy(targets = Vector(target.copy(entries = entries))))
    val evidence = InternalModelVerifiedArtifactContext(specificationArtifact, "evidence/existing-specification.scala", true, Vector.empty, true)
    val context = prepared.candidate.packageContext.copy(artifacts = ordered(prepared.candidate.packageContext.artifacts :+ evidence))
    val candidate = prepared.candidate.copy(packageContext = context)
    prepared.copy(candidate = candidate, diff = diff.copy(candidateAdmission = candidate))
  }

  def retained(producerRoot: Path, root: Path): Preparation = {
    val basis = ModelEditSourceProjectionCodec.decodePublication(Files.readString(root.resolve("publication.json"))).toOption.get
    val edited = ModelEditSessionCodec.decode(Files.readAllBytes(root.resolve("src/main/internal-model/projections/edit.json")).toVector).toOption.get
    val request = io.circe.parser.parse(Files.readString(root.resolve("candidate-v2.json"))).toOption.get.as[ModelEditCmlProjection.Request].toOption.get
    val rendering = ModelEditSourceProjectionCodec.decodeRendering(Files.readString(root.resolve("render-candidate.json"))).toOption.get
    val classpath = Files.readString(producerRoot.resolve("target/cozy.d/runtime-classpath.txt")).trim
    val reproduced = ModelEditSourceProjectionCodec.decodePublication(runProducer(classpath, root, "reobserve-candidate", Vector("produce",
      root.resolve("candidate.cml").toString, request.source.owner, request.source.identity, request.source.revision))).toOption.get
    val bindings = sourceSelection(reproduced).bindings.map(value => edited.candidate.slices.find(_.identity == value.semanticIdentity)
      .fold(value)(slice => value.copy(recordReference = slice.reference)))
    val plan = ModelEditCmlProjection.prepare(sourceSelection(basis), edited, request.source, reproduced.document, bindings).toOption.get
    val confirmed = ModelEditCmlProjection.confirm(plan, rendering, reproduced).toOption.get
    val baseline = InternalModelCandidateCmlProjectionValidator.validate(root).toOption.get.targetBytes.head.baseline.rawBytes
    val expected = completeDiff(ModelEditPhase10SpecFixture.prepare(confirmed, edited, baseline).toOption.get, basis, reproduced)
    val captured = InternalModelSemanticDiffValidator.validate(root).toOption.get
    require(captured.diff == expected.diff.diff, "retained displayed diff must cover the complete actual accumulated source additions")
    val mappingentry = captured.candidateAdmission.packageContext.artifacts.find(_.reference.artifactId.value == "model-edit-source-mapping").get
    val mapping = ModelEditCmlProjection.decodeMapping(InternalModelVerifiedProjection(mappingentry.reference, mappingentry.path,
      mappingentry.required, mappingentry.dependencies, Files.readAllBytes(root.resolve("src/main/internal-model").resolve(mappingentry.path)).toVector)).toOption.get
    val phase10 = expected.copy(candidate = captured.candidateAdmission, diff = captured, mapping = mapping)
    val requestpaths = io.circe.parser.parse(Files.readString(root.resolve("review-request.json"))).toOption.get.hcursor.get[Vector[String]]("paths").toOption.get
    val confirmation = ModelEditConfirmation.prepare(edited, confirmed, phase10.mapping, phase10.diff, record("phase12-product-subject"),
      ModelEditSpecFixture.artifact("phase12-product-review", role = InternalModelArtifactRole.Validation), record("phase12-product-review"),
      requestpaths, Vector(specificationArtifact)).toOption.get
    val observations = io.circe.parser.parse(Files.readString(root.resolve("confirmation.json"))).toOption.get.hcursor
    val reread = ModelEditSourceProjectionCodec.decodePublication(Files.readString(root.resolve("read-basis.json"))).toOption.get
    val view = ModelEditViewProjection.projectConfirmed(edited, confirmed, Map.empty).toOption.get
    Preparation(root, baseline, Files.readAllBytes(root.resolve("models/order.cml")).toVector, basis, reread, edited,
      observations.get[Boolean]("missingTicketFindingObserved").toOption.get,
      observations.get[Boolean]("missingTicketFindingCleared").toOption.get, view, confirmation, rendering, reproduced,
      classpath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator), -1).forall(value => value.nonEmpty && Files.exists(Path.of(value))),
      view.actorGoals.actors.exists(_.reference == editSession(basis).candidate.actors.head) &&
        view.eventStorming.nodes.exists(_.identity == editSession(basis).candidate.actors.head.semanticTargetId), phase10)
  }

  def ordered(entries: Vector[InternalModelVerifiedArtifactContext]): Vector[InternalModelVerifiedArtifactContext] = {
    def _collect_(remaining: Vector[InternalModelVerifiedArtifactContext], accumulated: Vector[InternalModelVerifiedArtifactContext]): Vector[InternalModelVerifiedArtifactContext] =
      if (remaining.isEmpty) accumulated else {
        val next = remaining.filter(value => value.dependencies.forall(reference => accumulated.exists(_.reference == reference))).sortBy(_.reference.artifactId.value).head
        _collect_(remaining.filterNot(_.reference == next.reference), accumulated :+ next)
      }
    _collect_(entries, Vector.empty)
  }

  def writePackage(root: Path, prepared: ModelEditPhase10SpecFixture.Prepared, session: ModelEditSession, baseline: Vector[Byte]): Unit = {
    val candidate = prepared.candidate
    val context = candidate.packageContext
    val packagepath = root.resolve("src/main/internal-model")
    Files.createDirectories(packagepath)
    Files.writeString(root.resolve("project.yaml"), "project:\n  namespace: " + context.reference.projectNamespace.value + "\n  id: " + context.reference.projectId.value + "\n")
    val sourcetarget = candidate.targetBytes.head.baseline.source
    val cml = Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"),
      "source" -> Json.obj("authority" -> Json.fromString(sourcetarget.authority), "identity" -> Json.fromString(sourcetarget.identity),
        "locator" -> sourcetarget.locator.asJson, "revision" -> sourcetarget.revision.asJson),
      "basis" -> Json.obj("byteLength" -> Json.fromLong(baseline.size.toLong), "projectRelativePath" -> Json.fromString("models/order.cml"),
        "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(baseline.toArray))))
    val payloads = Map("sources/model-context.json" -> prepared.canonical.snapshot, "sources/cml.json" -> bytes(cml),
      "realization.json" -> InternalModelSemanticRealizationEncoder.encode(candidate.continuity.realization).toVector,
      "projections/continuity.json" -> InternalModelProjectionBindingCodec.encode(candidate.continuity.binding).toVector,
      "projections/candidate.json" -> InternalModelCandidateCmlProjectionCodec.encode(candidate.projection).toVector,
      "projections/diff.json" -> InternalModelSemanticDiffCodec.encode(prepared.diff.diff).toVector,
      "projections/edit.json" -> ModelEditSessionCodec.encode(session), "projections/source-mapping.json" -> ModelEditCmlProjection.encodeMapping(prepared.mapping),
      "evidence/existing-specification.scala" -> Files.readAllBytes(specificationPath).toVector)
    context.artifacts.foreach { entry =>
      val path = packagepath.resolve(entry.path)
      Files.createDirectories(path.getParent)
      Files.write(path, payloads(entry.path).toArray)
    }
    val inventory = context.artifacts.map(value => referenceJson(value.reference).mapObject(_.add("path", Json.fromString(value.path))
      .add("required", Json.fromBoolean(value.required)).add("dependsOn", Json.fromValues(value.dependencies.map(referenceJson)))))
    val manifest = Json.obj("schemaVersion" -> Json.fromString(context.schemaversion), "packageId" -> Json.fromString(context.reference.packageId.value),
      "projectNamespace" -> Json.fromString(context.reference.projectNamespace.value), "projectId" -> Json.fromString(context.reference.projectId.value),
      "revision" -> Json.fromLong(context.revision), "lifecycleState" -> Json.fromString(context.lifecyclestate), "artifacts" -> Json.fromValues(inventory))
    Files.write(packagepath.resolve("manifest.yaml"), bytes(manifest).toArray)
  }

  def writeConfirmation(value: Preparation): Unit = {
    val prepared = value.confirmation
    val subject = prepared.subject
    val result = Json.obj("state" -> Json.fromString("PreparedAwaitingActualProductReviewAndHumanInput"), "projectRoot" -> Json.fromString(value.root.toString),
      "canonicalTarget" -> Json.fromString(value.root.resolve("models/order.cml").toString), "candidateSource" -> Json.fromString(value.root.resolve("candidate.cml").toString),
      "session" -> recordJson(value.session.reference), "candidate" -> recordJson(value.session.candidate.reference),
      "subject" -> InternalModelCandidateReviewBindingCodec.encodeSubject(subject), "reviewArtifact" -> referenceJson(prepared.reviewArtifact),
      "candidateArtifact" -> referenceJson(prepared.diff.diff.candidateArtifactReference),
      "candidateModelIdentity" -> Json.fromString(prepared.diff.diff.candidateModelIdentity),
      "semanticDiffArtifact" -> referenceJson(prepared.diff.diffArtifactReference),
      "realizationArtifact" -> referenceJson(prepared.diff.candidateAdmission.projection.realizationArtifactReference),
      "realizationReference" -> recordJson(prepared.diff.candidateAdmission.continuity.realization.realizationReference),
      "continuityArtifact" -> referenceJson(prepared.diff.candidateAdmission.projection.continuityArtifactReference),
      "reviewReference" -> recordJson(prepared.reviewReference), "semanticDiff" -> recordJson(prepared.diff.diff.semanticDiffReference),
      "applicationTargets" -> Json.fromValues(prepared.diff.candidateAdmission.projection.targets.map(target => Json.obj(
        "targetId" -> Json.fromString(target.targetId), "projectRelativePath" -> Json.fromString(target.projectRelativePath),
        "sourceAuthority" -> Json.fromString(target.source.authority), "sourceIdentity" -> Json.fromString(target.source.identity),
        "nextSourceRevision" -> Json.fromString(prepared.confirmed.plan.request.source.revision)))),
      "noCanonicalWrite" -> Json.fromBoolean(value.afterPreparation == value.original), "producer" -> value.basis.producer.asJson,
      "missingTicketFindingObserved" -> Json.fromBoolean(value.missingTicketObserved),
      "missingTicketFindingCleared" -> Json.fromBoolean(value.missingTicketCleared),
      "unavailablePhase11" -> Json.fromString("No positive independent Phase11 demand projection was selected; shared View retains Unavailable."),
      "externalReferenceBasis" -> Json.fromString("Explicit source alias example/order maps to asami/textus-cbd-support / textus-cbd-support. Existing ModelEditServiceSpec source is captured at caller-declared revision stage12-07-spec-source-1. Ticket is provisional sample only; no live Ticket existence is claimed."),
      "pending" -> Json.arr(Json.fromString("Actual independent product review of complete subject and effect assessments"),
        Json.fromString("Independently attributable actual-human approval of the exact candidate/dependency/review revision"),
        Json.fromString("Approved Phase10 canonical application, actual per-target Cozy lint and refreshed source-backed post-validation")))
    Files.write(value.root.resolve("confirmation.json"), bytes(result).toArray)
  }
  /** Caller-owned material only; these files contain existing closed Phase10 records. */
  def suppliedInputs(prepared: Preparation): Either[String, ModelEditPromotion.Inputs] = {
    val root = prepared.root.resolve("admitted-inputs")
    val required = Vector("review.json", "execution-basis.json", "approval.json", "human-input.json", "mutation-authority.json")
    val missing = required.filterNot(name => Files.isRegularFile(root.resolve(name)))
    if (missing.nonEmpty) Left("Retained concrete target " + prepared.root + "; missing independently supplied inputs: " + missing.mkString(", "))
    else {
      val review = InternalModelCandidateReviewBindingCodec.decode(Files.readAllBytes(root.resolve("review.json")).toVector)
      val approval = InternalModelCandidateHumanApprovalCodec.decode(Files.readAllBytes(root.resolve("approval.json")).toVector)
      val human = InternalModelCandidateHumanApprovalCodec.decode(Files.readAllBytes(root.resolve("human-input.json")).toVector)
      for {
        admittedreview <- review
        admittedapproval <- approval
        admittedhuman <- human
        _ <- Either.cond(admittedapproval.approval == admittedhuman.approval, (), "recorded approval must equal separately supplied actual Human input")
        basisjson <- io.circe.parser.parse(Files.readString(root.resolve("execution-basis.json"))).left.map(_.message)
        rules <- basisjson.hcursor.get[Vector[Json]]("rules").left.map(_.message)
        providers <- basisjson.hcursor.get[Vector[Json]]("providers").left.map(_.message)
        selectedrules <- rules.foldLeft[Either[String, Vector[InternalModelCandidateReviewRule]]](Right(Vector.empty)) { (result, value) => for {
          values <- result
          id <- value.hcursor.get[String]("ruleId").left.map(_.message)
          version <- value.hcursor.get[String]("ruleVersion").left.map(_.message)
        } yield values :+ InternalModelCandidateReviewRule(id, version) }
        selectedproviders <- providers.foldLeft[Either[String, Vector[InternalModelCandidateReviewProvider]]](Right(Vector.empty)) { (result, value) => for {
          values <- result
          id <- value.hcursor.get[String]("providerId").left.map(_.message)
          version <- value.hcursor.get[String]("providerVersion").left.map(_.message)
        } yield values :+ InternalModelCandidateReviewProvider(id, version) }
        basis = InternalModelCandidateReviewExecutionBasis(selectedrules, selectedproviders)
        _ <- InternalModelCandidateReviewBindingCodec.validateExecutionBasis(basis)
        authorityjson <- io.circe.parser.parse(Files.readString(root.resolve("mutation-authority.json"))).left.map(_.message)
        authority <- decodeAuthority(authorityjson)
        _ <- Either.cond(admittedreview.subject == prepared.confirmation.subject && admittedreview.reviewReference == prepared.confirmation.reviewReference,
          (), "independent review must select the retained complete product subject/revision")
      } yield {
        val approvalartifact = ModelEditSpecFixture.artifact("phase12-actual-human-approval", role = InternalModelArtifactRole.Approval)
        installApprovalMaterial(prepared, admittedreview, admittedapproval, approvalartifact)
        val facts = io.circe.parser.parse(new String(prepared.phase10.canonical.snapshot.toArray, StandardCharsets.UTF_8)).toOption.get.hcursor.downField("source")
        val source = prepared.phase10.candidate.targetBytes.head.baseline.source
        val live = Map(ModelEditSpecFixture.artifact("model-edit-facts") -> InternalModelPackageFreshnessInput.SourceObservation(
          InternalModelLiveSourceObservation.Observed(facts.get[String]("authority").toOption.get, facts.get[String]("identity").toOption.get,
            facts.get[Option[String]]("revision").toOption.get, prepared.phase10.canonical.snapshot, None)),
          snapshot -> InternalModelPackageFreshnessInput.CmlObserved(source.authority, source.identity, source.revision, "models/order.cml"))
        ModelEditPromotion.Inputs(basis, admittedhuman.approval, approvalartifact, authority, live)
      }
    }
  }

  def decodeAuthority(json: Json): Either[String, InternalModelCmlMutationAuthority] = {
    val cursor = json.hcursor
    for {
      requestjson <- cursor.get[Json]("requestReference").left.map(_.message)
      request <- InternalModelTypedControlCodec.decodeRecordReference(bytes(requestjson))
      root <- cursor.get[String]("projectRoot").left.map(_.message)
      packagejson <- cursor.get[Json]("packageReference").left.map(_.message)
      packageref <- InternalModelTypedControlCodec.decodePackageReference(bytes(packagejson))
      component <- cursor.downField("scope").get[String]("componentIdentity").left.map(_.message)
      context <- cursor.downField("scope").get[String]("projectionContextIdentity").left.map(_.message)
      usecase <- cursor.downField("scope").get[String]("selectedUseCaseElementIdentity").left.map(_.message)
      owner <- cursor.downField("provenance").get[String]("authority").left.map(_.message)
      identity <- cursor.downField("provenance").get[String]("identity").left.map(_.message)
      locator <- cursor.downField("provenance").get[Option[String]]("locator").left.map(_.message)
      revision <- cursor.downField("provenance").get[Option[String]]("revision").left.map(_.message)
      targets <- cursor.get[Vector[Json]]("targets").left.map(_.message)
      selected <- targets.foldLeft[Either[String, Vector[InternalModelCmlMutationTarget]]](Right(Vector.empty)) { (result, target) => for {
        values <- result
        id <- target.hcursor.get[String]("targetId").left.map(_.message)
        path <- target.hcursor.get[String]("projectRelativePath").left.map(_.message)
        authority <- target.hcursor.get[String]("sourceAuthority").left.map(_.message)
        source <- target.hcursor.get[String]("sourceIdentity").left.map(_.message)
        next <- target.hcursor.get[String]("nextSourceRevision").left.map(_.message)
      } yield values :+ InternalModelCmlMutationTarget(id, path, authority, source, next) }
    } yield InternalModelCmlMutationAuthority(request, root, packageref, InternalModelSemanticScope(component, context, usecase),
      InternalModelSemanticSource(owner, identity, locator, revision), selected)
  }

  def installApprovalMaterial(prepared: Preparation, review: InternalModelCandidateReviewBinding,
    approval: InternalModelCandidateHumanApproval, approvalArtifact: InternalModelArtifactReference): Unit = {
    val packagepath = prepared.root.resolve("src/main/internal-model")
    val confirmation = prepared.confirmation
    val reviewentry = InternalModelVerifiedArtifactContext(confirmation.reviewArtifact, "validation/product-review.json", true, review.subject.artifacts, true)
    val approvalentry = InternalModelVerifiedArtifactContext(approvalArtifact, "approval/human.json", true, Vector(confirmation.reviewArtifact), true)
    val resumeref = ModelEditSpecFixture.artifact("phase12-resume", role = InternalModelArtifactRole.Resume)
    val entries = ordered(prepared.phase10.candidate.packageContext.artifacts.filterNot(entry => Set(reviewentry.reference.artifactId, approvalentry.reference.artifactId, resumeref.artifactId).contains(entry.reference.artifactId)) ++ Vector(reviewentry, approvalentry,
      InternalModelVerifiedArtifactContext(resumeref, "resume.yaml", true, Vector.empty, true)))
    Vector((reviewentry.path, InternalModelCandidateReviewBindingCodec.encode(review).toVector),
      (approvalentry.path, InternalModelCandidateHumanApprovalCodec.encode(approval).toVector)).foreach { case (relative, content) =>
      val path = packagepath.resolve(relative)
      Files.createDirectories(path.getParent)
      Files.write(path, content.toArray)
    }
    val selected = entries.map(_.reference).filterNot(_.role == InternalModelArtifactRole.Resume).sortBy(_.artifactId.value)
    val context = prepared.phase10.candidate.packageContext
    val cursor = InternalModelResumeCursor("2.0", "ccdm-resume-v2", context.reference.packageId, context.revision,
      selected, "human-decision-recorded", Some("request-human-decision"), Some("handoff-approved-candidate"), Vector.empty, Vector.empty, Vector.empty, Vector.empty)
    Files.write(packagepath.resolve("resume.yaml"), InternalModelResumeCursorCodec.encode(cursor).toArray)
    val manifestpath = packagepath.resolve("manifest.yaml")
    val manifest = io.circe.parser.parse(Files.readString(manifestpath)).toOption.get
    val inventory = entries.map(value => referenceJson(value.reference).mapObject(_.add("path", Json.fromString(value.path))
      .add("required", Json.fromBoolean(value.required)).add("dependsOn", Json.fromValues(value.dependencies.map(referenceJson)))))
    Files.write(manifestpath, bytes(manifest.mapObject(_.add("artifacts", Json.fromValues(inventory)))).toArray)
  }

  def reprojectApplied(producerRoot: Path, prepared: Preparation,
    application: InternalModelCmlChangeApplication.ApplicationReport): ModelEditSourceProjection.Publication = {
    require(application.disposition == InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation, "actual approved application must complete before re-projection")
    val classpath = Files.readString(producerRoot.resolve("target/cozy.d/runtime-classpath.txt")).trim
    val source = prepared.confirmation.confirmed.plan.request.source
    ModelEditSourceProjectionCodec.decodePublication(runProducer(classpath, prepared.root, "reproject-applied", Vector("produce",
      prepared.root.resolve("models/order.cml").toString, source.owner, source.identity, source.revision))).toOption.get
  }

  def bytes(value: Json): Vector[Byte] = (value.noSpaces + "\n").getBytes(StandardCharsets.UTF_8).toVector
  def referenceJson(value: InternalModelArtifactReference): Json = io.circe.parser.parse(new String(InternalModelTypedControlCodec.encodeArtifactReference(value).toArray, StandardCharsets.UTF_8)).toOption.get
  def recordJson(value: InternalModelRecordReference): Json = ModelEditOperationJson.record(value)
  def runProducer(classpath: String, root: Path, label: String, arguments: Vector[String]): String = {
    val output = root.resolve(label + ".json")
    val errors = root.resolve(label + ".stderr")
    val command = Vector(Path.of(sys.props("java.home"), "bin", "java").toString, "-Xmx1024m", "-cp", classpath, "cozy.modeler.CmlModelEditCli") ++ arguments
    val process = new ProcessBuilder(command*).directory(root.toFile).redirectOutput(output.toFile).redirectError(errors.toFile).start()
    try {
      require(process.waitFor(120, TimeUnit.SECONDS), "actual model-edit producer timed out: " + root)
      require(process.exitValue() == 0, "actual model-edit producer failed: " + errors)
      Files.readString(output)
    } finally if (process.isAlive) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); () }
  }
}

/** The actual application stays in this JVM until existing post-validation finishes. */
private[runtime] object ModelEditAppliedIntegration {
  import ModelEditIntegrationFixture.*
  import ModelEditProjectionSpecFixture.record
  import InternalModelCmlChangePostValidation.*

  final case class Validated(publication: ModelEditSourceProjection.Publication, report: Report, reviewedProject: Path)

  def validate(producerRoot: Path, executable: Path, prepared: Preparation,
    application: InternalModelCmlChangeApplication.ApplicationReport): Validated = {
    require(application.disposition == InternalModelCmlChangeApplication.Disposition.AppliedPendingValidation,
      "only the actual complete application can enter post-validation")
    val commands = application.gate.plan.get.targets.map { target =>
      val argv = Vector("--runtime", "0.3.3-SNAPSHOT", "lint", "cml",
        prepared.root.resolve(target.target.projectRelativePath).normalize.toString, "--format", "json")
      val label = "post-lint-" + target.target.targetId
      val output = prepared.root.resolve(label + ".json")
      val errors = prepared.root.resolve(label + ".stderr")
      val outcome = try {
        val process = new ProcessBuilder((Vector(executable.toString) ++ argv)*).directory(prepared.root.toFile)
          .redirectOutput(output.toFile).redirectError(errors.toFile).start()
        try {
          if (process.waitFor(120, TimeUnit.SECONDS)) CommandOutcome.Terminal(process.exitValue(), Files.readString(output), Files.readString(errors))
          else CommandOutcome.Indeterminate("actual Cozy lint exceeded its 120 second execution bound")
        } finally if (process.isAlive) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); () }
      } catch {
        case NonFatal(error) => CommandOutcome.Missing("actual Cozy lint observation unavailable: " + Option(error.getMessage).getOrElse(error.getClass.getSimpleName))
      }
      CommandObservation(record("phase12-" + label), application.applicationreference, prepared.root.toString,
        target.target.targetId, target.target.projectRelativePath, target.target.source.authority,
        target.target.source.identity, target.nextsourcerevision, "0.3.3-SNAPSHOT", argv, outcome)
    }
    val publication = reprojectApplied(producerRoot, prepared, application)
    val (owner, reviewed) = refreshOwner(prepared, application, publication)
    val result = ModelEditPromotion.validateApplied(prepared.root, application, record("phase12-post-validation"), commands, Some(owner))
    val report = result.toOption.getOrElse(throw new IllegalStateException(result.show))
    val observation = Json.obj("state" -> Json.fromString(report.disposition.toString),
      "applicationReference" -> recordJson(application.applicationreference),
      "postValidationReference" -> recordJson(report.postvalidationreference),
      "reviewedProject" -> Json.fromString(reviewed.toString),
      "currentPackageRevision" -> Json.fromLong(2),
      "commands" -> Json.fromValues(commands.map(command => Json.obj("reference" -> recordJson(command.commandreference),
        "targetId" -> Json.fromString(command.targetid), "argv" -> Json.fromValues(command.argv.map(Json.fromString)),
        "outcome" -> (command.outcome match {
          case CommandOutcome.Terminal(exitcode, stdout, stderr) => Json.obj("exitCode" -> Json.fromInt(exitcode), "stdout" -> Json.fromString(stdout), "stderr" -> Json.fromString(stderr))
          case CommandOutcome.Indeterminate(reason) => Json.obj("indeterminate" -> Json.fromString(reason))
          case CommandOutcome.Missing(reason) => Json.obj("missing" -> Json.fromString(reason))
        })))),
      "problems" -> Json.fromValues(report.problems.map(problem => Json.obj("kind" -> Json.fromString(problem.kind.toString),
        "dimension" -> Json.fromString(problem.dimension), "diagnostic" -> Json.fromString(problem.diagnostic)))),
      "permission" -> Json.fromString("Observation only; this summary cannot reconstruct or authorize an application"))
    Files.write(prepared.root.resolve("post-validation.json"), bytes(observation).toArray)
    Validated(publication, report, reviewed)
  }

  /** Rebuild canonical owners from actual applied-source production, preserving explicit witness identities. */
  def refreshOwner(prepared: Preparation, application: InternalModelCmlChangeApplication.ApplicationReport,
    publication: ModelEditSourceProjection.Publication): (OwnerEvidence, Path) = {
    import ModelEditSourceProjection.*
    val plan = application.gate.plan.get
    val selected = sourceSelection(publication)
    val original = prepared.phase10.candidate.continuity.realization
    val endpoints = publication.document.relationships.flatMap(value => Vector(value.from, value.to)).filterNot(_.isInstanceOf[Endpoint.Local]).distinct
    def _anchor_(endpoint: Endpoint): String = endpoint match {
      case Endpoint.External(value) => value.identity
      case Endpoint.SpecificationTarget(value) => value.identity
      case Endpoint.TicketTarget(value) => value.externalIdentity
      case Endpoint.Local(value) => value.identity
    }
    val witnesses = original.sourceReferences.map { reference =>
      val target = reference.target.get
      val local = selected.bindings.find(_.semanticIdentity.value == target.semanticIdentity).map(_.sourceRef)
      val external = if (local.nonEmpty) None else {
        val matching = endpoints.filter(value => _anchor_(value) == reference.sourceAnchor)
        require(matching.size == 1, "refreshed external endpoint must retain the exact source-owned witness")
        matching.headOption
      }
      val assertion = original.canonicalAssertions.filter(value => value.sourceReferenceId == reference.referenceId)
      require(assertion.size == 1, "this source projection has one explicit canonical assertion per witness")
      CanonicalWitness(target, local, external, assertion.head.assertionId, reference.referenceId, reference.sourceAnchor)
    }
    val factsref = ModelEditSpecFixture.artifact("model-edit-facts", 2)
    val cmlref = ModelEditSpecFixture.artifact("model-edit-source", 2)
    val realizationref = ModelEditSpecFixture.artifact("model-edit-realization", 2, InternalModelArtifactRole.Realization)
    val continuityref = ModelEditSpecFixture.artifact("model-edit-continuity", 2, InternalModelArtifactRole.Projection)
    val realizationrecord = record("canonical-realization", 2)
    val continuityrecord = record("canonical-continuity", 2)
    val canonical = projectCanonical(selected, plan.scope, factsref, realizationrecord, witnesses).toOption.get
    val binding = prepared.phase10.candidate.continuity.binding.copy(bindingReference = continuityrecord,
      realizationArtifactReference = realizationref)
    require(plan.targets.size == 1, "the declared representative source selection contains one canonical CML target")
    val target = plan.targets.head
    val currentsource = target.target.source.copy(revision = Some(target.nextsourcerevision))
    val currentbytes = Files.readAllBytes(prepared.root.resolve(target.target.projectRelativePath)).toVector
    val cml = Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"),
      "source" -> Json.obj("authority" -> Json.fromString(currentsource.authority), "identity" -> Json.fromString(currentsource.identity),
        "locator" -> currentsource.locator.asJson, "revision" -> currentsource.revision.asJson),
      "basis" -> Json.obj("projectRelativePath" -> Json.fromString(target.target.projectRelativePath),
        "byteLength" -> Json.fromLong(currentbytes.size.toLong), "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(currentbytes.toArray))))
    val packagepath = prepared.root.resolve("src/main/internal-model")
    val reviewed = prepared.root.resolve("review-materials/approved-candidate")
    val retainedpackage = reviewed.resolve("src/main/internal-model")
    Files.createDirectories(retainedpackage.getParent)
    Files.move(packagepath, retainedpackage)
    Files.copy(prepared.root.resolve("project.yaml"), reviewed.resolve("project.yaml"))
    val priorcml = reviewed.resolve(target.target.projectRelativePath)
    Files.createDirectories(priorcml.getParent)
    Files.write(priorcml, prepared.original.toArray)
    val entries = ordered(Vector(
      InternalModelVerifiedArtifactContext(factsref, "sources/model-context.json", true, Vector.empty, true),
      InternalModelVerifiedArtifactContext(cmlref, "sources/cml.json", true, Vector.empty, true),
      InternalModelVerifiedArtifactContext(realizationref, "realization.json", true, Vector(factsref), true),
      InternalModelVerifiedArtifactContext(continuityref, "projections/continuity.json", true, Vector(realizationref), true)))
    val payloads = Map("sources/model-context.json" -> canonical.snapshot, "sources/cml.json" -> bytes(cml),
      "realization.json" -> InternalModelSemanticRealizationEncoder.encode(canonical.realization).toVector,
      "projections/continuity.json" -> InternalModelProjectionBindingCodec.encode(binding).toVector)
    entries.foreach { entry =>
      val path = packagepath.resolve(entry.path)
      Files.createDirectories(path.getParent)
      Files.write(path, payloads(entry.path).toArray)
    }
    val inventory = entries.map(entry => referenceJson(entry.reference).mapObject(_.add("path", Json.fromString(entry.path))
      .add("required", Json.fromBoolean(entry.required)).add("dependsOn", Json.fromValues(entry.dependencies.map(referenceJson)))))
    val context = prepared.phase10.candidate.packageContext
    val manifest = Json.obj("schemaVersion" -> Json.fromString(context.schemaversion), "packageId" -> Json.fromString(context.reference.packageId.value),
      "projectNamespace" -> Json.fromString(context.reference.projectNamespace.value), "projectId" -> Json.fromString(context.reference.projectId.value),
      "revision" -> Json.fromLong(2), "lifecycleState" -> Json.fromString("reprojected-pending-acceptance"), "artifacts" -> Json.fromValues(inventory))
    Files.write(packagepath.resolve("manifest.yaml"), bytes(manifest).toArray)
    val actualsource = canonical.realization.sourceReferences.head.source
    val live = Map(factsref -> InternalModelPackageFreshnessInput.SourceObservation(InternalModelLiveSourceObservation.Observed(
      actualsource.authority, actualsource.identity, actualsource.revision, canonical.snapshot, None)),
      cmlref -> InternalModelPackageFreshnessInput.CmlObserved(currentsource.authority, currentsource.identity,
        currentsource.revision, target.target.projectRelativePath))
    (OwnerEvidence(plan.packagereference, plan.scope, realizationref, realizationrecord, continuityref, continuityrecord,
      Vector(TargetBaseline(target.target.targetId, cmlref)), live), reviewed)
  }
}
