/**
 * @since   Oct. 26, 2026
 * @version Oct. 27, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import io.circe.{Codec, Json, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.jawn.JawnParser
import io.circe.syntax.*
import ModelEditSession.*
import ModelEditCandidateCodec.{given, *}
import ModelEditCommandCodec.given

/** Complete typed edit projection in the existing closed package inventory. */
private[runtime] object ModelEditSessionCodec {
  val profile: String = "ccdm-model-edit-session-v1"
  val schemaVersion: String = "1.0"
  private val _parser = JawnParser(allowDuplicateKeys = false)
  private given Codec.AsObject[ModelEditSession] = closed[ModelEditSession]("reference", "scope", "candidate", "state", "history", "reviews")(using deriveDecoder[ModelEditSession], deriveEncoder[ModelEditSession])

  def encode(session: ModelEditSession): Vector[Byte] = {
    val current = session.candidate.actors.nonEmpty || session.candidate.slices.exists(_.fields.nonEmpty) ||
      session.history.exists(entry => entry.operation.command match {
        case Command.CreateUseCaseSlice(slice) => slice.fields.nonEmpty
        case Command.RefineUseCaseSlice(_, slice) => slice.fields.nonEmpty
        case _ => false
      })
    val payload = if (current) session.asJson else _legacy_shape(session.asJson)
    (Printer.noSpacesSortKeys.print(Json.obj("profile" -> Json.fromString(if (current) "ccdm-model-edit-session-v2" else profile),
      "schemaVersion" -> Json.fromString(if (current) "2.0" else schemaVersion), "session" -> payload)) + "\n")
      .getBytes(StandardCharsets.UTF_8).toVector
  }

  def hasProfile(bytes: Vector[Byte]): Boolean =
    _json(bytes).toOption.flatMap(_.hcursor.get[String]("profile").toOption).exists(value =>
      value == profile || value == "ccdm-model-edit-session-v2")

  def decode(bytes: Vector[Byte]): Either[String, ModelEditSession] = for {
    json <- _json(bytes)
    root <- json.asObject.toRight("edit projection must be an object")
    _ <- Either.cond(root.keys.toSet == Set("profile", "schemaVersion", "session"), (), "edit projection fields must match its closed shape")
    _ <- Either.cond(root("profile").contains(Json.fromString(profile)) &&
      root("schemaVersion").contains(Json.fromString(schemaVersion)) ||
      root("profile").contains(Json.fromString("ccdm-model-edit-session-v2")) &&
      root("schemaVersion").contains(Json.fromString("2.0")), (), "unsupported edit projection profile/schema")
    payload <- root("session").toRight("edit session missing")
    _ <- Either.cond(!root("profile").contains(Json.fromString("ccdm-model-edit-session-v2")) || _current_shape(payload), (), "v2 requires explicit independent Actor and Slice field collections")
    session <- payload.as[ModelEditSession].left.map(_.message)
    _ <- Either.cond(!root("profile").contains(Json.fromString(profile)) ||
      root("session").exists(value => _legacy_shape(value) == value), (), "v1 preserves its original Actor/field-empty wire shape")
    _ <- validate(session)
  } yield session

  def decodeCaptured(projection: InternalModelVerifiedProjection): Either[String, ModelEditSession] = for {
    _ <- Either.cond(projection.reference.role == InternalModelArtifactRole.Projection, (), "edit session must have Projection role")
    session <- decode(projection.bytes)
    _ <- Either.cond(projection.dependencies == session.candidate.artifacts, (), "edit projection inventory dependencies differ from candidate artifacts")
    _ <- Either.cond(!projection.dependencies.exists(_.artifactId == projection.reference.artifactId), (), "edit projection cannot depend on itself")
  } yield session

  def validate(session: ModelEditSession): Either[String, Unit] = {
    val candidate = session.candidate
    val sliceids = candidate.slices.map(_.identity)
    val goalids = candidate.goals.map(_.reference.semanticTargetId)
    val usecaseids = candidate.useCases.map(_.semanticTargetId)
    val importedgoals = candidate.useCases.flatMap(_.references).filter(_.referenceKind == Goal).map(_.semanticTargetId)
    val edges = candidate.specificationLinks.map(_.reference) ++ candidate.ticketLinks.map(_.reference) ++ candidate.goalLinks.map(_.reference)
    val history = session.history
    val chain = history.zipWithIndex.forall { case (entry, index) =>
      val previous = entry.previousCandidate
      val successor = entry.successorCandidate
      entry.operation.sessionReference == session.reference && entry.operation.preconditions.expectedCandidate == previous &&
        previous.recordId == candidate.reference.recordId && successor.recordId == previous.recordId &&
        successor.recordRevision.value > previous.recordRevision.value &&
        (index == 0 || history(index - 1).successorCandidate == previous) &&
        entry.authorization.session == session.reference && entry.authorization.scope == session.scope &&
        entry.authorization.permitted.contains(ModelEditSession.commandKind(entry.operation.command)) &&
        _resolution_binding(entry) &&
        entry.operation.preconditions.requiredArtifacts.forall(candidate.artifacts.contains) &&
        entry.resolutionEvidence.forall(evidence => candidate.artifacts.contains(evidence.source) &&
          entry.operation.preconditions.requiredArtifacts.contains(evidence.source) && evidence.state == ResolutionState.Resolved)
    }
    for {
      _ <- Either.cond(candidate.scope == session.scope, (), "candidate/session scope mismatch")
      _ <- UseCaseCommunicationProjection.create(session.scope.context, session.scope.component, candidate.useCases).left.map(_.violations.mkString("; "))
      _ <- Either.cond(sliceids.distinct.size == sliceids.size && goalids.distinct.size == goalids.size &&
        candidate.slices.map(_.reference.recordId).distinct.size == candidate.slices.size &&
        candidate.goals.map(_.reference.id).distinct.size == candidate.goals.size &&
        edges.map(_.recordId).distinct.size == edges.size, (), "candidate semantic or relationship identity is occupied")
      _ <- Either.cond(candidate.slices.forall(slice => usecaseids.contains(slice.useCaseIdentity) && slice.fields.map(_.name).distinct.size == slice.fields.size &&
          slice.fields.forall(field => field.name.trim.nonEmpty && field.datatype.trim.nonEmpty && Set("1", "?", "*", "+")(field.multiplicity))) &&
        candidate.specificationLinks.forall(edge => sliceids.contains(edge.sliceIdentity)) &&
        candidate.ticketLinks.forall(edge => sliceids.contains(edge.sliceIdentity)) &&
        candidate.goalLinks.forall(edge => usecaseids.contains(edge.useCaseIdentity) &&
          (goalids ++ importedgoals).contains(edge.goalIdentity)), (), "candidate contains an unknown relationship endpoint")
      _ <- Either.cond(candidate.goals.forall(goal => goal.reference.referenceKind == Goal &&
        goal.reference.component == session.scope.component && !candidate.actors.exists(actor => actor.id == goal.reference.id ||
          actor.semanticRelationshipId == goal.reference.semanticRelationshipId) && goal.actor.forall(actor =>
          actor.referenceKind == Actor && (candidate.actors.contains(actor) || candidate.useCases.exists(_.references.contains(actor)))) &&
        !sliceids.contains(goal.reference.semanticTargetId) && !usecaseids.contains(goal.reference.semanticTargetId) &&
        candidate.useCases.flatMap(_.references).filter(_.semanticTargetId == goal.reference.semanticTargetId)
          .forall(_ == goal.reference)), (), "independent Goal reference or Actor attribution is invalid")
      _ <- Either.cond(candidate.actors.map(_.semanticTargetId).distinct.size == candidate.actors.size &&
        candidate.actors.forall(actor => actor.referenceKind == Actor && actor.component == session.scope.component &&
          actor.id.nonEmpty && !sliceids.contains(actor.semanticTargetId) && !goalids.contains(actor.semanticTargetId) &&
          !usecaseids.contains(actor.semanticTargetId)), (), "independent Actor records are invalid")
      _ <- Either.cond(candidate.artifacts.distinct.size == candidate.artifacts.size, (), "duplicate candidate artifact dependencies")
      _ <- Either.cond(chain && history.lastOption.forall(_.successorCandidate == candidate.reference) &&
        history.map(_.operation.reference.recordId).distinct.size == history.size, (), "edit history does not bind the exact session/candidate chain")
      _ <- Either.cond(session.reviews.forall(review => review.dependency.validation.session == session.reference &&
        review.dependency.validation.artifacts.forall(candidate.artifacts.contains) &&
        review.findings.flatMap(_.evidenceRefs).forall(candidate.artifacts.contains) &&
        review.findings.forall(_.relationshipScope match {
          case RelationshipScopeEvidence.Complete(_, source) => candidate.artifacts.contains(source)
          case RelationshipScopeEvidence.Unresolved(_, _) => true
        })), (), "retained review evidence is outside candidate dependencies")
    } yield ()
  }

  private def _current_shape(value: Json): Boolean = value.arrayOrObject(true,
    values => values.forall(_current_shape),
    values => (!values.contains("useCases") || values.contains("actors")) &&
      (!values.contains("acceptanceRefs") || values.contains("fields")) && values.values.forall(_current_shape))

  private def _legacy_shape(value: Json): Json = value.arrayOrObject(value,
    values => Json.fromValues(values.map(_legacy_shape)),
    values => Json.fromJsonObject(values.remove("actors").remove("fields").mapValues(_legacy_shape)))

  private def _resolution_binding(entry: AppliedOperation): Boolean = {
    val target = entry.operation.command match {
      case Command.LinkSliceSpecification(edge) => Some(ExternalReference.Specification(edge.specification))
      case Command.LinkSliceTicket(edge) => Some(ExternalReference.Ticket(edge.ticket))
      case _ => None
    }
    target.fold(entry.resolutionEvidence.isEmpty)(value => entry.resolutionEvidence.map(_.target) == Vector(value)) &&
      (entry.operation.command match {
        case Command.AddGoal(_, _, _, None) => false
        case _ => true
      })
  }

  private def _json(bytes: Vector[Byte]): Either[String, Json] = {
    val content = try {
      Right(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toArray)).toString)
    } catch { case _: CharacterCodingException => Left("edit projection must be valid UTF-8") }
    for {
      value <- content
      _ <- Either.cond(!value.startsWith("\uFEFF"), (), "edit projection must not contain a BOM")
      json <- _parser.parse(value).left.map(_ => "edit projection must be strict JSON without duplicate members")
    } yield json
  }
}
