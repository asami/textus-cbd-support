package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.{Codec, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.jawn.JawnParser
import io.circe.syntax.*
import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.finding.FindingDispositionV1 as Disposition
import Disposition.{Coverage, Declaration, Finding, Inspection, Key, Kind, Report, Result, Status, Target}
import FindingResolution.*

/*
 * @since   Nov.  5, 2026
 * @version Nov.  5, 2026
 */
/** Host-owned single-writer preparation/admission; no source writer or AI engine. */
final class FindingResolutionApplication private (
  private val _imports: Vector[FindingResolutionApplication.Import],
  private val _bindings: Vector[FindingResolutionApplication.Binding]
) {
  import FindingResolutionApplication.*
  import JsonProtocol.{given, *}
  private var _working: Map[InternalModelRecordReference, Working] = Map.empty

  def execute(operation: String, requestJson: String)(using ctx: ExecutionContext): Consequence[Response] = for {
    principal <- ModelEditAuthorization.authenticatedPrincipalC
    binding <- _bindings.filter(_.principalId == principal) match {
      case Vector(value) => Consequence.success(value)
      case _ => Consequence.securityPermissionDenied("Finding resolution requires an exact server principal binding")
    }
    _ <- if (operations.contains(operation)) Consequence.unit else Consequence.operationInvalid("Unknown finding resolution Operation")
    document <- parse(requestJson).fold(Consequence.argumentInvalid, Consequence.success)
    response <- _execute(operation, document, binding)
  } yield response

  private def _execute(operation: String, document: Json, binding: Binding): Consequence[Response] = operation match {
    case "importFindingInspection" => for {
      request <- _decode[Selection](document)
      _ <- _access(binding, request.imported)
      _ <- if (!_working.contains(request.imported)) Consequence.unit
        else Consequence.argumentInvalid("Import is already retained; select its existing working state")
      imported <- _imports.find(_.reference == request.imported).map(Consequence.success)
        .getOrElse(Consequence.argumentInvalid("Select an exact host inspection import"))
      current <- FindingResolution.importC(imported.reference, imported.inspection, imported.declarations, imported.gaps)
    } yield {
      _working = _working.updated(current.reference, current)
      _response(operation, current)
    }
    case "groupFindingResolutions" => for {
      request <- _decode[GroupingRequest](document)
      current <- _current(binding, request.imported)
      grouped <- current.groupC(request.assignments)
    } yield _response(operation, current, Some(grouped))
    case "proposeFindingResolution" => for {
      request <- _decode[ProposalRequest](document)
      current <- _current(binding, request.imported)
      proposed <- current.proposeC(request.proposal, request.keys, request.kind, request.reason, request.provenance)
    } yield {
      _working = _working.updated(proposed.reference, proposed)
      _response(operation, proposed)
    }
    case "decideFindingResolution" => for {
      request <- _decode[DecisionRequest](document)
      current <- _current(binding, request.imported)
      direction <- binding.directions.filter(value => value.imported == request.imported && value.proposal == request.proposal) match {
        case Vector(value) => Consequence.success(value)
        case _ => Consequence.securityPermissionDenied("Select an exact host developer direction for this import and proposal")
      }
      decided <- current.decideC(request.decision, request.proposal, request.keys, request.choice, request.reason,
        DeveloperAuthority(binding.principalId, direction.reference, direction.imported, direction.proposal, direction.keys, direction.kinds))
    } yield {
      _working = _working.updated(decided.reference, decided)
      _response(operation, decided)
    }
    case "getFindingResolution" => for {
      request <- _decode[Selection](document)
      _ <- _access(binding, request.imported)
    } yield _working.get(request.imported).map(value => _response(operation, value)).getOrElse(
      Response("UNAVAILABLE", Json.obj("operation" -> Json.fromString(operation), "status" -> Json.fromString("UNAVAILABLE"),
        "imported" -> request.imported.asJson, "reason" -> Json.fromString("Selected host inspection has not been imported")).noSpaces))
    case _ => Consequence.operationInvalid("Unknown finding resolution Operation")
  }

  private def _decode[A: io.circe.Decoder](document: Json): Consequence[A] =
    document.as[A].left.map(_.message).fold(Consequence.argumentInvalid, Consequence.success)

  private def _access(binding: Binding, imported: InternalModelRecordReference): Consequence[Unit] =
    if (binding.imports(imported)) Consequence.unit
    else Consequence.securityPermissionDenied("Finding resolution requires access to the exact imported reference and revision")

  private def _current(binding: Binding, imported: InternalModelRecordReference): Consequence[Working] =
    _access(binding, imported).flatMap(_ => _working.get(imported).map(Consequence.success)
      .getOrElse(Consequence.argumentInvalid("Import the selected host inspection before preparing or deciding work")))

  private def _response(operation: String, current: Working, grouping: Option[Grouping] = None): Response =
    Response("OK", Json.obj("schemaVersion" -> Json.fromString("cbd.finding-resolution-operation.v1"),
      "operation" -> Json.fromString(operation), "status" -> Json.fromString("OK"), "imported" -> current.reference.asJson,
      "inspection" -> current.inspection.asJson, "declarations" -> current.declarations.asJson,
      "gaps" -> current.gaps.asJson, "report" -> current.report.asJson,
      "rawFindingCount" -> Json.fromInt(current.rawFindingCount), "actionableCount" -> Json.fromInt(current.actionableCount),
      "proposals" -> current.proposals.asJson, "decisions" -> current.decisions.asJson, "grouping" -> grouping.asJson).noSpaces)
}

object FindingResolutionApplication {
  /** Trusted embedding records, never operation input. */
  final case class Import(reference: InternalModelRecordReference, inspection: Inspection,
    declarations: Vector[Declaration], gaps: Vector[CoverageGap])
  final case class Direction(reference: InternalModelRecordReference, imported: InternalModelRecordReference,
    proposal: InternalModelRecordReference, keys: Set[Key], kinds: Set[Kind])
  final case class Binding(principalId: String, imports: Set[InternalModelRecordReference], directions: Vector[Direction])
  final case class Response(status: String, resultJson: String)

  final case class Selection(imported: InternalModelRecordReference)
  final case class GroupingRequest(imported: InternalModelRecordReference, assignments: Vector[Remediation])
  final case class ProposalRequest(imported: InternalModelRecordReference, proposal: InternalModelRecordReference,
    keys: Vector[Key], kind: Kind, reason: String, provenance: String)
  final case class DecisionRequest(imported: InternalModelRecordReference, proposal: InternalModelRecordReference,
    decision: InternalModelRecordReference, keys: Vector[Key], choice: Choice, reason: String)

  val operations: Set[String] = Set("importFindingInspection", "groupFindingResolutions", "proposeFindingResolution",
    "decideFindingResolution", "getFindingResolution")

  def create(imports: Vector[Import], bindings: Vector[Binding]): Consequence[FindingResolutionApplication] = {
    val references = imports.map(_.reference).toSet
    val valid = references.size == imports.size && bindings.map(_.principalId).distinct.size == bindings.size &&
      bindings.forall(value => value.principalId.trim.nonEmpty && value.imports.subsetOf(references) &&
        value.directions.map(item => (item.imported, item.proposal)).distinct.size == value.directions.size &&
        value.directions.forall(item => value.imports(item.imported) && item.keys.nonEmpty && item.kinds.nonEmpty))
    if (valid) Consequence.success(new FindingResolutionApplication(imports, bindings))
    else Consequence.argumentInvalid("Finding host bindings require unique principals, exact registered imports and unambiguous scoped directions")
  }

  /** Closed transport records; host Import, Direction and Binding have no decoders. */
  object JsonProtocol {
    import ModelEditCandidateCodec.*
    export ModelEditCandidateCodec.{given Codec[InternalModelRecordReference]}
    private val _parser = JawnParser(allowDuplicateKeys = false)
    def parse(document: String): Either[String, Json] = _parser.parse(document).left.map(_.message)

    given Codec[Kind] = enumeration(Kind.values.toVector, _.toString)
    given Codec[Choice] = enumeration(Choice.values.toVector, _.toString)
    given Codec[Status] = enumeration(Status.values.toVector, _.toString)
    given Codec[Availability] = enumeration(Availability.values.toVector, _.toString)
    given Codec.AsObject[Target] = closed[Target]("scope", "identity")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[Key] = closed[Key]("rule", "finding", "target")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[Remediation] = closed[Remediation]("key", "pattern")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[Selection] = closed[Selection]("imported")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[GroupingRequest] = closed[GroupingRequest]("imported", "assignments")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[ProposalRequest] = closed[ProposalRequest]("imported", "proposal", "keys", "kind", "reason", "provenance")(using deriveDecoder, deriveEncoder)
    given Codec.AsObject[DecisionRequest] = closed[DecisionRequest]("imported", "proposal", "decision", "keys", "choice", "reason")(using deriveDecoder, deriveEncoder)
    given Encoder.AsObject[Finding] = deriveEncoder
    given Encoder.AsObject[Coverage] = deriveEncoder
    given Encoder.AsObject[Inspection] = deriveEncoder
    given Encoder.AsObject[Declaration] = deriveEncoder
    given Encoder.AsObject[Result] = deriveEncoder
    given Encoder.AsObject[Report] = deriveEncoder
    given Encoder.AsObject[CoverageGap] = deriveEncoder
    given Encoder.AsObject[Group] = deriveEncoder
    given Encoder.AsObject[Grouping] = deriveEncoder
    given Encoder.AsObject[Proposal] = deriveEncoder
    given Encoder.AsObject[Decision] = deriveEncoder
  }
}
