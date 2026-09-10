# Static Cross-View Navigation

status=stable
decision_scope=P9-43A
updated_at=2026-09-10

## Purpose and authority

This design fixes one read-only static cross-view navigation index for one
exact already established Component identity and one bounded Canonical Component
Design Model (CCDM) context. It makes explicitly admitted shared semantic
identities navigable between the Mono-Koto, Entity, Structure, and
Classification projections without creating a shared replacement model.

The [Canonical Component Design Model Projections](canonical-component-design-model-projections.md)
and its [contract](../spec/canonical-component-design-model-contract.md)
retain authority for semantic identity, source authority, attribution, bounded
locators, conditions, navigation, and feedback. The
[Mono-Koto Semantic Bridge Model](mono-koto-semantic-bridge-model.md),
[Entity Model Projection](entity-model-projection.md),
[Structure View Projection](structure-view-projection.md), and
[Classification View Projection](classification-view-projection.md) retain
authority for their respective projection roles and assertions. Their
companion contracts remain normative.

This index is a relation over existing projection records. It is not a
Mono-Koto-to-Entity conversion, an Entity/Structure/Classification normalizer,
a source reader, a CML interpreter, or a canonical source of design truth. Its
companion [Static Cross-View Navigation Contract](../spec/static-cross-view-navigation-contract.md)
defines the normative requirements.

## Exact admitted identity index

For this boundary, a `ComponentDashboardSemanticTargetIdentity` is a retained
contract record pairing one existing projection record with its already
admitted exact shared semantic target identity. It is not a new runtime type,
canonical identity, persistence key, or identity-recovery algorithm. Every
such record retains:

- the exact Component identity and bounded CCDM context;
- the exact shared semantic target identity;
- the originating static projection and its independently admitted role;
- source and authority attribution;
- a bounded source locator; and
- distinct shared condition state for the record, attribution, locator, and
  navigation.

A static cross-view mapping relates two of these records only when that mapping
is itself explicitly admitted in the same Component/context. Sharing a label,
name, type, namespace, CML text, source order, layout, query result, visual
geometry, or view-local copy does not admit either an identity record or a
mapping. Nor does merely observing the same displayed identity establish an
unadmitted pairwise mapping.

The source and counterpart records remain independently retained. A mapping
preserves their distinct view/role, attribution, locator, and conditions; it
does not copy assertions from one record to the other or make either record an
authority for the other's meaning.

## Static projection roles and Mono-Koto links

The index recognizes only the following independently admitted static
projection roles:

| Projection | Retained source or counterpart role |
| --- | --- |
| Mono-Koto | Mono or Koto subject, or an admitted source reference from that subject. |
| Entity | Entity, Value, Aggregate, or independently admitted identity, ownership, lifecycle, or aggregate-boundary metadata/relationship. |
| Structure | Admitted structural relation, independently admitted structural-field assertion, or exact endpoint. |
| Classification | Admitted taxonomy relation, subject, endpoint, powertype dimension, or independently admitted taxonomy-field assertion. |

A Mono-Koto subject or source reference may navigate only through its retained
exact shared semantic target identity to an explicitly admitted Entity subject
or metadata record, Structure relation/assertion/endpoint record, or
Classification relation/subject/endpoint/dimension/assertion record. The
index does not claim that a Mono is an Entity, that a reference is a Structure
relation, or that a Classification assertion is Entity or Structure evidence.

Entity, Structure, and Classification records keep their independently
admitted roles and assertions. Cross-view membership does not convert Entity
metadata into structural or taxonomy semantics, or turn Structure or
Classification material into a Mono/Koto aggregation, canonical fact, or
cross-view source of authority.

## Attributable counterparts and bounded gaps

Every retained mapping, visible counterpart record, omitted counterpart, and
navigation affordance carries the distinct CCDM condition state: unavailable,
unauthorized, redacted, explicitly absent, ambiguous, conflicting, stale,
malformed, and limited. Conditions remain attributable to the affected source
record, counterpart record when present, attribution, locator, and affordance.
They are not converted into one another or resolved by freshness, authority,
detail, display order, or plausibility.

When a requested counterpart is unavailable or has no implemented receiving
contract, the index retains an attributable bounded gap instead of completing
the navigation. The gap identifies the exact Component/context; the affected
retained source identity; the requested counterpart projection and role; a
known exact counterpart identity only when already admitted; source/authority
attribution and a bounded locator or unavailable scope; and its distinct
condition and limitation reason. It has no inferred identity, broad lookup,
proxy target, replacement source, semantic merge, or negative assertion.

Explicit absence remains limited to its source-declared bounded scope. An
unavailable, unauthorized, redacted, ambiguous, conflicting, stale, malformed,
or limited counterpart is not an absent counterpart, and an omitted counterpart
does not remove the retained source record or its conditions.

## Separate target-gated forward and reverse navigation

Retention is distinct from navigation. Each admitted mapping may expose a
forward affordance from its source record to its counterpart and a reverse
affordance from that counterpart to its source. They are separate affordances
over the same retained mapping; neither creates a duplicate assertion,
one-to-one correspondence, primary target, or authority transfer.

Each direction is available only when all of the following are retained and
usable for that direction:

1. the exact receiving target identity and exact Component/context;
2. source and receiving-record attribution and bounded locators;
3. the distinct condition state of the source, counterpart, mapping,
   attribution, and locators; and
4. an implemented receiving contract for that exact receiving target.

Failure of a gate suppresses only that navigation affordance. It does not
delete, rewrite, proxy, substitute, complete, reverse by inference, or loosely
look up the mapping, either retained record, or its attributable gap. A label,
name, CML text, source order, layout, another projection, or a view-local copy
cannot create a dead link or recover an ineligible target.

## Deterministic identity-first presentation

Mappings and visible counterpart records use deterministic identity-first
order: exact shared semantic target identity first; then the originating view
and role; then the counterpart view/role and remaining stable admitted
identities; and only then an admitted stable non-semantic tie key needed to
make the order total. Source order, labels, names, namespaces, CML, queries,
visual geometry, layout, authority, freshness, ranking, provider preference,
and iteration order cannot choose an order, interpretation, or winner.

If the admitted identity and tie keys cannot establish a total order, the index
retains the unresolved ordering condition rather than inventing an order or
winner. The same rule applies to forward and reverse counterpart collections;
the direction changes the affordance, not the semantic ordering authority.

## Read-only and non-implementation boundary

The index is read-only with respect to CCDM, Mono-Koto, Entity, Structure,
Classification, CML, Cozy material, canonical sources, and feedback. It cannot
retrieve or mutate source material; create, reconstruct, normalize, merge, or
transfer static semantics; infer a relation, endpoint, role, taxonomy fact, or
source winner; or make feedback editable.

P9-43A creates no runtime value, Scala model, executable specification, test,
API, schema, persistence, Web route or renderer, source/CML I/O, technical
delivery, feedback behavior, validation result, review, commit, publication,
deployment, or Phase/checklist status update. P9-43B separately owns runtime
implementation and executable specifications for static cross-view navigation.
