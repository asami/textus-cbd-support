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
/** Strict canonical codec for the persisted Phase 9 semantic-diff projection. */
private[runtime] object InternalModelSemanticDiffCodec {
  private val _root_fields = Set("candidateArtifactId", "candidateArtifactSha256", "candidateIdentity", "candidateModelIdentity", "candidateRevision", "profile", "schemaVersion", "scope", "semanticDiffIdentity", "semanticDiffRevision", "targets")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _target_fields = Set("entries", "patchTrace", "targetId")
  private val _mapped_entry_fields = Set("entry", "mappingId", "semanticIdentityKind")
  private val _entry_fields = Set("action", "after", "attribution", "before", "candidateModelId", "category", "component", "condition", "context", "id", "limitations", "patchId", "relationship", "stableTieKey", "subject")
  private val _patch_fields = Set("attribution", "baseDigest", "cmlLocator", "cmlOwner", "component", "condition", "context", "id", "limitations", "proposedDigest", "stableTieKey")
  private val _attribution_fields = Set("authorityScope", "sourceId", "sourceLocator")
  private val _condition_fields = Set("ambiguity", "authorization", "availability", "conflict", "explicitAbsence", "limitations", "malformedEvidence", "redaction", "staleness")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(projection: InternalModelVerifiedProjection): Either[String, InternalModelSemanticDiff] =
    for {
      _ <- Either.cond(projection.role == "projection", (), "selected semantic diff artifact role must be projection")
      bytes = projection.bytes.toArray
      _ <- Either.cond(!_has_bom(bytes), (), "semantic diff bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(bytes, "semantic diff bytes")
      json <- _json_parser.parse(content).left.map(_ => "semantic diff bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("semantic diff root must be an object")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(bytes, canonical), (), "semantic diff bytes are not canonical JSON")
      _ <- _closed_fields(root, _root_fields, "semantic diff root")
      candidateartifactid <- _nonblank(root, "candidateArtifactId", "semantic diff root")
      candidateartifactsha <- _digest(root, "candidateArtifactSha256", "semantic diff root")
      candidateidentity <- _nonblank(root, "candidateIdentity", "semantic diff root")
      candidatemodelidentity <- _nonblank(root, "candidateModelIdentity", "semantic diff root")
      candidaterevision <- _positive_int(root, "candidateRevision", "semantic diff root")
      profile <- _nonblank(root, "profile", "semantic diff root")
      _ <- Either.cond(profile == "ccdm-semantic-diff-v1", (), "semantic diff profile is unsupported")
      schema <- _nonblank(root, "schemaVersion", "semantic diff root")
      _ <- Either.cond(schema == "1.0", (), "semantic diff schemaVersion is unsupported")
      scope <- _scope(root)
      diffidentity <- _nonblank(root, "semanticDiffIdentity", "semantic diff root")
      diffrevision <- _positive_int(root, "semanticDiffRevision", "semantic diff root")
      targets <- _targets(root)
      value = InternalModelSemanticDiff(candidateartifactid, candidateartifactsha, candidateidentity, candidatemodelidentity, candidaterevision, profile, schema, scope, diffidentity, diffrevision, targets, Vector.empty)
      encoded = encode(value).toVector
      _ <- Either.cond(encoded == canonical.toVector, (), "typed semantic diff re-encoding does not match supplied canonical bytes")
    } yield value.copy(canonicalBytes = encoded)

  def encode(value: InternalModelSemanticDiff): Array[Byte] =
    _canonical_bytes(Json.obj(
      "candidateArtifactId" -> Json.fromString(value.candidateArtifactId),
      "candidateArtifactSha256" -> Json.fromString(value.candidateArtifactSha256),
      "candidateIdentity" -> Json.fromString(value.candidateIdentity),
      "candidateModelIdentity" -> Json.fromString(value.candidateModelIdentity),
      "candidateRevision" -> Json.fromInt(value.candidateRevision),
      "profile" -> Json.fromString(value.profile),
      "schemaVersion" -> Json.fromString(value.schemaVersion),
      "scope" -> _scope_json(value.scope),
      "semanticDiffIdentity" -> Json.fromString(value.semanticDiffIdentity),
      "semanticDiffRevision" -> Json.fromInt(value.semanticDiffRevision),
      "targets" -> Json.fromValues(value.targets.map(_target_json))
    ))

  private def _scope(root: JsonObject): Either[String, InternalModelSemanticScope] =
    for {
      value <- root("scope").toRight("semantic diff scope is missing")
      objectvalue <- _object(value, "semantic diff scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "semantic diff scope")
      component <- _nonblank(objectvalue, "componentIdentity", "semantic diff scope")
      context <- _nonblank(objectvalue, "projectionContextIdentity", "semantic diff scope")
      usecase <- _nonblank(objectvalue, "selectedUseCaseElementIdentity", "semantic diff scope")
    } yield InternalModelSemanticScope(component, context, usecase)

  private def _targets(root: JsonObject): Either[String, Vector[InternalModelSemanticDiffTarget]] =
    for {
      values <- _array(root, "targets", "semantic diff root")
      _ <- Either.cond(values.nonEmpty, (), "semantic diff targets must be nonempty")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticDiffTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for { collected <- result; target <- _target(value, index) } yield collected :+ target
      }
      _ <- _strictly_sorted(targets.map(_.targetId), "semantic diff targets")
      _ <- _unique(targets.flatMap(_.entries.map(_.entry.id)), "semantic diff entry IDs")
    } yield targets

  private def _target(value: Json, index: Int): Either[String, InternalModelSemanticDiffTarget] =
    for {
      objectvalue <- _object(value, s"semantic diff target $index")
      _ <- _closed_fields(objectvalue, _target_fields, s"semantic diff target $index")
      entries <- _entries(objectvalue, index)
      patchvalue <- objectvalue("patchTrace").toRight(s"semantic diff target $index patchTrace is missing")
      patch <- _patch(patchvalue, index)
      targetid <- _nonblank(objectvalue, "targetId", s"semantic diff target $index")
    } yield InternalModelSemanticDiffTarget(entries, patch, targetid)

  private def _entries(target: JsonObject, targetindex: Int): Either[String, Vector[InternalModelSemanticDiffMappedEntry]] =
    for {
      values <- _array(target, "entries", s"semantic diff target $targetindex")
      _ <- Either.cond(values.nonEmpty, (), s"semantic diff target $targetindex entries must be nonempty")
      entries <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelSemanticDiffMappedEntry]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for { collected <- result; entry <- _mapped_entry(value, targetindex, index) } yield collected :+ entry
      }
      _ <- _strictly_sorted(entries.map(_.entry.id), s"semantic diff target $targetindex entries")
    } yield entries

  private def _mapped_entry(value: Json, targetindex: Int, index: Int): Either[String, InternalModelSemanticDiffMappedEntry] =
    for {
      objectvalue <- _object(value, s"semantic diff target $targetindex entry $index")
      _ <- _closed_fields(objectvalue, _mapped_entry_fields, s"semantic diff target $targetindex entry $index")
      entryvalue <- objectvalue("entry").toRight(s"semantic diff target $targetindex entry $index entry is missing")
      entry <- _entry(entryvalue, targetindex, index)
      mappingid <- _nonblank(objectvalue, "mappingId", s"semantic diff target $targetindex entry $index")
      kind <- _nonblank(objectvalue, "semanticIdentityKind", s"semantic diff target $targetindex entry $index")
      _ <- Either.cond(Set("element", "relationship").contains(kind), (), s"semantic diff target $targetindex entry $index semanticIdentityKind is invalid")
    } yield InternalModelSemanticDiffMappedEntry(entry, mappingid, kind)

  private def _entry(value: Json, targetindex: Int, index: Int): Either[String, CandidateDesignSemanticDiffEntry] =
    for {
      objectvalue <- _object(value, s"semantic diff target $targetindex entry $index entry")
      _ <- _closed_fields(objectvalue, _entry_fields, s"semantic diff target $targetindex entry $index entry")
      action <- _nonblank(objectvalue, "action", "semantic diff entry")
      after <- _nullable_nonblank(objectvalue, "after", "semantic diff entry")
      attribution <- _attribution(objectvalue, "semantic diff entry")
      before <- _nullable_nonblank(objectvalue, "before", "semantic diff entry")
      candidate <- _nonblank(objectvalue, "candidateModelId", "semantic diff entry")
      category <- _nonblank(objectvalue, "category", "semantic diff entry")
      component <- _component(objectvalue, "component", "semantic diff entry")
      condition <- _condition(objectvalue, "condition", "semantic diff entry")
      context <- _context(objectvalue, "context", "semantic diff entry")
      id <- _nonblank(objectvalue, "id", "semantic diff entry")
      limitations <- _nonblank_strings(objectvalue, "limitations", "semantic diff entry")
      patch <- _nonblank(objectvalue, "patchId", "semantic diff entry")
      relationship <- _nonblank(objectvalue, "relationship", "semantic diff entry")
      tie <- _nullable_nonblank(objectvalue, "stableTieKey", "semantic diff entry")
      subject <- _nonblank(objectvalue, "subject", "semantic diff entry")
      _ <- Either.cond((before.nonEmpty && after.nonEmpty) || condition.explicitAbsence.nonEmpty, (), "semantic diff null before or after requires explicitAbsence")
    } yield CandidateDesignSemanticDiffEntry(id, context, component, patch, candidate, category, action, subject, before, after, relationship, attribution, condition, limitations, tie)

  private def _patch(value: Json, targetindex: Int): Either[String, CandidateDesignProposedCmlPatchTrace] =
    for {
      objectvalue <- _object(value, s"semantic diff target $targetindex patchTrace")
      _ <- _closed_fields(objectvalue, _patch_fields, s"semantic diff target $targetindex patchTrace")
      attribution <- _attribution(objectvalue, "semantic diff patchTrace")
      base <- _digest(objectvalue, "baseDigest", "semantic diff patchTrace")
      locator <- _nonblank(objectvalue, "cmlLocator", "semantic diff patchTrace")
      owner <- _nonblank(objectvalue, "cmlOwner", "semantic diff patchTrace")
      component <- _component(objectvalue, "component", "semantic diff patchTrace")
      condition <- _condition(objectvalue, "condition", "semantic diff patchTrace")
      context <- _context(objectvalue, "context", "semantic diff patchTrace")
      id <- _nonblank(objectvalue, "id", "semantic diff patchTrace")
      limitations <- _nonblank_strings(objectvalue, "limitations", "semantic diff patchTrace")
      proposed <- _digest(objectvalue, "proposedDigest", "semantic diff patchTrace")
      tie <- _nullable_nonblank(objectvalue, "stableTieKey", "semantic diff patchTrace")
    } yield CandidateDesignProposedCmlPatchTrace(id, context, component, owner, locator, base, proposed, attribution, condition, limitations, tie)

  private def _attribution(objectvalue: JsonObject, label: String): Either[String, ComponentDashboardSourceAttribution] =
    for {
      value <- objectvalue("attribution").toRight(s"$label attribution is missing")
      attribution <- _object(value, s"$label attribution")
      _ <- _closed_fields(attribution, _attribution_fields, s"$label attribution")
      authority <- _nonblank(attribution, "authorityScope", s"$label attribution")
      source <- _nonblank(attribution, "sourceId", s"$label attribution")
      locator <- _nonblank(attribution, "sourceLocator", s"$label attribution")
    } yield ComponentDashboardSourceAttribution(source, authority, locator)

  private def _condition(objectvalue: JsonObject, key: String, label: String): Either[String, ComponentDashboardCondition] =
    for {
      value <- objectvalue(key).toRight(s"$label $key is missing")
      condition <- _object(value, s"$label $key")
      _ <- _closed_fields(condition, _condition_fields, s"$label $key")
      ambiguity <- _nullable_nonblank(condition, "ambiguity", s"$label $key")
      authorization <- _nonblank(condition, "authorization", s"$label $key")
      availability <- _nonblank(condition, "availability", s"$label $key")
      conflict <- _nullable_nonblank(condition, "conflict", s"$label $key")
      absence <- _nullable_nonblank(condition, "explicitAbsence", s"$label $key")
      limitations <- _nonblank_strings(condition, "limitations", s"$label $key")
      malformed <- _nullable_nonblank(condition, "malformedEvidence", s"$label $key")
      redaction <- _nullable_nonblank(condition, "redaction", s"$label $key")
      staleness <- _nullable_nonblank(condition, "staleness", s"$label $key")
    } yield ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)

  private def _component(objectvalue: JsonObject, key: String, label: String): Either[String, ComponentDashboardComponentIdentity] =
    _nonblank(objectvalue, key, label).map(ComponentDashboardComponentIdentity.apply)

  private def _context(objectvalue: JsonObject, key: String, label: String): Either[String, MonoKotoProjectionContextIdentity] =
    _nonblank(objectvalue, key, label).map(MonoKotoProjectionContextIdentity.apply)

  private def _scope_json(scope: InternalModelSemanticScope): Json = Json.obj("componentIdentity" -> Json.fromString(scope.componentIdentity), "projectionContextIdentity" -> Json.fromString(scope.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(scope.selectedUseCaseElementIdentity))
  private def _target_json(target: InternalModelSemanticDiffTarget): Json = Json.obj("entries" -> Json.fromValues(target.entries.map(_mapped_entry_json)), "patchTrace" -> _patch_json(target.patchTrace), "targetId" -> Json.fromString(target.targetId))
  private def _mapped_entry_json(value: InternalModelSemanticDiffMappedEntry): Json = Json.obj("entry" -> _entry_json(value.entry), "mappingId" -> Json.fromString(value.mappingId), "semanticIdentityKind" -> Json.fromString(value.semanticIdentityKind))
  private def _entry_json(value: CandidateDesignSemanticDiffEntry): Json = Json.obj("action" -> Json.fromString(value.action), "after" -> _nullable_json(value.after), "attribution" -> _attribution_json(value.attribution), "before" -> _nullable_json(value.before), "candidateModelId" -> Json.fromString(value.candidateModelId), "category" -> Json.fromString(value.category), "component" -> Json.fromString(value.component.value), "condition" -> _condition_json(value.condition), "context" -> Json.fromString(value.context.value), "id" -> Json.fromString(value.id), "limitations" -> _strings_json(value.limitations), "patchId" -> Json.fromString(value.patchId), "relationship" -> Json.fromString(value.relationship), "stableTieKey" -> _nullable_json(value.stableTieKey), "subject" -> Json.fromString(value.subject))
  private def _patch_json(value: CandidateDesignProposedCmlPatchTrace): Json = Json.obj("attribution" -> _attribution_json(value.attribution), "baseDigest" -> Json.fromString(value.baseDigest), "cmlLocator" -> Json.fromString(value.cmlLocator), "cmlOwner" -> Json.fromString(value.cmlOwner), "component" -> Json.fromString(value.component.value), "condition" -> _condition_json(value.condition), "context" -> Json.fromString(value.context.value), "id" -> Json.fromString(value.id), "limitations" -> _strings_json(value.limitations), "proposedDigest" -> Json.fromString(value.proposedDigest), "stableTieKey" -> _nullable_json(value.stableTieKey))
  private def _attribution_json(value: ComponentDashboardSourceAttribution): Json = Json.obj("authorityScope" -> Json.fromString(value.authorityScope), "sourceId" -> Json.fromString(value.sourceId), "sourceLocator" -> Json.fromString(value.sourceLocator))
  private def _condition_json(value: ComponentDashboardCondition): Json = Json.obj("ambiguity" -> _nullable_json(value.ambiguity), "authorization" -> Json.fromString(value.authorization), "availability" -> Json.fromString(value.availability), "conflict" -> _nullable_json(value.conflict), "explicitAbsence" -> _nullable_json(value.explicitAbsence), "limitations" -> _strings_json(value.limitations), "malformedEvidence" -> _nullable_json(value.malformedEvidence), "redaction" -> _nullable_json(value.redaction), "staleness" -> _nullable_json(value.staleness))
  private def _strings_json(values: Vector[String]): Json = Json.fromValues(values.map(Json.fromString))
  private def _nullable_json(value: Option[String]): Json = value.map(Json.fromString).getOrElse(Json.Null)

  private def _object(value: Json, label: String): Either[String, JsonObject] = value.asObject.toRight(s"$label must be an object")
  private def _array(value: JsonObject, key: String, label: String): Either[String, Vector[Json]] = value(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")
  private def _nonblank(objectvalue: JsonObject, key: String, label: String): Either[String, String] = objectvalue(key).flatMap(_.asString).filter(_.trim.nonEmpty).toRight(s"$label $key must be a nonblank JSON string")
  private def _nullable_nonblank(objectvalue: JsonObject, key: String, label: String): Either[String, Option[String]] = objectvalue(key) match { case Some(value) if value.isNull => Right(None); case Some(value) => value.asString.filter(_.trim.nonEmpty).map(Some(_)).toRight(s"$label $key must be null or a nonblank JSON string"); case None => Left(s"$label $key is missing") }
  private def _digest(objectvalue: JsonObject, key: String, label: String): Either[String, String] = _nonblank(objectvalue, key, label).flatMap(value => Either.cond(_digest_pattern.matches(value), value, s"$label $key is not a lowercase sha256 digest"))
  private def _positive_int(objectvalue: JsonObject, key: String, label: String): Either[String, Int] = objectvalue(key).flatMap(_.asNumber).flatMap(number => number.toInt.filter(value => value > 0 && number.toString == value.toString)).toRight(s"$label $key must be a canonical positive Int")
  private def _nonblank_strings(objectvalue: JsonObject, key: String, label: String): Either[String, Vector[String]] = _array(objectvalue, key, label).flatMap(_.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) => for { collected <- result; text <- value.asString.filter(_.trim.nonEmpty).toRight(s"$label $key $index must be a nonblank string") } yield collected :+ text })
  private def _closed_fields(objectvalue: JsonObject, fields: Set[String], label: String): Either[String, Unit] = Either.cond(objectvalue.keys.toSet == fields, (), s"$label fields are not closed")
  private def _unique(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.distinct.size == values.size, (), s"$label must be unique")
  private def _strictly_sorted(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.zip(values.drop(1)).forall { case (left, right) => Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)) < 0 }, (), s"$label must be unique and sorted by ascending UTF-8-byte order")
  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] = try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) } catch { case NonFatal(_) => Left(s"$label is not valid UTF-8") }
  private def _has_bom(bytes: Array[Byte]): Boolean = bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte
  private def _canonical_bytes(json: Json): Array[Byte] = (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
