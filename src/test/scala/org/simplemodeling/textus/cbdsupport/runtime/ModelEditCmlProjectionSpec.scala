package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditCmlProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  "Source-owned candidate planning" should {
    "connect the five creation commands to an explicit candidate-v2 document without canonical writing" in {
      Given("one source basis and an immutable candidate containing all independently linked development records")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("commands are applied and the proposed source document is planned")
      val edited = editedSession(basis, proposed)
      val plan = ModelEditCmlProjection.prepare(selection(basis), edited, successor, proposed.document, selection(proposed).bindings)
      val encoded = plan.map(ModelEditCmlProjection.encodeRequest)
      Then("the request contains explicit creation and every relationship, with no implicit replacement")
      edited.history.size shouldBe 5
      plan.toOption.get.request.addedElements.map(_.kind).toSet shouldBe Set("goal", "slice")
      plan.toOption.get.request.addedRelationships.map(_.kind).toSet shouldBe Set("actor-goal", "goal-use-case", "use-case-slice", "slice-specification", "slice-ticket")
      plan.toOption.get.request.elements shouldBe empty
      encoded.toOption.get should include("cozy.cml.model-edit-candidate.v2")
      basis.document.elements.size shouldBe 11
    }
    "reject an unrelated candidate, unavailable scope and stale producer re-production" in {
      Given("a complete candidate proposal and a captured canonical source")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("the exact proposal is prepared and then three contradictory observations are supplied")
      val edited = editedSession(basis, proposed)
      val planned = ModelEditCmlProjection.prepare(selection(basis), edited, successor, proposed.document, selection(proposed).bindings)
      val wrong = ModelEditCmlProjection.prepare(selection(basis), edited.copy(candidate = edited.candidate.copy(goalLinks = Vector.empty)),
        successor, proposed.document, selection(proposed).bindings)
      val unavailable = selection(basis.copy(document = basis.document.copy(coverage = basis.document.coverage.map(value =>
        if (value.family == "goal") value.copy(status = "unavailable", detail = Some("not available")) else value))))
      val blocked = ModelEditCmlProjection.prepare(unavailable, edited, successor, proposed.document, selection(proposed).bindings)
      val stale = planned.flatMap(plan => ModelEditCmlProjection.confirm(plan,
        ModelEditSourceProjection.Rendering(plan.request.schemaVersion, source, producer, successor, "proposed CML", Vector.empty), basis))
      Then("none can become a source-owned promotion mapping")
      planned.isRight shouldBe true
      wrong.isLeft shouldBe true
      blocked.isLeft shouldBe true
      stale.isLeft shouldBe true
    }
    "admit source-owned canonical facts, candidate mappings and semantic diff through the existing Phase 10 owners" in {
      Given("an explicit source proposal and caller-owned canonical assertion/reference identifiers")
      val basis = basisPublication()
      val proposed = proposedPublication()
      When("the candidate is confirmed and passed through normal captured canonical/diff admission")
      val edited = editedSession(basis, proposed)
      val plan = ModelEditCmlProjection.prepare(selection(basis), edited, successor, proposed.document, selection(proposed).bindings).toOption.get
      val rendering = ModelEditSourceProjection.Rendering(plan.request.schemaVersion, source, producer, successor, "fixture proposed CML", Vector.empty)
      val confirmed = ModelEditCmlProjection.confirm(plan, rendering, proposed).toOption.get
      val prepared = ModelEditPhase10SpecFixture.prepare(confirmed, edited, "fixture canonical CML".getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector)
      val stale = prepared.flatMap(value => ModelEditCmlProjection.validateMapping(value.mapping,
        edited.copy(candidate = edited.candidate.copy(reference = record("candidate", 99))), value.candidate))
      Then("the existing validators consume exact source mappings and complete subjects include the edit and mapping payloads")
      withClue(prepared.left.toOption) { prepared.isRight shouldBe true }
      prepared.toOption.get.canonical.realization.elements.map(_.kind) should contain("slice")
      prepared.toOption.get.diff.diff.targets.head.entries.head.entry.subject shouldBe "repeat-purchase"
      prepared.toOption.get.subject.map(_.artifactId.value) should contain allOf ("model-edit-source-mapping", "model-edit-session", "model-edit-facts", "model-edit-source")
      stale.isLeft shouldBe true
    }

    "require the declared successor source revision for every proposed creation" in {
      forAll(Gen.alphaNumStr.suchThat(value => value.nonEmpty && value != "r1")) { revision =>
        Given("a caller-selected successor revision")
        val basis = basisPublication()
        val original = proposedPublication()
        val successorvalue = successor.copy(revision = revision)
        val proposed = original.copy(source = successorvalue, document = original.document.copy(
          elements = original.document.elements.map(value => if (value.source == successor) value.copy(source = successorvalue) else value),
          relationships = original.document.relationships.map(value => if (value.source == successor) value.copy(source = successorvalue) else value)))
        When("the candidate is prepared for that explicit source revision")
        val edited = editedSession(basis, proposed)
        val plan = ModelEditCmlProjection.prepare(selection(basis), edited, successorvalue, proposed.document, selection(proposed).bindings)
        Then("the caller-selected revision is retained rather than computed from content")
        plan.toOption.get.request.source.revision shouldBe revision
      }
    }
  }
}

