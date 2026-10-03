package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.{Consequence, Conclusion}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable evidence policy: exact metadata, opaque omissions and fail-closed admission.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelEvidencePolicySpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelEvidenceInputPayload.{Raw, SafeIdentity, Unavailable}
  import InternalModelRetainedEvidencePayload.{Identity, Omitted}
  private val _policy = InternalModelEvidencePolicy
  private val _reference = _record("  証拠🧭\nlogical id  ", 37L)
  private val _redaction_reference = _record("独立した秘匿記録 λ", 83L)
  private val _identity_kinds = Vector(InternalModelEvidenceKind.ProviderIdentity,
    InternalModelEvidenceKind.ModelIdentity, InternalModelEvidenceKind.ToolIdentity)
  private val _excluded_kinds = Vector(InternalModelEvidenceKind.Prompt, InternalModelEvidenceKind.Response,
    InternalModelEvidenceKind.CallTree, InternalModelEvidenceKind.ExternalEvidence)
  private val _sensitive_text = Gen.nonEmptyListOf(Gen.oneOf("秘密", "🔐", "é", "e\u0301", "λ", "\n", " token=secret ")).map(_.mkString)
  private val _narrative_text = _sensitive_text.map(text => "語り" + text.take(4094))
  private val _revision = Gen.frequency(2 -> Gen.const(Long.MaxValue), 8 -> Gen.choose(1L, 100000L))
  private val _token = Gen.choose(1, 128).flatMap(size => Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString))
  private val _invalid_taxonomy = Conclusion.operationInvalid("expected").observation.taxonomy

  "Evidence retention" should {
    "raw bodies and purported safe labels" which {
      "omits every kind's generated sensitive body with output independent of its text" in {
        forAll(_sensitive_text, _sensitive_text) { (first, second) =>
          Given("two independently generated sensitive Unicode bodies and every evidence kind")
          val inputs = InternalModelEvidenceKind.values.toVector.flatMap { kind =>
            Vector(first, second).map(text => InternalModelEvidenceInput(_reference, kind, Raw(text)))
          }
          When("the raw bodies are admitted by the retention policy")
          val results = inputs.map(_policy.retain)
          Then("both bodies yield only the exact reference, kind and policy omission")
          results.zip(inputs).foreach { case (result, input) =>
            result.toOption shouldBe Some(InternalModelRetainedEvidence(_reference, input.kind,
              Omitted(InternalModelEvidenceOmission.PolicyExcluded)))
          }
        }
      }

      "excludes purported safe or redacted prompt and response bodies" in {
        Given("Prompt and Response with a valid identity or an upstream redacted-text claim")
        val inputs = Vector(InternalModelEvidenceKind.Prompt, InternalModelEvidenceKind.Response).flatMap { kind =>
          Vector(SafeIdentity("provider:model-1"),
            InternalModelEvidenceInputPayload.RedactedText("秘匿済み 🧭", _redaction_reference))
            .map(payload => InternalModelEvidenceInput(_reference, kind, payload))
        }
        When("retention interprets each purportedly safe body")
        val results = inputs.map(_policy.retain)
        Then("neither label admits a prompt or response body")
        results.zip(inputs).foreach { case (result, input) =>
          result.toOption shouldBe Some(InternalModelRetainedEvidence(input.reference, input.kind,
            Omitted(InternalModelEvidenceOmission.PolicyExcluded)))
        }
      }

      "retains exact bounded identity metadata only in the three identity kinds" in {
        forAll(_token, _revision, _revision) { (token, recordrevision, redactionrevision) =>
          Given("an identity token, an independent logical revision, and a distinct redaction revision")
          val reference = _record("  論理 λ  ", recordrevision)
          val redaction = _record("redaction", redactionrevision)
          val inputs = _identity_kinds.map(kind => InternalModelEvidenceInput(reference, kind, SafeIdentity(token)))
          val unsupported = _identity_kinds.map(kind => InternalModelEvidenceInput(reference, kind,
            InternalModelEvidenceInputPayload.RedactedText("claimed redaction", redaction))) :+
            InternalModelEvidenceInput(reference, InternalModelEvidenceKind.Narrative, SafeIdentity(token))
          When("supported and unsupported pairings are admitted and valid outputs revalidated")
          val results = inputs.map(input => _policy.retain(input).flatMap(_policy.validate))
          val rejected = unsupported.map(_policy.retain)
          Then("exact identity metadata survives and unsupported pairings remain structured failures")
          results.zip(inputs).foreach { case (result, input) =>
            result.toOption shouldBe Some(InternalModelRetainedEvidence(reference, input.kind, Identity(token)))
          }
          rejected.foreach(_invalid)
        }
      }
    }

    "narrative claims and opaque references" which {
      "preserves exact Unicode narrative and its independently versioned redaction claim" in {
        forAll(_narrative_text, _revision, _revision) { (text, revision, redactionrevision) =>
          Given("pre-redacted Narrative text with significant Unicode and independent logical references")
          val reference = _record("  語り🧭  ", revision)
          val redaction = _record("  秘匿 e\u0301  ", redactionrevision)
          val input = InternalModelEvidenceInput(reference, InternalModelEvidenceKind.Narrative,
            InternalModelEvidenceInputPayload.RedactedText(text, redaction))
          When("the claim is retained and its output independently revalidated")
          val result = _policy.retain(input).flatMap(_policy.validate)
          Then("the exact text and redaction reference survive without a secret-removal inference")
          result.toOption shouldBe Some(InternalModelRetainedEvidence(reference, input.kind,
            InternalModelRetainedEvidencePayload.RedactedText(text, redaction)))
        }
      }

      "keeps CallTree and external evidence opaque and unavailable distinct for every kind" in {
        Given("valid bodies for opaque kinds and explicit unavailable input for every kind")
        val opaque = Vector(InternalModelEvidenceKind.CallTree, InternalModelEvidenceKind.ExternalEvidence).flatMap { kind =>
          Vector(Raw("body"), SafeIdentity("tree:1"),
            InternalModelEvidenceInputPayload.RedactedText("claimed body", _redaction_reference))
            .map(payload => InternalModelEvidenceInput(_reference, kind, payload))
        }
        val unavailable = InternalModelEvidenceKind.values.toVector.map(kind => InternalModelEvidenceInput(_reference, kind, Unavailable))
        When("reference-only and unavailable evidence are retained and revalidated")
        val omitted = opaque.map(input => _policy.retain(input).flatMap(_policy.validate))
        val missing = unavailable.map(input => _policy.retain(input).flatMap(_policy.validate))
        Then("opaque references retain no body and missing evidence cannot become a reconstructed body")
        omitted.zip(opaque).foreach { case (result, input) =>
          result.toOption shouldBe Some(InternalModelRetainedEvidence(input.reference, input.kind,
            Omitted(InternalModelEvidenceOmission.PolicyExcluded)))
        }
        missing.zip(unavailable).foreach { case (result, input) =>
          result.toOption shouldBe Some(InternalModelRetainedEvidence(input.reference, input.kind,
            Omitted(InternalModelEvidenceOmission.Unavailable)))
        }
      }
    }

    "independent admission and finite bounds" which {
      "admits exact caps and rejects cap plus one even when the input would be omitted" in {
        Given("identity 128/129, Narrative 4096/4097 and raw 1048576/1048577 character pairs")
        val valid = Vector(
          InternalModelEvidenceInput(_reference, InternalModelEvidenceKind.ProviderIdentity, SafeIdentity("a" * 128)),
          InternalModelEvidenceInput(_reference, InternalModelEvidenceKind.Narrative,
            InternalModelEvidenceInputPayload.RedactedText("界" * 4096, _redaction_reference)),
          InternalModelEvidenceInput(_reference, InternalModelEvidenceKind.Prompt, Raw("x" * 1048576))
        )
        val invalid = Vector(
          valid(0).copy(payload = SafeIdentity("a" * 129)),
          valid(1).copy(payload = InternalModelEvidenceInputPayload.RedactedText("界" * 4097, _redaction_reference)),
          valid(2).copy(payload = Raw("x" * 1048577))
        )
        When("boundary inputs are retained and valid outputs revalidated")
        val admitted = valid.map(input => _policy.retain(input).flatMap(_policy.validate))
        val rejected = invalid.map(_policy.retain)
        Then("the exact caps are admitted and each next character rejects before omission")
        admitted.map(_.isSuccess) shouldBe Vector(true, true, true)
        rejected.foreach(_invalid)
      }

      "rejects complete malformed input graphs without sensitive diagnostics" in {
        Given("null graphs, forged logical references, malformed text and omission-destined bad values")
        val input = InternalModelEvidenceInput(_reference, InternalModelEvidenceKind.Prompt, Raw("secret-marker-秘密"))
        val badreferences = Vector(null, _reference.copy(recordId = null.asInstanceOf[InternalModelRecordId]),
          _reference.copy(recordId = " \t".asInstanceOf[InternalModelRecordId]),
          _reference.copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision]),
          _reference.copy(recordRevision = (-1L).asInstanceOf[InternalModelRecordRevision]))
        val badpayloads = Vector(null, Raw(null), SafeIdentity(null), SafeIdentity(""), SafeIdentity("  "),
          SafeIdentity("secret-marker-秘密"), SafeIdentity("a/b"), SafeIdentity("a\u0000"), SafeIdentity("a" * 129),
          InternalModelEvidenceInputPayload.RedactedText(null, _redaction_reference),
          InternalModelEvidenceInputPayload.RedactedText(" \n\t", _redaction_reference),
          InternalModelEvidenceInputPayload.RedactedText("secret-marker-秘密\u0000", _redaction_reference),
          InternalModelEvidenceInputPayload.RedactedText("界" * 4097, _redaction_reference),
          InternalModelEvidenceInputPayload.RedactedText("safe", null)) ++
          badreferences.map(reference => InternalModelEvidenceInputPayload.RedactedText("safe", reference))
        val inputs = Vector(null, input.copy(kind = null)) ++ badreferences.map(reference => input.copy(reference = reference)) ++
          _excluded_kinds.flatMap(kind => badpayloads.map(payload => input.copy(kind = kind, payload = payload)))
        When("all malformed inputs are interpreted before the exclusion decision")
        val results = inputs.map(_policy.retain)
        Then("every invalid graph fails structurally without exposing the raw or labeled text")
        results.foreach { result =>
          _invalid(result)
          result.show should not include "secret-marker-秘密"
        }
      }

      "rejects forged retained pairings and retained null or malformed values" in {
        Given("each forbidden body-kind pairing and independently malformed retained fields")
        val bodykinds = InternalModelEvidenceKind.values.toVector
        val identity = InternalModelRetainedEvidence(_reference, InternalModelEvidenceKind.ProviderIdentity, Identity("provider:1"))
        val narrative = InternalModelRetainedEvidence(_reference, InternalModelEvidenceKind.Narrative,
          InternalModelRetainedEvidencePayload.RedactedText("safe λ", _redaction_reference))
        val contradictions = bodykinds.filterNot(_identity_kinds.contains).map(kind => identity.copy(kind = kind)) ++
          bodykinds.filterNot(_ == InternalModelEvidenceKind.Narrative).map(kind => narrative.copy(kind = kind))
        val invalid = Vector(null, identity.copy(reference = null), identity.copy(kind = null), identity.copy(payload = null),
          identity.copy(payload = Identity(null)), identity.copy(payload = Identity("a/b")),
          identity.copy(payload = Identity("a" * 129)), identity.copy(payload = Omitted(null)),
          identity.copy(reference = _reference.copy(recordId = null.asInstanceOf[InternalModelRecordId])),
          identity.copy(reference = _reference.copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision])),
          narrative.copy(payload = InternalModelRetainedEvidencePayload.RedactedText(null, _redaction_reference)),
          narrative.copy(payload = InternalModelRetainedEvidencePayload.RedactedText(" ", _redaction_reference)),
          narrative.copy(payload = InternalModelRetainedEvidencePayload.RedactedText("safe\u0000", _redaction_reference)),
          narrative.copy(payload = InternalModelRetainedEvidencePayload.RedactedText("界" * 4097, _redaction_reference)),
          narrative.copy(payload = InternalModelRetainedEvidencePayload.RedactedText("safe", null)))
        When("supplied retained values pass through independent admission")
        val results = (contradictions ++ invalid).map(_policy.validate)
        Then("construction cannot bypass kind, graph or bounded-value admission")
        results.foreach(_invalid)
      }

      "admits either explicit omission for every kind without inventing an identity" in {
        Given("both omission reasons and every kind with an exact logical reference")
        val values = InternalModelEvidenceKind.values.toVector.flatMap { kind =>
          InternalModelEvidenceOmission.values.toVector.map(reason => InternalModelRetainedEvidence(_reference, kind, Omitted(reason)))
        }
        When("each supplied omission is independently admitted")
        val results = values.map(_policy.validate)
        Then("each retained value remains exactly the supplied reference-only omission")
        results.map(_.toOption) shouldBe values.map(Some(_))
      }

      "retains all independently invalid dimensions in one sanitized failure" in {
        Given("a malformed reference, null kind, and malformed omission-destined raw body")
        val input = InternalModelEvidenceInput(_reference.copy(recordId = " ".asInstanceOf[InternalModelRecordId],
          recordRevision = 0L.asInstanceOf[InternalModelRecordRevision]), null, Raw(null))
        When("independent admission dimensions are composed")
        val result = _policy.retain(input)
        Then("the structured result carries every dimension rather than defaulting or short-circuiting")
        _invalid(result)
        val diagnostics = result match {
          case Consequence.Failure(conclusion) => conclusion.causes.map(_.show)
          case Consequence.Success(_) => fail("expected a structured invalid operation")
        }
        Vector("record-id", "record-revision", "kind", "raw")
          .foreach(dimension => diagnostics.exists(_.contains(dimension)) shouldBe true)
      }
    }
  }

  private def _record(recordid: String, revision: Long): InternalModelRecordReference = {
    InternalModelRecordReference(InternalModelRecordId.from(recordid).fold(message => fail(message), value => value),
      InternalModelRecordRevision.from(revision).fold(message => fail(message), value => value))
  }

  private def _invalid(result: Consequence[?]): Unit = {
    result match {
      case Consequence.Failure(conclusion) =>
        conclusion.causes.foreach { cause =>
          cause.observation.taxonomy shouldBe _invalid_taxonomy
          cause.getException shouldBe None
        }
      case Consequence.Success(_) => fail("expected a structured invalid operation")
    }
  }
}
