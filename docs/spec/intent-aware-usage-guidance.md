# Intent-Aware Usage Guidance Contract

## Exact Selection

`CbdRetrieval.getUsage` resolves one catalog-backed component before it reads
usage evidence. Its response identifies the selected source ID, source kind,
and component version independently of the operation and reference lists. An
explicit `catalogId` or `version` remains an exact lookup constraint. Without
an explicit value, the existing catalog and version-selection contracts choose
the returned profile; the response reports that actual choice rather than
presenting another source or version as equivalent.

Guidance is attributable only when the selected profile retains runtime source
context. Missing source identity or kind produces a warning and no guidance;
the runtime does not infer `published-catalog` merely because a provider
returned a profile. A missing version remains absent and diagnostic rather than
being replaced with `latestStable` or another observed identity.

## Intent and Statement Kinds

The optional `intent` is bounded to 512 characters and 32 distinct matching
tokens. The response echoes an accepted intent. Every guidance record cites its
source ID, source kind, selected version when available, and evidence URIs.
`statementKind` has three defined meanings:

- `observed-fact` reports source/version selection read from the catalog-backed
  profile and its source observation;
- `deterministic-inference` recommends only an observed operation whose
  service, operation, kind, or description shares an explicit intent token;
- `model-inference` is reserved for advice produced by a generative model and
  must never be used for deterministic matching or an observed catalog fact.

The current runtime does not call a generative model and therefore never emits
`model-inference`. Deterministic guidance reports its token-overlap score and
rationale. Service identity and operation name remain separate fields rather
than placing a service-qualified label in `ComponentOperationName`. Matching
admits Unicode letters and numbers so that non-ASCII published terms retain
their script. Its evidence cites model metadata when present plus the catalog
profile evidence. At most 32 total guidance records are returned.

## Absence and Ownership

An intent with no operation-metadata overlap produces no inferred operation
guidance. The runtime does not choose an unrelated operation, complete missing
model metadata from BoK evidence, or convert semantic evidence into a usage
fact. `operations` and `references` remain the provider's catalog-owned
evidence; guidance is a separate interpretation layer.

Source-aware search precedence does not flow into `getUsage` as a hidden
winner. Working-directory, local-published, and cached observations remain
search alternatives until a catalog-backed exact usage source is selected.
BoK-site and SIE evidence remain terminology evidence and do not supply
component operations, versions, or usage statements.

## Executable Evidence

`IntentAwareUsageGuidanceSpec` verifies source/version attribution, observed
fact versus deterministic-inference labeling, model-inference absence,
operation evidence citations, unrelated-intent absence, and missing-source
withholding, Unicode matching, and the character, token, and response bounds.
`ComponentFactorySpec` verifies that `intent` is exposed in the read-only MCP
input schema and preserved by the generated request contract.
