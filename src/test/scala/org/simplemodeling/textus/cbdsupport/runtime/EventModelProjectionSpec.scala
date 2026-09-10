package org.simplemodeling.textus.cbdsupport.runtime

import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
final class EventModelProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "EventModelProjection" should {
    "P9-50B pure caller-admitted Event Model projection" which {
      "retain direct Command and Event subjects plus every independently admitted assertion kind and exact directed evidence" in {
        Given("one exact Component with direct Command and Event subjects and four independently admitted assertions")
        val component = _component()
        val command = _subject("command-place-order", component, "command-place-order", EventModelCommand)
        val event = _subject(
          "event-order-placed",
          component,
          "event-order-placed",
          EventModelEvent,
          condition = _condition(limitations = Vector("bounded-event-disclosure"))
        )
        val causal = _assertion(
          "assertion-command-causes-event",
          component,
          "causal-command-causes-event",
          EventModelCausalAssertion,
          _endpoint("endpoint-causal-command", component, "command-place-order", "command-origin"),
          _endpoint("endpoint-causal-event", component, "event-order-placed", "event-effect")
        )
        val consequence = _assertion(
          "assertion-event-consequence",
          component,
          "consequence-event-order",
          EventModelConsequenceAssertion,
          _endpoint("endpoint-consequence-event", component, "event-order-placed", "event-origin"),
          _endpoint("endpoint-consequence-domain", component, "domain-order-history", "domain-consequence")
        )
        val affected = _assertion(
          "assertion-event-affected-domain",
          component,
          "affected-domain-event-order",
          EventModelAffectedDomainElementAssertion,
          _endpoint("endpoint-affected-event", component, "event-order-placed", "event-origin"),
          _endpoint("endpoint-affected-domain", component, "domain-order", "affected-domain-element")
        )
        val generated = _assertion(
          "assertion-event-generated-state-effect",
          component,
          "generated-state-effect-event-order",
          EventModelGeneratedStateEffectAssertion,
          _endpoint("endpoint-generated-event", component, "event-order-placed", "event-origin"),
          _endpoint("endpoint-generated-state", component, "state-order-placed", "generated-state-effect")
        )

        When("the caller supplies those exact direct records without source lookup or reconstruction")
        val result = EventModelProjection.create(
          _context(),
          component,
          Vector(command, event),
          Vector(causal, consequence, affected, generated),
          Vector.empty
        )

        Then("the projection retains every supplied role, direction, endpoint, attribution, locator, and condition without inferring extra subjects")
        val projected = result.toOption.get
        projected.subjects.map(_.sourceSubject.role).toSet shouldBe Set(EventModelCommand, EventModelEvent)
        projected.assertions.map(_.sourceAssertion.role).toSet shouldBe Set(
          EventModelCausalAssertion,
          EventModelConsequenceAssertion,
          EventModelAffectedDomainElementAssertion,
          EventModelGeneratedStateEffectAssertion
        )
        projected.assertions.find(_.sourceAssertion.id == "assertion-command-causes-event").map(_.sourceAssertion) shouldBe Some(causal)
        projected.assertions.find(_.sourceAssertion.id == "assertion-event-consequence").map(_.sourceAssertion.sourceEndpoint) shouldBe
          Some(consequence.sourceEndpoint)
        projected.assertions.find(_.sourceAssertion.id == "assertion-event-generated-state-effect").map(_.sourceAssertion.targetEndpoint) shouldBe
          Some(generated.targetEndpoint)
        projected.subjects.map(_.sourceSubject.semanticTargetId.value).toSet shouldBe Set("command-place-order", "event-order-placed")
        projected.subjects.map(_.sourceSubject.semanticTargetId.value) should not contain "domain-order"
        projected.subjects.find(_.sourceSubject.id == "event-order-placed").map(_.sourceSubject.condition) shouldBe Some(event.condition)
        projected.assertions.map(_.sourceAssertion.attribution.sourceLocator).toSet should contain("assertion:assertion-event-affected-domain")
      }

      "retain an explicit bounded source gap without inferring an identity endpoint absence or navigation target" in {
        Given("an admitted Command and an attributable unavailable Cozy field gap without an affected retained identity")
        val component = _component()
        val command = _subject("command-place-order", component, "command-place-order", EventModelCommand)
        val gapcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-provider-no-event-field"),
          limitations = Vector("cozy-event-field-boundary")
        )
        val gap = _gap(
          "gap-event-field",
          component,
          EventModelEvent,
          "cozy:order/event-field",
          None,
          condition = gapcondition,
          reason = "The admitted Cozy field cannot supply the requested direct Event subject."
        )

        When("the caller projects the bounded unsupported source material")
        val result = EventModelProjection.create(_context(), component, Vector(command), Vector.empty, Vector(gap))

        Then("the attributable condition and limitation remain explicit with no inferred subject proxy or navigation")
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
        result.toOption.map(_.gaps.head.sourceGap.affectedSemanticTargetId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.condition) shouldBe Some(gapcondition)
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe Vector("command-place-order")
      }

