---
status: working
decision_scope: P10-34A
updated_at: 2026-09-29
---

# Internal-model Candidate Approval Lifecycle Contract

This contract defines point-in-time applicability for one already admitted
P10-33 human approval. It is implemented by the package-private lifecycle
values, [portable supersession codec](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalSupersessionCodec.scala),
[pure evaluator](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalLifecycleEvaluator.scala),
and [executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateApprovalLifecycleSpec.scala).
It does not authenticate a human, choose an approval, authorize CML, or write
an artifact.

## Immutable derived state

`InternalModelCandidateApprovalLifecycleState` is closed: `approved`,
`rejected`, `changes-requested`, `superseded`, and `invalidated`. A report
always retains the complete original P10-33 admission, its historical decision,
the ordered complete invalidation evidence, and an optional explicit successor
input. It never replaces the original approval, candidate bytes/hash, rationale,
or ordered duplicate unresolved items.

Evaluation is not an irreversible mutable state machine. Re-evaluating the
same immutable approval with newly supplied current review or source-owner
evidence may produce a different derived state. No derived state proves
operational readiness, source authentication, actual human approval, or a CML
write grant.

The invalidation vocabulary is closed: candidate, review, semantic-diff,
reviewed-package, scope, rules, providers, changed source, unavailable source,
unauthorized source, malformed source, ambiguous/conflicting source, and
missing baseline. Every entry preserves its optional affected source artifact
ID and ordered concrete changed dimensions; it is not a free-form authority
claim.

## Portable explicit supersession

`ccdm-candidate-approval-supersession-v1` is canonical UTF-8 JSON with exactly
these root members: `predecessorApproval`, `profile`, `schemaVersion`, and
`successorApproval`. The profile is exactly that name and schema version is
`1.0`. Each tuple has exactly `artifactId` and lowercase raw `sha256:` digest.
Artifact IDs are nonblank Unicode scalar sequences retained verbatim; a tuple
cannot name itself.

The byte grammar is strict: no BOM, duplicate/unknown/missing member,
whitespace, CRLF, trailing data, noncanonical unsigned-UTF-8 key order,
wrong/null field type, malformed UTF-8, surrogate escape, Unicode-blank ID, or
typed re-encoding difference is accepted. Encoding has exactly one final LF.

The caller supplies both the link and a separately admitted successor approval.
The predecessor tuple equals the original external approval tuple and the
successor tuple equals the successor external approval tuple. The link is
portable external selection evidence only: it retires one predecessor and does
not grant successor applicability, human approval, ranking, authentication, or
storage authority. There is no latest selection, timestamp, automatic ranking,
filesystem selector, transitive-history engine, or new package schema.

The successor must have the same project namespace/ID/package ID, scope, candidate
identity, and candidate-model identity; cannot roll candidate revision back;
and cannot change candidate bytes at the same candidate revision. Within one
approval identity, successor revision is strictly greater. Distinct approval
identities intentionally have no numeric ordering: the exact explicit link is
the only replacement selection. A higher candidate revision may have changed
candidate bytes, and a genuine successor may hold any human decision.

## Currentness evidence and precedence

The evaluator accepts explicit original approval, current review admission,
independent current rule/provider basis, full historical source-snapshot
inventory, caller-owned observations, and optional link. It validates canonical
approval/review/diff/candidate/link bytes and their external hashes before
classifying currentness. A malformed original historical basis is a structured
failure, not an ordinary drift reason.

It compares exact candidate artifact ID/hash and identity/model/revision,
review artifact ID/hash and identity/revision, diff artifact ID/hash and
identity/revision, all semantic-scope fields, all six historical reviewed
package fields, and original and current review rule/provider basis against the
independent expected basis. Carrier-only additions, transport, or carrier
revision are not historical-subject drift. Every differing dimension is kept;
comparison never short-circuits.

The supported reviewed-package schema is fixed at `1.0`. A current admission
whose retained package-context fields disagree with its canonical reviewed
manifest (including schema version) is structurally invalid and fails closed;
it is not a hand-forged successful currentness variant.

The supplied source snapshots must exactly equal every historical reviewed
`source-snapshot` inventory entry, including optional absence and its metadata.
Each present snapshot has valid canonical grammar and its exact historical raw
hash. The observations map has only those artifact IDs; a missing known
observation means `SourceUnavailable`. The evaluator compares every present
snapshot through `InternalModelSourceSnapshotFreshness.compare`, without I/O,
and preserves changed authority, identity, revision, raw bytes, and CML path
dimensions. Optional absence becomes `MissingBaseline`.

Invalidations appear first by the closed basis-kind order, then by unsigned
UTF-8 source artifact ID and the existing deterministic source dimensions. A
valid link has precedence and yields `Superseded`, while retaining all current
drifts. Without one, any invalidation yields `Invalidated`; otherwise the
original historical human decision maps exactly to `Approved`, `Rejected`, or
`ChangesRequested`. A successor is evaluated independently with its own
evidence when applicability is needed.

## Boundary

This contract neither reads a filesystem, source, provider, Git, clock, or
network nor writes state. It owns applicability only. Future resume
orchestration owns storing/selecting a portable link, and later contracts own
human authentication, reconciliation, CML permission, and CML application.
