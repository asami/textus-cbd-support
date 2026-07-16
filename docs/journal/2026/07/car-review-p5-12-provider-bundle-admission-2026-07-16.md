# P5-12 Provider Bundle Admission — 2026-07-16

status=complete
checklist=P5-12

## Decision

CBD Support admits a provider evidence bundle at one CBD-owned boundary before
any provider Observation reaches canonical Review reconciliation. The boundary
accepts the exact v1 descriptor/request/bundle exchange only after it verifies
the document identity and strict root shape, descriptor capability coverage,
Review and target identity, request and bundle digests, local Evidence
references, and declared item/byte limits.

The admitted result retains the provider and rule-set identities plus the two
digests and provider-local IDs. It is deliberately not a canonical Finding or
Assurance. P5-13 owns reconciliation into the canonical report.

## Failure Semantics

An incompatible bundle becomes a provider-attributed `incompatible` Unknown-
shaped refusal. An unavailable or disabled provider produces an attributable
`unavailable` or `disabled` refusal. A failed provider similarly retains its
identity but is marked as a run failure. No outcome invokes a replacement
provider, retries implicitly, or manufactures an Assurance.

## Scope Boundary

`CarReviewProviderBundleAdmission` implements the deterministic input boundary
and `CarReviewProviderBundleAdmissionSpec` proves the compatible, target-
digest mismatch, unavailable, disabled, and failed cases. Provider invocation,
timeout measurement/cancellation at the execution boundary, duplicate bundle
memory across runs, and report reconciliation remained open when the initial
admission boundary was recorded.

## Execution Follow-up

The follow-up execution coordinator uses the normalized provider request digest
as its cache key. Once a bundle has been admitted for the same provider and
request, the coordinator returns the admitted result without invoking the
provider again. The coordinator checks the provider-declared timeout against
injected execution timestamps, sends cancellation to the runner on timeout or
pre-cancelled work, and keeps cancellation, timeout, and runner failures as
provider-attributed outcomes. This is a provider-neutral CBD boundary; actual
Cozy discovery/adapter work is still P5-21 and later. The P5-12 checklist item
is complete; P5-13 owns canonical report reconciliation.
