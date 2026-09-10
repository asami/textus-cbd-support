# Event Model Projection

status=stable
decision_scope=P9-50A
updated_at=2026-09-10

## Purpose and authority

This design fixes the read-only Event Model projection boundary for one exact
Component identity and one bounded Canonical Component Design Model (CCDM)
context. It refines the Event role in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and preserves the identity, source, condition, and navigation boundaries of
the [Mono-Koto Semantic Bridge Model](mono-koto-semantic-bridge-model.md) and
the [Entity Model Projection](entity-model-projection.md). Those predecessor
designs and their contracts retain authority for CCDM admission, attribution,
bounded locators, and shared conditions.

The Event Model is an engineering projection of already admitted behavioral
identities and assertions. It is not a source reader, CML interpreter, event
store, workflow model, state-machine model, lifecycle engine, or new source of
domain truth. A projected identity or assertion remains attributable to the
source that admitted it.

## Exact projection boundary

The projection is scoped to exactly one established Component identity and one
bounded CCDM context. Its ledger retains only records directly admitted to
that exact pair. Every retained record preserves:

- the exact Component identity and bounded CCDM context;
- its exact semantic identity, record role, and admitted endpoints where it is
  an assertion;
- source and authority attribution;
- a bounded source locator; and
- the distinct shared condition state affecting the record, endpoints,
  attribution, locator, or navigation.

The ledger has two identity families and four independently identified
assertion families:

| Retained record | Projection meaning | Boundary |
| --- | --- | --- |
| Command identity | An already admitted exact Command semantic identity. | It is not an Event, and its presence does not establish an Event occurrence. |
| Event identity | An already admitted exact Event semantic identity. | It does not establish an occurrence, causal relation, Workflow flow, StateMachine transition, Entity lifecycle, or lifecycle-engine fact. |
| Causal assertion | An independently admitted exact causal assertion with its own identity, exact direction, and exact endpoints. | A Command, Event, name, order, or proximity cannot create, complete, reverse, or reinterpret it. |
| Consequence assertion | An independently admitted exact consequence assertion with its own identity, exact direction, and exact endpoints. | It does not become a state transition, Workflow activity, or lifecycle fact without a separate admitted assertion. |
| Affected-domain-element assertion | An independently admitted exact assertion identifying its exact affected domain-element endpoint or endpoints. | It does not create an Entity Model record, ownership, membership, lifecycle, or StateMachine fact. |
| Generated-state-effect assertion | An independently admitted exact assertion identifying its exact generated-state-effect endpoint or endpoints. | It does not create a StateMachine transition, Workflow flow, Entity lifecycle, or lifecycle-engine fact. |

The assertion identity, assertion kind, endpoint identities, endpoint roles,
direction, attribution, locator, and condition are retained as one source
assertion. The projection does not synthesize a reverse assertion, infer a
missing endpoint, collapse assertion identities, or turn a display label into
an identity. A relation can be retained even when its target cannot be
navigated.

## Admission and semantic non-implication

Command and Event identities are retained only from direct CCDM admission.
Causal, consequence, affected-domain-element, and generated-state-effect
records are retained only when their own exact assertions are admitted. A
Command does not prove that an Event occurred. An Event identity does not
prove a StateMachine transition, Workflow flow, Entity lifecycle, or causal
relation. An affected-domain-element or generated-state-effect assertion does
not create an Entity Model, StateMachine, or lifecycle-engine fact.

Labels, names, namespaces, CML text, query results, diagram layout, display
proximity, source order, Mono-Koto, Use Case, another projection, a local
copy, or a plausibility judgment can explain an admitted record but cannot
admit, repair, redirect, complete, or reinterpret it. The Event Model does not
choose a hidden source winner or use a projection copy as authority.

The Mono-Koto bridge supplies only existing exact identity navigation in the
same Component/context. A Koto may have zero or more admitted Command, Event,
Workflow, or state-effect links. A Koto is never normalized to one Event; the
bridge cannot establish Event Model input, causal meaning, endpoint, reverse
link, or a primary Event. An existing bridge relation remains evidence-
preserving navigation and does not transfer authority to this projection.

