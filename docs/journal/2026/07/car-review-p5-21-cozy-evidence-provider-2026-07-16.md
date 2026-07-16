# P5-21 Cozy evidence provider

date=2026-07-16
status=complete
checklist=P5-21

## Decision

Cozy owns a public, CBD-neutral `CozyCarReviewProvider` output boundary. CBD
supplies an already-admitted Review ID, target identity/digest, and request
digest; Cozy returns only its provider-owned descriptor and evidence bundle.
The implementation imports no CBD Support API, report model, gate policy, or
credential mechanism.

The provider projects CAR project metadata, the resolved CML source, build
metadata, generated CAR archives, and the existing integrated CAR lint output.
Each record retains a stable provider-local ID, source-relative location, and
source-owned facts. Provider and rule-set identity are explicit. Supported CNCF
versions come from the CAR packaging metadata; their absence is an attributable
target limitation. Runtime evidence remains a separate explicit limitation and
cannot be inferred from static analysis.

## Evidence and next work

`cozy.review.CozyCarReviewProviderSpec` proves a representative CAR produces a
`textus.cbd.review-provider.v1` bundle bound to the caller's request/target
digest, including CAR/CML/build/package/ABI/documentation evidence and declared
CNCF version identity. The same scenario verifies the runtime limitation.

The provider request now requires positive evidence, observation, input-byte,
and timeout limits. It totals the direct CAR/CML/documentation input set before
lint runs, withholds static analysis when the admitted byte limit is exceeded,
and emits deterministic limitations for invalid request values, input-byte
rejection, evidence truncation, or observation truncation. The executable
specification covers a one-byte input limit without a permissive fallback. It
also applies the requested Cozy capability, Evidence kinds, and include/exclude
rule selectors; an unrequested capability yields no static analysis and an
attributable limitation rather than implicit provider behavior.

P5-22 remains separate: it must prove exact preservation of every existing
`cozy car lint` result through the provider adapter, rather than relying on the
representative bundle test alone.
