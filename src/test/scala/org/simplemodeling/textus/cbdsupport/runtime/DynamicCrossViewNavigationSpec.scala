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
final class DynamicCrossViewNavigationSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "DynamicCrossViewNavigation" should {
    "P9-53B typed immutable dynamic cross-view navigation" which {
      "retain every caller-admitted dynamic category and role without reconstructing records or mappings" in {
        Given("one exact Component and context with independently admitted Koto, Event, Workflow, StateMachine, and Entity records")
        val component = _component()
        val context = _context()
        val records = Vector(
          _record("mono-subject", context, component, "order", DynamicKoto, DynamicKotoSubject),
          _record("mono-reference", context, component, "order", DynamicKoto, DynamicKotoReference),
          _record("event-subject", context, component, "order", DynamicEvent, DynamicEventSubject),
          _record("event-assertion", context, component, "order", DynamicEvent, DynamicEventAssertion),
          _record("event-endpoint", context, component, "order", DynamicEvent, DynamicEventEndpoint),
          _record("workflow-subject", context, component, "order", DynamicWorkflow, DynamicWorkflowSubject),
          _record("workflow-flow", context, component, "order", DynamicWorkflow, DynamicWorkflowFlow),
          _record("workflow-endpoint", context, component, "order", DynamicWorkflow, DynamicWorkflowEndpoint),
          _record("state-machine-subject", context, component, "order", DynamicStateMachine, DynamicStateMachineSubject),
          _record("state-machine-transition", context, component, "order", DynamicStateMachine, DynamicStateMachineTransition),
          _record("state-machine-adjunct", context, component, "order", DynamicStateMachine, DynamicStateMachineTransitionAdjunct),
          _record("state-machine-endpoint", context, component, "order", DynamicStateMachine, DynamicStateMachineEndpoint),
          _record("entity-subject", context, component, "order", DynamicEntity, DynamicEntitySubject),
          _record("entity-metadata", context, component, "order", DynamicEntity, DynamicEntityMetadata)
        )
        val mappings = Vector(
          _mapping("mapping-koto-event", context, component, records(0), records(2)),
          _mapping("mapping-event-workflow", context, component, records(3), records(5)),
          _mapping("mapping-workflow-state-machine", context, component, records(6), records(8)),
          _mapping("mapping-state-machine-entity", context, component, records(10), records(12))
        )
        val gapcondition = _condition(availability = "unavailable", authorization = "denied", limitations = Vector("receiving-contract-not-implemented"))
        val gaps = Vector(
          _gap(
            "gap-koto-entity",
            context,
            component,
            records(1),
            DynamicEntity,
            DynamicEntityMetadata,
            Some(ComponentDashboardSemanticTargetIdentity("order")),
            gapcondition
          )
        )

        When("the caller-admitted identity index is assembled")
        val result = DynamicCrossViewNavigation.create(context, component, records.reverse, mappings.reverse, gaps)

        Then("the exact records, roles, attribution locators, conditions, mappings, and bounded gap remain independently retained")
        val projectedrecords = result.toOption.toVector.flatMap(_.records)
        projectedrecords.map(record => record.category -> record.role).toSet shouldBe Set(
          DynamicKoto -> DynamicKotoSubject,
          DynamicKoto -> DynamicKotoReference,
          DynamicEvent -> DynamicEventSubject,
          DynamicEvent -> DynamicEventAssertion,
          DynamicEvent -> DynamicEventEndpoint,
          DynamicWorkflow -> DynamicWorkflowSubject,
          DynamicWorkflow -> DynamicWorkflowEndpoint,
          DynamicWorkflow -> DynamicWorkflowFlow,
          DynamicStateMachine -> DynamicStateMachineSubject,
          DynamicStateMachine -> DynamicStateMachineTransition,
          DynamicStateMachine -> DynamicStateMachineTransitionAdjunct,
          DynamicStateMachine -> DynamicStateMachineEndpoint,
          DynamicEntity -> DynamicEntitySubject,
          DynamicEntity -> DynamicEntityMetadata
        )
        projectedrecords.map(_.id) shouldBe Vector(
          "mono-subject",
          "mono-reference",
          "event-subject",
          "event-assertion",
          "event-endpoint",
          "workflow-subject",
          "workflow-flow",
          "workflow-endpoint",
          "state-machine-subject",
          "state-machine-transition",
          "state-machine-adjunct",
          "state-machine-endpoint",
          "entity-subject",
          "entity-metadata"
        )
        projectedrecords.find(_.id == "entity-metadata").map(_.attribution.sourceLocator) shouldBe Some("source:entity-metadata")
        projectedrecords.find(_.id == "event-assertion").map(_.condition) shouldBe Some(_condition())
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-koto-event", "mapping-event-workflow", "mapping-workflow-state-machine", "mapping-state-machine-entity")
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceRecord.id)) shouldBe
          Vector("mono-subject", "event-assertion", "workflow-flow", "state-machine-adjunct")
        result.toOption.toVector.flatMap(_.mappings.map(_.counterpartRecord.id)) shouldBe
          Vector("event-subject", "workflow-subject", "state-machine-subject", "entity-subject")
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap)) shouldBe gaps
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.knownCounterpartSemanticTargetId)) shouldBe
          Vector(Some(ComponentDashboardSemanticTargetIdentity("order")))
      }

      "retain attributable bounded gaps without inferring a counterpart, proxy, lookup, or navigation target" in {
        Given("one exact retained Koto source and an unavailable Workflow counterpart gap")
        val component = _component()
        val context = _context()
        val source = _record("mono-subject", context, component, "order", DynamicKoto, DynamicKotoSubject)
        val condition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-source-does-not-state-a-workflow-dimension"),
          limitations = Vector("workflow-receiver-unimplemented")
        )
        val gap = _gap(
          "gap-workflow-dimension",
          context,
          component,
          source,
          DynamicWorkflow,
          DynamicWorkflowEndpoint,
          None,
          condition
        )

        When("the source and bounded unavailable gap are assembled")
        val result = DynamicCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(gap))

        Then("the gap retains only its source and limitation information")
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap)) shouldBe Vector(gap)
        result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.knownCounterpartSemanticTargetId)) shouldBe Vector(None)
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "present records mappings and gaps by identity category role record identity and admitted tie key independently of caller permutation" in {
        Given("two independently admitted shared identities with records mappings and gaps in arbitrary caller order")
        val component = _component()
        val context = _context()
        val monoalpha = _record("mono-alpha", context, component, "alpha", DynamicKoto, DynamicKotoSubject, stabletiekey = Some("z"))
        val entityalpha = _record("entity-alpha", context, component, "alpha", DynamicEntity, DynamicEntitySubject, stabletiekey = Some("a"))
        val eventbeta = _record("event-beta", context, component, "beta", DynamicEvent, DynamicEventSubject)
        val workflowbeta = _record("workflow-beta", context, component, "beta", DynamicWorkflow, DynamicWorkflowFlow)
        val records = Vector(monoalpha, entityalpha, eventbeta, workflowbeta)
        val mappings = Vector(
          _mapping("mapping-beta", context, component, eventbeta, workflowbeta, stabletiekey = Some("b")),
          _mapping("mapping-alpha", context, component, monoalpha, entityalpha, stabletiekey = Some("a"))
        )
        val gaps = Vector(
          _gap("gap-beta", context, component, eventbeta, DynamicEntity, DynamicEntityMetadata, None, _condition(), stabletiekey = Some("b")),
          _gap("gap-alpha", context, component, monoalpha, DynamicWorkflow, DynamicWorkflowEndpoint, None, _condition(), stabletiekey = Some("a"))
        )

        When("ScalaCheck generates independent record mapping and gap permutations")
        forAll(Gen.pick(records.size, records), Gen.pick(mappings.size, mappings), Gen.pick(gaps.size, gaps)) { (recordpermutation, mappingpermutation, gappermutation) =>
          val result = DynamicCrossViewNavigation.create(
            context,
            component,
            recordpermutation.toVector,
            mappingpermutation.toVector,
            gappermutation.toVector
          )

          Then("only exact identities categories roles record identities and admitted tie keys choose every retained presentation order")
          result.toOption.toVector.flatMap(_.records.map(_.id)) shouldBe
            Vector("mono-alpha", "entity-alpha", "event-beta", "workflow-beta")
          result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
            Vector("mapping-alpha", "mapping-beta")
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe
            Vector("gap-alpha", "gap-beta")
        }
      }

      "gate forward and reverse navigation independently while retaining valid mappings when receiving implementation is unavailable" in {
        Given("one exact Koto to Entity mapping with independent forward and reverse implementation gates")
        val component = _component()
        val context = _context()
        val source = _record("mono-order", context, component, "order", DynamicKoto, DynamicKotoSubject)
        val counterpart = _record("entity-order", context, component, "order", DynamicEntity, DynamicEntitySubject)
        val forwardonly = _mapping(
          "mapping-forward-suppressed",
          context,
          component,
          source,
          counterpart,
          forwardimplemented = false,
          reverseimplemented = true
        )
        val reverseonly = _mapping(
          "mapping-reverse-suppressed",
          context,
          component,
          source,
          counterpart,
          forwardimplemented = true,
          reverseimplemented = false
        )

        When("the mappings have unusable receiving contracts in one direction only")
        val result = DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(forwardonly, reverseonly), Vector.empty)

        Then("only the relevant forward or reverse affordance is suppressed and both mappings remain retained")
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-forward-suppressed", "mapping-reverse-suppressed")
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-forward-suppressed").flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-forward-suppressed").flatMap(_.reverseNavigationTarget)) should have size 1
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-reverse-suppressed").flatMap(_.forwardNavigationTarget)) should have size 1
        result.toOption.toVector.flatMap(_.mappings.find(_.sourceMapping.id == "mapping-reverse-suppressed").flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "suppress affordances for source counterpart or mapping conditions without deleting retained identity records or mappings" in {
        Given("otherwise exact mappings whose source counterpart or mapping condition is not usable")
        val component = _component()
        val context = _context()
        val usablemono = _record("mono-order", context, component, "order", DynamicKoto, DynamicKotoSubject)
        val usableentity = _record("entity-order", context, component, "order", DynamicEntity, DynamicEntitySubject)
        val conditionedsource = usablemono.copy(id = "mono-denied", condition = _condition(authorization = "denied"))
        val conditionedcounterpart = usableentity.copy(id = "entity-limited", condition = _condition(limitations = Vector("counterpart-limited")))
        val mappings = Vector(
          _mapping("mapping-source-condition", context, component, conditionedsource, usableentity),
          _mapping("mapping-counterpart-condition", context, component, usablemono, conditionedcounterpart),
          _mapping("mapping-condition", context, component, usablemono, usableentity, condition = _condition(availability = "unavailable"))
        )
        val records = Vector(conditionedsource, usableentity, usablemono, conditionedcounterpart)

        When("the exact condition gates are evaluated after all records and mappings are retained")
        val result = DynamicCrossViewNavigation.create(context, component, records, mappings, Vector.empty)

        Then("each condition suppresses navigation only and preserves the caller-admitted mappings")
        result.toOption.toVector.flatMap(_.records.map(_.id)).toSet shouldBe records.map(_.id).toSet
        result.toOption.toVector.flatMap(_.mappings.map(_.sourceMapping.id)) shouldBe
          Vector("mapping-source-condition", "mapping-counterpart-condition", "mapping-condition")
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.forwardNavigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.mappings.flatMap(_.reverseNavigationTarget)) shouldBe Vector.empty
      }

      "reject invalid scope identity locator tie mapping gap and navigation input as one typed no-partial failure" in {
        Given("otherwise caller-admitted records plus isolated invalid admission variants")
        val component = _component()
        val context = _context()
        val source = _record("mono-order", context, component, "order", DynamicKoto, DynamicKotoSubject)
        val counterpart = _record("entity-order", context, component, "order", DynamicEntity, DynamicEntitySubject)
        val mapping = _mapping("mapping-order", context, component, source, counterpart)
        val samemono = _record("mono-reference", context, component, "order", DynamicKoto, DynamicKotoReference)
        val otheridentity = counterpart.copy(id = "entity-payment", semanticTargetId = ComponentDashboardSemanticTargetIdentity("payment"))
        val unadmittedeventcounterpartgap = _gap(
          "gap-unadmitted-event-counterpart",
          context,
          component,
          source,
          DynamicEvent,
          DynamicEventAssertion,
          Some(ComponentDashboardSemanticTargetIdentity("order")),
          _condition()
        )
        val malformedidentity = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetIdentity = ComponentDashboardSemanticTargetIdentity("other")))
        )
        val malformedcomponent = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetComponent = ComponentDashboardComponentIdentity("other-component")))
        )
        val malformedattribution = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetAttribution = ComponentDashboardSourceAttribution("other", "other", "other:locator")))
        )
        val malformedcondition = mapping.copy(
          forwardNavigationTarget = mapping.forwardNavigationTarget.map(_.copy(targetCondition = _condition(limitations = Vector("different"))))
        )
        val invalidresults = Vector(
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(id = " ")), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(MonoKotoProjectionContextIdentity(" "), component, Vector(source), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(context = MonoKotoProjectionContextIdentity("other-context"))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(context = MonoKotoProjectionContextIdentity(" "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(component = ComponentDashboardComponentIdentity("other-component"))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(component = ComponentDashboardComponentIdentity(" "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(semanticTargetId = ComponentDashboardSemanticTargetIdentity(" "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(attribution = source.attribution.copy(sourceLocator = " "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(condition = _condition(availability = " "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(stableTieKey = Some(" "))), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, source), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping, mapping), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(id = source.id)), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-duplicate", context, component, source, DynamicEntity, DynamicEntitySubject, None, _condition()), _gap("gap-duplicate", context, component, source, DynamicEntity, DynamicEntitySubject, None, _condition()))),
          DynamicCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-locator", context, component, source, DynamicEntity, DynamicEntitySubject, None, _condition()).copy(attribution = ComponentDashboardSourceAttribution("gap", "authority", " ")))),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(sourceRecordId = "missing")), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(mapping.copy(context = MonoKotoProjectionContextIdentity("other-context"))), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(_mapping("mapping-self", context, component, source, source)), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, samemono), Vector(_mapping("mapping-same-category", context, component, source, samemono)), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, otheridentity), Vector(_mapping("mapping-different-identity", context, component, source, otheridentity)), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source.copy(category = DynamicEntity)), Vector.empty, Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-same-category", context, component, source, DynamicKoto, DynamicKotoReference, None, _condition()))),
          DynamicCrossViewNavigation.create(context, component, Vector(source), Vector.empty, Vector(_gap("gap-different-identity", context, component, source, DynamicEntity, DynamicEntitySubject, Some(ComponentDashboardSemanticTargetIdentity("other")), _condition()))),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector.empty, Vector(unadmittedeventcounterpartgap)),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedidentity), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedcomponent), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedattribution), Vector.empty),
          DynamicCrossViewNavigation.create(context, component, Vector(source, counterpart), Vector(malformedcondition), Vector.empty)
        )

        When("each malformed input is admitted")
        val failures = invalidresults.flatMap(_.left.toOption)

        Then("every invalid admission returns a typed failure and none returns a partially projected index")
        failures should have size invalidresults.size
        failures.flatMap(_.violations) should not be empty
        invalidresults.flatMap(_.toOption) shouldBe Vector.empty
      }
    }
  }

  private def _component(): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity("textus-dynamic-navigation")

  private def _context(): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity("ccdm-dynamic-navigation")

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

  private def _record(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    category: DynamicCrossViewProjectionCategory,
    role: DynamicCrossViewRecordRole,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution("source", "authority", "source:record"),
    condition: ComponentDashboardCondition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty),
    stabletiekey: Option[String] = None
  ): DynamicCrossViewIdentityRecord =
    DynamicCrossViewIdentityRecord(
      id,
      context,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      category,
      role,
      attribution.copy(sourceId = s"source-$id", sourceLocator = s"source:$id"),
      condition,
      stabletiekey
    )

  private def _navigation(
    record: DynamicCrossViewIdentityRecord,
    implemented: Boolean = true
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      record.semanticTargetId,
      record.component,
      record.attribution,
      record.condition,
      implemented
    )

  private def _mapping(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    source: DynamicCrossViewIdentityRecord,
    counterpart: DynamicCrossViewIdentityRecord,
    condition: ComponentDashboardCondition = ComponentDashboardCondition("available", "admitted", None, None, None, None, None, None, Vector.empty),
    stabletiekey: Option[String] = None,
    forwardimplemented: Boolean = true,
    reverseimplemented: Boolean = true
  ): DynamicCrossViewMapping =
    DynamicCrossViewMapping(
      id,
      context,
      component,
      source.id,
      counterpart.id,
      ComponentDashboardSourceAttribution(s"mapping-$id", "mapping-authority", s"mapping:$id"),
      condition,
      stabletiekey,
      Some(_navigation(counterpart, forwardimplemented)),
      Some(_navigation(source, reverseimplemented))
    )

  private def _gap(
    id: String,
    context: MonoKotoProjectionContextIdentity,
    component: ComponentDashboardComponentIdentity,
    source: DynamicCrossViewIdentityRecord,
    requestedcategory: DynamicCrossViewProjectionCategory,
    requestedrole: DynamicCrossViewRecordRole,
    knownidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition,
    stabletiekey: Option[String] = None
  ): DynamicCrossViewGap =
    DynamicCrossViewGap(
      id,
      context,
      component,
      source.id,
      requestedcategory,
      requestedrole,
      knownidentity,
      ComponentDashboardSourceAttribution(s"gap-$id", "gap-authority", s"gap:$id"),
      condition,
      "The bounded counterpart receiving contract is not implemented.",
      stabletiekey
    )
}
