---
status: target
decision_scope: P10-20A--P10-20B / P104-TYPED-SEMANTIC-REALIZATION
updated_at: 2026-10-01
---

# Internal-model Semantic Realization Design

This V3 target design explains the durable-recording boundary established by the
[Internal-model Semantic Realization
Contract](../spec/internal-model-semantic-realization-contract.md). That
contract is normative. The package envelope remains owned by the
[Internal-model Package Contract](../spec/internal-model-package-contract.md),
and semantic meaning remains owned by the Phase 9 [Canonical Component Design
Model Contract](../spec/canonical-component-design-model-contract.md).
The [Typed Control Contract](../spec/internal-model-typed-control-contract.md)
owns explicit record/artifact identities and versions; the
[Source Snapshot Contract](../spec/internal-model-source-snapshot-contract.md)
owns V2 source evidence. Original P10-20 acceptance at commit
`fb3716d071692fb9e2540b7459ce0a5d31113c97` remains historical evidence.
V3 authoring, coordinated downstream conversion, parent validation and independent
review do not establish current acceptance or full Phase 10.4 closure.

## Purpose: retain the CCDM, do not model it again

A realization is a portable candidate record for one exact Component, one
bounded CCDM projection context, and one selected Use Case. It retains the
Phase 9 semantic ledger needed to resume that selected candidate, including
identity-bearing elements, role/direction-bearing relationships, exact
endpoints, and their attributed evidence state.

The artifact is deliberately a recording boundary, not another domain model.
It does not translate the CCDM into a separate Entity/Event/Workflow schema,
collapse Mono-Koto into Entity/Event, infer a relation from a view, or give CML
or storage structure semantic primacy. Its fields repeat information needed to
retain an already admitted assertion; they do not define what that assertion
means. That distinction keeps Phase 9 as the one semantic authority and avoids
making a persistence format the hidden owner of design semantics.

## Durable handoff across processes

The selected realization is portable because identity and evidence links are
recorded in the project-owned internal-model package, not held in chat history,
provider session state, process memory, `target/`, or a retained CBD Support
database. The manifest binds explicit artifact identity/revision/role. Its typed `dependsOn` links
bind exact source-snapshot versions used by the candidate, while each source
reference retains source-owned anchor, authority, identity, revision when
available, and locator. Only source revision and locator may be unavailable.
Unknown source revisions remain unknown: positive record and artifact revisions
provide no substitute source version or proof of freshness.

Consequently, a later process can locate the same candidate and asserted basis
without asking a prior chat or provider to reconstruct it. That process still
cannot manufacture a missing source assertion: unknown, unavailable, redacted,
malformed, ambiguous, or conflicting evidence remains a recorded condition.
The realization contains its explicit logical RecordReference/profile and consumed-basis
references. Separately, external manifest/package context carries the package
revision and the inventory entry's ArtifactReference/path. The logical record
uses the existing opaque realization identity as recordId and a producer-supplied
positive Long recordRevision. It is distinct from a semantic identity, an
artifact reference and source revision. These declared versions provide control
provenance, with no content-derived identity, hash authenticity or undeclared
mutation detection. The producer advances versions explicitly when their control
meaning changes. A validator neither allocates missing versions nor infers history.

```text
Phase 9 CCDM identity ledger and source authority
                    |
                    | exact identities and attributed assertions
                    v
selected Phase 10.1 source snapshots -----> realization artifact
         (selected basis)             dependsOn / source anchors
                                                |
                                                | exact artifact reference
                                                v
                                    project-owned package manifest
                                                |
                                                v
                          later process resumes the same candidate state
```

The arrows preserve traceability, not authority transfer. A snapshot anchor is
neither a credential nor permission to read a source. Declared artifact and record
versions never transfer source authority or imply approval.

## One-capture V3 read-only admission path

The executable path deliberately has one filesystem/inventory authority pass:

```text
project-bound package validation
  -> one present realization reference + captured ordinary bytes/typed dependencies
  -> exact captured source-snapshot reference selection
  -> source V2 validation before extraction
  -> strict realization V3 parse and scoped source-witness matching
  -> immutable semantic candidate, with ordinary encoding on demand
```

The package validator does not decide semantic readiness: it returns the
manifest-order verified source snapshots and the selected present realization
entry without rereading a mutable artifact path. The realization validator then
checks the closed V3 field grammar, exact source envelope and anchor witness,
Component/context/Use Case scope, lane separation, endpoints, role/direction,
conditions, and manifest dependency equality. It only returns an immutable
candidate record. Null capture metadata, duplicate supplied source IDs/references,
wrong role/revision and missing captured dependencies reject before returning a
partial record. No bare-ID lookup or duplicate-collapsing map admits a selection.
Selected source bytes are validated before extraction; unrelated snapshots are
structurally captured without semantic parsing. Captured admission performs no
filesystem reread. There is no approval result, source refresh, CML
application, projection, rehydration, or interpretation of opaque source prose
on this path.

This is a bounded structural/source-basis proof. It neither establishes a
human decision, live-source freshness, Git acceptance, safe CML application,
complete rehydration, nor P10-21 reconstruction of all eight projections.

