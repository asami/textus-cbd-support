package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 11, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class LifecycleUnifiedEvidenceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "LifecycleUnifiedEvidenceIntegration" should {
    "P9-62B preserve caller-admitted five-lane evidence without changing its authority" which {
      "retain all five lanes and apply only their fixed deterministic presentation orders" in {
        Given("three declared relationship families and reverse-ordered runtime, Usage, Operation, and Quality/Review evidence")
        val relationships = Vector(
          _relationship("relationship-association", LifecycleUnifiedEvidenceAssociation),
          _relationship("relationship-aggregation", LifecycleUnifiedEvidenceAggregation),
          _relationship("relationship-composition", LifecycleUnifiedEvidenceComposition)
        )
        val runtime = LifecycleUnifiedEvidenceRuntimeLane(
          Vector(
            _runtime("runtime-composition", "relationship-composition"),
            _runtime("runtime-association", "relationship-association"),
            _runtime("runtime-aggregation", "relationship-aggregation")
          ),
          None
        )
        val usage = LifecycleUnifiedEvidenceUsageLane(
          Vector(
            _usage("usage-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
            _usage("usage-alpha", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-composition"))
          ),
          None
        )
        val operation = LifecycleUnifiedEvidenceOperationLane(
          Vector(
            _operation("operation-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
            _operation("operation-alpha", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-aggregation"))
          ),
          None
        )
        val quality = LifecycleUnifiedEvidenceQualityReviewLane(
          Vector(
            _quality("quality-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
            _quality("quality-alpha", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-association"))
          ),
          None
        )

        When("the same exact Component and CCDM context values are admitted")
        val result = LifecycleUnifiedEvidenceIntegration.create(_context, _component, relationships, runtime, usage, operation, quality)

        Then("the projected integration retains every supplied lane value in the non-semantic fixed order")
        result shouldBe a[LifecycleUnifiedEvidenceProjectedIntegration]
        val integration = _projected(result)
        integration.context shouldBe _context
        integration.component shouldBe _component
        integration.declaredRelationships shouldBe Vector(relationships(2), relationships(1), relationships(0))
        integration.runtimeLane.evidence shouldBe Vector(runtime.evidence(2), runtime.evidence(1), runtime.evidence(0))
        integration.usageLane.evidence shouldBe Vector(usage.evidence(1), usage.evidence(0))
        integration.operationLane.evidence shouldBe Vector(operation.evidence(1), operation.evidence(0))
        integration.qualityReviewLane.evidence shouldBe Vector(quality.evidence(1), quality.evidence(0))
        integration.declaredRelationships.map(_.id) shouldBe
          Vector("relationship-composition", "relationship-aggregation", "relationship-association")
        integration.runtimeLane.evidence.map(_.id) shouldBe
          Vector("runtime-aggregation", "runtime-association", "runtime-composition")
        integration.usageLane.evidence.map(_.id) shouldBe Vector("usage-alpha", "usage-zulu")
        integration.operationLane.evidence.map(_.id) shouldBe Vector("operation-alpha", "operation-zulu")
        integration.qualityReviewLane.evidence.map(_.id) shouldBe Vector("quality-alpha", "quality-zulu")
      }

      "retain distinct Usage/Discovery, Operation, and Quality/Review authority representations only through explicit association types" in {
        Given("one relationship-associated Usage record, one Component/context-associated Operation record, and one relationship-associated Quality/Review record")
        val relationship = _relationship("relationship-1", LifecycleUnifiedEvidenceComposition)
        val usage = _usage("usage-1", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-1"), "Discovery representation")
        val operation = _operation("operation-1", LifecycleUnifiedEvidenceComponentContextAssociation, "Operation representation")
        val quality = _quality("quality-1", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-1"), "authorized Review representation")

        When("the distinct caller-admitted evidence records are integrated")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector(relationship),
          LifecycleUnifiedEvidenceRuntimeLane(Vector(_runtime("runtime-1", "relationship-1")), None),
          LifecycleUnifiedEvidenceUsageLane(Vector(usage), None),
          LifecycleUnifiedEvidenceOperationLane(Vector(operation), None),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector(quality), None)
        )

        Then("each lane preserves its own representation and its only admitted explicit association")
        val integration = _projected(result)
        integration.usageLane.evidence shouldBe Vector(usage)
        integration.operationLane.evidence shouldBe Vector(operation)
        integration.qualityReviewLane.evidence shouldBe Vector(quality)
        integration.usageLane.evidence.map(_.association) shouldBe
          Vector(LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-1"))
        integration.operationLane.evidence.map(_.association) shouldBe
          Vector(LifecycleUnifiedEvidenceComponentContextAssociation)
        integration.qualityReviewLane.evidence.map(_.representation) shouldBe Vector("authorized Review representation")
      }

      "reject blank foreign and out-of-scope explicit associations without returning a partial integration" in {
        Given("records with a blank declared relationship association, a foreign relationship association, and mismatched context and Component scope")
        val relationship = _relationship("relationship-1", LifecycleUnifiedEvidenceComposition)
        val usage = _usage("usage-missing", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation(" "))
        val operation = _operation("operation-foreign", LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-foreign"))
        val quality = _quality(
          "quality-other-context",
          LifecycleUnifiedEvidenceComponentContextAssociation,
          context = MonoKotoProjectionContextIdentity("context-other")
        )
        val runtime = _runtime(
          "runtime-other-component",
          "relationship-1",
          component = ComponentDashboardComponentIdentity("component-other")
        )

        When("the invalid associations and scopes are admitted together")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector(relationship),
          LifecycleUnifiedEvidenceRuntimeLane(Vector(runtime), None),
          LifecycleUnifiedEvidenceUsageLane(Vector(usage), None),
          LifecycleUnifiedEvidenceOperationLane(Vector(operation), None),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector(quality), None)
        )

        Then("one typed rejection carries the failures and no projected integration is exposed")
        result shouldBe a[LifecycleUnifiedEvidenceRejectedIntegration]
        result.isInstanceOf[LifecycleUnifiedEvidenceProjectedIntegration] shouldBe false
        val failure = _rejected(result)
        failure.violations.exists(_.contains("Usage/Discovery evidence 'usage-missing' declared relationship association")) shouldBe true
        failure.violations.exists(_.contains("Operation evidence 'operation-foreign' explicit relationship association")) shouldBe true
        failure.violations.exists(_.contains("Quality/Review evidence 'quality-other-context' is outside the exact")) shouldBe true
        failure.violations.exists(_.contains("Runtime lifecycle evidence 'runtime-other-component' is outside the exact")) shouldBe true
      }

      "reject missing declared semantics and lane occupancy that contains both or neither evidence and an empty-lane condition" in {
        Given("no declared relationship, a runtime lane with neither value, and a Usage lane with both evidence and an empty-lane condition")
        val usage = _usage("usage-1", LifecycleUnifiedEvidenceComponentContextAssociation)

        When("the structurally incomplete integration is admitted")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector.empty,
          LifecycleUnifiedEvidenceRuntimeLane(Vector.empty, None),
          LifecycleUnifiedEvidenceUsageLane(Vector(usage), Some(_empty_lane_condition("usage-empty"))),
          LifecycleUnifiedEvidenceOperationLane(Vector(_operation("operation-1", LifecycleUnifiedEvidenceComponentContextAssociation)), None),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector(_quality("quality-1", LifecycleUnifiedEvidenceComponentContextAssociation)), None)
        )

        Then("the complete input is rejected without inferring declared semantics or an empty-lane condition")
        result shouldBe a[LifecycleUnifiedEvidenceRejectedIntegration]
        val failure = _rejected(result)
        failure.violations should contain("Lifecycle Unified Evidence Integration requires one or more declared relationships.")
        failure.violations should contain(
          "Runtime lifecycle evidence lane must contain evidence or exactly one caller-admitted empty-lane condition, but not both or neither."
        )
        failure.violations should contain(
          "Usage/Discovery evidence lane must contain evidence or exactly one caller-admitted empty-lane condition, but not both or neither."
        )
      }

      "accept and retain exactly one caller-admitted empty-lane condition for every non-declared authority lane" in {
        Given("one declared relationship and separate valid empty-lane conditions for Runtime, Usage/Discovery, Operation, and Quality/Review")
        val relationship = _relationship("relationship-1", LifecycleUnifiedEvidenceComposition)
        val runtimeemptycondition = _empty_lane_condition("runtime-empty")
        val usageemptycondition = _empty_lane_condition("usage-empty")
        val operationemptycondition = _empty_lane_condition("operation-empty")
        val qualityemptycondition = _empty_lane_condition("quality-empty")

        When("each non-declared authority lane admits no evidence and its one caller-admitted empty-lane condition")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector(relationship),
          LifecycleUnifiedEvidenceRuntimeLane(Vector.empty, Some(runtimeemptycondition)),
          LifecycleUnifiedEvidenceUsageLane(Vector.empty, Some(usageemptycondition)),
          LifecycleUnifiedEvidenceOperationLane(Vector.empty, Some(operationemptycondition)),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector.empty, Some(qualityemptycondition))
        )

        Then("the projected integration retains each exact empty condition without rejection or inferred lifecycle, absence, availability, operation, or Review conclusion")
        result shouldBe a[LifecycleUnifiedEvidenceProjectedIntegration]
        val integration = _projected(result)
        integration.runtimeLane.evidence shouldBe Vector.empty
        integration.usageLane.evidence shouldBe Vector.empty
        integration.operationLane.evidence shouldBe Vector.empty
        integration.qualityReviewLane.evidence shouldBe Vector.empty
        integration.runtimeLane.emptyCondition shouldBe Some(runtimeemptycondition)
        integration.usageLane.emptyCondition shouldBe Some(usageemptycondition)
        integration.operationLane.emptyCondition shouldBe Some(operationemptycondition)
        integration.qualityReviewLane.emptyCondition shouldBe Some(qualityemptycondition)
      }

      "reject blank required values attribution condition and limitation values as one all-or-nothing admission failure" in {
        Given("five lanes carrying blank identities, owners, locators, representations, condition values, limitations, and stable tie keys")
        val relationship = _relationship(" ", LifecycleUnifiedEvidenceComposition).copy(
          subject = ComponentDashboardSemanticTargetIdentity(" "),
          objectTarget = ComponentDashboardSemanticTargetIdentity(" "),
          sourceOwner = " ",
          sourceLocator = " ",
          attribution = _attribution(" ", " "),
          condition = _condition(availability = " ", redaction = Some(" "), limitations = Vector(" ")),
          limitations = Vector(" "),
          stableTieKey = Some(" ")
        )
        val runtime = _runtime("runtime-1", " ").copy(
          observedValue = " ",
          attribution = _attribution("runtime-source", " "),
          condition = _condition(authorization = " "),
          limitations = Vector(" ")
        )
        val usage = _usage("usage-1", LifecycleUnifiedEvidenceComponentContextAssociation, " ")
        val operation = _operation("operation-1", LifecycleUnifiedEvidenceComponentContextAssociation, " ")
        val quality = _quality("quality-1", LifecycleUnifiedEvidenceComponentContextAssociation, " ")

        When("the malformed caller-admitted values are integrated")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector(relationship),
          LifecycleUnifiedEvidenceRuntimeLane(Vector(runtime), None),
          LifecycleUnifiedEvidenceUsageLane(Vector(usage), None),
          LifecycleUnifiedEvidenceOperationLane(Vector(operation), None),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector(quality), None)
        )

        Then("one typed rejection retains no partial success while exposing the required-value violations")
        result shouldBe a[LifecycleUnifiedEvidenceRejectedIntegration]
        val failure = _rejected(result)
        failure.violations.exists(_.contains("Declared relationship identity must not be blank")) shouldBe true
        failure.violations.exists(_.contains("subject identity must not be blank")) shouldBe true
        failure.violations.exists(_.contains("source owner must not be blank")) shouldBe true
        failure.violations.exists(_.contains("source locator must not be blank")) shouldBe true
        failure.violations.exists(_.contains("condition availability must not be blank")) shouldBe true
        failure.violations.exists(_.contains("Runtime lifecycle evidence 'runtime-1' observed value must not be blank")) shouldBe true
        failure.violations.exists(_.contains("Usage/Discovery evidence 'usage-1' representation must not be blank")) shouldBe true
        failure.violations.exists(_.contains("Operation evidence 'operation-1' representation must not be blank")) shouldBe true
        failure.violations.exists(_.contains("Quality/Review evidence 'quality-1' representation must not be blank")) shouldBe true
      }

      "retain supplied lifecycle, explicit-absence, and Review representation values without deriving a lifecycle or Review conclusion" in {
        Given("runtime and Quality/Review records whose values and conditions are supplied by their separate owners")
        val runtimecondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("source-bounded explicit absence"),
          limitations = Vector("runtime source limitation")
        )
        val qualitycondition = _condition(
          redaction = Some("review representation redacted"),
          limitations = Vector("Review owner limitation")
        )
        val runtime = _runtime("runtime-1", "relationship-1", "source-observed lifecycle value", condition = runtimecondition)
        val quality = _quality(
          "quality-1",
          LifecycleUnifiedEvidenceDeclaredRelationshipAssociation("relationship-1"),
          "authorized existing Review representation",
          condition = qualitycondition
        )

        When("the caller-admitted values are integrated")
        val result = LifecycleUnifiedEvidenceIntegration.create(
          _context,
          _component,
          Vector(_relationship("relationship-1", LifecycleUnifiedEvidenceComposition)),
          LifecycleUnifiedEvidenceRuntimeLane(Vector(runtime), None),
          LifecycleUnifiedEvidenceUsageLane(Vector(_usage("usage-1", LifecycleUnifiedEvidenceComponentContextAssociation)), None),
          LifecycleUnifiedEvidenceOperationLane(Vector(_operation("operation-1", LifecycleUnifiedEvidenceComponentContextAssociation)), None),
          LifecycleUnifiedEvidenceQualityReviewLane(Vector(quality), None)
        )

        Then("the source records and their conditions remain values without an inferred absence, lifecycle state, or Review result")
        val integration = _projected(result)
        integration.runtimeLane.evidence shouldBe Vector(runtime)
        integration.qualityReviewLane.evidence shouldBe Vector(quality)
        integration.runtimeLane.evidence.map(_.condition.explicitAbsence) shouldBe Vector(Some("source-bounded explicit absence"))
        integration.qualityReviewLane.evidence.map(_.condition) shouldBe Vector(qualitycondition)
      }

      "preserve deterministic fixed ordering for ScalaCheck-generated input permutations" in {
        Given("identity-distinct caller-admitted values in each of the five lanes")
        val relationships = Vector(
          _relationship("relationship-association", LifecycleUnifiedEvidenceAssociation),
          _relationship("relationship-aggregation", LifecycleUnifiedEvidenceAggregation),
          _relationship("relationship-composition", LifecycleUnifiedEvidenceComposition)
        )
        val runtime = Vector(
          _runtime("runtime-composition", "relationship-composition"),
          _runtime("runtime-association", "relationship-association"),
          _runtime("runtime-aggregation", "relationship-aggregation")
        )
        val usage = Vector(
          _usage("usage-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
          _usage("usage-alpha", LifecycleUnifiedEvidenceComponentContextAssociation)
        )
        val operation = Vector(
          _operation("operation-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
          _operation("operation-alpha", LifecycleUnifiedEvidenceComponentContextAssociation)
        )
        val quality = Vector(
          _quality("quality-zulu", LifecycleUnifiedEvidenceComponentContextAssociation),
          _quality("quality-alpha", LifecycleUnifiedEvidenceComponentContextAssociation)
        )
        val permutations = for {
          relationshippermutation <- Gen.pick(relationships.size, relationships)
          runtimepermutation <- Gen.pick(runtime.size, runtime)
          usagepermutation <- Gen.pick(usage.size, usage)
          operationpermutation <- Gen.pick(operation.size, operation)
          qualitypermutation <- Gen.pick(quality.size, quality)
        } yield (
          relationshippermutation.toVector,
          runtimepermutation.toVector,
          usagepermutation.toVector,
          operationpermutation.toVector,
          qualitypermutation.toVector
        )

        forAll(permutations) { case (relationshippermutation, runtimepermutation, usagepermutation, operationpermutation, qualitypermutation) =>
          When("one generated permutation is admitted")
          val result = LifecycleUnifiedEvidenceIntegration.create(
            _context,
            _component,
            relationshippermutation,
            LifecycleUnifiedEvidenceRuntimeLane(runtimepermutation, None),
            LifecycleUnifiedEvidenceUsageLane(usagepermutation, None),
            LifecycleUnifiedEvidenceOperationLane(operationpermutation, None),
            LifecycleUnifiedEvidenceQualityReviewLane(qualitypermutation, None)
          )

          Then("identity-first fixed ordering does not inherit caller iteration order")
          val integration = _projected(result)
          integration.declaredRelationships.map(_.id) shouldBe
            Vector("relationship-composition", "relationship-aggregation", "relationship-association")
          integration.runtimeLane.evidence.map(_.id) shouldBe
            Vector("runtime-aggregation", "runtime-association", "runtime-composition")
          integration.usageLane.evidence.map(_.id) shouldBe Vector("usage-alpha", "usage-zulu")
          integration.operationLane.evidence.map(_.id) shouldBe Vector("operation-alpha", "operation-zulu")
          integration.qualityReviewLane.evidence.map(_.id) shouldBe Vector("quality-alpha", "quality-zulu")
        }
      }
    }
  }

  private val _context = MonoKotoProjectionContextIdentity("context-sales")
  private val _component = ComponentDashboardComponentIdentity("component-sales")

  private def _projected(
    result: LifecycleUnifiedEvidenceIntegrationResult
  ): LifecycleUnifiedEvidenceIntegration =
    result.asInstanceOf[LifecycleUnifiedEvidenceProjectedIntegration].sourceIntegration

  private def _rejected(
    result: LifecycleUnifiedEvidenceIntegrationResult
  ): LifecycleUnifiedEvidenceIntegrationFailure =
    result.asInstanceOf[LifecycleUnifiedEvidenceRejectedIntegration].failure

  private def _relationship(
    id: String,
    family: LifecycleUnifiedEvidenceRelationshipFamily,
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    subjectid: String = "subject-order",
    objectid: String = "object-order",
    attribution: ComponentDashboardSourceAttribution = _attribution("design-source", "design://relationship"),
    condition: ComponentDashboardCondition = _condition(),
    limitations: Vector[String] = Vector("declared source limitation"),
    tiekey: Option[String] = Some("relationship-tie")
  ): LifecycleUnifiedEvidenceDeclaredRelationship =
    LifecycleUnifiedEvidenceDeclaredRelationship(
      id,
      context,
      component,
      family,
      ComponentDashboardSemanticTargetIdentity(subjectid),
      ComponentDashboardSemanticTargetIdentity(objectid),
      "canonical-design-owner",
      "design://relationship",
      attribution,
      condition,
      limitations,
      tiekey
    )

  private def _runtime(
    id: String,
    declaredrelationshipid: String,
    observedvalue: String = "caller-observed runtime value",
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    attribution: ComponentDashboardSourceAttribution = _attribution("runtime-source", "runtime://observation"),
    condition: ComponentDashboardCondition = _condition(),
    limitations: Vector[String] = Vector("runtime source limitation"),
    tiekey: Option[String] = Some("runtime-tie")
  ): LifecycleUnifiedEvidenceRuntimeEvidence =
    LifecycleUnifiedEvidenceRuntimeEvidence(
      id,
      declaredrelationshipid,
      observedvalue,
      context,
      component,
      attribution,
      condition,
      limitations,
      tiekey
    )

  private def _usage(
    id: String,
    association: LifecycleUnifiedEvidenceEvidenceAssociation,
    representation: String = "caller-admitted Usage/Discovery representation",
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    attribution: ComponentDashboardSourceAttribution = _attribution("discovery-source", "discovery://record"),
    condition: ComponentDashboardCondition = _condition(),
    limitations: Vector[String] = Vector("Discovery source limitation"),
    tiekey: Option[String] = Some("usage-tie")
  ): LifecycleUnifiedEvidenceUsageEvidence =
    LifecycleUnifiedEvidenceUsageEvidence(
      id,
      context,
      component,
      association,
      representation,
      attribution,
      condition,
      limitations,
      tiekey
    )

  private def _operation(
    id: String,
    association: LifecycleUnifiedEvidenceEvidenceAssociation,
    representation: String = "caller-admitted Operation representation",
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    attribution: ComponentDashboardSourceAttribution = _attribution("operation-source", "operation://record"),
    condition: ComponentDashboardCondition = _condition(),
    limitations: Vector[String] = Vector("Operation source limitation"),
    tiekey: Option[String] = Some("operation-tie")
  ): LifecycleUnifiedEvidenceOperationEvidence =
    LifecycleUnifiedEvidenceOperationEvidence(
      id,
      context,
      component,
      association,
      representation,
      attribution,
      condition,
      limitations,
      tiekey
    )

  private def _quality(
    id: String,
    association: LifecycleUnifiedEvidenceEvidenceAssociation,
    representation: String = "authorized existing Quality/Review representation",
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    attribution: ComponentDashboardSourceAttribution = _attribution("review-source", "review://record"),
    condition: ComponentDashboardCondition = _condition(),
    limitations: Vector[String] = Vector("Review source limitation"),
    tiekey: Option[String] = Some("quality-tie")
  ): LifecycleUnifiedEvidenceQualityReviewEvidence =
    LifecycleUnifiedEvidenceQualityReviewEvidence(
      id,
      context,
      component,
      association,
      representation,
      attribution,
      condition,
      limitations,
      tiekey
    )

  private def _empty_lane_condition(
    id: String,
    context: MonoKotoProjectionContextIdentity = _context,
    component: ComponentDashboardComponentIdentity = _component,
    attribution: ComponentDashboardSourceAttribution = _attribution("lane-source", "lane://empty"),
    condition: ComponentDashboardCondition = _condition(explicitabsence = Some("caller-admitted empty lane")),
    limitations: Vector[String] = Vector("empty lane limitation"),
    tiekey: Option[String] = Some("empty-lane-tie")
  ): LifecycleUnifiedEvidenceEmptyLaneCondition =
    LifecycleUnifiedEvidenceEmptyLaneCondition(id, context, component, attribution, condition, limitations, tiekey)

  private def _attribution(sourceid: String, locator: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, "caller-admitted authority", locator)

  private def _condition(
    availability: String = "available",
    authorization: String = "admitted",
    redaction: Option[String] = None,
    explicitabsence: Option[String] = None,
    ambiguity: Option[String] = None,
    conflict: Option[String] = None,
    staleness: Option[String] = None,
    malformedevidence: Option[String] = None,
    limitations: Vector[String] = Vector.empty
  ): ComponentDashboardCondition =
    ComponentDashboardCondition(
      availability,
      authorization,
      redaction,
      explicitabsence,
      ambiguity,
      conflict,
      staleness,
      malformedevidence,
      limitations
    )
}
