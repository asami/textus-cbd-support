package org.simplemodeling.textus.cbdsupport.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext

/*
 * @since   Aug. 15, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
/**
 * A server-owned, single-use production Review execution capsule.  Its
 * registry and provider documents are deliberately private: callers can only
 * choose the admitted Review start identity, never a provider, rule, policy,
 * template, or gate.
 */
final class CarReviewProductionExecution private (
  val plan: CarReviewExecutionPlan,
  private val _execution: CarReviewRegisteredReviewExecution,
  private val _application: CarReviewRegisteredProviderReviewApplication
) {
  def execute(
    actorRoles: Set[String],
    completedAt: ReviewInstant
  ): Consequence[CarReviewCanonicalResponse] =
    (_instant(plan.request.startedAt), _instant(completedAt)) match {
      case (_, None) => Consequence.operationInvalid("review-completed-at-invalid")
      case (Some(startedat), Some(completedat)) if completedat.isBefore(startedat) =>
        Consequence.operationInvalid("review-completed-before-start")
      case (Some(_), Some(_)) =>
        _application.execute(
          _execution.copy(template = _execution.template.copy(
            createdAt = completedAt,
            execution = _execution.template.execution.copy(completedAt = completedAt)
          )),
          actorRoles
        )
      case (None, _) => Consequence.operationInvalid("review-started-at-invalid")
    }

  private def _instant(value: ReviewInstant): Option[Instant] =
    scala.util.Try(Instant.parse(value.value)).toOption
}

object CarReviewProductionExecution {
  private val _profiles = Set("development", "ci", "release", "server")
  private val _provider = ReviewProviderIdentity(
    ReviewProviderId("cbd-initial-static-quality"),
    ReviewVersion("1.0.0")
  )
  private val _rule_set = ReviewRuleIdentity(
    ReviewRuleId("cbd-initial-static-quality.car-review"),
    ReviewVersion("1.0.0")
  )
  private val _quality_policy = CarReviewQualityProviderPolicy(
    CarReviewQualityProviderAuthority.Deterministic,
    declaredCostUnits = 0L,
    maximumCostUnits = 0L
  )

  def create(
    request: ReviewStartRequest
  )(using ctx: ExecutionContext): Consequence[CarReviewProductionExecution] =
    if (!_profiles.contains(request.profile.value))
      Consequence.operationInvalid("review-profile-unsupported")
    else if (!CarReviewProductionIdentity.isValid(request.reviewId.value))
      Consequence.operationInvalid("review-id-invalid")
    else _started_at_millis(request.startedAt) match {
      case Left(code) => Consequence.operationInvalid(code)
      case Right(startedatmillis) => {
      val runnerprofile = CarReviewInitialStaticQualityProviderProfile(
        _provider,
        _rule_set,
        CarReviewInitialStaticQualityEvidence(
          request.target.digest.value,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None
        )
      )
      val runner = new CarReviewInitialStaticQualityProviderRunner(runnerprofile)
      val descriptor = CarReviewInitialStaticQualityProviderRunner.descriptorDocument(runnerprofile)
      val providerrequest = CarReviewInitialStaticQualityProviderRunner.requestDocument(request.reviewId, request.target)
      val registry = new CarReviewProviderRegistry()
      registry.register(descriptor, runner, _quality_policy) match {
        case Left(error) => Consequence.operationInvalid(error.code)
        case Right(_) =>
          _plan(request, descriptor, providerrequest).fold(
            error => Consequence.operationInvalid(s"${error.code}: ${error.message}"),
            plan => {
              val template = _template(plan, _report_id(ctx))
              val selection = CarReviewRegisteredProviderSelection(
                descriptor,
                ProviderBundleExecutionRequest(
                  request.reviewId,
                  request.target,
                  _provider,
                  ProviderBundleAvailability.Enabled,
                  descriptor,
                  providerrequest,
                  startedatmillis
                )
              )
              Consequence.success(new CarReviewProductionExecution(
                plan,
                CarReviewRegisteredReviewExecution(plan, template, Vector(selection)),
                new CarReviewRegisteredProviderReviewApplication(registry)
              ))
            }
          )
      }
      }
    }

