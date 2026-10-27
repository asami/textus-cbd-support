package org.simplemodeling.textus.cbdsupport.runtime

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen
import ModelEditSourceProjection.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class ModelEditSourceProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import ModelEditProjectionSpecFixture.*
  "Source-owned model admission" should {
    "retain independent Actor, Goal, Slice, fields and external trace identities through the supported JSON reader" in {
      Given("an explicitly qualified source publication and supplied CBD mappings")
      val publication = proposedPublication()
      val selected = selection(publication)
      When("the publication is encoded, read and imported")
      val decoded = ModelEditSourceProjectionCodec.decodePublication(ModelEditSourceProjectionCodec.encodePublication(publication))
      val candidate = decoded.flatMap(value => importCandidate(selected.copy(publication = value), record("candidate"), scope))
      Then("source values and independent endpoints retain their meaning")
      decoded shouldBe Right(publication)
      candidate.toOption.get.actors.map(_.semanticTargetId.value) shouldBe Vector("customer")
      candidate.toOption.get.goals.map(_.intent) shouldBe Vector("Place another order")
      candidate.toOption.get.slices.map(_.lifecycle) shouldBe Vector(ModelEditSession.SliceLifecycle.Proposed)
      candidate.toOption.get.specificationLinks.head.specification.specificationIdentity.value shouldBe "OrderSubmissionSpec"
      candidate.toOption.get.ticketLinks.head.ticket.ticketIdentity.value shouldBe "I_kwDOOrderIssue42"
      decoded.toOption.get.document.elements.find(_.kind == "slice").get.fields shouldBe Vector(Field("requestId", "string", "1"))
    }
    "reject guessed identity mapping and unknown producer fields for arbitrary labels" in {
      forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { label =>
        Given("a source label that is unrelated to the caller-selected identity")
        val publication = proposedPublication().copy(producer = producer.copy(version = label))
        val selected = selection(publication).copy(bindings = selection(publication).bindings.filterNot(_.sourceRef.identity == "repeat-purchase"))
        When("the incomplete mapping and an extended wire record are admitted")
        val missing = importCandidate(selected, record("candidate"), scope)
        val unknown = ModelEditSourceProjectionCodec.decodePublication(ModelEditSourceProjectionCodec.encodePublication(publication).dropRight(1) + ",\"guessed\":true}")
        Then("neither names nor unknown JSON create a source capability")
        missing.isLeft shouldBe true
        unknown.isLeft shouldBe true
      }
    }
    "preserve an unlinked Actor in the canonical basis without making a Use Case relationship" in {
      Given("a source basis with an Actor and Use Case but no Goal edges")
      val selected = selection(basisPublication())
      When("the canonical basis becomes an editing candidate")
      val candidate = importCandidate(selected, record("candidate"), scope)
      Then("the independent Actor can be selected while communication references remain empty")
      candidate.toOption.get.actors.size shouldBe 1
      candidate.toOption.get.useCases.head.references shouldBe empty
    }
  }
}

