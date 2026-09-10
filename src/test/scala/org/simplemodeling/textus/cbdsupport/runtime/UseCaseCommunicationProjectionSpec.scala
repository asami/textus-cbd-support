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
final class UseCaseCommunicationProjectionSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckDrivenPropertyChecks {
  "UseCaseCommunicationProjection" should {
    "P9-33B pure read-only Use Case communication projection" which {
      "retain every admitted communication role, exact relationship, provenance, locator, and condition" in {
        Given("one exact Use Case with all explicitly admitted communication roles and one source-owned flow")
        val component = _component()
        val subjectcondition = _condition(limitations = Vector("bounded-use-case-scope"))
        val references = Vector(
          _reference("actor", component, "actor-buyer", "relationship-use-case-actor", Actor),
          _reference("goal", component, "goal-place-order", "relationship-use-case-goal", Goal),
          _reference("trigger", component, "trigger-cart-submitted", "relationship-use-case-trigger", Trigger),
          _reference("postcondition", component, "postcondition-order-accepted", "relationship-use-case-postcondition", Postcondition),
          _reference("domain-element", component, "entity-order", "relationship-use-case-domain", DomainElement),
          _reference("collaborator", component, "collaborator-payment", "relationship-use-case-collaborator", Collaborator),
          _reference("workflow", component, "workflow-place-order", "relationship-use-case-workflow", RealizingWorkflow),
          _reference("mono-koto", component, "event-order-placed", "relationship-use-case-mono-koto", MonoKoto)
        )
        val flow = _flow(
          "flow-place-order",
          component,
          "flow-place-order",
          "relationship-use-case-flow",
          Vector(
            _step("step-confirm", component, "command-confirm-order", "relationship-flow-confirm", "20"),
            _step("step-accept", component, "event-order-accepted", "relationship-flow-accept", "10")
          ),
          condition = _condition(conflict = Some("displayed-conflict"))
        )
        val subject = _subject(
          "use-case-place-order",
          component,
          "use-case-place-order",
          references,
          Vector(flow),
          condition = subjectcondition
        )

        When("the caller supplies the exact already-admitted records")
        val result = UseCaseCommunicationProjection.create(_context(), component, Vector(subject))

        Then("the projected ledger retains all source values without completing or reconstructing a role")
        val projected = result.toOption.get.subjects.head
        projected.sourceSubject shouldBe subject
        projected.references.map(_.sourceReference) should contain theSameElementsAs references
        projected.references.map(_.sourceReference.referenceKind).toSet shouldBe Set(
          Actor,
          Goal,
          Trigger,
          Postcondition,
          DomainElement,
          Collaborator,
          RealizingWorkflow,
          MonoKoto
        )
        projected.flows.head.sourceFlow shouldBe flow
        projected.flows.head.steps.map(_.sourceFlowStep) shouldBe Vector(flow.steps(1), flow.steps(0))
        projected.sourceSubject.condition shouldBe subjectcondition
        projected.flows.head.sourceFlow.condition shouldBe flow.condition
      }

      "present subjects, references, and flows by exact admitted identities independently of caller iteration" in {
        Given("three exact Use Cases with distinct related records in reverse caller order")
        val component = _component()
        val alpha = _subject(
          "use-case-alpha",
          component,
          "use-case-alpha",
          Vector(
            _reference("alpha-zulu", component, "target-zulu", "relationship-alpha-zulu", Actor),
            _reference("alpha-alpha", component, "target-alpha", "relationship-alpha-alpha", Goal)
          ),
          Vector(_flow("flow-alpha-zulu", component, "flow-zulu", "relationship-flow-zulu", Vector.empty), _flow("flow-alpha-alpha", component, "flow-alpha", "relationship-flow-alpha", Vector.empty))
        )
        val middle = _subject("use-case-middle", component, "use-case-middle", Vector.empty, Vector.empty)
        val zulu = _subject("use-case-zulu", component, "use-case-zulu", Vector.empty, Vector.empty)
        val expectedsubjects = Vector("use-case-alpha", "use-case-middle", "use-case-zulu")
        val expectedreferences = Vector("target-alpha", "target-zulu")
        val expectedflows = Vector("flow-alpha", "flow-zulu")

        forAll(Gen.pick(3, Vector(alpha, middle, zulu))) { permutedsubjects =>
          When("a property-generated subject permutation is supplied")
          val result = UseCaseCommunicationProjection.create(_context(), component, permutedsubjects.toVector)

          Then("subject, reference, and flow presentation use exact identities rather than source iteration")
          result.toOption.toVector.flatMap(_.subjects.map(_.sourceSubject.semanticTargetId.value)) shouldBe expectedsubjects
          result.toOption.toVector.flatMap(_.subjects.head.references.map(_.sourceReference.semanticTargetId.value)) shouldBe expectedreferences
          result.toOption.toVector.flatMap(_.subjects.head.flows.map(_.sourceFlow.semanticTargetId.value)) shouldBe expectedflows
        }
      }

      "preserve explicit source-owned flow sequence keys instead of caller iteration" in {
        Given("one flow whose admitted step vector is reverse to its source-owned sequence keys")
        val component = _component()
        val flow = _flow(
          "flow-place-order",
          component,
          "flow-place-order",
          "relationship-use-case-flow",
          Vector(
            _step("step-final", component, "event-order-accepted", "relationship-final", "030"),
            _step("step-initial", component, "command-place-order", "relationship-initial", "010"),
            _step("step-middle", component, "activity-validate-order", "relationship-middle", "020")
          )
        )
        val subject = _subject("use-case-place-order", component, "use-case-place-order", Vector.empty, Vector(flow))

        When("the immutable factory projects the admitted flow")
        val result = UseCaseCommunicationProjection.create(_context(), component, Vector(subject))

        Then("only the source-owned sequence key determines the displayed flow order")
        result.toOption.toVector.flatMap(_.subjects.head.flows.head.steps.map(_.sourceFlowStep.id)) shouldBe
          Vector("step-initial", "step-middle", "step-final")
      }

      "suppress ineligible navigation while retaining every exact affected source record" in {
        Given("a subject, reference, flow, and flow step with non-usable independent conditions")
        val component = _component()
        val referencecondition = _condition(authorization = "denied", redaction = Some("reference-redaction"))
        val flowcondition = _condition(ambiguity = Some("flow-ambiguity"))
        val stepcondition = _condition(limitations = Vector("step-boundary"))
        val reference = _reference(
          "workflow",
          component,
          "workflow-place-order",
          "relationship-use-case-workflow",
          RealizingWorkflow,
          referencecondition,
          Some(_navigation("workflow-place-order", component))
        )
        val step = _step(
          "step-workflow",
          component,
          "workflow-place-order",
          "relationship-flow-workflow",
          "010",
          stepcondition,
          Some(_navigation("workflow-place-order", component))
        )
        val flow = _flow(
          "flow-place-order",
          component,
          "flow-place-order",
          "relationship-use-case-flow",
          Vector(step),
          flowcondition,
          Some(_navigation("flow-place-order", component))
        )
        val subject = _subject(
          "use-case-place-order",
          component,
          "use-case-place-order",
          Vector(reference),
          Vector(flow),
          navigation = Some(_navigation("use-case-place-order", component, implementedandusable = false))
        )

        When("the exact records retain a target but its implementation or any affected condition is unusable")
        val result = UseCaseCommunicationProjection.create(_context(), component, Vector(subject))

        Then("all source records remain visible and every ineligible navigation affordance is suppressed")
        val projected = result.toOption.get.subjects.head
        projected.sourceSubject shouldBe subject
        projected.navigationTarget shouldBe empty
        projected.references.head.sourceReference shouldBe reference
        projected.references.head.navigationTarget shouldBe empty
        projected.flows.head.sourceFlow shouldBe flow
        projected.flows.head.navigationTarget shouldBe empty
        projected.flows.head.steps.head.sourceFlowStep shouldBe step
        projected.flows.head.steps.head.navigationTarget shouldBe empty
      }

      "return a typed no-partial failure for malformed identities and unresolved duplicate flow order" in {
        Given("otherwise admitted records with one blank relationship identity and one duplicate source-owned flow sequence key")
        val component = _component()
        val malformedreference = _reference("actor", component, "actor-buyer", " ", Actor)
        val duplicateflow = _flow(
          "flow-place-order",
          component,
          "flow-place-order",
          "relationship-use-case-flow",
          Vector(
            _step("step-one", component, "command-place-order", "relationship-one", "010"),
            _step("step-two", component, "event-order-placed", "relationship-two", "010")
          )
        )
        val subject = _subject("use-case-place-order", component, "use-case-place-order", Vector(malformedreference), Vector(duplicateflow))

        When("the factory receives malformed and unresolved source-owned records")
        val result = UseCaseCommunicationProjection.create(_context(), component, Vector(subject))

        Then("it returns UseCaseCommunicationProjectionFailure with no partial projection")
        result.isLeft shouldBe true
        result.toOption shouldBe empty
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Use Case communication reference 'actor' semantic relationship identity must not be blank."
        )
        result.left.toOption.toVector.flatMap(_.violations) should contain(
          "Use Case communication flow 'flow-place-order' has unresolved duplicate source-owned sequence key '010'."
        )
      }
    }
  }

  private def _context(): MonoKotoProjectionContextIdentity =
    MonoKotoProjectionContextIdentity("textus-use-case-ccdm")

  private def _component(componentid: String = "textus-use-case"): ComponentDashboardComponentIdentity =
    ComponentDashboardComponentIdentity(componentid)

  private def _attribution(
    sourceid: String = "source",
    authorityscope: String = "authority",
    sourcelocator: String = "source:locator"
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
    implementedandusable: Boolean = true,
    condition: ComponentDashboardCondition = _condition()
  ): ComponentDashboardNavigationTarget =
    ComponentDashboardNavigationTarget(
      ComponentDashboardSemanticTargetIdentity(targetid),
      component,
      _attribution("navigation", "navigation-authority", "navigation:locator"),
      condition,
      implementedandusable
    )

  private def _reference(
    referenceid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    relationshipid: String,
    kind: UseCaseCommunicationReferenceKind,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): UseCaseCommunicationReference =
    UseCaseCommunicationReference(
      referenceid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      kind,
      _attribution(s"reference-$referenceid", "reference-authority", s"reference:$referenceid"),
      condition,
      None,
      navigation
    )

  private def _step(
    stepid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    relationshipid: String,
    sequencekey: String,
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): UseCaseCommunicationFlowStep =
    UseCaseCommunicationFlowStep(
      stepid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      sequencekey,
      _attribution(s"step-$stepid", "step-authority", s"step:$stepid"),
      condition,
      None,
      navigation
    )

  private def _flow(
    flowid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    relationshipid: String,
    steps: Vector[UseCaseCommunicationFlowStep],
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): UseCaseCommunicationFlow =
    UseCaseCommunicationFlow(
      flowid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      ComponentDashboardSemanticTargetIdentity(relationshipid),
      _attribution(s"flow-$flowid", "flow-authority", s"flow:$flowid"),
      condition,
      steps,
      None,
      navigation
    )

  private def _subject(
    subjectid: String,
    component: ComponentDashboardComponentIdentity,
    targetid: String,
    references: Vector[UseCaseCommunicationReference],
    flows: Vector[UseCaseCommunicationFlow],
    condition: ComponentDashboardCondition = _condition(),
    navigation: Option[ComponentDashboardNavigationTarget] = None
  ): UseCaseCommunicationProjectionSubject =
    UseCaseCommunicationProjectionSubject(
      subjectid,
      component,
      ComponentDashboardSemanticTargetIdentity(targetid),
      _attribution(s"subject-$subjectid", "subject-authority", s"subject:$subjectid"),
      condition,
      references,
      flows,
      None,
      navigation
    )
}
