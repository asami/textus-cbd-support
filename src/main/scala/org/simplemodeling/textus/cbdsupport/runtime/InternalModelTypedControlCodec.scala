package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/**
 * Strict structural admission of declared references, independent of JSON presentation.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] object InternalModelTypedControlCodec {
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys
  private val _package_fields = Set("packageId", "projectNamespace", "projectId")
  private val _artifact_fields = Set("artifactId", "artifactRevision", "role")
  private val _record_fields = Set("recordId", "recordRevision")

  def decodePackageReference(bytes: Vector[Byte]): Either[String, InternalModelPackageReference] =
    for {
      root <- _object(bytes, _package_fields)
      packageid <- _string(root, "packageId").flatMap(InternalModelPackageId.from)
      namespace <- _string(root, "projectNamespace").flatMap(InternalModelProjectToken.from)
      projectid <- _string(root, "projectId").flatMap(InternalModelProjectToken.from)
    } yield InternalModelPackageReference(packageid, namespace, projectid)

  def decodeArtifactReference(bytes: Vector[Byte]): Either[String, InternalModelArtifactReference] =
    for {
      root <- _object(bytes, _artifact_fields)
      artifactid <- _string(root, "artifactId").flatMap(InternalModelArtifactId.from)
      revision <- _positive_long(root, "artifactRevision").flatMap(InternalModelArtifactRevision.from)
      role <- _string(root, "role").flatMap(InternalModelArtifactRole.fromWire)
    } yield InternalModelArtifactReference(artifactid, revision, role)

  def decodeRecordReference(bytes: Vector[Byte]): Either[String, InternalModelRecordReference] =
    for {
      root <- _object(bytes, _record_fields)
      recordid <- _string(root, "recordId").flatMap(InternalModelRecordId.from)
      revision <- _positive_long(root, "recordRevision").flatMap(InternalModelRecordRevision.from)
    } yield InternalModelRecordReference(recordid, revision)

  def encodePackageReference(value: InternalModelPackageReference): Vector[Byte] =
    _encode(Json.obj(
      "packageId" -> Json.fromString(value.packageId.value),
      "projectNamespace" -> Json.fromString(value.projectNamespace.value),
      "projectId" -> Json.fromString(value.projectId.value)
    ))

  def encodeArtifactReference(value: InternalModelArtifactReference): Vector[Byte] =
    _encode(Json.obj(
      "artifactId" -> Json.fromString(value.artifactId.value),
      "artifactRevision" -> Json.fromLong(value.artifactRevision.value),
      "role" -> Json.fromString(value.role.wireValue)
    ))

  def encodeRecordReference(value: InternalModelRecordReference): Vector[Byte] =
    _encode(Json.obj(
      "recordId" -> Json.fromString(value.recordId.value),
      "recordRevision" -> Json.fromLong(value.recordRevision.value)
    ))

  private def _encode(json: Json): Vector[Byte] =
    (_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8).toVector

  private def _object(bytes: Vector[Byte], fields: Set[String]): Either[String, JsonObject] =
    for {
      content <- _utf8(bytes)
      _ <- Either.cond(!content.startsWith("\uFEFF"), (), "reference JSON must not have a BOM")
      json <- _parser.parse(content).left.map(_ => "reference must be strict JSON without duplicate members")
      root <- json.asObject.toRight("reference must be an object")
      _ <- Either.cond(root.keys.toSet == fields, (), "reference fields must match the exact closed shape")
    } yield root

  private def _string(root: JsonObject, key: String): Either[String, String] =
    root(key).flatMap(_.asString).toRight(s"$key must be a string")

  private def _positive_long(root: JsonObject, key: String): Either[String, Long] =
    root(key).flatMap(_.asNumber).flatMap { number =>
      number.toLong.filter(value => value > 0 && number.toString == value.toString)
    }.toRight(s"$key must be a positive canonical Long integer")

  private def _utf8(bytes: Vector[Byte]): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes.toArray)).toString)
    } catch {
      case _: CharacterCodingException => Left("reference bytes must be valid UTF-8")
    }
}
