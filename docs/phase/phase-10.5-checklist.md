# Phase 10.5 Checklist: Drift and CML Change Gate

Status: OPEN
phase=[Phase 10.5](phase-10.5.md)
predecessor=[Phase 10.4](phase-10.4.md)
successor=[Phase 10.6](phase-10.6.md)
development-item=DEV-CBD-002

contract=[Internal-model CML Change Contract](../spec/internal-model-cml-change-contract.md)
design=[Internal-model CML Change Design](../design/internal-model-cml-change.md)
Step=P105-CML-CHANGE; Slice A=P105-CML-CHANGE-A (authoring pending acceptance)
Slice B=P105-CML-CHANGE-B (focused dependency validation passed; complete Step acceptance pending)
Slice C=P105-CML-CHANGE-C (parent focused/accumulator and actual positive/negative probe validation passed; complete Step acceptance pending)

Slice B authoring evidence is the [application engine](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplication.scala),
[native writer](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriter.scala),
[native executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/NativeCmlFileWriterSpec.scala),
[application executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplicationSpec.scala),
[repository skill source](../skills/cbd-apply-cml-change/SKILL.md), and
[developer guide](../developer-guide-internal-model-cml-change.md). These links
record authoring. A focused validation passed 40 + 70 and B passed 27 + 118
specifications, admitting dependencies only. No Step acceptance or Phase review
is asserted.

Slice C authored evidence is the [post-validator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala),
[post-validation specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidationSpec.scala),
[same-JVM test-only probe](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceProbe.scala)
and [independent test-owner support](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeAcceptanceSupport.scala).
C parent focused validation passed 17 specifications and accumulator validation
passed 215 specifications (232 = 17 + 215). Actual positive Cozy commands on both
applied targets exited 0/0 and produced `ReprojectedPendingAcceptance` with all
eight nonempty families, retained conditions and an unchanged cursor. The separate
actual alpha string-attribute FAIL Cozy commands exited 1/0; the test probe
exited 0 after its expected-refusal checks passed and produced the expected
`Failed` result with no projections, an unchanged cursor and retained applied
changes; exit 1 is refusal proof, not CLI success. The
[developer guide](../developer-guide-internal-model-cml-change.md) records the
reproducible probe procedure. Synthetic unit observations are admission coverage
only. Complete Step acceptance, full Phase review/release and all six obligations
below remain pending.

## P10-50: Semantic drift invalidation

- [ ] Invalidate or require re-review when Scenario, realization/design,
  referenced model, glossary/BoK basis, mapping rules, or CML projection changes.

## P10-51: Baseline drift handling

- [ ] Detect live CML drift and require explicit reconciliation/review rather
  than silently applying a proposal against a different baseline.

## P10-52: Exact approval gate

- [ ] Reject CML mutation unless an applicable human approval binds the exact
  selected realization/design, candidate projection and complete semantic subject
  through exact typed artifact and logical references with explicit revisions.

## P10-53: Skill-applied CML change boundary

- [ ] Define the dedicated skill/repository mutation contract so approved CML
  changes preserve canonical-source ownership and Git-governed acceptance.

## P10-54: Post-change validation and re-projection

- [ ] Run Cozy/CBD validation after the CML change and regenerate the Phase 9
  Canonical Component Design Model and stakeholder/engineering projections.

## Closure

- [ ] Release the accepted Phase 10.5 contract to Phase 10.6.
