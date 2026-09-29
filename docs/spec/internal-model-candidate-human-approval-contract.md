---
status: working
decision_scope: P10-33A
updated_at: 2026-09-29
---

# Internal-model Candidate Human Approval Contract

This contract defines the closed `ccdm-candidate-human-approval-v1` record.
It is implemented by the package-private value model, canonical codec, and
[executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCandidateHumanApprovalValidatorSpec.scala).
The [candidate-review binding contract](internal-model-candidate-review-binding-contract.md)
continues to own provider/rule evidence and the complete historical review
subject.

## Record and grammar

The canonical UTF-8 JSON root has exactly `approval`, `profile`, and
`schemaVersion`, with profile `ccdm-candidate-human-approval-v1` and schema
version `1.0`. There is no BOM, whitespace, duplicate key, unknown/missing
member, noncanonical key order, invalid UTF-8 or surrogate escape, absent or
extra final LF, or trailing data. Decode accepts only byte-identical typed
canonical re-encoding.

`approval` has exactly `actor`, `approvalIdentity`, `approvalRevision`,
`basis`, `decision`, `provenance`, `rationale`, and `unresolvedItems`. The
closed decisions are `approved`, `rejected`, and `changes-requested`. Actor is
exactly a nonblank `human` kind, identity, and role. Provenance is exactly
`human-decision`, identity, nullable nonblank locator/revision, and a lowercase
raw `sha256:` digest. These fields are attributable human-input claims; they
are neither authentication nor signature verification.

All identity and prose fields are nonblank Unicode scalar sequences retained
verbatim. Revisions are positive canonical JSON integers. `unresolvedItems` is
a required ordered, duplicate-preserving array; an empty array records no item
without proving absence. An approved record may retain unresolved items.

`basis` exactly carries candidate/review/diff artifact ID-plus-raw-hash tuples,
their identities and revisions, the exact three-part semantic scope, and the
six-field historical reviewed package context. The package has schema `1.0`, a
lowercase UUID, V1 project tokens, positive long revision, and exact digest.
The record serializes neither its own artifact hash, the carrier package hash,
a timestamp, provider rank, nor review snapshots.

## Admission and failure matrix

The caller explicitly selects a present approval-role artifact and a present
validation review artifact from one captured package. The approval depends on
exactly that review ID. Missing, optional absent, wrong-role, ambiguous,
tampered, malformed, or wrong-dependency artifacts reject. Other approval-role
siblings remain inert: no latest, ordering, hash, profile, or time heuristic
selects or decodes them.

Admission first applies the P10-32 review validator using independently supplied
rules/providers. It also validates a separately supplied complete human input,
then requires exact equality to the decoded record: no default, re-binding,
trimming, repair, or self-authorizing value exists. Candidate/review/diff
tuples, identities, revisions, scope, and every historical package field bind
to the admitted P10-32 review result. The reviewed package is the complete
historical subject, never its newer carrier.

Any grammar, typed-input, selection, hash, identity, revision, scope, package,
rule/provider, or captured-syntax mismatch fails closed. Admission never reads
a provider, provenance source, CML, Git, or a path after capture; it does not
authenticate a person, mutate an artifact, grant CML permission, make a
lifecycle/currentness conclusion, or transform a rejected/changes-requested
record into approval.

## Boundary

P10-32 provider `approved` evidence is not a human decision. A matching
`Approved` record is retained evidence for that exact whole candidate basis,
not P10-34 applicability, supersession/invalidation, actual candidate approval,
or a canonical CML write grant.
