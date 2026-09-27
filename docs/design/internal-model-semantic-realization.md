---
status: draft
decision_scope: P10-20A--P10-20B
updated_at: 2026-09-28
---

# Internal-model Semantic Realization Design

This design explains the durable-recording boundary established by the
[Internal-model Semantic Realization
Contract](../spec/internal-model-semantic-realization-contract.md). That
contract is normative. The package envelope remains owned by the
[Internal-model Package Contract](../spec/internal-model-package-contract.md),
and semantic meaning remains owned by the Phase 9 [Canonical Component Design
Model Contract](../spec/canonical-component-design-model-contract.md).

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
database. The manifest binds exact raw bytes. Its explicit `dependsOn` links
bind exact source-snapshot artifacts used by the candidate, while each source
reference retains source-owned anchor, authority, identity, revision when
available, and its exact snapshot-supplied raw-source hash. Only revision and
locator may be unavailable; every selected source snapshot has an exact
raw-source hash.

Consequently, a later process can locate the same candidate and asserted basis
without asking a prior chat or provider to reconstruct it. That process still
cannot manufacture a missing source assertion: unknown, unavailable, redacted,
malformed, ambiguous, or conflicting evidence remains a recorded condition.
The realization contains its own identity/profile and consumed-basis
references. Separately, external manifest/package context carries the package
revision and the inventory entry's artifact ID/path and raw-byte digest; the
digest is not an in-band realization field. Digest binding makes selected bytes
identifiable; it does not make them approved, fresh, accepted in Git, or safe
to apply to CML.

```text
Phase 9 CCDM identity ledger and source authority
                    |
                    | exact identities and attributed assertions
                    v
selected Phase 10.1 source snapshots -----> realization artifact
         (selected basis)             dependsOn / source anchors
                                                |
                                                | exact raw-byte digest
                                                v
                                    project-owned package manifest
                                                |
                                                v
                          later process resumes the same candidate state
```

The arrows preserve traceability, not authority transfer. A snapshot anchor is
neither a credential nor permission to read a source, and a realization digest
is not a replacement for source authority.

## P10-20B read-only admission path

The executable path deliberately has one filesystem/inventory authority pass:

```text
project-bound package validation
  -> one present realization entry + its verified bytes/dependencies
  -> exact verified source-snapshot bytes
  -> Phase 10.1 snapshot validation before extraction
  -> realization canonical-JSON parse and source-witness matching
  -> immutable candidate whose re-encoding is byte-identical
```

The package validator does not decide semantic readiness: it returns the
manifest-order verified source snapshots and the selected present realization
entry without rereading a mutable artifact path. The realization validator then
checks the closed V1 field grammar, exact source envelope and anchor witness,
Component/context/Use Case scope, lane separation, endpoints, role/direction,
conditions, and manifest dependency equality. It only returns an immutable
candidate record. There is no writer, approval result, source refresh, CML
application, projection, rehydration, or interpretation of opaque source prose
on this path.

This is a bounded structural/source-basis proof. It neither establishes a
human decision, live-source freshness, Git acceptance, safe CML application,
complete rehydration, nor P10-21 reconstruction of all eight projections.

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

## Manifest-led candidate integrity

The package manifest supplies the structural boundary: its entry has role
`realization`, the artifact digest covers exact raw bytes, and `dependsOn` lists
every consumed source snapshot. The realization gives semantic meaning to those
dependencies by naming the exact snapshot artifact ID, exact snapshot-supplied
raw-source hash, and source-owned anchor for every assertion, condition,
limitation, and successor link. For a `cml-baseline`, the owner-supplied
`basis.projectRelativePath` identifies the bounded exact-byte baseline file;
it is not an assertion-level source-owned anchor or evidence of a particular
CCDM assertion. When a required finer assertion anchor is unavailable,
admission fails rather than inferring one from CML text or the file path.

This two-level arrangement prevents a closed inventory from masquerading as a
complete interpretation. The manifest can prove a closed set of bytes and
dependencies; only the realization can say which selected basis it used, and
even that candidate record cannot establish that all semantic facts are present
or currently fresh. Missing endpoint/basis, dangling or cross-scope ID,
unresolvable provenance, or digest/dependency mismatch is rejected rather than
repaired from a view copy or plausible source.

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
byte grammar and executable failure cases; Phase 10.3 owns projection, review,
and approval; Phase 10.4 owns rehydration; and Phase 10.5 owns drift and the
CML gate.

## Deliberate non-claims

The candidate state is not a human decision, a CML-write capability, a current
live-source statement, Git acceptance, or full rehydration. The design creates
no parser, storage implementation, public API, provider integration, approval
record, CML mutation, or executable specification. It leaves such behavior to
its explicitly owned later phase and contract.
