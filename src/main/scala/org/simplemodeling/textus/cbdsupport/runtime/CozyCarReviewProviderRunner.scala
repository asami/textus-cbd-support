package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets

import org.goldenport.Consequence
import org.goldenport.cncf.context.ScopeContext
import org.goldenport.cncf.processexecution.{ProcessCapabilityId, ProcessExecutionAdmission, ProcessExecutionDriver, ProcessExecutionInput, ProcessExecutionRequest, ProcessExecutionTermination}

/*
 * @since   Jul. 16, 2026
 * @version Jul. 17, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * CBD's logical Cozy evidence-provider capability. The executable location,
 * fixed command arguments, empty/fixed environment, limits, WorkArea inputs,
 * and artifact policy are registered by the CNCF runtime. CBD submits only
 * the provider request bytes to that pre-admitted capability.
 */
object CozyCarReviewProviderProcess {
  val capability: ProcessCapabilityId = ProcessCapabilityId.parseC("cozy-car-review").TAKE
}

object CozyCarReviewProviderRunner {
  /**
   * Resolves the registered Cozy process capability at the invocation scope.
   * Program definition and admission remain runtime-owned; a missing service
   * is a structured failure, never permission to fall back to host execution.
   */
  def fromScopeC(
    admittedTarget: ReviewTarget,
    provider: ReviewProviderIdentity,
    scope: ScopeContext
  ): Consequence[CozyCarReviewProviderRunner] =
    for {
      admission <- scope.processExecutionAdmissionOption.map(Consequence.success).getOrElse(
        Consequence.serviceUnavailable("Cozy provider Process Execution admission is not configured")
      )
      driver <- scope.processExecutionDriverOption.map(Consequence.success).getOrElse(
        Consequence.serviceUnavailable("Cozy provider Process Execution driver is not configured")
      )
    } yield new CozyCarReviewProviderRunner(admittedTarget, provider, admission, driver)
}

/**
 * Process Execution adapter for the existing CBD Review provider protocol.
 * It owns neither a host path nor a command string and maps neutral terminal
 * results back to the stable CBD provider outcomes.
 */
final class CozyCarReviewProviderRunner(
  admittedTarget: ReviewTarget,
  provider: ReviewProviderIdentity,
  admission: ProcessExecutionAdmission,
  driver: ProcessExecutionDriver
) extends CarReviewProviderRunner {
  private var _active: Option[org.goldenport.cncf.processexecution.ProcessExecutionHandle] = None

  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult = synchronized {
    if (request.provider.id != ReviewProviderId("cozy"))
      ProviderBundleRunnerResult.Failed("provider-identity-mismatch", "Configured runner is restricted to the Cozy provider.", request.startedAtMillis)
    else if (request.provider != provider)
      ProviderBundleRunnerResult.Failed("provider-identity-mismatch", "Configured Cozy capability does not match the registered provider identity.", request.startedAtMillis)
    else if (request.target != admittedTarget)
      ProviderBundleRunnerResult.Failed("provider-target-not-admitted", "Configured Cozy target does not match the admitted Review target.", request.startedAtMillis)
    else CarReviewProviderBundleAdmission.timeoutMillis(request.providerRequest) match {
      case Left(code) =>
        ProviderBundleRunnerResult.Failed(code, "Provider request is not admissible for Cozy execution.", request.startedAtMillis)
      case Right(_) =>
        val processrequest = ProcessExecutionRequest(
          CozyCarReviewProviderProcess.capability,
          input = ProcessExecutionInput.Bytes(request.providerRequest.getBytes(StandardCharsets.UTF_8).toVector)
        )
        admission.admitC(processrequest) match {
          case Consequence.Failure(_) =>
            ProviderBundleRunnerResult.Failed("provider-process-not-admitted", "Cozy provider process capability is not admitted.", request.startedAtMillis)
          case Consequence.Success(execution) =>
            driver.startC(execution) match {
              case Consequence.Failure(_) =>
                ProviderBundleRunnerResult.Failed("provider-transport-failed", "Cozy provider process could not be started.", request.startedAtMillis)
              case Consequence.Success(handle) =>
                _active = Some(handle)
                try {
                  handle.awaitC match {
                    case Consequence.Failure(_) =>
                      ProviderBundleRunnerResult.Failed("provider-transport-failed", "Cozy provider process did not return a terminal result.", request.startedAtMillis)
                    case Consequence.Success(result) =>
                      _provider_result(request.startedAtMillis, result)
                  }
                } finally {
                  _active = None
                }
            }
        }
    }
  }

  def cancel(request: ProviderBundleExecutionRequest): Unit = synchronized {
    val _ = request
    _active.foreach(_.cancelC)
    _active = None
  }

  private def _provider_result(
    startedAtMillis: Long,
    result: org.goldenport.cncf.processexecution.ProcessExecutionResult
  ): ProviderBundleRunnerResult =
    result.termination match {
      case ProcessExecutionTermination.Exited(0) =>
        val bundle = new String(result.stdout.content.toArray, StandardCharsets.UTF_8)
        ProviderBundleRunnerResult.Completed(bundle, _completed_at(startedAtMillis, result.elapsedMillis))
      case ProcessExecutionTermination.Exited(_) =>
        ProviderBundleRunnerResult.Failed("provider-command-failed", "Cozy provider process returned a non-zero status.", startedAtMillis)
      case ProcessExecutionTermination.TimedOut =>
        ProviderBundleRunnerResult.Failed("provider-timeout", "Cozy provider process timed out.", startedAtMillis)
      case ProcessExecutionTermination.Cancelled =>
        ProviderBundleRunnerResult.Failed("provider-cancelled", "Cozy provider process was cancelled.", startedAtMillis)
      case ProcessExecutionTermination.OutputLimitExceeded(_) =>
        ProviderBundleRunnerResult.Failed("provider-response-byte-limit", "Cozy provider response exceeded the admitted output limit.", startedAtMillis)
      case ProcessExecutionTermination.ArtifactLimitExceeded =>
        ProviderBundleRunnerResult.Failed("provider-response-byte-limit", "Cozy provider artifacts exceeded the admitted output limit.", startedAtMillis)
      case ProcessExecutionTermination.LaunchFailed =>
        ProviderBundleRunnerResult.Failed("provider-transport-failed", "Cozy provider process could not be launched.", startedAtMillis)
    }

  private def _completed_at(startedAtMillis: Long, elapsedMillis: Long): Long =
    try Math.addExact(startedAtMillis, math.max(0L, elapsedMillis))
    catch {
      case _: ArithmeticException => startedAtMillis
    }
}
