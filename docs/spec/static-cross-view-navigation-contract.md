# Static Cross-View Navigation Contract

## Scope and status

status=stable
decision_scope=P9-43A
updated_at=2026-09-10

This specification normatively defines one read-only static cross-view
navigation index for one exact already established Component identity and one
bounded Canonical Component Design Model (CCDM) context. The
[Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
retains authority for semantic identity, source authority, attribution, bounded
locators, shared conditions, navigation, and feedback. The
[Mono-Koto Semantic Bridge Contract](mono-koto-semantic-bridge-contract.md),
[Entity Model Projection Contract](entity-model-projection-contract.md),
[Structure View Projection Contract](structure-view-projection-contract.md),
and [Classification View Projection Contract](classification-view-projection-contract.md)
retain authority for their independently admitted projection roles and
assertions.

This contract specifies a documentation-only navigation boundary. It does not
claim a Scala model, runtime, executable specification, test, API, schema,
persistence, Dashboard/Web view, source or CML retrieval/mutation, feedback
editor, canonical mutation, technical delivery, validation result, or Phase
status behavior.

## Exact identity-record and mapping admission

A `ComponentDashboardSemanticTargetIdentity` record MUST pair one existing
projection record with one already admitted exact shared semantic target
identity. It MUST NOT create a new semantic identity, runtime type,
persistence identity, canonical assertion, or identity-recovery mechanism.
Every such record MUST retain and keep distinct:

- the exact Component identity and bounded CCDM context;
- the exact shared semantic target identity;
- the originating static projection and its independently admitted role;
- source and authority attribution;
- a bounded source locator; and
- the applicable distinct shared condition state for the record, attribution,
  locator, and navigation.

Each static cross-view mapping MUST be explicitly admitted in that same
Component/context and relate two retained identity records. Sharing a display
label, name, type, namespace, CML text, source order, layout, query result,
visual geometry, view-local copy, or another projection MUST NOT establish,
recover, repair, redirect, replace, broaden, or pair an identity record or
mapping. Observing the same displayed identity MUST NOT create an unadmitted
pairwise mapping.

The mapping MUST preserve each source and counterpart record's distinct
projection/role, attribution, locator, and condition state. It is navigation
and explanation only; it MUST NOT copy an assertion, transfer authority, or
turn either record into evidence for an independent role in another projection.

## Independent static projection roles

The index MAY retain only the following existing, independently admitted roles:

| Projection | Permitted retained role |
| --- | --- |
| Mono-Koto | Mono or Koto subject, or an admitted source reference from that subject. |
| Entity | Entity, Value, Aggregate, or independently admitted identity, ownership, lifecycle, or aggregate-boundary metadata/relationship. |
| Structure | Admitted structural relation, independently admitted structural-field assertion, or exact endpoint. |
| Classification | Admitted taxonomy relation, subject, endpoint, powertype dimension, or independently admitted taxonomy-field assertion. |

A Mono-Koto subject or source reference MUST navigate only through its retained
exact shared semantic target identity to an explicitly admitted Entity subject
or metadata record, Structure relation/assertion/endpoint record, or
Classification relation/subject/endpoint/dimension/assertion record. This
contract MUST NOT require or imply `Mono = Entity`, make a Mono-Koto reference
a Structure relation, or make a Classification assertion Entity or Structure
evidence.

Entity, Structure, and Classification records MUST retain their independently
admitted roles and assertions. Cross-view membership MUST NOT turn Entity
metadata into structural or taxonomy semantics, or turn Structure or
Classification material into a Mono/Koto aggregation, canonical fact, or
cross-view source of authority.

## Conditions and attributable bounded gaps

Every retained mapping, source record, counterpart record, omitted counterpart,
attribution, locator, and navigation affordance MUST retain, distinguish, and
attribute the following conditions:

| Condition | Required interpretation |
| --- | --- |
| unavailable | The bounded source or counterpart cannot supply material; it is not explicit absence. |
| unauthorized | The required admission or access basis is lacking; it is not absence or redaction. |
| redacted | Known material, attribution, or locator is withheld; it is not unavailable material. |
| explicitly absent | The source cannot establish an expected fact within its stated scope; it is not a broader negative fact. |
| ambiguous | More than one admissible identity, mapping, role, or interpretation remains without a source-owned discriminator. |
| conflicting | Attributed material asserts incompatible facts or mappings; all affected attributions remain visible. |
| stale | Supplied freshness state prevents representation as current. |
| malformed | Admitted material cannot support its intended statement and cannot be repaired by local inference. |
| limited | A source, authority, projection, compatibility, implementation, or disclosure boundary prevents a stronger claim. |

When a requested counterpart is unavailable or lacks an implemented receiving
contract, the index MUST retain an attributable bounded gap rather than
complete the navigation. The gap MUST retain:

1. the exact Component identity and bounded CCDM context;
2. the affected retained source identity;
3. the requested counterpart projection and role;
4. a known exact counterpart identity only when already admitted;
5. source and authority attribution and a bounded locator or bounded
   unavailable scope; and
6. the applicable distinct condition and limitation reason.

A gap MUST NOT contain an inferred identity, broad lookup, proxy target,
replacement source, semantic merge, hidden winner, or negative assertion. The
index MUST NOT convert unavailable, unauthorized, redacted, ambiguous,
conflicting, stale, malformed, or limited material into explicit absence; turn
explicit absence into a broad negative fact; disclose withheld material;
reauthorize material; or resolve ambiguity or conflict.

## Separately target-gated forward and reverse navigation

Retention of a mapping is distinct from navigation. An admitted mapping MAY
expose a forward affordance from its source record to its counterpart and a
reverse affordance from that counterpart to its source. These are separate
affordances over the same retained mapping; neither is a duplicate assertion,
one-to-one correspondence, primary target, or authority transfer.

Each direction MAY be exposed only when all of the following are retained and
usable for that direction:

1. the exact receiving target identity and exact Component/context scope;
2. source and receiving-record attribution and bounded locators;
3. the distinct condition state of the source, counterpart, mapping,
   attribution, and locators; and
4. an implemented receiving contract for that exact receiving target.

When any gate is unmet, the index MUST suppress only that affordance. It MUST
NOT delete, rewrite, proxy, substitute, complete, reverse by inference, or
loosely look up the retained mapping, either record, or its attributable gap.
It MUST NOT create a dead link, broad query, name-based target, reconstructed
target, or cross-view semantic reconstruction.

## Deterministic identity-first order

Every mapping and visible counterpart collection MUST use deterministic
identity-first order: exact shared semantic target identity first; then the
originating view and role; then the counterpart view/role and remaining stable
admitted identities; and only then an admitted stable non-semantic tie key
needed to make the order total. The same order applies to forward and reverse
collections; direction changes the affordance, not the semantic ordering
authority.

Labels, names, namespaces, types, CML, queries, source order, visual geometry,
layout, authority, freshness, ranking, provider preference, and iteration order
MUST NOT choose an order, interpretation, or winner. If admitted keys cannot
establish a total order, the unresolved ordering condition MUST remain explicit
instead of selecting an iteration order.

## Read-only prohibitions and later work

The static index MUST remain read-only with respect to CCDM, Mono-Koto, Entity,
Structure, Classification, CML, Cozy material, canonical sources, and
feedback. It MUST NOT retrieve or mutate source material; create, reconstruct,
normalize, merge, or transfer static semantics; infer a relation, endpoint,
role, taxonomy fact, source fact, or winner; or provide editable feedback.

This contract does not authorize runtime, executable-specification, test, API,
schema, persistence, Dashboard/Web, source/CML I/O or mutation, technical
delivery, feedback behavior, validation, review, commit, push, publication,
deployment, Phase/checklist/journal update, or Phase closure. P9-43B
separately owns runtime implementation and executable specifications for static
cross-view navigation.
