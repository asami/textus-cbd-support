package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest

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
 * @version Sep. 28, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelDecisionRecordValidatorSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen
    with ScalaCheckPropertyChecks {

  private val _printer = Printer.noSpacesSortKeys
  private val _model_raw = "model source bytes\n".getBytes(StandardCharsets.UTF_8)
  private val _scope = InternalModelSemanticScope("component-order", "context-order", "e-usecase")
  private val _source = InternalModelSemanticSource(
    "model-authority", "model-source", Some("catalog/model-source"), Some("revision-1"), _sha256(_model_raw)
  )
  private val _provenance = InternalModelSemanticSource(
    "human-decision", "human-decision-source", Some("decisions/record"), Some("decision-revision-1"), "sha256:" + ("1" * 64)
  )
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _identity_gen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaLowerChar).map(chars => chars.mkString("id-", "", ""))
  private val _prose_gen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => s"ASCII-${chars.mkString}-日本語-𩸽")

  "Internal-model decision record validation" should {
    "admit one complete current decision account and retain its complete selected realization" in {
      Given("a portable package with a selected V1 realization, human decision provenance, source evidence, external evidence, and provider evidence")
      val realization = _realization()
      val ledger = _ledger(realization)

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("the captured decision artifact is admitted through the package handoff")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the immutable admission retains the complete typed ledger, supplied prose order and duplicates, provenance, nullable source fields, and byte-exact encoding")
        result.isSuccess shouldBe true
        result.toOption.map { admission =>
          admission.ledger
        } shouldBe Some(ledger.copy(canonicalBytes = InternalModelDecisionRecordCodec.encode(ledger).toVector))
        result.toOption.map(_.realization.conditions.map(_.detail)) shouldBe Some(Vector("source limitation", "source limitation secondary"))
      }
    }

    "preserve empty optional arrays, independent topics, later human selection, and same-basis supersession without using array position as authority" in {
      Given("two current topic chains whose retained prose and selected alternatives are explicit")
      val realization = _realization()
      val base = _ledger(realization)
      val first = base.records.head.copy(
        decisionIdentity = "decision-topic-a-old",
        topicIdentity = "topic-a",
        state = "superseded",
        selectedChoice = InternalModelDecisionChoice("choice-a-old", "Earlier alternative"),
        rejectedAlternatives = Vector(InternalModelDecisionAlternative("choice-a-new", "A later explicit human selection", "The earlier decision retained this option as rejected")),
        assumptions = Vector.empty,
        conditions = Vector.empty,
        limitations = Vector.empty
      )
      val second = base.records.head.copy(
        decisionIdentity = "decision-topic-a-current",
        topicIdentity = "topic-a",
        selectedChoice = InternalModelDecisionChoice("choice-a-new", "A later explicit human selection"),
        rejectedAlternatives = Vector(
          InternalModelDecisionAlternative("choice-a-old", "Earlier alternative", "The later human decision changed the selected choice"),
          InternalModelDecisionAlternative("choice-secondary", "Use a separately sourced relationship", "The later human decision retained the selected source")
        ),
        supersedes = Some("decision-topic-a-old")
      )
      val independent = base.records.head.copy(
        decisionIdentity = "decision-topic-b-current",
        topicIdentity = "topic-b",
        selectedChoice = InternalModelDecisionChoice("choice-b", "Independent topic choice")
      )
      val ledger = base.copy(records = Vector(second, independent, first))

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("the logical records are supplied in a noncanonical construction order")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("canonical decoding retains explicit empty arrays and accepts each independent exact chain while the later human record selects its predecessor's rejected choice explicitly")
        result.isSuccess shouldBe true
        result.toOption.map(_.ledger.records.map(record => (record.decisionIdentity, record.assumptions, record.selectedChoice.choiceIdentity, record.rejectedAlternatives.map(_.alternativeIdentity)))) shouldBe Some(Vector(
          ("decision-topic-a-current", Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"), "choice-a-new", Vector("choice-a-old", "choice-secondary")),
          ("decision-topic-a-old", Vector.empty, "choice-a-old", Vector("choice-a-new")),
          ("decision-topic-b-current", Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"), "choice-b", Vector("choice-alternative", "choice-secondary"))
        ))
      }
    }

    "retain an unavailable old basis only as historical-unverified metadata" in {
      Given("a superseded predecessor with an old hash, unavailable target and source reference, and one exact-current accepted terminal")
      val realization = _realization()
      val base = _ledger(realization)
      val old = base.records.head.copy(
        decisionIdentity = "decision-old",
        topicIdentity = "topic-history",
        state = "superseded",
        affectedTargets = Vector(InternalModelSemanticTarget("element", "e-removed")),
        consideredEvidence = Vector(InternalModelDecisionEvidence("e-old", "realization-source", _source, Some("ref-removed"), Vector("c-removed"), Vector("old condition"), Vector("old limitation"))),
        realizationConditionIds = Vector("c-removed"),
        basis = InternalModelDecisionBasis("realization-old", "realization-old", "sha256:" + ("2" * 64), _scope, "historical-unverified"),
        supersedes = None
      )
      val current = base.records.head.copy(
        decisionIdentity = "decision-current",
        topicIdentity = "topic-history",
        supersedes = Some("decision-old")
      )
      val ledger = base.copy(records = Vector(old, current))

      _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
        When("only the current realization is captured in the package")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the current terminal is admitted while the old record is retained without historical lookup, retargeting, or current-basis promotion")
        result.isSuccess shouldBe true
        result.toOption.map(_.ledger.records.map(record => (record.decisionIdentity, record.basis.status, record.affectedTargets.head.semanticIdentity))) shouldBe Some(Vector(
          ("decision-current", "current", "e-customer"),
          ("decision-old", "historical-unverified", "e-removed")
        ))
      }
    }

    "fail closed for byte grammar, closed vocabulary, identity ordering, source-condition visibility, current basis, and explicit chain violations" in {
      Given("a complete canonical ledger with focused malformed and semantic mutations")
      val realization = _realization()
      val base = _ledger(realization)
      val canonical = InternalModelDecisionRecordCodec.encode(base)
      val badbytes = canonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8)
      val unsortedtargets = new String(canonical, StandardCharsets.UTF_8).replace(
        "\"affectedTargets\":[{\"semanticIdentity\":\"e-customer\",\"semanticIdentityKind\":\"element\"},{\"semanticIdentity\":\"r-uses\",\"semanticIdentityKind\":\"relationship\"}]",
        "\"affectedTargets\":[{\"semanticIdentity\":\"r-uses\",\"semanticIdentityKind\":\"relationship\"},{\"semanticIdentity\":\"e-customer\",\"semanticIdentityKind\":\"element\"}]"
      ).getBytes(StandardCharsets.UTF_8)
      val codecinputs = Vector(
        InternalModelVerifiedDecision("decision-main", "decision", "decisions.json", true, Vector("realization-main"), badbytes.toVector),
        InternalModelVerifiedDecision("decision-main", "decision", "decisions.json", true, Vector("realization-main"), InternalModelDecisionRecordCodec.encode(base.copy(profile = "unsupported")).toVector),
        InternalModelVerifiedDecision("decision-main", "decision", "decisions.json", true, Vector("realization-main"), unsortedtargets.toVector)
      )

      codecinputs.foreach { decision =>
        When("canonical bytes, profile vocabulary, or an identity collection order is invalid")
        val result = InternalModelDecisionRecordCodec.decode(decision)

        Then("the codec rejects instead of normalizing or repairing the candidate")
        result.isLeft shouldBe true
      }

      val badcurrent = base.copy(records = Vector(base.records.head.copy(basis = base.records.head.basis.copy(sha256 = "sha256:" + ("f" * 64)))))
      val hiddencondition = base.copy(records = Vector(base.records.head.copy(realizationConditionIds = Vector.empty)))
      val hiddensourcecondition = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == "realization-source" then evidence.copy(conditionIds = Vector.empty) else evidence
      })))
      val changedsource = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == "realization-source" then evidence.copy(source = evidence.source.copy(identity = "other-source")) else evidence
      })))
      val provideractor = base.copy(records = Vector(base.records.head.copy(actor = InternalModelDecisionActor("provider", "provider-1", "provider"))))
      val historicalterminal = base.copy(records = Vector(base.records.head.copy(basis = base.records.head.basis.copy(status = "historical-unverified"))))
      val fork = base.copy(records = Vector(
        base.records.head.copy(decisionIdentity = "decision-root", topicIdentity = "topic-fork", state = "superseded"),
        base.records.head.copy(decisionIdentity = "decision-left", topicIdentity = "topic-fork", supersedes = Some("decision-root")),
        base.records.head.copy(decisionIdentity = "decision-right", topicIdentity = "topic-fork", supersedes = Some("decision-root"))
      ))
      val invalid = Vector(badcurrent, hiddencondition, changedsource, provideractor, historicalterminal, fork)

      invalid.foreach { ledger =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("a current-basis, actor, source-condition, historical-terminal, or chain invariant is changed")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("admission fails closed without selecting a winner, repairing history, or promoting provider input")
          result.isSuccess shouldBe false
        }
      }

      val oldcurrent = base.records.head.copy(
        decisionIdentity = "decision-old-current",
        topicIdentity = "topic-old-current",
        state = "superseded",
        basis = base.records.head.basis.copy(sha256 = "sha256:" + ("2" * 64), status = "current")
      )
      val oldcurrentterminal = base.records.head.copy(
        decisionIdentity = "decision-old-current-terminal",
        topicIdentity = "topic-old-current",
        supersedes = Some("decision-old-current")
      )
      Vector(
        ("old predecessor is labeled current", base.copy(records = Vector(oldcurrent, oldcurrentterminal))),
        ("source condition IDs are hidden", hiddensourcecondition)
      ).foreach { case (_, ledger) =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("a historical predecessor is labeled current or a realization-source condition is omitted")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("current admission fails without accepting an old basis or hiding the selected source limitation")
          result.isSuccess shouldBe false
        }
      }

      val sourcefieldmutations = Vector(
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == "realization-source" then evidence.copy(source = evidence.source.copy(authority = "other-authority")) else evidence
        }))),
        changedsource,
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == "realization-source" then evidence.copy(source = evidence.source.copy(locator = Some("other-locator"))) else evidence
        }))),
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == "realization-source" then evidence.copy(source = evidence.source.copy(revision = Some("other-revision"))) else evidence
        }))),
        base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == "realization-source" then evidence.copy(source = evidence.source.copy(sha256 = "sha256:" + ("e" * 64))) else evidence
        })))
      )
      sourcefieldmutations.foreach { ledger =>
        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger))) { root =>
          When("one realization-source authority, identity, locator, revision, or digest field is changed")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("the exact admitted source object is required for current decision admission")
          result.isSuccess shouldBe false
        }
      }
    }

    "keep structural package validity separate from decision readiness and use only captured handoff bytes" in {
      Given("a package whose optional decision is absent and a separately captured valid handoff")
      val realization = _realization()
      val decision = InternalModelDecisionRecordCodec.encode(_ledger(realization))

      _with_fixture(realization, None) { root =>
        When("the V1 structural inventory has no present optional decision")
        val structural = InternalModelPackageValidator.validateStructure(root)
        val admission = InternalModelDecisionRecordValidator.validate(root)

        Then("structural validation remains compatible while decision readiness fails")
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

    "retain compatible selected realization V1 and V2 forms and a safe nonconventional decision path" in {
      Given("portable V1 and V2 semantic realizations with the same bounded decision ledger and a manifest-relative decision filename")
      forAll(Gen.oneOf(false, true)) { v2 =>
        val realization = _realization(v2)
        val ledger = _ledger(realization)

        _with_fixture(realization, Some(InternalModelDecisionRecordCodec.encode(ledger)), decisionpath = "records/decision-ledger.json") { root =>
          When("the decision is admitted from a safe path that is not the conventional filename")
          val result = InternalModelDecisionRecordValidator.validate(root)

          Then("the decision grammar remains independent of filename and preserves the selected realization profile")
          result.toOption.map(_.realization.profile) shouldBe Some(if v2 then "ccdm-realization-v2" else "ccdm-realization-v1")
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
        val result = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision("decision-main", "decision", "decision.json", true, Vector.empty, bytes.toVector))

        Then("the closed parser rejects it without parser fallback")
        result.isLeft shouldBe true
      }

      val invalidcodec = Vector(
        base.copy(ledgerIdentity = ""),
        base.copy(records = Vector.empty),
        base.copy(records = Vector(record.copy(state = "implicit"))),
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
        base.copy(records = Vector(record.copy(basis = record.basis.copy(status = "unrecognized")))),
        base.copy(records = Vector(record.copy(consideredEvidence = record.consideredEvidence.map { evidence =>
          if evidence.kind == "external-human" then evidence.copy(sourceReferenceId = Some("ref-relationship"), conditionIds = Vector("c-limitation")) else evidence
        })))
      )
      invalidcodec.foreach { ledger =>
        When("a required scalar, state/status vocabulary, local identity uniqueness, choice collision, or external realization link is invalid")
        val bytes = InternalModelDecisionRecordCodec.encode(ledger)
        val result = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision("decision-main", "decision", "decision.json", true, Vector.empty, bytes.toVector))

        Then("the codec rejects the typed candidate rather than treating encoding as admission")
        result.isLeft shouldBe true
      }

      val badartifact = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationArtifactId = "other-realization"))))
      val badidentity = base.copy(records = Vector(record.copy(basis = record.basis.copy(realizationIdentity = "other-identity"))))
      val badscope = base.copy(records = Vector(record.copy(basis = record.basis.copy(scope = _scope.copy(componentIdentity = "other-component")))))
      val unknowntarget = base.copy(records = Vector(record.copy(affectedTargets = Vector(InternalModelSemanticTarget("element", "e-missing")))))
      val unknowncondition = base.copy(records = Vector(record.copy(realizationConditionIds = Vector("c-missing"))))
      val unknownsource = base.copy(records = Vector(record.copy(consideredEvidence = record.consideredEvidence.map { evidence =>
        if evidence.kind == "realization-source" then evidence.copy(sourceReferenceId = Some("ref-missing"), conditionIds = Vector.empty) else evidence
      })))
      val supersededterminal = base.copy(records = Vector(record.copy(state = "superseded")))
      val dangling = base.copy(records = Vector(record.copy(supersedes = Some("decision-missing"))))
      val self = base.copy(records = Vector(record.copy(supersedes = Some("decision-current"))))
      val acceptedwithsuccessor = base.copy(records = Vector(
        record.copy(decisionIdentity = "decision-root", topicIdentity = "topic-chain"),
        record.copy(decisionIdentity = "decision-successor", topicIdentity = "topic-chain", supersedes = Some("decision-root"))
      ))
      val duplicatedterminal = base.copy(records = Vector(
        record.copy(decisionIdentity = "decision-one", topicIdentity = "topic-terminal"),
        record.copy(decisionIdentity = "decision-two", topicIdentity = "topic-terminal")
      ))
      val cycle = base.copy(records = Vector(
        record.copy(decisionIdentity = "decision-cycle-a", topicIdentity = "topic-cycle", state = "superseded", supersedes = Some("decision-cycle-b")),
        record.copy(decisionIdentity = "decision-cycle-b", topicIdentity = "topic-cycle", state = "superseded", supersedes = Some("decision-cycle-a")),
        record.copy(decisionIdentity = "decision-cycle-terminal", topicIdentity = "topic-cycle")
      ))
      val cross = base.copy(records = Vector(
        record.copy(decisionIdentity = "decision-topic-a", topicIdentity = "topic-a", state = "superseded"),
        record.copy(decisionIdentity = "decision-topic-b", topicIdentity = "topic-b", supersedes = Some("decision-topic-a"))
      ))
      val ledgerscopemismatch = base.copy(scope = _scope.copy(componentIdentity = "other-component"))
      val invalidadmission = Vector(badartifact, badidentity, badscope, ledgerscopemismatch, unknowntarget, unknowncondition, unknownsource, supersededterminal, dangling, self, acceptedwithsuccessor, duplicatedterminal, cycle, cross)
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

        Then("structural V1 compatibility is preserved while decision admission rejects the missing direct handoff dependency")
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
        Given("a package whose manifest-verified decision bytes are replaced after fixture construction")
        Files.write(root.resolve("src/main/internal-model/decisions/decision.json"), "tampered\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)

        When("a fresh package-bound decision admission is requested")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("the manifest raw-byte digest mismatch fails before decision parsing")
        result.isSuccess shouldBe false
      }
    }

    "reject every closed-schema and canonical-byte mutation deterministically" in {
      Given("one canonical decision ledger and table-driven mutations at every closed object boundary")
      val base = _ledger(_realization())
      val decision = InternalModelVerifiedDecision("decision-main", "decision", "decision.json", true, Vector.empty, InternalModelDecisionRecordCodec.encode(base).toVector)

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

      val unsupportedschema = base.copy(schemaVersion = "2.0")
      val unsupportedactor = base.copy(records = Vector(base.records.head.copy(actor = base.records.head.actor.copy(kind = "provider"))))
      val supportedevidence = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.evidenceIdentity == "e-external" then evidence.copy(kind = "provider") else evidence
      })))
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
      val uppercasedigest = base.copy(records = Vector(base.records.head.copy(provenance = base.records.head.provenance.copy(sha256 = "sha256:" + ("A" * 64)))))
      val shortdigest = base.copy(records = Vector(base.records.head.copy(basis = base.records.head.basis.copy(sha256 = "sha256:abcd"))))
      val duplicatedecision = base.copy(records = Vector(base.records.head, base.records.head.copy(topicIdentity = "topic-other")))
      val duplicatecondition = base.copy(records = Vector(base.records.head.copy(realizationConditionIds = Vector("c-limitation", "c-limitation"))))
      val duplicateevidencecondition = base.copy(records = Vector(base.records.head.copy(consideredEvidence = base.records.head.consideredEvidence.map { evidence =>
        if evidence.kind == "realization-source" then evidence.copy(conditionIds = Vector("c-limitation", "c-limitation")) else evidence
      })))
      val canonicalmutations = Vector(
        "unsupported schemaVersion" -> unsupportedschema,
        "unsupported actor kind" -> unsupportedactor,
        "unsupported evidence kind" -> supportedevidence,
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
        "uppercase digest" -> uppercasedigest,
        "short digest" -> shortdigest,
        "duplicate decision identity" -> duplicatedecision,
        "duplicate realization condition identity" -> duplicatecondition,
        "duplicate evidence condition identity" -> duplicateevidencecondition
      )
      canonicalmutations.foreach { case (_, ledger) =>
        When("a closed scalar, vocabulary, digest, or identity uniqueness invariant is mutated")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = InternalModelDecisionRecordCodec.encode(ledger).toVector))

        Then("the codec rejects the typed candidate rather than allowing encoding to become admission")
        result.isLeft shouldBe true
      }

      val rawbase = base.copy(records = Vector(
        base.records.head.copy(decisionIdentity = "decision-a", topicIdentity = "topic-a"),
        base.records.head.copy(decisionIdentity = "decision-b", topicIdentity = "topic-b")
      ))
      val rawcanonical = InternalModelDecisionRecordCodec.encode(rawbase)
      val rawmutations = Vector(
        ("malformed Unicode scalar escape", new String(rawcanonical, StandardCharsets.UTF_8).replace("\"rationale\":\"利用者の根拠と制約を保持する\"", "\"rationale\":\"\\ud800\"").getBytes(StandardCharsets.UTF_8)),
        ("trailing bytes", rawcanonical ++ "\n".getBytes(StandardCharsets.UTF_8)),
        ("noncanonical whitespace", rawcanonical.dropRight(1) ++ " \n".getBytes(StandardCharsets.UTF_8))
      ) ++ _unsorted_raw_mutations(rawbase)
      rawmutations.foreach { case (_, bytes) =>
        When("malformed Unicode, trailing/noncanonical bytes, or any identity array order is supplied")
        val result = InternalModelDecisionRecordCodec.decode(decision.copy(bytes = bytes.toVector))

        Then("the codec fails closed and never silently sorts the raw mutation")
        result.isLeft shouldBe true
      }
    }

    "separate structural compatibility from decision readiness across inventory and digest mutations" in {
      Given("portable package fixtures with optional, missing, duplicate, dependent, and tampered decision artifacts")
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

        When("the selected realization bytes are tampered after manifest construction")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("fresh package admission fails on the captured artifact digest")
        result.isSuccess shouldBe false
      }
      _with_fixture(realization, Some(decision)) { root =>
        val path = root.resolve("src/main/internal-model/manifest.yaml")
        val bytes = Files.readAllBytes(path)
        val text = new String(bytes, StandardCharsets.UTF_8)
        val marker = "\"packageDigest\":\"sha256:"
        val offset = text.indexOf(marker) + marker.length
        val chars = text.toCharArray
        chars(offset) = if chars(offset) == '0' then '1' else '0'
        Files.write(path, new String(chars).getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)

        When("the manifest packageDigest is changed without rewriting its artifacts")
        val result = InternalModelDecisionRecordValidator.validate(root)

        Then("fresh package admission fails closed on manifest digest mismatch")
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
      } yield (token, rationale, choicedescription, alternativedescription, rejectionrationale, prose, recordorder, targetorder, evidenceorder, alternativeorder, conditionorder, sourceconditionorder)

      forAll(propertyinput) { case (token, rationale, choicedescription, alternativedescription, rejectionrationale, prose, recordorder, targetorder, evidenceorder, alternativeorder, conditionorder, sourceconditionorder) =>
        val sourceevidence = base.records.head.consideredEvidence.map { evidence =>
          if evidence.kind == "realization-source" then evidence.copy(conditionIds = sourceconditionorder.map(index => Vector("c-limitation", "c-source-secondary")(index))) else evidence
        }
        val logicalrecords = Vector(
          base.records.head.copy(
            decisionIdentity = s"decision-$token-a",
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
            decisionIdentity = s"decision-$token-b",
            topicIdentity = s"topic-$token-b",
            rationale = prose,
            selectedChoice = base.records.head.selectedChoice.copy(description = rationale)
          )
        )
        val permuted = recordorder.map(logicalrecords)
        val expectedrecords = logicalrecords.map { logicalrecord =>
          logicalrecord.copy(
            affectedTargets = base.records.head.affectedTargets,
            consideredEvidence = logicalrecord.consideredEvidence.sortBy(_.evidenceIdentity).map(evidence => evidence.copy(conditionIds = evidence.conditionIds.sorted)),
            rejectedAlternatives = logicalrecord.rejectedAlternatives.sortBy(_.alternativeIdentity),
            realizationConditionIds = logicalrecord.realizationConditionIds.sorted
          )
        }

        When("the pure codec encodes permuted records, targets, evidence, alternatives, condition IDs, and generated explanatory text")
        val encoded = InternalModelDecisionRecordCodec.encode(base.copy(records = permuted))
        val expected = InternalModelDecisionRecordCodec.encode(base.copy(records = expectedrecords))
        val decoded = InternalModelDecisionRecordCodec.decode(InternalModelVerifiedDecision("decision-main", "decision", "decisions.json", true, Vector("realization-main"), encoded.toVector))

        Then("canonical bytes and complete retained identities are invariant while generated text remains verbatim and makes no winner inference")
        encoded.toVector shouldBe expected.toVector
        decoded.toOption.map(_.records.map(record => (record.decisionIdentity, record.topicIdentity))) shouldBe Some(Vector((s"decision-$token-a", s"topic-$token-a"), (s"decision-$token-b", s"topic-$token-b")))
        decoded.toOption.map(_.records.head).map { record =>
          (record.rationale, record.selectedChoice.choiceIdentity, record.selectedChoice.description, record.rejectedAlternatives.map(_.alternativeIdentity), record.rejectedAlternatives.map(_.description), record.assumptions, record.conditions, record.limitations)
        } shouldBe Some((rationale, "choice-primary", choicedescription, Vector("choice-alternative", "choice-secondary"), Vector(alternativedescription, alternativedescription), Vector(prose, rationale, prose), Vector(rationale, prose, rationale), Vector(prose, rationale, prose)))
      }
    }
  }

  private def _ledger(realization: Array[Byte]): InternalModelDecisionLedger = {
    val record = InternalModelDecisionRecord(
      "decision-current",
      "topic-current",
      "accepted",
      InternalModelDecisionActor("human", "architect-1", "component architect"),
      _provenance,
      InternalModelDecisionChoice("choice-primary", "Keep the source-backed relationship"),
      "利用者の根拠と制約を保持する",
      Vector(InternalModelSemanticTarget("element", "e-customer"), InternalModelSemanticTarget("relationship", "r-uses")),
      Vector(
        InternalModelDecisionEvidence("e-external", "external-human", InternalModelSemanticSource("external-human", "external-1", None, None, "sha256:" + ("3" * 64)), None, Vector.empty, Vector("external condition"), Vector("external limitation")),
        InternalModelDecisionEvidence("e-provider", "provider-proposal", InternalModelSemanticSource("provider", "provider-1", Some("provider/result"), None, "sha256:" + ("4" * 64)), None, Vector.empty, Vector.empty, Vector("proposal limitation")),
        InternalModelDecisionEvidence("e-realization", "realization-source", _source, Some("ref-relationship"), Vector("c-limitation", "c-source-secondary"), Vector("source condition", "source condition duplicate", "source condition duplicate"), Vector("source limitation", "source limitation duplicate"))
      ),
      Vector("assumption retained in supplied order", "assumption duplicate", "assumption duplicate"),
      Vector("decision condition", "decision condition second", "decision condition second"),
      Vector("decision limitation", "decision limitation second", "decision limitation second"),
      Vector("c-limitation", "c-source-secondary"),
      Vector(
        InternalModelDecisionAlternative("choice-alternative", "Use an unbacked relationship", "It lacks the exact selected source witness"),
        InternalModelDecisionAlternative("choice-secondary", "Use a separately sourced relationship", "It is not the selected source-backed choice")
      ),
      InternalModelDecisionBasis("realization-main", "realization-order", _sha256(realization), _scope, "current"),
      None
    )
    InternalModelDecisionLedger("ccdm-decision-records-v1", "1.0", "ledger-order", _scope, Vector(record), Vector.empty)
  }

  private def _realization(v2: Boolean = false): Array[Byte] = {
    val references = Vector(
      _reference("ref-customer", "element", "e-customer", "anchor-customer"),
      _reference("ref-relationship", "relationship", "r-uses", "anchor-relationship"),
      _reference("ref-usecase", "element", "e-usecase", "anchor-usecase")
    )
    _canonical(Json.obj(
      "canonicalAssertions" -> Json.fromValues(Vector(
        _assertion("a-customer", "element", "e-customer", "ref-customer", "Customer is a party", Vector.empty, v2),
        _assertion("a-relationship", "relationship", "r-uses", "ref-relationship", "Use case uses Customer", Vector("c-limitation"), v2),
        _assertion("a-usecase", "element", "e-usecase", "ref-usecase", "Place an order", Vector.empty, v2)
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
      "profile" -> Json.fromString(if v2 then "ccdm-realization-v2" else "ccdm-realization-v1"),
      "realizationIdentity" -> Json.fromString("realization-order"),
      "relationships" -> Json.arr(_relationship()),
      "schemaVersion" -> Json.fromString(if v2 then "2.0" else "1.0"),
      "scope" -> _scope_json,
      "sourceReferences" -> Json.fromValues(references),
      "successorLinks" -> Json.arr(),
      "traceability" -> Json.obj("consumedSnapshotArtifactIds" -> Json.arr(Json.fromString("snapshot-model")))
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
      "schemaVersion" -> Json.fromString("1.0"),
      "snapshotKind" -> Json.fromString("model-context"),
      "source" -> _source_json
    ))

  private def _reference(referenceid: String, kindvalue: String, identity: String, anchor: String): Json =
    Json.obj(
      "referenceId" -> Json.fromString(referenceid),
      "snapshotArtifactId" -> Json.fromString("snapshot-model"),
      "source" -> _source_json,
      "sourceAnchor" -> Json.fromString(anchor),
      "target" -> Json.obj("semanticIdentity" -> Json.fromString(identity), "semanticIdentityKind" -> Json.fromString(kindvalue))
    )

  private def _assertion(assertionid: String, kindvalue: String, identity: String, referenceid: String, content: String, conditionids: Vector[String], v2: Boolean): Json = {
    val fields = Vector(
      "assertionId" -> Json.fromString(assertionid),
      "conditionIds" -> Json.fromValues(conditionids.map(Json.fromString)),
      "content" -> Json.fromString(content),
      "semanticIdentity" -> Json.fromString(identity),
      "semanticIdentityKind" -> Json.fromString(kindvalue),
      "sourceReferenceId" -> Json.fromString(referenceid)
    )
    Json.obj((if v2 then fields :+ ("association" -> Json.Null) else fields)*)
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
    val root = Files.createTempDirectory("internal-model-decision-record-")
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
    val sourcemissing = _canonical(_update_record_child(ledger, "provenance", _.remove("sha256"))).toVector
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
      "source missing digest" -> sourcemissing,
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
      "dependsOn" -> Json.fromValues(dependencies.map(Json.fromString)),
      "path" -> Json.fromString(path),
      "required" -> Json.fromBoolean(required),
      "role" -> Json.fromString(rolevalue),
      "sha256" -> Json.fromString(_sha256(bytes))
    )

  private def _manifest(artifacts: Vector[Json]): Array[Byte] = {
    def _order_(remaining: Vector[Json], collected: Vector[Json]): Vector[Json] =
      if remaining.isEmpty then collected else {
        val ready = remaining.filter(artifact => artifact.hcursor.get[Vector[String]]("dependsOn").toOption.getOrElse(Vector.empty).forall(id => collected.exists(_.hcursor.get[String]("artifactId").toOption.contains(id)))).sortBy(_.hcursor.get[String]("artifactId").toOption.getOrElse("")).head
        _order_(remaining.filterNot(_ == ready), collected :+ ready)
      }
    val root = JsonObject.fromIterable(Vector(
      "artifacts" -> Json.fromValues(_order_(artifacts, Vector.empty)),
      "lifecycleState" -> Json.fromString("draft"),
      "packageDigest" -> Json.fromString("sha256:" + ("0" * 64)),
      "packageId" -> Json.fromString("01234567-89ab-cdef-0123-456789abcdef"),
      "projectId" -> Json.fromString("decision-sample"),
      "projectNamespace" -> Json.fromString("org.example"),
      "revision" -> Json.fromInt(1),
      "schemaVersion" -> Json.fromString("1.0")
    ))
    _canonical(root.add("packageDigest", Json.fromString(_sha256(_canonical(root.remove("packageDigest").toJson)))).toJson)
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
      "revision" -> _source.revision.map(Json.fromString).getOrElse(Json.Null),
      "sha256" -> Json.fromString(_source.sha256)
    )

  private def _canonical(json: Json): Array[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString

  private def _write(path: Path, bytes: Array[Byte]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }

  private def _delete_tree(root: Path): Unit =
    if Files.exists(root) then Files.walk(root).iterator.asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.delete)
}
