package org.simplemodeling.textus.cbdsupport.runtime

import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import ModelEditSession.*
import ModelEditCandidateCodec.given
import ModelEditSourceProjectionCodec.given
import ModelEditOperationJson.{given, *}

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** One host-admitted local single-writer session, shared by ordinary transports. */
final class ModelEditApplication private (
  private var _session: ModelEditSession,
  private val _source: ModelEditSourceProjection.Selection,
  private val _bindings: Vector[ModelEditAuthorization.Binding],
  private val _policy: FindingPolicy,
  private val _resolutions: Vector[ResolutionEvidence],
  private val _behavior: Vector[ModelEditReview.BehaviorCoverage],
  private val _demands: Vector[ModelEditReview.CapabilityDemand],
  private val _diff: Option[ModelEditApplication.DiffBinding]
) {
  import ModelEditApplication.*

  def execute(operation: String, requestJson: String)(using ctx: ExecutionContext): Consequence[Response] =
    for {
      binding <- ModelEditAuthorization.authorize(_bindings, _session, operation == "applyModelEdit")
      _ <- if (operations.contains(operation)) Consequence.unit else Consequence.operationInvalid("Unknown Model Edit Operation")
      response <- _execute(operation, requestJson, binding).fold(Consequence.operationInvalid, Consequence.success)
    } yield response

  private def _execute(operation: String, document: String, binding: ModelEditAuthorization.Binding): Either[String, Response] = {
    if (operation == "applyModelEdit") for {
      request <- decodeEdit(document)
      _ <- _selected(request.selection)
      grant <- binding.authorization.toRight("Model Edit mutation grant unavailable")
      applied <- ModelEditModelMutation.applyOperation(_session, request.context, request.operation,
        request.successorCandidate, grant, _resolutions, _source, _demands.map(value => value.useCase -> value).toMap).left.map(_.toString)
    } yield {
      _session = applied.session
      _response(operation, "OK", ModelEditOperationJson.candidate(_session.candidate), ModelEditWebView.confirmation(applied.confirmation))
    }
    else if (operation == "listModelEditOperations") for {
      request <- decodeContext(document)
      _ <- _selected(request.selection)
      grant <- binding.authorization.toRight("Model Edit mutation grant unavailable")
      palette <- ModelEditPalette.inspect(_session, request.context, grant).left.map(_.toString)
    } yield _response(operation, "OK", Json.obj("context" -> request.context.asJson,
      "authorization" -> record(grant.reference), "entries" -> Json.fromValues(palette.entries.map(value =>
        Json.obj("kind" -> Json.fromString(value.kind.toString), "label" -> Json.fromString(value.label),
          "inputs" -> Json.fromValues(value.inputs.map(Json.fromString)))))), ModelEditWebView.palette(palette))
    else for {
      selection <- decodeSelection(document)
      _ <- _selected(selection)
      response <- _read(operation)
    } yield response
  }

  private def _read(operation: String): Either[String, Response] = operation match {
    case "getModelEditCandidate" => Right(_response(operation, "OK", ModelEditOperationJson.candidate(_session.candidate)))
    case "getModelEditReview" => ModelEditReview.reviewSource(_session, _policy, _source, _resolutions, _behavior, _demands)
      .map(value => _response(operation, "OK", value.asJson))
    case "getModelEditDiff" =>
      val diff = _diff.filter { value =>
        val selected = value.admission.diff
        selected.candidateReference == _session.candidate.reference && selected.scope.componentIdentity == _session.scope.component.value &&
          selected.scope.projectionContextIdentity == _session.scope.context.value && value.mapping.session == _session.reference &&
          value.mapping.operations == _session.history.map(_.operation.reference) && value.mapping.sourceArtifact == _source.sourceArtifact
      }
      Right(_response(operation, if (diff.isDefined) "OK" else "UNAVAILABLE", Json.obj(
        "operations" -> history(_session), "semanticDiff" -> diff.map(value => semanticDiff(value.admission.diff)).getOrElse(Json.Null),
        "availability" -> Json.fromString(if (diff.isDefined) "ExactPhase10Admission" else if (_diff.isDefined) "StalePhase10Admission" else "Phase10AdmissionNotSupplied"))))
    case _ => ModelEditViewProjection.project(_session, _source, _demands.map(value => value.useCase -> value).toMap).map { view =>
      val value = if (operation == "getModelEditTraces") Json.obj("traces" -> view.traces.asJson, "capabilityDemands" -> view.capabilityDemands.asJson)
        else Json.obj("confirmation" -> Json.obj("state" -> Json.fromString(view.confirmation.state.toString),
          "canonicalSource" -> Json.obj("owner" -> Json.fromString(_source.publication.source.owner),
            "identity" -> Json.fromString(_source.publication.source.identity), "revision" -> Json.fromString(_source.publication.source.revision)),
          "candidate" -> record(view.confirmation.candidate), "operations" -> Json.fromValues(view.confirmation.operations.map(record)),
          "abandoned" -> Json.fromBoolean(view.confirmation.abandoned)), "actorGoals" -> view.actorGoals.asJson,
          "eventStorming" -> view.eventStorming.asJson, "traces" -> view.traces.asJson, "capabilityDemands" -> view.capabilityDemands.asJson)
      _response(operation, "OK", value, ModelEditWebView.confirmation(view))
    }
  }

  private def _selected(selection: Selection): Either[String, Unit] =
    Either.cond(selection.session == _session.reference && selection.candidate == _session.candidate.reference, (), "Model Edit requires the exact current session and candidate references")
  private def _response(operation: String, status: String, data: Json, html: String = ""): Response =
    Response(status, Json.obj("schemaVersion" -> Json.fromString("cbd.model-edit-operation.v1"), "operation" -> Json.fromString(operation),
      "status" -> Json.fromString(status), "session" -> record(_session.reference), "candidate" -> record(_session.candidate.reference), "data" -> data).noSpaces, html)
}

