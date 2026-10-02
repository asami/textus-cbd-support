package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 29, 2026
 *  version Sep. 29, 2026
 * @version Oct.  1, 2026
 * @author  ASAMI, Tomoharu
 */
/** Portable explicit typed lineage, independent of harmless JSON presentation. */
private[runtime] object InternalModelCandidateApprovalSupersessionCodec {
  private val _root_fields = Set("predecessorApproval", "profile", "schemaVersion", "successorApproval")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateApprovalSupersession] = {
    for {
      _ <- Either.cond(bytes != null, (), "candidate approval supersession bytes are missing")
      content <- _decode_utf8(bytes.toArray)
      _ <- Either.cond(!content.startsWith("\uFEFF"), (), "candidate approval supersession must not have a BOM")
      json <- _json_parser.parse(content).left.map(_ => "candidate approval supersession must be strict JSON without duplicate members or trailing content")
      value <- _value(json)
    } yield value
  }

  /** Structural grammar only, never independent successor admission. */
  def validateValue(value: InternalModelCandidateApprovalSupersession): Either[String, Unit] = {
    try _value(_value_json(value)).map(_ => ())
    catch { case NonFatal(_) => Left("candidate approval supersession contains a null or invalid object graph") }
  }

  def encode(value: InternalModelCandidateApprovalSupersession): Array[Byte] = _json_bytes(_value_json(value))

  private def _value(json: Json): Either[String, InternalModelCandidateApprovalSupersession] = {
    for {
      _ <- _valid_json_unicode(json)
      root <- json.asObject.toRight("candidate approval supersession root must be an object")
      _ <- Either.cond(root.keys.toSet == _root_fields, (), "candidate approval supersession root fields are not closed")
      predecessor <- _artifact(root, "predecessorApproval")
      profile <- root("profile").flatMap(_.asString).toRight("candidate approval supersession profile must be a string")
      _ <- Either.cond(profile == "ccdm-candidate-approval-supersession-v2", (), "candidate approval supersession profile is unsupported")
      schema <- root("schemaVersion").flatMap(_.asString).toRight("candidate approval supersession schemaVersion must be a string")
      _ <- Either.cond(schema == "2.0", (), "candidate approval supersession schemaVersion is unsupported")
      successor <- _artifact(root, "successorApproval")
      _ <- Either.cond(predecessor != successor, (), "candidate approval supersession predecessor and successor must differ")
    } yield InternalModelCandidateApprovalSupersession(predecessor, successor, profile, schema)
  }

  private def _artifact(root: JsonObject, key: String): Either[String, InternalModelArtifactReference] = {
    for {
      value <- root(key).toRight(s"candidate approval supersession $key is missing")
      reference <- InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(value).toVector)
      _ <- Either.cond(reference.role == InternalModelArtifactRole.Approval, (), s"candidate approval supersession $key must have Approval role")
    } yield reference
  }

  private def _value_json(value: InternalModelCandidateApprovalSupersession): Json = Json.obj(
    "predecessorApproval" -> _artifact_json(value.predecessorApproval),
    "profile" -> Json.fromString(value.profile),
    "schemaVersion" -> Json.fromString(value.schemaVersion),
    "successorApproval" -> _artifact_json(value.successorApproval)
  )
  private def _artifact_json(value: InternalModelArtifactReference): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId.value), "artifactRevision" -> Json.fromLong(value.artifactRevision.value), "role" -> Json.fromString(value.role.wireValue))
  private def _valid_unicode(value: String): Boolean = {
    var index = 0
    var valid = value != null
    while (valid && index < value.length) {
      val unit = value.charAt(index)
      if (Character.isHighSurrogate(unit)) { valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1)); index += 2 }
      else { valid = !Character.isLowSurrogate(unit); index += 1 }
    }
    valid
  }
  private def _valid_json_unicode(value: Json): Either[String, Unit] = {
    val valid = value != null && value.fold(true, _ => true, _ => true, _valid_unicode, values => values.forall(item => _valid_json_unicode(item).isRight), fields => fields.toVector.forall { case (key, item) => _valid_unicode(key) && _valid_json_unicode(item).isRight })
    Either.cond(valid, (), "candidate approval supersession JSON contains invalid Unicode")
  }
  private def _decode_utf8(bytes: Array[Byte]): Either[String, String] = {
    try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) }
    catch { case NonFatal(_) => Left("candidate approval supersession bytes must be valid UTF-8") }
  }
  private def _json_bytes(value: Json): Array[Byte] = {
    _valid_json_unicode(value).fold(message => throw new IllegalArgumentException(message), _ => ())
    (_printer.print(value) + "\n").getBytes(StandardCharsets.UTF_8)
  }
}