  private def _plan(
    request: ReviewStartRequest,
    descriptor: String,
    providerrequest: String
  ): Either[CarReviewExecutionPlanFailure, CarReviewExecutionPlan] =
    for {
      descriptordigest <- CarReviewProviderBundleAdmission.descriptorDigest(descriptor).left.map(code => CarReviewExecutionPlanFailure(code, "Production provider descriptor is invalid."))
      _ <- CarReviewProviderBundleAdmission.requestDigest(providerrequest).left.map(code => CarReviewExecutionPlanFailure(code, "Production provider request is invalid."))
      selectiondigest <- CarReviewProviderSelectionPolicy.reuseDigest(
        descriptordigest,
        providerrequest,
        ProviderBundleAvailability.Enabled,
        _quality_policy
      ).left.map(code => CarReviewExecutionPlanFailure(code, "Production provider request is invalid."))
      reuseinput = CarReviewReuseKeyInput(
        CarReviewReuseKey.DEFINITION_ID,
        ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
        request.target,
        request.profile,
        None,
        Vector(_rule_set),
        Vector(CarReviewReuseProviderSelection(_provider, _rule_set, selectiondigest)),
        Vector.empty,
        _policy_bindings(request.profile)
      )
      plan <- CarReviewExecutionPlan.create(request, reuseinput)
    } yield plan

  private def _template(
    plan: CarReviewExecutionPlan,
    reportid: ReviewReportId
  ): CarReviewReport = {
    val request = plan.request
    CarReviewReport(
      ReviewSchemaVersion(CarReviewVocabulary.SCHEMA_VERSION),
      ReviewDocumentType(CarReviewVocabulary.DOCUMENT_TYPE),
      reportid,
      ReviewDigest("sha256:" + ("0" * 64)),
      request.reviewId,
      request.startedAt,
      request.target,
      request.profile,
      ReviewExecution(request.startedAt, request.startedAt, Vector.empty),
      Vector.empty,
      Vector.empty,
      Vector(ReviewAssessment(
        ReviewCapabilityId("quality.domain.identity-consistency"),
        ReviewApplicability("unknown"),
        ReviewMaturity("unassessed"),
        None,
        ReviewConfidence("low"),
        Vector.empty,
        Vector.empty,
        Vector.empty,
        Vector.empty,
        Vector.empty
      )),
      Vector.empty,
      None,
      ReviewGate(
        s"cbd-review.${request.profile.value}",
        ReviewVersion("1.0.0"),
        ReviewGateResult("unknown"),
        Vector(s"CBD ${request.profile.value} profile has not yet reconciled provider observations."),
        Vector.empty
      )
    )
  }

  private def _policy_bindings(profile: ReviewProfile): Vector[CarReviewReusePolicyBinding] =
    Vector("profile", "gate", "reconciliation", "suppression").map { scope =>
      CarReviewReusePolicyBinding(
        scope,
        s"cbd-review-$scope-${profile.value}",
        ReviewVersion("1.0.0"),
        ReviewDigest(_sha256(s"textus.cbd.review-policy-binding.v1|$scope|${profile.value}"))
      )
    }

  private def _started_at_millis(value: ReviewInstant): Either[String, Long] =
    scala.util.Try(java.time.Instant.parse(value.value).toEpochMilli).toEither.left.map(_ => "review-started-at-invalid")

  private def _report_id(ctx: ExecutionContext): ReviewReportId =
    ReviewReportId(s"report-${ctx.idGeneration.opaqueId("cbd.review.report")}")

  private def _sha256(value: String): String = {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    "sha256:" + bytes.map(byte => f"${byte & 0xff}%02x").mkString
  }
}
