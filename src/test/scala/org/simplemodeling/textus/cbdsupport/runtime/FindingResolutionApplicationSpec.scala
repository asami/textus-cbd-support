package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.syntax.*
import org.goldenport.Consequence
import org.goldenport.cncf.context.{ExecutionContext, SessionContext, SubjectKind}
import org.goldenport.cncf.finding.FindingDispositionV1 as Disposition
import Disposition.{Coverage, Declaration, Finding, Inspection, Key, Kind, Target}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import FindingResolution.*
import FindingResolutionApplication.*
import FindingResolutionApplication.JsonProtocol.given

/*
 * @since   Nov.  5, 2026
 * @version Nov.  5, 2026
 */
final class FindingResolutionApplicationSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  import FindingResolutionApplicationSpecFixture.*

  "Authenticated finding preparation and decisions" should {
    "retained evidence and explicit developer scope" which {
      "retain the CNCF report and incomplete coverage while admitting only a selected subset" in {
        forAll(Gen.choose(2, 8)) { size =>
          Given("host inspection facts, existing accepted/deferred dispositions and independent developer direction")
          val fixture = createFindingFixture(size)
          given ExecutionContext = context()
          val selected = fixture.keys.take(size - 1)
          When("the developer imports, groups, proposes and explicitly admits part of a bulk proposal")
          val imported = executeFinding(fixture, "importFindingInspection", Selection(fixture.imported.reference).asJson)
          val grouped = executeFinding(fixture, "groupFindingResolutions", GroupingRequest(fixture.imported.reference,
            fixture.keys.take(1).map(key => Remediation(key, "Use host adapter"))).asJson)
          val proposed = executeFinding(fixture, "proposeFindingResolution", proposal(fixture).asJson)
          val admitted = executeFinding(fixture, "decideFindingResolution", decision(fixture, selected).asJson)
          val retained = executeFinding(fixture, "getFindingResolution", Selection(fixture.imported.reference).asJson)
          Then("workflow admission preserves original evidence and unresolved backlog and records the exact host provenance")
          retained shouldBe admitted.mapObject(_.add("operation", Json.fromString("getFindingResolution")))
          Vector(imported, grouped, proposed, admitted).foreach { value =>
            value.hcursor.get[Int]("rawFindingCount") shouldBe Right(size + 2)
            value.hcursor.get[Int]("actionableCount") shouldBe Right(size)
            value.hcursor.downField("inspection").focus shouldBe imported.hcursor.downField("inspection").focus
            value.hcursor.downField("declarations").focus shouldBe imported.hcursor.downField("declarations").focus
            value.hcursor.downField("report").focus shouldBe imported.hcursor.downField("report").focus
            value.hcursor.downField("gaps").focus shouldBe imported.hcursor.downField("gaps").focus
          }
          grouped.hcursor.downField("grouping").get[Vector[Key]]("unassigned") shouldBe
            Right(fixture.imported.inspection.findings.map(_.key).filterNot(fixture.keys.take(1).contains))
          admitted.hcursor.downField("decisions").downArray.get[Vector[Key]]("keys") shouldBe Right(selected)
          admitted.hcursor.downField("decisions").downArray.get[String]("principalId") shouldBe Right("developer")
          admitted.hcursor.downField("decisions").downArray.downField("direction").focus shouldBe Some(record("human-direction").asJson)
          admitted.hcursor.downField("decisions").downArray.downField("proposal").focus shouldBe Some(record("proposal").asJson)
          admitted.hcursor.downField("decisions").downArray.downField("imported").focus shouldBe Some(fixture.imported.reference.asJson)
        }
      }

      "retain a rejection and refuse import replacement without changing its decision" in {
        Given("a host-authorized proposal whose developer will reject it")
        val fixture = createFindingFixture()
        given ExecutionContext = context()
        executeFinding(fixture, "importFindingInspection", Selection(fixture.imported.reference).asJson)
        executeFinding(fixture, "proposeFindingResolution", proposal(fixture).asJson)
        When("the developer rejects it and a caller tries to re-import over retained work")
        val rejected = executeFinding(fixture, "decideFindingResolution", decision(fixture, Vector(fixture.keys.head)).copy(choice = Choice.Reject).asJson)
        val replacement = fixture.application.execute("importFindingInspection", Selection(fixture.imported.reference).asJson.noSpaces)
        val retained = executeFinding(fixture, "getFindingResolution", Selection(fixture.imported.reference).asJson)
        Then("rejection remains visible and cannot become permission through replacement")
        rejected.hcursor.downField("decisions").downArray.get[String]("choice") shouldBe Right("Reject")
        replacement.isSuccess shouldBe false
        retained.hcursor.downField("decisions").focus shouldBe rejected.hcursor.downField("decisions").focus
        retained.hcursor.get[Int]("actionableCount") shouldBe Right(3)
      }
    }

    "the existing principal boundary" which {
      "refuse missing, anonymous, unspecified, future, expired and wrongly selected principals before parsing" in {
        Given("an exact developer host binding and contexts that do not satisfy Phase12 authentication")
        val fixture = createFindingFixture()
        val base = context()
        val security = base.security
        val invalid = Vector(context("unknown"), context(""),
          ExecutionContext.withSecurityContext(base, security.copy(subjectKind = SubjectKind.Anonymous)),
          ExecutionContext.withSecurityContext(base, security.copy(subjectKind = SubjectKind.Unspecified)),
          ExecutionContext.withSecurityContext(base, security.copy(session = Some(SessionContext(authenticatedAt = Some(base.clock.instant().plusSeconds(60)))))),
          ExecutionContext.withSecurityContext(base, security.copy(session = Some(SessionContext(expiresAt = Some(base.clock.instant().minusSeconds(1)))))))
        When("these contexts call an ordinary application operation with malformed JSON")
        val results = invalid.map(value => fixture.application.execute("getFindingResolution", "not JSON")(using value))
        Then("the shared authentication and exact principal binding deny every request")
        results.foreach { result =>
          result.isSuccess shouldBe false
          result match {
            case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe org.goldenport.Conclusion.securityPermissionDenied("expected").observation.taxonomy
            case _ => fail("Expected structured permission denial")
          }
        }
      }
    }

    "closed client requests and exact host direction" which {
      "reject client-installed authority and inspection facts, duplicate fields and unknown nested variants" in {
        Given("a configured application whose import facts and developer direction exist only at the host")
        val fixture = createFindingFixture()
        given ExecutionContext = context()
        val selection = Selection(fixture.imported.reference).asJson
        val forged = Vector(selection.deepMerge(Json.obj("inspection" -> Json.obj())),
          selection.deepMerge(Json.obj("principalId" -> Json.fromString("developer"))),
          selection.deepMerge(Json.obj("direction" -> record("forged").asJson)),
          Json.obj("imported" -> fixture.imported.reference.asJson.deepMerge(Json.obj("grant" -> Json.fromBoolean(true)))))
        When("a client tries to install facts or authority and then alters proposal/decision record structure")
        val failures = forged.map(value => fixture.application.execute("importFindingInspection", value.noSpaces)) :+
          fixture.application.execute("importFindingInspection", "{\"imported\":" + fixture.imported.reference.asJson.noSpaces + ",\"imported\":" + fixture.imported.reference.asJson.noSpaces + "}")
        executeFinding(fixture, "importFindingInspection", selection)
        val badproposal = proposal(fixture).asJson.deepMerge(Json.obj("keys" -> Json.arr(
          fixture.keys.head.asJson.deepMerge(Json.obj("authority" -> Json.fromBoolean(true))))))
        val badkind = proposal(fixture).asJson.deepMerge(Json.obj("kind" -> Json.fromString("All")))
        executeFinding(fixture, "proposeFindingResolution", proposal(fixture).asJson)
        val badchoice = decision(fixture, Vector(fixture.keys.head)).asJson.deepMerge(Json.obj("choice" -> Json.fromString("Automatic")))
        val more = Vector(fixture.application.execute("proposeFindingResolution", badproposal.noSpaces),
          fixture.application.execute("proposeFindingResolution", badkind.noSpaces),
          fixture.application.execute("decideFindingResolution", badchoice.noSpaces),
          fixture.application.execute("decideFindingResolution", decision(fixture, Vector(fixture.keys.head)).asJson
            .deepMerge(Json.obj("authorization" -> Json.obj())).noSpaces))
        Then("closed records cannot install privileged fields or imply an enum alternative")
        (failures ++ more).foreach(_.isSuccess shouldBe false)
        executeFinding(fixture, "getFindingResolution", selection).hcursor.get[Vector[Json]]("decisions") shouldBe Right(Vector.empty)
      }

      "require exact import and proposal revisions, allowed kind and explicitly selected keys" in {
        Given("host direction for one proposal revision and only a subset of its occurrences")
        given ExecutionContext = context()
        val variations: Vector[Direction => Direction] = Vector(
          value => value.copy(imported = record("other-import")),
          value => value.copy(proposal = record("proposal", 2)),
          value => value.copy(kinds = Set(Kind.Fix)),
          value => value.copy(keys = Set(value.keys.head.copy(finding = "unrelated"))))
        When("otherwise valid requests disagree with independent host direction")
        val denied = variations.map { variation =>
          val fixture = createFindingFixture(transform = variation)
          executeFinding(fixture, "importFindingInspection", Selection(fixture.imported.reference).asJson)
          executeFinding(fixture, "proposeFindingResolution", proposal(fixture).asJson)
          fixture.application.execute("decideFindingResolution", decision(fixture, Vector(fixture.keys.head)).asJson.noSpaces)
        }
        val fixture = createFindingFixture()
        executeFinding(fixture, "importFindingInspection", Selection(fixture.imported.reference).asJson)
        executeFinding(fixture, "proposeFindingResolution", proposal(fixture).asJson)
        val invalid = Vector(decision(fixture, fixture.keys), decision(fixture, Vector.empty),
          decision(fixture, Vector(fixture.keys.head)).copy(proposal = record("proposal", 2)),
          decision(fixture, Vector(fixture.keys.head)).copy(imported = record("inspection", 2)))
          .map(value => fixture.application.execute("decideFindingResolution", value.asJson.noSpaces))
        val reader = fixture.application.execute("decideFindingResolution", decision(fixture, Vector(fixture.keys.head)).asJson.noSpaces)(using context("reader"))
        Then("neither group membership, proposal identity alone nor read access supplies developer admission")
        (denied ++ invalid :+ reader).foreach(_.isSuccess shouldBe false)
        executeFinding(fixture, "getFindingResolution", Selection(fixture.imported.reference).asJson).hcursor.get[Vector[Json]]("decisions") shouldBe Right(Vector.empty)
      }
    }
  }
}

