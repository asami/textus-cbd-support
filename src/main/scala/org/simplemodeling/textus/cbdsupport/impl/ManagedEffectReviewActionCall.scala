package org.simplemodeling.textus.cbdsupport.impl

import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.cbdsupport.CbdSupportComponent.CbdReviewAdminService
import org.simplemodeling.textus.cbdsupport.runtime.CarReviewManagedEffectProvider
import org.simplemodeling.textus.cbdsupport.value

/*
 * @since   Oct. 17, 2026
 * @version Oct. 17, 2026
 */
final case class ManagedEffectReviewActionCall(
  core: ActionCall.Core,
  override val action: CbdReviewAdminService.ManagedEffectReviewRequest
) extends CbdReviewAdminService.ReviewManagedEffectsActionCall {
  protected def build_Program: ExecUowM[OperationResponse] = exec_from {
    value.ManagedEffectReviewRequest.createC(action.record)
      .flatMap(input => CarReviewManagedEffectProvider.reviewC(core, input))
      .map(result => OperationResponse(result.toRecord()))
  }
}
