# CAR Review Report, Run, and Attestation Contract v1

status=specified
checklist=P5-03
updated_at=2026-07-16

## Purpose

This specification defines the CBD-owned canonical state and result contracts
for CAR Review. Provider bundles from P5-02 are admitted inputs. They never
replace the Review Run, canonical Review Report, or Review attestation defined
here.

Normative machine-readable artifacts are:

- `docs/spec/schema/car-review-report-v1.schema.json`
- `docs/spec/examples/car-review-run-v1.json`
- `docs/spec/examples/car-review-report-v1.json`
- `docs/spec/examples/car-review-attestation-v1.json`

## Document Identity

Every document uses `schemaVersion=textus.cbd.review-report.v1` and one exact
`documentType`:

| Document | `documentType` | Meaning |
| --- | --- | --- |
| Review Run | `review-run` | Mutable lifecycle projection for one authorized execution. |
| Review Report | `review-report` | Immutable canonical result for one completed evidence set. |
| Review attestation | `review-attestation` | Immutable binding of target, report, profile, providers, rule sets, and gate result. |

Unknown schema identities and document types are incompatible. V1 has no
legacy field-name translation.

## Review Run

A Review Run records:

- Review ID, admitted target, and selected profile;
- lifecycle state and start/update/completion times;
- selected providers and their attributable execution state;
- run limitations and failure code when applicable; and
- completed report ID and digest when a canonical report exists.

Run states are `admitted`, `queued`, `running`, `cancelling`, `cancelled`,
`completed`, and `failed`. A completed Run references exactly one immutable
report identity. A failed or cancelled Run does not fabricate a report.
Detailed command authorization, cancellation propagation, persistence, and
state-transition implementation are P5-11 and P5-14 work.

## Canonical Review Report

The report contains one normalized body of:

- target and execution identity;
- admitted provider executions and bundle digests;
- normalized Evidence;
- canonical Finding, Assurance, and Unknown Observations;
- capability assessments;
- limitations;
- optional baseline comparison; and
- one profile-specific gate result.

Text, HTML, SARIF, Web, CLI, and MCP views project this report. A renderer does
not rerun a rule, promote maturity, suppress an Observation, or derive a gate
result absent from the canonical report.

## Evidence and Provider Attribution

Every report Evidence record has a canonical report-local ID and retains its
source provider ID, provider bundle digest, and provider-local Evidence ID.
Evidence also records kind, subject, optional safe location and digest, and
bounded facts. This allows several providers to report related facts without
merging their identities.

Provider execution records retain provider and rule-set versions, bundle
digest, completion state, and limitations. An unavailable, incompatible,
disabled, or failed provider remains attributable; it is not omitted to make
coverage appear complete. A completed provider execution requires its bundle
digest and start/completion instants.

## Observation Contract

Every Observation has an ID, type, rule identity/version, subject, message,
rationale, confidence, Evidence references, locations, provider attribution,
disposition, and cross-view mappings.

- `finding` records a problem and requires severity.
- `assurance` records a positive conclusion supported by admitted Evidence.
- `unknown` records missing, incompatible, disabled, or insufficient Evidence.

Severity is `info`, `low`, `medium`, `high`, or `critical`. Confidence is
`low`, `medium`, or `high`. Severity applies only to Findings; it is never used
to disguise uncertainty. An Assurance requires at least one admitted Evidence
reference; absence of a Finding alone cannot satisfy that requirement.

Disposition is `active`, `accepted`, `suppressed`, or `deferred`. A non-active
disposition requires a bounded reason and author, and may have an expiry.
Disposition preserves the Observation in the report and cannot erase its
Evidence or provider attribution.

## Capability Assessment

An assessment records capability identity, applicability, maturity, coverage,
confidence, providers, Observation/Evidence references, strengths, and gaps.

Applicability is `applicable`, `not-applicable`, or `unknown`. Maturity is
`unassessed`, `missing`, `ad-hoc`, `partial`, `established`, `verified`, or
`operational`.

Coverage uses integers to avoid non-portable floating-point normalization:

- `applicableSubjects` is the assessment denominator;
- `assessedSubjects` has admitted assessment Evidence;
- `unknownSubjects` remains unassessed; and
- `basisPoints` equals `assessedSubjects * 10000 / applicableSubjects` using
  integer division.

`assessedSubjects + unknownSubjects` equals `applicableSubjects`. Coverage is
`null` when applicability is not `applicable`. Static analysis alone cannot
justify `operational`; the later runtime assessment contract enforces the
required runtime Evidence.

## Baseline and Gate

An optional baseline binds a prior report ID and digest and records added,
removed, and unchanged Observation IDs. A baseline comparison does not mutate
either report and cannot compare reports whose target identity is incompatible.

The gate records policy ID/version, result `pass`, `fail`, or `unknown`,
reasons, and blocking Observation IDs. Blocking IDs resolve to Findings in the
same report. Provider results do not define the gate directly; CBD Support
applies the selected profile policy after reconciliation.

## Attestation

The attestation binds:

- attestation, Review, and report identities;
- target and report digests;
- selected profile;
- every admitted provider, rule-set, and bundle digest;
- the exact canonical gate result; and
- creation time.

`reportDigest` is SHA-256 over normalized report JSON with root
`reportDigest` omitted. `attestationDigest` is SHA-256 over normalized
attestation JSON with root `attestationDigest` omitted. Normalization is the
same sorted-key, no-whitespace UTF-8 form defined by the P5-02 provider
contract. Signing and trust policy remain later CI/release integration work.

## Admission Invariants

CBD Support accepts these documents only when:

1. schema and document identities are exact;
2. Review, target, report, and digest references agree;
3. provider, Evidence, Observation, and assessment IDs are unique where
   required;
4. every Observation Evidence reference resolves inside the report;
5. every assessment Observation/Evidence reference resolves inside the report;
6. every gate blocking ID resolves to a Finding;
7. coverage counts and basis points satisfy the defined equation;
8. report and attestation digests recompute exactly; and
9. a completed Run and its attestation reference the same report, target,
   profile, providers, and gate result.

Failure of an invariant is an incompatible document or failed run outcome. CBD
Support does not repair stale IDs, recalculate a provider result, or choose an
implicit report winner.

## Deferred Contracts

P5-03 does not define target-path authorization, credential resolution,
redaction, retention limits, MCP command publication, or offline/AI execution
policy. Those are P5-04 responsibilities. Runtime implementation and
persistence begin under P5-10 through P5-14.