## Unsupported material and attributable gaps

When an admitted source or bounded Cozy material cannot supply requested Event
Model material, the projection retains an attributable bounded gap rather than
completing the record locally. Each gap identifies:

1. the exact Component identity and bounded CCDM context;
2. the affected exact admitted Command, Event, or assertion identity when
   known;
3. the requested projection role and unsupported source/Cozy field or
   bounded assertion;
4. source and authority attribution;
5. the bounded locator or bounded unavailable scope; and
6. the applicable distinct condition and limitation reason.

A gap contains no inferred value, inferred endpoint, proxy relation,
substitute source, hidden winner, or broad absence claim. It is not filled from
CML text, labels, names, namespaces, queries, source order, layout, Mono-Koto,
Use Case, another view, a local copy, provider preference, freshness, or
plausibility. Explicit absence applies only to the source-declared bounded
scope and is never widened into a claim that a Component lacks the material in
general.

## Conditions and deterministic presentation

The Event Model carries the shared CCDM conditions distinctly and
attributably for every subject, assertion, endpoint, attribution, locator,
gap, and navigation target:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply the requested material; it is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated bounded scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, assertion, endpoint, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, Cozy-field, or disclosure boundary prevents a stronger claim. |

The projection does not convert unavailable, unauthorized, redacted, stale,
malformed, ambiguous, conflicting, or limited material into explicit absence.
It does not broaden explicit absence, disclose withheld material, reauthorize a
record, resolve ambiguity or conflict, or choose a source winner.

Every collection uses deterministic identity-first order: exact admitted
subject identity first; then exact retained assertion or metadata identity
where relevant; then other stable admitted identities; and only then an
admitted stable non-semantic tie key needed to make the order total. Labels,
names, CML, queries, source order, layout, category preference, authority,
freshness, ranking, provider preference, and iteration order do not choose an
order, interpretation, or winner. If admitted keys cannot establish a total
order, the unresolved ordering condition remains explicit instead of selecting
an iteration order.

## Target-gated navigation

Retention of a Command, Event, or assertion is distinct from navigation. The
projection may expose a navigation affordance only when all of the following
are retained and usable:

1. the exact target identity and exact Component/context scope;
2. source attribution and a bounded locator for the retained target relation;
3. the distinct shared condition state of source, relation, target,
   attribution, and locator; and
4. an implemented receiving contract for that exact target.

When any gate is unmet, navigation is suppressed without deleting, rewriting,
proxying, substituting, completing, or reconstructing the retained record or
assertion. The projection does not create a dead link, loose lookup, broad
query, proxy target, or name-based target. Forward and reverse indexes are
presentation indexes over the same retained assertion or identity relation;
they do not create an independently inferred reverse assertion.

A direct exact-identity link to Mono-Koto, Use Case, Entity, Structure,
Classification, Workflow, or StateMachine may be retained only when the
relationship is already admitted in the same Component/context and its target
gate passes. Such a link is navigation and explanation only. It does not
transfer authority or establish Event, Entity, Workflow, StateMachine,
lifecycle, causal, or state-transition semantics.

## Read-only and non-implementation boundary

The Event Model is read-only with respect to CCDM, source and Cozy material,
CML, canonical sources, Mono-Koto, Use Case, Entity Model, and every other
projection. It cannot create or normalize Command, Event, causal,
consequence, affected-domain-element, generated-state-effect, Entity,
Workflow, StateMachine, lifecycle, or source facts. Feedback can only be an
attributable semantic proposal under the CCDM contract and cannot mutate this
projection, CCDM, Cozy material, CML, or a canonical source.

P9-50A creates no runtime value, executable specification, API, schema,
persistence, CML/source/Cozy retrieval or mutation, Web route or renderer,
feedback mutation, validation, review, Phase/checklist/journal update,
commit, push, publication, deployment, P9-51 Workflow, P9-52 StateMachine,
P9-53 dynamic cross-view navigation, or later Phase behavior. Those behaviors
require separately admitted contracts.
