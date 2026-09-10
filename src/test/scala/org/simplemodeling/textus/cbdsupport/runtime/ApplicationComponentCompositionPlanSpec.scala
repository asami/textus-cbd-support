package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class ApplicationComponentCompositionPlanSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ApplicationComponentCompositionPlan" should {
    "P9-10 transient composition plan" which {
    "preserve distinct catalog and semantic evidence provenance in one transient typed plan" in {
      Given("one application intent, its required capability, and separately attributed catalog and semantic evidence")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val catalog = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val semantic = ComponentEvidence(
        "semantic-order",
        _provenance("semantic", "query:order-processing"),
        _condition("available", "semantic-query", limitations = Vector("candidate-only")),
        Vector(capability.id),
        None
      )

      When("the transient composition-input plan is created")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(capability), Vector(catalog, semantic))

      Then("each evidence record retains its own provenance and condition without a selected component")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(catalog, semantic),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(catalog.id, semantic.id),
              UnresolvedCoverageDispositionOutcome
            )
          )
        )
      )
      result.toOption.toVector.flatMap(_.evidence.map(_.provenance.sourceId)) shouldBe Vector("catalog", "semantic")
      result.toOption.toVector.flatMap(_.evidence.map(_.condition.authorization.value)) shouldBe
        Vector("catalog-read", "semantic-query")
      result.toOption.toVector.flatMap(_.evidence.flatMap(_.componentId)) shouldBe Vector("textus-order")
    }

    "reject duplicate identities and evidence links to an unknown capability without selecting or mutating any source" in {
      Given("duplicate capability identities and evidence that names a capability outside the application intent")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val first = RequiredCapability("order-processing", intent.id, _provenance("application", "capability:first"))
      val duplicate = RequiredCapability("order-processing", intent.id, _provenance("application", "capability:duplicate"))
      val evidence = ComponentEvidence(
        "semantic-order",
        _provenance("semantic", "query:order-processing"),
        _condition("available", "semantic-query"),
        Vector("unknown-capability"),
        None
      )

      When("the transient composition-input plan validates the supplied links")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(first, duplicate), Vector(evidence))

      Then("validation returns the typed failure and does not produce a plan or component selection")
      result.isLeft shouldBe true
      result.left.toOption.toVector.flatMap(_.violations) should contain("Duplicate required capability ID 'order-processing'.")
      result.left.toOption.toVector.flatMap(_.violations) should contain(
        "Component evidence 'semantic-order' names unknown required capability 'unknown-capability'."
      )
    }

    "account for each admitted capability with ordered evidence identities and an unresolved disposition" in {
      Given("two required capabilities and evidence records linked in a different evidence order")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val order = RequiredCapability("order-processing", intent.id, _provenance("application", "capability:order"))
      val fulfillment = RequiredCapability("fulfillment", intent.id, _provenance("application", "capability:fulfillment"))
      val catalog = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(order.id, fulfillment.id),
        Some("textus-order")
      )
      val semantic = ComponentEvidence(
        "semantic-order",
        _provenance("semantic", "query:order-processing"),
        _condition("available", "semantic-query"),
        Vector(order.id),
        None
      )
      val fulfillmentevidence = ComponentEvidence(
        "semantic-fulfillment",
        _provenance("semantic", "query:fulfillment"),
        _condition("available", "semantic-query"),
        Vector(fulfillment.id),
        None
      )

      When("the transient composition plan accounts for its admitted evidence")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(order, fulfillment),
        Vector(catalog, semantic, fulfillmentevidence)
      )

      Then("each capability has one unresolved disposition with only its input evidence identities in evidence order")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(order, fulfillment),
          Vector(catalog, semantic, fulfillmentevidence),
          Vector(
            CoverageDisposition(
              order.id,
              Vector(catalog.id, semantic.id),
              UnresolvedCoverageDispositionOutcome
            ),
            CoverageDisposition(
              fulfillment.id,
              Vector(catalog.id, fulfillmentevidence.id),
              UnresolvedCoverageDispositionOutcome
            )
          )
        )
      )
    }

    }

    "P9-11 deterministic structural coverage" which {
    "classify asserted exact evidence as an alternative and no evidence as an explicit gap" in {
      Given("one evidenced capability, one capability without evidence, and an asserted component identity")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val order = RequiredCapability("order-processing", intent.id, _provenance("application", "capability:order"))
      val invoicing = RequiredCapability("invoicing", intent.id, _provenance("application", "capability:invoicing"))
      val catalog = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(order.id),
        Some("textus-order")
      )

      When("the transient composition plan accounts for its admitted evidence")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(order, invoicing), Vector(catalog))

      Then("the asserted candidate remains an alternative and the absent evidence account becomes a typed gap")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(order, invoicing),
          Vector(catalog),
          Vector(
            CoverageDisposition(
              order.id,
              Vector(catalog.id),
              AlternativeCoverageDispositionOutcome(Vector("textus-order"))
            ),
            CoverageDisposition(
              invoicing.id,
              Vector.empty,
              GapCoverageDispositionOutcome(NoLinkedEvidenceCoverageGapReason)
            )
          )
        )
      )
      result.toOption.toVector.flatMap(_.evidence.flatMap(_.componentId)) shouldBe Vector("textus-order")
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe
        Vector(
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          GapCoverageDispositionOutcome(NoLinkedEvidenceCoverageGapReason)
        )
    }

    "retain deliberately nonalphabetical exact asserted candidates in alternative evidence and component order" in {
      Given("one capability and two exact asserted candidates supplied in deliberately nonalphabetical evidence order")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val zulu = ComponentEvidence(
        "semantic-zulu",
        _provenance("semantic", "component:textus-zulu"),
        _condition("opaque-availability", "opaque-authorization"),
        Vector(capability.id),
        Some("textus-zulu")
      )
      val alpha = ComponentEvidence(
        "catalog-alpha",
        _provenance("catalog", "component:textus-alpha"),
        _condition("opaque-availability", "opaque-authorization"),
        Vector(capability.id),
        Some("textus-alpha")
      )

      When("the transient composition plan classifies the exact asserted candidates")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(capability), Vector(zulu, alpha))

      Then("the single disposition preserves evidence and component input order as an alternative without selection or ranking")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(zulu, alpha),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(zulu.id, alpha.id),
              AlternativeCoverageDispositionOutcome(Vector("textus-zulu", "textus-alpha"))
            )
          )
        )
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.evidenceIds)) shouldBe Vector(Vector("semantic-zulu", "catalog-alpha"))
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe
        Vector(AlternativeCoverageDispositionOutcome(Vector("textus-zulu", "textus-alpha")))
    }

    "classify all explicit absences as an ordered typed gap" in {
      Given("one capability with two linked explicit-absence evidence records in input order")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("reporting", intent.id, _provenance("application", "capability:reporting"))
      val first = ComponentEvidence(
        "catalog-reporting",
        _provenance("catalog", "component:reporting"),
        _condition("opaque-availability", "opaque-authorization", absencereason = Some("not-published")),
        Vector(capability.id),
        Some("textus-reporting")
      )
      val second = ComponentEvidence(
        "semantic-reporting",
        _provenance("semantic", "query:reporting"),
        _condition("opaque-availability", "opaque-authorization", absencereason = Some("no-exact-candidate")),
        Vector(capability.id),
        Some("semantic-reporting")
      )

      When("the transient composition plan classifies the linked evidence")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(capability), Vector(first, second))

      Then("the gap preserves the evidence and explicit absence reason order without selecting a component")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(first, second),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(first.id, second.id),
              GapCoverageDispositionOutcome(
                AllEvidenceAbsentCoverageGapReason(Vector("not-published", "no-exact-candidate"))
              )
            )
          )
        )
      )
    }

    "retain redacted, limited, componentless, and mixed evidence as unresolved" in {
      Given("four capabilities with structural conditions that prevent a stronger coverage outcome")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val redacted = RequiredCapability("redacted", intent.id, _provenance("application", "capability:redacted"))
      val limited = RequiredCapability("limited", intent.id, _provenance("application", "capability:limited"))
      val componentless = RequiredCapability("componentless", intent.id, _provenance("application", "capability:componentless"))
      val mixed = RequiredCapability("mixed", intent.id, _provenance("application", "capability:mixed"))
      val redactedEvidence = ComponentEvidence(
        "redacted-evidence",
        _provenance("catalog", "component:redacted"),
        _condition("opaque", "opaque", redactionreason = Some("restricted-source")),
        Vector(redacted.id),
        Some("textus-redacted")
      )
      val limitedEvidence = ComponentEvidence(
        "limited-evidence",
        _provenance("catalog", "component:limited"),
        _condition("opaque", "opaque", limitations = Vector("incomplete-contract")),
        Vector(limited.id),
        Some("textus-limited")
      )
      val componentlessEvidence = ComponentEvidence(
        "componentless-evidence",
        _provenance("semantic", "query:componentless"),
        _condition("opaque", "opaque"),
        Vector(componentless.id),
        None
      )
      val mixedAbsence = ComponentEvidence(
        "mixed-absence",
        _provenance("catalog", "component:mixed-absence"),
        _condition("opaque", "opaque", absencereason = Some("not-currently-available")),
        Vector(mixed.id),
        Some("textus-mixed-absence")
      )
      val mixedAssertion = ComponentEvidence(
        "mixed-assertion",
        _provenance("catalog", "component:mixed-assertion"),
        _condition("opaque", "opaque"),
        Vector(mixed.id),
        Some("textus-mixed-assertion")
      )

      When("the transient composition plan classifies each structural limitation")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(redacted, limited, componentless, mixed),
        Vector(redactedEvidence, limitedEvidence, componentlessEvidence, mixedAbsence, mixedAssertion)
      )

      Then("every affected capability remains unresolved with all of its evidence identities retained")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(redacted, limited, componentless, mixed),
          Vector(redactedEvidence, limitedEvidence, componentlessEvidence, mixedAbsence, mixedAssertion),
          Vector(
            CoverageDisposition(redacted.id, Vector(redactedEvidence.id), UnresolvedCoverageDispositionOutcome),
            CoverageDisposition(limited.id, Vector(limitedEvidence.id), UnresolvedCoverageDispositionOutcome),
            CoverageDisposition(componentless.id, Vector(componentlessEvidence.id), UnresolvedCoverageDispositionOutcome),
            CoverageDisposition(
              mixed.id,
              Vector(mixedAbsence.id, mixedAssertion.id),
              UnresolvedCoverageDispositionOutcome
            )
          )
        )
      )
    }

    }

    "P9-12 HumanDecision admission" which {
    "admit one explicit exact-scope human decision as an attributable selected outcome" in {
      Given("one capability, two linked exact Component assertions, and an explicit human decision for the first Component")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val selectedcondition = _condition("available", "catalog-read")
      val alternativecondition = _condition("available", "semantic-read", limitations = Vector("independent-review"))
      val selectedevidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        selectedcondition,
        Vector(capability.id),
        Some("textus-order")
      )
      val alternativeevidence = ComponentEvidence(
        "semantic-order",
        _provenance("semantic", "component:textus-order-alternative"),
        alternativecondition,
        Vector(capability.id),
        Some("textus-order-alternative")
      )
      val decision = _decision(
        "decision-order",
        intent.id,
        capability.id,
        "textus-order",
        Vector(selectedevidence.id),
        conditions = Vector("architecture-review-complete"),
        limitations = Vector("revisit-on-catalog-revision")
      )

      When("the transient plan admits the explicit decision against its considered linked evidence")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(selectedevidence, alternativeevidence),
        Vector(decision)
      )

      Then("only the addressed disposition is selected and retains its decision, target, evidence, and condition attribution")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(selectedevidence, alternativeevidence),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(selectedevidence.id, alternativeevidence.id),
              SelectedCoverageDispositionOutcome(
                decision.id,
                decision.componentId,
                decision.actor,
                decision.rationale,
                decision.provenance,
                decision.consideredEvidenceIds,
                Vector(selectedevidence.id),
                Vector(selectedevidence.id, alternativeevidence.id),
                Vector(selectedcondition, alternativecondition),
                decision.conditions,
                decision.limitations
              ),
              Some(decision.id)
            )
          ),
          Vector.empty
        )
      )
    }

    "preserve the P9-11 alternative disposition when no human decision is supplied" in {
      Given("one capability and one exact asserted Component without a decision input")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )

      When("the plan receives an explicitly empty decision collection")
      val result = ApplicationComponentCompositionPlan.create(intent, Vector(capability), Vector(evidence), Vector.empty)

      Then("the P9-11 alternative remains visible with neither a selected outcome nor an admitted decision identity")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(evidence),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(evidence.id),
              AlternativeCoverageDispositionOutcome(Vector("textus-order")),
              None
            )
          ),
          Vector.empty
        )
      )
    }

    "retain P9-11 dispositions and typed admission failures for invalid decision scope target and considered evidence" in {
      Given("two capabilities, exact evidence, and decisions with an outside scope, unmatched target, and cross-scope considered evidence")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val order = RequiredCapability("order-processing", intent.id, _provenance("application", "capability:order"))
      val fulfillment = RequiredCapability("fulfillment", intent.id, _provenance("application", "capability:fulfillment"))
      val orderevidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(order.id),
        Some("textus-order")
      )
      val fulfillmentevidence = ComponentEvidence(
        "catalog-fulfillment",
        _provenance("catalog", "component:textus-fulfillment"),
        _condition("available", "catalog-read"),
        Vector(fulfillment.id),
        Some("textus-fulfillment")
      )
      val outsidescope = _decision(
        "decision-scope",
        intent.id,
        "missing-capability",
        "textus-order",
        Vector(orderevidence.id)
      )
      val unmatchedtarget = _decision(
        "decision-target",
        intent.id,
        fulfillment.id,
        "textus-unmatched",
        Vector(fulfillmentevidence.id)
      )
      val crossscopeevidence = _decision(
        "decision-evidence",
        intent.id,
        order.id,
        "textus-order",
        Vector(fulfillmentevidence.id)
      )

      When("the plan receives decisions that cannot be admitted")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(order, fulfillment),
        Vector(orderevidence, fulfillmentevidence),
        Vector(outsidescope, unmatchedtarget, crossscopeevidence)
      )

      Then("each invalid decision becomes a typed admission failure while both original alternatives remain visible without selection")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          order.id,
          Vector(orderevidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        ),
        CoverageDisposition(
          fulfillment.id,
          Vector(fulfillmentevidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-fulfillment")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.map(_.decision.id)) shouldBe
        Vector(outsidescope.id, unmatchedtarget.id, crossscopeevidence.id)
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Human decision 'decision-scope' names unknown required capability 'missing-capability'."
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Human decision 'decision-target' target component 'textus-unmatched' is not asserted by its considered linked evidence."
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Human decision 'decision-evidence' considered component evidence 'catalog-fulfillment' is not linked to required capability 'order-processing'."
      )
    }

    "reject duplicate or blank decision authority inputs without selecting an asserted Component" in {
      Given("one exact Component assertion and duplicate decisions plus a decision with a blank required actor")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val first = _decision("decision-duplicate", intent.id, capability.id, "textus-order", Vector(evidence.id))
      val duplicate = _decision("decision-duplicate", intent.id, capability.id, "textus-order", Vector(evidence.id))
      val blankactor = _decision("decision-blank-actor", intent.id, capability.id, "textus-order", Vector(evidence.id), actor = "")

      When("the plan evaluates the duplicate and blank authority inputs")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector(first, duplicate, blankactor)
      )

      Then("every authority input is rejected as a typed admission failure and the structural alternative is retained")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          capability.id,
          Vector(evidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.map(_.decision.id)) shouldBe
        Vector(first.id, duplicate.id, blankactor.id)
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Duplicate human decision ID 'decision-duplicate'."
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Duplicate human decision scope intent 'order-service' capability 'order-processing'."
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "human decision 'decision-blank-actor' actor must not be blank."
      )
    }

    "treat opaque source order provider score and navigation values as evidence attribution without a selected outcome" in {
      Given("two deliberately ordered exact candidates whose opaque evidence strings resemble provider score and Dashboard navigation data")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val providerattribution = ComponentEvidence(
        "provider-zulu",
        CompositionProvenance("provider-output:rank-1", "score:999", "dashboard:component/textus-zulu"),
        _condition("opaque-provider-availability", "opaque-provider-authorization"),
        Vector(capability.id),
        Some("textus-zulu")
      )
      val navigationattribution = ComponentEvidence(
        "navigation-alpha",
        CompositionProvenance("dashboard-link:rank-2", "display-order:1", "discovery:component/textus-alpha"),
        _condition("opaque-navigation-availability", "opaque-navigation-authorization"),
        Vector(capability.id),
        Some("textus-alpha")
      )

      When("the plan receives no HumanDecision beside the opaque attributed evidence")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(providerattribution, navigationattribution),
        Vector.empty
      )

      Then("input values and order remain attributable alternatives and cannot infer an admitted decision or selected Component")
      result shouldBe Right(
        ApplicationComponentCompositionPlan(
          intent,
          Vector(capability),
          Vector(providerattribution, navigationattribution),
          Vector(
            CoverageDisposition(
              capability.id,
              Vector(providerattribution.id, navigationattribution.id),
              AlternativeCoverageDispositionOutcome(Vector("textus-zulu", "textus-alpha")),
              None
            )
          ),
          Vector.empty
        )
      )
    }

    }

    "P9-13 advisory provider and ComponentProposal admission" which {
    "admit an available version-matching provider proposal while retaining the P9-11 alternative baseline and attribution" in {
      Given("one asserted existing Component, one typed available provider, and one exact attributable proposal")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val condition = _condition("catalog-available", "catalog-read")
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        condition,
        Vector(capability.id),
        Some("textus-order")
      )
      val provider = _provider("composition-advisor", "1.0")
      val proposal = _proposal(
        "proposal-order",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id),
        conditions = Vector("review-before-promotion"),
        limitations = Vector("advisory-only")
      )

      When("the transient plan admits the provider-attributed proposal without a HumanDecision")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector.empty,
        Vector(provider),
        Vector(proposal)
      )

      Then("the proposal outcome retains the structural alternative and complete provider proposal and evidence attribution without selection")
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe Vector(
        ProposalCoverageDispositionOutcome(
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          Vector(
            AdmittedComponentProposal(
              proposal,
              provider,
              Vector(evidence.id),
              Vector(evidence.id),
              Vector(condition)
            )
          )
        )
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
      result.toOption.toVector.flatMap(_.providerAdmissionFailures) shouldBe Vector.empty
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures) shouldBe Vector.empty
    }

    "retain the structural baseline and typed failures for unavailable unknown version-mismatched out-of-scope and unlinked provider proposals" in {
      Given("one asserted Component, an unavailable provider, and proposal inputs that each violate a separate admission boundary")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("catalog-available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val available = _provider("composition-advisor", "1.0")
      val unavailable = _provider(
        "offline-advisor",
        "1.0",
        UnavailableAdvisoryProviderAvailability("provider-maintenance")
      )
      val unavailableproposal = _proposal(
        "proposal-unavailable",
        unavailable.id,
        unavailable.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val unknownprovider = _proposal(
        "proposal-unknown-provider",
        "missing-advisor",
        "1.0",
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val versionmismatch = _proposal(
        "proposal-version-mismatch",
        available.id,
        "2.0",
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val outofscope = _proposal(
        "proposal-outside-intent",
        available.id,
        available.contractVersion,
        "other-application",
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val unlinked = _proposal(
        "proposal-unknown-evidence",
        available.id,
        available.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector("missing-evidence")
      )

      When("the transient plan evaluates invalid provider-proposal combinations")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector.empty,
        Vector(available, unavailable),
        Vector(unavailableproposal, unknownprovider, versionmismatch, outofscope, unlinked)
      )

      Then("the alternative remains visible while typed provider and proposal failure accounting retains every rejected input in input order")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          capability.id,
          Vector(evidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.map(_.provider.id)) shouldBe Vector(unavailable.id)
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.map(_.proposal.id)) shouldBe
        Vector(unavailableproposal.id, unknownprovider.id, versionmismatch.id, outofscope.id, unlinked.id)
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal 'proposal-unavailable' names unavailable advisory provider 'offline-advisor'."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal 'proposal-version-mismatch' provider contract version '2.0' does not match advisory provider 'composition-advisor'."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal 'proposal-outside-intent' is outside application intent 'order-service'."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal 'proposal-unknown-evidence' names unknown considered component evidence 'missing-evidence'."
      )
    }

    "reject duplicate provider and proposal identities without replacing the P9-11 alternative or selecting a Component" in {
      Given("one asserted Component plus duplicate advisory providers and duplicate attributable proposals")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("catalog-available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val firstprovider = _provider("composition-advisor", "1.0")
      val duplicateprovider = _provider("composition-advisor", "1.0")
      val firstproposal = _proposal(
        "proposal-order",
        firstprovider.id,
        firstprovider.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val duplicateproposal = _proposal(
        "proposal-order",
        firstprovider.id,
        firstprovider.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )

      When("the plan admits the duplicate provider and proposal identities")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector.empty,
        Vector(firstprovider, duplicateprovider),
        Vector(firstproposal, duplicateproposal)
      )

      Then("typed provider and proposal failures retain the baseline alternative and leave every disposition unselected")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          capability.id,
          Vector(evidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.map(_.provider.id)) shouldBe
        Vector(firstprovider.id, duplicateprovider.id)
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.map(_.proposal.id)) shouldBe
        Vector(firstproposal.id, duplicateproposal.id)
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.flatMap(_.violations)) should contain(
        "Duplicate advisory provider ID 'composition-advisor'."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Duplicate component proposal ID 'proposal-order'."
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
    }

    "reject blank mandatory provider and proposal fields without replacing the P9-11 alternative or selecting a Component" in {
      Given("one asserted Component and provider-proposal inputs with blank mandatory identity and attribution fields")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("catalog-available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val blankprovider = _provider("", "")
      val blankproposal = _proposal("", "", "", "", "", "", Vector.empty, rationale = "")

      When("the plan admits the blank mandatory provider and proposal inputs")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector.empty,
        Vector(blankprovider),
        Vector(blankproposal)
      )

      Then("typed admission failures preserve the baseline alternative and no Component becomes selected")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          capability.id,
          Vector(evidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.map(_.provider.id)) shouldBe Vector(blankprovider.id)
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.map(_.proposal.id)) shouldBe Vector(blankproposal.id)
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.flatMap(_.violations)) should contain(
        "advisory provider ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.providerAdmissionFailures.flatMap(_.violations)) should contain(
        "advisory provider '' contract version must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' advisory provider ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' provider contract version must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' application intent ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' required capability ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' component ID must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "component proposal '' rationale must not be blank."
      )
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal '' must name considered component evidence."
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
    }

    "reject a proposal target that differs from its known linked evidence without replacing the P9-11 alternative or selecting a Component" in {
      Given("one known linked exact assertion and an available provider proposal that targets a different Component")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("catalog-available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val provider = _provider("composition-advisor", "1.0")
      val proposal = _proposal(
        "proposal-different-target",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-unasserted",
        Vector(evidence.id)
      )

      When("the plan evaluates the proposal target against the known linked considered evidence")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector.empty,
        Vector(provider),
        Vector(proposal)
      )

      Then("the typed proposal failure retains the asserted alternative and does not select the proposal target")
      result.toOption.toVector.flatMap(_.dispositions) shouldBe Vector(
        CoverageDisposition(
          capability.id,
          Vector(evidence.id),
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          None
        )
      )
      result.toOption.toVector.flatMap(_.providerAdmissionFailures) shouldBe Vector.empty
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.map(_.proposal.id)) shouldBe Vector(proposal.id)
      result.toOption.toVector.flatMap(_.proposalAdmissionFailures.flatMap(_.violations)) should contain(
        "Component proposal 'proposal-different-target' target component 'textus-unasserted' is not asserted by its considered linked evidence."
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
    }

    "retain multiple admitted proposals in input order without selecting or ranking one" in {
      Given("two exact asserted Components, one available provider, and two reverse-alphabetical attributable proposals")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val zulucondition = _condition("available", "catalog-read")
      val alphacondition = _condition("available", "semantic-read")
      val zulu = ComponentEvidence(
        "evidence-zulu",
        _provenance("catalog", "component:textus-zulu"),
        zulucondition,
        Vector(capability.id),
        Some("textus-zulu")
      )
      val alpha = ComponentEvidence(
        "evidence-alpha",
        _provenance("catalog", "component:textus-alpha"),
        alphacondition,
        Vector(capability.id),
        Some("textus-alpha")
      )
      val provider = _provider("composition-advisor", "1.0")
      val first = _proposal(
        "proposal-zulu",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-zulu",
        Vector(zulu.id)
      )
      val second = _proposal(
        "proposal-alpha",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-alpha",
        Vector(alpha.id)
      )

      When("the transient plan receives the proposals in zulu then alpha input order without a decision")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(zulu, alpha),
        Vector.empty,
        Vector(provider),
        Vector(first, second)
      )

      Then("the proposal outcome retains both proposals and their matching evidence in input order without a winner or selected identity")
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe Vector(
        ProposalCoverageDispositionOutcome(
          AlternativeCoverageDispositionOutcome(Vector("textus-zulu", "textus-alpha")),
          Vector(
            AdmittedComponentProposal(first, provider, Vector(zulu.id), Vector(zulu.id, alpha.id), Vector(zulucondition, alphacondition)),
            AdmittedComponentProposal(second, provider, Vector(alpha.id), Vector(zulu.id, alpha.id), Vector(zulucondition, alphacondition))
          )
        )
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
    }

    "allow a valid HumanDecision to select only with exact admitted proposal promotion attribution" in {
      Given("one asserted Component, an admitted proposal, and an evidence-backed HumanDecision that explicitly promotes it")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val condition = _condition("available", "catalog-read")
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        condition,
        Vector(capability.id),
        Some("textus-order")
      )
      val provider = _provider("composition-advisor", "1.0")
      val proposal = _proposal(
        "proposal-order",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val decision = _decision(
        "decision-order",
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id),
        promotedproposalid = Some(proposal.id)
      )

      When("the plan admits the human decision after admitting its exact proposal attribution")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector(decision),
        Vector(provider),
        Vector(proposal)
      )

      Then("only the HumanDecision produces selected while retaining the promoted proposal identity as accountable attribution")
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe Vector(
        SelectedCoverageDispositionOutcome(
          decision.id,
          decision.componentId,
          decision.actor,
          decision.rationale,
          decision.provenance,
          decision.consideredEvidenceIds,
          Vector(evidence.id),
          Vector(evidence.id),
          Vector(condition),
          decision.conditions,
          decision.limitations,
          Some(proposal.id)
        )
      )
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures) shouldBe Vector.empty
    }

    "reject an unadmitted proposal promotion reference without selecting a Component" in {
      Given("one asserted Component, an available provider proposal, and a HumanDecision that names no admitted proposal")
      val intent = ApplicationIntent("order-service", "application:order-service", _provenance("application", "intent"))
      val capability = RequiredCapability("order-processing", intent.id, _provenance("application", "capability"))
      val evidence = ComponentEvidence(
        "catalog-order",
        _provenance("catalog", "component:textus-order"),
        _condition("available", "catalog-read"),
        Vector(capability.id),
        Some("textus-order")
      )
      val provider = _provider("composition-advisor", "1.0")
      val proposal = _proposal(
        "proposal-order",
        provider.id,
        provider.contractVersion,
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id)
      )
      val decision = _decision(
        "decision-order",
        intent.id,
        capability.id,
        "textus-order",
        Vector(evidence.id),
        promotedproposalid = Some("missing-proposal")
      )

      When("the plan evaluates the invalid promotion reference")
      val result = ApplicationComponentCompositionPlan.create(
        intent,
        Vector(capability),
        Vector(evidence),
        Vector(decision),
        Vector(provider),
        Vector(proposal)
      )

      Then("the decision is a typed failure and the unapproved admitted proposal retains the alternative baseline without selection")
      result.toOption.toVector.flatMap(_.dispositions.map(_.outcome)) shouldBe Vector(
        ProposalCoverageDispositionOutcome(
          AlternativeCoverageDispositionOutcome(Vector("textus-order")),
          Vector(
            AdmittedComponentProposal(
              proposal,
              provider,
              Vector(evidence.id),
              Vector(evidence.id),
              Vector(evidence.condition)
            )
          )
        )
      )
      result.toOption.toVector.flatMap(_.dispositions.map(_.decisionId)) shouldBe Vector(None)
      result.toOption.toVector.flatMap(_.decisionAdmissionFailures.flatMap(_.violations)) should contain(
        "Human decision 'decision-order' names unadmitted component proposal 'missing-proposal'."
      )
    }
    }
  }

  private def _provenance(sourceid: String, locator: String): CompositionProvenance =
    CompositionProvenance(sourceid, s"$sourceid-authority", locator)

  private def _condition(
    availability: String,
    authorization: String,
    redactionreason: Option[String] = None,
    absencereason: Option[String] = None,
    limitations: Vector[String] = Vector.empty
  ): CompositionEvidenceCondition =
    CompositionEvidenceCondition(
      CompositionEvidenceAvailability(availability),
      CompositionEvidenceAuthorization(authorization),
      redactionreason,
      absencereason,
      limitations
    )

  private def _decision(
    decisionid: String,
    intentid: String,
    capabilityid: String,
    componentid: String,
    consideredevidenceids: Vector[String],
    actor: String = "architecture-reviewer",
    rationale: String = "approved-for-application-composition",
    provenance: CompositionProvenance = _provenance("human", "architecture-review"),
    conditions: Vector[String] = Vector.empty,
    limitations: Vector[String] = Vector.empty,
    promotedproposalid: Option[String] = None
  ): HumanDecision =
    HumanDecision(
      decisionid,
      intentid,
      capabilityid,
      componentid,
      actor,
      rationale,
      provenance,
      consideredevidenceids,
      conditions,
      limitations,
      promotedproposalid
    )

  private def _provider(
    providerid: String,
    contractversion: String,
    availability: AdvisoryProviderAvailability = AvailableAdvisoryProviderAvailability,
    limitations: Vector[String] = Vector("provider-limitation")
  ): AdvisoryProviderContract =
    AdvisoryProviderContract(
      providerid,
      contractversion,
      _provenance("provider", s"provider:$providerid"),
      availability,
      limitations
    )

  private def _proposal(
    proposalid: String,
    providerid: String,
    providerversion: String,
    intentid: String,
    capabilityid: String,
    componentid: String,
    consideredevidenceids: Vector[String],
    rationale: String = "provider-suggested-existing-component",
    provenance: CompositionProvenance = _provenance("provider", "composition-proposal"),
    conditions: Vector[String] = Vector.empty,
    limitations: Vector[String] = Vector.empty
  ): ComponentProposal =
    ComponentProposal(
      proposalid,
      providerid,
      providerversion,
      intentid,
      capabilityid,
      componentid,
      rationale,
      provenance,
      consideredevidenceids,
      conditions,
      limitations
    )
}
