package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.context.{ExecutionContext, Principal, PrincipalId, SubjectKind}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ModelEditSession.*
import ModelEditOperationJson.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import ModelEditApplicationSpecFixture.*
  import ModelEditProjectionSpecFixture.{record, selection}

  "One admitted Model Edit application" should {
    "apply through the shared service and expose the successor consistently in all ordinary reads" in {
      Given("one server-owned session, source, policy and exact principal grant")
      When("a contextual Goal refinement is applied and its successor is inspected")
      val fixture = create()
      given ExecutionContext = principalContext("chatgpt")
      val request = refinement(fixture.current)
      val expected = ModelEditService.applyOperation(fixture.current, request.operation, request.successorCandidate,
        ModelEditSpecFixture.authorization(fixture.current), Vector.empty).toOption.get
      val applied = successfulValue(fixture.application.execute("applyModelEdit", edit(request)))
      val reads = Vector("inspectModelEdit", "getModelEditCandidate", "getModelEditView", "getModelEditTraces", "getModelEditReview", "getModelEditDiff")
        .map(name => name -> successfulValue(fixture.application.execute(name, ModelEditOperationJson.selection(selected(expected))))).toMap
      val stale = fixture.application.execute("getModelEditCandidate", ModelEditOperationJson.selection(selected(fixture.current)))
      val applieddata = parsed(applied).hcursor.downField("data").focus.get
      val readcandidates = reads.values.map(response => parsed(response).hcursor.downField("candidate").focus.get).toSet
      val expecteddata = ModelEditCandidateCodec.encode(expected.candidate)
      val expectedreference = ModelEditOperationJson.record(expected.candidate.reference)
      val originalsource = selection(fixture.publication)
      Then("candidate identity, exact history and canonical-basis confirmation remain coherent without promotion")
      applieddata shouldBe expecteddata
      readcandidates shouldBe Set(expectedreference)
      reads("getModelEditView").html should include("Candidate")
      reads("getModelEditTraces").resultJson should include("I_kwDOOrderIssue42")
      reads("getModelEditReview").resultJson should include("unresolved")
      reads("getModelEditDiff").status shouldBe "UNAVAILABLE"
      reads("getModelEditDiff").resultJson should include("Phase10AdmissionNotSupplied")
      reads("getModelEditDiff").resultJson should include(request.operation.reference.recordId.value)
      fixture.source shouldBe originalsource
      stale.isSuccess shouldBe false
    }

    "reject fabricated external identities and retain the original candidate after a failed edit" in {
      Given("an exact external edge whose supplied ticket identity has no host resolution evidence")
      When("a caller proposes that altered edge and then reads the candidate")
      val fixture = create()
      given ExecutionContext = principalContext("codex")
      val edge = fixture.current.candidate.ticketLinks.head.copy(reference = record("new-ticket-edge"),
        ticket = fixture.current.candidate.ticketLinks.head.ticket.copy(ticketIdentity = record("fabricated-ticket").recordId))
      val operation = ModelEditSpecFixture.operationFor(fixture.current, Command.LinkSliceTicket(edge))
      val request = EditRequest(selected(fixture.current), ModelEditPalette.Context(ModelEditPalette.Surface.EventStorming,
        fixture.current.scope, Subject.Slice(edge.sliceIdentity)), operation, record("candidate", 99))
      val rejected = fixture.application.execute("applyModelEdit", edit(request))
      val candidate = successfulValue(fixture.application.execute("getModelEditCandidate", ModelEditOperationJson.selection(selected(fixture.current))))
      val candidatedata = parsed(candidate).hcursor.downField("data").focus.get
      val expected = ModelEditCandidateCodec.encode(fixture.current.candidate)
      Then("ordinary validation prevents an external identity from becoming an admitted fact or mutating a ticket")
      rejected.isSuccess shouldBe false
      candidatedata shouldBe expected
    }

    "reject unknown fields, mismatched contexts and unsupported mutation paths" in {
      Given("the closed external request schema and one candidate")
      When("requests try to add a grant, change scope or invoke canonical/ticket mutation")
      val fixture = create()
      given ExecutionContext = principalContext("chatgpt")
      val request = refinement(fixture.current)
      val injected = io.circe.parser.parse(edit(request)).toOption.get.mapObject(_.add("authorization", Json.obj())).noSpaces
      val outside = request.copy(context = request.context.copy(scope = fixture.current.scope.copy(component = ComponentDashboardComponentIdentity("other"))))
      val failures = Vector(fixture.application.execute("applyModelEdit", injected), fixture.application.execute("applyModelEdit", edit(outside)),
        fixture.application.execute("promoteCanonicalModel", edit(request)), fixture.application.execute("updateExternalTicket", edit(request)))
      Then("the application has no privileged route or request-side fact/admission injection")
      failures.map(_.isSuccess) shouldBe Vector.fill(4)(false)
    }

    "inspect the existing Phase10 diff through its source mapping and expose its staleness after another edit" in {
      Given("the accepted source projection fixture and the real captured Phase10 admission route")
      val basis = ModelEditProjectionSpecFixture.basisPublication()
      val proposed = ModelEditProjectionSpecFixture.proposedPublication()
      When("its confirmed candidate/diff is admitted, inspected and then refined through the same application")
      val current = ModelEditProjectionSpecFixture.editedSession(basis, proposed)
      val source = selection(basis)
      val plan = ModelEditCmlProjection.prepare(source, current, proposed.source, proposed.document, selection(proposed).bindings).toOption.get
      val rendering = ModelEditSourceProjection.Rendering(plan.request.schemaVersion, basis.source, basis.producer, proposed.source, "fixture proposed CML", Vector.empty)
      val confirmed = ModelEditCmlProjection.confirm(plan, rendering, proposed).toOption.get
      val captured = ModelEditPhase10SpecFixture.prepare(confirmed, current, "fixture canonical CML".getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector).toOption.get
      val grant = ModelEditSpecFixture.authorization(current)
      val policy = FindingPolicy(record("policy"), FindingKind.values.map(kind => kind -> FindingDisposition(Severity.Warning, false)).toMap)
      val application = ModelEditApplication.createWithDiff(current, source, Vector(ModelEditAuthorization.Binding("codex", true, Some(grant))),
        policy, Vector.empty, Vector.empty, Vector.empty, captured.mapping, captured.diff).toOption.get
      given ExecutionContext = principalContext("codex")
      val exact = successfulValue(application.execute("getModelEditDiff", ModelEditOperationJson.selection(selected(current))))
      val request = refinement(current)
      val applied = successfulValue(application.execute("applyModelEdit", edit(request)))
      val stale = successfulValue(application.execute("getModelEditDiff", ModelEditOperationJson.selection(Selection(current.reference, request.successorCandidate))))
      val exactdiff = parsed(exact).hcursor.downField("data").downField("semanticDiff").focus.get
      val expected = ModelEditOperationJson.semanticDiff(captured.diff.diff)
      val stalediff = parsed(stale).hcursor.downField("data").downField("semanticDiff").focus.get
      Then("semantic diff retains actual Phase10 evidence and cannot approve or describe a different candidate")
      exact.status shouldBe "OK"
      exactdiff shouldBe expected
      applied.status shouldBe "OK"
      stale.status shouldBe "UNAVAILABLE"
      stale.resultJson should include("StalePhase10Admission")
      stalediff shouldBe Json.Null
    }
  }
}

