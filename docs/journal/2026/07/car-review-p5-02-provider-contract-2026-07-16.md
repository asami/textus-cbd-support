# CAR Review P5-02 Provider Contract — 2026-07-16

## Work

The first machine-facing CAR Review integration contract was promoted from the
P5-01 architecture and exploratory provider examples into a normative static
specification.

## Decision

`textus.cbd.review-provider.v1` is the single v1 schema identity for three
strict document types:

- `provider-descriptor` advertises provider/rule-set identity, supported schema
  versions, versioned capabilities, Evidence/Observation kinds, and known
  limitations;
- `provider-request` binds one Review ID and target digest to requested
  capabilities, Evidence kinds, rule selection, optional baseline, and finite
  limits; and
- `evidence-bundle` returns the effective provider/rule-set identity,
  attributable Evidence, candidate Observations, limitations, and request and
  bundle digests.

V1 admission is exact. CBD Support does not infer document type, translate
legacy fields, repair contradictory identity, select an implicit capability,
or silently invoke a fallback provider.

## Evidence

- `docs/spec/car-review-provider-contract.md`
- `docs/spec/schema/car-review-provider-v1.schema.json`
- `docs/spec/examples/car-review-provider-descriptor-v1.json`
- `docs/spec/examples/car-review-provider-request-v1.json`
- `docs/spec/examples/car-review-evidence-bundle-v1.json`
- `CarReviewProviderContractSpec`: 3 scenarios passed

The executable specification recomputes normalized SHA-256 request and bundle
digests and verifies schema, capability, target, provider, rule-set, Review ID,
and evidence-reference agreement.

## Boundary

This slice does not define canonical Review Report/Run persistence, final
observation admission, gate policy, target authorization, retention, or
redaction. Those remain P5-03 and P5-04 work. No Cozy, sbt-cozy, or Textus AI
implementation was changed and no provider was invoked.
