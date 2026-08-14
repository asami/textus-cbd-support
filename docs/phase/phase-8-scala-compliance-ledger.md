# Phase 8 Scala Compliance Ledger

Status: independent re-review complete and P8-60 human confirmation recorded;
final Phase technical and release validation pending.

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
| `src/test/scala/org/simplemodeling/textus/cbdsupport/Phase8ExecutableCoverageSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/TextusAiCarReviewProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/TextusAiSurfaceCarReviewProviderRunnerSpec.scala` | pass | pass | focused suite |
| `src/test/scala/org/simplemodeling/textus/cbdsupport/impl/ReviewDiagnosisPersistenceSpec.scala` | pass | pass | focused suite |

## Acceptance evidence

- Whole-file naming and assertion scans: `rg` scans over all 31 ledger paths
  found no nonconforming private member name, camel-case ordinary local, or
  bare `assert` (the `runFailure` values are required public trait members).
- Focused validation: the eight directly affected specifications passed 25
  tests; the three renamed provider/specification surfaces passed 7 tests.
- `sbt --batch Test/compile`: passed after the complete review-fix set.
- Final independent re-review: clean. P8-60 human confirmation is recorded;
  this ledger does not substitute for pending final Phase release validation.
