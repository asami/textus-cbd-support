package org.simplemodeling.textus.cbdsupport.runtime

import ModelEditSession.*
import ModelEditReview.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen
import java.nio.file.Paths
import org.goldenport.cncf.component.ComponentId
import org.goldenport.cncf.component.capability.{ComponentCapabilityAdmissionScope, ComponentCapabilityPackageSource}
import org.goldenport.cncf.component.capability.ComponentCapabilityReference.{ModelElementId, Source, Present}
import org.goldenport.cncf.component.capability.ComponentCapabilityProjection.{ComponentCoordinate, PackageCoordinate, Producer}
import CapabilityCatalogModel.{CatalogSelection, PackageProjection, PortableProjection}
import CapabilityIngestionModel.{ComponentUsageCapabilityReference, ComponentUsageDefinition, ComponentUsageDocument,
  ComponentUsageExpectation, ComponentUsageFamily, ComponentUsagePackage, ComponentUsageProjection, PackageExpectation,
  ProjectionFamily, Provenance, SnapshotReference, UnavailableUpstreamProjection}
import CapabilityQueryModel.{CapabilityGap, DemandSelection, MissingComponentUsageReason}

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditReviewSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  "Project-policy model edit review" should {
    "emit every required finding with complete-scope evidence and exact subject identity" in {
      Given("complete project policy and independent incomplete development traces")
      val publication = proposedPublication()
      val policy = FindingPolicy(record("project-finding-policy"), FindingKind.values.map(value => value -> FindingDisposition(Severity.Warning, false)).toMap)
      When("the candidate and explicit complete observations are reviewed")
      val imported = session(publication)
      val usecase = imported.candidate.useCases.head.copy(id = "uncovered-use-case", semanticTargetId = identity("uncovered-use-case"), references = Vector.empty)
      val slice = imported.candidate.slices.head.copy(reference = record("unlinked-slice"), identity = identity("unlinked-slice"))
      val current = imported.copy(candidate = imported.candidate.copy(useCases = imported.candidate.useCases :+ usecase,
        slices = imported.candidate.slices :+ slice, goalLinks = Vector.empty))
      val scopes = _scopes(current)
      val behavior = current.candidate.useCases.map(value => BehaviorCoverage(value.semanticTargetId,
        Vector(Subject.Relationship(record("uncovered-behavior"))), RelationshipScopeEvidence.Complete(Subject.UseCase(value.semanticTargetId), snapshot)))
      val demands = current.candidate.useCases.map(value => CapabilityDemand(value.semanticTargetId, Vector.empty,
        RelationshipScopeEvidence.Complete(Subject.UseCase(value.semanticTargetId), snapshot)))
      val specification = current.candidate.specificationLinks.head
      val ticket = current.candidate.ticketLinks.head
      val resolutions = Vector(ResolutionEvidence(ExternalReference.Specification(specification.specification), ResolutionState.Missing, snapshot, specification.attribution),
        ResolutionEvidence(ExternalReference.Ticket(ticket.ticket), ResolutionState.Missing, snapshot, ticket.attribution))
      val result = ModelEditReview.review(current, policy, scopes, resolutions, behavior, demands)
      Then("the complete findings retain project disposition and separate dangling endpoint identities")
      result.toOption.get.findings.map(_.kind).toSet shouldBe FindingKind.values.toSet
      result.toOption.get.findings.forall(_.policy == policy.reference) shouldBe true
      result.toOption.get.findings.forall(_.disposition == FindingDisposition(Severity.Warning, false)) shouldBe true
      result.toOption.get.findings.filter(value => Set(FindingKind.DanglingExecutableSpecification, FindingKind.DanglingTicket)(value.kind))
        .map(_.subject).toSet shouldBe Set(Subject.Relationship(specification.reference), Subject.Relationship(ticket.reference))
      result.toOption.get.unresolved shouldBe empty
      result.toOption.get.dependency.candidate shouldBe current.candidate.reference
    }
    "keep unavailable scope distinct from a complete empty relationship family" in {
      Given("one unlinked Goal and a complete project policy")
      val publication = proposedPublication()
      val policy = FindingPolicy(record("policy"), FindingKind.values.map(value => value -> FindingDisposition(Severity.Error, true)).toMap)
      When("complete and unavailable observations are independently reviewed")
      val imported = session(publication)
      val current = imported.copy(candidate = imported.candidate.copy(goalLinks = Vector.empty))
      val subject = Subject.Goal(current.candidate.goals.head.reference.semanticTargetId)
      val complete = _scopes(current)
      val unavailable = complete.map(value => if (value.subject == subject) value.copy(evidence = RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable)) else value)
      val missing = ModelEditReview.review(current, policy, complete, Vector.empty, Vector.empty, Vector.empty)
      val unresolved = ModelEditReview.review(current, policy, unavailable, Vector.empty, Vector.empty, Vector.empty)
      val nopolicy = ModelEditReview.review(current, policy.copy(dispositions = Map.empty), complete, Vector.empty, Vector.empty, Vector.empty)
      Then("only complete evidence proves absence and an incomplete policy cannot silently choose a severity")
      missing.toOption.get.findings.map(_.kind) should contain(FindingKind.GoalWithoutUseCase)
      unresolved.toOption.get.findings.map(_.kind) should not contain FindingKind.GoalWithoutUseCase
      unresolved.toOption.get.unresolved should contain(RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable))
      nopolicy.isLeft shouldBe true
    }
    "derive absence only from complete source-owned coverage and keep unsupported source scope unresolved" in {
      Given("a complete canonical basis that declares its missing Goal link family")
      val basis = basisPublication()
      val proposed = proposedPublication()
      val policy = FindingPolicy(record("source-policy"), FindingKind.values.map(value => value -> FindingDisposition(Severity.Warning, false)).toMap)
      When("the same unlinked candidate is reviewed against complete and unsupported source coverage")
      val edited = editedSession(basis, proposed)
      val unlinked = edited.copy(candidate = edited.candidate.copy(goalLinks = Vector.empty))
      val unsupported = basis.copy(document = basis.document.copy(coverage = basis.document.coverage.map(value =>
        if (value.family == "goal-use-case") value.copy(status = "unsupported", detail = Some("unsupported source scope")) else value)))
      val complete = ModelEditReview.reviewSource(unlinked, policy, selection(basis), Vector.empty, Vector.empty, Vector.empty)
      val unavailable = ModelEditReview.reviewSource(unlinked, policy, selection(unsupported), Vector.empty, Vector.empty, Vector.empty)
      Then("only complete source evidence establishes a missing link")
      complete.toOption.get.findings.map(_.kind) should contain(FindingKind.GoalWithoutUseCase)
      unavailable.toOption.get.findings.map(_.kind) should not contain FindingKind.GoalWithoutUseCase
      unavailable.toOption.get.unresolved should contain(RelationshipScopeEvidence.Unresolved(Subject.Goal(identity("repeat-purchase")), ResolutionState.Unsupported))
    }

    "retain actual Phase 11 projection availability before deriving Review and shared View evidence" in {
      Given("admitted semantic packages with unavailable, mixed, available-absent and available-empty Usage observations")
      val caseinputs: Vector[(String, Vector[Boolean], Boolean, Option[MissingComponentUsageReason])] = Vector(
        ("unavailable", Vector(false), false, Some(MissingComponentUsageReason.ProjectionNotSupplied)),
        ("mixed", Vector(true, false), false, Some(MissingComponentUsageReason.ProjectionNotSupplied)),
        ("absent-reference", Vector(true), false, Some(MissingComponentUsageReason.AbsentSelectedReference)),
        ("present-empty", Vector(true), true, None))
      val inputs = caseinputs.map { case (_, available, present, _) => _capability_projection(available, present) }
      val publication = proposedPublication()
      val policy = FindingPolicy(record("demand-policy"), FindingKind.values.map(value => value -> FindingDisposition(Severity.Warning, false)).toMap)
      When("the existing query Session diagnoses each exact selection and its typed result passes through the adapter and View")
      val current = session(publication)
      val usecase = current.candidate.useCases.head.semanticTargetId
      val subject = Subject.UseCase(usecase)
      val selector = DemandSelection.ComponentUsage(_usage_reference)
      val selected = CapabilitySelection(usecase, selector, Vector.empty, RelationshipScopeEvidence.Complete(subject, snapshot))
      val observed = inputs.map { input =>
        val query = CapabilityQueries.open(Right(input), CatalogSelection(input.snapshotReference, input.provenance))
          .fold(error => fail(error.toString), value => value)
        val original = query.diagnoseCapabilityDemand(selector)
        val adapted = ModelEditReview.reviewWithCapabilityDemand(current, policy, _scopes(current), Vector.empty,
          Vector.empty, query, Vector(selected)).toOption.get
        val view = ModelEditViewProjection.project(current, selection(publication), adapted.demands.map(value => value.useCase -> value).toMap)
        (query.scope, original, adapted, view)
      }
      Then("projection-not-supplied never proves a missing demand trace, while complete absent and empty scopes remain distinct original diagnoses")
      observed.zip(caseinputs).zip(inputs).foreach { case (((scope, original, adapted, view), (label, _, present, reason)), input) =>
        withClue(label) {
          adapted.observations shouldBe Vector(Phase11Demand(selector, original))
          val diagnosis = original.toOption.get
          diagnosis.scope shouldBe scope
          diagnosis.scope.provenance shouldBe input.provenance
          diagnosis.value.gaps shouldBe reason.toVector.map(value =>
            CapabilityGap.MissingComponentUsageCapabilityProjection(scope, _usage_reference, value, input.usage))
          diagnosis.value.usages.size shouldBe (if (present) 1 else 0)
          diagnosis.value.usages.flatMap(_.paths) shouldBe empty
          val unavailable = reason.contains(MissingComponentUsageReason.ProjectionNotSupplied)
          adapted.review.findings.exists(_.kind == FindingKind.MissingCapabilityDemandTrace) shouldBe !unavailable
          adapted.demands.head.evidence shouldBe (if (unavailable)
            RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable) else selected.evidence)
          view.toOption.get.capabilityDemands shouldBe adapted.demands
          view.toOption.get.traces.head.capabilityDemandRefs shouldBe empty
          if (unavailable) adapted.review.unresolved should contain(RelationshipScopeEvidence.Unresolved(subject, ResolutionState.Unavailable))
        }
      }
    }

    "retain deterministic findings when complete scopes arrive in any order" in {
      forAll(Gen.oneOf(true, false)) { reverse =>
        Given("the same project policy and explicitly identified source observations")
        val publication = proposedPublication()
        val policy = FindingPolicy(record("policy"), FindingKind.values.map(value => value -> FindingDisposition(Severity.Information, false)).toMap)
        When("the exact candidate is reviewed with both scope orders")
        val imported = session(publication)
        val current = imported.copy(candidate = imported.candidate.copy(goalLinks = Vector.empty))
        val scopes = _scopes(current)
        val first = ModelEditReview.review(current, policy, scopes, Vector.empty, Vector.empty, Vector.empty)
        val second = ModelEditReview.review(current, policy, if (reverse) scopes.reverse else scopes, Vector.empty, Vector.empty, Vector.empty)
        Then("the review does not use collection position as a control identity")
        second shouldBe first
      }
    }
  }
  private val _usage_reference = ComponentUsageCapabilityReference(ModelElementId("model:demand", "buy"), "usage-v1")

  /** Pure declared carriers admitted by the existing Phase 11 owner, not producer delivery evidence. */
  private def _capability_projection(availability: Vector[Boolean], present: Boolean): PortableProjection = {
    val source = Source("model:demand", "models/demand.cml", "source-r1", None)
    val scope = ComponentCapabilityAdmissionScope.Scope("scope:model-edit-demand", source,
      Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
    val usecase = UnavailableUpstreamProjection(ProjectionFamily.UseCase, "not-yet-admitted", "No positive Use Case owner mapping is admitted.")
    val unavailable = UnavailableUpstreamProjection(ProjectionFamily.ComponentUsage, "not-supplied", "Selected Usage projection is unavailable.")
    val producer = Producer("synthetic-semantic", "fixture-v1")
    val packages = availability.indices.map { index =>
      val component = ComponentCoordinate(ComponentId("org.example.Demand" + index), "1.0.0-SNAPSHOT")
      val coordinate = PackageCoordinate("package:demand:" + index, "package-v1")
      val input: ComponentCapabilityPackageSource.PackageInput.Selected = new ComponentCapabilityPackageSource.PackageInput.Selected(scope.scopeId,
        ComponentCapabilityPackageSource.Location.Directory(Paths.get("/declared/model-edit-demand/" + index)), component, coordinate)
      PackageProjection(input, Vector.empty, Vector.empty)
    }.toVector
    val projections = packages.zip(availability).map { case (pkg, available) =>
      if (available) {
        val definition = ComponentUsageDefinition(pkg.input.component.componentId, _usage_reference.identity, _usage_reference.contractVersion,
          "Buy", Present("Declared Usage"), Present("No demand mappings declared."), source, Present(Vector.empty), Present(Vector.empty),
          Present(Vector.empty), Present(Vector.empty), Present(Vector.empty), Present(Vector.empty))
        Some(ComponentUsageProjection(1, "cozy.component-usage-capability.v1", producer, pkg.input.component, pkg.input.packageCoordinate,
          if (present) Vector(ComponentUsageDocument(source, Vector(definition))) else Vector.empty))
      } else None
    }
    val expectations = packages.zip(projections).map { case (pkg, projection) =>
      PackageExpectation(pkg.input, Vector.empty, projection.map(value =>
        ComponentUsageExpectation(value.producer, value.schemaVersion, value.generatedAbiVersion, value.documents.map(_.source))))
    }
    val families = packages.zip(projections).map { case (pkg, projection) =>
      ComponentUsagePackage(pkg.input, CapabilityComponentUsageAcquisitionPart.resourcePath, projection match {
        case Some(value) => ComponentUsageFamily.Available(value, Vector.empty)
        case None => ComponentUsageFamily.Unavailable(unavailable)
      })
    }
    PortableProjection(1, SnapshotReference.from(record("phase11-demand-snapshot")).toOption.get,
      Provenance(scope, expectations, usecase), packages, families, usecase)
  }

  private def _scopes(session: ModelEditSession): Vector[SubjectScope] = {
    val candidate = session.candidate
    val goals = candidate.goals.map(value => (Subject.Goal(value.reference.semanticTargetId), "goal-use-case"))
    val usecases = candidate.useCases.flatMap(value => Vector((Subject.UseCase(value.semanticTargetId), "goal-use-case"), (Subject.UseCase(value.semanticTargetId), "use-case-slice")))
    val slices = candidate.slices.flatMap(value => Vector((Subject.Slice(value.identity), "slice-specification"), (Subject.Slice(value.identity), "slice-ticket")))
    (goals ++ usecases ++ slices).map { case (subject, family) => SubjectScope(subject, family, RelationshipScopeEvidence.Complete(subject, snapshot)) }
  }
}
