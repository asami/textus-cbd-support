# CAR Review Diagnosis Reuse-Key Contract v1

status=specified
phase=Phase 8
checklist=P8-41
updated_at=2026-07-23

## Purpose

This contract defines the exact pre-execution identity used to decide whether a
new diagnosis can join or reuse compatible Review work. Its definition ID is
`textus.cbd.review-reuse-key.v1`; its digest is SHA-256 over sorted-key,
no-whitespace UTF-8 JSON with each array sorted by its canonical JSON value.

The key is a conclusion-affecting input identity, not a Report digest, Run ID,
attestation, or provider output digest. P8-42 owns the database lookup,
concurrent coalescing, and completed-Run reuse decision.

## Exact Input

The canonical document contains exactly these fields:

| Field | Required contents | Invalidates reuse when changed |
| --- | --- | --- |
| `definitionId` | the fixed v1 definition ID | always |
| `reviewSchemaVersion` | the supported canonical Review document schema, currently `textus.cbd.review-report.v1` | yes; unsupported schema is rejected |
| `target` | kind, organization, name, version, and reviewed CAR/project digest | yes |
| `profile` | selected CBD Review profile | yes |
| `baselineDigest` | prior baseline digest or explicit `null` | yes |
| `ruleSets` | sorted unique CBD/provider rule-set IDs and versions | yes |
| `providerSelections` | sorted unique provider identity/version, selected rule set, and availability-policy digest | yes |
| `evidenceSnapshots` | sorted unique accepted Evidence class, stable snapshot ID, provider identity/version, and digest; runtime entries use `evidenceClass=runtime` | yes |
| `policyBindings` | exactly one sorted binding for `profile`, `gate`, `reconciliation`, and `suppression`, each with ID, version, and configuration digest | yes |

An empty evidence-snapshot array is canonical and means that no optional
snapshot was admitted. It is not equivalent to an omitted field. The
availability-policy digest captures an explicit enabled/disabled/offline/AI or
external-provider selection policy; a change to availability therefore cannot
reuse an earlier Unknown or Assurance under a different provider posture.

## Exclusions

The key must not contain Review/Report/attestation IDs, execution timestamps,
provider execution order, rendered artifacts, credential references, raw
provider requests/responses, filesystem paths, or Evidence facts. Those values
are either volatile, sensitive, or provider output; retaining them would make
equivalent admissible requests fail to coalesce or enlarge storage authority.

## Invalidation and Admission

Every admissible listed input change yields another key digest and requires a
new Run. An unsupported schema instead fails admission. A new Run is also
required when the persisted record is absent, not completed, expired,
incompatible with this definition ID, or not authorized for the current
request. P8-43 defines non-success and expiry outcomes; P8-45 defines
authorization. Neither absence of a runtime snapshot nor a disabled provider
may be silently replaced by a more permissive provider selection.

Malformed identity, unsupported Review schema, digest, duplicate
rule/provider/evidence identity, or a
missing/duplicated required policy scope fails key admission. CBD Support does
not guess a policy value or use an incomplete key.

## Deferred Implementation

This contract defines only canonical key calculation and invalidation. It does
not perform persistence, create a Run, call a provider, join concurrent work,
or load a Report. Those effects begin in P8-42.
