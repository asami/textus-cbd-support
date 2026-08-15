package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.action.ActionCall

/*
 * @since   Jul. 16, 2026
 *  version Jul. 24, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final case class ProviderBundleExecutionRequest(
  reviewId: ReviewId,
  target: ReviewTarget,
  provider: ReviewProviderIdentity,
  availability: ProviderBundleAvailability,
  descriptor: String,
  providerRequest: String,
  startedAtMillis: Long,
  cancellationRequested: Boolean = false
)

sealed trait ProviderBundleRunnerResult {
  def completedAtMillis: Long
}

object ProviderBundleRunnerResult {
  final case class Completed(
    bundle: String,
    completedAtMillis: Long
  ) extends ProviderBundleRunnerResult

  final case class Failed(
    code: String,
    message: String,
    completedAtMillis: Long
  ) extends ProviderBundleRunnerResult
}

trait CarReviewProviderRunner {
  def execute(request: ProviderBundleExecutionRequest): ProviderBundleRunnerResult
  def cancel(request: ProviderBundleExecutionRequest): Unit
}

sealed trait ProviderBundleExecutionOutcome

object ProviderBundleExecutionOutcome {
  final case class Admitted(
    value: AdmittedProviderBundle,
    fromCache: Boolean
  ) extends ProviderBundleExecutionOutcome

  final case class Refused(
    value: ProviderBundleUnknown
  ) extends ProviderBundleExecutionOutcome
}

/**
 * Executes one provider through its bounded contract and turns every
 * non-admitted outcome into attributable provider state. The coordinator keeps
 * a request-digest cache so an already admitted bundle is never re-run.
 */
final class CarReviewProviderExecutionCoordinator {
  private var _admitted = Map.empty[(ReviewProviderIdentity, ReviewDigest), AdmittedProviderBundle]

  def execute(
    request: ProviderBundleExecutionRequest,
    runner: CarReviewProviderRunner
  ): ProviderBundleExecutionOutcome = synchronized {
    _request_digest(request) match {
      case Left(code) => _refused(request.provider, "incompatible", code, runfailure = false)
      case Right(digest) =>
        _admitted.get((request.provider, digest)) match {
          case Some(value) => ProviderBundleExecutionOutcome.Admitted(value, fromCache = true)
          case None => _execute_new(request, runner, digest, CarReviewProviderBundleAdmission.admit)
        }
    }
  }

  /** Executes a quality provider only after finite-cost preflight and authority/redaction admission. */
  def execute(
    request: ProviderBundleExecutionRequest,
    runner: CarReviewProviderRunner,
    qualityPolicy: CarReviewQualityProviderPolicy
  ): ProviderBundleExecutionOutcome = synchronized {
    CarReviewQualityProviderAdmission.preflight(request.provider, qualityPolicy) match {
      case Left(value) => ProviderBundleExecutionOutcome.Refused(value)
      case Right(_) =>
        _request_digest(request) match {
          case Left(code) => _refused(request.provider, "incompatible", code, runfailure = false)
          case Right(digest) =>
            _admitted.get((request.provider, digest)) match {
              case Some(value) => ProviderBundleExecutionOutcome.Admitted(value, fromCache = true)
              case None => _execute_new(request, runner, digest, context => CarReviewQualityProviderAdmission.admit(context, qualityPolicy))
            }
        }
    }
  }

  def execute(
    request: ProviderBundleExecutionRequest,
    registry: CarReviewProviderRegistry
  ): ProviderBundleExecutionOutcome = synchronized {
    _execute_registered(request, registry, identity)
  }

  def execute(
    request: ProviderBundleExecutionRequest,
    registry: CarReviewProviderRegistry,
    actioncore: ActionCall.Core
  ): ProviderBundleExecutionOutcome = synchronized {
    _execute_registered(request, registry, runner => new CncfCarReviewProviderRunner(actioncore, runner))
  }

