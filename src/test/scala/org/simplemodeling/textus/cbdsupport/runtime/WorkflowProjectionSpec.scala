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
final class WorkflowProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "WorkflowProjection" should {
    "P9-51B pure caller-admitted Workflow projection" which {
      "retain one direct Workflow ledger with all seven admitted subject roles and directed flow evidence without causal or lifecycle inference" in {
        Given("one exact Component with all seven direct Workflow subject roles and an independently admitted directed flow")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val activity = _subject("activity-accept-order", component, "activity-accept-order", WorkflowProjectionActivity)
        val participant = _subject("participant-customer", component, "participant-customer", WorkflowProjectionParticipant)
        val domain = _subject("domain-order", component, "domain-order", WorkflowProjectionDomainElement)
        val operation = _subject("operation-accept-order", component, "operation-accept-order", WorkflowProjectionOperation)
        val event = _subject("event-order-accepted", component, "event-order-accepted", WorkflowProjectionEvent)
        val effect = _subject("effect-order-accepted", component, "effect-order-accepted", WorkflowProjectionPublishedStateEffect)
        val flow = _flow(
          "flow-accept-order",
          component,
          "flow-accept-order",
          "workflow-order-fulfillment",
          "010",
          _endpoint("endpoint-accept-order-source", component, "activity-accept-order", "activity-source"),
          _endpoint("endpoint-accept-order-target", component, "event-order-accepted", "event-target")
        )

        When("the caller supplies only its direct Workflow records without source lookup reconstruction or semantic completion")
        val result = WorkflowProjection.create(
          _context(),
          component,
          Vector(workflow, activity, participant, domain, operation, event, effect),
          Vector(flow),
          Vector.empty
        )

        Then("the projection retains exact role attribution locator condition endpoint direction and source-owned sequence evidence without extra subjects")
        val projected = result.toOption.get
        projected.subjects.map(_.sourceSubject.role).toSet shouldBe Set(
          WorkflowProjectionWorkflow,
          WorkflowProjectionActivity,
          WorkflowProjectionParticipant,
          WorkflowProjectionDomainElement,
          WorkflowProjectionOperation,
          WorkflowProjectionEvent,
          WorkflowProjectionPublishedStateEffect
        )
        projected.flows.map(_.sourceFlow) shouldBe Vector(flow)
        projected.flows.head.sourceFlow.workflowSemanticTargetId shouldBe workflow.semanticTargetId
        projected.flows.head.sourceFlow.sourceOwnedSequenceKey.value shouldBe "010"
        projected.flows.head.sourceFlow.sourceEndpoint shouldBe flow.sourceEndpoint
        projected.flows.head.sourceFlow.targetEndpoint shouldBe flow.targetEndpoint
        projected.subjects.map(_.sourceSubject.semanticTargetId.value) should not contain "state-transition-order-accepted"
        projected.subjects.map(_.sourceSubject.semanticTargetId.value) should not contain "causal-order-accepted"
        projected.flows.head.sourceFlow.attribution.sourceLocator shouldBe "flow:flow-accept-order"
      }

      "retain an attributable bounded source gap without inferring an endpoint proxy broad absence or navigation target" in {
        Given("one admitted Workflow and a bounded unavailable source field gap with no affected retained identity")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val gapcondition = _condition(
          availability = "unavailable",
          authorization = "denied",
          explicitabsence = Some("bounded-provider-no-workflow-flow-field"),
          limitations = Vector("cozy-workflow-field-boundary")
        )
        val gap = _gap(
          "gap-workflow-flow-field",
          component,
          WorkflowProjectionFlowRelation,
          "cozy:order/workflow-flow-field",
          None,
          condition = gapcondition,
          reason = "The admitted Cozy field cannot supply the requested direct Workflow flow relation."
        )

        When("the caller projects the bounded unsupported source material")
        val result = WorkflowProjection.create(_context(), component, Vector(workflow), Vector.empty, Vector(gap))

        Then("the source condition scope and limitation remain explicit with no inferred endpoint subject broad absence or navigation")
        result.toOption.map(_.gaps.map(_.sourceGap)) shouldBe Some(Vector(gap))
        result.toOption.map(_.gaps.head.sourceGap.affectedSemanticTargetId) shouldBe Some(None)
        result.toOption.map(_.gaps.head.sourceGap.condition) shouldBe Some(gapcondition)
        result.toOption.toVector.flatMap(_.gaps.flatMap(_.navigationTarget)) shouldBe Vector.empty
        result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe Vector("workflow-order-fulfillment")
      }

      "present direct subjects and gaps identity-first and flows by Workflow plus explicit source sequence under caller permutations" in {
        Given("reverse-order direct records whose admitted identities and source-owned flow sequence keys make every presentation order total")
        val component = _component()
        val workflowzulu = _subject("workflow-zulu", component, "workflow-zulu", WorkflowProjectionWorkflow)
        val workflowalpha = _subject("workflow-alpha", component, "workflow-alpha", WorkflowProjectionWorkflow)
        val activity = _subject("activity-alpha", component, "activity-alpha", WorkflowProjectionActivity)
        val flows = Vector(
          _flow(
            "flow-zulu",
            component,
            "flow-zulu",
            "workflow-zulu",
            "020",
            _endpoint("endpoint-zulu-source", component, "activity-zulu", "activity-source"),
            _endpoint("endpoint-zulu-target", component, "event-zulu", "event-target")
          ),
          _flow(
            "flow-alpha-later",
            component,
            "flow-alpha-zulu",
            "workflow-alpha",
            "020",
            _endpoint("endpoint-alpha-later-source", component, "activity-alpha-later", "activity-source"),
            _endpoint("endpoint-alpha-later-target", component, "event-alpha-later", "event-target")
          ),
          _flow(
            "flow-alpha-earlier",
            component,
            "flow-alpha-alpha",
            "workflow-alpha",
            "010",
            _endpoint("endpoint-alpha-earlier-source", component, "activity-alpha-earlier", "activity-source"),
            _endpoint("endpoint-alpha-earlier-target", component, "event-alpha-earlier", "event-target")
          )
        )
        val gaps = Vector(
          _gap("gap-zulu", component, WorkflowProjectionActivity, "cozy:zulu", Some(ComponentDashboardSemanticTargetIdentity("workflow-zulu"))),
          _gap("gap-alpha", component, WorkflowProjectionFlowRelation, "cozy:alpha", Some(ComponentDashboardSemanticTargetIdentity("flow-alpha-alpha")))
        )

        forAll(Gen.oneOf(true, false), Gen.oneOf(true, false), Gen.oneOf(true, false)) { (reversesubjects, reverseflows, reversegaps) =>
          val suppliedsubjects = if (reversesubjects) Vector(workflowzulu, workflowalpha, activity) else Vector(activity, workflowalpha, workflowzulu)
          val suppliedflows = if (reverseflows) flows.reverse else flows
          val suppliedgaps = if (reversegaps) gaps.reverse else gaps

          When("property-generated caller permutations are projected")
          val result = WorkflowProjection.create(_context(), component, suppliedsubjects, suppliedflows, suppliedgaps)

          Then("identity and explicit source-owned sequence keys alone determine the retained presentation and both indexes retain the same flow records")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe
            suppliedsubjects.sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))).map(_.semanticTargetId.value)
          result.toOption.toVector.flatMap(_.flows.map(_.sourceFlow.id)) shouldBe
            suppliedflows.sortBy(flow => (
              flow.workflowSemanticTargetId.value,
              flow.sourceOwnedSequenceKey.value,
              flow.semanticFlowId.value,
              flow.sourceEndpoint.semanticTargetId.value,
              flow.targetEndpoint.semanticTargetId.value,
              flow.id,
              flow.stableTieKey.getOrElse("")
            )).map(_.id)
          result.toOption.toVector.flatMap(_.gaps.map(_.sourceGap.id)) shouldBe
            suppliedgaps.sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))).map(_.id)
          result.toOption.toVector.flatMap(_.forwardFlows.map(_.sourceFlow.id)) shouldBe
            result.toOption.toVector.flatMap(_.flows.map(_.sourceFlow.id))
          result.toOption.toVector.flatMap(_.reverseFlows.map(_.sourceFlow.id)) shouldBe
            result.toOption.toVector.flatMap(_.flows.map(_.sourceFlow.id))
        }
      }

      "admit only implemented usable forward and reverse receiving-endpoint navigation while retaining every flow in both indexes without a synthesized reverse flow" in {
        Given("one usable directed flow and one independently retained flow with an unavailable source endpoint")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val usable = _flow(
          "flow-accept-order",
          component,
          "flow-accept-order",
          "workflow-order-fulfillment",
          "010",
          _endpoint(
            "endpoint-accept-order-source",
            component,
            "activity-accept-order",
            "activity-source",
            navigation = Some(_endpoint_navigation("endpoint-accept-order-source", component, "activity-accept-order"))
          ),
          _endpoint(
            "endpoint-accept-order-target",
            component,
            "event-order-accepted",
            "event-target",
            navigation = Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted"))
          )
        )
        val conditioned = _flow(
          "flow-condition-limited",
          component,
          "flow-condition-limited",
          "workflow-order-fulfillment",
          "020",
          _endpoint(
            "endpoint-condition-source",
            component,
            "activity-verify-order",
            "activity-source",
            condition = _condition(availability = "unavailable"),
            navigation = Some(_endpoint_navigation(
              "endpoint-condition-source",
              component,
              "activity-verify-order",
              _condition(availability = "unavailable")
            ))
          ),
          _endpoint(
            "endpoint-condition-target",
            component,
            "event-order-verified",
            "event-target",
            navigation = Some(_endpoint_navigation("endpoint-condition-target", component, "event-order-verified"))
          )
        )

        When("the caller supplies exact endpoint targets and their independent relation endpoint and target conditions")
        val result = WorkflowProjection.create(_context(), component, Vector(workflow), Vector(conditioned, usable), Vector.empty)

        Then("only usable exact receiving endpoints expose their matching affordance while the retained directional flows remain unchanged")
        val projected = result.toOption.get
        projected.flows.find(_.sourceFlow.id == "flow-accept-order").map(_.forwardNavigationTarget) shouldBe
          Some(Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted")))
        projected.flows.find(_.sourceFlow.id == "flow-accept-order").map(_.reverseNavigationTarget) shouldBe
          Some(Some(_endpoint_navigation("endpoint-accept-order-source", component, "activity-accept-order")))
        projected.flows.find(_.sourceFlow.id == "flow-condition-limited").map(_.forwardNavigationTarget) shouldBe Some(None)
        projected.flows.find(_.sourceFlow.id == "flow-condition-limited").map(_.reverseNavigationTarget) shouldBe Some(None)
        projected.forwardFlows.map(_.sourceFlow.id).toSet shouldBe Set("flow-accept-order", "flow-condition-limited")
        projected.reverseFlows.map(_.sourceFlow.id).toSet shouldBe Set("flow-accept-order", "flow-condition-limited")
        projected.reverseFlows.find(_.sourceFlow.id == "flow-accept-order").map(_.sourceFlow.direction) shouldBe Some(usable.direction)
      }

      "return a typed no-partial failure when receiving endpoint navigation evidence differs from exact attribution or condition" in {
        Given("otherwise admitted flows whose receiving endpoint targets carry mismatched attribution or condition evidence")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val attributionmismatch = _flow(
          "flow-attribution-mismatch",
          component,
          "flow-attribution-mismatch",
          "workflow-order-fulfillment",
          "010",
          _endpoint("endpoint-attribution-source", component, "activity-accept-order", "activity-source"),
          _endpoint(
            "endpoint-attribution-target",
            component,
            "event-order-accepted",
            "event-target",
            navigation = Some(_navigation(
              "event-order-accepted",
              component,
              attribution = _attribution("different-source", "different-authority", "different:locator")
            ))
          )
        )
        val conditionmismatch = _flow(
          "flow-condition-mismatch",
          component,
          "flow-condition-mismatch",
          "workflow-order-fulfillment",
          "020",
          _endpoint("endpoint-condition-mismatch-source", component, "activity-verify-order", "activity-source"),
          _endpoint(
            "endpoint-condition-mismatch-target",
            component,
            "event-order-verified",
            "event-target",
            navigation = Some(_navigation(
              "event-order-verified",
              component,
              condition = _condition(limitations = Vector("different-receiving-condition"))
            ))
          )
        )

        When("the projection factory validates exact receiving endpoint navigation evidence")
        val result = WorkflowProjection.create(
          _context(),
          component,
          Vector(workflow),
          Vector(attributionmismatch, conditionmismatch),
          Vector.empty
        )

        Then("it returns WorkflowProjectionFailure and exposes no projection with mismatched receiving endpoint evidence")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-attribution-mismatch' target endpoint 'endpoint-attribution-target' navigation target must retain its exact receiving endpoint attribution."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-condition-mismatch' target endpoint 'endpoint-condition-mismatch-target' navigation target must retain its exact receiving endpoint condition."
        )
      }

      "return a typed no-partial failure for invalid role placement scope duplicate identity non-Workflow membership sequence endpoint locator direction and tie-key input" in {
        Given("otherwise direct records with all admitted-boundary validation violations")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val activity = _subject("activity-process-order", component, "activity-process-order", WorkflowProjectionActivity)
        val malformedsubject = _subject("subject-malformed", component, "subject-malformed", WorkflowProjectionFlowRelation)
        val blanklocator = _subject(
          "subject-blank-locator",
          component,
          "subject-blank-locator",
          WorkflowProjectionEvent,
          attribution = ComponentDashboardSourceAttribution("workflow-source", "workflow-authority", " ")
        )
        val duplicateandnonworkflow = _flow(
          "flow-nonworkflow-membership",
          component,
          "subject-malformed",
          "activity-process-order",
          "010",
          _endpoint("endpoint-membership-source", component, "activity-process-order", "activity-source"),
          _endpoint("endpoint-membership-target", component, "event-order-processed", "event-target")
        )
        val blanksequenceanddirection = _flow(
          "flow-blank-sequence-direction",
          component,
          "flow-blank-sequence-direction",
          "workflow-order-fulfillment",
          " ",
          _endpoint("endpoint-blank-sequence-source", component, "activity-process-order", "activity-source"),
          _endpoint("endpoint-blank-sequence-target", component, "event-order-processed", "event-target"),
          direction = " "
        )
        val duplicateendpoint = _flow(
          "flow-duplicate-endpoint",
          component,
          "flow-duplicate-endpoint",
          "workflow-order-fulfillment",
          "030",
          _endpoint("endpoint-duplicate", component, "activity-process-order", "activity-source"),
          _endpoint("endpoint-duplicate", component, "event-order-processed", " ")
        )
        val outofscope = _gap("gap-scope", _component("textus-other-workflow"), WorkflowProjectionFlowRelation, "cozy:workflow", None)
        val blanktiekey = _gap("gap-blank-tie", component, WorkflowProjectionActivity, "cozy:workflow", None, tiekey = Some(" "))

        When("the immutable factory validates the complete caller-supplied boundary")
        val result = WorkflowProjection.create(
          _context(),
          component,
          Vector(workflow, activity, malformedsubject, blanklocator),
          Vector(duplicateandnonworkflow, blanksequenceanddirection, duplicateendpoint),
          Vector(outofscope, blanktiekey)
        )

        Then("it returns WorkflowProjectionFailure and cannot expose a partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow subject 'subject-malformed' has flow relation role WorkflowProjectionFlowRelation."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Workflow subject or flow semantic identity 'subject-malformed'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-nonworkflow-membership' Workflow semantic identity 'activity-process-order' is not a retained direct Workflow subject."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-blank-sequence-direction' source-owned sequence key must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-blank-sequence-direction' direction must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-duplicate-endpoint' target endpoint 'endpoint-duplicate' role must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Duplicate Workflow subject, flow, endpoint, or gap identity 'endpoint-duplicate'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow subject 'subject-blank-locator' source locator must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow gap 'gap-scope' is outside Projection Component 'textus-workflow'."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow gap 'gap-blank-tie' stable non-semantic tie key must not be blank."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-workflow-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-workflow"): ComponentDashboardComponentIdentity =
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
    role: WorkflowProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    attribution: ComponentDashboardSourceAttribution = ComponentDashboardSourceAttribution(
      "subject-source",
      "subject-authority",
      "subject:locator"
    )
  ): WorkflowProjectionSubject =
    WorkflowProjectionSubject(
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
  ): WorkflowProjectionFlowEndpoint =
    WorkflowProjectionFlowEndpoint(
      endpointid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticid),
      WorkflowProjectionFlowEndpointRole(role),
      _attribution(s"endpoint-$endpointid", "endpoint-authority", s"endpoint:$endpointid"),
      condition,
      None,
      navigation
    )

  private def _flow(
    flowid: String,
    component: ComponentDashboardComponentIdentity,
    semanticflowid: String,
    workflowsemanticid: String,
    sequencekey: String,
    source: WorkflowProjectionFlowEndpoint,
    target: WorkflowProjectionFlowEndpoint,
    direction: String = "source-to-target",
    condition: ComponentDashboardCondition = _condition(),
    tiekey: Option[String] = None
  ): WorkflowProjectionFlow =
    WorkflowProjectionFlow(
      flowid,
      component,
      ComponentDashboardSemanticTargetIdentity(semanticflowid),
      ComponentDashboardSemanticTargetIdentity(workflowsemanticid),
      WorkflowProjectionFlowRelation,
      WorkflowProjectionFlowDirection(direction),
      WorkflowProjectionSourceOwnedSequenceKey(sequencekey),
      source,
      target,
      _attribution(s"flow-$flowid", "flow-authority", s"flow:$flowid"),
      condition,
      tiekey
    )

  private def _gap(
    gapid: String,
    component: ComponentDashboardComponentIdentity,
    requestedrole: WorkflowProjectionRole,
    boundedfieldorscope: String,
    affectedidentity: Option[ComponentDashboardSemanticTargetIdentity],
    condition: ComponentDashboardCondition = _condition(),
    reason: String = "The admitted bounded material cannot support this Workflow role.",
    tiekey: Option[String] = None,
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): WorkflowProjectionGap =
    WorkflowProjectionGap(
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
