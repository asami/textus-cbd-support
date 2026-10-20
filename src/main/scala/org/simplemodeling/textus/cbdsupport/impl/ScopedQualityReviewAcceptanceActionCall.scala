package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.CbdReviewAdminService
import org.simplemodeling.textus.cbdsupport.runtime.ScopedQualityReviewProvider
import org.simplemodeling.textus.cbdsupport.value

/*
 * @since   Oct. 18, 2026
 * @version Oct. 18, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ScopedQualityReviewAcceptanceActionCall(
  core: ActionCall.Core,
  override val action: CbdReviewAdminService.ScopedQualityReviewAcceptance
) extends CbdReviewAdminService.AcceptScopedQualityActionCall {
  protected def build_Program: ExecUowM[OperationResponse] = exec_from {
    value.ScopedQualityReviewAcceptance.createC(action.record)
      .flatMap(input => ScopedQualityReviewProvider.acceptC(core, input))
      .map(result => OperationResponse(result.toRecord()))
  }
}
