package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.finding.FindingDispositionV1 as Disposition
import Disposition.{Coverage, Declaration, Finding, Inspection, Key, Kind, Status, Target}
import FindingResolution.*
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.Gen

/*
 * @since   Oct. 31, 2026
 * @version Nov.  5, 2026
 */
final class FindingResolutionSpec extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckDrivenPropertyChecks {
  "Finding resolution preparation" should {
    "retain CNCF decisions, raw findings and incomplete check observations" in {
      Given("one unresolved, one accepted and one deferred finding plus an unsupported rule")
      val findings = _findings(3)
      val inspection = _inspection(findings)
      val declarations = Vector(Declaration(findings(1).key, Kind.Accept, "Intentional adapter access", "decision:adapter"),
        Declaration(findings(2).key, Kind.Defer, "Follow up after migration", "ticket:migration"))
      val gaps = Vector(CoverageGap(Coverage("car.unsupported", Target("component", "orders")),
        Availability.Unsupported, "Producer does not implement this rule"))
      When("the exact inspection and existing disposition records are imported")
      val current = FindingResolution.importC(_record("inspection"), inspection, declarations, gaps).toOption.get
      Then("the original evidence and CNCF statuses remain distinct from workflow proposals")
      current.inspection shouldBe inspection
      current.declarations shouldBe declarations
      current.gaps shouldBe gaps
      current.report.results.map(_.status).toSet shouldBe Set(Status.Unresolved, Status.Accepted, Status.Deferred)
      current.rawFindingCount shouldBe 3
      current.actionableCount shouldBe 1
      current.report.deferredBacklog.map(_.key) shouldBe Vector(findings(2).key)
      current.proposals shouldBe empty
      current.decisions shouldBe empty
    }

    "group arbitrary occurrence sets without losing identity or implying scope admission" in {
      forAll(Gen.choose(1, 40), Gen.oneOf(true, false)) { (size, reverse) =>
        Given("distinct semantic findings with explicit alternating remediation patterns")
        val findings = _findings(size)
        val current = _working(findings)
        val assignments = findings.zipWithIndex.map { case (finding, index) =>
          Remediation(finding.key, if (index % 2 == 0) "Use the host adapter" else "Remove direct access")
        }
        When("the caller groups these assignments in either order")
        val grouped = current.groupC(if (reverse) assignments.reverse else assignments).toOption.get
        Then("every exact key appears once and ordering grants neither admission nor resolution")
        grouped.groups.flatMap(_.keys).toSet shouldBe findings.map(_.key).toSet
        grouped.groups.flatMap(_.keys).size shouldBe size
        grouped shouldBe current.groupC(assignments).toOption.get
        grouped.unassigned shouldBe empty
        current.decisions shouldBe empty
        current.actionableCount shouldBe size
      }
    }

    "keep unassigned occurrences visible and reject ambiguous or unknown group assignments" in {
      Given("two findings and a partial remediation selection")
      val findings = _findings(2)
      val current = _working(findings)
      val assignment = Remediation(findings.head.key, "Host adapter access")
      When("partial, duplicate and unknown-key groupings are requested")
      val partial = current.groupC(Vector(assignment)).toOption.get
      val duplicate = current.groupC(Vector(assignment, assignment.copy(pattern = "Different meaning")))
      val unknown = current.groupC(Vector(assignment.copy(key = findings.head.key.copy(finding = "unknown"))))
      Then("only the explicit selection is grouped; the remaining key remains visible")
      partial.unassigned shouldBe Vector(findings(1).key)
      duplicate.isSuccess shouldBe false
      unknown.isSuccess shouldBe false
    }

    "offer all CNCF kinds as reasoned proposals without applying a disposition" in {
      forAll(Gen.oneOf(Kind.values.toSeq)) { kind =>
        Given("an unresolved finding and a reasoned external candidate")
        val findings = _findings(1)
        val current = _working(findings)
        When("the candidate becomes a proposal")
        val proposed = current.proposeC(_record("proposal"), findings.map(_.key), kind,
          "Keep the explicit source-owner boundary", "analysis:adapter").toOption.get
        Then("the kind, reason and exact import are retained without any admitted work")
        proposed.proposals.head.kind shouldBe kind
        proposed.proposals.head.imported shouldBe current.reference
        proposed.report shouldBe current.report
        proposed.declarations shouldBe empty
        proposed.decisions shouldBe empty
        proposed.admittedDeclarationsC(_record("proposal")).isSuccess shouldBe false
      }
    }

    "reject proposals that omit reasons or select other or already accepted findings" in {
      Given("an imported accepted finding and another unresolved occurrence")
      val findings = _findings(2)
      val current = FindingResolution.importC(_record("inspection"), _inspection(findings),
        Vector(Declaration(findings.head.key, Kind.Accept, "Existing design decision", "decision:existing")), Vector.empty).toOption.get
      When("a proposal omits its reason or claims an unavailable scope")
      val invalid = Vector(
        current.proposeC(_record("proposal"), Vector(findings(1).key), Kind.Fix, "", "analysis:1"),
        current.proposeC(_record("proposal"), Vector(findings.head.key), Kind.Fix, "Correct access", "analysis:1"),
        current.proposeC(_record("proposal"), Vector(findings(1).key.copy(finding = "unknown")), Kind.Fix, "Correct access", "analysis:1"))
      Then("none changes the working evidence or invents an applicable decision")
      invalid.foreach(_.isSuccess shouldBe false)
      current.actionableCount shouldBe 1
      current.proposals shouldBe empty
    }
  }

