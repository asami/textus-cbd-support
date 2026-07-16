package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.Consequence

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
 * @author  ASAMI, Tomoharu
 */
/** Builds the sole CBD-owned canonical report and gate from already admitted provider bundles. */
final case class CarReviewCanonicalResponse(
  report: CarReviewReport,
  gate: ReviewGate
)

final class CarReviewCanonicalResponseApplication {
  def build(
    template: CarReviewReport,
    bundles: Vector[AdmittedProviderBundleInput],
    actorroles: Set[String]
  ): Consequence[CarReviewCanonicalResponse] =
    template.assessments match {
      case Vector(assessment) =>
        for {
          _ <- CarReviewAuthorization.authorize("review.submit-bundle", actorroles)
          _ <- _validate_template(template)
          _ <- _validate_bundles(bundles)
          reconciled <- _reconcile(bundles)
          assessed = CarReviewAssessmentGateBuilder.build(ReviewAssessmentGateInput(
            assessment.capabilityId,
            reconciled.observations,
            reconciled.evidence,
            template.gate.policyId,
            template.gate.policyVersion
          ))
          report <- _assemble(template, bundles, reconciled, assessed)
        } yield CarReviewCanonicalResponse(report, report.gate)
      case _ =>
        Consequence.operationInvalid("canonical review response requires exactly one configured capability assessment")
    }

  private def _validate_template(template: CarReviewReport): Consequence[Unit] =
    if template.baseline.nonEmpty then
      Consequence.operationInvalid("canonical review response requires a separately recalculated baseline")
    else
      Consequence.unit

  private def _validate_bundles(bundles: Vector[AdmittedProviderBundleInput]): Consequence[Unit] = {
    val providers = _unique_bundles(bundles).map(_.admitted.provider)
    if providers.distinct.size == providers.size then Consequence.unit
    else Consequence.operationInvalid("canonical review response cannot select between multiple bundles from one provider identity")
  }

  private def _reconcile(bundles: Vector[AdmittedProviderBundleInput]): Consequence[CarReviewReconciliationResult] =
    CarReviewBundleReconciler.reconcile(bundles) match {
      case Right(value) => Consequence.success(value)
      case Left(error) => Consequence.operationInvalid(s"${error.code}: ${error.message}")
    }

  private def _assemble(
    template: CarReviewReport,
    bundles: Vector[AdmittedProviderBundleInput],
    reconciled: CarReviewReconciliationResult,
    assessed: ReviewAssessmentGateResult
  ): Consequence[CarReviewReport] =
    val providers = _unique_bundles(bundles).map { value =>
      ReviewProviderExecution(
        value.admitted.provider,
        value.admitted.ruleSet,
        ReviewProviderState("completed"),
        Some(value.admitted.bundleDigest),
        Some(template.execution.startedAt),
        Some(template.execution.completedAt),
        value.admitted.limitations.map(_provider_limitation)
      )
    }.sortBy(value => (value.provider.id.value, value.provider.version.value))
    CarReviewReportAssembler.assemble(template.copy(execution = template.execution.copy(providers = providers)), reconciled, assessed) match {
      case Right(value) => Consequence.success(value)
      case Left(error) => Consequence.operationInvalid(s"${error.code}: ${error.message}")
    }

  private def _unique_bundles(bundles: Vector[AdmittedProviderBundleInput]): Vector[AdmittedProviderBundleInput] =
    bundles.foldLeft(Vector.empty[AdmittedProviderBundleInput]) { (z, value) =>
      if z.exists(_.admitted.bundleDigest == value.admitted.bundleDigest) then z else z :+ value
    }

  private def _provider_limitation(value: ReviewLimitation): ReviewLimitation =
    value.copy(scope = ReviewLimitationScope("provider"))
}