private[runtime] object ModelEditApplicationSpecFixture {
  import ModelEditProjectionSpecFixture.*
  final case class Fixture(current: ModelEditSession, publication: ModelEditSourceProjection.Publication,
    source: ModelEditSourceProjection.Selection, application: ModelEditApplication)
  private final case class SpecPrincipal(id: PrincipalId, attributes: Map[String, String]) extends Principal

  def principalContext(principal: String): ExecutionContext = {
    val base = ExecutionContext.create()
    ExecutionContext.withSecurityContext(base, base.security.copy(principal = SpecPrincipal(PrincipalId(principal), Map.empty),
      subjectKind = SubjectKind.User))
  }
  def create(): Fixture = {
    val publication = proposedPublication()
    val current = session(publication)
    val source = selection(publication)
    val grant = ModelEditSpecFixture.authorization(current)
    val bindings = Vector("chatgpt", "codex").map(name => ModelEditAuthorization.Binding(name, true, Some(grant))) :+
      ModelEditAuthorization.Binding("reader", true, None)
    val policy = FindingPolicy(record("external-project-policy"), FindingKind.values.map(kind => kind -> FindingDisposition(Severity.Warning, false)).toMap)
    val application = ModelEditApplication.create(current, source, bindings, policy, Vector.empty, Vector.empty, Vector.empty).toOption.get
    Fixture(current, publication, source, application)
  }
  def selected(session: ModelEditSession): Selection = Selection(session.reference, session.candidate.reference)
  def refinement(session: ModelEditSession): EditRequest = {
    val goal = session.candidate.goals.head.reference
    val operation = ModelEditSpecFixture.operationFor(session, Command.RefineGoal(goal, "Refined by an ordinary authorized client"))
    EditRequest(selected(session), ModelEditPalette.Context(ModelEditPalette.Surface.ActorGoal, session.scope, Subject.Goal(goal.semanticTargetId)),
      operation, session.candidate.reference.copy(recordRevision = InternalModelRecordRevision.from(session.candidate.reference.recordRevision.value + 1).toOption.get))
  }
  def successfulValue[A](result: Consequence[A]): A = result match {
    case Consequence.Success(value) => value
    case Consequence.Failure(error) => throw new IllegalStateException(error.toString)
  }
  def parsed(response: ModelEditApplication.Response): Json = io.circe.parser.parse(response.resultJson).toOption.get
}
