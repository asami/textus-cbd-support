# CAR Review Provider and sbt-cozy Integration — 2026-07-16

## Context

The CAR Review product boundary was assigned to CBD Support, with Cozy acting
as the CAR/CML deterministic analyzer. The follow-up decision generalizes that
relationship: CBD Support leads Review development and Cozy, `sbt-cozy`, CNCF,
SIE, catalog, runtime, and AI integrations participate through a common Review
Provider contract.

CI/CD integration through `sbt-cozy` is part of the design rather than a later
renderer concern.

## Provider Decision

The Cozy-specific `CarAnalysisRequest` and `CozyEvidenceBundle` proposal is
superseded by the generic `ReviewProviderRequest` and
`ReviewEvidenceBundle` contracts.

Every bundle carries:

- schema version;
- provider identity, version, capabilities, and rule-set identity;
- reviewed target and target digest;
- attributable Evidence;
- provider-owned deterministic or heuristic Observations; and
- explicit provider limitations.

CBD Support owns provider registration, selection, compatibility admission,
cross-provider reconciliation, quality assessment, canonical report creation,
and gate policy. Providers do not produce competing complete reports.

## sbt-cozy Decision

`sbt-cozy` has two roles:

1. It is a Review Provider for sbt generation, compilation, test,
   dependency-resolution, CAR-build, and task-result evidence.
2. It is the local/CI client that invokes Cozy, sends Cozy and sbt evidence
   bundles to CBD Support, receives the canonical report and gate result, and
   exposes them as sbt tasks and CI artifacts.

`sbt-cozy` does not own Review rules outside its provider evidence and does not
recalculate CBD Support's gate result.

## Call and Dependency Directions

The local and CI route is:

```text
sbt-cozy -> Cozy analyzer -> Cozy ReviewEvidenceBundle
sbt-cozy -> sbt tasks      -> sbt ReviewEvidenceBundle
sbt-cozy -> CBD Review Application -> report and gate result
```

For an admitted uploaded CAR, CBD Support may invoke Cozy directly through the
Review Provider protocol.

The negative dependency statements mean:

- Cozy does not call or depend on CBD Support;
- CBD Support does not import or depend on `sbt-cozy` implementation classes;
- CBD Support may call Cozy only through the provider protocol or an adapter
  implementing that protocol; and
- when `sbt-cozy` already supplied a valid Cozy bundle, CBD Support does not
  invoke Cozy a second time for the same Review Run.

The earlier `-/→` notation was discarded because it could be mistaken for a
route. The documentation now states positive calls and prohibited dependencies
in words.

## CI/CD Decision

Provisional tasks are:

```text
cozyReviewCar
cozyReviewCarCheck
cozyReviewCarReport
```

They respectively obtain the canonical report, apply the CBD gate result, and
materialize requested projections. The proposed artifacts are canonical JSON,
HTML, SARIF, and a Review attestation under `target/cbd-review/latest`.

The attestation binds the target digest to the report digest, profile,
provider/rule-set versions, and gate result. A future release configuration may
require a passing attestation before publish, distribution, or deployment, but
the initial integration does not silently redefine existing sbt publication
tasks.

Standard CI is deterministic and offline. External network and AI providers
are opt-in, and credentials or sensitive evidence must not enter reports,
attestations, SARIF, HTML, logs, or task output.

## Remaining Decisions

- final Review Provider schema ownership and publication artifact;
- final sbt task and setting names;
- required sbt evidence for development, CI, and release profiles;
- local embedded versus server-backed CBD Review transport;
- Cozy version discovery for source projects and built CARs;
- CI cache keys and invalidation rules;
- attestation signing and trust policy; and
- opt-in publish, distribution, and deployment gate configuration.

## Documentation Result

`docs/notes/car-review-design-proposal.md` now describes the generic provider
model, the positive and prohibited dependency directions, `sbt-cozy`'s dual
role, CI artifacts, attestation, and the expanded incremental delivery order.
The design remains exploratory and non-normative.