/** Pure source fixtures. Only ModelEditProducerIntegrationSpec claims actual Cozy production. */
private[runtime] object ModelEditProjectionSpecFixture {
  import ModelEditSpecFixture.artifact
  val scope = ModelEditSpecFixture.scope
  val source = Source("example", "models/order.cml", "r1")
  val successor = source.copy(revision = "r2")
  val producer = Producer("cozy", "0.3.3-SNAPSHOT", "fixture-producer-revision", false)
  val snapshot = artifact("model-edit-source")
  val owner = ModelEditSession.RepositoryProjectReference(record("example/order").recordId, record("order-project").recordId)
  val baseIds: Vector[String] = Vector("customer", "place-order", "submit", "validate", "order", "line", "order-placed", "fulfil", "sales", "orders", "order-view",
    "submit-causes-placed", "placed-fulfil", "fulfil-warehouse", "place-query-context")
  val newIds: Vector[String] = Vector("repeat-purchase", "submit-repeat", "customer-repeat", "repeat-place", "place-repeat", "repeat-spec", "repeat-ticket")
  def ref(value: String): RecordRef = RecordRef("example", value, 1)
  def binding(value: String): Binding = Binding(ref(value), identity(value), record(value), value)
  def selection(publication: Publication): Selection = Selection(publication, snapshot,
    (baseIds ++ newIds).map(binding), Vector(OwnerBinding("example/order", owner)), ComponentDashboardCondition("available", "caller-admitted", None, None, None, None, None, None, Vector.empty))
  def record(value: String, revision: Long = 1): InternalModelRecordReference = ModelEditSpecFixture.record(value, revision)
  def identity(value: String): ComponentDashboardSemanticTargetIdentity = ModelEditSpecFixture.identity(value)
  def element(value: String, kind: String, title: String): Element = Element(ref(value), kind, title, None, Vector.empty, Vector.empty, Vector.empty, None, source)
  def edge(value: String, kind: String, from: String, to: Endpoint): Relationship = Relationship(ref(value), kind, Endpoint.Local(ref(from)), to, source)
  def basisPublication(): Publication = {
    val elements = Vector(element("customer", "actor", "Customer"), element("place-order", "use-case", "Place order"),
      element("submit", "command", "Submit"), element("validate", "operation", "Validate"), element("order", "aggregate", "Order"),
      element("line", "entity", "Line"), element("order-placed", "domain-event", "Order placed"), element("fulfil", "policy", "Fulfil"),
      element("sales", "component", "Sales"), element("orders", "query", "Orders"), element("order-view", "view", "Order view"))
    val relationships = Vector(edge("submit-causes-placed", "cause", "submit", Endpoint.Local(ref("order-placed"))),
      edge("placed-fulfil", "reaction", "order-placed", Endpoint.Local(ref("fulfil"))),
      edge("fulfil-warehouse", "dependency", "fulfil", Endpoint.External(RecordRef("logistics", "warehouse", 3))),
      edge("place-query-context", "context", "place-order", Endpoint.Local(ref("orders"))))
    val missing = Set("goal", "slice", "actor-goal", "goal-use-case", "use-case-slice", "slice-specification", "slice-ticket")
    val coverage = families.map(value => Coverage(value, if (missing(value)) "missing" else "present", if (missing(value)) Some("Not declared in this complete basis") else None))
    Publication("cozy.cml.model-edit-production.v1", source, producer, Document("cozy.cml.model-edit.v1", coverage, elements, relationships), Vector.empty)
  }
  def proposedPublication(): Publication = {
    val basis = basisPublication()
    val goal = element("repeat-purchase", "goal", "Repeat purchase").copy(intent = Some("Place another order"), source = successor)
    val slice = element("submit-repeat", "slice", "Submit repeat order").copy(intent = Some("Submit one explicitly selected repeat order"),
      fields = Vector(Field("requestId", "string", "1")), scope = Vector(ref("place-order")), lifecycle = Some("Proposed"), source = successor)
    val specification = Specification("example/order", "OrderSubmissionSpec", "spec-r7")
    val ticket = Ticket("github", "example/order", "I_kwDOOrderIssue42", Some(Observation("open", Some("observed-r4"))))
    val edges = Vector(edge("customer-repeat", "actor-goal", "customer", Endpoint.Local(ref("repeat-purchase"))),
      edge("repeat-place", "goal-use-case", "repeat-purchase", Endpoint.Local(ref("place-order"))),
      edge("place-repeat", "use-case-slice", "place-order", Endpoint.Local(ref("submit-repeat"))),
      edge("repeat-spec", "slice-specification", "submit-repeat", Endpoint.SpecificationTarget(specification)),
      edge("repeat-ticket", "slice-ticket", "submit-repeat", Endpoint.TicketTarget(ticket))).map(_.copy(source = successor))
    basis.copy(source = successor, document = basis.document.copy(coverage = families.map(value => Coverage(value, "present", None)),
      elements = basis.document.elements ++ Vector(goal, slice), relationships = basis.document.relationships ++ edges))
  }
  def session(publication: Publication = proposedPublication()): ModelEditSession =
    ModelEditSession(record("session"), scope, importCandidate(selection(publication), record("candidate"), scope).toOption.get, ModelEditSession.SessionState.Active)
  def editedSession(basis: Publication, proposed: Publication): ModelEditSession = {
    import ModelEditSession.*
    val initial = session(basis)
    val desired = session(proposed).candidate
    val actor = initial.candidate.actors.head
    val goal = desired.goals.head
    val added = ModelEditSpecFixture.applyCommand(initial, Command.AddGoal(actor, goal.reference.semanticTargetId, goal.intent, Some(goal.reference)))
    val sliced = ModelEditSpecFixture.applyCommand(added, Command.CreateUseCaseSlice(desired.slices.head))
    val linked = ModelEditSpecFixture.applyCommand(sliced, Command.LinkGoalUseCase(desired.goalLinks.head))
    val specification = desired.specificationLinks.head
    val ticket = desired.ticketLinks.head
    val withspec = ModelEditSpecFixture.applyCommand(linked, Command.LinkSliceSpecification(specification),
      Vector(ResolutionEvidence(ExternalReference.Specification(specification.specification), ResolutionState.Resolved, snapshot, specification.attribution)))
    ModelEditSpecFixture.applyCommand(withspec, Command.LinkSliceTicket(ticket),
      Vector(ResolutionEvidence(ExternalReference.Ticket(ticket.ticket), ResolutionState.Resolved, snapshot, ticket.attribution)))
  }
}
