---
status: authored-unvalidated
decision_scope: P106-STEP-01
updated_at: 2026-10-03
---

# Internal-model Retained-state Security Contract

This contract specifies the pure Step 01 history and evidence policy for
[Phase 10.6](../phase/phase-10.6.md). Validation and independent review remain
pending. [Typed control references](internal-model-typed-control-contract.md)
and [repository rules](../rules/repository-rules.md) govern identities and
revisions. The [design](../design/internal-model-retained-state-security.md)
records responsibility boundaries.

## Storage authority

The project package is the complete selected-state authority for resumption.
Optional Entity-backed audit history records collaboration and historical
references; disposable target caches are derived outputs. Neither history nor
caches select current state, supply human approval, resolve issues, advance a
cursor, or replace independent requirements or actual human input. Historical
references may outlive their presence in the current package. Structural history
validity makes no current artifact-admission or subject-completeness claim.

Raw prompts and responses are excluded from project-source defaults and retained
history defaults. This Step implements the pure history policy; it does not prove
project-source or build-output exclusion.

## Immutable history vocabulary

All new types and policy operations are private to the runtime package. They
reuse `InternalModelRecordReference`, `InternalModelArtifactReference`,
`InternalModelPackageReference`, `InternalModelReviewSubject`,
`InternalModelSemanticScope` and `InternalModelDecisionActor` without a second
CCDM model or a byte-derived control identity.

`InternalModelRetainedHistoryRecord` has exactly `reference`, `subject`, `actor`,
`occurredAt`, `payload`, and ordered `evidence`. The audit reference is independent
of all payload references. The payload derives its `kind`; no duplicate tag is
stored.

| Payload / derived kind | Exact payload fields |
| --- | --- |
| Review | `reviewReference`, `candidateReference`, `semanticDiffReference`, `outcome` |
| Proposal | `candidateReference`, `projectionArtifactReference` |
| Alternative | `proposalReference`, `alternativeReference`, `disposition` |
| Supersession | `previousReference`, `successorReference` |
| Evidence | `evidenceReference` |

Review outcomes are `Recorded`, `ChangesRequested`, `Rejected`, and
`AcceptedAsReview`. The last is a review opinion only. Alternative dispositions
are `Considered`, `Rejected`, and `SelectedAsProposal`; selection here is only
recorded proposal history. Neither enum creates human approval or an action grant.

`InternalModelRetainedHistoryTombstone` has exactly `reference`,
`packageReference`, `scope`, `kind`, `action`, and `effectiveAt`. Its action is
`Expired` or `Deleted`. It retains typed record/package/scope attribution and
time, without payload, actor prose, provider text or evidence body. No expiry or
deletion operation is implemented in Step 01.

## Evidence input and retained output

An input has `reference`, `kind`, and `payload`. Kinds are `Prompt`, `Response`,
`ProviderIdentity`, `ModelIdentity`, `ToolIdentity`, `CallTree`,
`ExternalEvidence`, and `Narrative`. Input payloads are `Raw(value)`,
`SafeIdentity(value)`, `RedactedText(value, redactionReference)`, or `Unavailable`.
The structurally separate retained payload is `Identity(value)`,
`RedactedText(value, redactionReference)`, or `Omitted(reason)`; no raw variant
exists. Reasons are `PolicyExcluded` and `Unavailable`.

`InternalModelEvidencePolicy.retain(input)` first validates the whole supplied
input, including data destined for omission, then returns a
`Consequence[InternalModelRetainedEvidence]` with the same reference and kind.

| Valid kind / valid input payload | Retained result |
| --- | --- |
| Any / Unavailable | Omitted(Unavailable) |
| Any / Raw | Omitted(PolicyExcluded) |
| Prompt or Response / SafeIdentity or RedactedText | Omitted(PolicyExcluded) |
| ProviderIdentity, ModelIdentity, ToolIdentity / SafeIdentity | Identity, exact token |
| Narrative / RedactedText | Exact text and independent redaction reference |
| CallTree or ExternalEvidence / SafeIdentity or RedactedText | Omitted(PolicyExcluded), reference only |
| Any remaining SafeIdentity or RedactedText pairing | Structured invalid operation |

