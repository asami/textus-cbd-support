package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.{ByteBuffer, CharBuffer}
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.time.Instant
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser
import org.goldenport.Consequence

/**
 * Closed, bounded document serialization; presentation never selects identity or authority.
 *
 * @since   Oct.  3, 2026
 * @version Oct.  3, 2026
 */
private[runtime] opaque type InternalModelHistoryStorageId = String

private[runtime] object InternalModelHistoryStorageId {
  private val _pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  def from(value: String): Consequence[InternalModelHistoryStorageId] = {
    if (value != null && _pattern.matches(value)) Consequence.success(value)
    else Consequence.operationInvalid("internal-model history invalid: storage-id")
  }

  extension (identity: InternalModelHistoryStorageId) def value: String = identity
}

private[runtime] final case class InternalModelHistorySelection(
  storageId: InternalModelHistoryStorageId,
  reference: InternalModelRecordReference,
  packageReference: InternalModelPackageReference,
  scope: InternalModelSemanticScope
)

private[runtime] enum InternalModelHistoryDocumentState {
  case Retained(record: InternalModelRetainedHistoryRecord, retainedAt: Instant)
  case Removed(tombstone: InternalModelRetainedHistoryTombstone)
}

private[runtime] final case class InternalModelHistoryDocument(
  selection: InternalModelHistorySelection,
  content: InternalModelHistoryDocumentState
)

