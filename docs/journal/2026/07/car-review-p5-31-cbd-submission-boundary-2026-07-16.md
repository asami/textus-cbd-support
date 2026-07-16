# P5-31 CBD Submission Boundary

status=in-progress
phase=5
checklist=P5-31
updated_at=2026-07-16

## Context

The local and CI route must submit Cozy and `sbt-cozy` evidence to CBD without
giving a CBD server a client workspace path, a process command, or authority
to acquire source files. CBD remains the only owner of the canonical Review
Report and Gate.

## Implementation Event

`CarReviewPairedBundleReviewApplication` now receives a canonical report
template and one or more `SuppliedProviderBundleSubmission` values. Each value
contains only Review/Target identity, availability, descriptor, request, and
bundle document text. The boundary requires every supplied Review and Target
to match the report template, admits documents through
`CarReviewSuppliedBundleApplication`, and delegates reconciliation, assessment,
and report construction to `CarReviewCanonicalResponseApplication`.

The canonical response rebuilds completed provider executions from admitted
provider identity, rule set, digest, and limitations. It rejects an ambiguous
capability policy, an unrecalculated baseline, or two distinct bundles from
the same provider identity rather than selecting or discarding data silently.
Provider limitation scopes that are valid in provider-v1 but not report-v1 are
retained as provider-attributed report limitations.

## Evidence

- `CarReviewCanonicalResponseApplicationSpec` proves canonical report/gate
  identity, provider execution attribution, ambiguous-capability refusal, and
  stale-baseline refusal.
- `CarReviewPairedBundleReviewApplicationSpec` proves path-free client
  admission, canonical response construction, and Review identity refusal.
- `CarReviewBundleReconcilerSpec` proves provider limitation scope projection.
- `sbt --batch test` passed 194 CBD Support specifications on 2026-07-16.

## Remaining Work

P5-31 remains open. `sbt-cozy` must still implement its concrete transport to
the CBD public submission contract and demonstrate one Cozy plus one
`sbt-cozy` bundle in an end-to-end scenario. Provider-refusal-to-Unknown
workflow coverage remains part of P5-63.
