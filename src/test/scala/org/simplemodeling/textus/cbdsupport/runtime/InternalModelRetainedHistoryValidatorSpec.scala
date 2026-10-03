package org.simplemodeling.textus.cbdsupport.runtime

import java.time.Instant
import org.goldenport.{Consequence, Conclusion}
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Executable history contract: immutable audit values, exact relations and body-free tombstones.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class InternalModelRetainedHistoryValidatorSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelRetainedHistoryPayload.*
  private val _validator = InternalModelRetainedHistoryValidator
  private val _package_reference = InternalModelPackageReference(
    InternalModelPackageId.from("01234567-89ab-cdef-0123-456789abcdef").fold(message => fail(message), value => value),
    InternalModelProjectToken.from("org.example").fold(message => fail(message), value => value),
    InternalModelProjectToken.from("Project:one").fold(message => fail(message), value => value)
  )
  private val _scope = InternalModelSemanticScope("component λ", "projection-context", "usecase:記録")
  private val _projection = _artifact("projection-main", 73L, InternalModelArtifactRole.Projection)
  private val _subject = InternalModelReviewSubject(
    _reference("  対象 e\u0301🧭\n  ", 47L).recordId,
    _reference("subject revision", 47L).recordRevision,
    _package_reference, _scope, Vector(_projection, _artifact("source-main", 29L, InternalModelArtifactRole.SourceSnapshot))
  )
  private val _actor = InternalModelDecisionActor("provider", "  作成者 λ  ", "reviewer")
  private val _time = Instant.parse("2026-10-03T01:23:45.123456789Z")
  private val _evidence = Vector(
    InternalModelRetainedEvidence(_reference("provider evidence", 19L), InternalModelEvidenceKind.ProviderIdentity,
      InternalModelRetainedEvidencePayload.Identity("provider:one")),
    InternalModelRetainedEvidence(_reference("narrative evidence", 41L), InternalModelEvidenceKind.Narrative,
      InternalModelRetainedEvidencePayload.RedactedText("  秘匿済み e\u0301 🧭\n  ", _reference("redaction", 89L))),
    InternalModelRetainedEvidence(_reference("missing evidence", 59L), InternalModelEvidenceKind.ExternalEvidence,
      InternalModelRetainedEvidencePayload.Omitted(InternalModelEvidenceOmission.Unavailable))
  )
  private val _payloads = Vector(
    Review(_reference("review", 7L), _reference("candidate", 11L), _reference("semantic-diff", 17L),
      InternalModelRetainedReviewOutcome.AcceptedAsReview),
    Proposal(_reference("candidate", 11L), _projection),
    Alternative(_reference("proposal", 23L), _reference("alternative", 31L),
      InternalModelRetainedAlternativeDisposition.SelectedAsProposal),
    Supersession(_reference("previous", 43L), _reference("successor", 61L)),
    Evidence(_reference("evidence logical record", 97L))
  )
  private val _revision = Gen.frequency(2 -> Gen.const(Long.MaxValue), 8 -> Gen.choose(1L, 100000L))
  private val _logical_id = Gen.nonEmptyListOf(Gen.oneOf(" 記録 ", "λ", "é", "e\u0301", "🧭", "\n中")).map(_.mkString)
  private val _invalid_taxonomy = Conclusion.operationInvalid("expected").observation.taxonomy

  "Retained history admission" should {
    "exact immutable accounts" which {
      "retains all five payload families and all independent historical values" in {
        Given("all five families with Unicode subject IDs, independent revisions, actor, time and admitted evidence")
        val records = _payloads.map(payload => _history(payload))
        val expectedkinds = Vector(InternalModelRetainedHistoryKind.Review, InternalModelRetainedHistoryKind.Proposal,
          InternalModelRetainedHistoryKind.Alternative, InternalModelRetainedHistoryKind.Supersession,
          InternalModelRetainedHistoryKind.Evidence)
        When("each supplied account is structurally admitted")
        val results = records.map(_validator.validate)
        Then("the complete values and derived kinds remain exact without a current-state selector")
        results.map(_.toOption) shouldBe records.map(Some(_))
        records.map(_.payload.kind) shouldBe expectedkinds
        results.flatMap(_.toOption).foreach { record =>
          record.subject shouldBe _subject
          record.actor shouldBe _actor
          record.occurredAt shouldBe _time
          record.evidence shouldBe _evidence
        }
      }

      "records every review opinion and alternative disposition without creating approval" in {
        Given("all review outcomes and alternative dispositions over exact historical references")
        val reviews = InternalModelRetainedReviewOutcome.values.toVector.map(outcome => _history(
          Review(_reference("review", 7L), _reference("candidate", 11L), _reference("diff", 17L), outcome)))
        val alternatives = InternalModelRetainedAlternativeDisposition.values.toVector.map(disposition => _history(
          Alternative(_reference("proposal", 23L), _reference("alternative", 31L), disposition)))
        When("the accounts are admitted as their original immutable payloads")
        val results = (reviews ++ alternatives).map(_validator.validate)
        Then("accepted review and selected proposal remain recorded history with exact unchanged semantic basis")
        results.map(_.toOption) shouldBe (reviews ++ alternatives).map(Some(_))
        results.flatMap(_.toOption).map(_.payload.kind) shouldBe
          (Vector.fill(reviews.size)(InternalModelRetainedHistoryKind.Review) ++
            Vector.fill(alternatives.size)(InternalModelRetainedHistoryKind.Alternative))
        results.flatMap(_.toOption).map(_.subject) shouldBe Vector.fill(results.size)(_subject)
      }

      "preserves independent logical revisions and every evidence permutation without selection" in {
        forAll(_logical_id, _revision, _revision, Gen.choose(2, 6)) { (recordid, auditrevision, subjectrevision, size) =>
          Given("an independently versioned Unicode audit and subject with uniquely versioned evidence in opposite orders")
          val evidence = Vector.tabulate(size) { index =>
            InternalModelRetainedEvidence(_reference(" same evidence identity λ ", index.toLong + 1L),
              InternalModelEvidenceKind.ExternalEvidence,
              InternalModelRetainedEvidencePayload.Omitted(InternalModelEvidenceOmission.PolicyExcluded))
          }
          val subject = _subject.copy(subjectRevision = _reference("subject", subjectrevision).recordRevision)
          val forward = _history(Evidence(_reference("logical evidence", 101L)),
            _reference(recordid, auditrevision), subject, evidence)
          val reverse = forward.copy(evidence = evidence.reverse)
          When("both encounter orders are independently admitted")
          val first = _validator.validate(forward)
          val second = _validator.validate(reverse)
          Then("every accepted field and independent revision remains exact and neither ordering chooses a head")
          first.toOption shouldBe Some(forward)
          second.toOption shouldBe Some(reverse)
          first.toOption.map(_.evidence) shouldBe Some(evidence)
          second.toOption.map(_.evidence) shouldBe Some(evidence.reverse)
          first.toOption.map(_.payload) shouldBe second.toOption.map(_.payload)
          first.toOption should not be second.toOption
        }
      }

      "accepts historical semantic references without current artifact admission or ID normalization" in {
        Given("historical logical IDs with significant whitespace, different revisions and no current semantic inventory")
        val before = _reference("  same logical identity\n🧭  ", 103L)
        val after = _reference("  same logical identity\n🧭  ", 107L)
        val record = _history(Supersession(before, after), subject = _subject.copy(artifacts = Vector.empty))
        When("only the supplied history's structure is admitted")
        val result = _validator.validate(record)
        Then("different full references remain distinct and absent current artifacts do not erase historical meaning")
        result.toOption shouldBe Some(record)
        before.recordId.value shouldBe "  same logical identity\n🧭  "
        after.recordId.value shouldBe before.recordId.value
        after should not be before
      }
    }

    "malformed graphs, payload links and count boundaries" which {
      "rejects null and malformed package, subject, scope, actor, reference and evidence graphs" in {
        Given("a valid account and malformed versions of every directly supplied structural dimension")
        val base = _history(_payloads.head)
        val badreferences = Vector(null, base.reference.copy(recordId = null.asInstanceOf[InternalModelRecordId]),
          base.reference.copy(recordId = " \n".asInstanceOf[InternalModelRecordId]),
          base.reference.copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision]),
          base.reference.copy(recordRevision = (-1L).asInstanceOf[InternalModelRecordRevision]))
        val badpackages = Vector(null,
          _package_reference.copy(packageId = null.asInstanceOf[InternalModelPackageId]),
          _package_reference.copy(packageId = "INVALID".asInstanceOf[InternalModelPackageId]),
          _package_reference.copy(projectNamespace = null.asInstanceOf[InternalModelProjectToken]),
          _package_reference.copy(projectNamespace = "bad namespace".asInstanceOf[InternalModelProjectToken]),
          _package_reference.copy(projectId = null.asInstanceOf[InternalModelProjectToken]),
          _package_reference.copy(projectId = "".asInstanceOf[InternalModelProjectToken]))
        val badscopes = Vector(null, _scope.copy(componentIdentity = null), _scope.copy(componentIdentity = " "),
          _scope.copy(projectionContextIdentity = null), _scope.copy(projectionContextIdentity = "\n"),
          _scope.copy(selectedUseCaseElementIdentity = null), _scope.copy(selectedUseCaseElementIdentity = "\t"))
        val badartifacts = Vector(null, _projection.copy(artifactId = null.asInstanceOf[InternalModelArtifactId]),
          _projection.copy(artifactId = "bad id".asInstanceOf[InternalModelArtifactId]),
          _projection.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision]),
          _projection.copy(artifactRevision = (-1L).asInstanceOf[InternalModelArtifactRevision]), _projection.copy(role = null))
        val badsubjects = Vector(null, _subject.copy(subjectId = null.asInstanceOf[InternalModelRecordId]),
          _subject.copy(subjectId = " ".asInstanceOf[InternalModelRecordId]),
          _subject.copy(subjectRevision = 0L.asInstanceOf[InternalModelRecordRevision]),
          _subject.copy(subjectRevision = (-1L).asInstanceOf[InternalModelRecordRevision]), _subject.copy(artifacts = null)) ++
          badpackages.map(value => _subject.copy(packageReference = value)) ++
          badscopes.map(value => _subject.copy(scope = value)) ++
          badartifacts.map(value => _subject.copy(artifacts = Vector(value)))
        val badactors = Vector(null, _actor.copy(kind = null), _actor.copy(kind = " "),
          _actor.copy(identity = null), _actor.copy(identity = "\n"), _actor.copy(role = null), _actor.copy(role = "\t"))
        val badrecords = Vector(null, base.copy(occurredAt = null), base.copy(payload = null),
          base.copy(evidence = null), base.copy(evidence = Vector(null)),
          base.copy(evidence = Vector(_evidence.head.copy(kind = null))),
          base.copy(evidence = Vector(_evidence.head.copy(reference = null))),
          base.copy(evidence = Vector(_evidence.head.copy(kind = InternalModelEvidenceKind.Prompt)))) ++
          badreferences.map(value => base.copy(reference = value)) ++
          badsubjects.map(value => base.copy(subject = value)) ++ badactors.map(value => base.copy(actor = value))
        When("each malformed graph is structurally admitted")
        val results = badrecords.map(_validator.validate)
        Then("every malformed dimension is a structured invalid operation rather than a dereference or repair")
        results.foreach(_invalid)
      }

      "rejects malformed payload references and contradictory distinctness or projection selections" in {
        Given("wrong logical links, enum nulls and projection role or exact-selection contradictions")
        val review = _reference("review", 7L)
        val candidate = _reference("candidate", 11L)
        val diff = _reference("diff", 17L)
        val badreference = _reference("valid", 1L).copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision])
        val malformed = Vector(null, badreference)
        val invalidpayloads = malformed.flatMap { reference => Vector(
          Review(reference, candidate, diff, InternalModelRetainedReviewOutcome.Recorded),
          Review(review, reference, diff, InternalModelRetainedReviewOutcome.Recorded),
          Review(review, candidate, reference, InternalModelRetainedReviewOutcome.Recorded),
          Proposal(reference, _projection),
          Alternative(reference, candidate, InternalModelRetainedAlternativeDisposition.Considered),
          Alternative(review, reference, InternalModelRetainedAlternativeDisposition.Considered),
          Supersession(reference, candidate), Supersession(review, reference), Evidence(reference)
        ) } ++ Vector(
          Review(review, candidate, diff, null),
          Review(review, review, diff, InternalModelRetainedReviewOutcome.Recorded),
          Review(review, candidate, review, InternalModelRetainedReviewOutcome.Recorded),
          Review(review, candidate, candidate, InternalModelRetainedReviewOutcome.Recorded),
          Proposal(candidate, null), Proposal(candidate, _projection.copy(role = InternalModelArtifactRole.Validation)),
          Proposal(candidate, _projection.copy(artifactRevision = 0L.asInstanceOf[InternalModelArtifactRevision])),
          Proposal(candidate, _artifact("projection-other", 73L, InternalModelArtifactRole.Projection)),
          Proposal(candidate, _artifact("projection-main", 79L, InternalModelArtifactRole.Projection)),
          Alternative(review, candidate, null),
          Alternative(review, review, InternalModelRetainedAlternativeDisposition.Considered), Supersession(review, review)
        )
        val records = invalidpayloads.map(payload => _history(payload)) :+
          _history(Proposal(candidate, _projection), subject = _subject.copy(artifacts = Vector.empty))
        When("all contradictory payloads are admitted")
        val results = records.map(_validator.validate)
        Then("no malformed link or role/revision/selection mismatch becomes a historical admission")
        results.foreach(_invalid)
      }

      "admits artifact and evidence caps and rejects duplicates or the next element" in {
        Given("512 unique artifacts and 64 unique full evidence references plus invalid boundary variants")
        val artifacts = Vector.tabulate(512)(index => _artifact(s"historical-$index", index.toLong + 1L,
          InternalModelArtifactRole.Projection))
        val evidence = Vector.tabulate(64)(index => InternalModelRetainedEvidence(
          _reference("same logical evidence ID", index.toLong + 1L), InternalModelEvidenceKind.CallTree,
          InternalModelRetainedEvidencePayload.Omitted(InternalModelEvidenceOmission.Unavailable)))
        val base = _history(Evidence(_reference("evidence", 97L)), subject = _subject.copy(artifacts = artifacts), evidence = evidence)
        val invalid = Vector(
          base.copy(subject = base.subject.copy(artifacts = artifacts :+ _artifact("one-too-many", 701L, InternalModelArtifactRole.Projection))),
          base.copy(subject = base.subject.copy(artifacts = Vector(artifacts.head, artifacts.head))),
          base.copy(subject = base.subject.copy(artifacts = Vector(artifacts.head,
            artifacts.head.copy(artifactRevision = artifacts(1).artifactRevision)))),
          base.copy(evidence = evidence :+ evidence.head.copy(reference = _reference("same logical evidence ID", 65L))),
          base.copy(evidence = Vector(evidence.head, evidence.head)),
          base.copy(evidence = Vector(evidence.head, evidence.head.copy(kind = InternalModelEvidenceKind.ExternalEvidence)))
        )
        When("the capped account and its count or identity contradictions are admitted")
        val admitted = _validator.validate(base)
        val rejected = invalid.map(_validator.validate)
        Then("caps preserve exact order while duplicate artifact IDs or full evidence references reject")
        admitted.toOption shouldBe Some(base)
        rejected.foreach(_invalid)
      }

      "collects independent malformed dimensions without exposing actor or narrative text" in {
        Given("a missing reference, invalid actor fields and invalid timestamp beside a redacted narrative")
        val record = _history(_payloads.head).copy(reference = null, occurredAt = null,
          actor = InternalModelDecisionActor(null, " ", null))
        When("independent account dimensions are admitted together")
        val result = _validator.validate(record)
        Then("the structured failure includes all dimensions without any retained prose")
        _invalid(result)
        val diagnostics = result match {
          case Consequence.Failure(conclusion) => conclusion.causes.map(_.show)
          case Consequence.Success(_) => fail("expected a structured invalid operation")
        }
        Vector("reference", "actor-kind", "actor-identity", "actor-role", "occurred-at")
          .foreach(dimension => diagnostics.exists(_.contains(dimension)) shouldBe true)
        diagnostics.foreach(diagnostic => diagnostic should not include "秘匿済み")
      }
    }

    "explicit supplied supersession" which {
      "accepts exact historical endpoint pairs for each non-supersession family with changed subject revisions" in {
        forAll(_revision, _revision) { (previousrevision, nextrevision) =>
          Given("each non-supersession family with distinct full endpoint references and changed subject revision")
          val previoussubject = _subject.copy(subjectRevision = _reference("subject", previousrevision).recordRevision)
          val nextsubject = _subject.copy(subjectRevision = _reference("subject", if (previousrevision == Long.MaxValue) 1L else previousrevision + 1L).recordRevision)
          val triples = _payloads.filterNot(_.kind == InternalModelRetainedHistoryKind.Supersession).map { payload =>
            val previous = _history(payload, _reference("previous λ", previousrevision), previoussubject)
            val successor = _history(payload, _reference("successor λ", nextrevision), nextsubject)
            val relation = _history(Supersession(previous.reference, successor.reference))
            (relation, previous, successor)
          }
          When("the exact independently supplied relations are validated")
          val results = triples.map { case (relation, previous, successor) =>
            _validator.validateSupersession(relation, previous, successor)
          }
          Then("only the relation is admitted and neither historical endpoint is changed or rebased")
          results.map(_.toOption) shouldBe triples.map(value => Some(value._1))
          triples.foreach { case (_, previous, successor) =>
            previous.subject shouldBe previoussubject
            successor.subject shouldBe nextsubject
            previous.evidence shouldBe _evidence
            successor.evidence shouldBe _evidence
          }
        }
      }

      "rejects foreign package or scope, kind conflicts, self links and missing or malformed endpoints" in {
        Given("a valid explicit relation with independently contradictory endpoint dimensions")
        val previous = _history(_payloads.head, _reference("previous", 43L))
        val successor = _history(_payloads.head, _reference("successor", 61L),
          _subject.copy(subjectRevision = _reference("subject", 53L).recordRevision))
        val relation = _history(Supersession(previous.reference, successor.reference))
        val foreignpackage = _package_reference.copy(projectId = InternalModelProjectToken.from("OtherProject").fold(message => fail(message), value => value))
        val foreignscope = _scope.copy(componentIdentity = "another component")
        val invalid = Vector(
          (null, previous, successor), (relation, null, successor), (relation, previous, null),
          (relation.copy(payload = _payloads.head), previous, successor),
          (relation.copy(payload = Supersession(_reference("missing", 1L), successor.reference)), previous, successor),
          (relation.copy(payload = Supersession(previous.reference, _reference("missing", 1L))), previous, successor),
          (relation.copy(payload = Supersession(successor.reference, previous.reference)), previous, successor),
          (relation.copy(reference = previous.reference), previous, successor),
          (relation.copy(reference = successor.reference), previous, successor),
          (relation, previous, successor.copy(reference = previous.reference)),
          (relation, previous, successor.copy(payload = _payloads(1))),
          (relation, previous.copy(payload = _payloads(3)), successor.copy(payload = _payloads(3))),
          (relation, previous.copy(subject = _subject.copy(packageReference = foreignpackage)), successor),
          (relation, previous, successor.copy(subject = _subject.copy(packageReference = foreignpackage))),
          (relation.copy(subject = _subject.copy(packageReference = foreignpackage)), previous, successor),
          (relation, previous.copy(subject = _subject.copy(scope = foreignscope)), successor),
          (relation, previous, successor.copy(subject = _subject.copy(scope = foreignscope))),
          (relation.copy(subject = _subject.copy(scope = foreignscope)), previous, successor),
          (relation, previous.copy(actor = null), successor), (relation, previous, successor.copy(evidence = null))
        )
        When("each supplied relation is validated without fetching or inferring an endpoint")
        val results = invalid.map { case (record, before, after) => _validator.validateSupersession(record, before, after) }
        Then("every contradiction or missing endpoint fails instead of selecting history by order")
        results.foreach(_invalid)
      }
    }

    "body-free retention accounts" which {
      "retains exact tombstones for every kind and both retention actions" in {
        Given("typed reference/package/scope attribution, exact time, each kind and both retention actions")
        val tombstones = InternalModelRetainedHistoryKind.values.toVector.flatMap { kind =>
          InternalModelHistoryRetentionAction.values.toVector.map(action => InternalModelRetainedHistoryTombstone(
            _reference("  削除された記録 🧭  ", 127L), _package_reference, _scope, kind, action, _time))
        }
        When("the body-free accounts are independently admitted")
        val results = tombstones.map(_validator.validateTombstone)
        Then("only exact typed attribution, kind, action and time survive in each tombstone value")
        results.map(_.toOption) shouldBe tombstones.map(Some(_))
        results.flatMap(_.toOption).map(_.effectiveAt) shouldBe Vector.fill(tombstones.size)(_time)
      }

      "rejects malformed or null tombstone attribution, kind, action and time" in {
        Given("a body-free tombstone and every malformed retained attribution dimension")
        val base = InternalModelRetainedHistoryTombstone(_reference("expired record", 131L), _package_reference,
          _scope, InternalModelRetainedHistoryKind.Review, InternalModelHistoryRetentionAction.Expired, _time)
        val invalid = Vector(null, base.copy(reference = null),
          base.copy(reference = base.reference.copy(recordId = null.asInstanceOf[InternalModelRecordId])),
          base.copy(reference = base.reference.copy(recordRevision = 0L.asInstanceOf[InternalModelRecordRevision])),
          base.copy(packageReference = null),
          base.copy(packageReference = _package_reference.copy(packageId = "invalid".asInstanceOf[InternalModelPackageId])),
          base.copy(packageReference = _package_reference.copy(projectNamespace = null.asInstanceOf[InternalModelProjectToken])),
          base.copy(packageReference = _package_reference.copy(projectId = "bad project".asInstanceOf[InternalModelProjectToken])),
          base.copy(scope = null), base.copy(scope = _scope.copy(componentIdentity = null)),
          base.copy(scope = _scope.copy(projectionContextIdentity = " ")),
          base.copy(scope = _scope.copy(selectedUseCaseElementIdentity = null)),
          base.copy(kind = null), base.copy(action = null), base.copy(effectiveAt = null))
        When("each malformed tombstone is admitted")
        val results = invalid.map(_validator.validateTombstone)
        Then("body-free construction still requires valid typed attribution and explicit retention facts")
        results.foreach(_invalid)
      }
    }
  }

  private def _reference(recordid: String, revision: Long): InternalModelRecordReference = {
    InternalModelRecordReference(InternalModelRecordId.from(recordid).fold(message => fail(message), value => value),
      InternalModelRecordRevision.from(revision).fold(message => fail(message), value => value))
  }

  private def _artifact(artifactid: String, revision: Long, role: InternalModelArtifactRole): InternalModelArtifactReference = {
    InternalModelArtifactReference(InternalModelArtifactId.from(artifactid).fold(message => fail(message), value => value),
      InternalModelArtifactRevision.from(revision).fold(message => fail(message), value => value), role)
  }

  private def _history(
    payload: InternalModelRetainedHistoryPayload,
    reference: InternalModelRecordReference = _reference("  audit 記録 λ  ", 113L),
    subject: InternalModelReviewSubject = _subject,
    evidence: Vector[InternalModelRetainedEvidence] = _evidence
  ): InternalModelRetainedHistoryRecord = {
    InternalModelRetainedHistoryRecord(reference, subject, _actor, _time, payload, evidence)
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