/** Caller-owned IDs are fixture data; production adapters require these inputs explicitly. */
private[runtime] object ModelEditPhase10SpecFixture {
  import ModelEditProjectionSpecFixture.*
  import ModelEditSourceProjection.*
  import java.nio.charset.StandardCharsets
  import java.util.Base64
  import io.circe.Json

  final case class Prepared(candidate: InternalModelCandidateCmlAdmission, mapping: ModelEditCmlProjection.Mapping,
    diff: InternalModelSemanticDiffAdmission, subject: Vector[InternalModelArtifactReference], canonical: CanonicalProduction)

  def prepare(confirmed: ModelEditCmlProjection.Confirmed, session: ModelEditSession,
    baseline: Vector[Byte]): Either[String, Prepared] = {
    val publication = confirmed.reproduced
    val selected = selection(publication)
    val semanticscope = InternalModelSemanticScope(scope.component.value, scope.context.value, "place-order")
    val factsref = ModelEditSpecFixture.artifact("model-edit-facts")
    val realizationref = ModelEditSpecFixture.artifact("model-edit-realization", role = InternalModelArtifactRole.Realization)
    val continuityref = ModelEditSpecFixture.artifact("model-edit-continuity", role = InternalModelArtifactRole.Projection)
    val candidateref = ModelEditSpecFixture.artifact("model-edit-candidate-cml", role = InternalModelArtifactRole.Projection)
    val diffref = ModelEditSpecFixture.artifact("model-edit-diff", role = InternalModelArtifactRole.Projection)
    val editref = ModelEditSpecFixture.artifact("model-edit-session", role = InternalModelArtifactRole.Projection)
    val mappingref = ModelEditSpecFixture.artifact("model-edit-source-mapping", role = InternalModelArtifactRole.Projection)
    val reviewref = ModelEditSpecFixture.artifact("model-edit-review", role = InternalModelArtifactRole.Validation)
    val witnessrecords = publication.document.elements.map(value => (value.ref, "element")) ++ publication.document.relationships.map(value => (value.ref, "relationship"))
    val localwitnesses = witnessrecords.map { case (reference, kind) => CanonicalWitness(InternalModelSemanticTarget(kind, reference.identity),
      Some(reference), None, "fixture-assertion-" + reference.identity, "fixture-reference-" + reference.identity, reference.identity) }
    val externalwitnesses = publication.document.relationships.flatMap(value => Vector(value.from, value.to)).distinct.collect {
      case endpoint @ Endpoint.External(reference) => CanonicalWitness(InternalModelSemanticTarget("element", "external-warehouse"), None, Some(endpoint), "fixture-assertion-warehouse", "fixture-reference-warehouse", reference.identity)
      case endpoint @ Endpoint.SpecificationTarget(reference) => CanonicalWitness(InternalModelSemanticTarget("element", "external-specification"), None, Some(endpoint), "fixture-assertion-specification", "fixture-reference-specification", reference.identity)
      case endpoint @ Endpoint.TicketTarget(reference) => CanonicalWitness(InternalModelSemanticTarget("element", "external-ticket"), None, Some(endpoint), "fixture-assertion-ticket", "fixture-reference-ticket", reference.externalIdentity)
    }
    val source = InternalModelSemanticSource(confirmed.plan.selection.publication.source.owner,
      confirmed.plan.selection.publication.source.identity, Some(confirmed.plan.selection.publication.source.identity), Some(confirmed.plan.selection.publication.source.revision))
    val artifactentry = (reference: InternalModelArtifactReference, path: String, dependencies: Vector[InternalModelArtifactReference]) =>
      InternalModelVerifiedArtifactContext(reference, path, true, dependencies.sortBy(_.artifactId.value), true)
    val canonical = ModelEditSourceProjection.projectCanonical(selected, semanticscope, factsref, record("canonical-realization"), localwitnesses ++ externalwitnesses)
    canonical.flatMap { canonical =>
      val families = Vector("MonoKotoProjection", "UseCaseCommunicationProjection", "EntityModelProjection", "EventModelProjection", "StructureViewProjection", "ClassificationViewProjection", "WorkflowProjection", "StateMachineProjection")
      val usecase = canonical.realization.elements.find(_.identity == "place-order").get
      val binding = InternalModelProjectionBinding(record("canonical-continuity"), "ccdm-projection-binding-v3", "3.0", realizationref,
        InternalModelProjectionBindingScope(semanticscope.componentIdentity, semanticscope.projectionContextIdentity, semanticscope.selectedUseCaseElementIdentity),
        families.map(family => InternalModelProjectionBindingView(family, if (family == "UseCaseCommunicationProjection") Vector(
          InternalModelProjectionBindingRecord("element", usecase.identity, "use-case", usecase.canonicalAssertionIds, Vector.empty, Vector.empty, None, None)) else Vector.empty)))
      val baselinebytes = Json.obj("schemaVersion" -> Json.fromString("2.0"), "snapshotKind" -> Json.fromString("cml-baseline"),
        "source" -> Json.obj("authority" -> Json.fromString(source.authority), "identity" -> Json.fromString(source.identity),
          "locator" -> Json.fromString(source.locator.get), "revision" -> Json.fromString(source.revision.get)),
        "basis" -> Json.obj("byteLength" -> Json.fromLong(baseline.size.toLong), "projectRelativePath" -> Json.fromString("models/order.cml"),
          "rawBytesBase64" -> Json.fromString(Base64.getEncoder.encodeToString(baseline.toArray)))).noSpaces.getBytes(StandardCharsets.UTF_8).toVector
      val mappings = witnessrecords.map { case (reference, kind) =>
        val assertion = canonical.realization.canonicalAssertions.find(value => value.semanticIdentityKind == kind && value.semanticIdentity == reference.identity).get
        InternalModelCandidateCmlMapping(Vector(assertion.assertionId), reference.identity, reference.identity,
          Vector.empty, Vector.empty, "fixture-mapping-" + reference.identity, reference.identity, kind)
      }.sortBy(_.mappingId)
      val effects = Vector(InternalModelCandidateCmlEffect("unknown", "Caller declares compatibility assessment pending", "fixture-compatibility", "compatibility", mappings.map(_.mappingId), canonical.realization.sourceReferences.head.referenceId),
        InternalModelCandidateCmlEffect("unknown", "Caller declares migration assessment pending", "fixture-migration", "migration", mappings.map(_.mappingId), canonical.realization.sourceReferences.head.referenceId))
      val proposedbytes = confirmed.rendering.text.getBytes(StandardCharsets.UTF_8).toVector
      val target = InternalModelCandidateCmlTarget(snapshot, effects, mappings, "fixture-patch", "models/order.cml",
        InternalModelCandidateCmlContent(record("fixture-proposed-content"), proposedbytes.size.toLong, Base64.getEncoder.encodeToString(proposedbytes.toArray)), source, "fixture-target")
      val candidate = InternalModelCandidateCmlProjection(session.candidate.reference, "fixture-candidate-model", continuityref,
        "ccdm-candidate-cml-projection-v2", realizationref, "2.0", semanticscope, Vector(target))
      val condition = ComponentDashboardCondition("available", "admitted", None, Some("New declared Goal was absent in canonical basis"), None, None, None, None, Vector.empty)
      val attribution = ComponentDashboardSourceAttribution(source.identity, source.authority, source.locator.get)
      val patch = CandidateDesignProposedCmlPatchTrace("fixture-patch", scope.context, scope.component, source.authority, source.locator.get,
        snapshot, target.proposedContent.contentReference, attribution, condition, Vector.empty, None)
      val goalmapping = mappings.find(_.semanticIdentity == "repeat-purchase").get
      val entry = CandidateDesignSemanticDiffEntry("fixture-diff-goal", scope.context, scope.component, patch.id,
        candidate.candidateModelIdentity, "goal", "add", "repeat-purchase", None, Some("Place another order"), "explicit creation", attribution, condition, Vector.empty, None)
      val diff = InternalModelSemanticDiff(candidateref, candidate.candidateReference, candidate.candidateModelIdentity,
        "ccdm-semantic-diff-v2", "2.0", semanticscope, record("fixture-semantic-diff"),
        Vector(InternalModelSemanticDiffTarget(Vector(InternalModelSemanticDiffMappedEntry(entry, goalmapping.mappingId, "element")), patch, target.targetId)))
      val candidatedependencies = Vector(realizationref, continuityref, snapshot).sortBy(_.artifactId.value)
      val diffdependencies = Vector(candidateref).sortBy(_.artifactId.value)
      val mappingdependencies = Vector(snapshot, editref, candidateref).sortBy(_.artifactId.value)
      val entries = Vector(artifactentry(factsref, "sources/model-context.json", Vector.empty), artifactentry(snapshot, "sources/cml.json", Vector.empty),
        artifactentry(realizationref, "realization.json", Vector(factsref)), artifactentry(continuityref, "projections/continuity.json", Vector(realizationref)),
        artifactentry(candidateref, "projections/candidate.json", candidatedependencies), artifactentry(diffref, "projections/diff.json", diffdependencies),
        artifactentry(editref, "projections/edit.json", session.candidate.artifacts), artifactentry(mappingref, "projections/source-mapping.json", mappingdependencies))
      val packageidentity = InternalModelPackageReference(InternalModelPackageId.from("22000000-0000-0000-0000-000000000001").toOption.get,
        InternalModelProjectToken.from("example").toOption.get, InternalModelProjectToken.from("order").toOption.get)
      def _ordered_(remaining: Vector[InternalModelVerifiedArtifactContext], accumulated: Vector[InternalModelVerifiedArtifactContext]): Vector[InternalModelVerifiedArtifactContext] =
        if (remaining.isEmpty) accumulated else {
          val ready = remaining.filter(value => value.dependencies.forall(reference => accumulated.exists(_.reference == reference))).sortBy(_.reference.artifactId.value).head
          _ordered_(remaining.filterNot(_.reference == ready.reference), accumulated :+ ready)
        }
      val context = InternalModelVerifiedPackageContext(packageidentity, "2.0", 1, "candidate", _ordered_(entries, Vector.empty))
      val realizationcapture = InternalModelVerifiedRealization(realizationref, "realization.json", true, Vector(factsref), InternalModelSemanticRealizationEncoder.encode(canonical.realization).toVector)
      val snapshots = Vector(InternalModelVerifiedSourceSnapshot(factsref, "sources/model-context.json", true, Vector.empty, Some(canonical.snapshot)),
        InternalModelVerifiedSourceSnapshot(snapshot, "sources/cml.json", true, Vector.empty, Some(baselinebytes)))
      val continuitycapture = InternalModelVerifiedProjectionContinuityPackage(InternalModelVerifiedRealizationPackage(realizationcapture, snapshots),
        InternalModelVerifiedProjection(continuityref, "projections/continuity.json", true, Vector(realizationref), InternalModelProjectionBindingCodec.encode(binding).toVector))
      val candidatecapture = InternalModelVerifiedCandidateCmlProjectionPackage(context, continuitycapture,
        InternalModelVerifiedProjection(candidateref, "projections/candidate.json", true, candidatedependencies, InternalModelCandidateCmlProjectionCodec.encode(candidate).toVector))
      val diffcapture = InternalModelVerifiedSemanticDiffPackage(candidatecapture,
        InternalModelVerifiedProjection(diffref, "projections/diff.json", true, diffdependencies, InternalModelSemanticDiffCodec.encode(diff).toVector))
      val candidateresult = InternalModelCandidateCmlProjectionValidator.validateVerified(candidatecapture)
      for {
        admitted <- candidateresult.toOption.toRight(candidateresult.show)
        mapped <- ModelEditCmlProjection.bind(confirmed, session, editref, admitted)
        decoded <- ModelEditCmlProjection.decodeMapping(InternalModelVerifiedProjection(mappingref, "projections/source-mapping.json", true, mappingdependencies, ModelEditCmlProjection.encodeMapping(mapped)))
        _ <- ModelEditCmlProjection.validateMapping(decoded, session, admitted)
        diffresult = InternalModelSemanticDiffValidator.validateVerified(diffcapture)
        semanticdiff <- diffresult.toOption.toRight(diffresult.show)
        subject <- InternalModelCandidateReviewBindingValidator.completeSubject(context, Vector.empty, reviewref)
      } yield Prepared(admitted, mapped, semanticdiff, subject, canonical)
    }
  }
}
