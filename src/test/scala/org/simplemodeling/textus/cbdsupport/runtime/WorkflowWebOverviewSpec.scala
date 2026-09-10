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
final class WorkflowWebOverviewSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "WorkflowWebOverview" should {
    "P9-51C pure immutable exact Workflow Web delivery" which {
      "retain direct projected subject flow endpoint and gap values without adding a second Workflow model" in {
        Given("one exact Component entry with direct projected Workflow activity and directed flow values")
        val component = _component()
        val workflow = _subject(
          "workflow-order-fulfillment",
          component,
          "workflow-order-fulfillment",
          WorkflowProjectionWorkflow,
          navigation = Some(_subject_navigation("workflow-order-fulfillment", component, "workflow-order-fulfillment"))
        )
        val activity = _subject("activity-accept-order", component, "activity-accept-order", WorkflowProjectionActivity)
        val source = _endpoint(
          "endpoint-accept-order-source",
          component,
          "activity-accept-order",
          "activity-source",
          navigation = Some(_endpoint_navigation("endpoint-accept-order-source", component, "activity-accept-order"))
        )
        val target = _endpoint(
          "endpoint-accept-order-target",
          component,
          "event-order-accepted",
          "event-target",
          navigation = Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted"))
        )
        val flow = _flow(
          "flow-accept-order",
          component,
          "flow-accept-order",
          "workflow-order-fulfillment",
          "010",
          source,
          target
        )
        val gap = _gap(
          "gap-published-effect",
          component,
          WorkflowProjectionPublishedStateEffect,
          "cozy:order/published-effect",
          Some(ComponentDashboardSemanticTargetIdentity("event-order-accepted")),
          condition = _condition(availability = "unavailable", limitations = Vector("bounded-published-effect-field")),
          reason = "The admitted bounded material cannot provide a published state effect."
        )
        val projection = _projection(component, Vector(activity, workflow), Vector(flow), Vector(gap))

        When("the existing authorized entry and already-created projection are delivered")
        val result = WorkflowWebOverview.create(_web_entry(component), projection)

        Then("the Web value copies every retained identity role Component provenance condition sequence and admitted navigation unchanged")
        val overview = result.toOption.get
        overview.context shouldBe projection.context
        overview.component shouldBe component
        overview.subjects.map { subject =>
          (
            subject.sourceSubjectId,
            subject.component,
            subject.semanticTargetId,
            subject.role,
            subject.attribution,
            subject.condition,
            subject.stableTieKey,
            subject.navigationTarget
          )
        } shouldBe Vector(
          (
            activity.id,
            activity.component,
            activity.semanticTargetId,
            activity.role,
            activity.attribution,
            activity.condition,
            activity.stableTieKey,
            None
          ),
          (
            workflow.id,
            workflow.component,
            workflow.semanticTargetId,
            workflow.role,
            workflow.attribution,
            workflow.condition,
            workflow.stableTieKey,
            projection.subjects.find(_.sourceSubject.id == workflow.id).flatMap(_.navigationTarget)
          )
        )
        overview.flows.map { delivered =>
          (
            delivered.sourceFlowId,
            delivered.component,
            delivered.semanticFlowId,
            delivered.workflowSemanticTargetId,
            delivered.role,
            delivered.direction,
            delivered.sourceOwnedSequenceKey,
            delivered.sourceEndpoint,
            delivered.targetEndpoint,
            delivered.attribution,
            delivered.condition,
            delivered.stableTieKey,
            delivered.forwardNavigationTarget,
            delivered.reverseNavigationTarget
          )
        } shouldBe Vector(
          (
            flow.id,
            flow.component,
            flow.semanticFlowId,
            flow.workflowSemanticTargetId,
            flow.role,
            flow.direction,
            flow.sourceOwnedSequenceKey,
            _web_endpoint(source),
            _web_endpoint(target),
            flow.attribution,
            flow.condition,
            flow.stableTieKey,
            Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted")),
            Some(_endpoint_navigation("endpoint-accept-order-source", component, "activity-accept-order"))
          )
        )
        overview.gaps shouldBe Vector(
          WorkflowWebOverviewGap(
            gap.id,
            gap.component,
            gap.requestedRole,
            gap.boundedFieldOrScope,
            gap.affectedSemanticTargetId,
            gap.attribution,
            gap.condition,
            gap.limitationReason,
            gap.stableTieKey,
            None
          )
        )
        overview.forwardFlows shouldBe overview.flows
        overview.reverseFlows shouldBe overview.flows
      }

      "preserve an attributable bounded gap without inferring a subject endpoint flow or navigation target" in {
        Given("one retained Workflow and an unavailable bounded gap with no affected identity")
        val component = _component()
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val condition = _condition(
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
          condition = condition,
          reason = "The admitted Cozy field cannot supply the requested direct Workflow flow relation."
        )
        val projection = _projection(component, Vector(workflow), Vector.empty, Vector(gap))

        When("the gap-only bounded delivery is created")
        val result = WorkflowWebOverview.create(_web_entry(component), projection)

        Then("the exact bounded provenance condition limitation and absence remain visible without a proxy or inferred relation")
        result.toOption.map(_.subjects.map(_.semanticTargetId.value)) shouldBe Some(Vector("workflow-order-fulfillment"))
        result.toOption.map(_.flows) shouldBe Some(Vector.empty)
        result.toOption.map(_.gaps.map { delivered =>
          (
            delivered.sourceGapId,
            delivered.affectedSemanticTargetId,
            delivered.boundedFieldOrScope,
            delivered.attribution,
            delivered.condition,
            delivered.limitationReason,
            delivered.navigationTarget
          )
        }) shouldBe Some(
          Vector(
            (
              gap.id,
              None,
              gap.boundedFieldOrScope,
              gap.attribution,
              condition,
              gap.limitationReason,
              None
            )
          )
        )
      }

      "preserve inherited identity and source-owned sequence order under ScalaCheck caller permutations" in {
        Given("three direct subjects flows and gaps whose admitted order is independent of caller iteration")
        val component = _component()
        val subjects = Vector(
          _subject("workflow-zulu", component, "workflow-zulu", WorkflowProjectionWorkflow),
          _subject("workflow-alpha", component, "workflow-alpha", WorkflowProjectionWorkflow),
          _subject("activity-alpha", component, "activity-alpha", WorkflowProjectionActivity)
        )
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

        forAll(Gen.pick(subjects.size, subjects), Gen.pick(flows.size, flows), Gen.pick(gaps.size, gaps)) {
          (suppliedsubjects, suppliedflows, suppliedgaps) =>
            When("a property-generated caller permutation is projected before exact Web delivery")
            val projection = _projection(component, suppliedsubjects.toVector, suppliedflows.toVector, suppliedgaps.toVector)
            val result = WorkflowWebOverview.create(_web_entry(component), projection)

            Then("the delivery retains only inherited identity order and exact source-owned sequence rather than caller order")
            result.toOption.toVector.flatMap(_.subjects.map(_.semanticTargetId.value)) shouldBe
              subjects.sortBy(subject => (subject.semanticTargetId.value, subject.id, subject.stableTieKey.getOrElse(""))).map(_.semanticTargetId.value)
            result.toOption.toVector.flatMap(_.flows.map(_.sourceFlowId)) shouldBe
              flows.sortBy(flow => (
                flow.workflowSemanticTargetId.value,
                flow.sourceOwnedSequenceKey.value,
                flow.semanticFlowId.value,
                flow.sourceEndpoint.semanticTargetId.value,
                flow.targetEndpoint.semanticTargetId.value,
                flow.id,
                flow.stableTieKey.getOrElse("")
              )).map(_.id)
            result.toOption.toVector.flatMap(_.gaps.map(_.sourceGapId)) shouldBe
              gaps.sortBy(gap => (gap.affectedSemanticTargetId.map(_.value).getOrElse(""), gap.id, gap.stableTieKey.getOrElse(""))).map(_.id)
        }
      }

      "copy only projection-admitted forward and reverse navigation while retaining suppressed-flow evidence" in {
        Given("one usable flow and one independently retained flow whose unavailable endpoint condition suppresses both affordances")
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
        val projection = _projection(component, Vector(workflow), Vector(conditioned, usable), Vector.empty)

        When("the already-gated directed projection is delivered")
        val result = WorkflowWebOverview.create(_web_entry(component), projection)

        Then("the existing exact usable targets are copied and every disqualified affordance remains absent without removing its flow")
        result.toOption.toVector.flatMap(_.flows.map { delivered =>
          delivered.sourceFlowId -> (delivered.forwardNavigationTarget, delivered.reverseNavigationTarget)
        }) shouldBe Vector(
          "flow-accept-order" -> (
            Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted")),
            Some(_endpoint_navigation("endpoint-accept-order-source", component, "activity-accept-order"))
          ),
          "flow-condition-limited" -> (None, None)
        )
        result.toOption.toVector.flatMap(_.forwardFlows.map(_.sourceFlowId)) shouldBe
          Vector("flow-accept-order", "flow-condition-limited")
        result.toOption.toVector.flatMap(_.reverseFlows.map(_.sourceFlowId)) shouldBe
          Vector("flow-accept-order", "flow-condition-limited")
      }

      "return typed no-partial failures for mismatched delivery and manually malformed projection navigation" in {
        Given("one valid exact projection plus a different authorized entry and a manually altered admitted navigation value")
        val component = _component()
        val othercomponent = _component("textus-payment")
        val workflow = _subject("workflow-order-fulfillment", component, "workflow-order-fulfillment", WorkflowProjectionWorkflow)
        val flow = _flow(
          "flow-accept-order",
          component,
          "flow-accept-order",
          "workflow-order-fulfillment",
          "010",
          _endpoint("endpoint-accept-order-source", component, "activity-accept-order", "activity-source"),
          _endpoint(
            "endpoint-accept-order-target",
            component,
            "event-order-accepted",
            "event-target",
            navigation = Some(_endpoint_navigation("endpoint-accept-order-target", component, "event-order-accepted"))
          )
        )
        val projection = _projection(component, Vector(workflow), Vector(flow), Vector.empty)
        val malformedflow = projection.flows.head.copy(forwardNavigationTarget = None)
        val malformedprojection = WorkflowProjection(
          projection.context,
          projection.component,
          projection.subjects,
          Vector(malformedflow),
          projection.gaps,
          Vector(malformedflow),
          Vector(malformedflow)
        )

        When("each exact boundary violation is submitted without rebuilding a near projection")
        val mismatchresult = WorkflowWebOverview.create(_web_entry(othercomponent), projection)
        val malformedresult = WorkflowWebOverview.create(_web_entry(component), malformedprojection)

        Then("both return typed failures and no partial overview is exposed")
        mismatchresult.isLeft shouldBe true
        mismatchresult.toOption shouldBe empty
        mismatchresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow Web overview delivered Component 'textus-payment' does not match Projection Component 'textus-workflow'."
        )
        malformedresult.isLeft shouldBe true
        malformedresult.toOption shouldBe empty
        malformedresult.left.toOption.toVector.flatMap(_.violations) should contain(
          "Workflow flow 'flow-accept-order' forward endpoint 'endpoint-accept-order-target' projected navigation target must retain only the exact admitted target."
        )
      }
    }
  }

  private def _context(contextid: String = "textus-workflow-ccdm"): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity(contextid)

  private def _component(componentid: String = "textus-workflow"): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _web_entry(component: ComponentDashboardComponentIdentity): ComponentDashboardWebEntry =
    ComponentDashboardWebEntry.create(
      ComponentDashboardWebEntryAuthorizationAffirmative(component),
      ComponentDashboard(component, Vector.empty, Vector.empty)
    ).toOption.get

  private def _projection(
    component: ComponentDashboardComponentIdentity,
    subjects: Vector[WorkflowProjectionSubject],
    flows: Vector[WorkflowProjectionFlow],
    gaps: Vector[WorkflowProjectionGap]
  ): WorkflowProjection =
    WorkflowProjection.create(_context(), component, subjects, flows, gaps).toOption.get

  private def _attribution(sourceid: String): ComponentDashboardSourceAttribution =
    ComponentDashboardSourceAttribution(sourceid, s"$sourceid-authority", s"$sourceid:locator")

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
    attribution: ComponentDashboardSourceAttribution,
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

  private def _subject_navigation(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    _navigation(targetid, component, _attribution(s"subject-$subjectid"), condition)

  private def _endpoint_navigation(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    _navigation(targetid, component, _attribution(s"endpoint-$endpointid"), condition)

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    role: WorkflowProjectionRole,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    tiekey: Option[String] = None
  ): WorkflowProjectionSubject =
    WorkflowProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      role,
      _attribution(s"subject-$subjectid"),
      condition,
      tiekey,
      navigation
    )

  private def _endpoint(
    endpointid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    role: String,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None,
    tiekey: Option[String] = None
  ): WorkflowProjectionFlowEndpoint =
    WorkflowProjectionFlowEndpoint(
      endpointid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      WorkflowProjectionFlowEndpointRole(role),
      _attribution(s"endpoint-$endpointid"),
      condition,
      tiekey,
      navigation
    )

  private def _flow(
    flowid: String,
    component: ComponentDashboardComponentIdentity,
    semanticflowid: String,
    workflowtargetid: String,
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
      ComponentDashboardSemanticTargetIdentity(workflowtargetid),
      WorkflowProjectionFlowRelation,
      WorkflowProjectionFlowDirection(direction),
      WorkflowProjectionSourceOwnedSequenceKey(sequencekey),
      source,
      target,
      _attribution(s"flow-$flowid"),
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
      _attribution(s"gap-$gapid"),
      condition,
      reason,
      tiekey,
      navigation
    )

  private def _web_endpoint(endpoint: WorkflowProjectionFlowEndpoint): WorkflowWebOverviewFlowEndpoint =
    WorkflowWebOverviewFlowEndpoint(
      endpoint.id,
      endpoint.component,
      endpoint.semanticTargetId,
      endpoint.role,
      endpoint.attribution,
      endpoint.condition,
      endpoint.stableTieKey
    )
}