private[runtime] object InternalModelRetainedHistoryCodec {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpaces
  private val _format = "textus.internal-model.retained-history"
  private val _limit = 1048576
  private val _retention_seconds = 30L * 24L * 60L * 60L

  def validateSelection(selection: InternalModelHistorySelection): Consequence[InternalModelHistorySelection] = {
    if (selection == null) _invalid("selection")
    else {
      val tombstone = InternalModelRetainedHistoryTombstone(selection.reference, selection.packageReference,
        selection.scope, InternalModelRetainedHistoryKind.Evidence, InternalModelHistoryRetentionAction.Deleted, Instant.EPOCH)
      InternalModelHistoryStorageId.from(selection.storageId.value)
        .zip(InternalModelRetainedHistoryValidator.validateTombstone(tombstone)).map(_ => selection)
    }
  }

  def expiresAt(retainedAt: Instant): Consequence[Instant] = {
    if (retainedAt == null) _invalid("retained-at")
    else _boundary("retention-time")(Consequence.success(retainedAt.plusSeconds(_retention_seconds)))
  }

  def validate(document: InternalModelHistoryDocument): Consequence[InternalModelHistoryDocument] = {
    if (document == null || document.content == null) _invalid("document")
    else validateSelection(document.selection).flatMap { selection =>
      document.content match {
        case InternalModelHistoryDocumentState.Retained(record, retainedat) =>
          InternalModelRetainedHistoryValidator.validate(record).zip(expiresAt(retainedat)).flatMap { _ =>
            _check(record.reference == selection.reference && record.subject.packageReference == selection.packageReference &&
              record.subject.scope == selection.scope, "record-selection").map(_ => document)
          }
        case InternalModelHistoryDocumentState.Removed(tombstone) =>
          InternalModelRetainedHistoryValidator.validateTombstone(tombstone).flatMap { _ =>
            _check(tombstone.reference == selection.reference && tombstone.packageReference == selection.packageReference &&
              tombstone.scope == selection.scope, "tombstone-selection").map(_ => document)
          }
      }
    }
  }

  def encode(document: InternalModelHistoryDocument): Consequence[Vector[Byte]] = {
    validate(document).flatMap { admitted =>
      _boundary("document-encoding") {
        val json = Json.obj("format" -> Json.fromString(_format), "schemaVersion" -> Json.fromInt(1),
          "selection" -> _selection_json(admitted.selection), "content" -> _content_json(admitted.content))
        _unicode_document(admitted).flatMap { _ =>
          _utf8_output(_printer.print(json)).flatMap { bytes =>
            _check(bytes.size <= _limit, "document-byte-limit").map(_ => bytes)
          }
        }
      }
    }
  }

  def decode(bytes: Vector[Byte]): Consequence[InternalModelHistoryDocument] = {
    if (bytes == null || bytes.size > _limit) _invalid("document-byte-limit")
    else _boundary("document-json") {
      val parsed = for {
        text <- _utf8_input(bytes)
        _ <- Either.cond(!text.startsWith("\uFEFF"), (), "utf8-bom")
        json <- _parser.parse(text).left.map(_ => "strict-json")
        _ <- _unicode_json(json)
        root <- _object(json, Set("format", "schemaVersion", "selection", "content"))
        format <- _string(root, "format")
        _ <- Either.cond(format == _format, (), "format")
        version <- _long(root, "schemaVersion")
        _ <- Either.cond(version == 1L, (), "schema-version")
        selection <- _selection(root("selection").get)
        content <- _content(root("content").get)
      } yield InternalModelHistoryDocument(selection, content)
      parsed.fold(_invalid, validate)
    }
  }

  def decodeText(text: String): Consequence[InternalModelHistoryDocument] = {
    if (text == null || text.length > _limit) _invalid("document-byte-limit")
    else _utf8_output(text).flatMap(decode)
  }

  private def _selection_json(selection: InternalModelHistorySelection): Json = Json.obj(
    "storageId" -> Json.fromString(selection.storageId.value), "reference" -> _record_reference_json(selection.reference),
    "packageReference" -> _package_reference_json(selection.packageReference), "scope" -> _scope_json(selection.scope)
  )

  private def _content_json(content: InternalModelHistoryDocumentState): Json = content match {
    case InternalModelHistoryDocumentState.Retained(record, retainedat) =>
      Json.obj("kind" -> Json.fromString("retained"), "record" -> _record_json(record),
        "retainedAt" -> Json.fromString(retainedat.toString))
    case InternalModelHistoryDocumentState.Removed(tombstone) =>
      Json.obj("kind" -> Json.fromString("removed"), "tombstone" -> _tombstone_json(tombstone))
  }

  private def _record_json(record: InternalModelRetainedHistoryRecord): Json = Json.obj(
    "reference" -> _record_reference_json(record.reference), "subject" -> _subject_json(record.subject),
    "actor" -> Json.obj("kind" -> Json.fromString(record.actor.kind), "identity" -> Json.fromString(record.actor.identity),
      "role" -> Json.fromString(record.actor.role)), "occurredAt" -> Json.fromString(record.occurredAt.toString),
    "payload" -> _payload_json(record.payload), "evidence" -> Json.fromValues(record.evidence.map(_evidence_json))
  )

  private def _subject_json(subject: InternalModelReviewSubject): Json = Json.obj(
    "subjectId" -> Json.fromString(subject.subjectId.value), "subjectRevision" -> Json.fromLong(subject.subjectRevision.value),
    "packageReference" -> _package_reference_json(subject.packageReference), "scope" -> _scope_json(subject.scope),
    "artifacts" -> Json.fromValues(subject.artifacts.map(_artifact_reference_json))
  )

  private def _scope_json(scope: InternalModelSemanticScope): Json = Json.obj(
    "componentIdentity" -> Json.fromString(scope.componentIdentity),
    "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity),
    "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity)
  )

  private def _payload_json(payload: InternalModelRetainedHistoryPayload): Json = {
    import InternalModelRetainedHistoryPayload.*
    val fields = payload match {
      case Review(review, candidate, diff, outcome) => Vector("reviewReference" -> _record_reference_json(review),
        "candidateReference" -> _record_reference_json(candidate), "semanticDiffReference" -> _record_reference_json(diff),
        "outcome" -> Json.fromString(outcome.toString))
      case Proposal(candidate, projection) => Vector("candidateReference" -> _record_reference_json(candidate),
        "projectionArtifactReference" -> _artifact_reference_json(projection))
      case Alternative(proposal, alternative, disposition) => Vector("proposalReference" -> _record_reference_json(proposal),
        "alternativeReference" -> _record_reference_json(alternative), "disposition" -> Json.fromString(disposition.toString))
      case Supersession(previous, successor) => Vector("previousReference" -> _record_reference_json(previous),
        "successorReference" -> _record_reference_json(successor))
      case Evidence(reference) => Vector("evidenceReference" -> _record_reference_json(reference))
    }
    Json.obj((Vector("kind" -> Json.fromString(payload.kind.toString)) ++ fields)*)
  }

  private def _evidence_json(evidence: InternalModelRetainedEvidence): Json = {
    val payload = evidence.payload match {
      case InternalModelRetainedEvidencePayload.Identity(value) =>
        Json.obj("kind" -> Json.fromString("Identity"), "value" -> Json.fromString(value))
      case InternalModelRetainedEvidencePayload.RedactedText(value, reference) =>
        Json.obj("kind" -> Json.fromString("RedactedText"), "value" -> Json.fromString(value),
          "redactionReference" -> _record_reference_json(reference))
      case InternalModelRetainedEvidencePayload.Omitted(reason) =>
        Json.obj("kind" -> Json.fromString("Omitted"), "reason" -> Json.fromString(reason.toString))
    }
    Json.obj("reference" -> _record_reference_json(evidence.reference), "kind" -> Json.fromString(evidence.kind.toString),
      "payload" -> payload)
  }

  private def _tombstone_json(tombstone: InternalModelRetainedHistoryTombstone): Json = Json.obj(
    "reference" -> _record_reference_json(tombstone.reference), "packageReference" -> _package_reference_json(tombstone.packageReference),
    "scope" -> _scope_json(tombstone.scope), "kind" -> Json.fromString(tombstone.kind.toString),
    "action" -> Json.fromString(tombstone.action.toString), "effectiveAt" -> Json.fromString(tombstone.effectiveAt.toString)
  )

  private def _reference_json(bytes: Vector[Byte]): Json = _parser.parse(new String(bytes.toArray, StandardCharsets.UTF_8)).toOption.get
  private def _record_reference_json(reference: InternalModelRecordReference): Json =
    _reference_json(InternalModelTypedControlCodec.encodeRecordReference(reference))
  private def _artifact_reference_json(reference: InternalModelArtifactReference): Json =
    _reference_json(InternalModelTypedControlCodec.encodeArtifactReference(reference))
  private def _package_reference_json(reference: InternalModelPackageReference): Json =
    _reference_json(InternalModelTypedControlCodec.encodePackageReference(reference))

  private def _selection(json: Json): Either[String, InternalModelHistorySelection] = for {
    root <- _object(json, Set("storageId", "reference", "packageReference", "scope"))
    storageid <- _string(root, "storageId").flatMap(value => InternalModelHistoryStorageId.from(value).toOption.toRight("storage-id"))
    reference <- _record_reference(root("reference").get)
    packagevalue <- _package_reference(root("packageReference").get)
    scope <- _scope(root("scope").get)
  } yield InternalModelHistorySelection(storageid, reference, packagevalue, scope)

  private def _content(json: Json): Either[String, InternalModelHistoryDocumentState] = for {
    root <- json.asObject.toRight("content-object")
    kind <- _string(root, "kind")
    content <- kind match {
      case "retained" => for {
        fields <- _object(json, Set("kind", "record", "retainedAt"))
        record <- _record(fields("record").get)
        time <- _time(fields, "retainedAt")
      } yield InternalModelHistoryDocumentState.Retained(record, time)
      case "removed" => for {
        fields <- _object(json, Set("kind", "tombstone"))
        tombstone <- _tombstone(fields("tombstone").get)
      } yield InternalModelHistoryDocumentState.Removed(tombstone)
      case _ => Left("content-kind")
    }
  } yield content

  private def _record(json: Json): Either[String, InternalModelRetainedHistoryRecord] = for {
    root <- _object(json, Set("reference", "subject", "actor", "occurredAt", "payload", "evidence"))
    reference <- _record_reference(root("reference").get)
    subject <- _subject(root("subject").get)
    actor <- _actor(root("actor").get)
    time <- _time(root, "occurredAt")
    payload <- _payload(root("payload").get)
    evidence <- _array(root, "evidence", 64).flatMap(_traverse(_)(_evidence))
  } yield InternalModelRetainedHistoryRecord(reference, subject, actor, time, payload, evidence)

  private def _subject(json: Json): Either[String, InternalModelReviewSubject] = for {
    root <- _object(json, Set("subjectId", "subjectRevision", "packageReference", "scope", "artifacts"))
    subjectid <- _string(root, "subjectId").flatMap(InternalModelRecordId.from)
    revision <- _long(root, "subjectRevision").flatMap(InternalModelRecordRevision.from)
    packagevalue <- _package_reference(root("packageReference").get)
    scope <- _scope(root("scope").get)
    artifacts <- _array(root, "artifacts", 512).flatMap(_traverse(_)(_artifact_reference))
  } yield InternalModelReviewSubject(subjectid, revision, packagevalue, scope, artifacts)

  private def _actor(json: Json): Either[String, InternalModelDecisionActor] = for {
    root <- _object(json, Set("kind", "identity", "role"))
    kind <- _string(root, "kind")
    identity <- _string(root, "identity")
    role <- _string(root, "role")
  } yield InternalModelDecisionActor(kind, identity, role)

  private def _scope(json: Json): Either[String, InternalModelSemanticScope] = for {
    root <- _object(json, Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity"))
    component <- _string(root, "componentIdentity")
    projection <- _string(root, "projectionContextIdentity")
    usecase <- _string(root, "selectedUseCaseElementIdentity")
  } yield InternalModelSemanticScope(component, projection, usecase)

  private def _payload(json: Json): Either[String, InternalModelRetainedHistoryPayload] = {
    import InternalModelRetainedHistoryPayload.*
    for {
      root <- json.asObject.toRight("payload-object")
      kind <- _string(root, "kind")
      payload <- kind match {
        case "Review" => for {
          fields <- _object(json, Set("kind", "reviewReference", "candidateReference", "semanticDiffReference", "outcome"))
          review <- _record_reference(fields("reviewReference").get)
          candidate <- _record_reference(fields("candidateReference").get)
          diff <- _record_reference(fields("semanticDiffReference").get)
          outcome <- _token(fields, "outcome", InternalModelRetainedReviewOutcome.values.toVector)
        } yield Review(review, candidate, diff, outcome)
        case "Proposal" => for {
          fields <- _object(json, Set("kind", "candidateReference", "projectionArtifactReference"))
          candidate <- _record_reference(fields("candidateReference").get)
          projection <- _artifact_reference(fields("projectionArtifactReference").get)
        } yield Proposal(candidate, projection)
        case "Alternative" => for {
          fields <- _object(json, Set("kind", "proposalReference", "alternativeReference", "disposition"))
          proposal <- _record_reference(fields("proposalReference").get)
          alternative <- _record_reference(fields("alternativeReference").get)
          disposition <- _token(fields, "disposition", InternalModelRetainedAlternativeDisposition.values.toVector)
        } yield Alternative(proposal, alternative, disposition)
        case "Supersession" => for {
          fields <- _object(json, Set("kind", "previousReference", "successorReference"))
          previous <- _record_reference(fields("previousReference").get)
          successor <- _record_reference(fields("successorReference").get)
        } yield Supersession(previous, successor)
        case "Evidence" => for {
          fields <- _object(json, Set("kind", "evidenceReference"))
          reference <- _record_reference(fields("evidenceReference").get)
        } yield Evidence(reference)
        case _ => Left("payload-kind")
      }
    } yield payload
  }

  private def _evidence(json: Json): Either[String, InternalModelRetainedEvidence] = for {
    root <- _object(json, Set("reference", "kind", "payload"))
    reference <- _record_reference(root("reference").get)
    kind <- _token(root, "kind", InternalModelEvidenceKind.values.toVector)
    payload <- _evidence_payload(root("payload").get)
  } yield InternalModelRetainedEvidence(reference, kind, payload)

  private def _evidence_payload(json: Json): Either[String, InternalModelRetainedEvidencePayload] = for {
    root <- json.asObject.toRight("evidence-payload-object")
    kind <- _string(root, "kind")
    payload <- kind match {
      case "Identity" => for {
        fields <- _object(json, Set("kind", "value"))
        value <- _string(fields, "value")
      } yield InternalModelRetainedEvidencePayload.Identity(value)
      case "RedactedText" => for {
        fields <- _object(json, Set("kind", "value", "redactionReference"))
        value <- _string(fields, "value")
        reference <- _record_reference(fields("redactionReference").get)
      } yield InternalModelRetainedEvidencePayload.RedactedText(value, reference)
      case "Omitted" => for {
        fields <- _object(json, Set("kind", "reason"))
        reason <- _token(fields, "reason", InternalModelEvidenceOmission.values.toVector)
      } yield InternalModelRetainedEvidencePayload.Omitted(reason)
      case _ => Left("retained-evidence-kind")
    }
  } yield payload

  private def _tombstone(json: Json): Either[String, InternalModelRetainedHistoryTombstone] = for {
    root <- _object(json, Set("reference", "packageReference", "scope", "kind", "action", "effectiveAt"))
    reference <- _record_reference(root("reference").get)
    packagevalue <- _package_reference(root("packageReference").get)
    scope <- _scope(root("scope").get)
    kind <- _token(root, "kind", InternalModelRetainedHistoryKind.values.toVector)
    action <- _token(root, "action", InternalModelHistoryRetentionAction.values.toVector)
    time <- _time(root, "effectiveAt")
  } yield InternalModelRetainedHistoryTombstone(reference, packagevalue, scope, kind, action, time)

  private def _record_reference(json: Json): Either[String, InternalModelRecordReference] =
    InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(json)).left.map(_ => "record-reference")
  private def _artifact_reference(json: Json): Either[String, InternalModelArtifactReference] =
    InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(json)).left.map(_ => "artifact-reference")
  private def _package_reference(json: Json): Either[String, InternalModelPackageReference] =
    InternalModelTypedControlCodec.decodePackageReference(_json_bytes(json)).left.map(_ => "package-reference")
  private def _json_bytes(json: Json): Vector[Byte] = _printer.print(json).getBytes(StandardCharsets.UTF_8).toVector

  private def _object(json: Json, fields: Set[String]): Either[String, JsonObject] = for {
    root <- json.asObject.toRight("object")
    _ <- Either.cond(root.keys.toSet == fields, (), "closed-fields")
  } yield root
  private def _string(root: JsonObject, key: String): Either[String, String] =
    root(key).flatMap(_.asString).toRight("string-" + key)
  private def _long(root: JsonObject, key: String): Either[String, Long] =
    root(key).flatMap(_.asNumber).flatMap(number => number.toLong.filter(value => value > 0 && number.toString == value.toString))
      .toRight("positive-integral-" + key)
  private def _array(root: JsonObject, key: String, limit: Int): Either[String, Vector[Json]] =
    root(key).flatMap(_.asArray).filter(_.size <= limit).toRight("bounded-array-" + key)
  private def _token[A](root: JsonObject, key: String, values: Vector[A]): Either[String, A] =
    _string(root, key).flatMap(token => values.find(_.toString == token).toRight("closed-token-" + key))
  private def _time(root: JsonObject, key: String): Either[String, Instant] =
    _string(root, key).flatMap { text =>
      try Right(Instant.parse(text)) catch { case NonFatal(_) => Left("instant-" + key) }
    }
  private def _traverse[A](values: Vector[Json])(decode: Json => Either[String, A]): Either[String, Vector[A]] =
    values.foldLeft[Either[String, Vector[A]]](Right(Vector.empty)) { (result, json) =>
      result.flatMap(admitted => decode(json).map(admitted :+ _))
    }
  private def _utf8_input(bytes: Vector[Byte]): Either[String, String] = {
    try Right(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toArray)).toString)
    catch { case NonFatal(_) => Left("strict-utf8") }
  }
  private def _utf8_output(text: String): Consequence[Vector[Byte]] = {
    if (text == null || text.length > _limit) _invalid("document-byte-limit")
    else _boundary("strict-utf8") {
      val buffer = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text))
      val bytes = new Array[Byte](buffer.remaining())
      buffer.get(bytes)
      Consequence.success(bytes.toVector)
    }
  }
  private def _unicode_json(json: Json): Either[String, Unit] = {
    json.asString match {
      case Some(text) => _utf8_output(text).toOption.map(_ => ()).toRight("strict-unicode")
      case None =>
        val children = json.asArray.getOrElse(Vector.empty) ++ json.asObject.toVector.flatMap(_.values)
        children.foldLeft[Either[String, Unit]](Right(()))((result, child) => result.flatMap(_ => _unicode_json(child)))
    }
  }
  private def _unicode_document(document: InternalModelHistoryDocument): Consequence[Unit] = {
    def _scope_strings_(scope: InternalModelSemanticScope): Vector[String] =
      Vector(scope.componentIdentity, scope.projectionContextIdentity, scope.selectedUseCaseElementIdentity)
    def _package_strings_(reference: InternalModelPackageReference): Vector[String] =
      Vector(reference.packageId.value, reference.projectNamespace.value, reference.projectId.value)
    val selection = document.selection
    val common = Vector(selection.storageId.value, selection.reference.recordId.value) ++
      _scope_strings_(selection.scope) ++ _package_strings_(selection.packageReference)
    val strings = document.content match {
      case InternalModelHistoryDocumentState.Removed(tombstone) => common ++ Vector(tombstone.reference.recordId.value) ++
        _scope_strings_(tombstone.scope) ++ _package_strings_(tombstone.packageReference)
      case InternalModelHistoryDocumentState.Retained(record, _) =>
        val payload = record.payload match {
          case InternalModelRetainedHistoryPayload.Review(review, candidate, diff, _) => Vector(review.recordId.value, candidate.recordId.value, diff.recordId.value)
          case InternalModelRetainedHistoryPayload.Proposal(candidate, projection) => Vector(candidate.recordId.value, projection.artifactId.value)
          case InternalModelRetainedHistoryPayload.Alternative(proposal, alternative, _) => Vector(proposal.recordId.value, alternative.recordId.value)
          case InternalModelRetainedHistoryPayload.Supersession(previous, successor) => Vector(previous.recordId.value, successor.recordId.value)
          case InternalModelRetainedHistoryPayload.Evidence(reference) => Vector(reference.recordId.value)
        }
        val evidence = record.evidence.flatMap { retained =>
          Vector(retained.reference.recordId.value) ++ (retained.payload match {
            case InternalModelRetainedEvidencePayload.Identity(value) => Vector(value)
            case InternalModelRetainedEvidencePayload.RedactedText(value, reference) => Vector(value, reference.recordId.value)
            case InternalModelRetainedEvidencePayload.Omitted(_) => Vector.empty
          })
        }
        common ++ Vector(record.reference.recordId.value, record.subject.subjectId.value,
          record.actor.kind, record.actor.identity, record.actor.role) ++ record.subject.artifacts.map(_.artifactId.value) ++
          _scope_strings_(record.subject.scope) ++ _package_strings_(record.subject.packageReference) ++ payload ++ evidence
    }
    Consequence.zipN(strings.map(text => _utf8_output(text).map(_ => ()))).map(_ => ())
  }
  private def _boundary[A](dimension: String)(body: => Consequence[A]): Consequence[A] = {
    try body catch { case NonFatal(_) => _invalid(dimension) }
  }
  private def _check(valid: Boolean, dimension: String): Consequence[Unit] = if (valid) Consequence.unit else _invalid(dimension)
  private def _invalid[A](dimension: String): Consequence[A] = Consequence.operationInvalid("internal-model history invalid: " + dimension)
}
