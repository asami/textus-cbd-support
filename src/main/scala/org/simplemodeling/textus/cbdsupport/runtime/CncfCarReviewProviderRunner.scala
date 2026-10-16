package org.simplemodeling.textus.cbdsupport.runtime

import scala.util.control.NonFatal

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.provider.{ProviderCall, ProviderEngine, ProviderRequest}
import org.goldenport.cncf.unitofwork.ExecUowM

/*
 * @since   Jul. 16, 2026
 * @version Oct. 16, 2026
 * @author  ASAMI, Tomoharu
 */
final class CncfCarReviewProviderRunner(
  actionCore: ActionCall.Core,
  delegate: CarReviewProviderRunner
) extends CarReviewProviderRunner {
  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult =
    ProviderEngine.execute(Call(_core("execute", request), request, delegate)) match {
      case Consequence.Success(value) => value
      case Consequence.Failure(_) => ProviderBundleRunnerResult.Failed("provider-engine-failed", "Provider execution failed at the CNCF boundary.", request.startedAtMillis)
    }

  def cancel(request: ProviderBundleExecutionRequest): Unit = {
    val _ = cancelC(request)
  }

  def cancelC(request: ProviderBundleExecutionRequest): Consequence[Unit] =
    try {
      ProviderEngine.execute(CancellationCall(_core("cancel", request), request, delegate)) match {
        case Consequence.Success(_) => Consequence.success(())
        case Consequence.Failure(_) => Consequence.operationInvalid("provider-cancellation-failed")
      }
    } catch {
      case NonFatal(_) => Consequence.operationInvalid("provider-cancellation-failed")
    }

  private def _core(operation: String, request: ProviderBundleExecutionRequest): ProviderCall.Core =
    ProviderCall.Core(
      ProviderRequest("cbd-car-review-provider", operation, _attributes(request)),
      actionCore.executionContext,
      actionCore.component,
      actionCore.correlationId
    )

  private def _attributes(request: ProviderBundleExecutionRequest): Map[String, String] =
    Map(
      "review_id" -> request.reviewId.value,
      "provider_id" -> request.provider.id.value,
      "provider_version" -> request.provider.version.value,
      "target_digest" -> request.target.digest.value
    )

  private final case class Call(
    core: ProviderCall.Core,
    request: ProviderBundleExecutionRequest,
    delegate: CarReviewProviderRunner
  ) extends ProviderCall[ProviderBundleRunnerResult] {
    protected def build_Program: ExecUowM[ProviderBundleRunnerResult] =
      provider_step(
        "execute-car-review-provider",
        Map(
          "provider_id" -> request.provider.id.value,
          "provider_version" -> request.provider.version.value,
          "target_digest" -> request.target.digest.value
        )
      ) {
        try {
          Consequence.success(delegate.execute(request))
        } catch {
          case NonFatal(_) => Consequence.success(ProviderBundleRunnerResult.Failed("provider-runner-exception", "Provider runner raised an internal failure.", request.startedAtMillis))
        }
      }
  }

  private final case class CancellationCall(
    core: ProviderCall.Core,
    request: ProviderBundleExecutionRequest,
    delegate: CarReviewProviderRunner
  ) extends ProviderCall[Unit] {
    protected def build_Program: ExecUowM[Unit] =
      provider_step(
        "cancel-car-review-provider",
        Map(
          "provider_id" -> request.provider.id.value,
          "provider_version" -> request.provider.version.value,
          "target_digest" -> request.target.digest.value
        )
      ) {
        try {
          delegate.cancel(request)
          Consequence.success(())
        } catch {
          case NonFatal(_) => Consequence.operationInvalid("provider-cancellation-failed")
        }
      }
  }
}