object ModelEditApplication {
  private[runtime] final case class DiffBinding(mapping: ModelEditCmlProjection.Mapping, admission: InternalModelSemanticDiffAdmission)
  final case class Response(status: String, resultJson: String, html: String)
  val operations: Set[String] = Set("inspectModelEdit", "listModelEditOperations", "getModelEditCandidate", "applyModelEdit",
    "getModelEditView", "getModelEditDiff", "getModelEditTraces", "getModelEditReview")

  def create(session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    bindings: Vector[ModelEditAuthorization.Binding], policy: FindingPolicy,
    resolutions: Vector[ResolutionEvidence], behavior: Vector[ModelEditReview.BehaviorCoverage],
    demands: Vector[ModelEditReview.CapabilityDemand]): Either[String, ModelEditApplication] =
    _create(session, source, bindings, policy, resolutions, behavior, demands, None)

  /** Runtime-package host receives this admission from the existing Phase10 validator. */
  private[runtime] def createWithDiff(session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    bindings: Vector[ModelEditAuthorization.Binding], policy: FindingPolicy,
    resolutions: Vector[ResolutionEvidence], behavior: Vector[ModelEditReview.BehaviorCoverage],
    demands: Vector[ModelEditReview.CapabilityDemand], mapping: ModelEditCmlProjection.Mapping,
    diff: InternalModelSemanticDiffAdmission): Either[String, ModelEditApplication] = for {
    _ <- ModelEditCmlProjection.validateMapping(mapping, session, diff.candidateAdmission)
    _ <- Either.cond(mapping.sourceArtifact == source.sourceArtifact && mapping.confirmed.plan.selection == source &&
      diff.diff.candidateReference == session.candidate.reference && diff.diff.candidateArtifactReference == mapping.candidateArtifact,
      (), "Model Edit diff must retain the exact admitted source mapping and Phase10 candidate")
    application <- _create(session, source, bindings, policy, resolutions, behavior, demands, Some(DiffBinding(mapping, diff)))
  } yield application

  private def _create(session: ModelEditSession, source: ModelEditSourceProjection.Selection,
    bindings: Vector[ModelEditAuthorization.Binding], policy: FindingPolicy,
    resolutions: Vector[ResolutionEvidence], behavior: Vector[ModelEditReview.BehaviorCoverage],
    demands: Vector[ModelEditReview.CapabilityDemand], diff: Option[DiffBinding]): Either[String, ModelEditApplication] = for {
    _ <- ModelEditSessionCodec.validate(session)
    _ <- ModelEditSourceProjection.validate(source.publication)
    _ <- Either.cond(bindings.map(_.principalId).distinct.size == bindings.size && bindings.forall(value =>
      value.principalId.trim.nonEmpty && value.authorization.forall(grant => grant.session == session.reference && grant.scope == session.scope)), (), "Model Edit host bindings must be unique and exactly scoped")
    _ <- ModelEditViewProjection.project(session, source, demands.map(value => value.useCase -> value).toMap)
    _ <- ModelEditReview.reviewSource(session, policy, source, resolutions, behavior, demands)
  } yield new ModelEditApplication(session, source, bindings, policy, resolutions, behavior, demands, diff)
}
