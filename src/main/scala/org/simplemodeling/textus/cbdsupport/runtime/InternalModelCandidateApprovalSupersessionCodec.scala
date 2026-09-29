package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Strict portable JSON codec for one explicit candidate-approval replacement link. */
private[runtime] object InternalModelCandidateApprovalSupersessionCodec {
  private val _root_fields = Set("predecessorApproval", "profile", "schemaVersion", "successorApproval")
  private val _artifact_fields = Set("artifactId", "sha256")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateApprovalSupersession] =
    if bytes == null then Left("candidate approval supersession bytes are missing")
    else {
      val raw = bytes.toArray
      for {
        _ <- Either.cond(!_has_bom(raw), (), "candidate approval supersession bytes must not contain a UTF-8 byte-order mark")
        content <- _decode_utf8(raw, "candidate approval supersession bytes")
        json <- _json_parser.parse(content).left.map(_ => "candidate approval supersession bytes must be valid JSON without duplicate members")
        root <- _object(json, "candidate approval supersession root")
        canonical = _canonical_bytes(json)
        _ <- Either.cond(Arrays.equals(raw, canonical), (), "candidate approval supersession bytes are not canonical JSON")
        _ <- _closed_fields(root, _root_fields, "candidate approval supersession root")
        predecessor <- _artifact(root, "predecessorApproval", "candidate approval supersession root")
        profile <- _nonblank(root, "profile", "candidate approval supersession root")
        _ <- Either.cond(profile == "ccdm-candidate-approval-supersession-v1", (), "candidate approval supersession profile is unsupported")
        schema <- _nonblank(root, "schemaVersion", "candidate approval supersession root")
        _ <- Either.cond(schema == "1.0", (), "candidate approval supersession schemaVersion is unsupported")
        successor <- _artifact(root, "successorApproval", "candidate approval supersession root")
        _ <- Either.cond(predecessor != successor, (), "candidate approval supersession predecessor and successor must differ")
        value = InternalModelCandidateApprovalSupersession(predecessor, successor, profile, schema, Vector.empty)
        encoded = encode(value).toVector
        _ <- Either.cond(encoded == canonical.toVector, (), "typed candidate approval supersession re-encoding does not match supplied canonical bytes")
      } yield value.copy(canonicalBytes = encoded)
    }

  def encode(value: InternalModelCandidateApprovalSupersession): Array[Byte] =
    _canonical_bytes(Json.obj(
      "predecessorApproval" -> _artifact_json(value.predecessorApproval),
      "profile" -> Json.fromString(value.profile),
      "schemaVersion" -> Json.fromString(value.schemaVersion),
      "successorApproval" -> _artifact_json(value.successorApproval)
    ))

  private def _artifact(
    root: JsonObject,
    key: String,
    label: String
  ): Either[String, InternalModelCandidateReviewArtifact] =
    for {
      value <- root(key).toRight(s"$label $key is missing")
      objectvalue <- _object(value, s"$label $key")
      _ <- _closed_fields(objectvalue, _artifact_fields, s"$label $key")
      identity <- _nonblank(objectvalue, "artifactId", s"$label $key")
      digest <- _digest(objectvalue, "sha256", s"$label $key")
    } yield InternalModelCandidateReviewArtifact(identity, digest)

  private def _artifact_json(value: InternalModelCandidateReviewArtifact): Json =
    Json.obj("artifactId" -> Json.fromString(value.artifactId), "sha256" -> Json.fromString(value.sha256))

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _string(root: JsonObject, key: String, label: String): Either[String, String] =
    root(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonblank(root: JsonObject, key: String, label: String): Either[String, String] =
    _string(root, key, label).flatMap(value => Either.cond(_is_nonblank_unicode(value), value, s"$label $key must be a nonblank Unicode string"))

  private def _digest(root: JsonObject, key: String, label: String): Either[String, String] =
    _string(root, key, label).flatMap(value => Either.cond(_digest_pattern.matches(value), value, s"$label $key must be a lowercase sha256 digest"))

  private def _closed_fields(root: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(root.keys.toSet == fields, (), s"$label fields are not closed")

  private def _is_nonblank_unicode(value: String): Boolean =
    _is_unicode_scalar_sequence(value) && value.codePoints().anyMatch(codepoint => codepoint > 0x20 && codepoint != 0x85 && !Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint))

  private def _is_unicode_scalar_sequence(value: String): Boolean = {
    var index = 0
    var valid = value != null
    while index < Option(value).map(_.length).getOrElse(0) && valid do {
      val unit = value.charAt(index)
      if Character.isHighSurrogate(unit) then {
        valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1))
        index += 2
      } else if Character.isLowSurrogate(unit) then valid = false
      else index += 1
    }
    valid
  }

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
