package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.util.Arrays
import scala.util.control.NonFatal
import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/**
 * Closed syntactic admission only; stage and action eligibility belongs to the later gate.
 *
 * @since   Oct.  1, 2026
 * @version Oct.  1, 2026
 */
private[runtime] object InternalModelResumeCursorCodec {
  private val _root_fields = Set(
    "schemaVersion", "profile", "packageId", "packageRevision", "selectedArtifacts", "currentStage",
    "lastCompletedAction", "nextPermittedAction", "blockers", "preconditions", "invalidationChecks", "acceptanceCriteria"
  )
  private val _token_pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelResumeCursor] =
    try {
      for {
        _ <- Either.cond(!bytes.startsWith(Vector(0xef.toByte, 0xbb.toByte, 0xbf.toByte)), (), "resume bytes must not have a BOM")
        content <- _utf8(bytes)
        json <- _parser.parse(content).left.map(_ => "resume bytes must be JSON without duplicate keys")
        _ <- Either.cond(_unicode_scalars(json), (), "resume strings and member names must contain Unicode scalars")
        root <- _object(json, _root_fields, "resume root")
        schema <- _string(root, "schemaVersion")
        _ <- Either.cond(schema == "2.0", (), "resume schemaVersion must be 2.0")
        profile <- _string(root, "profile")
        _ <- Either.cond(profile == "ccdm-resume-v2", (), "resume profile must be ccdm-resume-v2")
        packageid <- _string(root, "packageId").flatMap(InternalModelPackageId.from)
        number <- root("packageRevision").flatMap(_.asNumber).toRight("resume packageRevision must be an integer")
        revision <- number.toLong.filter(value => value > 0 && number.toString == value.toString).toRight("resume packageRevision must be a positive canonical Long integer")
        bindings <- _items(root, "selectedArtifacts")(_reference)
        _ <- _ordered(bindings.map(_.artifactId.value), "selectedArtifacts")
        stage <- _token(root, "currentStage")
        last <- _optional_token(root, "lastCompletedAction")
        next <- _optional_token(root, "nextPermittedAction")
        blockers <- _conditions(root, "blockers", bindings)
        preconditions <- _conditions(root, "preconditions", bindings)
        invalidations <- _conditions(root, "invalidationChecks", bindings)
        criteria <- _conditions(root, "acceptanceCriteria", bindings)
      } yield InternalModelResumeCursor(schema, profile, packageid, revision, bindings, stage, last, next, blockers, preconditions, invalidations, criteria)
    } catch {
      case NonFatal(error) => Left(s"resume decoding failed: ${Option(error.getMessage).getOrElse(error.getClass.getSimpleName)}")
    }

  def encode(cursor: InternalModelResumeCursor): Vector[Byte] =
    (_printer.print(Json.obj(
      "schemaVersion" -> Json.fromString(cursor.schemaVersion),
      "profile" -> Json.fromString(cursor.profile),
      "packageId" -> Json.fromString(cursor.packageId.value),
      "packageRevision" -> Json.fromLong(cursor.packageRevision),
      "selectedArtifacts" -> Json.fromValues(cursor.selectedArtifacts.map(binding => Json.obj(
        "artifactId" -> Json.fromString(binding.artifactId.value),
        "artifactRevision" -> Json.fromLong(binding.artifactRevision.value),
        "role" -> Json.fromString(binding.role.wireValue)
      ))),
      "currentStage" -> Json.fromString(cursor.currentStage),
      "lastCompletedAction" -> cursor.lastCompletedAction.map(Json.fromString).getOrElse(Json.Null),
      "nextPermittedAction" -> cursor.nextPermittedAction.map(Json.fromString).getOrElse(Json.Null),
      "blockers" -> _condition_json(cursor.blockers),
      "preconditions" -> _condition_json(cursor.preconditions),
      "invalidationChecks" -> _condition_json(cursor.invalidationChecks),
      "acceptanceCriteria" -> _condition_json(cursor.acceptanceCriteria)
    )) + "\n").getBytes(StandardCharsets.UTF_8).toVector

  private def _condition_json(values: Vector[InternalModelResumeCondition]): Json =
    Json.fromValues(values.map { value =>
      val fields = Vector("code" -> Json.fromString(value.code), "detail" -> Json.fromString(value.detail)) ++
        value.artifactId.toVector.map(id => "artifactId" -> Json.fromString(id))
      Json.fromJsonObject(JsonObject.fromIterable(fields))
    })

  private def _reference(value: Json): Either[String, InternalModelArtifactReference] =
    for {
      reference <- InternalModelTypedControlCodec.decodeArtifactReference(_printer.print(value).getBytes(StandardCharsets.UTF_8).toVector)
      _ <- Either.cond(reference.role != InternalModelArtifactRole.Resume, (), "resume selection must not reference a resume artifact")
    } yield reference

  private def _conditions(root: JsonObject, key: String, bindings: Vector[InternalModelArtifactReference]): Either[String, Vector[InternalModelResumeCondition]] =
    for {
      values <- _items(root, key)(_condition)
      _ <- _ordered(values.map(_.code), key)
      _ <- Either.cond(values.forall(_.artifactId.forall(id => bindings.count(_.artifactId.value == id) == 1)), (), s"resume $key references an unselected or ambiguous artifact")
    } yield values

  private def _condition(value: Json): Either[String, InternalModelResumeCondition] =
    for {
      objectvalue <- value.asObject.toRight("resume condition must be an object")
      root <- _object(value, if objectvalue.contains("artifactId") then Set("artifactId", "code", "detail") else Set("code", "detail"), "resume condition")
      code <- _token(root, "code")
      detail <- _string(root, "detail")
      id <- if root.contains("artifactId") then _token(root, "artifactId").map(Some(_)) else Right(None)
    } yield InternalModelResumeCondition(code, detail, id)

  private def _items[A](root: JsonObject, key: String)(decode: Json => Either[String, A]): Either[String, Vector[A]] =
    root(key).flatMap(_.asArray).toRight(s"resume $key must be an array").flatMap { values =>
      values.foldLeft[Either[String, Vector[A]]](Right(Vector.empty)) { (result, value) =>
        for { collected <- result; item <- decode(value) } yield collected :+ item
      }
    }

  private def _object(value: Json, fields: Set[String], label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object").flatMap(root => Either.cond(root.keys.toSet == fields, root, s"$label fields are not the closed schema"))

  private def _string(root: JsonObject, key: String): Either[String, String] =
    root(key).flatMap(_.asString).toRight(s"resume $key must be a string")

  private def _token(root: JsonObject, key: String): Either[String, String] =
    _string(root, key).flatMap(value => Either.cond(_token_pattern.matches(value), value, s"resume $key must be an ASCII token"))

  private def _optional_token(root: JsonObject, key: String): Either[String, Option[String]] =
    if root(key).contains(Json.Null) then Right(None) else _token(root, key).map(Some(_))

  private def _ordered(values: Vector[String], label: String): Either[String, Unit] =
    Either.cond(values == values.sortWith((left, right) => Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)) < 0) &&
      values.distinct.size == values.size, (), s"resume $label must be sorted and unique")

  /** Check before nested reference serialization, which would otherwise replace lone surrogates. */
  private def _unicode_scalars(value: Json): Boolean =
    value.asString.forall(_scalar_text) && value.asArray.forall(_.forall(_unicode_scalars)) &&
      value.asObject.forall(_.toVector.forall { case (key, item) => _scalar_text(key) && _unicode_scalars(item) })

  private def _scalar_text(value: String): Boolean = {
    var index = 0
    while (index < value.length) {
      val character = value.charAt(index)
      if (Character.isHighSurrogate(character)) {
        if (index + 1 >= value.length || !Character.isLowSurrogate(value.charAt(index + 1))) return false
        index += 2
      } else if (Character.isLowSurrogate(character)) return false
      else index += 1
    }
    true
  }

  private def _utf8(bytes: Vector[Byte]): Either[String, String] =
    try {
      Right(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toArray)).toString)
    } catch { case NonFatal(_) => Left("resume bytes must be valid UTF-8") }
}
