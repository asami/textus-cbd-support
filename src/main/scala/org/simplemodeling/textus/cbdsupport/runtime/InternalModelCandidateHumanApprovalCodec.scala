package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CodingErrorAction, StandardCharsets}
import java.security.MessageDigest
import java.util.Arrays

import scala.util.control.NonFatal

import io.circe.{Json, JsonObject, Printer}
import io.circe.jawn.JawnParser

/*
 * @since   Sep. 29, 2026
 * @version Sep. 29, 2026
 * @author  ASAMI, Tomoharu
 */
/** Strict canonical codec for independently supplied human approval records. */
private[runtime] object InternalModelCandidateHumanApprovalCodec {
  private val _root_fields = Set("approval", "profile", "schemaVersion")
  private val _approval_fields = Set("actor", "approvalIdentity", "approvalRevision", "basis", "decision", "provenance", "rationale", "unresolvedItems")
  private val _actor_fields = Set("identity", "kind", "role")
  private val _basis_fields = Set("candidateArtifact", "candidateIdentity", "candidateModelIdentity", "candidateRevision", "reviewArtifact", "reviewIdentity", "reviewRevision", "reviewedPackage", "scope", "semanticDiffArtifact", "semanticDiffIdentity", "semanticDiffRevision")
  private val _artifact_fields = Set("artifactId", "sha256")
  private val _package_fields = Set("packageDigest", "packageId", "projectId", "projectNamespace", "revision", "schemaVersion")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _source_fields = Set("authority", "identity", "locator", "revision", "sha256")
  private val _digest_pattern = "sha256:[0-9a-f]{64}".r
  private val _uuid_pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r
  private val _token_pattern = "[A-Za-z0-9][A-Za-z0-9._:-]*".r
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _canonical_printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateHumanApproval] = {
    val raw = bytes.toArray
    for {
      _ <- Either.cond(!_has_bom(raw), (), "candidate human approval bytes must not contain a UTF-8 byte-order mark")
      content <- _decode_utf8(raw, "candidate human approval bytes")
      json <- _json_parser.parse(content).left.map(_ => "candidate human approval bytes must be valid JSON without duplicate members")
      root <- _object(json, "candidate human approval root")
      canonical = _canonical_bytes(json)
      _ <- Either.cond(Arrays.equals(raw, canonical), (), "candidate human approval bytes are not canonical JSON")
      _ <- _closed_fields(root, _root_fields, "candidate human approval root")
      approvalvalue <- root("approval").toRight("candidate human approval root approval is missing")
      approval <- _approval(approvalvalue)
      profile <- _nonblank(root, "profile", "candidate human approval root")
      _ <- Either.cond(profile == "ccdm-candidate-human-approval-v1", (), "candidate human approval profile is unsupported")
      schema <- _nonblank(root, "schemaVersion", "candidate human approval root")
      _ <- Either.cond(schema == "1.0", (), "candidate human approval schemaVersion is unsupported")
      value = InternalModelCandidateHumanApproval(approval, profile, schema, Vector.empty)
      encoded = encode(value).toVector
      _ <- Either.cond(encoded == canonical.toVector, (), "typed candidate human approval re-encoding does not match supplied canonical bytes")
    } yield value.copy(canonicalBytes = encoded)
  }

  def encode(value: InternalModelCandidateHumanApproval): Array[Byte] =
    _canonical_bytes(Json.obj(
      "approval" -> _approval_json(value.approval),
      "profile" -> Json.fromString(value.profile),
      "schemaVersion" -> Json.fromString(value.schemaVersion)
    ))

  private def _approval(value: Json): Either[String, InternalModelCandidateHumanApprovalInput] =
    for {
      objectvalue <- _object(value, "candidate human approval")
      _ <- _closed_fields(objectvalue, _approval_fields, "candidate human approval")
      actorvalue <- objectvalue("actor").toRight("candidate human approval actor is missing")
      actor <- _actor(actorvalue)
      identity <- _nonblank(objectvalue, "approvalIdentity", "candidate human approval")
      revision <- _positive_int(objectvalue, "approvalRevision", "candidate human approval")
      basisvalue <- objectvalue("basis").toRight("candidate human approval basis is missing")
      basis <- _basis(basisvalue)
      decisiontoken <- _nonblank(objectvalue, "decision", "candidate human approval")
      decision <- InternalModelCandidateHumanApprovalDecision.fromToken(decisiontoken).toRight("candidate human approval decision is invalid")
      provenancevalue <- objectvalue("provenance").toRight("candidate human approval provenance is missing")
      provenance <- _source(provenancevalue)
      rationale <- _nonblank(objectvalue, "rationale", "candidate human approval")
      items <- _prose(objectvalue, "unresolvedItems", "candidate human approval")
    } yield InternalModelCandidateHumanApprovalInput(actor, identity, revision, basis, decision, provenance, rationale, items)

  private def _actor(value: Json): Either[String, InternalModelDecisionActor] =
    for {
      objectvalue <- _object(value, "candidate human approval actor")
      _ <- _closed_fields(objectvalue, _actor_fields, "candidate human approval actor")
      kind <- _nonblank(objectvalue, "kind", "candidate human approval actor")
      _ <- Either.cond(kind == "human", (), "candidate human approval actor kind must be human")
      identity <- _nonblank(objectvalue, "identity", "candidate human approval actor")
      role <- _nonblank(objectvalue, "role", "candidate human approval actor")
    } yield InternalModelDecisionActor(kind, identity, role)

  private def _basis(value: Json): Either[String, InternalModelCandidateHumanApprovalBasis] =
    for {
      objectvalue <- _object(value, "candidate human approval basis")
      _ <- _closed_fields(objectvalue, _basis_fields, "candidate human approval basis")
      candidateartifact <- _artifact(objectvalue, "candidateArtifact", "candidate human approval basis")
      candidateidentity <- _nonblank(objectvalue, "candidateIdentity", "candidate human approval basis")
      candidatemodelidentity <- _nonblank(objectvalue, "candidateModelIdentity", "candidate human approval basis")
      candidaterevision <- _positive_int(objectvalue, "candidateRevision", "candidate human approval basis")
      reviewartifact <- _artifact(objectvalue, "reviewArtifact", "candidate human approval basis")
      reviewidentity <- _nonblank(objectvalue, "reviewIdentity", "candidate human approval basis")
      reviewrevision <- _positive_int(objectvalue, "reviewRevision", "candidate human approval basis")
      packagevalue <- objectvalue("reviewedPackage").toRight("candidate human approval basis reviewedPackage is missing")
      reviewedpackage <- _reviewed_package(packagevalue)
      scopevalue <- objectvalue("scope").toRight("candidate human approval basis scope is missing")
      scope <- _scope(scopevalue)
      diffartifact <- _artifact(objectvalue, "semanticDiffArtifact", "candidate human approval basis")
      diffidentity <- _nonblank(objectvalue, "semanticDiffIdentity", "candidate human approval basis")
      diffrevision <- _positive_int(objectvalue, "semanticDiffRevision", "candidate human approval basis")
    } yield InternalModelCandidateHumanApprovalBasis(candidateartifact, candidateidentity, candidatemodelidentity, candidaterevision, reviewartifact, reviewidentity, reviewrevision, reviewedpackage, scope, diffartifact, diffidentity, diffrevision)

  private def _artifact(root: JsonObject, key: String, label: String): Either[String, InternalModelCandidateReviewArtifact] =
    for {
      value <- root(key).toRight(s"$label $key is missing")
      objectvalue <- _object(value, s"$label $key")
      _ <- _closed_fields(objectvalue, _artifact_fields, s"$label $key")
      identity <- _nonblank(objectvalue, "artifactId", s"$label $key")
      digest <- _digest(objectvalue, "sha256", s"$label $key")
    } yield InternalModelCandidateReviewArtifact(identity, digest)

  private def _reviewed_package(value: Json): Either[String, InternalModelCandidateHumanApprovalPackageBasis] =
    for {
      objectvalue <- _object(value, "candidate human approval basis reviewedPackage")
      _ <- _closed_fields(objectvalue, _package_fields, "candidate human approval basis reviewedPackage")
      digest <- _digest(objectvalue, "packageDigest", "candidate human approval basis reviewedPackage")
      packageid <- _string(objectvalue, "packageId", "candidate human approval basis reviewedPackage")
      _ <- Either.cond(_uuid_pattern.matches(packageid), (), "candidate human approval basis reviewedPackage packageId must be a lowercase UUID")
      projectid <- _token(objectvalue, "projectId", "candidate human approval basis reviewedPackage")
      namespace <- _token(objectvalue, "projectNamespace", "candidate human approval basis reviewedPackage")
      revision <- _positive_long(objectvalue, "revision", "candidate human approval basis reviewedPackage")
      schema <- _nonblank(objectvalue, "schemaVersion", "candidate human approval basis reviewedPackage")
      _ <- Either.cond(schema == "1.0", (), "candidate human approval basis reviewedPackage schemaVersion is unsupported")
    } yield InternalModelCandidateHumanApprovalPackageBasis(digest, packageid, projectid, namespace, revision, schema)

  private def _scope(value: Json): Either[String, InternalModelSemanticScope] =
    for {
      objectvalue <- _object(value, "candidate human approval basis scope")
      _ <- _closed_fields(objectvalue, _scope_fields, "candidate human approval basis scope")
      component <- _nonblank(objectvalue, "componentIdentity", "candidate human approval basis scope")
      context <- _nonblank(objectvalue, "projectionContextIdentity", "candidate human approval basis scope")
      usecase <- _nonblank(objectvalue, "selectedUseCaseElementIdentity", "candidate human approval basis scope")
    } yield InternalModelSemanticScope(component, context, usecase)

  private def _source(value: Json): Either[String, InternalModelSemanticSource] =
    for {
      objectvalue <- _object(value, "candidate human approval provenance")
      _ <- _closed_fields(objectvalue, _source_fields, "candidate human approval provenance")
      authority <- _nonblank(objectvalue, "authority", "candidate human approval provenance")
      _ <- Either.cond(authority == "human-decision", (), "candidate human approval provenance authority must be human-decision")
      identity <- _nonblank(objectvalue, "identity", "candidate human approval provenance")
      locator <- _nullable_nonblank(objectvalue, "locator", "candidate human approval provenance")
      revision <- _nullable_nonblank(objectvalue, "revision", "candidate human approval provenance")
      digest <- _digest(objectvalue, "sha256", "candidate human approval provenance")
    } yield InternalModelSemanticSource(authority, identity, locator, revision, digest)

  private def _prose(root: JsonObject, key: String, label: String): Either[String, Vector[String]] =
    _array(root, key, label).flatMap(_.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) =>
      for { collected <- result; prose <- value.asString.filter(_is_nonblank_unicode).toRight(s"$label $key $index must be a nonblank Unicode string") } yield collected :+ prose
    })

  private def _approval_json(value: InternalModelCandidateHumanApprovalInput): Json = Json.obj(
    "actor" -> Json.obj("identity" -> Json.fromString(value.actor.identity), "kind" -> Json.fromString(value.actor.kind), "role" -> Json.fromString(value.actor.role)),
    "approvalIdentity" -> Json.fromString(value.approvalIdentity),
    "approvalRevision" -> Json.fromInt(value.approvalRevision),
    "basis" -> _basis_json(value.basis),
    "decision" -> Json.fromString(value.decision.token),
    "provenance" -> _source_json(value.provenance),
    "rationale" -> Json.fromString(value.rationale),
    "unresolvedItems" -> Json.fromValues(value.unresolvedItems.map(Json.fromString))
  )
  private def _basis_json(value: InternalModelCandidateHumanApprovalBasis): Json = Json.obj(
    "candidateArtifact" -> _artifact_json(value.candidateArtifact), "candidateIdentity" -> Json.fromString(value.candidateIdentity), "candidateModelIdentity" -> Json.fromString(value.candidateModelIdentity), "candidateRevision" -> Json.fromInt(value.candidateRevision), "reviewArtifact" -> _artifact_json(value.reviewArtifact), "reviewIdentity" -> Json.fromString(value.reviewIdentity), "reviewRevision" -> Json.fromInt(value.reviewRevision), "reviewedPackage" -> _package_json(value.reviewedPackage), "scope" -> _scope_json(value.scope), "semanticDiffArtifact" -> _artifact_json(value.semanticDiffArtifact), "semanticDiffIdentity" -> Json.fromString(value.semanticDiffIdentity), "semanticDiffRevision" -> Json.fromInt(value.semanticDiffRevision)
  )
  private def _artifact_json(value: InternalModelCandidateReviewArtifact): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId), "sha256" -> Json.fromString(value.sha256))
  private def _package_json(value: InternalModelCandidateHumanApprovalPackageBasis): Json = Json.obj("packageDigest" -> Json.fromString(value.packageDigest), "packageId" -> Json.fromString(value.packageId), "projectId" -> Json.fromString(value.projectId), "projectNamespace" -> Json.fromString(value.projectNamespace), "revision" -> Json.fromLong(value.revision), "schemaVersion" -> Json.fromString(value.schemaVersion))
  private def _scope_json(value: InternalModelSemanticScope): Json = Json.obj("componentIdentity" -> Json.fromString(value.componentIdentity), "projectionContextIdentity" -> Json.fromString(value.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(value.selectedUseCaseElementIdentity))
  private def _source_json(value: InternalModelSemanticSource): Json = Json.obj("authority" -> Json.fromString(value.authority), "identity" -> Json.fromString(value.identity), "locator" -> value.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.revision.map(Json.fromString).getOrElse(Json.Null), "sha256" -> Json.fromString(value.sha256))

  private def _object(value: Json, label: String): Either[String, JsonObject] = value.asObject.toRight(s"$label must be an object")
  private def _array(root: JsonObject, key: String, label: String): Either[String, Vector[Json]] = root(key).flatMap(_.asArray).map(_.toVector).toRight(s"$label $key must be an array")
  private def _string(root: JsonObject, key: String, label: String): Either[String, String] = root(key).flatMap(_.asString).toRight(s"$label $key must be a JSON string")
  private def _nonblank(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(_is_nonblank_unicode(value), value, s"$label $key must be a nonblank Unicode string"))
  private def _nullable_nonblank(root: JsonObject, key: String, label: String): Either[String, Option[String]] = root(key).toRight(s"$label $key is missing").flatMap { case value if value.isNull => Right(None); case value => value.asString.filter(_is_nonblank_unicode).map(Some(_)).toRight(s"$label $key must be null or a nonblank Unicode string") }
  private def _token(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(_token_pattern.matches(value), value, s"$label $key must be an ASCII token"))
  private def _digest(root: JsonObject, key: String, label: String): Either[String, String] = _string(root, key, label).flatMap(value => Either.cond(_digest_pattern.matches(value), value, s"$label $key must be a lowercase sha256 digest"))
  private def _positive_int(root: JsonObject, key: String, label: String): Either[String, Int] = root(key).flatMap(_.asNumber).flatMap(_.toInt).filter(value => value > 0 && root(key).flatMap(_.asNumber).exists(_.toString == value.toString)).toRight(s"$label $key must be a positive canonical JSON integer")
  private def _positive_long(root: JsonObject, key: String, label: String): Either[String, Long] = root(key).flatMap(_.asNumber).flatMap(_.toLong).filter(value => value > 0 && root(key).flatMap(_.asNumber).exists(_.toString == value.toString)).toRight(s"$label $key must be a positive canonical JSON integer")
  private def _closed_fields(root: JsonObject, fields: Set[String], label: String): Either[String, Unit] = Either.cond(root.keys.toSet == fields, (), s"$label fields are not closed")
  private def _is_nonblank_unicode(value: String): Boolean = _is_unicode_scalar_sequence(value) && value.codePoints().anyMatch(codepoint => codepoint > 0x20 && codepoint != 0x85 && !Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint))
  private def _is_unicode_scalar_sequence(value: String): Boolean = { var index = 0; var valid = true; while index < value.length && valid do { val unit = value.charAt(index); if Character.isHighSurrogate(unit) then { valid = index + 1 < value.length && Character.isLowSurrogate(value.charAt(index + 1)); index += 2 } else if Character.isLowSurrogate(unit) then valid = false else index += 1 }; valid }
  private def _decode_utf8(bytes: Array[Byte], label: String): Either[String, String] = try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) } catch { case NonFatal(_) => Left(s"$label is not valid UTF-8") }
  private def _has_bom(bytes: Array[Byte]): Boolean = bytes.length >= 3 && bytes(0) == 0xef.toByte && bytes(1) == 0xbb.toByte && bytes(2) == 0xbf.toByte
  private def _canonical_bytes(json: Json): Array[Byte] = (_canonical_printer.print(json) + "\n").getBytes(StandardCharsets.UTF_8)
}
