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
/** Strict structural grammar; independently supplied human consent is admitted separately. */
private[runtime] object InternalModelCandidateHumanApprovalCodec {
  private val _root_fields = Set("approval", "profile", "schemaVersion")
  private val _approval_fields = Set("actor", "approvalReference", "basis", "decision", "provenance", "rationale", "unresolvedItems")
  private val _actor_fields = Set("identity", "kind", "role")
  private val _basis_fields = Set("candidateArtifactReference", "candidateReference", "candidateModelIdentity", "reviewArtifactReference", "reviewReference", "subject", "scope", "semanticDiffArtifactReference", "semanticDiffReference")
  private val _scope_fields = Set("componentIdentity", "projectionContextIdentity", "selectedUseCaseElementIdentity")
  private val _source_fields = Set("authority", "identity", "locator", "revision")
  private val _json_parser = JawnParser(allowDuplicateKeys = false)
  private val _printer = Printer.noSpacesSortKeys

  def decode(bytes: Vector[Byte]): Either[String, InternalModelCandidateHumanApproval] = {
    for {
      _ <- Either.cond(bytes != null, (), "candidate human approval bytes must be present")
      content <- _decode_utf8(bytes.toArray)
      _ <- Either.cond(!content.startsWith("\uFEFF"), (), "candidate human approval must not have a BOM")
      json <- _json_parser.parse(content).left.map(_ => "candidate human approval must be strict JSON without duplicate members or trailing content")
      value <- _record(json)
    } yield value
  }

  /** Checks ordinary in-memory structure, never authority or authenticity. */
  def validateValue(value: InternalModelCandidateHumanApproval): Either[String, Unit] = {
    try _record(_record_json(value)).map(_ => ())
    catch { case NonFatal(_) => Left("candidate human approval contains a null or invalid object graph") }
  }

  def validateValue(value: InternalModelCandidateHumanApprovalInput): Either[String, Unit] = {
    try {
      val json = _approval_json(value)
      _valid_json_unicode(json).flatMap(_ => _approval(json)).map(_ => ())
    } catch { case NonFatal(_) => Left("candidate human input contains a null or invalid object graph") }
  }

  def encode(value: InternalModelCandidateHumanApproval): Array[Byte] = _json_bytes(_record_json(value))

  private def _record(value: Json): Either[String, InternalModelCandidateHumanApproval] = {
    for {
      _ <- _valid_json_unicode(value)
      root <- _object(value, "candidate human approval root")
      _ <- _closed_fields(root, _root_fields, "candidate human approval root")
      approvalvalue <- root("approval").toRight("candidate human approval is missing")
      approval <- _approval(approvalvalue)
      profile <- _nonblank(root, "profile", "candidate human approval root")
      _ <- Either.cond(profile == "ccdm-candidate-human-approval-v2", (), "candidate human approval profile is unsupported")
      schema <- _nonblank(root, "schemaVersion", "candidate human approval root")
      _ <- Either.cond(schema == "2.0", (), "candidate human approval schemaVersion is unsupported")
    } yield InternalModelCandidateHumanApproval(approval, profile, schema)
  }

  private def _approval(value: Json): Either[String, InternalModelCandidateHumanApprovalInput] = {
    for {
      root <- _object(value, "candidate human approval")
      _ <- _closed_fields(root, _approval_fields, "candidate human approval")
      actorvalue <- root("actor").toRight("candidate human approval actor is missing")
      actor <- _actor(actorvalue)
      reference <- _record_reference(root, "approvalReference")
      basisvalue <- root("basis").toRight("candidate human approval basis is missing")
      basis <- _basis(basisvalue)
      token <- _nonblank(root, "decision", "candidate human approval")
      decision <- InternalModelCandidateHumanApprovalDecision.fromToken(token).toRight("candidate human approval decision is invalid")
      provenancevalue <- root("provenance").toRight("candidate human approval provenance is missing")
      provenance <- _source(provenancevalue)
      rationale <- _nonblank(root, "rationale", "candidate human approval")
      items <- _prose(root, "unresolvedItems")
    } yield InternalModelCandidateHumanApprovalInput(actor, reference, basis, decision, provenance, rationale, items)
  }

  private def _actor(value: Json): Either[String, InternalModelDecisionActor] = {
    for {
      root <- _object(value, "candidate human approval actor")
      _ <- _closed_fields(root, _actor_fields, "candidate human approval actor")
      kind <- _nonblank(root, "kind", "candidate human approval actor")
      _ <- Either.cond(kind == "human", (), "candidate human approval actor kind must be human")
      identity <- _nonblank(root, "identity", "candidate human approval actor")
      role <- _nonblank(root, "role", "candidate human approval actor")
    } yield InternalModelDecisionActor(kind, identity, role)
  }

  private def _basis(value: Json): Either[String, InternalModelCandidateHumanApprovalBasis] = {
    for {
      root <- _object(value, "candidate human approval basis")
      _ <- _closed_fields(root, _basis_fields, "candidate human approval basis")
      candidateartifact <- _artifact(root, "candidateArtifactReference", InternalModelArtifactRole.Projection)
      candidate <- _record_reference(root, "candidateReference")
      model <- _nonblank(root, "candidateModelIdentity", "candidate human approval basis")
      reviewartifact <- _artifact(root, "reviewArtifactReference", InternalModelArtifactRole.Validation)
      review <- _record_reference(root, "reviewReference")
      subjectvalue <- root("subject").toRight("candidate human approval subject is missing")
      subject <- InternalModelCandidateReviewBindingCodec.decodeSubject(subjectvalue)
      scopevalue <- root("scope").toRight("candidate human approval scope is missing")
      scope <- _scope(scopevalue)
      diffartifact <- _artifact(root, "semanticDiffArtifactReference", InternalModelArtifactRole.Projection)
      diff <- _record_reference(root, "semanticDiffReference")
    } yield InternalModelCandidateHumanApprovalBasis(candidateartifact, candidate, model, reviewartifact, review, subject, scope, diffartifact, diff)
  }

  private def _artifact(root: JsonObject, key: String, role: InternalModelArtifactRole): Either[String, InternalModelArtifactReference] = {
    for {
      value <- root(key).toRight(s"candidate human approval $key is missing")
      reference <- InternalModelTypedControlCodec.decodeArtifactReference(_json_bytes(value).toVector)
      _ <- Either.cond(reference.role == role, (), s"candidate human approval $key has wrong role")
    } yield reference
  }

  private def _record_reference(root: JsonObject, key: String): Either[String, InternalModelRecordReference] =
    root(key).toRight(s"candidate human approval $key is missing").flatMap { value =>
      for {
        fields <- _object(value, s"candidate human approval $key")
        _ <- _nonblank(fields, "recordId", s"candidate human approval $key")
        reference <- InternalModelTypedControlCodec.decodeRecordReference(_json_bytes(value).toVector)
      } yield reference
    }

  private def _scope(value: Json): Either[String, InternalModelSemanticScope] = {
    for {
      root <- _object(value, "candidate human approval scope")
      _ <- _closed_fields(root, _scope_fields, "candidate human approval scope")
      component <- _nonblank(root, "componentIdentity", "candidate human approval scope")
      context <- _nonblank(root, "projectionContextIdentity", "candidate human approval scope")
      usecase <- _nonblank(root, "selectedUseCaseElementIdentity", "candidate human approval scope")
    } yield InternalModelSemanticScope(component, context, usecase)
  }

  private def _source(value: Json): Either[String, InternalModelSemanticSource] = {
    for {
      root <- _object(value, "candidate human approval provenance")
      _ <- _closed_fields(root, _source_fields, "candidate human approval provenance")
      authority <- _nonblank(root, "authority", "candidate human approval provenance")
      _ <- Either.cond(authority == "human-decision", (), "candidate human approval provenance authority must be human-decision")
      identity <- _nonblank(root, "identity", "candidate human approval provenance")
      locator <- _nullable_nonblank(root, "locator")
      revision <- _nullable_nonblank(root, "revision")
    } yield InternalModelSemanticSource(authority, identity, locator, revision)
  }

  private def _prose(root: JsonObject, key: String): Either[String, Vector[String]] =
    root(key).flatMap(_.asArray).toRight(s"candidate human approval $key must be an array").flatMap(_.toVector.zipWithIndex.foldLeft[Either[String, Vector[String]]](Right(Vector.empty)) { case (result, (value, index)) =>
      for { collected <- result; prose <- value.asString.filter(_valid_text).toRight(s"candidate human approval $key $index must be nonblank Unicode") } yield collected :+ prose
    })

  private def _record_json(value: InternalModelCandidateHumanApproval): Json = Json.obj("approval" -> _approval_json(value.approval), "profile" -> Json.fromString(value.profile), "schemaVersion" -> Json.fromString(value.schemaVersion))
  private def _approval_json(value: InternalModelCandidateHumanApprovalInput): Json = Json.obj(
    "actor" -> Json.obj("identity" -> Json.fromString(value.actor.identity), "kind" -> Json.fromString(value.actor.kind), "role" -> Json.fromString(value.actor.role)),
    "approvalReference" -> _record_reference_json(value.approvalReference),
    "basis" -> _basis_json(value.basis),
    "decision" -> Json.fromString(value.decision.token),
    "provenance" -> Json.obj("authority" -> Json.fromString(value.provenance.authority), "identity" -> Json.fromString(value.provenance.identity), "locator" -> value.provenance.locator.map(Json.fromString).getOrElse(Json.Null), "revision" -> value.provenance.revision.map(Json.fromString).getOrElse(Json.Null)),
    "rationale" -> Json.fromString(value.rationale),
    "unresolvedItems" -> Json.fromValues(value.unresolvedItems.map(Json.fromString))
  )
  private def _basis_json(value: InternalModelCandidateHumanApprovalBasis): Json = Json.obj(
    "candidateArtifactReference" -> _artifact_json(value.candidateArtifactReference), "candidateReference" -> _record_reference_json(value.candidateReference),
    "candidateModelIdentity" -> Json.fromString(value.candidateModelIdentity), "reviewArtifactReference" -> _artifact_json(value.reviewArtifactReference),
    "reviewReference" -> _record_reference_json(value.reviewReference), "subject" -> InternalModelCandidateReviewBindingCodec.encodeSubject(value.subject),
    "scope" -> _scope_json(value.scope), "semanticDiffArtifactReference" -> _artifact_json(value.semanticDiffArtifactReference), "semanticDiffReference" -> _record_reference_json(value.semanticDiffReference)
  )
  private def _artifact_json(value: InternalModelArtifactReference): Json = Json.obj("artifactId" -> Json.fromString(value.artifactId.value), "artifactRevision" -> Json.fromLong(value.artifactRevision.value), "role" -> Json.fromString(value.role.wireValue))
  private def _record_reference_json(value: InternalModelRecordReference): Json = Json.obj("recordId" -> Json.fromString(value.recordId.value), "recordRevision" -> Json.fromLong(value.recordRevision.value))
  private def _scope_json(value: InternalModelSemanticScope): Json = Json.obj("componentIdentity" -> Json.fromString(value.componentIdentity), "projectionContextIdentity" -> Json.fromString(value.projectionContextIdentity), "selectedUseCaseElementIdentity" -> Json.fromString(value.selectedUseCaseElementIdentity))
  private def _object(value: Json, label: String): Either[String, JsonObject] = value.asObject.toRight(s"$label must be an object")
  private def _nonblank(root: JsonObject, key: String, label: String): Either[String, String] = root(key).flatMap(_.asString).filter(_valid_text).toRight(s"$label $key must be a nonblank Unicode string")
  private def _nullable_nonblank(root: JsonObject, key: String): Either[String, Option[String]] = root(key) match {
    case Some(value) if value.isNull => Right(None)
    case Some(value) => value.asString.filter(_valid_text).map(Some(_)).toRight(s"candidate human approval provenance $key must be null or nonblank Unicode")
    case None => Left(s"candidate human approval provenance $key is missing")
  }
  private def _closed_fields(root: JsonObject, fields: Set[String], label: String): Either[String, Unit] = Either.cond(root.keys.toSet == fields, (), s"$label fields are not closed")
  private def _valid_text(value: String): Boolean = value != null && _valid_unicode(value) && value.codePoints().anyMatch(codepoint => codepoint > 0x20 && codepoint != 0x85 && !Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint))
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
    Either.cond(valid, (), "candidate human approval JSON contains invalid Unicode")
  }
  private def _decode_utf8(bytes: Array[Byte]): Either[String, String] = {
    try { val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT); Right(decoder.decode(ByteBuffer.wrap(bytes)).toString) }
    catch { case NonFatal(_) => Left("candidate human approval bytes must be valid UTF-8") }
  }
  private def _json_bytes(value: Json): Array[Byte] = {
    _valid_json_unicode(value).fold(message => throw new IllegalArgumentException(message), _ => ())
    (_printer.print(value) + "\n").getBytes(StandardCharsets.UTF_8)
  }
}
