/**
 * @since   Oct. 26, 2026
 * @version Oct. 26, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import ModelEditSession.*

/** Closed records: every field, including optional fields, is explicitly represented. */
private[runtime] object ModelEditCandidateCodec {
  def closed[A](fields: String*)(using decoder: Decoder[A], encoder: Encoder.AsObject[A]): Codec.AsObject[A] =
    Codec.AsObject.from(Decoder.instance { cursor =>
      cursor.value.asObject match {
        case Some(value) if value.keys.toSet == fields.toSet => decoder(cursor)
        case _ => Left(DecodingFailure("unexpected or missing record fields", cursor.history))
      }
    }, encoder)

  def enumeration[A](values: Vector[A], name: A => String): Codec[A] =
    Codec.from(Decoder.decodeString.emap(value => values.find(item => name(item) == value).toRight("unknown enum value")),
      Encoder.encodeString.contramap(name))

  given Codec[InternalModelRecordId] = Codec.from(Decoder.decodeString.emap(InternalModelRecordId.from), Encoder.encodeString.contramap(_.value))
  given Codec[InternalModelRecordReference] = Codec.from(Decoder.decodeJson.emap(value =>
    InternalModelTypedControlCodec.decodeRecordReference(value.noSpaces.getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector)),
    Encoder.instance(value => io.circe.parser.parse(new String(InternalModelTypedControlCodec.encodeRecordReference(value).toArray, java.nio.charset.StandardCharsets.UTF_8)).toOption.get))
  given Codec[InternalModelArtifactReference] = Codec.from(Decoder.decodeJson.emap(value =>
    InternalModelTypedControlCodec.decodeArtifactReference(value.noSpaces.getBytes(java.nio.charset.StandardCharsets.UTF_8).toVector)),
    Encoder.instance(value => io.circe.parser.parse(new String(InternalModelTypedControlCodec.encodeArtifactReference(value).toArray, java.nio.charset.StandardCharsets.UTF_8)).toOption.get))
  given Codec[ComponentDashboardComponentIdentity] = Codec.from(Decoder.decodeString.emap(value => Either.cond(value.trim.nonEmpty, ComponentDashboardComponentIdentity(value), "blank component identity")), Encoder.encodeString.contramap(_.value))
  given Codec[ComponentDashboardSemanticTargetIdentity] = Codec.from(Decoder.decodeString.emap(value => Either.cond(value.trim.nonEmpty, ComponentDashboardSemanticTargetIdentity(value), "blank semantic identity")), Encoder.encodeString.contramap(_.value))
  given Codec[MonoKotoProjectionContextIdentity] = Codec.from(Decoder.decodeString.emap(value => Either.cond(value.trim.nonEmpty, MonoKotoProjectionContextIdentity(value), "blank context identity")), Encoder.encodeString.contramap(_.value))
  given Codec[UseCaseCommunicationReferenceKind] = enumeration[UseCaseCommunicationReferenceKind](Vector(Actor, Goal, Trigger, Postcondition, DomainElement, Collaborator, RealizingWorkflow, MonoKoto), _.toString)
  given Codec[SliceLifecycle] = enumeration(SliceLifecycle.values.toVector, _.toString)
  given Codec[ResolutionState] = enumeration(ResolutionState.values.toVector, _.toString)
  given Codec[Severity] = enumeration(Severity.values.toVector, _.toString)
  given Codec[FindingKind] = enumeration(FindingKind.values.toVector, _.toString)
  given Codec[CommandKind] = enumeration(CommandKind.values.toVector, _.toString)
  given Codec[SessionState] = enumeration(SessionState.values.toVector, _.toString)
  given Codec.AsObject[ComponentDashboardSourceAttribution] = closed[ComponentDashboardSourceAttribution]("sourceId", "authorityScope", "sourceLocator")(using deriveDecoder[ComponentDashboardSourceAttribution], deriveEncoder[ComponentDashboardSourceAttribution])
  given Codec.AsObject[ComponentDashboardCondition] = closed[ComponentDashboardCondition]("availability", "authorization", "redaction", "explicitAbsence", "ambiguity", "conflict", "staleness", "malformedEvidence", "limitations")(using deriveDecoder[ComponentDashboardCondition], deriveEncoder[ComponentDashboardCondition])
  given Codec.AsObject[ComponentDashboardNavigationTarget] = closed[ComponentDashboardNavigationTarget]("targetIdentity", "targetComponent", "targetAttribution", "targetCondition", "implementedAndUsable")(using deriveDecoder[ComponentDashboardNavigationTarget], deriveEncoder[ComponentDashboardNavigationTarget])
  given Codec.AsObject[UseCaseCommunicationReference] = closed[UseCaseCommunicationReference]("id", "component", "semanticTargetId", "semanticRelationshipId", "referenceKind", "attribution", "condition", "stableTieKey", "navigationTarget")(using deriveDecoder[UseCaseCommunicationReference], deriveEncoder[UseCaseCommunicationReference])
  given Codec.AsObject[UseCaseCommunicationFlowStep] = closed[UseCaseCommunicationFlowStep]("id", "component", "semanticTargetId", "semanticRelationshipId", "sequenceKey", "attribution", "condition", "stableTieKey", "navigationTarget")(using deriveDecoder[UseCaseCommunicationFlowStep], deriveEncoder[UseCaseCommunicationFlowStep])
  given Codec.AsObject[UseCaseCommunicationFlow] = closed[UseCaseCommunicationFlow]("id", "component", "semanticTargetId", "semanticRelationshipId", "attribution", "condition", "steps", "stableTieKey", "navigationTarget")(using deriveDecoder[UseCaseCommunicationFlow], deriveEncoder[UseCaseCommunicationFlow])
  given Codec.AsObject[UseCaseCommunicationProjectionSubject] = closed[UseCaseCommunicationProjectionSubject]("id", "component", "semanticTargetId", "attribution", "condition", "references", "flows", "stableTieKey", "navigationTarget")(using deriveDecoder[UseCaseCommunicationProjectionSubject], deriveEncoder[UseCaseCommunicationProjectionSubject])
  given Codec.AsObject[Scope] = closed[Scope]("context", "component")(using deriveDecoder[Scope], deriveEncoder[Scope])
  given Codec.AsObject[RepositoryProjectReference] = closed[RepositoryProjectReference]("repository", "project")(using deriveDecoder[RepositoryProjectReference], deriveEncoder[RepositoryProjectReference])
  given Codec.AsObject[SpecificationReference] = closed[SpecificationReference]("owner", "specificationIdentity", "sourceRevision")(using deriveDecoder[SpecificationReference], deriveEncoder[SpecificationReference])
  given Codec.AsObject[TicketRef] = closed[TicketRef]("provider", "owner", "ticketIdentity")(using deriveDecoder[TicketRef], deriveEncoder[TicketRef])
  given Codec.AsObject[TicketObservation] = closed[TicketObservation]("state", "version")(using deriveDecoder[TicketObservation], deriveEncoder[TicketObservation])
  given Codec.AsObject[UseCaseSlice] = closed[UseCaseSlice]("reference", "identity", "useCaseIdentity", "title", "intent", "scopeRefs", "acceptanceRefs", "lifecycle", "attribution")(using deriveDecoder[UseCaseSlice], deriveEncoder[UseCaseSlice])
  given Codec.AsObject[SliceSpecificationRelationship] = closed[SliceSpecificationRelationship]("reference", "sliceIdentity", "specification", "attribution")(using deriveDecoder[SliceSpecificationRelationship], deriveEncoder[SliceSpecificationRelationship])
  given Codec.AsObject[SliceTicketRelationship] = closed[SliceTicketRelationship]("reference", "sliceIdentity", "ticket", "observation", "attribution")(using deriveDecoder[SliceTicketRelationship], deriveEncoder[SliceTicketRelationship])
  given Codec.AsObject[GoalUseCaseRelationship] = closed[GoalUseCaseRelationship]("reference", "goalIdentity", "useCaseIdentity", "attribution")(using deriveDecoder[GoalUseCaseRelationship], deriveEncoder[GoalUseCaseRelationship])
  given Codec.AsObject[ActorGoal] = closed[ActorGoal]("reference", "actor", "intent", "attribution")(using deriveDecoder[ActorGoal], deriveEncoder[ActorGoal])
  given Codec.AsObject[Candidate] = closed[Candidate]("reference", "scope", "useCases", "slices", "specificationLinks", "ticketLinks", "goalLinks", "artifacts", "goals")(using deriveDecoder[Candidate], deriveEncoder[Candidate])
  given Codec.AsObject[ExternalReference.Specification] = closed[ExternalReference.Specification]("reference")(using deriveDecoder[ExternalReference.Specification], deriveEncoder[ExternalReference.Specification])
  given Codec.AsObject[ExternalReference.Ticket] = closed[ExternalReference.Ticket]("reference")(using deriveDecoder[ExternalReference.Ticket], deriveEncoder[ExternalReference.Ticket])
  given Codec[ExternalReference] = sum[ExternalReference](using deriveDecoder[ExternalReference], deriveEncoder[ExternalReference])
  given Codec.AsObject[ResolutionEvidence] = closed[ResolutionEvidence]("target", "state", "source", "attribution")(using deriveDecoder[ResolutionEvidence], deriveEncoder[ResolutionEvidence])
  given Codec.AsObject[Subject.UseCase] = closed[Subject.UseCase]("identity")(using deriveDecoder[Subject.UseCase], deriveEncoder[Subject.UseCase])
  given Codec.AsObject[Subject.Actor] = closed[Subject.Actor]("identity")(using deriveDecoder[Subject.Actor], deriveEncoder[Subject.Actor])
  given Codec.AsObject[Subject.Goal] = closed[Subject.Goal]("identity")(using deriveDecoder[Subject.Goal], deriveEncoder[Subject.Goal])
  given Codec.AsObject[Subject.Slice] = closed[Subject.Slice]("identity")(using deriveDecoder[Subject.Slice], deriveEncoder[Subject.Slice])
  given Codec.AsObject[Subject.Relationship] = closed[Subject.Relationship]("reference")(using deriveDecoder[Subject.Relationship], deriveEncoder[Subject.Relationship])
  given Codec[Subject] = sum[Subject](using deriveDecoder[Subject], deriveEncoder[Subject])
  given Codec.AsObject[ValidationDependency] = closed[ValidationDependency]("session", "candidate", "operations", "artifacts")(using deriveDecoder[ValidationDependency], deriveEncoder[ValidationDependency])
  given Codec.AsObject[ApprovalDependency] = closed[ApprovalDependency]("validation", "validationReference", "semanticDiffReference", "reviewReference")(using deriveDecoder[ApprovalDependency], deriveEncoder[ApprovalDependency])
  given Codec.AsObject[FindingDisposition] = closed[FindingDisposition]("severity", "blocking")(using deriveDecoder[FindingDisposition], deriveEncoder[FindingDisposition])
  given Codec.AsObject[RelationshipScopeEvidence.Complete] = closed[RelationshipScopeEvidence.Complete]("subject", "source")(using deriveDecoder[RelationshipScopeEvidence.Complete], deriveEncoder[RelationshipScopeEvidence.Complete])
  given Codec.AsObject[RelationshipScopeEvidence.Unresolved] = closed[RelationshipScopeEvidence.Unresolved]("subject", "state")(using deriveDecoder[RelationshipScopeEvidence.Unresolved], deriveEncoder[RelationshipScopeEvidence.Unresolved])
  given Codec[RelationshipScopeEvidence] = sum[RelationshipScopeEvidence](using deriveDecoder[RelationshipScopeEvidence], deriveEncoder[RelationshipScopeEvidence])
  given Codec.AsObject[UseCaseReviewFinding] = closed[UseCaseReviewFinding]("kind", "subject", "relatedRefs", "policy", "disposition", "evidenceRefs", "relationshipScope")(using deriveDecoder[UseCaseReviewFinding], deriveEncoder[UseCaseReviewFinding])
  given Codec.AsObject[RetainedReview] = closed[RetainedReview]("dependency", "findings")(using deriveDecoder[RetainedReview], deriveEncoder[RetainedReview])
  given Codec.AsObject[Authorization] = closed[Authorization]("reference", "session", "scope", "permitted")(using deriveDecoder[Authorization], deriveEncoder[Authorization])

  /** Sum alternatives have exactly one known member; each alternative codec closes its payload. */
  def sum[A](using decoder: Decoder[A], encoder: Encoder[A]): Codec[A] =
    Codec.from(Decoder.instance { cursor =>
      if (cursor.value.asObject.exists(_.size == 1)) decoder(cursor)
      else Left(DecodingFailure("sum requires exactly one alternative", cursor.history))
    }, encoder)

  def encode(candidate: Candidate): Json = candidate.asJson
  def decode(value: Json): Either[String, Candidate] = value.as[Candidate].left.map(_.message)
}
