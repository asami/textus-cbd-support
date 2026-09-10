# Mono-Koto Semantic Bridge Model

status=stable
decision_scope=P9-32A
updated_at=2026-09-10

## Purpose and authority

This design fixes the read-only Mono-Koto semantic-bridge boundary for one
exact Component and one bounded Canonical Component Design Model (CCDM)
context. It refines the navigation roles in the
[Mono-Koto Projection Model](mono-koto-projection-model.md) and the shared
identity, provenance, and visibility rules in the
[Canonical Component Design Model Projections](canonical-component-design-model-projections.md).
Those predecessor designs and their contracts retain authority.

The bridge is a relation ledger over already admitted Mono/Koto subjects and
already admitted CCDM semantic targets in that same exact scope. It makes
existing shared identities and evidence navigable; it is neither an analysis
model nor an Entity, Event, Workflow, or StateMachine model. It does not
assert that a Mono is an Entity or that a Koto is an Event.

## Retained bridge relation ledger

Every retained bridge relation has one exact Mono or Koto subject and one exact
semantic target. It preserves, without a view-local replacement:

- the exact Component identity and bounded CCDM context;
- the Mono/Koto source-subject identity and its admitted aggregation role;
- the target's exact semantic identity and its already admitted category;
- source attribution and a bounded source locator; and
- every shared condition affecting the subject, target, relation, attribution,
  locator, or navigation eligibility.

The ledger has structural and behavioral partitions only to disclose the
target category already admitted by CCDM. They do not create a new semantic
category, rank targets, or make an engineering-model assertion.

| Source subject | Permitted admitted target categories |
| --- | --- |
| Mono | Aggregate, Entity, Value, or admitted structural relation |
| Koto | Command, Event, Workflow activity, or state effect |

Each partition is many-to-many. A Mono or Koto can have zero or more retained
relations, and one target can have relations from zero or more distinct
subjects. An empty retained set communicates no admitted bridge relation; it
does not turn a missing, unavailable, or condition-affected semantic target
into a broader absence claim.

## Forward and reverse navigation

Forward navigation indexes the same retained relation ledger from its Mono or
Koto subject to every exact linked target. Reverse navigation indexes that
same ledger from a target to every exact linked Mono or Koto subject. The
reverse link is the inverse navigation of its corresponding retained forward
relation, not a duplicate or independently inferred semantic fact.

Therefore a reverse navigation record can exist only when its exact
corresponding forward bridge relation has been admitted, and a forward record
can exist only when that same retained record can yield its reverse navigation.
Both directions retain the same Component scope, bounded CCDM context, subject
and target identities/categories, provenance, locator, conditions, and
target-gated navigation eligibility. Neither direction may choose a primary
target, collapse the relations to one-to-one, or substitute a near identity.

A display label, type name, CML text, source order, diagram layout, Web
overview presentation label, other view, or view-local copy may explain a
retained relation but cannot create, repair, redirect, reverse, or broaden it.
A bridge relation does not itself make a receiving Entity/Event/Workflow or
StateMachine implementation exist.

## Conditions and navigation eligibility

The bridge carries the shared CCDM condition ledger distinctly for unavailable,
unauthorized, redacted, explicitly absent, ambiguous, conflicting, stale,
malformed, and limited material. The affected subject, target, relation,
attribution, and locator remain attributable even when navigation is not
eligible.

When the exact target, scope, provenance, locator, target implementation gate,
or condition does not admit navigation, the bridge suppresses that navigation
without deleting, rewriting, proxying, or substituting the retained relation.
It does not reauthorize material, disclose a withheld locator or attribution,
claim that an unavailable target is explicitly absent, or claim that a missing
semantic target is complete or resolved. Ambiguity and conflict remain
visible; freshness, authority, category, provider, display detail, or position
does not choose a winner.

## Deterministic relation navigation

Forward and reverse collections each use deterministic identity-first order.
For forward collections the exact source and target identities lead; for
reverse collections the exact target and source identities lead. Remaining
stable admitted identities follow, then an admitted stable non-semantic tie
key only when needed to make an order total.

Source order, labels, category preference, authority, freshness, ranking,
provider preference, and iteration order are never ordering, interpretation,
or selection rules. Where admitted keys do not make a total order, the bridge
keeps the unresolved ordering condition instead of inventing an order or
winner.

## Relationship to stakeholder and engineering views

The P9-31 stakeholder overview remains non-engineering by default. A later,
separately admitted delivery contract may offer exact bridge drill-down, but
this design neither alters that overview nor makes an overview label a bridge
input. CCDM and their owning canonical sources retain authority; a bridge
relation is evidence-preserving navigation, not an Entity, Value, Aggregate,
Command, Event, Workflow, state effect, or state-transition fact.

Future P9-40/P9-43 and P9-50/P9-53 engineering projections may consume exact
identities through their separately admitted contracts. They must not take
this bridge as permission to create their schema, detail, lifecycle, causal,
or persistence semantics. P9-33 likewise gains no Use Case selection,
relation, or navigation behavior from this design.

## Read-only and non-implementation boundary

The bridge is read-only. It cannot create, normalize, mutate, or reinterpret
Entity, Value, Aggregate, Command, Event, Workflow, state-effect, or state-
transition semantics; CCDM, CML, canonical sources, or Mono-Koto inputs. It
cannot infer implications or causality, transfer source authority, use a broad
query, label lookup, or proxy target, or edit feedback.

P9-32A creates no runtime value, executable specification, Entity/Event/
Workflow/StateMachine projection, Web route or renderer, API/schema,
persistence, source retrieval, feedback behavior, Use Case behavior,
Phase/checklist status update, validation result, or release claim.
