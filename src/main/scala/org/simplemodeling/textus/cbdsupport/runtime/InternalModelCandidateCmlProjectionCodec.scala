package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.security.MessageDigest
import java.util.{Arrays, Base64}

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Parses and encodes the closed candidate CML projection profile without CML interpretation. */
private[runtime] object InternalModelCandidateCmlProjectionCodec {
  private val _root_fields = Set(
    "candidateIdentity", "candidateModelIdentity", "candidateRevision", "continuityArtifactId", "profile",
    "realizationArtifactId", "schemaVersion", "scope", "targets"
  )
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _target_fields = Set("baselineArtifactId", "effects", "mappings", "patchIdentity", "projectRelativePath", "proposedContent", "source", "targetId")
  private val _content_fields = Set("byteLength", "rawBytesBase64", "sha256")
  private val _source_fields = Set("authority", "identity", "locator", "revision", "sha256")
  private val _mapping_fields = Set("canonicalAssertionIds", "cmlAnchor", "cmlSemanticIdentity", "conditionIds", "enrichmentAssertionIds", "mappingId", "semanticIdentity", "semanticIdentityKind")
  private val _effect_fields = Set("assessment", "detail", "effectId", "kind", "mappingIds", "sourceReferenceId")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(projection: InternalModelVerifiedProjection): Either[String, InternalModelCandidateCmlProjection] =
    for {
      _ <- Either.cond(projection.role == "projection", (), "selected candidate artifact role must be projection")
      bytes = projection.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "candidate CML projection bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "candidate CML projection bytes")
      json <- _json_parser.parse(content).left.map(_ => "candidate CML projection bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("candidate CML projection root must be an object")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(bytes, canonical), (), "candidate CML projection bytes are not canonical JSON")
      _ <- _closed_fields(root, _root_fields, "candidate CML projection root")
      candidateidentity <- _nonempty_string(root, "candidateIdentity", "candidate CML projection root")
      modelidentity <- _nonempty_string(root, "candidateModelIdentity", "candidate CML projection root")
      revision <- _candidate_revision(root)
      continuityartifactid <- _nonempty_string(root, "continuityArtifactId", "candidate CML projection root")
      profile <- _nonempty_string(root, "profile", "candidate CML projection root")
      _ <- Either.cond(profile == "ccdm-candidate-cml-projection-v1", (), "candidate CML projection profile is unsupported")
      realizationartifactid <- _nonempty_string(root, "realizationArtifactId", "candidate CML projection root")
      schema <- _nonempty_string(root, "schemaVersion", "candidate CML projection root")
      _ <- Either.cond(schema == "1.0", (), "candidate CML projection schemaVersion is unsupported")
      scope <- _scope(root)
      targets <- _targets(root)
      candidate = InternalModelCandidateCmlProjection(
        candidateIdentity = candidateidentity,
        candidateModelIdentity = modelidentity,
        candidateRevision = revision,
        continuityArtifactId = continuityartifactid,
        profile = profile,
        realizationArtifactId = realizationartifactid,
        schemaVersion = schema,
        scope = scope,
        targets = targets,
        canonicalBytes = Vector.empty
      )
      encoded = encode(candidate).toVector
      _ <- Either.cond(encoded == canonical.toVector, (), "typed candidate CML projection re-encoding does not match supplied canonical bytes")
    } yield candidate.copy(canonicalBytes = encoded)

  def encode(projection: InternalModelCandidateCmlProjection): Array[Byte] =
    _canonical_bytes(Json.obj(
      "candidateIdentity" -> Json.fromString(projection.candidateIdentity),
      "candidateModelIdentity" -> Json.fromString(projection.candidateModelIdentity),
      "candidateRevision" -> Json.fromInt(projection.candidateRevision),
      "continuityArtifactId" -> Json.fromString(projection.continuityArtifactId),
      "profile" -> Json.fromString(projection.profile),
      "realizationArtifactId" -> Json.fromString(projection.realizationArtifactId),
      "schemaVersion" -> Json.fromString(projection.schemaVersion),
      "scope" -> _scope_json(projection.scope),
      "targets" -> Json.fromValues(projection.targets.map(_target_json))
    ))

  private def _scope(root: JsonObject): Either[String, InternalModelSemanticScope] =
    for {
      value <- root("scope").toRight("candidate CML projection scope is missing")
      objectvalue <- _object(value, "candidate CML projection scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "candidate CML projection scope")
      componentidentity <- _nonempty_string(objectvalue, "componentIdentity", "candidate CML projection scope")
      contextidentity <- _nonempty_string(objectvalue, "projectionContextIdentity", "candidate CML projection scope")
      usecaseidentity <- _nonempty_string(objectvalue, "selectedUseCaseElementIdentity", "candidate CML projection scope")
    } yield InternalModelSemanticScope(componentidentity, contextidentity, usecaseidentity)

  private def _targets(root: JsonObject): Either[String, Vector[InternalModelCandidateCmlTarget]] =
    for {
      values <- _array(root, "targets", "candidate CML projection root")
      _ <- Either.cond(values.nonEmpty, (), "candidate CML projection targets must be nonempty")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateCmlTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          target <- _target(value, index)
        } yield collected :+ target
      }
      _ <- _strictly_sorted(targets.map(target => Vector(target.targetId)), "candidate CML projection targets")
      _ <- _unique(targets.map(_.patchIdentity), "candidate CML projection patch identities")
      _ <- _unique(targets.map(_.baselineArtifactId), "candidate CML projection baseline artifact IDs")
      _ <- _unique(targets.map(_.projectRelativePath), "candidate CML projection project-relative paths")
      _ <- _unique(targets.flatMap(_.mappings.map(_.mappingId)), "candidate CML projection mapping IDs")
      _ <- _unique(targets.flatMap(_.effects.map(_.effectId)), "candidate CML projection effect IDs")
    } yield targets

  private def _target(value: Json, index: Int): Either[String, InternalModelCandidateCmlTarget] =
    for {
      objectvalue <- _object(value, s"candidate CML projection target $index")
      _ <- _closed_fields(objectvalue, _target_fields, s"candidate CML projection target $index")
      baselineartifactid <- _nonempty_string(objectvalue, "baselineArtifactId", s"candidate CML projection target $index")
      effects <- _effects(objectvalue, index)
      mappings <- _mappings(objectvalue, index)
      patchidentity <- _nonempty_string(objectvalue, "patchIdentity", s"candidate CML projection target $index")
      path <- _nonempty_string(objectvalue, "projectRelativePath", s"candidate CML projection target $index")
      _ <- Either.cond(InternalModelSourceSnapshotFreshness.isSafeCmlProjectRelativePath(path), (), s"candidate CML projection target $index projectRelativePath is unsafe")
      contentvalue <- objectvalue("proposedContent").toRight(s"candidate CML projection target $index proposedContent is missing")
      proposed <- _content(contentvalue, s"candidate CML projection target $index proposedContent")
      sourcevalue <- objectvalue("source").toRight(s"candidate CML projection target $index source is missing")
      source <- _source(sourcevalue, s"candidate CML projection target $index source")
      targetid <- _nonempty_string(objectvalue, "targetId", s"candidate CML projection target $index")
    } yield InternalModelCandidateCmlTarget(baselineartifactid, effects, mappings, patchidentity, path, proposed, source, targetid)

  private def _content(value: Json, label: String): Either[String, InternalModelCandidateCmlContent] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _content_fields, label)
      bytelength <- _nonnegative_integer(objectvalue, "byteLength", label)
      encoded <- _string(objectvalue, "rawBytesBase64", label)
      rawbytes <- _decode_base64(encoded, label)
      _ <- Either.cond(_canonical_base64(rawbytes) == encoded, (), s"$label rawBytesBase64 is not canonical RFC 4648 padded Base64")
      _ <- Either.cond(bytelength == rawbytes.length.toLong, (), s"$label byteLength does not match decoded bytes")
      sha256 <- _string(objectvalue, "sha256", label)
      _ <- Either.cond(_digest_pattern.matches(sha256) && sha256 == _sha256(rawbytes), (), s"$label sha256 does not match decoded bytes")
    } yield InternalModelCandidateCmlContent(bytelength, encoded, sha256)

  private def _source(value: Json, label: String): Either[String, InternalModelSemanticSource] =
    for {
      objectvalue <- _object(value, label)
      _ <- _closed_fields(objectvalue, _source_fields, label)
      authority <- _nonempty_string(objectvalue, "authority", label)
      identity <- _nonempty_string(objectvalue, "identity", label)
      locator <- _nullable_nonempty_string(objectvalue, "locator", label)
      revision <- _nullable_nonempty_string(objectvalue, "revision", label)
      sha256 <- _string(objectvalue, "sha256", label)
      _ <- Either.cond(_digest_pattern.matches(sha256), (), s"$label sha256 is invalid")
    } yield InternalModelSemanticSource(authority, identity, locator, revision, sha256)

  private def _mappings(objectvalue: JsonObject, targetindex: Int): Either[String, Vector[InternalModelCandidateCmlMapping]] =
    for {
      values <- _array(objectvalue, "mappings", s"candidate CML projection target $targetindex")
      _ <- Either.cond(values.nonEmpty, (), s"candidate CML projection target $targetindex mappings must be nonempty")
      mappings <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateCmlMapping]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          mapping <- _mapping(value, targetindex, index)
        } yield collected :+ mapping
      }
      _ <- _strictly_sorted(mappings.map(mapping => Vector(mapping.mappingId)), s"candidate CML projection target $targetindex mappings")
    } yield mappings

  private def _mapping(value: Json, targetindex: Int, index: Int): Either[String, InternalModelCandidateCmlMapping] =
    for {
      objectvalue <- _object(value, s"candidate CML projection target $targetindex mapping $index")
      _ <- _closed_fields(objectvalue, _mapping_fields, s"candidate CML projection target $targetindex mapping $index")
      canonicalids <- _sorted_strings(objectvalue, "canonicalAssertionIds", s"candidate CML projection target $targetindex mapping $index")
      anchor <- _nonempty_string(objectvalue, "cmlAnchor", s"candidate CML projection target $targetindex mapping $index")
      cmlidentity <- _nonempty_string(objectvalue, "cmlSemanticIdentity", s"candidate CML projection target $targetindex mapping $index")
      conditionids <- _sorted_strings(objectvalue, "conditionIds", s"candidate CML projection target $targetindex mapping $index")
      enrichmentids <- _sorted_strings(objectvalue, "enrichmentAssertionIds", s"candidate CML projection target $targetindex mapping $index")
      mappingid <- _nonempty_string(objectvalue, "mappingId", s"candidate CML projection target $targetindex mapping $index")
      semanticidentity <- _nonempty_string(objectvalue, "semanticIdentity", s"candidate CML projection target $targetindex mapping $index")
      kind <- _nonempty_string(objectvalue, "semanticIdentityKind", s"candidate CML projection target $targetindex mapping $index")
      _ <- Either.cond(Set("element", "relationship").contains(kind), (), s"candidate CML projection target $targetindex mapping $index semanticIdentityKind is invalid")
    } yield InternalModelCandidateCmlMapping(canonicalids, anchor, cmlidentity, conditionids, enrichmentids, mappingid, semanticidentity, kind)

  private def _effects(objectvalue: JsonObject, targetindex: Int): Either[String, Vector[InternalModelCandidateCmlEffect]] =
    for {
      values <- _array(objectvalue, "effects", s"candidate CML projection target $targetindex")
      _ <- Either.cond(values.nonEmpty, (), s"candidate CML projection target $targetindex effects must be nonempty")
      effects <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateCmlEffect]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          effect <- _effect(value, targetindex, index)
        } yield collected :+ effect
      }
      _ <- _strictly_sorted(effects.map(effect => Vector(effect.effectId)), s"candidate CML projection target $targetindex effects")
      _ <- Either.cond(effects.exists(_.kind == "compatibility") && effects.exists(_.kind == "migration"), (), s"candidate CML projection target $targetindex effects must include compatibility and migration")
    } yield effects

  private def _effect(value: Json, targetindex: Int, index: Int): Either[String, InternalModelCandidateCmlEffect] =
    for {
      objectvalue <- _object(value, s"candidate CML projection target $targetindex effect $index")
      _ <- _closed_fields(objectvalue, _effect_fields, s"candidate CML projection target $targetindex effect $index")
      assessment <- _nonempty_string(objectvalue, "assessment", s"candidate CML projection target $targetindex effect $index")
      detail <- _nonempty_string(objectvalue, "detail", s"candidate CML projection target $targetindex effect $index")
      effectid <- _nonempty_string(objectvalue, "effectId", s"candidate CML projection target $targetindex effect $index")
      kind <- _nonempty_string(objectvalue, "kind", s"candidate CML projection target $targetindex effect $index")
      _ <- _assessment(kind, assessment, s"candidate CML projection target $targetindex effect $index")
      mappingids <- _nonempty_sorted_strings(objectvalue, "mappingIds", s"candidate CML projection target $targetindex effect $index")
      referenceid <- _nonempty_string(objectvalue, "sourceReferenceId", s"candidate CML projection target $targetindex effect $index")
    } yield InternalModelCandidateCmlEffect(assessment, detail, effectid, kind, mappingids, referenceid)

  private def _assessment(kind: String, assessment: String, label: String): Either[String, Unit] =
    kind match {
      case "compatibility" => Either.cond(Set("unchanged", "compatible", "breaking", "unknown").contains(assessment), (), s"$label compatibility assessment is invalid")
      case "migration" => Either.cond(Set("not-required", "required", "unknown").contains(assessment), (), s"$label migration assessment is invalid")
      case _ => Left(s"$label kind is invalid")
    }

  private def _candidate_revision(root: JsonObject): Either[String, Int] =
    root("candidateRevision").flatMap(_.asNumber).flatMap { number =>
      number.toInt.filter(value => value >= 1 && number.toString == value.toString)
    }.toRight("candidate CML projection candidateRevision must be a canonical integer in 1..2147483647")

  private def _nonnegative_integer(objectvalue: JsonObject, key: String, label: String): Either[String, Long] =
    objectvalue(key).flatMap(_.asNumber).flatMap { number =>
      number.toLong.filter(value => value >= 0 && number.toString == value.toString)
    }.toRight(s"$label $key must be a nonnegative canonical JSON integer")

  private def _scope_json(scope: InternalModelSemanticScope): Json =
    Json.obj(
      "componentIdentity" -> Json.fromString(scope.componentIdentity),
      "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity),
      "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity)
    )

  private def _target_json(target: InternalModelCandidateCmlTarget): Json =
    Json.obj(
      "baselineArtifactId" -> Json.fromString(target.baselineArtifactId),
      "effects" -> Json.fromValues(target.effects.map(_effect_json)),
      "mappings" -> Json.fromValues(target.mappings.map(_mapping_json)),
      "patchIdentity" -> Json.fromString(target.patchIdentity),
      "projectRelativePath" -> Json.fromString(target.projectRelativePath),
      "proposedContent" -> _content_json(target.proposedContent),
      "source" -> _source_json(target.source),
      "targetId" -> Json.fromString(target.targetId)
    )

  private def _content_json(content: InternalModelCandidateCmlContent): Json =
    Json.obj(
      "byteLength" -> Json.fromLong(content.byteLength),
      "rawBytesBase64" -> Json.fromString(content.rawBytesBase64),
      "sha256" -> Json.fromString(content.sha256)
    )

  private def _source_json(source: InternalModelSemanticSource): Json =
    Json.obj(
      "authority" -> Json.fromString(source.authority),
      "identity" -> Json.fromString(source.identity),
      "locator" -> source.locator.map(Json.fromString).getOrElse(Json.Null),
      "revision" -> source.revision.map(Json.fromString).getOrElse(Json.Null),
      "sha256" -> Json.fromString(source.sha256)
    )

  private def _mapping_json(mapping: InternalModelCandidateCmlMapping): Json =
    Json.obj(
      "canonicalAssertionIds" -> Json.fromValues(mapping.canonicalAssertionIds.map(Json.fromString)),
      "cmlAnchor" -> Json.fromString(mapping.cmlAnchor),
      "cmlSemanticIdentity" -> Json.fromString(mapping.cmlSemanticIdentity),
      "conditionIds" -> Json.fromValues(mapping.conditionIds.map(Json.fromString)),
      "enrichmentAssertionIds" -> Json.fromValues(mapping.enrichmentAssertionIds.map(Json.fromString)),
      "mappingId" -> Json.fromString(mapping.mappingId),
      "semanticIdentity" -> Json.fromString(mapping.semanticIdentity),
      "semanticIdentityKind" -> Json.fromString(mapping.semanticIdentityKind)
    )

  private def _effect_json(effect: InternalModelCandidateCmlEffect): Json =
    Json.obj(
      "assessment" -> Json.fromString(effect.assessment),
      "detail" -> Json.fromString(effect.detail),
      "effectId" -> Json.fromString(effect.effectId),
      "kind" -> Json.fromString(effect.kind),
      "mappingIds" -> Json.fromValues(effect.mappingIds.map(Json.fromString)),
      "sourceReferenceId" -> Json.fromString(effect.sourceReferenceId)
    )

  private def _object(value: Json, label: String): Either[String, JsonObject] =
    value.asObject.toRight(s"$label must be an object")

  private def _array(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[Json]] =
    objectvalue(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")

  private def _string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    objectvalue(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")

  private def _nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, String] =
    _string(objectvalue, key, label).flatMap(value => Either.cond(value.nonEmpty, value, s"$label $key must be nonempty"))

  private def _nullable_nonempty_string(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] =
    objectvalue(key) match {
      case Some(value) if value.isNull => Right(None)
      case Some(value) => value.asString.filter(_.nonEmpty).map(Some(_)).toRight(s"$label $key must be null or a nonempty string")
      case None => Left(s"$label $key is missing")
    }

  private def _sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    _strings(objectvalue, key, label).flatMap { values =>
      _strictly_sorted(values.map(value => Vector(value)), s"$label $key").map(_ => values)
    }

  private def _nonempty_sorted_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    _sorted_strings(objectvalue, key, label).flatMap(values => Either.cond(values.nonEmpty, values, s"$label $key must be nonempty"))

  private def _strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    _array(objectvalue, key, label).flatMap { values =>
      values.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          stringvalue <- value.asString.filter(_.nonEmpty).toRight(s"$label $key $index must be a nonempty string")
        } yield collected :+ stringvalue
      }
    }

  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] =
    Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")

  private def _unique(values: Vector[String], label: String): Either[String, Unit] =
    Either.cond(values.distinct.size == values.size, (), s"$label must be unique")

  private def _strictly_sorted(values: Vector[Vector[String]], label: String): Either[String, Unit] =
    Either.cond(values.zip(values.drop(1)).forall { case (left, right) => _compare_tuple(left, right) < 0 }, (), s"$label must be unique and sorted by ascending UTF-8-byte order")

  private def _compare_tuple(left: Vector[String], right: Vector[String]): Int =
    left.zip(right).iterator.map { case (leftvalue, rightvalue) => _compare_text(leftvalue, rightvalue) }.find(_ != 0).getOrElse(left.length.compare(right.length))

  private def _compare_text(left: String, right: String): Int =
    Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))

  private def _decode_base64(value: String, label: String): Either[String, Array[Byte]] =
    try Right(Base64.getDecoder.decode(value))
    catch {
      case NonFatal(_) => Left(s"$label rawBytesBase64 is invalid")
    }

  private def _canonical_base64(bytes: Array[Byte]): String =
    Base64.getEncoder.encodeToString(bytes)

  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] =
    try {
      val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      Right(decoder.decode(ByteBuffer.wrap(bytes)).toString)
    } catch {
      case NonFatal(_) => Left(s"$label is not valid UTF-8")
    }

  private def _has_bom(bytes: Array[Byte]): Boolean =
    bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte

  private def _canonical_bytes(json: Json): Array[Byte] =
    (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)

  private def _sha256(bytes: Array[Byte]): String =
    "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
}
