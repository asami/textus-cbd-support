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
final case class ScopedQualityReviewPreparationActionCall(
  core: ActionCall.Core,
  override val action: CbdReviewAdminService.ScopedQualityReviewPreparation
) extends CbdReviewAdminService.PrepareScopedQualityActionCall {
  protected def build_Program: ExecUowM[OperationResponse] = exec_from {
    value.ScopedQualityReviewPreparation.createC(action.record)
      .flatMap(input => ScopedQualityReviewProvider.prepareC(core, input))
      .map(result => OperationResponse(result.toRecord()))
  }
}
