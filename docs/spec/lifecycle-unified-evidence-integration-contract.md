---
status: stable
decision_scope: P9-62A
updated_at: 2026-09-11
---

# Lifecycle and Unified Evidence Integration Contract

This normative P9-62A contract defines one transient, read-only integration of
declared composition, aggregation, and association semantics with explicitly
admitted runtime, Usage/Discovery, Operation, and Quality/Review evidence. Its
rationale is in the [Lifecycle and Unified Evidence Integration Design](../design/lifecycle-unified-evidence-integration.md).

## 1. Scope and authority

Each integration SHALL be scoped to one exact established Component identity
and one bounded Canonical Component Design Model (CCDM) context. CCDM/source
retains authority for declared relationship semantics and canonical facts.
Runtime sources retain authority for lifecycle observations. Discovery retains
retrieval, ambiguity, and absence. Operation sources retain operation semantics.
Canonical Review retains evaluation and every Review conclusion. This contract
SHALL NOT transfer, merge, or replace those authorities.

The integration SHALL retain only caller-admitted, attributable evidence. It
SHALL NOT establish a canonical fact, semantic relationship, runtime fact,
lifecycle state, operation result, Review conclusion, selection, acceptance,
source authority, or mutation authority.

## 2. Required integration shape

An integration SHALL retain one or more declared relationship semantics in its
exact Component/context. Each declared semantic SHALL carry an exact identity,
one explicitly supplied family of `composition`, `aggregation`, or
`association`, exact subject and object identities, source owner, bounded
locator, attribution, every applicable condition, and limitations.

The integration SHALL retain separate bounded positions for:

1. runtime lifecycle evidence explicitly associated with an exact declared
   relationship identity;
2. Usage/Discovery evidence explicitly associated with an exact declared
   relationship or the integration's exact Component/context according to its
   source contract;
3. Operation evidence under the same explicit-association rule; and
4. canonical Quality/Review evidence under the same explicit-association rule.

Each retained evidence record SHALL have an exact identity, source authority,
bounded locator, permitted representation, conditions, and limitations. A lane
with no record SHALL carry a caller-admitted bounded condition. It SHALL NOT be
silently omitted or converted into inferred absence, unavailability, failure,
or a negative conclusion.

Every supplied value that fails Component/context scope, required identity,
attribution, condition, or explicit-association validation SHALL reject the
complete integration without returning a partial result.

## 3. Explicit association and no inference

A runtime lifecycle record SHALL name the exact declared relationship identity
it observes. A Usage/Discovery, Operation, or Quality/Review record SHALL name
either an exact declared relationship identity or the exact Component/context
association admitted by its source contract. Missing associations SHALL NOT be
recovered from labels, names, endpoints, source locators, timestamps, family,
runtime state, Dashboard concern, source order, presentation order, CML text,
candidate/diff shape, or graph layout.

The integration SHALL NOT infer the composition, aggregation, or association
family from multiplicity, ownership text, runtime behavior, operation names,
CML/model text, or Review content. It SHALL NOT transform one supplied family
into another.

## 4. Lifecycle and unified-evidence semantics

Runtime lifecycle evidence is an externally supplied observation only. It MAY
retain a supplied state/value and every supplied condition, including
unavailable, unauthorized, redacted, explicitly absent, ambiguous,
conflicting, stale, malformed, and limited states. It SHALL NOT establish that
a declared relationship is executable, current, healthy, complete, accepted,
or canonical.

This contract SHALL NOT compare evidence times, choose a current observation,
resolve conflict, infer a transition or causal relation, execute a lifecycle,
or create a lifecycle state machine.

Usage/Discovery, Operation, and Quality/Review records SHALL remain distinct
source records even when they have one Component or semantic identity in
common. A Quality/Review record MAY retain only its authorized existing Review
representation. The integration SHALL NOT aggregate, strengthen, weaken,
synthesize, recalculate, or invoke a canonical Review conclusion. Runtime or
operation evidence SHALL NOT alter a Review conclusion, and Review evidence
SHALL NOT authorize a lifecycle action or semantic reinterpretation.

## 5. Conditions and deterministic presentation

The integration SHALL preserve availability, authorization, redaction,
explicit absence, ambiguity, conflict, staleness, malformed evidence, and
limitations as distinct attributed conditions. Explicit absence is bounded to
its stated owner and scope. Unavailable or unauthorized evidence is not
absence; redaction is not unavailability; ambiguous or conflicting evidence
has no local winner; and limitations SHALL NOT be removed by inference.

Presentation SHALL be identity-first: exact Component/context; declared family
in the fixed non-semantic composition/aggregation/association sequence;
declared relationship identity; linked runtime evidence identity; then the
fixed Usage/Discovery, Operation, Quality/Review lane sequence and each source
projection identity. An admitted stable non-semantic tie key MAY be used only
when required for a total order. Labels, names, locators, timestamps, runtime
state, Review result, caller iteration, or layout SHALL NOT determine order,
relation, authority, interpretation, or a winner.

## 6. Prohibitions and reserved work

This contract SHALL NOT authorize source retrieval or mutation, CML parsing,
model interpretation, runtime execution, lifecycle transition execution,
Discovery query, operation resolution, Review invocation, conclusion
calculation, Dashboard navigation, Web/API/schema/transport behavior,
persistence, selection, canonical-source change, commit, push, publication,
deployment, or Phase/checklist/README/journal state change.

P9-62B may separately implement pure immutable values and executable
specifications preserving this contract. P9-63 owns cross-surface
candidate-to-Dashboard navigation. Phase 10 owns durable lifecycle state,
approval/history, continuation, and rehydration. None of that successor work
is implied or admitted here.
