package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.action.ActionCall
import org.goldenport.Consequence
import scala.util.control.NonFatal

/*
 * @since   Jul. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * CBD-owned execution entry point for registered CAR Review providers.
 *
 * A provider runner is intentionally wrapped only after registry selection.
 * This preserves immutable descriptor registration while making every
 * production invocation cross the CNCF ProviderCall boundary.
 */
final class CarReviewProviderExecutionApplication(
  registry: CarReviewProviderRegistry,
  coordinator: CarReviewProviderExecutionCoordinator
) {
  def execute(
    actionCore: ActionCall.Core,
    request: ProviderBundleExecutionRequest
  ): ProviderBundleExecutionOutcome =
    coordinator.execute(request, registry, actionCore)

  def executeForCanonicalReport(
    actionCore: ActionCall.Core,
    request: ProviderBundleExecutionRequest
  ): CanonicalProviderBundleExecutionOutcome =
    coordinator.executeForCanonicalReport(request, registry, actionCore)

  def cancelRegistered(
    actionCore: ActionCall.Core,
    request: ProviderBundleExecutionRequest
  ): Consequence[Unit] =
    if actionCore == null || request == null || request.provider == null || request.descriptor == null then
      Consequence.operationInvalid("provider-cancellation-request-invalid")
    else try {
      registry.registrationFor(request.provider) match {
        case None => Consequence.operationInvalid("provider-not-registered")
        case Some(registration) =>
          CarReviewProviderBundleAdmission.describeDescriptor(request.descriptor) match {
            case Right(descriptor) if descriptor == registration.descriptor =>
              new CncfCarReviewProviderRunner(actionCore, registration.runner).cancelC(request)
            case _ => Consequence.operationInvalid("provider-registration-mismatch")
          }
      }
    } catch {
      case NonFatal(_) => Consequence.operationInvalid("provider-cancellation-failed")
    }
}