      "present direct subjects assertions and gaps in deterministic identity-first order under caller permutations" in {
        Given("reverse-order direct records whose supplied identity keys make every presentation order total")
        val component = _component()
        val command = _subject("command-zulu", component, "command-zulu", EventModelCommand)
        val event = _subject("event-alpha", component, "event-alpha", EventModelEvent)
        val assertions = Vector(
          _assertion(
            "assertion-zulu",
            component,
            "assertion-zulu",
            EventModelConsequenceAssertion,
            _endpoint("endpoint-zulu-source", component, "event-zulu", "event-origin"),
            _endpoint("endpoint-zulu-target", component, "domain-zulu", "domain-target")
          ),
          _assertion(
            "assertion-alpha",
            component,
            "assertion-alpha",
            EventModelCausalAssertion,
            _endpoint("endpoint-alpha-source", component, "command-alpha", "command-origin"),
            _endpoint("endpoint-alpha-target", component, "event-alpha", "event-target")
          )
        )
        val gaps = Vector(
          _gap("gap-zulu", component, EventModelEvent, "cozy:zulu", Some(ComponentDashboardSemanticTargetIdentity("command-zulu"))),
          _gap("gap-alpha", component, EventModelCausalAssertion, "cozy:alpha", Some(ComponentDashboardSemanticTargetIdentity("assertion-alpha")))
        )

        forAll(Gen.oneOf(true, false), Gen.oneOf(true, false), Gen.oneOf(true, false)) { (reversesubjects, reverseassertions, reversegaps) =>
          val suppliedsubjects = if (reversesubjects) Vector(command, event) else Vector(event, command)
          val suppliedassertions = if (reverseassertions) assertions.reverse else assertions
          val suppliedgaps = if (reversegaps) gaps.reverse else gaps

          When("property-generated caller permutations are projected")
          val result = EventModelProjection.create(_context(), component, suppliedsubjects, suppliedassertions, suppliedgaps)

          Then("only semantic identity directed endpoints record identity and stable tie keys choose the retained presentation order")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe
            suppliedsubjects.sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))).map(_.semanticTargetId.value)
          result.toOption.toVector.flatMap(_.assertions.map(_.sourceAssertion.id)) shouldBe
            suppliedassertions.sortBy(assertion => (
              assertion.semanticAssertionId.value,
              assertion.sourceEndpoint.semanticTargetId.value,
              assertion.targetEndpoint.semanticTargetId.value,
              assertion.id,
              assertion.stableTieKey.getOrElse("")
            )).map(_.id)
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe
            suppliedgaps.sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))).map(_.id)
        }
      }

      "admit only implemented usable forward and reverse endpoint navigation while retaining the same supplied assertion in both indexes" in {
        Given("one usable directed assertion and one independently retained assertion with an unavailable source endpoint")
        val component = _component()
        val usable = _assertion(
          "assertion-command-event",
          component,
          "causal-command-event",
          EventModelCausalAssertion,
          _endpoint(
            "endpoint-command",
            component,
            "command-place-order",
            "command-origin",
            navigation = Some(_endpoint_navigation("endpoint-command", component, "command-place-order"))
          ),
          _endpoint(
            "endpoint-event",
            component,
            "event-order-placed",
            "event-effect",
            navigation = Some(_endpoint_navigation("endpoint-event", component, "event-order-placed"))
          )
        )
        val conditioned = _assertion(
          "assertion-conditioned",
          component,
          "consequence-conditioned",
          EventModelConsequenceAssertion,
          _endpoint(
            "endpoint-conditioned-source",
            component,
            "event-order-placed",
            "event-origin",
            condition = _condition(availability = "unavailable"),
            navigation = Some(_endpoint_navigation(
              "endpoint-conditioned-source",
              component,
              "event-order-placed",
              _condition(availability = "unavailable")
            ))
          ),
          _endpoint(
            "endpoint-conditioned-target",
            component,
            "domain-order-history",
            "domain-consequence",
            navigation = Some(_endpoint_navigation(
              "endpoint-conditioned-target",
              component,
              "domain-order-history"
            ))
          )
        )

        When("the caller supplies the exact endpoint targets and their independent conditions")
        val result = EventModelProjection.create(_context(), component, Vector.empty, Vector(conditioned, usable), Vector.empty)

        Then("both affordances retain their exact receiving endpoints only when all relation conditions and the receiving contract are usable")
        val projected = result.toOption.get
        projected.assertions.find(_.sourceAssertion.id == "assertion-command-event").map(_.forwardNavigationTarget) shouldBe
          Some(Some(_endpoint_navigation("endpoint-event", component, "event-order-placed")))
        projected.assertions.find(_.sourceAssertion.id == "assertion-command-event").map(_.reverseNavigationTarget) shouldBe
          Some(Some(_endpoint_navigation("endpoint-command", component, "command-place-order")))
        projected.assertions.find(_.sourceAssertion.id == "assertion-conditioned").map(_.forwardNavigationTarget) shouldBe Some(None)
        projected.assertions.find(_.sourceAssertion.id == "assertion-conditioned").map(_.reverseNavigationTarget) shouldBe Some(None)
        projected.forwardAssertions.map(_.sourceAssertion.id).toSet shouldBe Set("assertion-command-event", "assertion-conditioned")
        projected.reverseAssertions.map(_.sourceAssertion.id).toSet shouldBe Set("assertion-command-event", "assertion-conditioned")
        projected.reverseAssertions.find(_.sourceAssertion.id == "assertion-command-event").map(_.sourceAssertion.direction) shouldBe Some(usable.direction)
      }

      "return a typed no-partial failure when endpoint navigation evidence differs from the exact receiving attribution or condition" in {
        Given("otherwise admitted assertions whose receiving endpoint navigation targets carry mismatched attribution or condition evidence")
        val component = _component()
        val attributionmismatch = _assertion(
          "assertion-attribution-mismatch",
          component,
          "causal-attribution-mismatch",
          EventModelCausalAssertion,
          _endpoint("endpoint-attribution-mismatch-source", component, "command-place-order", "command-origin"),
          _endpoint(
            "endpoint-attribution-mismatch-target",
            component,
            "event-order-placed",
            "event-effect",
            navigation = Some(_navigation(
              "event-order-placed",
              component,
              attribution = _attribution("different-source", "different-authority", "different:locator")
            ))
          )
        )
        val conditionmismatch = _assertion(
          "assertion-condition-mismatch",
          component,
          "consequence-condition-mismatch",
          EventModelConsequenceAssertion,
          _endpoint("endpoint-condition-mismatch-source", component, "event-order-placed", "event-origin"),
          _endpoint(
            "endpoint-condition-mismatch-target",
            component,
            "domain-order-history",
            "domain-consequence",
            navigation = Some(_navigation(
              "domain-order-history",
              component,
              condition = _condition(limitations = Vector("different-receiving-condition"))
            ))
          )
        )

        When("the projection factory validates the exact receiving endpoint navigation evidence")
        val result = EventModelProjection.create(
          _context(),
          component,
          Vector.empty,
          Vector(attributionmismatch, conditionmismatch),
          Vector.empty
        )

        Then("it returns EventModelProjectionFailure and cannot expose a projection with mismatched receiving endpoint evidence")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model assertion 'assertion-attribution-mismatch' target endpoint 'endpoint-attribution-mismatch-target' navigation target must retain its exact receiving endpoint attribution."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model assertion 'assertion-condition-mismatch' target endpoint 'endpoint-condition-mismatch-target' navigation target must retain its exact receiving endpoint condition."
        )
      }

      "return a typed no-partial failure for invalid role scope semantic identity endpoint locator and tie-key input" in {
        Given("otherwise direct records with role placement scope identity endpoint and locator violations")
        val component = _component()
        val malformedsubject = _subject("subject-malformed", component, "subject-malformed", EventModelCausalAssertion)
        val duplicateassertionsemantic = _assertion(
          "assertion-duplicate-semantic",
          component,
          "subject-malformed",
          EventModelCommand,
          _endpoint("endpoint-duplicate", component, "command-place-order", "command-origin"),
          _endpoint("endpoint-duplicate", component, "event-order-placed", " ")
        )
        val blanklocator = _subject(
          "subject-blank-locator",
          component,
          "subject-blank-locator",
          EventModelEvent,
          attribution = ComponentDashboardSourceAttribution("event-source", "event-authority", " ")
        )
        val outofscope = _gap("gap-scope", _component("textus-payment"), EventModelEvent, "cozy:event", None)
        val blanktiekey = _gap("gap-blank-tie", component, EventModelEvent, "cozy:event", None, tiekey = Some(" "))

        When("the immutable factory validates the complete caller-supplied boundary")
        val result = EventModelProjection.create(
          _context(),
          component,
          Vector(malformedsubject, blanklocator),
          Vector(duplicateassertionsemantic),
          Vector(outofscope, blanktiekey)
        )

        Then("it returns EventModelProjectionFailure and cannot expose a partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model subject 'subject-malformed' has assertion role EventModelCausalAssertion."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Event Model subject or assertion semantic identity 'subject-malformed'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model assertion 'assertion-duplicate-semantic' has subject role EventModelCommand."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model assertion 'assertion-duplicate-semantic' target endpoint 'endpoint-duplicate' role must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Event Model assertion endpoint identity 'endpoint-duplicate'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model subject 'subject-blank-locator' source locator must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model gap 'gap-scope' is outside Projection Component 'textus-event-model'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Event Model gap 'gap-blank-tie' stable non-semantic tie key must not be blank."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-event-model-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-event-model"): ComponentDashboardComponentIdentity =
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

  private def _endpoint_navigation(
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

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: EventModelProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "subject-source",
      "subject-authority",
      "subject:locator"
    )
  ): EventModelProjectionSubject =
    EventModelProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      role,
      attribution,
      condition,
      None,
      navigation
    )

  private def _endpoint(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: String,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): EventModelAssertionEndpoint =
    EventModelAssertionEndpoint(
      endpointid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      EventModelAssertionEndpointRole(role),
      _attribution(s"endpoint-$endpointid", "endpoint-authority", s"endpoint:$endpointid"),
      condition,
      None,
      navigation
    )

  private def _assertion(
    assertionid: String,
    component: ComponentDashboardComponentIdentity,
    semanticid: String,
    role: EventModelProjectionRole,
    source: EventModelAssertionEndpoint,
    target: EventModelAssertionEndpoint,
    direction: String = "source-to-target",
    condition: ComponentDashboardCondition = _condition()
  ): EventModelProjectionAssertion =
    EventModelProjectionAssertion(
      assertionid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      role,
      EventModelAssertionDirection(direction),
      source,
      target,
      _attribution(s"assertion-$assertionid", "assertion-authority", s"assertion:$assertionid"),
      condition,
      None
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: EventModelProjectionRole,
    boundedfieldorscope: String,
    affectedidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition = _condition(),
    reason: String = "The admitted bounded material cannot support this Event Model role.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): EventModelProjectionGap =
    EventModelProjectionGap(
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