`validate(retained)` independently admits retained values. Identity is allowed
only for the three identity kinds; RedactedText only for Narrative. Either
omission reason is allowed for every kind. Constructors are not admission;
forged contradictory combinations reject.

Provider/model/tool identity is metadata, not authentication. A CallTree or
external-evidence reference neither retains the body nor authorizes retrieval;
neither operation dereferences it. Unavailable means original evidence cannot
be reconstructed from this record. Missing evidence or identity is never invented.

Redacted narrative is an upstream redaction owner's explicit claim. Its supplied
reference is not proof of secret removal or self-authentication. The authorized
Step 02 caller owns whether that pre-redacted narrative may be admitted. No regex
scrubber, raw opt-in, or prompt/response retention is introduced. Redaction can
lose evidence; retain omission/unavailability instead of reconstructing it.

## Finite limits and structural failure

Null records, enums, payloads, reference objects/fields, timestamps, collections
and elements reject before dereference. Existing constructors govern lowercase
package UUIDs, project/artifact tokens, positive artifact/record revisions and
nonblank logical IDs. Valid Unicode and whitespace in logical IDs are preserved
exactly; provider token rules do not apply to logical IDs.

| Dimension | Admission |
| --- | --- |
| Raw | Non-null, at most 1,048,576 characters; never echoed or retained |
| SafeIdentity / Identity | `[A-Za-z0-9][A-Za-z0-9._:-]*`, at most 128 characters |
| RedactedText | Nonblank, at most 4096 characters, no NUL; valid explicit redaction reference |
| Subject artifacts | At most 512 valid exact references, unique artifact IDs |
| Record evidence | At most 64 independently valid retained entries, unique full logical references |
| Scope and actor | Each of their three fields non-null and nonblank |
| Subject | Valid ID/revision, package, scope and artifacts |
| Time | Non-null caller-supplied Instant |

Review's three logical references must be distinct. Alternative and Supersession
each require distinct logical endpoint references. Proposal must name a Projection
artifact whose exact full reference occurs in its subject selections. Evidence
requires a valid logical evidence reference. Success preserves the complete input
value and evidence order without normalization, sorting or silent dropping.

Failures use `Consequence.operationInvalid` with stable generic dimension
diagnostics, never payload text, string-only generic failure, throw, repair or
approval inference. Independent invalid dimensions are collected.

## Supplied supersession relation

`validateSupersession(record, previous, successor)` first validates all three
records. The first must contain Supersession naming exactly the supplied endpoint
references. Both endpoints have the same non-Supersession kind. All three have
the same package and scope and distinct full record references. Changed subject
revisions are permitted and preserved. Cross-package/scope/kind, missing,
self-referential or conflicting endpoints reject. This validates one relation;
it infers no global graph, current head, latest value or endpoint from order.

`validate(record)` and `validateTombstone(tombstone)` are pure admission of the
supplied immutable values. No store, source acquisition or mutation occurs.

## Executable specifications and remaining Phase obligations

[Evidence policy specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEvidencePolicySpec.scala)
and [history specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryValidatorSpec.scala)
author Given/When/Then behavior and ScalaCheck properties for all payload families,
policy pairings, null graphs, limits, exact identities/revisions, tombstones,
supersession and encounter order. Their authoring is not passing validation.

Step 02 persistence/API is **NOT YET IMPLEMENTED**: P10-60 still requires optional
Entity/UnitOfWork persistence across independent reads, exact selections and
payload expiry/deletion with tombstones, plus package-only resume with absent,
unavailable, different or stale history. P10-61 still requires authenticated,
bounded read/propose/review/resume/record operations without implicit approval or
arbitrary mutation. P10-62 still requires integration of this evidence policy
through that authorized caller and persistence, preserving unavailable evidence.
No DB persistence or public/MCP endpoint is claimed here.

Step 03 exclusion proof is **NOT YET IMPLEMENTED**: P10-63 requires actual-owner
positive-control evidence that internal-model source is excluded by default from
runtime packaging, public APIs, ordinary CML generation, documentation publication
and CAR/SAR artifacts. All P10-60–63 obligations, release to Phase 10.7, checklist
completion and Phase acceptance remain subject to the
[Phase checklist](../phase/phase-10.6-checklist.md).
