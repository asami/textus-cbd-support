package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.CbdReviewAdminService
import org.simplemodeling.textus.cbdsupport.runtime.ScopedQualityReviewProvider
import org.simplemodeling.textus.cbdsupport.value

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ScopedQualityReviewActionCall(
  core: ActionCall.Core,
  override val action: CbdReviewAdminService.ScopedQualityReviewRequest
) extends CbdReviewAdminService.ReviewScopedQualityActionCall {
  protected def build_Program: ExecUowM[OperationResponse] = exec_from {
    value.ScopedQualityReviewRequest.createC(action.record)
      .flatMap(input => ScopedQualityReviewProvider.reviewC(core, input))
      .map(result => OperationResponse(result.toRecord()))
  }
}
