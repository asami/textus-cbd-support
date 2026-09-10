---
status: stable
decision_scope: P9-62A
updated_at: 2026-09-11
---

# Lifecycle and Unified Evidence Integration Design

This design fixes P9-62A's transient, read-only boundary for presenting
declared composition, aggregation, and association semantics beside admitted
runtime, Usage/Discovery, Operation, and Quality/Review evidence. Its normative
companion is the [Lifecycle and Unified Evidence Integration Contract](../spec/lifecycle-unified-evidence-integration-contract.md).

## Purpose and authority

One integration makes independently owned evidence inspectable in the exact
Component and Canonical Component Design Model (CCDM) context in which a
declared relationship is already known. It does not turn runtime observation
into a model fact, turn a declared relationship into a runtime claim, or merge
Usage/Discovery, Operation, or Quality/Review records into a new conclusion.

The [Canonical Component Design Model Contract](../spec/canonical-component-design-model-contract.md)
retains authority for declared semantic identities, composition/aggregation/
association meaning, canonical-source ownership, source attribution, and
conditions. CNCF runtime evidence retains authority for its observed lifecycle
facts. Discovery retains retrieval, ambiguity, and absence. Operation retains
its declared operation projection. Canonical Review retains evaluation,
Finding, Assurance, Unknown, limitation, gate, report, and attestation
conclusions. The [Component Dashboard Projection Contract](../spec/component-dashboard-projection-contract.md)
retains Dashboard concern, ordering, and navigation semantics.

This integration is a presentation-ready evidence record only. It is not a
source reader, lifecycle engine, state transition executor, freshness service,
Discovery query, operation resolver, Review invocation, conclusion calculator,
Dashboard route, CML parser, persistence model, or canonical authority.

## One bounded integration record

Each `LifecycleUnifiedEvidenceIntegration` belongs to one exact Component and
one bounded CCDM context. It has five separate evidence lanes:

| Lane | Caller-admitted retained values | Authority retained by the lane |
| --- | --- | --- |
| Declared relationship semantics | A declared composition, aggregation, or association identity; its exact subject, object, relationship family, source owner, locator, attribution, conditions, and limitations | Canonical CCDM/source owns semantic meaning and canonical status. |
| Runtime lifecycle evidence | An externally observed runtime-evidence identity explicitly linked to one declared relationship identity, together with observed lifecycle state/value, source/authority, locator, conditions, and limitations | Runtime source owns the observation; absence of an observation is not a semantic negative. |
| Usage/Discovery evidence | Exact externally supplied Usage/Discovery projection identities, each with its declared concern, source/authority, locator, representation, conditions, and limitations | Discovery owns retrieval, ambiguity, and absence. |
| Operation evidence | Exact externally supplied Operation projection identities, each with its declared concern, source/authority, locator, representation, conditions, and limitations | The operation owner owns operation semantics. |
| Quality/Review evidence | Exact externally supplied canonical Review evidence identities and authorized representations, with Review source/authority, locator, conditions, and limitations | Review owns every conclusion and its disclosed representation. |

The record requires at least one declared relationship semantic. Each evidence
lane has an explicit position: a lane may retain one or more supplied evidence
records, or no record only when it carries a caller-admitted bounded condition
such as unavailable, unauthorized, redacted, explicitly absent, ambiguous,
conflicting, stale, malformed, or limited. An empty lane never becomes an
inferred unavailable, absence, failure, lifecycle state, operation result, or
Review conclusion.

Every value is scoped to the exact Component/context. A Runtime lifecycle
record must name the exact declared relationship identity it observes. A
Usage/Discovery, Operation, or Quality/Review record must be explicitly
associated with an admitted declared semantic or with the integration's exact
Component/context according to its own source contract. Labels, names, endpoint
equality, source locators, timestamps, display order, relation family, runtime
state, concern, or graph shape cannot recover a missing association.

## Declared semantics and lifecycle evidence remain distinct

`composition`, `aggregation`, and `association` are supplied declared semantic
families. The integration neither derives a family from multiplicity, ownership
wording, runtime behavior, operation names, CML text, model layout, or a
candidate/diff record, nor converts one family into another.

Runtime lifecycle evidence may state only the supplied observation about a
declared relation. It can be available, unavailable, unauthorized, redacted,
ambiguous, conflicting, stale, malformed, explicitly absent within its source
scope, or limited. It does not prove that the declared relation is executable,
current, healthy, complete, accepted, or canonical. Conversely, a declared
relationship does not manufacture a runtime observation, state, transition,
availability, or operational conclusion.

The integration does not compare observation times, select a current value,
resolve conflicts, infer a transition, derive causality, or operate a lifecycle
state machine. Conditions retain their individual source attribution and remain
visible beside the relevant value.

## Unified presentation without a new authority

The integration presents its five lanes together so a reader can inspect a
declared design relationship and the evidence that explicitly traces to it or
to the same exact Component/context. It does not combine their truth claims.

Usage/Discovery and Operation are separate records even when they mention the
same semantic identity. Quality/Review records retain the authorized existing
Review representation and may not be summarized as a stronger or weaker
conclusion. A runtime fact cannot alter a Review conclusion, and a Review
conclusion cannot authorize, suppress, or reinterpret runtime or declared
semantic evidence.

The stable presentation order is identity-first:

1. exact Component and CCDM context;
2. declared relationship family in the fixed non-semantic sequence
   composition, aggregation, association, then declared relation identity and
   admitted stable tie key;
3. runtime evidence by linked declared relation identity, evidence identity,
   then admitted stable tie key; and
4. Usage/Discovery, Operation, and Quality/Review lanes in that fixed
   presentation sequence, then their source projection identity and admitted
   stable tie key.

This order does not rank evidence, select a winner, resolve a conflict,
represent freshness, or establish a relationship that was not explicitly
admitted.

## Conditions, exclusions, and successor boundaries

All relevant availability, authorization, redaction, explicit-absence,
ambiguity, conflict, staleness, malformed-evidence, and limitation conditions
remain separate. Explicit absence is bounded to its supplied source and scope;
unavailable and unauthorized evidence are not absence; redaction is not
unavailable; and conflict or ambiguity has no local winner.

P9-62 may later implement only pure immutable values and executable
specifications that preserve this contract. It may not retrieve or mutate a
source, execute a runtime, invoke Review, run Discovery, resolve an operation,
construct Dashboard navigation, make a selection, update canonical source,
persist a continuation, or create Web/API/schema behavior. P9-63 separately
owns candidate-to-Dashboard cross-surface navigation. Phase 10 separately owns
durable lifecycle, approval/history, continuation, and rehydration behavior.
