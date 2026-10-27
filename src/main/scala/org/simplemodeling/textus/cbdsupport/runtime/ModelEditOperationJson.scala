package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import io.circe.{Codec, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.jawn.JawnParser
import io.circe.syntax.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.CbdModelEditService
import ModelEditSession.*
import ModelEditCandidateCodec.{given, *}
import ModelEditCommandCodec.given

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Closed external selectors; these contain no server admission/evidence fields. */
object ModelEditOperationJson {
  final case class Selection(session: InternalModelRecordReference, candidate: InternalModelRecordReference)
  final case class ContextRequest(selection: Selection, context: ModelEditPalette.Context)
  final case class EditRequest(selection: Selection, context: ModelEditPalette.Context,
    operation: Operation, successorCandidate: InternalModelRecordReference)
  private val _parser = JawnParser(allowDuplicateKeys = false)
  given Codec[ModelEditPalette.Surface] = enumeration(ModelEditPalette.Surface.values.toVector, _.toString)
  given Codec.AsObject[ModelEditPalette.Context] = closed[ModelEditPalette.Context]("surface", "scope", "subject")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[Selection] = closed[Selection]("session", "candidate")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[ContextRequest] = closed[ContextRequest]("selection", "context")(using deriveDecoder, deriveEncoder)
  given Codec.AsObject[EditRequest] = closed[EditRequest]("selection", "context", "operation", "successorCandidate")(using deriveDecoder, deriveEncoder)

  def selection(value: Selection): String = value.asJson.noSpaces
  def context(value: ContextRequest): String = value.asJson.noSpaces
  def edit(value: EditRequest): String = value.asJson.noSpaces
  def decodeSelection(text: String): Either[String, Selection] = _parse(text).flatMap(_.as[Selection].left.map(_.message))
  def decodeContext(text: String): Either[String, ContextRequest] = _parse(text).flatMap(_.as[ContextRequest].left.map(_.message))
  def decodeEdit(text: String): Either[String, EditRequest] = _parse(text).flatMap(_.as[EditRequest].left.map(_.message))

  private[runtime] def record(value: InternalModelRecordReference): Json = value.asJson
  private[runtime] def candidate(value: Candidate): Json = ModelEditCandidateCodec.encode(value)
  private[runtime] def history(session: ModelEditSession): Json = Json.fromValues(session.history.map(value =>
    Json.obj("operation" -> value.operation.asJson, "previousCandidate" -> value.previousCandidate.asJson,
      "successorCandidate" -> value.successorCandidate.asJson, "authorization" -> value.authorization.reference.asJson)))
  private[runtime] def semanticDiff(value: InternalModelSemanticDiff): Json =
    _parser.parse(new String(InternalModelSemanticDiffCodec.encode(value), StandardCharsets.UTF_8)).toOption.get
  private def _parse(text: String): Either[String, Json] = _parser.parse(text).left.map(_.message)

  /** Supported ComponentFactory adapter for the existing generated service. */
  def serviceFactory(application: Option[ModelEditApplication]): CbdSupportComponent.CbdModelEditServiceFactory =
    new OperationFactory(application)

  private final class OperationFactory(private val _application: Option[ModelEditApplication]) extends CbdSupportComponent.CbdModelEditServiceFactory {
    import CbdModelEditService.*
    override def createInspectModelEditActionCall(core: ActionCall.Core, action: InspectModelEditRequest): InspectModelEditActionCall =
      InspectModelEditActionCallImpl(core, action)
    override def createListModelEditOperationsActionCall(core: ActionCall.Core, action: ListModelEditOperationsRequest): ListModelEditOperationsActionCall =
      ListModelEditOperationsActionCallImpl(core, action)
    override def createGetModelEditCandidateActionCall(core: ActionCall.Core, action: GetModelEditCandidateRequest): GetModelEditCandidateActionCall =
      GetModelEditCandidateActionCallImpl(core, action)
    override def createApplyModelEditActionCall(core: ActionCall.Core, action: ApplyModelEditRequest): ApplyModelEditActionCall =
      ApplyModelEditActionCallImpl(core, action)
    override def createGetModelEditViewActionCall(core: ActionCall.Core, action: GetModelEditViewRequest): GetModelEditViewActionCall =
      GetModelEditViewActionCallImpl(core, action)
    override def createGetModelEditDiffActionCall(core: ActionCall.Core, action: GetModelEditDiffRequest): GetModelEditDiffActionCall =
      GetModelEditDiffActionCallImpl(core, action)
    override def createGetModelEditTracesActionCall(core: ActionCall.Core, action: GetModelEditTracesRequest): GetModelEditTracesActionCall =
      GetModelEditTracesActionCallImpl(core, action)
    override def createGetModelEditReviewActionCall(core: ActionCall.Core, action: GetModelEditReviewRequest): GetModelEditReviewActionCall =
      GetModelEditReviewActionCallImpl(core, action)

    private def _model_edit_call(core: ActionCall.Core, operation: String, record: Record): Consequence[OperationResponse] = {
      given org.goldenport.cncf.context.ExecutionContext = core.executionContext
      _application match {
        case Some(application) => application.execute(operation, record.getString("requestJson").get)
          .map(response => OperationResponse(Record.dataAuto("status" -> response.status, "resultJson" -> response.resultJson, "html" -> response.html)))
        case None => Consequence.success(OperationResponse(Record.dataAuto("status" -> "UNAVAILABLE",
          "resultJson" -> "{\"status\":\"UNAVAILABLE\",\"reason\":\"Host Model Edit application not bound\"}", "html" -> "")))
      }
    }

    private final case class InspectModelEditActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.InspectModelEditRequest
    ) extends CbdModelEditService.InspectModelEditActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "inspectModelEdit", action.record)
      }
    }

    private final case class ListModelEditOperationsActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.ListModelEditOperationsRequest
    ) extends CbdModelEditService.ListModelEditOperationsActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "listModelEditOperations", action.record)
      }
    }

    private final case class GetModelEditCandidateActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.GetModelEditCandidateRequest
    ) extends CbdModelEditService.GetModelEditCandidateActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "getModelEditCandidate", action.record)
      }
    }

    private final case class ApplyModelEditActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.ApplyModelEditRequest
    ) extends CbdModelEditService.ApplyModelEditActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "applyModelEdit", action.record)
      }
    }

    private final case class GetModelEditViewActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.GetModelEditViewRequest
    ) extends CbdModelEditService.GetModelEditViewActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "getModelEditView", action.record)
      }
    }

    private final case class GetModelEditDiffActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.GetModelEditDiffRequest
    ) extends CbdModelEditService.GetModelEditDiffActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "getModelEditDiff", action.record)
      }
    }

    private final case class GetModelEditTracesActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.GetModelEditTracesRequest
    ) extends CbdModelEditService.GetModelEditTracesActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "getModelEditTraces", action.record)
      }
    }

    private final case class GetModelEditReviewActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdModelEditService.GetModelEditReviewRequest
    ) extends CbdModelEditService.GetModelEditReviewActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _model_edit_call(core, "getModelEditReview", action.record)
      }
    }

  }
}