private[cbdsupport] object FindingResolutionApplicationSpecFixture {
  final case class Fixture(imported: Import, keys: Vector[Key], application: FindingResolutionApplication)

  def record(identity: String, revision: Long = 1): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(identity).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)
  def context(principal: String = "developer"): ExecutionContext = ModelEditApplicationSpecFixture.principalContext(principal)

  def createFindingFixture(size: Int = 3, transform: Direction => Direction = value => value): Fixture = {
    val findings = (1 to size + 2).toVector.map(index => Finding(
      Key("car.host-access", f"host-$index%02d", Target("operation", f"orders:operation-$index%02d")), "Host access"))
    val keys = findings.take(size).map(_.key)
    val inspection = Inspection("orders-r1", findings, findings.map(value => Coverage(value.key.rule, value.key.target)).toSet)
    val imported = Import(record("inspection"), inspection,
      Vector(Declaration(findings(size).key, Kind.Accept, "Existing design", "decision:existing"),
        Declaration(findings(size + 1).key, Kind.Defer, "Migration follow-up", "ticket:existing")),
      Vector(CoverageGap(Coverage("car.unsupported", Target("component", "orders")), Availability.Unsupported, "Producer rule unavailable")))
    val direction = transform(Direction(record("human-direction"), imported.reference, record("proposal"), keys.dropRight(1).toSet, Set(Kind.Accept)))
    // A second import makes a wrong-import direction a valid host registration, but not authority for the selected request.
    val other = imported.copy(reference = record("other-import"))
    val imports = Set(imported.reference, other.reference)
    val application = FindingResolutionApplication.create(Vector(imported, other), Vector(
      Binding("developer", imports, Vector(direction)), Binding("reader", imports, Vector.empty))).toOption.get
    Fixture(imported, keys, application)
  }

  def modelEdit(): (ModelEditApplication, String) = {
    val fixture = ModelEditApplicationSpecFixture.create()
    (fixture.application, ModelEditOperationJson.selection(ModelEditApplicationSpecFixture.selected(fixture.current)))
  }
  def withCanonicalSource(body: java.nio.file.Path => Unit): Unit =
    InternalModelRetainedHistoryFixture.withCapture((root, _, _) => body(root))
  def sourceBytes(root: java.nio.file.Path): Map[String, Vector[Byte]] = InternalModelContinuationFixture.treeBytes(root)

  def proposal(fixture: Fixture): ProposalRequest = ProposalRequest(fixture.imported.reference, record("proposal"),
    fixture.keys, Kind.Accept, "Intentional host adapter", "analysis:adapter")
  def decision(fixture: Fixture, keys: Vector[Key]): DecisionRequest = DecisionRequest(fixture.imported.reference,
    record("proposal"), record("decision"), keys, Choice.Admit, "Approve selected occurrences")
  def executeFinding(fixture: Fixture, operation: String, document: Json)(using ctx: ExecutionContext): Json =
    io.circe.parser.parse(fixture.application.execute(operation, document.noSpaces).toOption.get.resultJson).toOption.get
}