Only `ccdm-realization-v3` / schemaVersion `3.0` is admitted, consuming package
V2 and source V2. V1/1.0, V2/2.0 and mismatched pairs reject without a retained
reader, overload, compatibility alias, migration or inferred version.
Strict UTF-8, BOM refusal, duplicate rejection, exact closed nested fields and
positive lexical Long revisions remain mandatory. Harmless object-key order,
whitespace and JSON escaping variation are admitted. There is no canonical-byte
cache, whole-file equality or re-encoding admission check; a deterministic encoder
is ordinary serialization only.

V3 requires `association` in every assertion object. Ordinary assertions record `null`; a canonical
relationship assertion may record one closed, typed source-backed association.
An enrichment assertion always records `null`.

The V3 association is deliberately an assertion detail rather than a new CCDM
model. Its relationship target asserts a directional relation to one retained
element or relationship identity. The closed role vocabulary fixes whether the
related target is an element or relationship, and one asserting relationship
cannot carry duplicate or competing claims for the same role. The exact source
reference still targets the asserting relationship, while the exact selected
snapshot fact has the literal association content. This lets role, sequence,
and association assertions retain different source-owned anchors when the
source provides different facts. No view, label, CML path, endpoint pair,
enrichment record, or serialized order gains authority as a substitute.

The fixed role grammar is intentionally small: `owner` names an element or
relationship; `subject`, `affected-subject`, and `affected-endpoint` name an
element; and `affected-relationship` names a relationship. It is a persisted
source-attribution constraint, not a conversion or ownership rule for Phase 9
objects. The realization therefore retains the complete association assertion
and source reference in its evidence ledger without transferring CCDM or
source authority to this package.

## Two assertion lanes and one condition ledger

The artifact has two intentionally non-interchangeable assertion lanes.
Canonical assertions report the canonical source's bounded claim. Enrichment
assertions retain useful but bounded evidence with their own attribution.
Keeping them separate prevents a detailed provider observation, convenient
document, or display-friendly summary from being silently promoted to design
truth.

Conditions form the shared visibility ledger alongside those assertions:

```text
canonical assertion ----+----> exact semantic ID / relationship endpoints
                         |                 |
enrichment assertion ---+                 +--> source reference and anchor
                                           |
condition / limitation -------------------+--> explicit absence, ambiguity,
                                                conflict, authorization/redaction,
                                                availability/staleness, malformed
                                                evidence, or limitation
```

This arrangement retains disagreement and incompleteness. There is no
resolution by order, freshness, label, diagram placement, source path, or
lexical resemblance. If a source revision changes the asserted subject, a new
identity or explicit successor link preserves the historical boundary;
overwriting an old identity would make later review and projection navigation
ambiguous.

## Manifest-led exact version attribution

The package manifest supplies the structural boundary: its entry has role
`realization`, a positive producer-declared artifact revision, and `dependsOn` lists
every consumed source ArtifactReference. The realization gives semantic meaning to those
dependencies by naming the exact snapshotReference, source metadata
and source-owned anchor for every assertion, condition,
limitation, and successor link. For a `cml-baseline`, the owner-supplied
`basis.projectRelativePath` identifies the bounded exact-byte baseline file;
it is not an assertion-level source-owned anchor or evidence of a particular
CCDM assertion. When a required finer assertion anchor is unavailable,
admission fails rather than inferring one from CML text or the file path.

This two-level arrangement prevents a closed inventory from masquerading as a
complete interpretation. The manifest can establish a closed inventory and
exact declared dependencies; only the realization can say which selected basis it used, and
even that candidate record cannot establish that all semantic facts are present
or currently fresh. Missing endpoint/basis, dangling or cross-scope ID,
unresolvable provenance, or exact reference/dependency mismatch is rejected rather than
repaired from a view copy or plausible source. The source metadata has exactly
authority, identity, locator and revision. Comparing it with admitted snapshot
metadata is semantic attribution equality; comparing assertion content with its
exact scoped source witness is semantic string equality. Neither comparison is a
whole-file control protocol or authentication. Matching explicit unknown source
revisions may admit recorded attributed state but cannot establish semantic
completeness or permit an action requiring a known source version.

## Eight-view continuation without storage interpretation

The realization keeps the same identity ledger that the eight Phase 9 views
consume. A later Mono-Koto, Use Case, Entity, Event, Structure, Classification,
Workflow, or StateMachine presentation follows an existing semantic element or
relationship ID back to its attributed record. It may choose a subset or
presentation form, but cannot make storage layout, serialization order,
diagram geometry, or a label into semantic evidence.

This design establishes continuity, not proof. It does not choose an exact
serialization for a view binding, establish a view cache, or show every
projection is reconstructible. P10-21 owns that future proof. P10-20B owns the
original grammar history; the current V3 target owns the typed realization shape
and its migrated executable scenarios. Phase 10.3 owns projection, review,
and approval; Phase 10.4 owns rehydration; and Phase 10.5 owns drift and the
CML gate.

## Deliberate non-claims

The candidate state is not a human decision, a CML-write capability, a current
live-source statement, Git acceptance, or full rehydration. Continuity,
decision/issue, candidate/diff, review/approval/lifecycle and continuation consumers
require separately owned V3 integration. Parent validation and independent review
remain pending. This target changes no earlier Phase checklist/history, claims
no current Step acceptance and grants no CML mutation authority.
