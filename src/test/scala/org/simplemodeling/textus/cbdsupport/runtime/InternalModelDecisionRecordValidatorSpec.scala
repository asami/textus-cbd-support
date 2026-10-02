package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, LinkOption, Path, StandardOpenOption}

import scala.jdk.CollectionConverters.*

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/*
 * @since   Sep. 28, 2026
 * @version Oct.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelDecisionRecordValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _scope = InternalModelSemanticScope("component-order", "context-order", "e-usecase")
  private val _source = InternalModelSemanticSource(
    "model-authority", "model-source", Some("catalog/model-source"), Some("revision-1")
  )
  private val _provenance = InternalModelSemanticSource(
    "human-decision", "human-decision-source", Some("decisions/record"), Some("decision-revision-1")
  )
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _identity_gen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaLowerChar).map(chars => chars.mkString("id-", "", ""))
  private val _prose_gen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => s"ASCII-${chars.mkString}-日本語-𩸽")

  "Internal-model decision record validation" should {
    "retained decision accounts" which {
    "admit one complete current decision account and retain its complete selected realization" in {
      Given("a portable package with a selected V3 realization, human decision provenance, source evidence, external evidence, and provider evidence")
      val realization = _realization()
      val ledger = _ledger(realization)

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("the captured decision artifact is admitted through the package handoff")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the immutable admission retains the complete typed ledger, supplied prose order and duplicates, provenance, nullable source fields, and presentation-independent values")
        result.isSuccess shouldBe true
        result.toOption.map { admission =>
          admission.ledger
        } shouldBe Some(ledger)
        result.toOption.map(_.realization.conditions.map(_.detail)) shouldBe Some(Vector("source limitation", "source limitation secondary"))
      }
    }

    "preserve empty optional arrays, independent topics, later human selection, and same-basis supersession without using array position as authority" in {
      Given("two current topic chains whose retained prose and selected alternatives are explicit")
      val realization = _realization()
      val base = _ledger(realization)
      val first = base.records.head.copy(
        decisionReference = _record_reference("decision-topic-a-old", 41),
        topicIdentity = "topic-a",
        state = InternalModelDecisionState.Superseded,
        selectedChoice = InternalModelDecisionChoice("choice-a-old", "Earlier alternative"),
        rejectedAlternatives = Vector(InternalModelDecisionAlternative("choice-a-new", "A later explicit human selection", "The earlier decision retained this option as rejected")),
        assumptions = Vector.empty,
        conditions = Vector.empty,
        limitations = Vector.empty
      )
      val second = base.records.head.copy(
        decisionReference = _record_reference("decision-topic-a-current", 42),
        topicIdentity = "topic-a",
        selectedChoice = InternalModelDecisionChoice("choice-a-new", "A later explicit human selection"),
        rejectedAlternatives = Vector(
          InternalModelDecisionAlternative("choice-a-old", "Earlier alternative", "The later human decision changed the selected choice"),
          InternalModelDecisionAlternative("choice-secondary", "Use a separately sourced relationship", "The later human decision retained the selected source")
        ),
        supersedes = Some(_record_reference("decision-topic-a-old", 41))
      )
      val independent = base.records.head.copy(
        decisionReference = _record_reference("decision-topic-b-current", 41),
        topicIdentity = "topic-b",
        selectedChoice = InternalModelDecisionChoice("choice-b", "Independent topic choice")
      )
      val ledger = base.copy(records = Vector(second, independent, first))

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("the logical records are supplied in a noncanonical construction order")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("deterministic identity collection decoding retains explicit empty arrays and accepts each independent exact chain while the later human record selects its predecessor's rejected choice explicitly")
        result.isSuccess shouldBe true
        result.toOption.map(_.ledger.records.map(record => (record.decisionReference.recordId.value, record.assumptions, record.selectedChoice.choiceIdentity, record.rejectedAlternatives.map(_.alternativeIdentity)))) shouldBe Some(Vector(
          ("decision-topic-a-current", Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"), "choice-a-new", Vector("choice-a-old", "choice-secondary")),
          ("decision-topic-a-old", Vector.empty, "choice-a-old", Vector("choice-a-new")),
          ("decision-topic-b-current", Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"), "choice-b", Vector("choice-alternative", "choice-secondary"))
        ))
      }
    }

    "retain an unavailable old basis only as historical-unverified metadata" in {
      Given("a superseded predecessor with an old reference versions, unavailable target and source reference, and one exact-current accepted terminal")
      val realization = _realization()
      val base = _ledger(realization)
      val old = base.records.head.copy(
        decisionReference = _record_reference("decision-old", 41),
        topicIdentity = "topic-history",
        state = InternalModelDecisionState.Superseded,
        affectedTargets = Vector(InternalModelSemanticTarget("element", "e-removed")),
        consideredEvidence = Vector(InternalModelDecisionEvidence("e-old", InternalModelDecisionEvidenceKind.RealizationSource, _source, Some("ref-removed"), Vector("c-removed"), Vector("old condition"), Vector("old limitation"))),
        realizationConditionIds = Vector("c-removed"),
        basis = InternalModelDecisionBasis(_artifact_reference("realization-old", "realization", 11), _record_reference("realization-old", 21), _scope, InternalModelDecisionBasisStatus.HistoricalUnverified),
        supersedes = None
      )
      val current = base.records.head.copy(
        decisionReference = _record_reference("decision-current", 41),
        topicIdentity = "topic-history",
        supersedes = Some(_record_reference("decision-old", 41))
      )
      val ledger = base.copy(records = Vector(old, current))

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("only the current realization is captured in the package")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the current terminal is admitted while the old record is retained without historical lookup, retargeting, or current-basis promotion")
        result.isSuccess shouldBe true
        result.toOption.map(_.ledger.records.map(record => (record.decisionReference.recordId.value, record.basis.status, record.affectedTargets.head.semanticIdentity))) shouldBe Some(Vector(
          ("decision-current", InternalModelDecisionBasisStatus.Current, "e-customer"),
          ("decision-old", InternalModelDecisionBasisStatus.HistoricalUnverified, "e-removed")
        ))
      }
    }

    }

    "closed grammar and exact current admission" which {
    "fail closed for byte grammar, closed vocabulary, identity ordering, source-condition visibility, current basis, and explicit chain violations" in {
      Given("a complete canonical ledger with focused malformed and semantic mutations")
      val realization = _realization()
      val base = _ledger(realization)
      val canonical = InternalModelDecisionRecordCodec.encode(base)
      val badbytes = canonical ++ "not-json".getBytes(StandardCharsets.UTF_8)
      val unsortedtargets = new String(canonical, StandardCharsets.UTF_8).replace(
        "\"affectedTargets\":[{\"semanticIdentity\":\"e-customer\",\"semanticIdentityKind\":\"element\"},{\"semanticIdentity\":\"r-uses\",\"semanticIdentityKind\":\"relationship\"}]",
        "\"affectedTargets\":[{\"semanticIdentity\":\"r-uses\",\"semanticIdentityKind\":\"relationship\"},{\"semanticIdentity\":\"e-customer\",\"semanticIdentityKind\":\"element\"}]"
      ).getBytes(StandardCharsets.UTF_8)
      val codecinputs = Vector(
        InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decisions.json", true, Vector(_artifact_reference("realization-main", "realization", 13)), badbytes.toVector),
        InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decisions.json", true, Vector(_artifact_reference("realization-main", "realization", 13)), InternalModelDecisionRecordCodec.encode(base.copy(profile = "unsupported")).toVector),
        InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decisions.json", true, Vector(_artifact_reference("realization-main", "realization", 13)), unsortedtargets.toVector)
      )

      codecinputs.foreach { decision =>
        When("strict JSON, profile vocabulary, or an identity collection order is invalid")
        val result = InternalModelDecisionRecordCodec.decode(decision)

        Then("the codec rejects instead of normalizing or repairing the candidate")
        result.isLeft shouldBe true
      }

      val badcurrent = base.copy(records = Vector(base.records.head.copy(basis = base.records.head.basis.copy(realizationArtifactReference = _artifact_reference("realization-main", "realization", 14)))))
      val hiddencondition = base.copy(records = Vector(base.records.head.copy(realizationConditionIds = Vector.empty)))
      val hiddensourcecondition = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(conditionIds = Vector.empty) else evidence
      })))
      val changedsource = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(identity = "other-source")) else evidence
      })))
      val provideractor = base.copy(records = Vector(base.records.head.copy(actor = InternalModelDecisionActor("provider", "provider-1", "provider"))))
      val historicalterminal = base.copy(records = Vector(base.records.head.copy(basis = base.records.head.basis.copy(status = InternalModelDecisionBasisStatus.HistoricalUnverified))))
      val fork = base.copy(records = Vector(
        base.records.head.copy(decisionReference = _record_reference("decision-root", 41), topicIdentity = "topic-fork", state = InternalModelDecisionState.Superseded),
        base.records.head.copy(decisionReference = _record_reference("decision-left", 41), topicIdentity = "topic-fork", supersedes = Some(_record_reference("decision-root", 41))),
        base.records.head.copy(decisionReference = _record_reference("decision-right", 41), topicIdentity = "topic-fork", supersedes = Some(_record_reference("decision-root", 41)))
      ))
      val invalid = Vector(badcurrent, hiddencondition, changedsource, provideractor, historicalterminal, fork)

      invalid.foreach { ledger =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          Given("the selected V3 realization independently meets its source and semantic contract")
          _admit_realization(root)
          When("a current-basis, actor, source-condition, historical-terminal, or chain invariant is changed")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("admission fails closed without selecting a winner, repairing history, or promoting provider input")
          result.isSuccess shouldBe false
        }
      }

      val oldcurrent = base.records.head.copy(
        decisionReference = _record_reference("decision-old-current", 41),
        topicIdentity = "topic-old-current",
        state = InternalModelDecisionState.Superseded,
        basis = base.records.head.basis.copy(realizationReference = _record_reference("realization-order", 22), status = InternalModelDecisionBasisStatus.Current)
      )
      val oldcurrentterminal = base.records.head.copy(
        decisionReference = _record_reference("decision-old-current-terminal", 41),
        topicIdentity = "topic-old-current",
        supersedes = Some(_record_reference("decision-old-current", 41))
      )
      Vector(
        ("old predecessor is labeled current", base.copy(records = Vector(oldcurrent, oldcurrentterminal))),
        ("source condition IDs are hidden", hiddensourcecondition)
      ).foreach { case (_, ledger) =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          Given("the selected V3 realization independently meets its source and semantic contract")
          _admit_realization(root)
          When("a historical predecessor is labeled current or a realization-source condition is omitted")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("current admission fails without accepting an old basis or hiding the selected source limitation")
          result.isSuccess shouldBe false
        }
      }

      val sourcefieldmutations = Vector(
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(authority = "other-authority")) else evidence
        }))),
        changedsource,
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(locator = Some("other-locator"))) else evidence
        }))),
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(revision = Some("other-revision"))) else evidence
        })))
      )
      sourcefieldmutations.foreach { ledger =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("one realization-source authority, identity, locator, or revision field is changed")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("the exact admitted source object is required for current decision admission")
          result.isSuccess shouldBe false
        }
      }
    }

    }

    "portable inventory and captured admission" which {
    "keep structural package validity separate from decision readiness and use only captured handoff bytes" in {
      Given("a package whose optional decision is absent and a separately captured valid handoff")
      val realization = _realization()
      val decision = InternalModelDecisionRecordCodec.encode(_ledger(realization))

      _with_fixture(realization, None) { root =>
        When("the V2 structural inventory has no present optional decision")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val admission = InternalModelDecisionRecordValidator.validate(root)

        Then("structural validation succeeds while decision readiness fails")
        structural.isSuccess shouldBe true
        admission.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(decision)) { root =>
        Given("an already captured handoff before on-disk decision, realization, and manifest files are removed")
        val handoff = InternalModelPackageValidator.verifiedDecisionRecords(root)
        Files.delete(root.resolve("src/main/internal-model/decisions/decision.json"))
        Files.delete(root.resolve("src/main/internal-model/realizations/main.json"))
        Files.delete(root.resolve("src/main/internal-model/manifest.yaml"))

        When("the captured handoff and a fresh public validation are requested")
        val captured = handoff.flatMap(InternalModelDecisionRecordValidator.validateVerified)
        val fresh = InternalModelDecisionRecordValidator.validate(root)

        Then("the captured immutable authority still admits while the fresh package read fails")
        captured.isSuccess shouldBe true
        fresh.isSuccess shouldBe false
      }
    }

    "admit only the current realization profile at a safe nonconventional decision path" in {
      Given("a portable V3 realization, V2 ledger and safe manifest-relative decision filename")
      val realization = _realization()
      val ledger = _ledger(realization)
      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger)), decisionpath = "records/decision-ledger.json") { root =>
        When("the decision is admitted from the declared path")
        val result = InternalModelDecisionRecordValidator.validate(root)
        Then("the path convention has no authority over the current realization profile")
        result.toOption.map(_.realization.profile) shouldBe Some("ccdm-realization-v3")
      }

      Vector(("ccdm-realization-v1", "1.0"), ("ccdm-realization-v2", "2.0"), ("ccdm-realization-v3", "2.0")).foreach { case (profile, schema) =>
        Given("a selected realization with a legacy or mismatched explicit profile pair")
        val original = _json_parser.parse(new String(realization, StandardCharsets.UTF_8)).toOption.get.asObject.get
        val legacy = _canonical(Json.fromJsonObject(original.add("profile", Json.fromString(profile)).add("schemaVersion", Json.fromString(schema))))
        _with_fixture(legacy, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("current decision admission encounters the legacy selected realization")
          val result = InternalModelDecisionRecordValidator.validate(root)
          Then("the profile rejects without a compatibility decoder or inferred version")
          result.isSuccess shouldBe false
        }
      }
    }

    "reject the remaining closed grammar and explicit-admission failure matrix" in {
      Given("canonical typed candidates with independent blank, vocabulary, collision, external-link, and chain mutations")
      val realization = _realization()
      val base = _ledger(realization)
      val record = base.records.head
      val malformed = Vector(
        Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ InternalModelDecisionRecordCodec.encode(base),
        Array[Byte](0xc3.toByte, 0x28.toByte),
        "{\"records\":[],\"records\":[]}\n".getBytes(StandardCharsets.UTF_8)
      )
      malformed.foreach { bytes =>
        When("a BOM, invalid UTF-8 sequence, or duplicate object member is supplied")
        val result = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decision.json", true, Vector.empty, bytes.toVector))

        Then("the closed parser rejects it without parser fallback")
        result.isLeft shouldBe true
      }

      val invalidcodec = Vector(
        base.copy(ledgerReference = _record_reference_unchecked("", 31)),
        base.copy(records = Vector.empty),
        base.copy(records = Vector(record.copy(actor = record.actor.copy(identity = " ")))),
        base.copy(records = Vector(record.copy(actor = record.actor.copy(role = "")))),
        base.copy(records = Vector(record.copy(provenance = record.provenance.copy(identity = "")))),
        base.copy(records = Vector(record.copy(rationale = "\t"))),
        base.copy(records = Vector(record.copy(selectedChoice = record.selectedChoice.copy(description = "")))),
        base.copy(records = Vector(record.copy(affectedTargets = Vector.empty))),
        base.copy(records = Vector(record.copy(affectedTargets = record.affectedTargets :+ record.affectedTargets.head))),
        base.copy(records = Vector(record.copy(consideredEvidence = record.consideredEvidence :+ record.consideredEvidence.head))),
        base.copy(records = Vector(record.copy(rejectedAlternatives = record.rejectedAlternatives :+ record.rejectedAlternatives.head))),
        base.copy(records = Vector(record.copy(rejectedAlternatives = Vector(InternalModelDecisionAlternative("choice-primary", "colliding", "not distinct"))))),
        base.copy(records = Vector(record.copy(consideredEvidence = record.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.ExternalHuman then evidence.copy(sourceReferenceId = Some("ref-relationship"), conditionIds = Vector("c-limitation")) else evidence
        })))
      )
      invalidcodec.foreach { ledger =>
        When("a required scalar, state/status vocabulary, local identity uniqueness, choice collision, or external realization link is invalid")
        val bytes = InternalModelDecisionRecordCodec.encode(ledger)
        val result = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decision.json", true, Vector.empty, bytes.toVector))

        Then("the codec rejects the typed candidate rather than treating encoding as admission")
        result.isLeft shouldBe true
      }

      val rawunsupportedtokens = Vector(
        _canonical(_update_record(base, _.add("state", Json.fromString("implicit")))),
        _canonical(_update_record_child(base, "basis", _.add("status", Json.fromString("unrecognized")))),
        _canonical(_update_evidence_entry(base, 0, _.add("kind", Json.fromString("unrecognized-evidence"))))
      )
      rawunsupportedtokens.foreach { bytes =>
        When("a raw decision wire token falls outside one of the closed typed domains")
        val result = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decision.json", true, Vector.empty, bytes.toVector))

        Then("the decoder rejects the unsupported token without widening the retained enum domain")
        result.isLeft shouldBe true
      }

      val badartifact = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationArtifactReference = _artifact_reference("other-realization", "realization", 13)))))
      val badidentity = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationReference = _record_reference("other-identity", 23)))))
      val badscope = base.copy(records = Vector(record.copy(basis = record.basis.copy(scope = _scope.copy(componentIdentity = "other-component")))))
      val unknowntarget = base.copy(records = Vector(record.copy(affectedTargets = Vector(InternalModelSemanticTarget("element", "e-missing")))))
      val unknowncondition = base.copy(records = Vector(record.copy(realizationConditionIds = Vector("c-missing"))))
      val unknownsource = base.copy(records = Vector(record.copy(consideredEvidence = record.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(sourceReferenceId = Some("ref-missing"), conditionIds = Vector.empty) else evidence
      })))
      val supersededterminal = base.copy(records = Vector(record.copy(state = InternalModelDecisionState.Superseded)))
      val dangling = base.copy(records = Vector(record.copy(supersedes = Some(_record_reference("decision-missing", 41)))))
      val self = base.copy(records = Vector(record.copy(supersedes = Some(_record_reference("decision-current", 41)))))
      val acceptedwithsuccessor = base.copy(records = Vector(
        record.copy(decisionReference = _record_reference("decision-root", 41), topicIdentity = "topic-chain"),
        record.copy(decisionReference = _record_reference("decision-successor", 41), topicIdentity = "topic-chain", supersedes = Some(_record_reference("decision-root", 41)))
      ))
      val duplicatedterminal = base.copy(records = Vector(
        record.copy(decisionReference = _record_reference("decision-one", 41), topicIdentity = "topic-terminal"),
        record.copy(decisionReference = _record_reference("decision-two", 41), topicIdentity = "topic-terminal")
      ))
      val cycle = base.copy(records = Vector(
        record.copy(decisionReference = _record_reference("decision-cycle-a", 41), topicIdentity = "topic-cycle", state = InternalModelDecisionState.Superseded, supersedes = Some(_record_reference("decision-cycle-b", 41))),
        record.copy(decisionReference = _record_reference("decision-cycle-b", 41), topicIdentity = "topic-cycle", state = InternalModelDecisionState.Superseded, supersedes = Some(_record_reference("decision-cycle-a", 41))),
        record.copy(decisionReference = _record_reference("decision-cycle-terminal", 41), topicIdentity = "topic-cycle")
      ))
      val cross = base.copy(records = Vector(
        record.copy(decisionReference = _record_reference("decision-topic-a", 41), topicIdentity = "topic-a", state = InternalModelDecisionState.Superseded),
        record.copy(decisionReference = _record_reference("decision-topic-b", 41), topicIdentity = "topic-b", supersedes = Some(_record_reference("decision-topic-a", 41)))
      ))
      val ledgerscopemismatch = base.copy(scope = _scope.copy(componentIdentity = "other-component"))
      val badartifactrevision = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationArtifactReference = _artifact_reference("realization-main", "realization", 14)))))
      val badrecordrevision = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationReference = _record_reference("realization-order", 24)))))
      val predecessorversion = base.copy(records = Vector(
        record.copy(decisionReference = _record_reference("decision-prior", 41), state = InternalModelDecisionState.Superseded),
        record.copy(decisionReference = _record_reference("decision-successor", 42), supersedes = Some(_record_reference("decision-prior", 40)))
      ))
      val invalidadmission = Vector(badartifact, badartifactrevision, badidentity, badrecordrevision, predecessorversion, badscope, ledgerscopemismatch, unknowntarget, unknowncondition, unknownsource, supersededterminal, dangling, self, acceptedwithsuccessor, duplicatedterminal, cycle, cross)
      invalidadmission.foreach { ledger =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("an exact current basis, current resolution, or predecessor/successor chain requirement is violated")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("admission rejects it without retargeting, historical verification, or chain repair")
          result.isSuccess shouldBe false
        }
      }

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(base)), decisiondependencies = Vector("snapshot-model")) { root =>
        When("the selected decision lacks its direct dependency on the selected realization")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("structural V2 validity is preserved while decision admission rejects the missing direct handoff dependency")
        structural.isSuccess shouldBe true
        result.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(base)), additionaldecision = Some(InternalModelDecisionRecordCodec.encode(base))) { root =>
        When("two present decision artifacts are structurally listed")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the unchanged structural role multiplicity remains valid while decision readiness rejects multiple selections")
        structural.isSuccess shouldBe true
        result.isSuccess shouldBe false
      }
      _with_fixture(
        realization,
        Some(InternalModelDecisionRecordCodec.encode(base)),
        decisiondependencies = Vector("realization-main", "realization-other"),
        additionalrealization = Some(realization)
      ) { root =>
        When("two present realization artifacts are structurally listed")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the unchanged structural role multiplicity remains valid while decision readiness rejects multiple selected realizations")
        structural.isSuccess shouldBe true
        result.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(base))) { root =>
        Given("a package whose captured decision payload are replaced after fixture construction")
        Files.write(root.resolve("src/main/internal-model/decisions/decision.json"), "tampered\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)

        When("a fresh package-bound decision admission is requested")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the malformed changed payload fails strict decision parsing")
        result.isSuccess shouldBe false
      }
    }

    "reject every closed-schema mutation deterministically" in {
      Given("one canonical decision ledger and table-driven mutations at every closed object boundary")
      val base = _ledger(_realization())
      val decision = InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decision.json", true, Vector.empty, InternalModelDecisionRecordCodec.encode(base).toVector)

      _closed_mutations(base).foreach { case (_, bytes) =>
        When("a required or unknown root, record, actor, provenance, source, scope, choice, target, evidence, alternative, or basis field is mutated")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = bytes))

        Then("the closed grammar rejects the mutation")
        result.isLeft shouldBe true
      }

      val explicitnull = base.copy(records = Vector(base.records.head.copy(provenance = base.records.head.provenance.copy(locator = None, revision = None))))
      When("required nullable locator and revision keys are explicitly represented by null")
      val nullresult = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = InternalModelDecisionRecordCodec.encode(explicitnull).toVector))

      Then("the closed codec accepts explicit null while retaining the typed absence")
      nullresult.isRight shouldBe true

      val nullableomissions = Vector(
        _remove_record_field(base, "supersedes"),
        _remove_record_child_field(base, "provenance", "locator"),
        _remove_record_child_field(base, "provenance", "revision"),
        _remove_evidence_child_field(base, 0, "sourceReferenceId")
      )
      nullableomissions.foreach { bytes =>
        When("a required nullable key is omitted instead of being represented by explicit null")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = bytes.toVector))

        Then("the omission fails while explicit null remains part of the closed schema")
        result.isLeft shouldBe true
      }

      val unsupportedschema = base.copy(schemaVersion = "1.0")
      val unsupportedactor = base.copy(records = Vector(base.records.head.copy(actor = base.records.head.actor.copy(kind = "provider"))))
      val unsupportedtarget = base.copy(records = Vector(base.records.head.copy(affectedTargets = Vector(InternalModelSemanticTarget("other", "e-customer"), base.records.head.affectedTargets(1)))))
      val providerprovenance = base.copy(records = Vector(base.records.head.copy(provenance = base.records.head.provenance.copy(authority = "provider"))))
      val blankactoridentity = base.copy(records = Vector(base.records.head.copy(actor = base.records.head.actor.copy(identity = ""))))
      val blankactorrole = base.copy(records = Vector(base.records.head.copy(actor = base.records.head.actor.copy(role = ""))))
      val blankprovenanceidentity = base.copy(records = Vector(base.records.head.copy(provenance = base.records.head.provenance.copy(identity = ""))))
      val blankrationale = base.copy(records = Vector(base.records.head.copy(rationale = "\t")))
      val blankchoicedescription = base.copy(records = Vector(base.records.head.copy(selectedChoice = base.records.head.selectedChoice.copy(description = ""))))
      val blankalternativedescription = base.copy(records = Vector(base.records.head.copy(rejectedAlternatives = Vector(base.records.head.rejectedAlternatives.head.copy(description = ""), base.records.head.rejectedAlternatives(1)))))
      val blankalternativerationale = base.copy(records = Vector(base.records.head.copy(rejectedAlternatives = Vector(base.records.head.rejectedAlternatives.head.copy(rejectionRationale = ""), base.records.head.rejectedAlternatives(1)))))
      val blankevidenceidentity = base.copy(records = Vector(base.records.head.copy(consideredEvidence = Vector(base.records.head.consideredEvidence.head.copy(evidenceIdentity = ""), base.records.head.consideredEvidence(1), base.records.head.consideredEvidence(2)))))
      val blanktargetidentity = base.copy(records = Vector(base.records.head.copy(affectedTargets = Vector(base.records.head.affectedTargets.head.copy(semanticIdentity = ""), base.records.head.affectedTargets(1)))))
      val invalidledgerrevision = base.copy(ledgerReference = _record_reference_unchecked(base.ledgerReference.recordId.value, 0L))
      val invaliddecisionrevision = base.copy(records = Vector(base.records.head.copy(decisionReference = _record_reference_unchecked(base.records.head.decisionReference.recordId.value, 0L))))
      val duplicatedecision = base.copy(records = Vector(base.records.head, base.records.head.copy(decisionReference = _record_reference("decision-current", 42), topicIdentity = "topic-other")))
      val duplicatecondition = base.copy(records = Vector(base.records.head.copy(realizationConditionIds = Vector("c-limitation", "c-limitation"))))
      val duplicateevidencecondition = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(conditionIds = Vector("c-limitation", "c-limitation")) else evidence
      })))
      val canonicalmutations = Vector(
        "unsupported schemaVersion" -> unsupportedschema,
        "unsupported actor kind" -> unsupportedactor,
        "unsupported target kind" -> unsupportedtarget,
        "provider provenance authority" -> providerprovenance,
        "blank actor identity" -> blankactoridentity,
        "blank actor role" -> blankactorrole,
        "blank provenance identity" -> blankprovenanceidentity,
        "blank rationale" -> blankrationale,
        "blank choice description" -> blankchoicedescription,
        "blank alternative description" -> blankalternativedescription,
        "blank alternative rationale" -> blankalternativerationale,
        "blank evidence identity" -> blankevidenceidentity,
        "blank target identity" -> blanktargetidentity,
        "nonpositive ledger revision" -> invalidledgerrevision,
        "nonpositive decision revision" -> invaliddecisionrevision,
        "duplicate decision identity" -> duplicatedecision,
        "duplicate realization condition identity" -> duplicatecondition,
        "duplicate evidence condition identity" -> duplicateevidencecondition
      )
      canonicalmutations.foreach { case (_, ledger) =>
        When("a closed scalar, vocabulary, revision, or identity uniqueness invariant is mutated")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = InternalModelDecisionRecordCodec.encode(ledger).toVector))

        Then("the codec rejects the typed candidate rather than allowing encoding to become admission")
        result.isLeft shouldBe true
      }

      Given("a canonical decision ledger whose external evidence kind is changed to the invalid legacy provider wire token")
      val rawproviderevidence = _canonical(_update_evidence_entry(base, 0, _.add("kind", Json.fromString("provider"))))
      When("the raw canonical JSON decision bytes are decoded")
      val rawproviderresult = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = rawproviderevidence.toVector))

      Then("the invalid provider token is rejected without manufacturing a typed enum case")
      rawproviderresult.isLeft shouldBe true

      val rawbase = base.copy(records = Vector(
        base.records.head.copy(decisionReference = _record_reference("decision-a", 41), topicIdentity = "topic-a"),
        base.records.head.copy(decisionReference = _record_reference("decision-b", 41), topicIdentity = "topic-b")
      ))
      val rawcanonical = InternalModelDecisionRecordCodec.encode(rawbase)
      val rawmutations = Vector(
        ("malformed Unicode scalar escape", new String(rawcanonical, StandardCharsets.UTF_8).replace("\"rationale\":\"利用者の根拠と制約を保持する\"", "\"rationale\":\"\\ud800\"").getBytes(StandardCharsets.UTF_8)),
        ("trailing non-JSON data", rawcanonical ++ "not-json".getBytes(StandardCharsets.UTF_8))
      ) ++ _unsorted_raw_mutations(rawbase)
      rawmutations.foreach { case (_, bytes) =>
        When("malformed Unicode, trailing non-JSON data, or any identity array order is supplied")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = bytes.toVector))

        Then("the codec fails closed and never silently sorts the raw mutation")
        result.isLeft shouldBe true
      }
    }

    "separate structural compatibility from decision readiness across inventory and malformed payload mutations" in {
      Given("portable package fixtures with optional, missing, duplicate, dependent, and malformed decision artifacts")
      val realization = _realization()
      val decision = InternalModelDecisionRecordCodec.encode(_ledger(realization))

      _with_fixture(realization, None, includeabsentdecision = false) { root =>
        When("the package inventory has no decision artifact entry")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val admission = InternalModelDecisionRecordValidator.validate(root)

        Then("unchanged structural validation succeeds while decision readiness fails")
        structural.isSuccess shouldBe true
        admission.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(decision)) { root =>
        Files.write(root.resolve("src/main/internal-model/realizations/main.json"), "tampered\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)

        When("the selected realization payload is replaced by malformed JSON")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("fresh realization admission rejects the malformed captured payload")
        result.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(decision)) { root =>
        _write(root.resolve("src/main/internal-model/unlisted.json"), "unlisted\n".getBytes(StandardCharsets.UTF_8))

        When("an unlisted regular package artifact is present")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("fresh package admission rejects the unlisted inventory member")
        result.isSuccess shouldBe false
      }
    }

    "canonicalize logical identity collection permutations while preserving identity and Unicode prose" in {
      Given("generated independent topic and decision identities, scalar-safe ASCII/Japanese/supplementary prose, and permutations of every logical identity collection")
      val realization = _realization()
      val base = _ledger(realization)
      val propertyinput = for {
        token <- _identity_gen
        ledgerrevision <- Gen.chooseNum(1L, Long.MaxValue)
        decisionrevision <- Gen.chooseNum(1L, Long.MaxValue)
        artifactrevision <- Gen.chooseNum(1L, Long.MaxValue)
        realizationrevision <- Gen.chooseNum(1L, Long.MaxValue)
        rationale <- _prose_gen
        choicedescription <- _prose_gen
        alternativedescription <- _prose_gen
        rejectionrationale <- _prose_gen
        prose <- _prose_gen
        recordorder <- _permutation(Vector(0, 1))
        targetorder <- _permutation(Vector(0, 1))
        evidenceorder <- _permutation(Vector(0, 1, 2))
        alternativeorder <- _permutation(Vector(0, 1))
        conditionorder <- _permutation(Vector(0, 1))
        sourceconditionorder <- _permutation(Vector(0, 1))
      } yield (token, ledgerrevision, decisionrevision, artifactrevision, realizationrevision, rationale, choicedescription, alternativedescription, rejectionrationale, prose, recordorder, targetorder, evidenceorder, alternativeorder, conditionorder, sourceconditionorder)

      forAll(propertyinput) { case (token, ledgerrevision, decisionrevision, artifactrevision, realizationrevision, rationale, choicedescription, alternativedescription, rejectionrationale, prose, recordorder, targetorder, evidenceorder, alternativeorder, conditionorder, sourceconditionorder) =>
        val sourceevidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(conditionIds = sourceconditionorder.map(index => Vector("c-limitation", "c-source-secondary")(index))) else evidence
        }
        val logicalrecords = Vector(
          base.records.head.copy(
            decisionReference = _record_reference(s"decision-$token-a", decisionrevision),
            topicIdentity = s"topic-$token-a",
            rationale = rationale,
            selectedChoice = base.records.head.selectedChoice.copy(description = choicedescription),
            affectedTargets = targetorder.map(index => base.records.head.affectedTargets(index)),
            consideredEvidence = evidenceorder.map(index => sourceevidence(index)),
            assumptions = Vector(prose, rationale, prose),
            conditions = Vector(rationale, prose, rationale),
            limitations = Vector(prose, rationale, prose),
            realizationConditionIds = conditionorder.map(index => Vector("c-limitation", "c-source-secondary")(index)),
            rejectedAlternatives = alternativeorder.map { index =>
              val alternative = base.records.head.rejectedAlternatives(index)
              alternative.copy(description = alternativedescription, rejectionRationale = rejectionrationale)
            }
          ),
          base.records.head.copy(
            decisionReference = _record_reference(s"decision-$token-b", decisionrevision),
            topicIdentity = s"topic-$token-b",
            rationale = prose,
            selectedChoice = base.records.head.selectedChoice.copy(description = rationale)
          )
        )
        val versionedbase = base.copy(ledgerReference = _record_reference(s"ledger-$token", ledgerrevision))
        val versionedrecords = logicalrecords.map(record => record.copy(basis = record.basis.copy(realizationArtifactReference = _artifact_reference("realization-main", "realization", artifactrevision), realizationReference = _record_reference("realization-order", realizationrevision))))
        val permuted = recordorder.map(versionedrecords)
        val expectedrecords = versionedrecords.map { logicalrecord =>
          logicalrecord.copy(
            affectedTargets = base.records.head.affectedTargets,
            consideredEvidence = logicalrecord.consideredEvidence.sortBy(_.evidenceIdentity).map(evidence => evidence.copy(conditionIds = evidence.conditionIds.sorted)),
            rejectedAlternatives = logicalrecord.rejectedAlternatives.sortBy(_.alternativeIdentity),
            realizationConditionIds = logicalrecord.realizationConditionIds.sorted
          )
        }

        When("the pure codec encodes permuted records, targets, evidence, alternatives, condition IDs, and generated explanatory text")
        val encoded = InternalModelDecisionRecordCodec.encode(versionedbase.copy(records = permuted))
        val expected = versionedbase.copy(records = expectedrecords)
        val decoded = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decisions.json", true, Vector(_artifact_reference("realization-main", "realization", 13)), encoded.toVector))

        Then("decoded semantic values and complete retained identities are invariant while generated text remains verbatim and makes no winner inference")
        decoded.toOption shouldBe Some(expected)
        decoded.toOption.map(_.records.map(record => (record.decisionReference.recordId.value, record.topicIdentity))) shouldBe Some(Vector((s"decision-$token-a", s"topic-$token-a"), (s"decision-$token-b", s"topic-$token-b")))
        decoded.toOption.map(_.records.head).map { record =>
          (record.rationale, record.selectedChoice.choiceIdentity, record.selectedChoice.description, record.rejectedAlternatives.map(_.alternativeIdentity), record.rejectedAlternatives.map(_.description), record.assumptions, record.conditions, record.limitations)
        } shouldBe Some((rationale, "choice-primary", choicedescription, Vector("choice-alternative", "choice-secondary"), Vector(alternativedescription, alternativedescription), Vector(prose, rationale, prose), Vector(rationale, prose, rationale), Vector(prose, rationale, prose)))
      }
    }

    }

    "typed reference and capture contracts" which {
    "preserve semantic values across harmless JSON presentation changes" in {
      Given("a complete typed ledger with nullable sources, Unicode prose and explicit independent revisions")
      val base = _ledger(_realization())
      val decision = _captured_decision(InternalModelDecisionRecordCodec.encode(base))
      val json = _json(base)
      val presentations = Vector(
        Printer.spaces2.print(json),
        _reverse_objects(json).noSpaces,
        json.noSpaces.replace("利用者", "\\u5229\\u7528\\u8005"),
        " \n\t" + json.noSpaces + " \n\t"
      )
      presentations.foreach { text =>
        When("the same values are supplied with key, whitespace or equivalent escape variations")
        val decoded = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = text.getBytes(StandardCharsets.UTF_8).toVector))
        Then("admission and ordinary roundtrip retain exactly the semantic ledger")
        decoded.toOption shouldBe Some(base)
        decoded.flatMap(ledger => InternalModelDecisionRecordCodec.decode(decision.copy(bytes = InternalModelDecisionRecordCodec.encode(ledger).toVector))).toOption shouldBe Some(base)
      }

      Given("a nested duplicate member inside an otherwise valid actor")
      val duplicate = new String(decision.bytes.toArray, StandardCharsets.UTF_8).replace("\"kind\":\"human\"", "\"kind\":\"human\",\"kind\":\"human\"")
      When("the duplicate-member payload is decoded")
      val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = duplicate.getBytes(StandardCharsets.UTF_8).toVector))
      Then("presentation tolerance never admits duplicate members")
      result.isLeft shouldBe true
    }

    "reject raw escaped lone surrogates at every decision record reference depth" in {
      val paths = Vector(Vector("ledgerReference"), Vector("records", "decisionReference"), Vector("records", "supersedes"), Vector("records", "basis", "realizationReference"))
      val original = _ledger(_realization())
      val base = original.copy(records = Vector(original.records.head.copy(supersedes = Some(_record_reference("decision-prior", 40)))))
      paths.zipWithIndex.foreach { case (path, index) =>
        Vector("\\ud800", "\\udfff").foreach { escape =>
          Given(s"valid decision JSON with a unique placeholder at ${path.mkString(".")}.recordId")
          val placeholder = s"unicode-decision-placeholder-$index"
          val marked = _replace_at(_json(base), path :+ "recordId", Json.fromString(placeholder))
          val bytes = marked.noSpaces.replace("\"" + placeholder + "\"", "\"decision-" + escape + "\"").getBytes(StandardCharsets.UTF_8)
          When("the original ASCII JSON escape reaches the decision codec before any nested UTF-8 conversion")
          val result = InternalModelDecisionRecordCodec.decode(_captured_decision(bytes))
          Then("the lone high or low surrogate is rejected rather than replaced by a question mark")
          result.isLeft shouldBe true
        }
      }
    }

    "preserve generated paired supplementary escapes and revisions at every decision reference depth" in {
      val paths = Vector(Vector("ledgerReference"), Vector("records", "decisionReference"), Vector("records", "supersedes"), Vector("records", "basis", "realizationReference"))
      forAll(Gen.choose(0x10000, 0x10ffff), Gen.choose(1L, Long.MaxValue)) { (codepoint, revision) =>
        paths.zipWithIndex.foreach { case (path, index) =>
          Given(s"a generated supplementary scalar and independent positive revision at ${path.mkString(".")}")
          val original = _ledger(_realization())
          val base = original.copy(records = Vector(original.records.head.copy(supersedes = Some(_record_reference("decision-prior", 40)))))
          val scalar = new String(Character.toChars(codepoint))
          val escaped = Character.toChars(codepoint).map(character => f"\\u${character.toInt}%04x").mkString
          val recordid = "decision-" + scalar
          val placeholder = s"unicode-decision-pair-$index"
          val expectedjson = _replace_at(_replace_at(_json(base), path :+ "recordId", Json.fromString(recordid)), path :+ "recordRevision", Json.fromLong(revision))
          val marked = _replace_at(expectedjson, path :+ "recordId", Json.fromString(placeholder))
          val bytes = marked.noSpaces.replace("\"" + placeholder + "\"", "\"decision-" + escaped + "\"").getBytes(StandardCharsets.UTF_8)
          val expected = InternalModelDecisionRecordCodec.decode(_captured_decision(_canonical(expectedjson)))
          When("the paired JSON escape is decoded and emitted by the ordinary decision writer")
          val result = InternalModelDecisionRecordCodec.decode(_captured_decision(bytes))
          val roundtrip = result.flatMap(value => InternalModelDecisionRecordCodec.decode(_captured_decision(InternalModelDecisionRecordCodec.encode(value))))
          Then("the exact scalar identity, declared revision and complete ledger survive without substitution")
          result.isRight shouldBe true
          result shouldBe expected
          roundtrip shouldBe result
          val ledger = result.toOption.get
          val references = Vector(ledger.ledgerReference, ledger.records.head.decisionReference, ledger.records.head.supersedes.get, ledger.records.head.basis.realizationReference)
          references(index).recordId.value shouldBe recordid
          references(index).recordRevision.value shouldBe revision
        }
      }
    }

    "reject invalid lexical references and legacy fields at every decision reference depth" in {
      Given("a codec-valid ledger whose explicit predecessor supplies every reference-bearing depth")
      val original = _ledger(_realization())
      val base = original.copy(records = Vector(original.records.head.copy(supersedes = Some(_record_reference("decision-prior", 40)))))
      val decision = _captured_decision(InternalModelDecisionRecordCodec.encode(base))
      InternalModelDecisionRecordCodec.decode(decision).toOption shouldBe Some(base)
      val recordpaths = Vector(
        Vector("ledgerReference"),
        Vector("records", "decisionReference"),
        Vector("records", "supersedes"),
        Vector("records", "basis", "realizationReference")
      )
      val artifactpath = Vector("records", "basis", "realizationArtifactReference")
      val referencepaths = recordpaths.map(path => (path, "recordRevision")) :+ (artifactpath, "artifactRevision")
      val invalidtokens = Vector("0", "-1", "+1", "1.0", "1e0", "01", "9223372036854775808", "\"1\"", "null")
      referencepaths.foreach { case (path, revisionkey) =>
        invalidtokens.foreach { token =>
          Given(s"an invalid lexical positive Long at the explicit ${path.mkString(".")} boundary")
          val marked = _replace_at(_json(base), path :+ revisionkey, Json.fromString("invalid-revision"))
          val bytes = _canonical(marked)
          val content = new String(bytes, StandardCharsets.UTF_8).replace("\"invalid-revision\"", token)
          When("the revision token is decoded through the strict reference codec")
          val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = content.getBytes(StandardCharsets.UTF_8).toVector))
          Then("no sign, fraction, exponent, leading zero, string, null or overflow supplies a version")
          result.isLeft shouldBe true
        }
        val idkey = if revisionkey == "recordRevision" then "recordId" else "artifactId"
        val malformed = Vector(
          _change_object_at(_json(base), path, _.remove(revisionkey)),
          _change_object_at(_json(base), path, _.add("unknown", Json.fromString("extra"))),
          _change_object_at(_json(base), path, _.add("sha256", Json.fromString("obsolete"))),
          _replace_at(_json(base), path, Json.fromString("bare-reference")),
          _replace_at(_json(base), path :+ idkey, Json.fromString(" "))
        )
        malformed.foreach { json =>
          When("an exact reference field is missing, extra, hash-bearing, bare or blank")
          val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = _canonical(json).toVector))
          Then("the closed reference shape rejects without defaults or aliases")
          result.isLeft shouldBe true
        }
      }

      val legacy = Vector(
        _update_root(base, root => root.remove("ledgerReference").add("ledgerIdentity", Json.fromString("ledger-order"))),
        _update_record(base, record => record.remove("decisionReference").add("decisionIdentity", Json.fromString("decision-current"))),
        _update_record_child(base, "basis", basis => basis.remove("realizationArtifactReference").add("realizationArtifactId", Json.fromString("realization-main"))),
        _update_record_child(base, "basis", basis => basis.remove("realizationReference").add("realizationIdentity", Json.fromString("realization-order"))),
        _update_root(base, _.add("sha256", Json.fromString("obsolete"))),
        _update_record_child(base, "basis", _.add("sha256", Json.fromString("obsolete"))),
        _update_record_child(base, "provenance", _.add("sha256", Json.fromString("obsolete"))),
        _update_evidence_child(base, 2, "source", _.add("sha256", Json.fromString("obsolete"))),
        _replace_at(_json(base), artifactpath :+ "role", Json.fromString("projection"))
      )
      legacy.foreach { json =>
        When("legacy bare IDs, hash fields or a non-realization basis role are supplied")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = _canonical(json).toVector))
        Then("only the actual closed V2 decision grammar is admitted")
        result.isLeft shouldBe true
      }
      Vector(("ccdm-decision-records-v1", "1.0"), ("ccdm-decision-records-v1", "2.0"), ("ccdm-decision-records-v2", "1.0")).foreach { case (profile, schema) =>
        When("an old or mismatched decision profile and schema pair is supplied explicitly")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = InternalModelDecisionRecordCodec.encode(base.copy(profile = profile, schemaVersion = schema)).toVector))
        Then("there is no legacy reader or inferred-version fallback")
        result.isLeft shouldBe true
      }
    }

    "reject malformed captured decision metadata before byte content" in {
      Given("valid decision bytes and typed selected artifact metadata")
      val base = _captured_decision(InternalModelDecisionRecordCodec.encode(_ledger(_realization())))
      val reference = base.reference
      val invalidid = reference.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId])
      val invalidrevision = reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])
      val metadata = Vector(
        null,
        base.copy(reference = null),
        base.copy(reference = invalidid),
        base.copy(reference = invalidrevision),
        base.copy(reference = reference.copy(role = null)),
        base.copy(reference = reference.copy(role = InternalModelArtifactRole.Projection)),
        base.copy(path = null),
        base.copy(bytes = null),
        base.copy(dependencies = null),
        base.copy(dependencies = Vector(null)),
        base.copy(dependencies = Vector(base.dependencies.head, base.dependencies.head)),
        base.copy(dependencies = Vector(_artifact_reference("snapshot-model", "source-snapshot", 7), base.dependencies.head)),
        base.copy(dependencies = Vector(reference))
      )
      metadata.foreach { decision =>
        When("a null, invalid, wrong-role, duplicate, unordered or self-dependent capture is decoded")
        val result = InternalModelDecisionRecordCodec.decode(decision)
        Then("the codec returns a failure value before reading content")
        result.isLeft shouldBe true
      }
    }

    "reject malformed capture and exact dependency contradictions without partial admission" in {
      Given("one independently admitted current realization capture and a complete decision")
      val realization = _realization()
      val ledger = _ledger(realization)
      val base = _capture(realization, InternalModelDecisionRecordCodec.encode(ledger))
      InternalModelSemanticRealizationValidator.validateVerified(base.realizationpackage).isSuccess shouldBe true
      val selected = base.realizationpackage.realization
      val malformed = Vector(
        null,
        base.copy(decision = null),
        base.copy(realizationpackage = null),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = null)),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = null)),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(reference = null))),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(path = null))),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(dependencies = null))),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(reference = selected.reference.copy(role = InternalModelArtifactRole.Projection)))),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(reference = selected.reference.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])))),
        base.copy(realizationpackage = base.realizationpackage.copy(realization = selected.copy(bytes = null))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = Vector(null))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = base.realizationpackage.sourcesnapshots.map(_.copy(reference = null)))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = base.realizationpackage.sourcesnapshots.map(_.copy(path = null)))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = base.realizationpackage.sourcesnapshots.map(_.copy(dependencies = null)))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = base.realizationpackage.sourcesnapshots.map(_.copy(bytes = null)))),
        base.copy(realizationpackage = base.realizationpackage.copy(sourcesnapshots = base.realizationpackage.sourcesnapshots.map(_.copy(bytes = Some(null)))))
      )
      malformed.foreach { capture =>
        When("required captured realization or decision metadata is null")
        val result = InternalModelDecisionRecordValidator.validateVerified(capture)
        Then("operationInvalid is returned without a partial decision account")
        result.isSuccess shouldBe false
      }

      Vector(
        Vector.empty,
        Vector(_artifact_reference("realization-main", "realization", 14)),
        Vector(_artifact_reference("realization-main", "projection", 13)),
        Vector(_artifact_reference("realization-other", "realization", 13))
      ).foreach { dependencies =>
        Given("the same valid selected realization and a contradictory exact direct dependency")
        val capture = base.copy(decision = base.decision.copy(dependencies = dependencies))
        When("decision admission checks its selected realization reference")
        val result = InternalModelDecisionRecordValidator.validateVerified(capture)
        Then("ID, revision and role must all match without rebinding")
        result.isSuccess shouldBe false
      }
    }

    "admit explicitly allocated logical revisions independently of their carrier artifact" in {
      Given("one exact-current basis and separate producer-allocated ledger and decision revisions")
      val realization = _realization()
      val base = _ledger(realization)
      val versions = Vector(
        base,
        base.copy(ledgerReference = _record_reference("ledger-order", 32)),
        base.copy(records = Vector(base.records.head.copy(decisionReference = _record_reference("decision-current", 42))))
      )
      versions.foreach { ledger =>
        val capture = _capture(realization, InternalModelDecisionRecordCodec.encode(ledger))
        InternalModelSemanticRealizationValidator.validateVerified(capture.realizationpackage).isSuccess shouldBe true
        When("the declared logical versions are admitted against the same selected artifact and semantic basis")
        val result = InternalModelDecisionRecordValidator.validateVerified(capture)
        Then("the declared ledger and record references remain exact without carrier revision substitution")
        result.toOption.map(_.ledger) shouldBe Some(ledger)
        capture.decision.reference.artifactRevision.value shouldBe 19L
        capture.realizationpackage.realization.reference.artifactRevision.value shouldBe 13L
      }
    }

    "retain unknown source versions independently of known producer revisions" in {
      Given("known package, artifact, realization, ledger and decision versions and an explicitly unknown source revision")
      val original = _realization()
      val realization = _canonical(_replace_source_revisions(_json_parser.parse(new String(original, StandardCharsets.UTF_8)).toOption.get))
      val base = _ledger(realization)
      val ledger = base.copy(records = base.records.map(record => record.copy(consideredEvidence = record.consideredEvidence.map { evidence =>
        if evidence.kind == InternalModelDecisionEvidenceKind.RealizationSource then evidence.copy(source = evidence.source.copy(revision = None)) else evidence
      })))
      val originalcapture = _capture(realization, InternalModelDecisionRecordCodec.encode(ledger))
      val snapshot = _canonical(_replace_source_revisions(_json_parser.parse(new String(_source_snapshot(), StandardCharsets.UTF_8)).toOption.get))
      val capture = originalcapture.copy(realizationpackage = originalcapture.realizationpackage.copy(sourcesnapshots = originalcapture.realizationpackage.sourcesnapshots.map(_.copy(bytes = Some(snapshot.toVector)))))
      InternalModelSemanticRealizationValidator.validateVerified(capture.realizationpackage).isSuccess shouldBe true
      When("the decision account is admitted against matching unknown source metadata")
      val result = InternalModelDecisionRecordValidator.validateVerified(capture)
      Then("all independent declared versions remain known while the source version remains absent")
      result.toOption.map(_.ledger.ledgerReference.recordRevision.value) shouldBe Some(31L)
      result.toOption.map(_.ledger.records.head.decisionReference.recordRevision.value) shouldBe Some(41L)
      result.toOption.map(_.realization.realizationReference.recordRevision.value) shouldBe Some(23L)
      result.toOption.map(_.realization.sourceReferences.map(_.source.revision)) shouldBe Some(Vector(None, None, None))
      result.toOption.map(_.ledger.records.head.consideredEvidence.find(_.kind == InternalModelDecisionEvidenceKind.RealizationSource).get.source.revision) shouldBe Some(None)
    }
    }
  }

  private def _record_reference(identity: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(InternalModelRecordId.from(identity).toOption.get, InternalModelRecordRevision.from(revision).toOption.get)

  private def _record_reference_unchecked(identity: String, revision: Long): InternalModelRecordReference =
    InternalModelRecordReference(identity.asInstanceOf[InternalModelRecordId], revision.asInstanceOf[InternalModelRecordRevision])

  private def _artifact_reference(identity: String, rolevalue: String, revision: Long): InternalModelArtifactReference =
    InternalModelArtifactReference(InternalModelArtifactId.from(identity).toOption.get, InternalModelArtifactRevision.from(revision).toOption.get, InternalModelArtifactRole.fromWire(rolevalue).toOption.get)

  private def _fixture_artifact_revision(identity: String): Long =
    identity match {
      case "snapshot-model" => 7
      case "realization-main" => 13
      case "realization-other" => 11
      case "decision-main" => 19
      case "decision-other" => 17
    }

  private def _fixture_artifact_reference(identity: String): InternalModelArtifactReference =
    _artifact_reference(identity, if identity.startsWith("snapshot-") then "source-snapshot" else if identity.startsWith("realization-") then "realization" else "decision", _fixture_artifact_revision(identity))

  private def _record_reference_json(reference: InternalModelRecordReference): Json =
    Json.obj("recordId" -> Json.fromString(reference.recordId.value), "recordRevision" -> Json.fromLong(reference.recordRevision.value))

  private def _artifact_reference_json(reference: InternalModelArtifactReference): Json =
    Json.obj("artifactId" -> Json.fromString(reference.artifactId.value), "artifactRevision" -> Json.fromLong(reference.artifactRevision.value), "role" -> Json.fromString(reference.role.wireValue))


  private def _captured_decision(bytes: Array[Byte]): InternalModelVerifiedDecision =
    InternalModelVerifiedDecision(_artifact_reference("decision-main", "decision", 19), "decisions/decision.json", true, Vector(_artifact_reference("realization-main", "realization", 13)), bytes.toVector)

  private def _capture(realization: Array[Byte], decision: Array[Byte]): InternalModelVerifiedDecisionPackage =
    InternalModelVerifiedDecisionPackage(
      InternalModelVerifiedRealizationPackage(
        InternalModelVerifiedRealization(_artifact_reference("realization-main", "realization", 13), "realizations/main.json", true, Vector(_artifact_reference("snapshot-model", "source-snapshot", 7)), realization.toVector),
        Vector(InternalModelVerifiedSourceSnapshot(_artifact_reference("snapshot-model", "source-snapshot", 7), "snapshots/model.json", true, Vector.empty, Some(_source_snapshot().toVector)))
      ),
      _captured_decision(decision)
    )

  private def _replace_at(json: Json, path: Vector[String], replacement: Json): Json =
    if path.isEmpty then replacement
    else json.asArray match {
      case Some(values) => Json.fromValues(values.headOption.map(value => _replace_at(value, path, replacement)).toVector ++ values.drop(1))
      case None => json.asObject.map(objectvalue => Json.fromJsonObject(objectvalue.add(path.head, _replace_at(objectvalue(path.head).getOrElse(Json.Null), path.tail, replacement)))).getOrElse(json)
    }

  private def _change_object_at(json: Json, path: Vector[String], update: JsonObject => JsonObject): Json =
    if path.isEmpty then json.asObject.map(objectvalue => Json.fromJsonObject(update(objectvalue))).getOrElse(json)
    else json.asArray match {
      case Some(values) => Json.fromValues(values.headOption.map(value => _change_object_at(value, path, update)).toVector ++ values.drop(1))
      case None => json.asObject.map(objectvalue => Json.fromJsonObject(objectvalue.add(path.head, _change_object_at(objectvalue(path.head).getOrElse(Json.Null), path.tail, update)))).getOrElse(json)
    }

  private def _reverse_objects(json: Json): Json =
    json.asObject match {
      case Some(objectvalue) => Json.fromJsonObject(JsonObject.fromIterable(objectvalue.toVector.reverse.map { case (key, value) => key -> _reverse_objects(value) }))
      case None => json.asArray.map(values => Json.fromValues(values.map(_reverse_objects))).getOrElse(json)
    }

  private def _replace_source_revisions(json: Json): Json =
    json.asObject match {
      case Some(objectvalue) =>
        val fields = objectvalue.toVector.map { case (key, value) => key -> _replace_source_revisions(value) }
        val updated = JsonObject.fromIterable(fields)
        Json.fromJsonObject(if objectvalue.keys.toSet == Set("authority", "identity", "locator", "revision") then updated.add("revision", Json.Null) else updated)
      case None => json.asArray.map(values => Json.fromValues(values.map(_replace_source_revisions))).getOrElse(json)
    }

  private def _admit_realization(root: Path): Unit =
    InternalModelPackageValidator.verifiedPresentRealization(root).flatMap(InternalModelSemanticRealizationValidator.validateVerified).isSuccess shouldBe true

  private def _ledger(realization: Array[Byte]): InternalModelDecisionLedger = {
    val record = InternalModelDecisionRecord(
      _record_reference("decision-current", 41),
      "topic-current",
      InternalModelDecisionState.Accepted,
      InternalModelDecisionActor("human", "architect-1", "component architect"),
      _provenance,
      InternalModelDecisionChoice("choice-primary", "Keep the source-backed relationship"),
      "利用者の根拠と制約を保持する",
      Vector(InternalModelSemanticTarget("element", "e-customer"), InternalModelSemanticTarget("relationship", "r-uses")),
      Vector(
        InternalModelDecisionEvidence("e-external", InternalModelDecisionEvidenceKind.ExternalHuman, InternalModelSemanticSource("external-human", "external-1", None, None), None, Vector.empty, Vector("external condition"), Vector("external limitation")),
        InternalModelDecisionEvidence("e-provider", InternalModelDecisionEvidenceKind.ProviderProposal, InternalModelSemanticSource("provider", "provider-1", Some("provider/result"), None), None, Vector.empty, Vector.empty, Vector("proposal limitation")),
        InternalModelDecisionEvidence("e-realization", InternalModelDecisionEvidenceKind.RealizationSource, _source, Some("ref-relationship"), Vector("c-limitation", "c-source-secondary"), Vector("source condition", "source condition duplicate", "source condition duplicate"), Vector("source limitation", "source limitation duplicate"))
      ),
      Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"),
      Vector("decision condition", "decision condition second", "decision condition second"),
      Vector("decision limitation", "decision limitation second", "decision limitation second"),
      Vector("c-limitation", "c-source-secondary"),
      Vector(
        InternalModelDecisionAlternative("choice-alternative", "Use an unbacked relationship", "It lacks the exact selected source witness"),
        InternalModelDecisionAlternative("choice-secondary", "Use a separately sourced relationship", "It is not the selected source-backed choice")
      ),
      InternalModelDecisionBasis(_artifact_reference("realization-main", "realization", 13), _record_reference("realization-order", 23), _scope, InternalModelDecisionBasisStatus.Current),
      None
    )
    InternalModelDecisionLedger("ccdm-decision-records-v2", "2.0", _record_reference("ledger-order", 31), _scope, Vector(record))
  }

  private def _realization(): Array[Byte] = {
    val references = Vector(
      _reference("ref-customer", "element", "e-customer", "anchor-customer"),
      _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship"),
      _reference("ref-usecase", "element", "e-usecase", "anchor-usecase")
    )
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(Vector(
        _assertion("a-customer", "element", "e-customer", "ref-customer", "Customer is a party", Vector.empty),
        _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", Vector("c-limitation")),
        _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty)
      )),
      "conditions" -> Json.fromValues(Vector(
        _condition("c-limitation", "limitation", "relationship", "r-uses", "ref-relationship", "source limitation"),
        _condition("c-source-secondary", "limitation", "relationship", "r-uses", "ref-relationship", "source limitation secondary")
      )),
      "elements" -> Json.fromValues(Vector(
        _element("e-customer", "customer", "Customer", Vector("a-customer")),
        _element("e-usecase", "use-case", "Place order", Vector("a-usecase"))
      )),
      "enrichmentAssertions" -> Json.arr(),
      "profile" -> Json.fromString("ccdm-realization-v3"),
      "realizationReference" -> _record_reference_json(_record_reference("realization-order", 23)),
      "relationships" -> Json.arr(_relationship()),
      "schemaVersion" -> Json.fromString("3.0"),
      "scope" -> _scope_json,
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotReferences" -> Json.arr(_artifact_reference_json(_artifact_reference("snapshot-model", "source-snapshot", 7))))
    ))
  }

  private def _source_snapshot(): Array[Byte] =
    _canonical(Json.obj(
      "basis" -> Json.obj(
        "contextIdentity" -> Json.fromString("model-context"),
        "facts" -> Json.fromValues(Vector(
          _fact("element", "e-customer", "anchor-customer", "Customer is a party", Vector.empty),
          _fact("element", "e-usecase", "anchor-usecase", "Place an order", Vector.empty),
          _fact("relationship", "r-uses", "anchor-relationship", "Use case uses Customer", Vector("source limitation", "source limitation secondary"))
        ))
      ),
      "schemaVersion" -> Json.fromString("2.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source_json
    ))

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotReference" -> _artifact_reference_json(_artifact_reference("snapshot-model", "source-snapshot", 7)),
      "source" -> _source_json,
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue))
    )

  private def _assertion(assertionid: String, kindvalue: String, identity: String, referenceid: String, content: String, conditionids: Vector[String]): Json = {
    val fields = Vector(
      "association" -> Json.Null,
      "assertionId" -> Json.fromString(assertionid),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )
    Json.obj(fields*)
  }

  private def _condition(conditionid: String, kindvalue: String, affectedkind: String, affectedidentity: String, referenceid: String, detail: String): Json =
    Json.obj(
      "affectedIdentity" -> Json.fromString(affectedidentity),
      "affectedKind" -> Json.fromString(affectedkind),
      "conditionId" -> Json.fromString(conditionid),
      "detail" -> Json.fromString(detail),
      "kind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )

  private def _element(identity: String, kindvalue: String, label: String, canonicalids: Vector[String]): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(canonicalids.map(Json.fromString)),
      "conditionIds" -> Json.arr(),
      "enrichmentAssertionIds" -> Json.arr(),
      "identity" -> Json.fromString(identity),
      "kind" -> Json.fromString(kindvalue),
      "label" -> Json.fromString(label)
    )

  private def _relationship(): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.arr(Json.fromString("a-relationship")),
      "conditionIds" -> Json.fromValues(Vector("c-limitation", "c-source-secondary").map(Json.fromString)),
      "direction" -> Json.fromString("source-to-target"),
      "enrichmentAssertionIds" -> Json.arr(),
      "identity" -> Json.fromString("r-uses"),
      "label" -> Json.fromString("uses"),
      "role" -> Json.fromString("uses"),
      "sourceElementIdentity" -> Json.fromString("e-usecase"),
      "targetElementIdentity" -> Json.fromString("e-customer")
    )

  private def _fact(kindvalue: String, identity: String, anchor: String, content: String, limitations: Vector[String]): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString("component-order"),
      "content" -> Json.fromString(content),
      "limitations" -> Json.fromValues(limitations.map(Json.fromString)),
      "projectionContextIdentity" -> Json.fromString("context-order"),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceAnchor" -> Json.fromString(anchor)
    )

  private def _with_fixture(
    realization: Array[Byte],
    decision: Option[Array[Byte]],
    decisiondependencies: Vector[String] = Vector("realization-main"),
    decisionpath: String = "decisions/decision.json",
    additionaldecision: Option[Array[Byte]] = None,
    additionalrealization: Option[Array[Byte]] = None,
    includeabsentdecision: Boolean = true
  )(f: Path => Unit): Unit = {
    val snapshot = _source_snapshot()
    val snapshotartifact = _artifact("snapshot-model", "snapshots/model.json", "source-snapshot", true, snapshot, Vector.empty)
    val realizationartifact = _artifact("realization-main", "realizations/main.json", "realization", true, realization, Vector("snapshot-model"))
    val additionalrealizationartifact = additionalrealization.map(bytes => _artifact("realization-other", "realizations/other.json", "realization", true, bytes, Vector("snapshot-model"))).toVector
    val decisionartifact = decision.map(bytes => _artifact("decision-main", decisionpath, "decision", true, bytes, decisiondependencies)).toVector
    val additionaldecisionartifact = additionaldecision.map(bytes => _artifact("decision-other", "decisions/other.json", "decision", true, bytes, decisiondependencies)).toVector
    val absentdecision = if decision.isEmpty && includeabsentdecision then Vector(_artifact("decision-main", decisionpath, "decision", false, "unused\n".getBytes(StandardCharsets.UTF_8), decisiondependencies)) else Vector.empty
    val workroot = Path.of("target/internal-model-decision-record/work")
    Files.createDirectories(workroot)
    val root = Files.createTempDirectory(workroot, "fixture-")
    try {
      _write(root.resolve("project.yaml"), "project:\n  namespace: org.example\n  id: decision-sample\n".getBytes(StandardCharsets.UTF_8))
      _write(root.resolve("src/main/internal-model/manifest.yaml"), _manifest(Vector(snapshotartifact, realizationartifact) ++ additionalrealizationartifact ++ decisionartifact ++ additionaldecisionartifact ++ absentdecision))
      _write(root.resolve("src/main/internal-model/snapshots/model.json"), snapshot)
      _write(root.resolve("src/main/internal-model/realizations/main.json"), realization)
      additionalrealization.foreach(bytes => _write(root.resolve("src/main/internal-model/realizations/other.json"), bytes))
      decision.foreach(bytes => _write(root.resolve("src/main/internal-model").resolve(decisionpath), bytes))
      additionaldecision.foreach(bytes => _write(root.resolve("src/main/internal-model/decisions/other.json"), bytes))
      f(root)
    } finally _delete_tree(root)
  }

  private def _permutation[T](values: Vector[T]): Gen[Vector[T]] = {
    def _loop_(remaining: Vector[T], collected: Vector[T]): Gen[Vector[T]] =
      if remaining.isEmpty then Gen.const(collected)
      else Gen.choose(0, remaining.size - 1).flatMap { index =>
        _loop_(remaining.patch(index, Vector.empty, 1), collected :+ remaining(index))
      }
    _loop_(values, Vector.empty)
  }

  private def _json(ledger: InternalModelDecisionLedger): Json =
    _json_parser.parse(new String(InternalModelDecisionRecordCodec.encode(ledger), StandardCharsets.UTF_8)).toOption.getOrElse(Json.Null)

  private def _update_root(ledger: InternalModelDecisionLedger, update: JsonObject => JsonObject): Json =
    _json(ledger).asObject.map(root => Json.fromJsonObject(update(root))).getOrElse(Json.Null)

  private def _update_record(ledger: InternalModelDecisionLedger, update: JsonObject => JsonObject): Json = {
    val root = _json(ledger).asObject.getOrElse(JsonObject.empty)
    val records = root("records").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
    records.headOption.flatMap(_.asObject) match {
      case Some(record) => Json.fromJsonObject(root.add("records", Json.fromValues(Json.fromJsonObject(update(record)) +: records.drop(1))))
      case None => Json.fromJsonObject(root)
    }
  }

  private def _update_record_child(ledger: InternalModelDecisionLedger, key: String, update: JsonObject => JsonObject): Json =
    _update_record(ledger, record => record(key).flatMap(_.asObject).map(child => record.add(key, Json.fromJsonObject(update(child)))).getOrElse(record))

  private def _update_evidence_entry(ledger: InternalModelDecisionLedger, index: Int, update: JsonObject => JsonObject): Json =
    _update_record(ledger, record => {
      val entries = record("consideredEvidence").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
      val updated = entries.zipWithIndex.map { case (entry, entryindex) =>
        if entryindex == index then entry.asObject.map(objectvalue => Json.fromJsonObject(update(objectvalue))).getOrElse(entry) else entry
      }
      record.add("consideredEvidence", Json.fromValues(updated))
    })

  private def _update_evidence_child(ledger: InternalModelDecisionLedger, index: Int, key: String, update: JsonObject => JsonObject): Json =
    _update_evidence_entry(ledger, index, entry => entry(key).flatMap(_.asObject).map(child => entry.add(key, Json.fromJsonObject(update(child)))).getOrElse(entry))

  private def _remove_record_field(ledger: InternalModelDecisionLedger, key: String): Array[Byte] =
    _canonical(_update_record(ledger, _.remove(key)))

  private def _remove_record_child_field(ledger: InternalModelDecisionLedger, key: String, childkey: String): Array[Byte] =
    _canonical(_update_record_child(ledger, key, _.remove(childkey)))

  private def _remove_evidence_child_field(ledger: InternalModelDecisionLedger, index: Int, key: String): Array[Byte] =
    _canonical(_update_evidence_entry(ledger, index, _.remove(key)))

  private def _closed_mutations(ledger: InternalModelDecisionLedger): Vector[(String, Vector[Byte])] = {
    val rootmissing = _canonical(_update_root(ledger, _.remove("records"))).toVector
    val rootunknown = _canonical(_update_root(ledger, _.add("unknown", Json.fromString("extra")))).toVector
    val recordmissing = _canonical(_update_record(ledger, _.remove("rationale"))).toVector
    val recordunknown = _canonical(_update_record(ledger, _.add("unknown", Json.fromString("extra")))).toVector
    val actormissing = _canonical(_update_record_child(ledger, "actor", _.remove("identity"))).toVector
    val actorunknown = _canonical(_update_record_child(ledger, "actor", _.add("unknown", Json.fromString("extra")))).toVector
    val provenancemissing = _canonical(_update_record_child(ledger, "provenance", _.remove("authority"))).toVector
    val provenanceunknown = _canonical(_update_record_child(ledger, "provenance", _.add("unknown", Json.fromString("extra")))).toVector
    val sourcemissing = _canonical(_update_record_child(ledger, "provenance", _.remove("revision"))).toVector
    val sourceunknown = _canonical(_update_record_child(ledger, "provenance", _.add("unknown", Json.fromString("extra")))).toVector
    val scopemissing = _canonical(_update_root(ledger, root => root("scope").flatMap(_.asObject).map(scope => root.add("scope", Json.fromJsonObject(scope.remove("componentIdentity")))).getOrElse(root))).toVector
    val scopeunknown = _canonical(_update_root(ledger, root => root("scope").flatMap(_.asObject).map(scope => root.add("scope", Json.fromJsonObject(scope.add("unknown", Json.fromString("extra"))))).getOrElse(root))).toVector
    val choicemissing = _canonical(_update_record_child(ledger, "selectedChoice", _.remove("description"))).toVector
    val choiceunknown = _canonical(_update_record_child(ledger, "selectedChoice", _.add("unknown", Json.fromString("extra")))).toVector
    val targetmissing = _canonical(_update_record(ledger, record => {
      val targets = record("affectedTargets").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
      record.add("affectedTargets", Json.fromValues(targets.headOption.map(target => target.asObject.map(_.remove("semanticIdentity")).map(Json.fromJsonObject).getOrElse(target)).toVector ++ targets.drop(1)))
    })).toVector
    val targetunknown = _canonical(_update_record(ledger, record => {
      val targets = record("affectedTargets").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
      record.add("affectedTargets", Json.fromValues(targets.headOption.map(target => target.asObject.map(_.add("unknown", Json.fromString("extra"))).map(Json.fromJsonObject).getOrElse(target)).toVector ++ targets.drop(1)))
    })).toVector
    val evidencemissing = _canonical(_update_evidence_entry(ledger, 0, _.remove("kind"))).toVector
    val evidenceunknown = _canonical(_update_evidence_entry(ledger, 0, _.add("unknown", Json.fromString("extra")))).toVector
    val sourcemissingnested = _canonical(_update_evidence_child(ledger, 2, "source", _.remove("identity"))).toVector
    val sourceunknownnested = _canonical(_update_evidence_child(ledger, 2, "source", _.add("unknown", Json.fromString("extra")))).toVector
    val alternativemissing = _canonical(_update_record(ledger, record => {
      val alternatives = record("rejectedAlternatives").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
      record.add("rejectedAlternatives", Json.fromValues(alternatives.headOption.map(alternative => alternative.asObject.map(_.remove("description")).map(Json.fromJsonObject).getOrElse(alternative)).toVector ++ alternatives.drop(1)))
    })).toVector
    val alternativeunknown = _canonical(_update_record(ledger, record => {
      val alternatives = record("rejectedAlternatives").flatMap(_.asArray).map(_.toVector).getOrElse(Vector.empty)
      record.add("rejectedAlternatives", Json.fromValues(alternatives.headOption.map(alternative => alternative.asObject.map(_.add("unknown", Json.fromString("extra"))).map(Json.fromJsonObject).getOrElse(alternative)).toVector ++ alternatives.drop(1)))
    })).toVector
    val basismissing = _canonical(_update_record_child(ledger, "basis", _.remove("status"))).toVector
    val basisunknown = _canonical(_update_record_child(ledger, "basis", _.add("unknown", Json.fromString("extra")))).toVector
    Vector(
      "root missing records" -> rootmissing,
      "root unknown field" -> rootunknown,
      "record missing rationale" -> recordmissing,
      "record unknown field" -> recordunknown,
      "actor missing identity" -> actormissing,
      "actor unknown field" -> actorunknown,
      "provenance missing authority" -> provenancemissing,
      "provenance unknown field" -> provenanceunknown,
      "source missing revision" -> sourcemissing,
      "source unknown field" -> sourceunknown,
      "scope missing identity" -> scopemissing,
      "scope unknown field" -> scopeunknown,
      "selected choice missing description" -> choicemissing,
      "selected choice unknown field" -> choiceunknown,
      "target missing identity" -> targetmissing,
      "target unknown field" -> targetunknown,
      "evidence missing kind" -> evidencemissing,
      "evidence unknown field" -> evidenceunknown,
      "evidence source missing identity" -> sourcemissingnested,
      "evidence source unknown field" -> sourceunknownnested,
      "alternative missing description" -> alternativemissing,
      "alternative unknown field" -> alternativeunknown,
      "basis missing status" -> basismissing,
      "basis unknown field" -> basisunknown
    )
  }

  private def _unsorted_raw_mutations(ledger: InternalModelDecisionLedger): Vector[(String, Array[Byte])] = {
    def _reverse_record_array_(key: String): Array[Byte] = _canonical(_update_record(ledger, record => record(key).flatMap(_.asArray).map(values => record.add(key, Json.fromValues(values.reverse))).getOrElse(record)))
    def _reverse_evidence_conditions_ : Array[Byte] = _canonical(_update_evidence_entry(ledger, 2, entry => entry("conditionIds").flatMap(_.asArray).map(values => entry.add("conditionIds", Json.fromValues(values.reverse))).getOrElse(entry)))
    val reversedrecords = _canonical(_update_root(ledger, root => root("records").flatMap(_.asArray).map(values => root.add("records", Json.fromValues(values.reverse))).getOrElse(root)))
    Vector(
      "unsorted records" -> reversedrecords,
      "unsorted affected targets" -> _reverse_record_array_("affectedTargets"),
      "unsorted evidence" -> _reverse_record_array_("consideredEvidence"),
      "unsorted alternatives" -> _reverse_record_array_("rejectedAlternatives"),
      "unsorted realization condition IDs" -> _canonical(_update_record(ledger, record => record("realizationConditionIds").flatMap(_.asArray).map(values => record.add("realizationConditionIds", Json.fromValues(values.reverse))).getOrElse(record))),
      "unsorted source condition IDs" -> _reverse_evidence_conditions_
    )
  }

  private def _artifact(id: String, path: String, rolevalue: String, required: Boolean, bytes: Array[Byte], dependencies: Vector[String]): Json =
    Json.obj(
      "artifactId" -> Json.fromString(id),
      "artifactRevision" -> Json.fromLong(_fixture_artifact_revision(id)),
      "dependsOn" -> Json.fromValues(dependencies.sorted.map(identity => _artifact_reference_json(_fixture_artifact_reference(identity)))),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(required),
      "role" -> Json.fromString(rolevalue)
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    def _order_(remaining: Vector[Json], collected: Vector[Json]): Vector[Json] =
      if remaining.isEmpty then collected else {
        val ready = remaining.filter(artifact => artifact.hcursor.downField("dependsOn").focus.flatMap(_.asArray).getOrElse(Vector.empty).forall(reference => collected.exists(_.hcursor.get[String]("artifactId").toOption == reference.hcursor.get[String]("artifactId").toOption))).sortBy(_.hcursor.get[String]("artifactId").toOption.getOrElse("")).head
        _order_(remaining.filterNot(_ == ready), collected :+ ready)
      }
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(_order_(artifacts, Vector.empty)),
      "lifecycleState" -> Json.fromString("draft"),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("decision-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromLong(5),
      "schemaVersion" -> Json.fromString("2.0")
    ))
    _canonical(root.toJson)
  }

  private def _scope_json: Json =
    Json.obj(
      "componentIdentity" -> Json.fromString(_scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(_scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(_scope.selectedUseCaseElementIdentity)
    )

  private def _source_json: Json =
    Json.obj(
      "authority" -> Json.fromString(_source.authority),
      "identity" -> Json.fromString(_source.identity),
      "locator" -> _source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> _source.revision.map(Json.fromString).getOrElse(Json.Null)
    )

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)


  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root, LinkOption.NOFOLLOW_LINKS) then {
      val stream = Files.walk(root)
      try stream.iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      finally stream.close()
    }
}
