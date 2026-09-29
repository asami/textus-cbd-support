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
/** Strict canonical codec for durable, non-approval candidate-review evidence. */
private[runtime] object InternalModelCandidateReviewBindingCodec {
  private val _root_fields = Set("candidateArtifact", "candidateIdentity", "candidateModelIdentity", "candidateRevision", "continuityArtifact", "evidenceArtifacts", "profile", "providers", "realizationArtifact", "realizationIdentity", "reviewIdentity", "reviewRevision", "reviewedPackageManifest", "rules", "schemaVersion", "scope", "semanticDiffArtifact", "semanticDiffIdentity", "semanticDiffRevision", "targets")
  private val _artifact_fields = Set("artifactId", "sha256")
  private val _rule_fields = Set("ruleId", "ruleVersion", "sha256")
  private val _provider_fields = Set("providerId", "providerVersion", "sha256")
  private val _content_fields = Set("byteLength", "rawBytesBase64", "sha256")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _target_fields = Set("evidenceArtifactIds", "reviewSnapshot", "targetId")
  private val _snapshot_fields = Set("attribution", "candidateModelId", "component", "condition", "context", "id", "limitations", "patchId", "stableTieKey", "state")
  private val _attribution_fields = Set("authorityScope", "sourceId", "sourceLocator")
  private val _condition_fields = Set("ambiguity", "authorization", "availability", "conflict", "explicitAbsence", "limitations", "malformedEvidence", "redaction", "staleness")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateReviewBinding] = {
    val raw = bytes.toArray
    for {
      _ <- Either.cond(!_has_bom(raw), (), "candidate review bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(raw, "candidate review bytes")
      json <- _json_parser.parse(content).left.map(_ => "candidate review bytes must be valid JSON without duplicate members")
      root <- json.asObject.toRight("candidate review root must be an object")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(raw, canonical), (), "candidate review bytes are not canonical JSON")
      _ <- _closed_fields(root, _root_fields, "candidate review root")
      candidateartifact <- _artifact(root, "candidateArtifact", "candidate review root")
      candidateidentity <- _nonblank(root, "candidateIdentity", "candidate review root")
      candidatemodelidentity <- _nonblank(root, "candidateModelIdentity", "candidate review root")
      candidaterevision <- _positive_int(root, "candidateRevision", "candidate review root")
      continuityartifact <- _artifact(root, "continuityArtifact", "candidate review root")
      evidence <- _artifacts(root, "evidenceArtifacts", "candidate review root", nonempty = true)
      profile <- _nonblank(root, "profile", "candidate review root")
      _ <- Either.cond(profile == "ccdm-candidate-review-binding-v1", (), "candidate review profile is unsupported")
      providers <- _providers(root)
      realizationartifact <- _artifact(root, "realizationArtifact", "candidate review root")
      realizationidentity <- _nonblank(root, "realizationIdentity", "candidate review root")
      reviewidentity <- _nonblank(root, "reviewIdentity", "candidate review root")
      reviewrevision <- _positive_int(root, "reviewRevision", "candidate review root")
      reviewedmanifest <- _content(root, "reviewedPackageManifest", "candidate review root")
      rules <- _rules(root)
      schema <- _nonblank(root, "schemaVersion", "candidate review root")
      _ <- Either.cond(schema == "1.0", (), "candidate review schemaVersion is unsupported")
      scope <- _scope(root)
      diffartifact <- _artifact(root, "semanticDiffArtifact", "candidate review root")
      diffidentity <- _nonblank(root, "semanticDiffIdentity", "candidate review root")
      diffrevision <- _positive_int(root, "semanticDiffRevision", "candidate review root")
      targets <- _targets(root, evidence)
      value = InternalModelCandidateReviewBinding(candidateartifact, candidateidentity, candidatemodelidentity, candidaterevision, continuityartifact, evidence, profile, providers, realizationartifact, realizationidentity, reviewidentity, reviewrevision, reviewedmanifest, rules, schema, scope, diffartifact, diffidentity, diffrevision, targets, Vector.empty)
      encoded = encode(value).toVector
      _ <- Either.cond(encoded == canonical.toVector, (), "typed candidate review re-encoding does not match supplied canonical bytes")
    } yield value.copy(canonicalBytes = encoded)
  }

  def encode(value: InternalModelCandidateReviewBinding): Array[Byte] =
    _canonical_bytes(Json.obj(
      "candidateArtifact" -> _artifact_json(value.candidateArtifact),
      "candidateIdentity" -> Json.fromString(value.candidateIdentity),
      "candidateModelIdentity" -> Json.fromString(value.candidateModelIdentity),
      "candidateRevision" -> Json.fromInt(value.candidateRevision),
      "continuityArtifact" -> _artifact_json(value.continuityArtifact),
      "evidenceArtifacts" -> Json.fromValues(value.evidenceArtifacts.map(_artifact_json)),
      "profile" -> Json.fromString(value.profile),
      "providers" -> Json.fromValues(value.providers.map(_provider_json)),
      "realizationArtifact" -> _artifact_json(value.realizationArtifact),
      "realizationIdentity" -> Json.fromString(value.realizationIdentity),
      "reviewIdentity" -> Json.fromString(value.reviewIdentity),
      "reviewRevision" -> Json.fromInt(value.reviewRevision),
      "reviewedPackageManifest" -> _content_json(value.reviewedPackageManifest),
      "rules" -> Json.fromValues(value.rules.map(_rule_json)),
      "schemaVersion" -> Json.fromString(value.schemaVersion),
      "scope" -> _scope_json(value.scope),
      "semanticDiffArtifact" -> _artifact_json(value.semanticDiffArtifact),
      "semanticDiffIdentity" -> Json.fromString(value.semanticDiffIdentity),
      "semanticDiffRevision" -> Json.fromInt(value.semanticDiffRevision),
      "targets" -> Json.fromValues(value.targets.map(_target_json))
    ))

  private[runtime] def validateExecutionBasis(
    basis: InternalModelCandidateReviewExecutionBasis
  ): Either[String, Unit] =
    for {
      _ <- _typed_basis_entries(basis.rules.map(value => (value.ruleId, value.ruleVersion, value.sha256)), "rules")
      _ <- _typed_basis_entries(basis.providers.map(value => (value.providerId, value.providerVersion, value.sha256)), "providers")
    } yield ()

  private def _artifact(root: JsonObject, key: String, label: String): Either[String, InternalModelCandidateReviewArtifact] =
    for {
      value <- root(key).toRight(s"$label $key is missing")
      objectvalue <- _object(value, s"$label $key")
      _ <- _closed_fields(objectvalue, _artifact_fields, s"$label $key")
      id <- _nonblank(objectvalue, "artifactId", s"$label $key")
      digest <- _digest(objectvalue, "sha256", s"$label $key")
    } yield InternalModelCandidateReviewArtifact(id, digest)

  private def _artifacts(root: JsonObject, key: String, label: String, nonempty: Boolean): Either[String, Vector[InternalModelCandidateReviewArtifact]] =
    for {
      values <- _array(root, key, label)
      artifacts <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateReviewArtifact]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          objectvalue <- _object(value, s"$label $key $index")
          _ <- _closed_fields(objectvalue, _artifact_fields, s"$label $key $index")
          id <- _nonblank(objectvalue, "artifactId", s"$label $key $index")
          digest <- _digest(objectvalue, "sha256", s"$label $key $index")
        } yield collected :+ InternalModelCandidateReviewArtifact(id, digest)
      }
      _ <- Either.cond(!nonempty || artifacts.nonEmpty, (), s"$label $key must be nonempty")
      _ <- _strictly_sorted(artifacts.map(_.artifactId), s"$label $key")
    } yield artifacts

  private def _rules(root: JsonObject): Either[String, Vector[InternalModelCandidateReviewRule]] =
    _basis_entries(root, "rules", _rule_fields, "ruleId", "ruleVersion", (id, version, digest) => InternalModelCandidateReviewRule(id, version, digest), "candidate review root")

  private def _providers(root: JsonObject): Either[String, Vector[InternalModelCandidateReviewProvider]] =
    _basis_entries(root, "providers", _provider_fields, "providerId", "providerVersion", (id, version, digest) => InternalModelCandidateReviewProvider(id, version, digest), "candidate review root")

  private def _basis_entries[A](
    root: JsonObject,
    key: String,
    fields: Set[String],
    idkey: String,
    versionkey: String,
    create: (String, String, String) => A,
    label: String
  ): Either[String, Vector[A]] =
    for {
      values <- _array(root, key, label)
      entries <- values.zipWithIndex.foldLeft[Either[String, Vector[(String, A)]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for {
          collected <- result
          objectvalue <- _object(value, s"$label $key $index")
          _ <- _closed_fields(objectvalue, fields, s"$label $key $index")
          id <- _nonblank(objectvalue, idkey, s"$label $key $index")
          version <- _nonblank(objectvalue, versionkey, s"$label $key $index")
          digest <- _digest(objectvalue, "sha256", s"$label $key $index")
        } yield collected :+ (id -> create(id, version, digest))
      }
      _ <- Either.cond(entries.nonEmpty, (), s"$label $key must be nonempty")
      _ <- _strictly_sorted(entries.map(_._1), s"$label $key")
    } yield entries.map(_._2)

  private def _typed_basis_entries(
    values: Vector[(String, String, String)],
    label: String
  ): Either[String, Unit] =
    for {
      _ <- Either.cond(values.nonEmpty, (), s"expected candidate review $label must be nonempty")
      _ <- Either.cond(values.forall { case (id, version, digest) => id.trim.nonEmpty && version.trim.nonEmpty && _digest_pattern.matches(digest) }, (), s"expected candidate review $label contains a malformed ID, version, or digest")
      _ <- _strictly_sorted(values.map(_._1), s"expected candidate review $label")
    } yield ()

  private def _content(root: JsonObject, key: String, label: String): Either[String, InternalModelCandidateCmlContent] =
    for {
      value <- root(key).toRight(s"$label $key is missing")
      objectvalue <- _object(value, s"$label $key")
      _ <- _closed_fields(objectvalue, _content_fields, s"$label $key")
      length <- objectvalue("byteLength").flatMap(_.asNumber).flatMap(_.toLong).filter(_ >= 0).toRight(s"$label $key byteLength must be a nonnegative JSON integer")
      encoded <- _string(objectvalue, "rawBytesBase64", s"$label $key")
      raw <- _base64(encoded, s"$label $key")
      digest <- _digest(objectvalue, "sha256", s"$label $key")
      _ <- Either.cond(raw.length.toLong == length, (), s"$label $key byteLength does not match raw bytes")
      _ <- Either.cond(_sha256(raw) == digest, (), s"$label $key sha256 does not match raw bytes")
    } yield InternalModelCandidateCmlContent(length, encoded, digest)

  private def _scope(root: JsonObject): Either[String, InternalModelSemanticScope] =
    for {
      value <- root("scope").toRight("candidate review scope is missing")
      objectvalue <- _object(value, "candidate review scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "candidate review scope")
      component <- _nonblank(objectvalue, "componentIdentity", "candidate review scope")
      context <- _nonblank(objectvalue, "projectionContextIdentity", "candidate review scope")
      usecase <- _nonblank(objectvalue, "selectedUseCaseElementIdentity", "candidate review scope")
    } yield InternalModelSemanticScope(component, context, usecase)

  private def _targets(root: JsonObject, evidence: Vector[InternalModelCandidateReviewArtifact]): Either[String, Vector[InternalModelCandidateReviewTarget]] =
    for {
      values <- _array(root, "targets", "candidate review root")
      _ <- Either.cond(values.nonEmpty, (), "candidate review targets must be nonempty")
      targets <- values.zipWithIndex.foldLeft[Either[String, Vector[InternalModelCandidateReviewTarget]]](Right(Vector.empty)) { case (result, (value, index)) =>
        for { collected <- result; target <- _target(value, index) } yield collected :+ target
      }
      _ <- _strictly_sorted(targets.map(_.targetId), "candidate review targets")
      _ <- _unique(targets.map(_.reviewSnapshot.id), "candidate review snapshot IDs")
      evidenceids = evidence.map(_.artifactId).toSet
      targetids = targets.flatMap(_.evidenceArtifactIds)
      _ <- Either.cond(targetids.toSet == evidenceids, (), "candidate review target evidence IDs must cover the complete evidence artifact set")
    } yield targets

  private def _target(value: Json, index: Int): Either[String, InternalModelCandidateReviewTarget] =
    for {
      objectvalue <- _object(value, s"candidate review target $index")
      _ <- _closed_fields(objectvalue, _target_fields, s"candidate review target $index")
      evidence <- _nonempty_sorted_strings(objectvalue, "evidenceArtifactIds", s"candidate review target $index")
      snapshotvalue <- objectvalue("reviewSnapshot").toRight(s"candidate review target $index reviewSnapshot is missing")
      snapshot <- _snapshot(snapshotvalue, index)
      targetid <- _nonblank(objectvalue, "targetId", s"candidate review target $index")
    } yield InternalModelCandidateReviewTarget(evidence, snapshot, targetid)

  private def _snapshot(value: Json, targetindex: Int): Either[String, CandidateDesignReviewSnapshot] =
    for {
      objectvalue <- _object(value, s"candidate review target $targetindex reviewSnapshot")
      _ <- _closed_fields(objectvalue, _snapshot_fields, s"candidate review target $targetindex reviewSnapshot")
      attribution <- _attribution(objectvalue, s"candidate review target $targetindex reviewSnapshot")
      candidate <- _nonblank(objectvalue, "candidateModelId", s"candidate review target $targetindex reviewSnapshot")
      component <- _nonblank(objectvalue, "component", s"candidate review target $targetindex reviewSnapshot")
      condition <- _condition(objectvalue, s"candidate review target $targetindex reviewSnapshot")
      context <- _nonblank(objectvalue, "context", s"candidate review target $targetindex reviewSnapshot")
      id <- _nonblank(objectvalue, "id", s"candidate review target $targetindex reviewSnapshot")
      limitations <- _nonblank_strings(objectvalue, "limitations", s"candidate review target $targetindex reviewSnapshot")
      patch <- _nonblank(objectvalue, "patchId", s"candidate review target $targetindex reviewSnapshot")
      tie <- _nullable_nonblank(objectvalue, "stableTieKey", s"candidate review target $targetindex reviewSnapshot")
      state <- _nonblank(objectvalue, "state", s"candidate review target $targetindex reviewSnapshot")
    } yield CandidateDesignReviewSnapshot(id, MonoKotoProjectionContextIdentity(context), ComponentDashboardComponentIdentity(component), patch, candidate, state, attribution, condition, limitations, tie)

  private def _attribution(root: JsonObject, label: String): Either[String, ComponentDashboardSourceAttribution] =
    for {
      value <- root("attribution").toRight(s"$label attribution is missing")
      objectvalue <- _object(value, s"$label attribution")
      _ <- _closed_fields(objectvalue, _attribution_fields, s"$label attribution")
      authority <- _nonblank(objectvalue, "authorityScope", s"$label attribution")
      source <- _nonblank(objectvalue, "sourceId", s"$label attribution")
      locator <- _nonblank(objectvalue, "sourceLocator", s"$label attribution")
    } yield ComponentDashboardSourceAttribution(source, authority, locator)

  private def _condition(root: JsonObject, label: String): Either[String, ComponentDashboardCondition] =
    for {
      value <- root("condition").toRight(s"$label condition is missing")
      objectvalue <- _object(value, s"$label condition")
      _ <- _closed_fields(objectvalue, _condition_fields, s"$label condition")
      ambiguity <- _nullable_nonblank(objectvalue, "ambiguity", s"$label condition")
      authorization <- _nonblank(objectvalue, "authorization", s"$label condition")
      availability <- _nonblank(objectvalue, "availability", s"$label condition")
      conflict <- _nullable_nonblank(objectvalue, "conflict", s"$label condition")
      absence <- _nullable_nonblank(objectvalue, "explicitAbsence", s"$label condition")
      limitations <- _nonblank_strings(objectvalue, "limitations", s"$label condition")
      malformed <- _nullable_nonblank(objectvalue, "malformedEvidence", s"$label condition")
      redaction <- _nullable_nonblank(objectvalue, "redaction", s"$label condition")
      staleness <- _nullable_nonblank(objectvalue, "staleness", s"$label condition")
    } yield ComponentDashboardCondition(availability, authorization, redaction, absence, ambiguity, conflict, staleness, malformed, limitations)

  private def _artifact_json(value: InternalModelCandidateReviewArtifact): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId), "sha256" -> Json.fromString(value.sha256))
  private def _rule_json(value: InternalModelCandidateReviewRule): Json = Json.obj("ruleId" -> Json.fromString(value.ruleId), "ruleVersion" -> Json.fromString(value.ruleVersion), "sha256" -> Json.fromString(value.sha256))
  private def _provider_json(value: InternalModelCandidateReviewProvider): Json = Json.obj("providerId" -> Json.fromString(value.providerId), "providerVersion" -> Json.fromString(value.providerVersion), "sha256" -> Json.fromString(value.sha256))
  private def _content_json(value: InternalModelCandidateCmlContent): Json = Json.obj("byteLength" -> Json.fromLong(value.byteLength), "rawBytesBase64" -> Json.fromString(value.rawBytesBase64), "sha256" -> Json.fromString(value.sha256))
  private def _scope_json(value: InternalModelSemanticScope): Json = Json.obj("componentIdentity" -> Json.fromString(value.componentIdentity), "projectionContextIdentity" -> Json.fromString(value.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(value.selectedUseCaseElementIdentity))
  private def _target_json(value: InternalModelCandidateReviewTarget): Json = Json.obj("evidenceArtifactIds" -> Json.fromValues(value.evidenceArtifactIds.map(Json.fromString)), "reviewSnapshot" -> _snapshot_json(value.reviewSnapshot), "targetId" -> Json.fromString(value.targetId))
  private def _snapshot_json(value: CandidateDesignReviewSnapshot): Json = Json.obj("attribution" -> _attribution_json(value.attribution), "candidateModelId" -> Json.fromString(value.candidateModelId), "component" -> Json.fromString(value.component.value), "condition" -> _condition_json(value.condition), "context" -> Json.fromString(value.context.value), "id" -> Json.fromString(value.id), "limitations" -> Json.fromValues(value.limitations.map(Json.fromString)), "patchId" -> Json.fromString(value.patchId), "stableTieKey" -> value.stableTieKey.map(Json.fromString).getOrElse(Json.Null), "state" -> Json.fromString(value.state))
  private def _attribution_json(value: ComponentDashboardSourceAttribution): Json = Json.obj("authorityScope" -> Json.fromString(value.authorityScope), "sourceId" -> Json.fromString(value.sourceId), "sourceLocator" -> Json.fromString(value.sourceLocator))
  private def _condition_json(value: ComponentDashboardCondition): Json = Json.obj("ambiguity" -> _nullable_json(value.ambiguity), "authorization" -> Json.fromString(value.authorization), "availability" -> Json.fromString(value.availability), "conflict" -> _nullable_json(value.conflict), "explicitAbsence" -> _nullable_json(value.explicitAbsence), "limitations" -> Json.fromValues(value.limitations.map(Json.fromString)), "malformedEvidence" -> _nullable_json(value.malformedEvidence), "redaction" -> _nullable_json(value.redaction), "staleness" -> _nullable_json(value.staleness))
  private def _nullable_json(value: Option[String]): Json = value.map(Json.fromString).getOrElse(Json.Null)

  private def _object(value: Json, label: String): Either[String, JsonObject] = value.asObject.toRight(s"$label must be an object")
  private def _array(root: JsonObject, key: String, label: String): Either[String, Vector[Json]] = root(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")
  private def _string(root: JsonObject, key: String, label: String): Either[String, String] = root(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")
  private def _nonblank(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(value.trim.nonEmpty, value, s"$label $key must be a nonblank JSON string"))
  private def _nullable_nonblank(root: JsonObject, key: String, label: String): Either[String, Option[String]] = root(key) match { case Some(value) if value.isNull => Right(None); case Some(value) => value.asString.filter(_.trim.nonEmpty).map(Some(_)).toRight(s"$label $key must be null or a nonblank JSON string"); case None => Left(s"$label $key is missing") }
  private def _nonblank_strings(root: JsonObject, key: String, label: String): Either[String, Vector[String]] = _array(root, key, label).flatMap(_.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) => for { collected <- result; stringvalue <- value.asString.filter(_.trim.nonEmpty).toRight(s"$label $key $index must be a nonblank JSON string") } yield collected :+ stringvalue })
  private def _nonempty_sorted_strings(root: JsonObject, key: String, label: String): Either[String, Vector[String]] = _nonblank_strings(root, key, label).flatMap(values => Either.cond(values.nonEmpty, values, s"$label $key must be nonempty").flatMap(values => _strictly_sorted(values, s"$label $key").map(_ => values)))
  private def _digest(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(_digest_pattern.matches(value), value, s"$label $key must be a lowercase sha256 digest"))
  private def _positive_int(root: JsonObject, key: String, label: String): Either[String, Int] = root(key).flatMap(_.asNumber).flatMap(_.toInt).filter(value => value > 0 && root(key).flatMap(_.asNumber).exists(number => number.toString == value.toString)).toRight(s"$label $key must be a positive canonical JSON integer")
  private def _closed_fields(root: JsonObject, fields: Set[String], label: String): Either[String, Unit] = Either.cond(root.keys.toSet == fields, (), s"$label fields are not closed")
  private def _unique(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.distinct.size == values.size, (), s"$label must be unique")
  private def _strictly_sorted(values: Vector[String], label: String): Either[String, Unit] = Either.cond(values.zip(values.drop(1)).forall { case (left, right) => _compare_text(left, right) < 0 }, (), s"$label must be unique and sorted by ascending UTF-8-byte order")
  private def _compare_text(left: String, right: String): Int = Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8))
  private def _base64(value: String, label: String): Either[String, Array[Byte]] = try { val raw = Base64.getDecoder.decode(value); Either.cond(Base64.getEncoder.encodeToString(raw) == value, raw, s"$label rawBytesBase64 is not canonical padded Base64") } catch { case NonFatal(_) => Left(s"$label rawBytesBase64 is invalid") }
  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] = try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) } catch { case NonFatal(_) => Left(s"$label is not valid UTF-8") }
  private def _has_bom(bytes: Array[Byte]): Boolean = bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte
  private def _canonical_bytes(json: Json): Array[Byte] = (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
  private def _sha256(bytes: Array[Byte]): String = "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).map(byte => f"${byte & 0xff}%02x").mkString
}
