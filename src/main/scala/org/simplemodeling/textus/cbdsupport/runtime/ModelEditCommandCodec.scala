/**
 * @since   Oct. 26, 2026
 * @version Oct. 26, 2026
 */
package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Codec, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import ModelEditSession.*
import ModelEditCandidateCodec.{given, *}

/** The ten admitted command alternatives, with exact typed nested records. */
private[runtime] object ModelEditCommandCodec {
  given Codec.AsObject[Command.CreateUseCaseSlice] = closed[Command.CreateUseCaseSlice]("proposed")(using deriveDecoder[Command.CreateUseCaseSlice], deriveEncoder[Command.CreateUseCaseSlice])
  given Codec.AsObject[Command.RefineUseCaseSlice] = closed[Command.RefineUseCaseSlice]("existing", "proposed")(using deriveDecoder[Command.RefineUseCaseSlice], deriveEncoder[Command.RefineUseCaseSlice])
  given Codec.AsObject[Command.LinkSliceSpecification] = closed[Command.LinkSliceSpecification]("relationship")(using deriveDecoder[Command.LinkSliceSpecification], deriveEncoder[Command.LinkSliceSpecification])
  given Codec.AsObject[Command.UnlinkSliceSpecification] = closed[Command.UnlinkSliceSpecification]("relationship")(using deriveDecoder[Command.UnlinkSliceSpecification], deriveEncoder[Command.UnlinkSliceSpecification])
  given Codec.AsObject[Command.LinkSliceTicket] = closed[Command.LinkSliceTicket]("relationship")(using deriveDecoder[Command.LinkSliceTicket], deriveEncoder[Command.LinkSliceTicket])
  given Codec.AsObject[Command.UnlinkSliceTicket] = closed[Command.UnlinkSliceTicket]("relationship")(using deriveDecoder[Command.UnlinkSliceTicket], deriveEncoder[Command.UnlinkSliceTicket])
  given Codec.AsObject[Command.AddGoal] = closed[Command.AddGoal]("actor", "proposed", "intent", "proposedReference")(using deriveDecoder[Command.AddGoal], deriveEncoder[Command.AddGoal])
  given Codec.AsObject[Command.RefineGoal] = closed[Command.RefineGoal]("goal", "intent")(using deriveDecoder[Command.RefineGoal], deriveEncoder[Command.RefineGoal])
  given Codec.AsObject[Command.LinkGoalUseCase] = closed[Command.LinkGoalUseCase]("relationship")(using deriveDecoder[Command.LinkGoalUseCase], deriveEncoder[Command.LinkGoalUseCase])
  given Codec.AsObject[Command.UnlinkGoalUseCase] = closed[Command.UnlinkGoalUseCase]("relationship")(using deriveDecoder[Command.UnlinkGoalUseCase], deriveEncoder[Command.UnlinkGoalUseCase])
  given Codec[Command] = sum[Command](using deriveDecoder[Command], deriveEncoder[Command])
  given Codec.AsObject[Preconditions] = closed[Preconditions]("expectedCandidate", "requiredArtifacts")(using deriveDecoder[Preconditions], deriveEncoder[Preconditions])
  given Codec.AsObject[Operation] = closed[Operation]("reference", "sessionReference", "preconditions", "command", "attribution")(using deriveDecoder[Operation], deriveEncoder[Operation])
  given Codec.AsObject[AppliedOperation] = closed[AppliedOperation]("operation", "previousCandidate", "successorCandidate", "authorization", "resolutionEvidence")(using deriveDecoder[AppliedOperation], deriveEncoder[AppliedOperation])

  def encode(command: Command): Json = command.asJson
  def decode(value: Json): Either[String, Command] = value.as[Command].left.map(_.message)
}
