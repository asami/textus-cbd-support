# Mono-Koto Semantic Bridge Contract

status=stable
decision_scope=P9-32A
updated_at=2026-09-10

## Scope and status

This specification normatively defines a read-only Mono-Koto semantic bridge
for one exact Component and one bounded Canonical Component Design Model
(CCDM) context. The [Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for CCDM identity, source authority, attribution, bounded
locator, conditions, and cross-view navigation. The
[Mono-Koto Projection Contract](mono-koto-projection-contract.md) retains
authority for Mono/Koto admission, aggregation, and stakeholder projection.
The [Mono-Koto Web Overview Contract](mono-koto-web-overview-contract.md)
remains a separate non-engineering presentation boundary.

This contract defines neither a runtime value nor an executable specification,
Entity/Event/Workflow/StateMachine projection, Web route or renderer,
transport/API/schema, persistence, source retrieval, feedback editor, CML
behavior, Use Case behavior, Phase/checklist update, validation result, or
release claim.

## Exact relation admission and category scope

Each bridge relation MUST be scoped to one exact already established Component
identity and one bounded CCDM context. It MUST contain one already admitted
exact Mono or Koto source subject and one already admitted exact CCDM semantic
target in that same scope. A label, type name, near identity, CML text,
source order, layout, presentation label, another view, broad query, or
view-local copy MUST NOT establish, replace, repair, redirect, or approximate
either endpoint.

Every retained relation, and every forward or reverse representation of it,
MUST preserve:

- the exact Component identity and bounded CCDM context;
- the Mono/Koto source-subject identity and admitted aggregation role;
- the exact target semantic identity and admitted target category;
- source attribution and a bounded source locator; and
- all shared condition state that affects the subject, target, relation,
  attribution, locator, or navigation.

A Mono relation MAY target only an already admitted Aggregate, Entity, Value,
or structural relation. A Koto relation MAY target only an already admitted
Command, Event, Workflow activity, or state effect. These categories disclose
existing admitted semantics only. They MUST NOT create a category, construct
an engineering model, or require `Mono = Entity` or `Koto = Event` as a
one-to-one mapping.

## One retained ledger and symmetric navigation

Forward navigation MUST expose every retained relation from its exact Mono or
Koto source subject to its exact target. Reverse navigation MUST expose every
retained relation from its exact target to its exact Mono or Koto source
subject. Both are stable many-to-many views of one retained relation ledger.
Neither direction may select a primary target, discard another retained link,
or infer a one-to-one correspondence.

A reverse link MUST be the inverse navigation of the exact same retained
forward relation, not a duplicate or independently inferred semantic fact. A
reverse link MUST NOT exist unless its exact corresponding forward relation has
been admitted. A forward link MUST NOT exist unless its exact reverse link is
derivable from that same retained record. Both directions MUST be subject to
the same Component/context scope, endpoint identity/category, target-
implementation eligibility, provenance, locator, and condition gates.

The bridge MUST NOT reconstruct a relation, endpoint, category, locator,
provenance, navigation target, or reverse link from a display label, type name,
CML text, source order, diagram/layout position, stakeholder overview
presentation, engineering projection, or view-local copy.

## Conditions, disclosure, and ineligible navigation

The bridge MUST retain, distinguish, and attribute the following conditions for
every affected subject, target, relation, attribution, locator, and navigation
affordance:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source cannot supply material; this is not explicit absence. |
| unauthorized | The necessary admission or access basis is lacking; this is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; this is not unavailable material. |
| explicitly absent | The source cannot establish the expected fact for its stated scope; this is not a broad negative fact. |
| ambiguous | More than one admitted identity, relation, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed sources assert incompatible material; the affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A stated source, authority, projection, compatibility, or disclosure boundary prevents a stronger claim. |

When an exact target, scope, provenance, bounded locator, target implementation
gate, or condition does not admit navigation, the bridge MUST suppress that
navigation without deleting, rewriting, completing, proxying, or substituting
the retained relation. It MUST NOT reauthorize material, disclose withheld
information, convert unavailable material to explicit absence, claim a missing
target is absent/complete/resolved, resolve ambiguity or conflict, or create a
dead link or loose lookup.

## Deterministic identity-first collections

Forward and reverse relation collections MUST be deterministic and identity-
first. A forward collection MUST order by its exact source and target
identities; a reverse collection MUST order by its exact target and source
identities. Each MAY then use remaining stable admitted identities and, only
when necessary to make the order total, an admitted stable non-semantic tie
key.

Source order, labels, names, category preference, authority, freshness,
ranking, provider preference, and iteration order MUST NOT determine order,
interpretation, or a winner. When the admitted keys cannot establish a total
order, the bridge MUST preserve the unresolved ordering condition and MUST NOT
select an iteration order or hidden winner.

## Read-only prohibitions and later work

The bridge MUST remain read-only. It MUST NOT create or normalize Aggregate,
Entity, Value, Command, Event, Workflow, state-effect, or state-transition
semantics; mutate CCDM, CML, any canonical source, or a Mono-Koto input;
reinterpret Mono/Koto; infer implications or causality; transfer source
authority; construct a broad lookup or proxy target; or edit feedback.

This contract does not authorize an Entity/Event/Workflow/StateMachine runtime
or model, a Web route/renderer, transport/API/schema, persistence, source
retrieval, CML processing, feedback mutation, Use Case behavior, executable
specification, Phase/checklist closure, validation, commit, push, publication,
or deployment. Those behaviors require separately admitted later contracts.
