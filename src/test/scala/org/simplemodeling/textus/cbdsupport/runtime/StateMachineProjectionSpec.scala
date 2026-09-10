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
final class StateMachineProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "StateMachineProjection" should {
    "P9-52B pure caller-admitted StateMachine projection" which {
      "retain distinct direct transition and transition-adjunct families with no lifecycle or cross-view inference" in {
        Given("one exact Component containing direct StateMachine subjects, one State-to-State transition, and one independently admitted transition-adjunct relationship")
        val component = _component()
        val statemachine = _subject("state-machine-order", component, "state-machine-order", StateMachineProjectionStateMachine)
        val draft = _subject("state-draft", component, "state-draft", StateMachineProjectionState)
        val submitted = _subject("state-submitted", component, "state-submitted", StateMachineProjectionState)
        val trigger = _subject("trigger-submit", component, "trigger-submit", StateMachineProjectionTrigger)
        val guard = _subject("guard-order-valid", component, "guard-order-valid", StateMachineProjectionGuard)
        val action = _subject("action-notify", component, "action-notify", StateMachineProjectionAction)
        val activity = _subject("activity-review", component, "activity-review", StateMachineProjectionActivity)
        val operation = _subject("operation-submit", component, "operation-submit", StateMachineProjectionOperation)
        val event = _subject("event-submitted", component, "event-submitted", StateMachineProjectionEvent)
        val rule = _subject("rule-submit-permitted", component, "rule-submit-permitted", StateMachineProjectionRule)
        val transition = _transition(
          "transition-submit",
          component,
          "transition-submit",
          "state-machine-order",
          _transition_endpoint("endpoint-draft", component, "state-draft", "source-state"),
          _transition_endpoint("endpoint-submitted", component, "state-submitted", "target-state")
        )
        val adjunct = _transition_adjunct(
          "transition-adjunct-trigger",
          component,
          "transition-adjunct-trigger",
          "transition-submit",
          _adjunct_endpoint("adjunct-trigger", component, "trigger-submit", "trigger")
        )

        When("the caller supplies only its direct StateMachine evidence")
        val result = StateMachineProjection.create(
          _context(),
          component,
          Vector(statemachine, draft, submitted, trigger, guard, action, activity, operation, event, rule),
          Vector(transition),
          Vector(adjunct),
          Vector.empty
        )

        Then("the two relation families retain their own identities, roles, directions, endpoint evidence, and unchanged directional records")
        val projected = result.toOption.get
        projected.subjects.map(_.sourceSubject.role).toSet shouldBe Set(
          StateMachineProjectionStateMachine,
          StateMachineProjectionState,
          StateMachineProjectionTrigger,
          StateMachineProjectionGuard,
          StateMachineProjectionAction,
          StateMachineProjectionActivity,
          StateMachineProjectionOperation,
          StateMachineProjectionEvent,
          StateMachineProjectionRule
        )
        projected.transitions.map(_.sourceTransition) shouldBe Vector(transition)
        projected.transitionAdjuncts.map(_.sourceTransitionAdjunct) shouldBe Vector(adjunct)
        projected.transitions.head.sourceTransition.sourceEndpoint.semanticTargetId shouldBe draft.semanticTargetId
        projected.transitions.head.sourceTransition.targetEndpoint.semanticTargetId shouldBe submitted.semanticTargetId
        projected.transitionAdjuncts.head.sourceTransitionAdjunct.transitionSemanticTargetId shouldBe transition.semanticTransitionId
        projected.transitionAdjuncts.head.sourceTransitionAdjunct.adjunctEndpoint.semanticTargetId shouldBe trigger.semanticTargetId
        projected.forwardTransitions.map(_.sourceTransition) shouldBe Vector(transition)
        projected.reverseTransitions.map(_.sourceTransition) shouldBe Vector(transition)
        projected.reverseTransitions.head.sourceTransition.direction shouldBe transition.direction
      }

      "retain an attributable bounded gap without inferring a State, transition, adjunct endpoint, or navigation target" in {
        Given("one directly admitted StateMachine and one bounded unavailable State gap without an affected retained identity")
        val component = _component()
        val statemachine = _subject("state-machine-order", component, "state-machine-order", StateMachineProjectionStateMachine)
        val gapcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-provider-no-state-field"),
          limitations = Vector("cozy-state-machine-field-boundary")
        )
        val gap = _gap(
          "gap-state-field",
          component,
          StateMachineProjectionState,
          "cozy:order/state-field",
          None,
          condition = gapcondition,
          reason = "The admitted Cozy field cannot supply the requested direct State identity."
        )

        When("the caller projects only the bounded unsupported material")
        val result = StateMachineProjection.create(_context(), component, Vector(statemachine), Vector.empty, Vector.empty, Vector(gap))

        Then("the bounded condition and limitation remain explicit without inferred lifecycle or transition material")
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
        result.toOption.map(_.gaps.head.sourceGap.affectedSemanticTargetId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.condition) shouldBe Some(gapcondition)
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe Vector("state-machine-order")
        result.toOption.toVector.flatMap(_.transitions) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.transitionAdjuncts) shouldBe Vector.empty
      }

      "present direct subjects, independent relations, and gaps in deterministic non-semantic order under caller permutations" in {
        Given("reverse-order evidence whose admitted identities and tie keys make every StateMachine presentation order total")
        val component = _component()
        val statemachine = _subject("state-machine-order", component, "state-machine-order", StateMachineProjectionStateMachine)
        val statealpha = _subject("state-alpha", component, "state-alpha", StateMachineProjectionState)
        val statezulu = _subject("state-zulu", component, "state-zulu", StateMachineProjectionState)
        val triggeralpha = _subject("trigger-alpha", component, "trigger-alpha", StateMachineProjectionTrigger)
        val triggerzulu = _subject("trigger-zulu", component, "trigger-zulu", StateMachineProjectionTrigger)
        val subjects = Vector(statemachine, statezulu, triggerzulu, statealpha, triggeralpha)
        val transitions = Vector(
          _transition(
            "transition-zulu",
            component,
            "transition-zulu",
            "state-machine-order",
            _transition_endpoint("endpoint-zulu-source", component, "state-zulu", "source-state"),
            _transition_endpoint("endpoint-zulu-target", component, "state-alpha", "target-state")
          ),
          _transition(
            "transition-alpha",
            component,
            "transition-alpha",
            "state-machine-order",
            _transition_endpoint("endpoint-alpha-source", component, "state-alpha", "source-state"),
            _transition_endpoint("endpoint-alpha-target", component, "state-zulu", "target-state")
          )
        )
        val adjuncts = Vector(
          _transition_adjunct(
            "adjunct-zulu",
            component,
            "adjunct-zulu",
            "transition-zulu",
            _adjunct_endpoint("endpoint-adjunct-zulu", component, "trigger-zulu", "trigger")
          ),
          _transition_adjunct(
            "adjunct-alpha",
            component,
            "adjunct-alpha",
            "transition-alpha",
            _adjunct_endpoint("endpoint-adjunct-alpha", component, "trigger-alpha", "trigger")
          )
        )
        val gaps = Vector(
          _gap("gap-zulu", component, StateMachineProjectionState, "cozy:zulu", Some(ComponentDashboardSemanticTargetIdentity("state-zulu"))),
          _gap("gap-alpha", component, StateMachineProjectionTransitionRelation, "cozy:alpha", Some(ComponentDashboardSemanticTargetIdentity("transition-alpha")))
        )

        forAll(Gen.oneOf(true, false), Gen.oneOf(true, false), Gen.oneOf(true, false), Gen.oneOf(true, false)) { (reversesubjects, reversetransitions, reverseadjuncts, reversegaps) =>
          val suppliedsubjects = if (reversesubjects) subjects.reverse else subjects
          val suppliedtransitions = if (reversetransitions) transitions.reverse else transitions
          val suppliedadjuncts = if (reverseadjuncts) adjuncts.reverse else adjuncts
          val suppliedgaps = if (reversegaps) gaps.reverse else gaps

          When("property-generated caller permutations are projected")
          val result = StateMachineProjection.create(
            _context(),
            component,
            suppliedsubjects,
            suppliedtransitions,
            suppliedadjuncts,
            suppliedgaps
          )

          Then("identity and admitted non-semantic keys alone determine the two independent relation orders and both indexes retain the same transition records")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe
            suppliedsubjects.sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))).map(_.semanticTargetId.value)
          result.toOption.toVector.flatMap(_.transitions.map(_.sourceTransition.id)) shouldBe
            suppliedtransitions.sortBy(transition => (
              transition.stateMachineSemanticTargetId.value,
              transition.sourceEndpoint.semanticTargetId.value,
              transition.targetEndpoint.semanticTargetId.value,
              transition.semanticTransitionId.value,
              transition.id,
              transition.stableTieKey.getOrElse("")
            )).map(_.id)
          result.toOption.toVector.flatMap(_.transitionAdjuncts.map(_.sourceTransitionAdjunct.id)) shouldBe
            suppliedadjuncts.sortBy(adjunct => (
              adjunct.transitionSemanticTargetId.value,
              adjunct.adjunctEndpoint.semanticTargetId.value,
              adjunct.semanticAdjunctRelationId.value,
              adjunct.id,
              adjunct.stableTieKey.getOrElse("")
            )).map(_.id)
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe
            suppliedgaps.sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))).map(_.id)
          result.toOption.toVector.flatMap(_.forwardTransitions.map(_.sourceTransition.id)) shouldBe
            result.toOption.toVector.flatMap(_.transitions.map(_.sourceTransition.id))
          result.toOption.toVector.flatMap(_.reverseTransitions.map(_.sourceTransition.id)) shouldBe
            result.toOption.toVector.flatMap(_.transitions.map(_.sourceTransition.id))
        }
      }

      "preserve only usable exact local transition and adjunct affordances while retaining every relation record" in {
        Given("one usable local transition and adjunct relationship plus retained relations whose local target evidence is unavailable or limited")
        val component = _component()
        val statemachine = _subject("state-machine-order", component, "state-machine-order", StateMachineProjectionStateMachine)
        val draft = _subject("state-draft", component, "state-draft", StateMachineProjectionState)
        val submitted = _subject("state-submitted", component, "state-submitted", StateMachineProjectionState)
        val rejected = _subject("state-rejected", component, "state-rejected", StateMachineProjectionState)
        val submittrigger = _subject("trigger-submit", component, "trigger-submit", StateMachineProjectionTrigger)
        val rejecttrigger = _subject("trigger-reject", component, "trigger-reject", StateMachineProjectionTrigger)
        val usable = _transition(
          "transition-submit",
          component,
          "transition-submit",
          "state-machine-order",
          _transition_endpoint(
            "endpoint-submit-source",
            component,
            "state-draft",
            "source-state",
            navigation = Some(_transition_endpoint_navigation("endpoint-submit-source", component, "state-draft"))
          ),
          _transition_endpoint(
            "endpoint-submit-target",
            component,
            "state-submitted",
            "target-state",
            navigation = Some(_transition_endpoint_navigation("endpoint-submit-target", component, "state-submitted"))
          )
        )
        val conditioned = _transition(
          "transition-reject",
          component,
          "transition-reject",
          "state-machine-order",
          _transition_endpoint(
            "endpoint-reject-source",
            component,
            "state-submitted",
            "source-state",
            condition = _condition(availability = "unavailable"),
            navigation = Some(_transition_endpoint_navigation(
              "endpoint-reject-source",
              component,
              "state-submitted",
              _condition(availability = "unavailable")
            ))
          ),
          _transition_endpoint(
            "endpoint-reject-target",
            component,
            "state-rejected",
            "target-state",
            navigation = Some(_transition_endpoint_navigation("endpoint-reject-target", component, "state-rejected"))
          )
        )
        val usableadjunct = _transition_adjunct(
          "adjunct-submit",
          component,
          "adjunct-submit",
          "transition-submit",
          _adjunct_endpoint(
            "endpoint-adjunct-submit",
            component,
            "trigger-submit",
            "trigger",
            navigation = Some(_adjunct_endpoint_navigation("endpoint-adjunct-submit", component, "trigger-submit"))
          )
        )
        val limitedadjunct = _transition_adjunct(
          "adjunct-reject",
          component,
          "adjunct-reject",
          "transition-reject",
          _adjunct_endpoint(
            "endpoint-adjunct-reject",
            component,
            "trigger-reject",
            "trigger",
            navigation = Some(_adjunct_endpoint_navigation("endpoint-adjunct-reject", component, "trigger-reject"))
          ),
          condition = _condition(limitations = Vector("adjunct-detail-receiver-not-implemented"))
        )

        When("the caller supplies target-gated same-Component affordance evidence")
        val result = StateMachineProjection.create(
          _context(),
          component,
          Vector(statemachine, draft, submitted, rejected, submittrigger, rejecttrigger),
          Vector(conditioned, usable),
          Vector(limitedadjunct, usableadjunct),
          Vector.empty
        )

        Then("only exact usable local affordances remain while both independently retained relation families remain unchanged")
        val projected = result.toOption.get
        projected.transitions.find(_.sourceTransition.id == "transition-submit").map(_.forwardNavigationTarget) shouldBe
          Some(Some(_transition_endpoint_navigation("endpoint-submit-target", component, "state-submitted")))
        projected.transitions.find(_.sourceTransition.id == "transition-submit").map(_.reverseNavigationTarget) shouldBe
          Some(Some(_transition_endpoint_navigation("endpoint-submit-source", component, "state-draft")))
        projected.transitions.find(_.sourceTransition.id == "transition-reject").map(_.forwardNavigationTarget) shouldBe Some(None)
        projected.transitions.find(_.sourceTransition.id == "transition-reject").map(_.reverseNavigationTarget) shouldBe Some(None)
        projected.transitionAdjuncts.find(_.sourceTransitionAdjunct.id == "adjunct-submit").map(_.navigationTarget) shouldBe
          Some(Some(_adjunct_endpoint_navigation("endpoint-adjunct-submit", component, "trigger-submit")))
        projected.transitionAdjuncts.find(_.sourceTransitionAdjunct.id == "adjunct-reject").map(_.navigationTarget) shouldBe Some(None)
        projected.forwardTransitions.map(_.sourceTransition.id).toSet shouldBe Set("transition-submit", "transition-reject")
        projected.reverseTransitions.map(_.sourceTransition.id).toSet shouldBe Set("transition-submit", "transition-reject")
        projected.transitionAdjuncts.map(_.sourceTransitionAdjunct.id).toSet shouldBe Set("adjunct-submit", "adjunct-reject")
      }

      "return typed no-partial failures for duplicate, invalid direct-reference, and non-local navigation evidence" in {
        Given("caller input containing duplicate identities, a non-State transition endpoint, and a navigation target that changes source attribution")
        val component = _component()
        val statemachine = _subject("state-machine-order", component, "state-machine-order", StateMachineProjectionStateMachine)
        val state = _subject("state-draft", component, "state-draft", StateMachineProjectionState)
        val event = _subject("event-submitted", component, "event-submitted", StateMachineProjectionEvent)
        val invalidtransition = _transition(
          "transition-invalid-endpoint",
          component,
          "transition-invalid-endpoint",
          "state-machine-order",
          _transition_endpoint("endpoint-invalid-source", component, "state-draft", "source-state"),
          _transition_endpoint("endpoint-invalid-target", component, "event-submitted", "target-state")
        )
        val navigationmismatch = _subject(
          "state-with-navigation-mismatch",
          component,
          "state-with-navigation-mismatch",
          StateMachineProjectionState,
          navigation = Some(_navigation(
            "state-with-navigation-mismatch",
            component,
            attribution = _attribution("different-source", "different-authority", "different:locator")
          ))
        )

        When("the factory receives each malformed caller-admitted ledger")
        val duplicated = StateMachineProjection.create(_context(), component, Vector(statemachine, statemachine), Vector.empty, Vector.empty, Vector.empty)
        val invalidreference = StateMachineProjection.create(_context(), component, Vector(statemachine, state, event), Vector(invalidtransition), Vector.empty, Vector.empty)
        val invalidnavigation = StateMachineProjection.create(_context(), component, Vector(statemachine, navigationmismatch), Vector.empty, Vector.empty, Vector.empty)
        val invalidcontext = StateMachineProjection.create(_context(" "), component, Vector(statemachine), Vector.empty, Vector.empty, Vector.empty)

        Then("every failure remains typed and exposes no partial projection")
        Vector(duplicated, invalidreference, invalidnavigation, invalidcontext).foreach { result =>
          result.isLeft shouldBe true
          result.toOption shouldBe None
        }
      }
    }
  }

  private def _context(contextid: String = "textus-state-machine-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-state-machine"): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _attribution(
    sourceid: String,
    authorityscope: String,
    sourcelocator: String
  ): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, authorityscope, sourcelocator)

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

  private def _navigation(
    targetid: String,
    component: ComponentDashboardComponentIdentity,
    attribution: ComponentDashboardSourceAttribution = _attribution(
      "navigation-source",
      "navigation-authority",
      "navigation:locator"
    ),
    condition: ComponentDashboardCondition = _condition(),
    implementedandusable: Boolean = true
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      attribution,
      condition,
      implementedandusable
    )

  private def _transition_endpoint_navigation(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    _navigation(
      semanticid,
      component,
      _attribution(s"endpoint-$endpointid", "endpoint-authority", s"endpoint:$endpointid"),
      condition
    )

  private def _adjunct_endpoint_navigation(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    _navigation(
      semanticid,
      component,
      _attribution(s"adjunct-endpoint-$endpointid", "adjunct-endpoint-authority", s"adjunct-endpoint:$endpointid"),
      condition
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: StateMachineProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "subject-source",
      "subject-authority",
      "subject:locator"
    )
  ): StateMachineProjectionSubject =
    StateMachineProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      role,
      attribution,
      condition,
      None,
      navigation
    )

  private def _transition_endpoint(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: String,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): StateMachineProjectionTransitionEndpoint =
    StateMachineProjectionTransitionEndpoint(
      endpointid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      StateMachineProjectionTransitionEndpointRole(role),
      _attribution(s"endpoint-$endpointid", "endpoint-authority", s"endpoint:$endpointid"),
      condition,
      None,
      navigation
    )

  private def _adjunct_endpoint(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: String,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): StateMachineProjectionAdjunctEndpoint =
    StateMachineProjectionAdjunctEndpoint(
      endpointid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      StateMachineProjectionAdjunctEndpointRole(role),
      _attribution(s"adjunct-endpoint-$endpointid", "adjunct-endpoint-authority", s"adjunct-endpoint:$endpointid"),
      condition,
      None,
      navigation
    )

  private def _transition(
    transitionid: String,
    component: ComponentDashboardComponentIdentity,
    semantictransitionid: String,
    statemachinesemanticid: String,
    source: StateMachineProjectionTransitionEndpoint,
    target: StateMachineProjectionTransitionEndpoint,
    direction: String = "source-to-target",
    condition: ComponentDashboardCondition = _condition(),
    tiekey: Option[String] = None
  ): StateMachineProjectionTransition =
    StateMachineProjectionTransition(
      transitionid,
      component,
      ComponentDashboardSemanticTargetIdentity(semantictransitionid),
      ComponentDashboardSemanticTargetIdentity(statemachinesemanticid),
      StateMachineProjectionTransitionRelation,
      StateMachineProjectionTransitionDirection(direction),
      source,
      target,
      _attribution(s"transition-$transitionid", "transition-authority", s"transition:$transitionid"),
      condition,
      tiekey
    )

  private def _transition_adjunct(
    adjunctid: String,
    component: ComponentDashboardComponentIdentity,
    semanticadjunctid: String,
    transitionsemanticid: String,
    endpoint: StateMachineProjectionAdjunctEndpoint,
    direction: String = "transition-to-adjunct",
    condition: ComponentDashboardCondition = _condition(),
    tiekey: Option[String] = None
  ): StateMachineProjectionTransitionAdjunct =
    StateMachineProjectionTransitionAdjunct(
      adjunctid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticadjunctid),
      ComponentDashboardSemanticTargetIdentity(transitionsemanticid),
      StateMachineProjectionTransitionAdjunctRelation,
      StateMachineProjectionTransitionAdjunctDirection(direction),
      endpoint,
      _attribution(s"transition-adjunct-$adjunctid", "transition-adjunct-authority", s"transition-adjunct:$adjunctid"),
      condition,
      tiekey
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: StateMachineProjectionRole,
    boundedfieldorscope: String,
    affectedidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition = _condition(),
    reason: String = "The admitted bounded material cannot support this StateMachine role.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): StateMachineProjectionGap =
    StateMachineProjectionGap(
      gapid,
      component,
      requestedrole,
      boundedfieldorscope,
      affectedidentity,
      _attribution(s"gap-$gapid", "gap-authority", s"gap:$gapid"),
      condition,
      reason,
      tiekey,
      navigation
    )
}
