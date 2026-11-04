package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.CbdFindingResolutionService
import org.simplemodeling.textus.cbdsupport.runtime.FindingResolutionApplication

/*
 * @since   Nov.  5, 2026
 * @version Nov.  5, 2026
 */
/** The ordinary generated service adapter; host selection is explicit. */
object FindingResolutionActionCall {
  def serviceFactory(application: Option[FindingResolutionApplication]): CbdSupportComponent.CbdFindingResolutionServiceFactory =
    new OperationFactory(application)

  private final class OperationFactory(private val _application: Option[FindingResolutionApplication])
      extends CbdSupportComponent.CbdFindingResolutionServiceFactory {
    import CbdFindingResolutionService.*
    override def createImportFindingInspectionActionCall(core: ActionCall.Core, action: ImportFindingInspectionRequest): ImportFindingInspectionActionCall =
      ImportFindingInspectionActionCallImpl(core, action)
    override def createGroupFindingResolutionsActionCall(core: ActionCall.Core, action: GroupFindingResolutionsRequest): GroupFindingResolutionsActionCall =
      GroupFindingResolutionsActionCallImpl(core, action)
    override def createProposeFindingResolutionActionCall(core: ActionCall.Core, action: ProposeFindingResolutionRequest): ProposeFindingResolutionActionCall =
      ProposeFindingResolutionActionCallImpl(core, action)
    override def createDecideFindingResolutionActionCall(core: ActionCall.Core, action: DecideFindingResolutionRequest): DecideFindingResolutionActionCall =
      DecideFindingResolutionActionCallImpl(core, action)
    override def createGetFindingResolutionActionCall(core: ActionCall.Core, action: GetFindingResolutionRequest): GetFindingResolutionActionCall =
      GetFindingResolutionActionCallImpl(core, action)

    private def _call(core: ActionCall.Core, operation: String, record: Record): Consequence[OperationResponse] = {
      given ExecutionContext = core.executionContext
      _application match {
        case Some(application) => record.getString("requestJson") match {
          case Some(document) => application.execute(operation, document).map(response =>
            OperationResponse(Record.dataAuto("status" -> response.status, "resultJson" -> response.resultJson)))
          case None => Consequence.argumentInvalid("Finding resolution requires requestJson")
        }
        case None => Consequence.success(OperationResponse(Record.dataAuto("status" -> "UNAVAILABLE",
          "resultJson" -> "{\"status\":\"UNAVAILABLE\",\"reason\":\"Host finding resolution application not bound\"}")))
      }
    }

    private final case class ImportFindingInspectionActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdFindingResolutionService.ImportFindingInspectionRequest
    ) extends CbdFindingResolutionService.ImportFindingInspectionActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _call(core, "importFindingInspection", action.record)
      }
    }

    private final case class GroupFindingResolutionsActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdFindingResolutionService.GroupFindingResolutionsRequest
    ) extends CbdFindingResolutionService.GroupFindingResolutionsActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _call(core, "groupFindingResolutions", action.record)
      }
    }

    private final case class ProposeFindingResolutionActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdFindingResolutionService.ProposeFindingResolutionRequest
    ) extends CbdFindingResolutionService.ProposeFindingResolutionActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _call(core, "proposeFindingResolution", action.record)
      }
    }

    private final case class DecideFindingResolutionActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdFindingResolutionService.DecideFindingResolutionRequest
    ) extends CbdFindingResolutionService.DecideFindingResolutionActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _call(core, "decideFindingResolution", action.record)
      }
    }

    private final case class GetFindingResolutionActionCallImpl(
      core: ActionCall.Core,
      override val action: CbdFindingResolutionService.GetFindingResolutionRequest
    ) extends CbdFindingResolutionService.GetFindingResolutionActionCall {
      protected def build_Program: ExecUowM[OperationResponse] = exec_from {
        _call(core, "getFindingResolution", action.record)
      }
    }
  }
}
