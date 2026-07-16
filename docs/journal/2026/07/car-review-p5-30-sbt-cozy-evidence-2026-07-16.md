# P5-30 sbt-cozy Build Evidence

status=complete
phase=5
checklist=P5-30
updated_at=2026-07-16

## Context

The CBD-led Review design needs CI build facts from `sbt-cozy`, but the plugin
must not become a second Review application. In particular, it must not turn
successful compilation or testing into an Assurance, a quality assessment, or
a publish/deployment gate.

## Decision

`sbt-cozy` provides two stable task surfaces:

- `cozyReviewEvidenceDir`, defaulting to `target/cbd-review/sbt-cozy`; and
- `cozyReviewSbtEvidence`, which writes `provider-descriptor.json`,
  `provider-request.json`, and `evidence-bundle.json`.

The generated documents use `textus.cbd.review-provider.v1` and identify the
provider as `sbt-cozy`. They carry task outcomes for generation, compilation,
test, dependency resolution, CAR build, and one aggregate task result. The
target digest is a deterministic hash of project source content and excludes
generated output (`target`) and IDE/VCS directories.
It refuses more than 10,000 source files or more than 16 MiB before reading
source bytes, matching the emitted provider-request input bound.

`cozyReviewSbtEvidence` uses a conditional sbt task: a CAR project evaluates
`cozyBuildCar`; a non-CAR project records `car-build=not-applicable` and never
evaluates the CAR packaging task. The provider advertises `unknown` only as a
possible attributable Observation type, emits no Observations in its normal
bundle, and attaches the capability limitation
`sbt-evidence-no-quality-assessment`. It does not create a Finding, Assurance,
canonical report, assessment, or gate decision.

## Evidence

- `sbt --batch 'testOnly org.goldenport.cozy.SbtReviewEvidenceSpec'` passed
  3 specifications on 2026-07-16.
- The `cozy/review-evidence` scripted fixture exercises a CAR-marked sbt
  project and verifies all six task records and the evidence-only limitation.
- The same fixture was executed directly with `clean`,
  `cozyReviewSbtEvidence`, and `verifyReviewEvidence` under sbt 1.10.11 and
  1.12.6; both runs wrote the expected bounded output path and passed.

## Consequence

P5-31 can treat the JSON documents as client-owned provider payloads. It must
invoke Cozy locally and submit both admitted provider bundles to the CBD Review
Application; it must not give a server a client workspace path or introduce a
second report/gate model in `sbt-cozy`.
