package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.file.Files
import java.time.Instant
import io.circe.{Json, JsonObject, Printer}
import org.goldenport.{Consequence, Conclusion}
import org.goldenport.id.UniversalId
import org.goldenport.observation.Taxonomy
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.cbdsupport.entity.{InternalModelHistoryEntry as HistoryEntity}

/**
 * Closed document and durable retention specifications; authored, not validated.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 */
final class InternalModelRetainedHistoryPersistenceSpec
    extends AnyWordSpec with Matchers with GivenWhenThen with ScalaCheckPropertyChecks {
  import InternalModelRetainedHistoryFixture.{value as fixtureValue, *}
  private val _codec = InternalModelRetainedHistoryCodec
  private val _logical_ids = Gen.nonEmptyListOf(Gen.oneOf(" 記録 ", "λ", "é", "e\u0301", "🧭", "\n中")).map(_.mkString)
  private val _revisions = Gen.frequency(2 -> Gen.const(Long.MaxValue), 8 -> Gen.choose(1L, 100000L))

  "Retained document codec" should {
    "the closed versioned graph" which {
      "C1 roundtrip every payload opinion disposition evidence and tombstone variant" in {
        Given("every history family, retained evidence case, review opinion, alternative disposition and retention action")
        val retained = payloads.map(payload => document(record = history(payload)))
        val removed = InternalModelRetainedHistoryKind.values.toVector.flatMap(kind =>
          InternalModelHistoryRetentionAction.values.toVector.map(action => InternalModelHistoryDocument(selection(),
            InternalModelHistoryDocumentState.Removed(InternalModelRetainedHistoryTombstone(selection().reference,
              packageReference, scope, kind, action, time)))))
        When("each complete document crosses the closed domain codec")
        val results = (retained ++ removed).map(document => _codec.decode(fixtureValue(_codec.encode(document))))
        Then("full typed values, independent revisions, timestamps, Unicode IDs and evidence order remain exact")
        results.map(_.toOption) shouldBe (retained ++ removed).map(Some(_))
        results.flatMap(_.toOption).take(retained.size).foreach { document =>
          document.content match {
            case InternalModelHistoryDocumentState.Retained(record, _) => record.evidence shouldBe evidence
            case _ => fail("Expected retained content")
          }
        }
      }

      "C2 preserve property-generated independent metadata narratives references and revisions" in {
        forAll(_logical_ids, _revisions, _revisions, Gen.alphaNumStr.suchThat(_.nonEmpty)) { (logicalid, auditrevision, subjectrevision, token) =>
          Given("independent positive logical/subject/artifact revisions, Unicode narrative and safe provider identity")
          val projectionvalue = projection.copy(artifactRevision = InternalModelArtifactRevision.from(73L).toOption.get)
          val recordsubject = subject.copy(subjectRevision = reference("subject", subjectrevision).recordRevision,
            artifacts = Vector(projectionvalue))
          val entries = Vector(InternalModelRetainedEvidence(reference(logicalid, auditrevision), InternalModelEvidenceKind.Narrative,
            InternalModelRetainedEvidencePayload.RedactedText(logicalid, reference("redaction " + logicalid, subjectrevision))),
            InternalModelRetainedEvidence(reference("metadata", 19L), InternalModelEvidenceKind.ProviderIdentity,
              InternalModelRetainedEvidencePayload.Identity("p" + token.take(100))))
          val record = history(InternalModelRetainedHistoryPayload.Proposal(reference("candidate " + logicalid, 11L), projectionvalue),
            reference(logicalid, auditrevision), recordsubject, entries.reverse)
          val expected = document(record = record)
          When("the generated account is encoded and independently decoded")
          val result = _codec.decode(fixtureValue(_codec.encode(expected)))
          Then("none of the independently declared values or encounter order is normalized")
          result.toOption shouldBe Some(expected)
          fixtureValue(result).selection.reference.recordRevision.value shouldBe auditrevision
          record.subject.subjectRevision.value shouldBe subjectrevision
          record.subject.artifacts.head.artifactRevision.value shouldBe 73L
        }
      }

      "C3 admit JSON presentation changes without changing exact selection" in {
        Given("one admitted document with a second whitespace and recursive key ordering")
        val expected = document()
        val json = _json(expected)
        val alternate = Printer.spaces2.print(_reverse(json))
        When("both presentations are decoded")
        val compact = _codec.decode(fixtureValue(_codec.encode(expected)))
        val pretty = _codec.decode(bytes(alternate))
        Then("semantic equality follows full typed attribution, not encoded bytes")
        pretty.toOption shouldBe compact.toOption
        fixtureValue(pretty).selection shouldBe expected.selection
      }

      "C4 reject missing extra null and wrong-type fields throughout every object graph" in {
        Given("all document variants and single-object closed-shape violations at every nesting level")
        val graphs = payloads.map(payload => _json(document(record = history(payload)))) :+
          _json(_removed(document(), InternalModelHistoryRetentionAction.Deleted))
        val invalid = graphs.flatMap { graph =>
          _object_variants(graph, objectvalue => objectvalue.toVector.flatMap { case (key, _) => Vector(
            Json.fromJsonObject(objectvalue.remove(key)), Json.fromJsonObject(objectvalue.add(key, Json.Null)),
            Json.fromJsonObject(objectvalue.add(key, Json.fromBoolean(false))))
          } :+ Json.fromJsonObject(objectvalue.add("unknown", Json.fromInt(1))))
        }
        When("each malformed graph is admitted")
        val results = invalid.map(json => _codec.decode(bytes(json.noSpaces)))
        Then("every entire malformed graph rejects with generic structured dimension diagnostics")
        results.foreach(_invalid)
      }

      "C5 reject unknown format version state payload evidence and enum tokens" in {
        Given("closed format/version and every state/payload/evidence vocabulary dimension")
        val record = history(payloads.head)
        val expected = document(record = record)
        val base = _json(expected)
        val variants = Vector(
          _replace(base, Vector("format"), Json.fromString("other")),
          _replace(base, Vector("schemaVersion"), Json.fromInt(2)),
          _replace(base, Vector("content", "kind"), Json.fromString("Retained")),
          _replace(base, Vector("content", "record", "payload", "kind"), Json.fromString("Raw")),
          _replace(base, Vector("content", "record", "payload", "outcome"), Json.fromString("Approved"))) ++
          _object_variants(base, objectvalue => {
            if (objectvalue.keys.toSet == Set("kind", "identity", "role")) Vector.empty
            else objectvalue("kind").toVector.map(_ =>
              Json.fromJsonObject(objectvalue.add("kind", Json.fromString("unknown-kind"))))
          }) ++
          _object_variants(base, objectvalue => objectvalue("reason").toVector.map(_ =>
            Json.fromJsonObject(objectvalue.add("reason", Json.fromString("Recovered"))))) ++
          Vector(_replace(_json(document(record = history(payloads(5)))), Vector("content", "record", "payload", "disposition"),
            Json.fromString("HumanApproved")),
            _replace(_json(_removed(document(), InternalModelHistoryRetentionAction.Deleted)), Vector("content", "tombstone", "action"),
              Json.fromString("Restored")))
        When("each unknown vocabulary token crosses the codec")
        val results = variants.map(json => _codec.decode(bytes(json.noSpaces)))
        Then("no migration or raw/freeform fallback is admitted")
        results.foreach(_invalid)
        Given("a nonblank descriptive actor kind, which is not a closed schema discriminator")
        val actor = record.actor.copy(kind = "unknown-kind")
        val descriptive = _replace(base, Vector("content", "record", "actor", "kind"), Json.fromString(actor.kind))
        When("the same unknown-kind token occurs only in historical actor attribution")
        val accepted = _codec.decode(bytes(descriptive.noSpaces))
        Then("the exact historical actor and the complete document are preserved")
        accepted.toOption shouldBe Some(expected.copy(content = InternalModelHistoryDocumentState.Retained(record.copy(actor = actor), time)))
        fixtureValue(accepted).content match {
          case InternalModelHistoryDocumentState.Retained(record, _) => record.actor shouldBe actor
          case _ => fail("Expected retained historical actor")
        }
      }

      "C6 reject noncanonical nonpositive fractional overflowing and wrong-type revisions" in {
        Given("every subject, artifact and logical record revision occurrence plus schemaVersion")
        val base = text(fixtureValue(_codec.encode(document())))
        val numerals = Vector("0", "-1", "1.0", "1e0", "1.5", "9223372036854775808", "\"1\"", "null", "true")
        val fields = Vector("recordRevision", "subjectRevision", "artifactRevision", "schemaVersion")
        val invalid = fields.flatMap(field => numerals.map(number =>
          ("\"" + field + "\":\\d+").r.replaceAllIn(base, _ => "\"" + field + "\":" + number)))
        When("noncanonical numeric encodings are supplied")
        val results = invalid.map(text => _codec.decode(bytes(text)))
        Then("only canonical positive integral Long revisions and version 1 survive")
        results.foreach(_invalid)
      }

      "C7 reject duplicate members at outer and nested graph boundaries" in {
        Given("one closed document and duplicate keys in each distinct nested object field family")
        val base = text(fixtureValue(_codec.encode(document())))
        val fields = Vector("format", "selection", "storageId", "reference", "recordId", "recordRevision", "packageId",
          "componentIdentity", "content", "kind", "subjectId", "actor", "identity", "occurredAt", "payload", "evidence", "reason", "value")
        val invalid = fields.map(field => base.replace("\"" + field + "\":", "\"" + field + "\":null,\"" + field + "\":"))
        When("the strict parser encounters each repeated member")
        val results = invalid.map(text => _codec.decode(bytes(text)))
        Then("duplicate keys reject without last-value interpretation")
        results.foreach(_invalid)
      }

      "C8 reject invalid instants and unrepresentable retention calculations" in {
        Given("invalid ISO timestamps, timestamp primitives and a retainedAt too large for thirty-day age")
        val base = _json(document())
        val invalid = Vector("occurredAt", "retainedAt").flatMap { field =>
          val path = if (field == "retainedAt") Vector("content", field) else Vector("content", "record", field)
          Vector(Json.fromString("not-an-instant"), Json.fromLong(123), Json.Null).map(value => _replace(base, path, value))
        } :+ _replace(base, Vector("content", "retainedAt"), Json.fromString(Instant.MAX.toString))
        When("time admission is attempted")
        val results = invalid.map(json => _codec.decode(bytes(json.noSpaces)))
        val encodefailure = _codec.encode(document(retainedAt = Instant.MAX))
        Then("parsing and retention overflow remain structured failures")
        results.foreach(_invalid)
        _invalid(encodefailure)
      }

      "C9 enforce strict UTF-8 and the one-megabyte input and output boundary" in {
        Given("invalid UTF-8/BOM bytes, unpaired UTF-16 surrogates, an oversized byte input and an oversized Unicode logical ID")
        val inputvalues = Vector(Vector(0xc3.toByte, 0x28.toByte), bytes("\uFEFF{}"), Vector.fill(1048577)(32.toByte))
        val oversized = document(record = history(recordReference = reference("λ" * 600000, 101L)))
        val surrogatevalues = Vector(0xd800.toChar.toString, 0xdc00.toChar.toString)
        val jsontext = _replace(_json(document()), Vector("selection", "scope", "componentIdentity"),
          Json.fromString("INVALID_SURROGATE_MARKER")).noSpaces
        val escapedvalues = Vector("\\ud800", "\\udc00").map(value => jsontext.replace("INVALID_SURROGATE_MARKER", value))
        When("the bounded codec receives or emits those documents")
        val results = inputvalues.map(_codec.decode)
        val encoded = _codec.encode(oversized)
        val malformedoutput = surrogatevalues.map(value => _codec.encode(document(record = history(
          recordSubject = subject.copy(scope = scope.copy(componentIdentity = value))))))
        val malformedtext = surrogatevalues.map(value => _codec.decodeText(jsontext.replace("INVALID_SURROGATE_MARKER", value)))
        val escapedresults = escapedvalues.map(value => _codec.decode(bytes(value)))
        Then("nothing is parsed or persisted beyond the bound and rejected content is absent from diagnostics")
        results.foreach(_invalid)
        _invalid(encoded)
        malformedoutput.foreach(_invalid)
        malformedtext.foreach(_invalid)
        escapedresults.foreach(_invalid)
        _diagnostic(encoded) should not include ("λλλλ")
      }

      "C10 reject malformed domain graphs and selection cross-binding before encoding" in {
        Given("null graphs, contradictory record/tombstone selection, duplicate evidence and artifact overcount")
        val base = document()
        val record = history()
        val invalid = Vector(null, base.copy(selection = null), base.copy(content = null),
          base.copy(selection = base.selection.copy(reference = reference("other", 101L))),
          base.copy(selection = base.selection.copy(scope = scope.copy(componentIdentity = "other"))),
          document(record = record.copy(actor = null)), document(record = record.copy(evidence = null)),
          document(record = record.copy(evidence = Vector.fill(65)(evidence.head))),
          document(record = record.copy(subject = subject.copy(artifacts = Vector.fill(513)(projection)))))
        When("encoding is requested for each invalid graph")
        val results = invalid.map(_codec.encode)
        Then("constructors alone do not admit a document or incomplete graph")
        results.foreach(_invalid)
      }

      "C11 admit only independently supplied canonical lowercase UUID storage identities" in {
        Given("canonical UUIDs and noncanonical, null, uppercase or non-UUID tokens")
        val bad = Vector(null, "", " 00000000-0000-0000-0000-000000000001", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "logical-record")
        When("storage identities are constructed without normalization")
        val results = bad.map(InternalModelHistoryStorageId.from)
        val admitted = InternalModelHistoryStorageId.from("ffffffff-ffff-ffff-ffff-ffffffffffff")
        Then("exact canonical UUID is retained and every other syntax rejects")
        results.foreach(_invalid)
        fixtureValue(admitted).value shouldBe "ffffffff-ffff-ffff-ffff-ffffffffffff"
      }

      "C12 bridge canonical UUID selections injectively without changing logical attribution" in {
        forAll(Gen.uuid, Gen.uuid) { (firstuuid, seconduuid) =>
          Given("two independently supplied canonical lowercase UUIDs and complete admitted selections")
          val firststorage = fixtureValue(InternalModelHistoryStorageId.from(firstuuid.toString))
          val secondstorage = fixtureValue(InternalModelHistoryStorageId.from(seconduuid.toString))
          val firstselection = selection().copy(storageId = firststorage)
          val secondselection = selection().copy(storageId = secondstorage)
          val logicalselection = firstselection.copy(reference = reference("another logical record", 211L))
          val expected = Vector(firstselection, secondselection).map(selected => document().copy(selection = selected))
          When("the selections cross the EntityId bridge and roundtrip their physical IDs and stored documents")
          val results = Vector(firstselection, secondselection).map(InternalModelRetainedHistoryStore.entityId)
          val ids = results.map(fixtureValue)
          val restored = ids.map(id => EntityId.parse(id.value))
          val repeated = InternalModelRetainedHistoryStore.entityId(firstselection)
          val logical = InternalModelRetainedHistoryStore.entityId(logicalselection)
          val decoded = expected.map(document => _codec.decode(fixtureValue(_codec.encode(document))))
          Then("all 32 hexadecimal digits survive with the generated collection and stable timestamp")
          results.foreach(_.isSuccess shouldBe true)
          ids.map(_.entropy) shouldBe Vector(firstuuid, seconduuid).map(uuid => Some(uuid.toString.replace("-", "")))
          ids.foreach { id =>
            id.entropy.get should fullyMatch regex "[0-9a-f]{32}"
            id.collection shouldBe HistoryEntity.collectionId
            id.major shouldBe HistoryEntity.collectionId.major
            id.minor shouldBe HistoryEntity.collectionId.minor
            id.timestamp shouldBe Some(UniversalId.StableTimestamp)
          }
          And("physical identity is equal exactly when the supplied UUIDs are equal and remains stable across logical references")
          restored.map(_.toOption) shouldBe ids.map(Some(_))
          (ids.head == ids(1)) shouldBe (firstuuid == seconduuid)
          repeated.toOption shouldBe Some(ids.head)
          logical.toOption shouldBe Some(ids.head)
          And("the codec retains each exact canonical hyphenated UUID and full logical selection")
          decoded.map(_.toOption) shouldBe expected.map(Some(_))
          decoded.map(result => fixtureValue(result).selection) shouldBe Vector(firstselection, secondselection)
          decoded.map(result => fixtureValue(result).selection.storageId.value) shouldBe Vector(firstuuid.toString, seconduuid.toString)
        }
      }
    }
  }

  "Optional Entity history persistence" should {
    "immutable durable exact selections" which {
      "P1 reopen the real generated Entity through fresh SQLite and UnitOfWork spaces" in {
        Given("a generated Entity written to a task-private SQLite file")
        withDatabase { path =>
          val first = database(path)
          val expected = document(retainedAt = time.plusSeconds(999999))
          When("one claim is followed by a separately constructed context and generated Entity decoder")
          val written = first.store.record(expected, None)
          first.close()
          val second = database(path)
          try {
            val loaded = second.store.read(expected.selection)
            val entity = second.entity(expected.selection).get
            Then("the independent read reconstructs the typed record using server storage time")
            fixtureValue(written) shouldBe InternalModelHistoryResult.Retained(expected.selection, history(), time)
            loaded.toOption shouldBe written.toOption
            entity.id shouldBe fixtureValue(InternalModelRetainedHistoryStore.entityId(expected.selection))
            fixtureValue(_codec.decodeText(entity.history_document.value.value)) shouldBe document()
            Files.size(path) should be > 0L
            (first.context eq second.context) shouldBe false
            (first.component.entitySpace eq second.component.entitySpace) shouldBe false
          } finally second.close()
        }
      }

      "P2 reject occupied keys for identical and different content without returning stored data" in {
        Given("one occupied UUID with an immutable retained document")
        withDatabase { path =>
          val db = database(path)
          try {
            val expected = document()
            fixtureValue(db.store.record(expected, None))
            When("same-content and different-logical-record claims target the occupied UUID")
            val same = db.store.record(expected, None)
            val different = db.store.record(document(record = history(recordReference = reference("other record", 211L))), None)
            Then("both claims reject and the original account is unchanged")
            _invalid(same)
            _invalid(different)
            _diagnostic(different) should not include ("provider:one")
            fixtureValue(db.store.read(expected.selection)) shouldBe InternalModelHistoryResult.Retained(expected.selection, history(), time)
          } finally db.close()
        }
      }

      "P3 reject mismatched full selections without disclosing or replacing the document" in {
        Given("one retained record and same-storage-ID selectors with wrong reference, package or scope")
        withDatabase { path =>
          val db = database(path)
          try {
            val expected = document()
            fixtureValue(db.store.record(expected, None))
            val selections = Vector(expected.selection.copy(reference = reference("other", 101L)),
              expected.selection.copy(reference = expected.selection.reference.copy(recordRevision = reference("revision", 102L).recordRevision)),
              expected.selection.copy(packageReference = packageReference.copy(projectId = InternalModelProjectToken.from("other").toOption.get)),
              expected.selection.copy(scope = scope.copy(componentIdentity = "other")))
            When("each mismatched exact selection is read or maintained")
            val results = selections.flatMap(selection => Vector(db.store.read(selection), db.store.delete(selection), db.store.expire(selection)))
            Then("every mismatch rejects generically and the actual selection still returns the original account")
            results.foreach(_invalid)
            results.foreach(result => _diagnostic(result) should not include ("provider:one"))
            fixtureValue(db.store.read(expected.selection)) shouldBe InternalModelHistoryResult.Retained(expected.selection, history(), time)
          } finally db.close()
        }
      }

      "P4 reject a stale generated Entity revision instead of overwriting a concurrent replacement" in {
        Given("two snapshots of one generated Entity at the same revision")
        withDatabase { path =>
          val first = database(path)
          val second = database(path)
          try {
            val expected = document()
            fixtureValue(first.store.record(expected, None))
            val before = first.snapshot(expected.selection)
            val stale = second.snapshot(expected.selection)
            When("the first snapshot installs a tombstone and the stale snapshot attempts another replacement")
            val committed = first.replace(expected.selection, _removed(expected, InternalModelHistoryRetentionAction.Deleted), before)
            val conflicting = second.replace(expected.selection, _removed(expected, InternalModelHistoryRetentionAction.Expired), stale)
            val loaded = second.store.read(expected.selection)
            Then("only the first revision transition survives and stale replacement fails")
            committed.isSuccess shouldBe true
            conflicting.isSuccess shouldBe false
            fixtureValue(loaded) shouldBe InternalModelHistoryResult.Removed(_tombstone(expected, InternalModelHistoryRetentionAction.Deleted))
          } finally { first.close(); second.close() }
        }
      }

      "P5 return explicit absence and reject invalid selections before Entity work" in {
        Given("an empty SQLite store and malformed selectors")
        withDatabase { path =>
          val db = database(path)
          try {
            When("an exact missing selection and malformed graphs are read or maintained")
            val missing = Vector(db.store.read(selection()), db.store.delete(selection()), db.store.expire(selection()))
            val invalid = Vector(db.store.read(null), db.store.delete(null), db.store.expire(null),
              db.store.read(selection().copy(storageId = "invalid".asInstanceOf[InternalModelHistoryStorageId])))
            Then("absence is typed Missing while malformed selectors are contract failures")
            missing.map(_.toOption) shouldBe Vector.fill(3)(Some(InternalModelHistoryResult.Missing))
            invalid.foreach(_invalid)
          } finally db.close()
        }
      }
    }

    "server-time retention and minimal durable tombstones" which {
      "P6 hide due bodies at the thirty-day boundary without a viewer write" in {
        Given("a record retained at server time and its exact generated Entity revision")
        withDatabase { path =>
          val db = database(path)
          try {
            val expected = document(retainedAt = time.plusSeconds(999999))
            fixtureValue(db.store.record(expected, None))
            val revision = db.snapshot(expected.selection).revision
            val expiry = time.plusSeconds(30L * 24L * 60L * 60L)
            When("viewer reads occur just before, exactly at and after thirty days")
            db.clock.set(expiry.minusNanos(1L))
            val before = db.store.read(expected.selection)
            db.clock.set(expiry)
            val at = db.store.read(expected.selection)
            db.clock.set(expiry.plusNanos(1L))
            val after = db.store.read(expected.selection)
            val afterrevision = db.snapshot(expected.selection).revision
            Then("server retainedAt controls expiry, bodies disappear at the boundary and no read mutates")
            fixtureValue(before) shouldBe InternalModelHistoryResult.Retained(expected.selection, history(), time)
            fixtureValue(at) shouldBe InternalModelHistoryResult.ExpiryRequired(expected.selection.reference, packageReference, scope,
              InternalModelRetainedHistoryKind.Evidence, expiry)
            after.toOption shouldBe at.toOption
            afterrevision shouldBe revision
          } finally db.close()
        }
      }

      "P7 reject early expiry and persist one minimal expiry tombstone at the boundary" in {
        Given("a live retained account with actor and narrative/evidence bodies")
        withDatabase { path =>
          val db = database(path)
          val expected = document()
          fixtureValue(db.store.record(expected, None))
          When("maintenance is requested before expiry and then exactly at server expiry")
          val early = db.store.expire(expected.selection)
          db.clock.set(time.plusSeconds(2592000L))
          val expired = db.store.expire(expected.selection)
          db.close()
          val reopened = database(path, time.plusSeconds(2592001L))
          try {
            val reread = reopened.store.read(expected.selection)
            val storedtext = reopened.entity(expected.selection).get.history_document.value.value
            Then("early maintenance fails and only the exact durable body-free tombstone remains")
            _invalid(early)
            fixtureValue(expired) shouldBe InternalModelHistoryResult.Removed(_tombstone(expected,
              InternalModelHistoryRetentionAction.Expired, time.plusSeconds(2592000L)))
            reread.toOption shouldBe expired.toOption
            _minimal(storedtext)
          } finally reopened.close()
        }
      }

      "P8 delete immediately and preserve the same tombstone through repeated maintenance" in {
        Given("a live record in a real generated Entity")
        withDatabase { path =>
          val db = database(path)
          val expected = document()
          fixtureValue(db.store.record(expected, None))
          When("delete replaces its whole document and later expiry/delete are repeated")
          val deleted = db.store.delete(expected.selection)
          db.clock.set(time.plusSeconds(777L))
          val repeated = Vector(db.store.expire(expected.selection), db.store.delete(expected.selection))
          db.close()
          val reopened = database(path, time.plusSeconds(999L))
          try {
            val reread = reopened.store.read(expected.selection)
            val storedtext = reopened.entity(expected.selection).get.history_document.value.value
            Then("the existing action/time remain exact and the independent reread has no body evidence or actor")
            fixtureValue(deleted) shouldBe InternalModelHistoryResult.Removed(_tombstone(expected, InternalModelHistoryRetentionAction.Deleted))
            repeated.map(_.toOption) shouldBe Vector.fill(2)(deleted.toOption)
            reread.toOption shouldBe deleted.toOption
            _minimal(storedtext)
          } finally reopened.close()
        }
      }

      "P9 prohibit resurrection while allowing a new independent storage identity" in {
        Given("a deleted storage UUID and the same logical record reference")
        withDatabase { path =>
          val db = database(path)
          try {
            val expected = document()
            fixtureValue(db.store.record(expected, None)); fixtureValue(db.store.delete(expected.selection))
            When("a record claim targets the tombstone and then a genuinely new explicit UUID")
            val occupied = db.store.record(expected, None)
            val fresh = document(2L)
            val created = db.store.record(fresh, None)
            Then("the occupied UUID remains removed and the new UUID is returned without logical-ID uniqueness inference")
            _invalid(occupied)
            fixtureValue(created) shouldBe InternalModelHistoryResult.Retained(fresh.selection, history(), time)
            fixtureValue(db.store.read(expected.selection)) shouldBe InternalModelHistoryResult.Removed(_tombstone(expected, InternalModelHistoryRetentionAction.Deleted))
          } finally db.close()
        }
      }
    }

    "explicit supersession and backend failure" which {
      "P10 admit only the exact supplied live endpoints and retain the historical relation after deletion" in {
        Given("two independently selected live Evidence accounts with changed subject revisions")
        withDatabase { path =>
          val db = database(path)
          try {
            val previous = document(1L, history(recordReference = reference("previous", 43L)))
            val successor = document(2L, history(recordReference = reference("successor", 61L),
              recordSubject = subject.copy(subjectRevision = reference("subject", 53L).recordRevision)))
            val relation = document(3L, history(InternalModelRetainedHistoryPayload.Supersession(previous.selection.reference,
              successor.selection.reference), reference("relation", 67L)))
            fixtureValue(db.store.record(previous, None)); fixtureValue(db.store.record(successor, None))
            When("the relation names both exact endpoint selections and one endpoint is later deleted")
            val result = db.store.record(relation, Some(InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection)))
            fixtureValue(db.store.delete(previous.selection))
            val later = db.store.read(relation.selection)
            Then("the accepted historical relation stays exact and does not rewrite endpoint current state")
            fixtureValue(result) shouldBe InternalModelHistoryResult.Retained(relation.selection,
              history(InternalModelRetainedHistoryPayload.Supersession(previous.selection.reference, successor.selection.reference), reference("relation", 67L)), time)
            later.toOption shouldBe result.toOption
          } finally db.close()
        }
      }

      "P11 reject missing expired removed mismatched and wrong-kind supersession endpoints" in {
        Given("explicit endpoint variants, including one different history family")
        withDatabase { path =>
          val db = database(path)
          try {
            val previous = document(1L, history(recordReference = reference("previous", 43L)))
            val successor = document(2L, history(recordReference = reference("successor", 61L)))
            val wrongkind = document(4L, history(payloads.head, reference("review endpoint", 71L)))
            fixtureValue(db.store.record(previous, None)); fixtureValue(db.store.record(successor, None)); fixtureValue(db.store.record(wrongkind, None))
            val relation = document(3L, history(InternalModelRetainedHistoryPayload.Supersession(previous.selection.reference,
              successor.selection.reference), reference("relation", 67L)))
            val pairs = Vector(InternalModelHistorySupersessionEndpoints(previous.selection, selection(99L)),
              InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection.copy(reference = reference("wrong", 61L))),
              InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection.copy(packageReference =
                packageReference.copy(projectId = InternalModelProjectToken.from("other").toOption.get))),
              InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection.copy(scope = scope.copy(componentIdentity = "other"))),
              InternalModelHistorySupersessionEndpoints(previous.selection, wrongkind.selection))
            When("each invalid exact endpoint relation, absent endpoint pair, expired pair and removed pair is requested")
            val initial = pairs.map(pair => db.store.record(relation, Some(pair))) :+ db.store.record(relation, None)
            db.clock.set(time.plusSeconds(2592000L))
            val expired = db.store.record(relation, Some(InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection)))
            db.clock.set(time)
            fixtureValue(db.store.delete(successor.selection))
            val removed = db.store.record(relation, Some(InternalModelHistorySupersessionEndpoints(previous.selection, successor.selection)))
            Then("every invalid relation rejects before claim and the relation Entity remains absent")
            (initial ++ Vector(expired, removed)).foreach(_invalid)
            db.entity(relation.selection) shouldBe None
          } finally db.close()
        }
      }

      "P12 report backend failure generically instead of absence or false persistence" in {
        Given("a task-private invalid SQLite database containing a raw failure sentinel")
        withDatabase { path =>
          Files.write(path, bytes("RAW-PROMPT-STORAGE-FAILURE-SENTINEL").toArray)
          val db = database(path)
          try {
            When("read, record, expire and delete reach that failed backend")
            val results = Vector(db.store.read(selection()), db.store.record(document(), None),
              db.store.expire(selection()), db.store.delete(selection()))
            Then("each returns structured generic storage failure without body text or success/absence")
            results.foreach { result =>
              result.isSuccess shouldBe false
              result match {
                case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe
                  Taxonomy.dataStoreUnavailable
                case _ => fail("Expected storage failure")
              }
              _diagnostic(result) should not include ("RAW-PROMPT-STORAGE-FAILURE-SENTINEL")
            }
          } finally db.close()
        }
      }
    }
  }

  private def _json(document: InternalModelHistoryDocument): Json =
    InternalModelContinuationFixture.json(fixtureValue(_codec.encode(document)))
  private def _reverse(json: Json): Json = json.arrayOrObject(json,
    values => Json.fromValues(values.map(_reverse)), objectvalue => Json.fromJsonObject(JsonObject.fromIterable(
      objectvalue.toVector.reverse.map { case (key, value) => key -> _reverse(value) })))
  private def _replace(json: Json, path: Vector[String], replacement: Json): Json = {
    if (path.isEmpty) replacement
    else json.mapObject(root => root.add(path.head, _replace(root(path.head).get, path.tail, replacement)))
  }
  private def _object_variants(json: Json, change: JsonObject => Vector[Json]): Vector[Json] = {
    json.asObject match {
      case Some(root) => change(root) ++ root.toVector.flatMap { case (key, child) =>
        _object_variants(child, change).map(value => Json.fromJsonObject(root.add(key, value)))
      }
      case None => json.asArray.toVector.flatMap(values => values.zipWithIndex.flatMap { case (child, index) =>
        _object_variants(child, change).map(value => Json.fromValues(values.updated(index, value)))
      })
    }
  }
  private def _tombstone(document: InternalModelHistoryDocument, action: InternalModelHistoryRetentionAction,
    effectiveat: Instant = time): InternalModelRetainedHistoryTombstone =
    InternalModelRetainedHistoryTombstone(document.selection.reference, document.selection.packageReference,
      document.selection.scope, InternalModelRetainedHistoryKind.Evidence, action, effectiveat)
  private def _removed(document: InternalModelHistoryDocument, action: InternalModelHistoryRetentionAction): InternalModelHistoryDocument =
    document.copy(content = InternalModelHistoryDocumentState.Removed(_tombstone(document, action)))
  private def _minimal(text: String): Unit = {
    Vector("\"actor\"", "\"evidence\"", "\"payload\"", "provider:one", "秘匿済み", principalId).foreach(token =>
      text should not include (token))
    text should include ("\"tombstone\"")
  }
  private def _invalid[A](result: Consequence[A]): Unit = result match {
    case Consequence.Failure(conclusion) => conclusion.observation.taxonomy shouldBe Conclusion.operationInvalid("expected").observation.taxonomy
    case _ => fail("Expected structured invalid operation")
  }
  private def _diagnostic[A](result: Consequence[A]): String = result match {
    case Consequence.Failure(conclusion) => conclusion.show + conclusion.display
    case _ => ""
  }
}