  private def _execute_registered(
    request: ProviderBundleExecutionRequest,
    registry: CarReviewProviderRegistry,
    runnertransform: CarReviewProviderRunner => CarReviewProviderRunner
  ): ProviderBundleExecutionOutcome =
    registry.registrationFor(request.provider) match {
      case None => _refused(request.provider, "unavailable", "provider-not-registered", runfailure = false)
      case Some(registration) =>
        CarReviewProviderBundleAdmission.describeDescriptor(request.descriptor) match {
          case Right(descriptor) if descriptor == registration.descriptor => execute(request, runnertransform(registration.runner))
          case _ => _refused(request.provider, "incompatible", "provider-registration-mismatch", runfailure = false)
        }
    }

  private def _execute_new(
    request: ProviderBundleExecutionRequest,
    runner: CarReviewProviderRunner,
    requestdigest: ReviewDigest,
    admit: ProviderBundleAdmissionContext => ProviderBundleAdmissionOutcome
  ): ProviderBundleExecutionOutcome =
    if request.cancellationRequested then {
      runner.cancel(request)
      _refused(request.provider, "cancelled", "provider-cancelled", runfailure = false)
    } else request.availability match {
      case ProviderBundleAvailability.Enabled =>
        runner.execute(request) match {
          case ProviderBundleRunnerResult.Completed(bundle, completedat) =>
            _completed(request, bundle, completedat, runner, requestdigest, admit)
          case ProviderBundleRunnerResult.Failed(code, _, _) =>
            _refused(request.provider, "failed", _failure_code(code), runfailure = true)
        }
      case availability =>
        _refused(request.provider, availability.state.value, s"provider-${availability.state.value}", availability.runFailure)
    }

  private def _completed(
    request: ProviderBundleExecutionRequest,
    bundle: String,
    completedat: Long,
    runner: CarReviewProviderRunner,
    requestdigest: ReviewDigest,
    admit: ProviderBundleAdmissionContext => ProviderBundleAdmissionOutcome
  ): ProviderBundleExecutionOutcome =
    if completedat < request.startedAtMillis then
      _refused(request.provider, "failed", "provider-time-invalid", runfailure = true)
    else CarReviewProviderBundleAdmission.timeoutMillis(request.providerRequest) match {
      case Right(timeout) if completedat - request.startedAtMillis > timeout =>
        runner.cancel(request)
        _refused(request.provider, "failed", "provider-timeout", runfailure = true)
      case Right(_) =>
        admit(ProviderBundleAdmissionContext(
          request.reviewId,
          request.target,
          ProviderBundleAvailability.Enabled,
          request.descriptor,
          request.providerRequest,
          bundle
        )) match {
          case ProviderBundleAdmissionOutcome.Admitted(value) if value.provider == request.provider =>
            _admitted = _admitted.updated((request.provider, requestdigest), value)
            ProviderBundleExecutionOutcome.Admitted(value, fromCache = false)
          case ProviderBundleAdmissionOutcome.Admitted(_) =>
            _refused(request.provider, "incompatible", "provider-identity-mismatch", runfailure = false)
          case ProviderBundleAdmissionOutcome.Refused(value) =>
            ProviderBundleExecutionOutcome.Refused(value)
        }
      case Left(code) => _refused(request.provider, "incompatible", code, runfailure = false)
    }

  private def _request_digest(request: ProviderBundleExecutionRequest): Either[String, ReviewDigest] =
    CarReviewProviderBundleAdmission.requestDigest(request.providerRequest)

  private def _failure_code(value: String): String =
    Option(value).map(_.trim).filter(_.matches("^[A-Za-z0-9][A-Za-z0-9._:/-]*$")).getOrElse("provider-failed")

  private def _refused(
    provider: ReviewProviderIdentity,
    state: String,
    code: String,
    runfailure: Boolean
  ): ProviderBundleExecutionOutcome.Refused =
    ProviderBundleExecutionOutcome.Refused(ProviderBundleUnknown(
      Some(provider),
      ReviewProviderState(state),
      ReviewLimitation(
        code,
        ReviewLimitationScope("provider"),
        Some(provider.id.value),
        s"Provider execution did not produce an admitted bundle: $code.",
        retryable = state == "unavailable" || state == "cancelled"
      ),
      runfailure
    ))
}
