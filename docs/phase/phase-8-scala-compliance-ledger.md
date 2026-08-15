# Phase 8 Scala Compliance Ledger

Status: P8-RQH-A review-boundary rollback is pending the parent-selected focused
compatibility, security, projection, and persistence validation. P8-60 human
confirmation remains recorded; this ledger makes no current-tree test,
compile, diff, or review-success claim.

This ledger is the Phase 8 accumulator record required by the CNCF phase
workflow. It covers the repository-managed Scala source and executable-
specification evidence reviewed for the Phase 8 technical accumulator.
Generated files beneath `target/` are excluded because their CML producer is
the reviewed source of truth.

## Compliance rule

Each listed file has been inspected as a whole, not only at changed lines.
Source files must satisfy the shared naming policy. Specifications must also
use narrative `AnyWordSpec`, `Given` / `When` / `Then` at their execution
boundaries, and matcher-based expectations. The recorded focused test and
`Test/compile` validations are required before this ledger can advance to a
final release commit.

## Source files

| File | Naming | Spec style | Relevant validation |
| --- | --- | --- | --- |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/impl/ComponentFactory.scala` | pass | not a spec | `ReviewDiagnosisPersistenceSpec`, `ComponentFactorySpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewBundleReconciler.scala` | pass | not a spec | `CarReviewBundleReconcilerSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewCapabilityCatalog.scala` | pass | not a spec | `CarReviewCapabilityCatalogSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewCostScenarioProviderRunner.scala` | pass | not a spec | `CarReviewCostScenarioProviderRunnerSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewCostViewProjection.scala` | pass | not a spec | `CarReviewCostScenarioProviderRunnerSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewEvolutionProjection.scala` | pass | not a spec | `CarReviewEvolutionProjectionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewInitialStaticQualityProviderRunner.scala` | pass | not a spec | `CarReviewInitialStaticQualityProviderRunnerSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewMcpReadProjection.scala` | pass | not a spec | `CarReviewMcpReadProjectionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewProviderBundleAdmission.scala` | pass | not a spec | `CarReviewProviderBundleAdmissionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewProviderExecutionCoordinator.scala` | pass | not a spec | `TextusAiCarReviewProviderRunnerSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewQualityCoverageProjection.scala` | pass | not a spec | `CarReviewQualityCoverageProjectionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewQualityProviderAdmission.scala` | pass | not a spec | `CarReviewQualityProviderAdmissionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewQualityRuleMatrix.scala` | pass | not a spec | `CarReviewQualityRuleMatrixSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewRunApplication.scala` | pass | not a spec | `ReviewDiagnosisPersistenceSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewViewProjection.scala` | pass | not a spec | `CarReviewViewProjectionSpec` |
| `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/TextusAiSurfaceCarReviewProviderRunner.scala` | pass | not a spec | `TextusAiSurfaceCarReviewProviderRunnerSpec` |

## Executable specifications

| File | Naming | Executable-specification style | Relevant validation |
| --- | --- | --- | --- |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewBundleReconcilerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewCapabilityCatalogSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewCostScenarioProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewEvolutionProjectionSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewInitialStaticQualityProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewProviderBundleAdmissionSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewQualityCoverageProjectionSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewQualityProviderAdmissionSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewQualityRuleMatrixSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewViewProjectionSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/ComponentFactorySpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/TextusAiCarReviewProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/TextusAiSurfaceCarReviewProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/impl/ReviewDiagnosisPersistenceSpec.scala` | pass | pass | focused suite |

## Current-tree validation boundary

P8-RQH-A removed the unaccepted local Review Job/action layer and restored the
established direct-submission, gateway, provider, and Entity-persistence
contract while retaining accepted exact Entity-backed reads, redaction, and
total-quality projections. The deleted
Phase8ExecutableCoverageSpec.scala is not ledger evidence.

The parent selected a serialized focused compatibility/security/projection/
persistence accumulator and git diff --check; neither has been run or recorded
by this rollback. The consumed Phase 8 full review and later invalid
second-review spike are historical context only. Phase full-suite validation is
reserved for the Phase release commit.
