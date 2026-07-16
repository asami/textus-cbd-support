# CAR Review P5-10 Canonical Model and Codec — 2026-07-16

## Work

The P5-03 canonical Review Report contract and P5-04 deterministic security
policy were implemented as the first Review Application runtime boundary.

## Decision

The application does not retain report concepts as undifferentiated Scala
strings. Review ID, report ID, Evidence ID, Observation ID, capability ID,
provider ID, rule ID, digest, version, profile, instant, schema/document type,
target kind, provider state, limitation scope, Observation type, severity,
confidence, disposition, applicability, maturity, and gate result are distinct
value types. Their JSON representation remains the scalar v1 contract.

`CarReviewReportCodec` is the single canonical codec. Decode performs strict
unknown-field inspection before typed decoding, then checks schema/document
identity, finite vocabularies, identifiers, digests, timestamps, uniqueness,
local references, provider/bundle attribution, Finding-only severity,
Evidence-backed Assurance, disposition accountability, coverage arithmetic,
baseline sets, gate blockers, location/text bounds, and provider/execution time
ordering. Failures use stable code/path/message values and do not include the
input payload.

Encode admits only a self-verifying model and recursively canonicalizes arrays
and JSON object keys. `calculateDigest` removes the P5-03 volatile fields and
hashes deterministic content; `withCalculatedDigest` supports controlled
report construction after all other invariants pass.

## Evidence

- `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewModel.scala`
- `src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CarReviewReportCodec.scala`
- `CarReviewReportCodecSpec`: 4 scenarios

The scenarios prove typed canonical decode, stable re-admissible encoding,
identical digest under different Run/report IDs, timestamps, baseline ID, and
array order, rejection of unknown fields/stale digest/unresolved references/
invalid coverage/duplicate providers/unsafe locations, and validated digest
recalculation.

## Boundary

P5-10 does not create or persist Review Runs, invoke a provider, or publish a
user operation. Authorized CNCF Job-backed command/query lifecycle is P5-11;
provider-bundle admission and reconciliation are P5-12/P5-13; persistence,
retention, comparison, and stale-gate protection are P5-14.
