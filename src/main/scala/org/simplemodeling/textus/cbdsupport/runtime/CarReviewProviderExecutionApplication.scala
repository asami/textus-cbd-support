package org.simplemodeling.textus.cbdsupport.runtime

import org.goldenport.cncf.action.ActionCall

/*
 * @since   Jul. 16, 2026
 * @version Jul. 16, 2026
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
    actioncore: ActionCall.Core,
    request: ProviderBundleExecutionRequest
  ): ProviderBundleExecutionOutcome =
    coordinator.execute(request, registry, actioncore)
}