  "Explicit developer decisions" should {
    "admit only the explicitly selected subset of a bulk proposal" in {
      forAll(Gen.choose(2, 30)) { size =>
        Given("a bulk proposal and independently host-supplied authority for its exact scope")
        val findings = _findings(size)
        val keys = findings.map(_.key)
        val proposed = _working(findings).proposeC(_record("proposal"), keys, Kind.Accept,
          "Intentional host adapter access", "analysis:adapter").toOption.get
        val selected = keys.take(size / 2)
        val authority = _authority(proposed, keys.toSet, Kind.Accept)
        When("the developer explicitly admits part of that scope")
        val admitted = proposed.decideC(_record("decision"), _record("proposal"), selected, Choice.Admit,
          "Approve the selected adapter occurrences", authority).toOption.get
        Then("the decision and candidate declarations contain exactly those keys and actual host facts")
        val decision = admitted.decisions.head
        decision.keys.toSet shouldBe selected.toSet
        decision.principalId shouldBe authority.principalid
        decision.direction shouldBe authority.direction
        admitted.admittedDeclarationsC(decision.reference).toOption.get.map(_.key).toSet shouldBe selected.toSet
        admitted.report shouldBe proposed.report
        admitted.actionableCount shouldBe size
      }
    }

    "retain rejection without generating work or changing imported dispositions" in {
      Given("a reasoned Fix proposal and a genuine-host-direction fixture")
      val findings = _findings(1)
      val keys = findings.map(_.key)
      val proposed = _working(findings).proposeC(_record("proposal"), keys, Kind.Fix,
        "Replace direct access", "analysis:adapter").toOption.get
      When("the host-authorized developer rejects the proposal")
      val rejected = proposed.decideC(_record("rejection"), _record("proposal"), keys, Choice.Reject,
        "Retain the current adapter design", _authority(proposed, keys.toSet, Kind.Fix)).toOption.get
      Then("rejection retains its scope and rationale but supplies no admitted declarations")
      rejected.decisions.head.choice shouldBe Choice.Reject
      rejected.decisions.head.keys shouldBe keys
      rejected.admittedDeclarationsC(_record("rejection")).isSuccess shouldBe false
      rejected.report shouldBe proposed.report
    }

    "reject mismatched authority and implicit or broadened bulk selections" in {
      Given("two occurrences, a proposal, and a host binding limited to one occurrence")
      val findings = _findings(2)
      val keys = findings.map(_.key)
      val proposed = _working(findings).proposeC(_record("proposal"), keys, Kind.Defer,
        "Migrate in the next milestone", "analysis:migration").toOption.get
      val authority = _authority(proposed, Set(keys.head), Kind.Defer)
      When("the request expands scope, omits keys, changes proposal revision, import or allowed kind")
      val invalid = Vector(
        proposed.decideC(_record("decision"), _record("proposal"), keys, Choice.Admit, "Approve", authority),
        proposed.decideC(_record("decision"), _record("proposal"), Vector.empty, Choice.Admit, "Approve", authority),
        proposed.decideC(_record("decision"), _record("proposal"), Vector(keys.head), Choice.Admit, "Approve",
          authority.copy(proposal = _record("proposal", 2))),
        proposed.decideC(_record("decision"), _record("proposal"), Vector(keys.head), Choice.Admit, "Approve",
          authority.copy(imported = _record("other-inspection"))),
        proposed.decideC(_record("decision"), _record("proposal"), Vector(keys.head), Choice.Admit, "Approve",
          authority.copy(kinds = Set(Kind.Accept))))
      Then("no request can supply its own permission or admit an unselected occurrence")
      invalid.foreach(_.isSuccess shouldBe false)
      proposed.decisions shouldBe empty
    }
  }

  private def _record(identity: String, revision: Long = 1): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(identity).toOption.get,
      InternalModelRecordRevision.from(revision).toOption.get)

  private def _findings(size: Int): Vector[Finding] = (1 to size).toVector.map(index =>
    Finding(Key("car.host-access", s"host-access-$index", Target("operation", s"orders:operation-$index")), "Direct filesystem access"))

  private def _inspection(findings: Vector[Finding]): Inspection =
    Inspection("orders-model-r1", findings, findings.map(value => Coverage(value.key.rule, value.key.target)).toSet)

  private def _working(findings: Vector[Finding]): Working =
    FindingResolution.importC(_record("inspection"), _inspection(findings), Vector.empty, Vector.empty).toOption.get

  private def _authority(working: Working, keys: Set[Key], kind: Kind): DeveloperAuthority =
    DeveloperAuthority("developer:asami", _record("actual-host-direction"), working.reference, _record("proposal"), keys, Set(kind))
}
