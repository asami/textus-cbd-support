---
status: accepted
decision_scope: PHASE-10.6
updated_at: 2026-10-04
---

# Internal-model Retained-state Security Contract

This contract specifies the accepted Step 01 history/evidence policy, Step 02
integration, and Step 03 output-exclusion proof for
[Phase 10.6](../phase/phase-10.6.md).
Step 01 passed 3 suites / 36 tests and independent review and was committed as
`3b24ec7bb3fc914a6bc782d9fc684e8682e61aaa`. Step 02 passed six suites / 99 tests
and focused review 002 PASS and was accepted at
`f82b5e4dfe5638b5334877d9ac99cef844ebf745`. That historical evidence does not
validate Step 03. Step 03 separately passed seven suites / 106 tests and
independent review and was committed as
`5f7e7f80760a77291f752faa6d549b1d76456c5a`. Final repository validation passed
129 suites / 1032 tests. The distinct validated Phase release is bound by the
[Phase checklist](../phase/phase-10.6-checklist.md).
[Typed control references](internal-model-typed-control-contract.md)
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
history defaults. The pure history policy alone does not prove project-source or
build-output exclusion; Step 03 specifies that separate actual-output boundary below.

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

Historical actor kind is descriptive nonblank attribution, not a closed schema
discriminator. The authenticated adapter separately derives new actor kinds from
trusted SubjectKind; decoding historical attribution does not authenticate it.

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

Step 02 persistence/API and the linked specifications below are accepted under
the historical evidence above. Step 03 actual-output validation and independent
review passed separately as recorded above. There is no public transport or MCP endpoint.

## Exact optional Entity storage

`InternalModelHistoryEntry` extends SimpleEntity and stores one mandatory
`history_document: InternalModelHistoryDocument`; the VALUE wraps ContentBody.
It uses task usage/operation and system application domain. The generated Entity
and its standard codecs are the sole storage owner through Entity/UnitOfWork.
Disabled configuration is `None` and returns Unavailable, with no memory fallback.

`InternalModelHistoryStorageId` is an opaque String admitted as an exact canonical
lowercase UUID. It is independently allocated, never derived from content or
logical references. A selection comprises storageId, reference, packageReference
and scope. Its EntityId bridges the generated collection, StableTimestamp and
the UUID's delimiter-free 32 lowercase hexadecimal digits as entropy. Only the
four fixed canonical UUID separators are removed at this physical-ID bridge;
the canonical hyphenated UUID remains exact in selections, documents and results.
The bridge is injective over admitted UUIDs and issues neither a timestamp nor
a content-derived identity. A different logical reference on the same admitted
storage UUID leaves the physical EntityId unchanged; logical attribution is
checked separately. Every storage call admits the whole selection before access; every
read cross-checks the EntityId, stored selection and record/tombstone attribution.
Callers retain the exact returned selection. No logical-ID uniqueness, head,
latest, search, listing or enumeration contract exists.

Claim/create is immutable: an occupied storageId rejects even for equal content
or a tombstone, without returning the occupied data. New records require a new
explicit storageId. No comparison of document bytes, content or digests controls
claims, revisions, identity or permission.

The closed document state is Retained(record, retainedAt) or Removed(tombstone).
Actor and occurredAt come from authenticated server context; retainedAt is the
server storage time. Retention is 30 days from retainedAt. Before expiry, read
returns Retained; at/after expiry it returns only reference, packageReference,
scope, kind and expiresAt as ExpiryRequired. Reads never mutate.

Explicit operator/admin expire rejects before the boundary and atomically
replaces the entire document with an Expired tombstone at server now at/after it.
Delete replaces it immediately with Deleted. Each uses the loaded snapshot's
exact Entity revision; a conflict fails without retry or overwrite. Removed
returns the existing tombstone unchanged. Missing is explicit; backend failure
is a generic structured storage failure, never Missing or success. Tombstones
retain no actor, evidence or payload. No resurrection or automatic scheduler
exists. Logical-current-record deletion does not promise physical erasure of
old database backups or transaction pages.

Supersession requires two explicit endpoint selections and exactly those two
live retained records. Missing, expired, removed, mismatched reference/package/
scope/kind endpoints reject. The accepted validateSupersession contract owns the
relation. Later endpoint deletion preserves the historical relation and grants
no current-state authority.

## Closed bounded document codec

Top-level keys are exactly format, schemaVersion, selection, content. Format is
`textus.internal-model.retained-history` and numeric schemaVersion is 1.
Selection keys are storageId, reference, packageReference, scope. Content is
`{kind:"retained",record,retainedAt}` or `{kind:"removed",tombstone}`. Record,
tombstone, scope and each payload retain the exact Step 01 field names. Payload
and evidence tokens use exact Scala case names; only the document content tokens
are lowercase. Evidence keys are reference, kind, payload; retained payload is
Identity(value), RedactedText(value, redactionReference), or Omitted(reason).
Reference objects reuse the typed control codec; their layout is not identity.

Input and output are strict UTF-8, at most 1,048,576 bytes before parsing or
persistence. Duplicate keys, unknown/missing/null fields, wrong primitive types,
unknown tokens/version/state, noncanonical/nonpositive/fractional/overflowing
revisions, invalid ISO-8601 Instants and invalid object graphs reject. Exact key
sets apply recursively. Accepted Step 01 limits and validators apply before
encoding and after decoding. Retention timestamps must permit the 30-day
calculation. There is no migration, raw evidence or compatibility fallback.
Invalid-operation diagnostics identify dimensions without echoing rejected data.

## Authenticated runtime-internal continuation API

The trusted embedding server supplies ActionCall.Core, a verified captured
package, an independently selected subject, an exact principalId, admitted
redaction references and optional store. Requests cannot construct grants,
roles, subjects, timestamps, approval or server configuration. The factory
validateVerified-admits the actual capture and binds package/carrier, realization
scope, positive subject revision and every exact subject artifact to it. This
is reference admission, not semantic-completeness or approval proof.

Every call safely validates the trusted ExecutionContext security graph before
using CarReviewAuthorization.roles. SubjectKind must be User, Service or
Subsystem; the nonblank principalId must exactly match the server binding.
Present sessions must be nonexpired and must not have future authenticatedAt.
An already authenticated service principal need not have a session. Anonymous,
Unspecified, null and malformed contexts reject. A default test user, system,
internal or unrelated role alone grants nothing. Denial is a generic structured
securityPermissionDenied before storage or source actions.

| Operation | Admitted roles |
| --- | --- |
| read, resume | viewer, reviewer, operator, admin |
| propose, review, record | reviewer, operator, admin |
| expire, delete | operator, admin |

Propose accepts only Proposal; review only Review; record only Alternative,
Supersession or Evidence. Input contains storageId, logical record reference,
typed payload and at most 64 evidence inputs. Server subject is fixed; actor
kind is the trusted SubjectKind lowercase token, identity is principalId and role
uses priority admin > operator > reviewer > viewer. Server clock supplies time.
All evidence passes the accepted policy before Entity work. Narrative RedactedText
also requires exact redactionReference membership in the server-admitted set.
Wrong kinds, malformed/oversized inputs and package/scope mismatches reject
without side effects. Safe identity is metadata, never authentication.

Resume uses evaluateVerified with the exact server capture and the independently
supplied continuation request. It neither reads nor writes history, including
health probes, and preserves every typed eligibility/problem dimension. Absent,
disabled, unrelated and stale history cannot change its report. Missing independent
decisions or human approval remain incomplete; recorded AcceptedAsReview or
SelectedAsProposal never supplies human approval. There is no filesystem,
provider, execution, cursor mutation or canonical CML write capability.

## Generated protocol boundary and executable integration

The actual implementation Factory create_Core preserves the same six generated
services in order, followed by the framework's unchanged meta and system defaults.
An independent Component.Core.create baseline with the same component identity,
default ComponentInstanceId and Protocol.empty supplies those default service
specifications. CbdRetrieval, CbdCatalogAdmin and CbdReviewAdmin stay unchanged. Copies
of Entity, Aggregate and View specifications remove exactly the new history
Entity's 28 generated operations: 12 Entity, 6 Aggregate and 10 View. The View
inventory includes four generic routes plus three default-generated Summary and
three default-generated Detail routes, even without authored named views. The
six named routes are `loadInternalModelHistoryEntrySummary`,
`searchInternalModelHistoryEntrySummary`, `searchInternalModelHistoryEntrySummaryRecord`,
`loadInternalModelHistoryEntryDetail`, `searchInternalModelHistoryEntryDetail` and
`searchInternalModelHistoryEntryDetailRecord`. These routes must be absent from
the actual constructed protocol listing and service-qualified resolution;
distinct excluded names must also fail unqualified resolution with
`Taxonomy.operationNotFound`. Each original generated owner's full history
inventory must exactly match its frozen exclusions, preventing omitted defaults.

All other operation definitions, service content/metadata/useDefault and service
order remain exact, including framework defaults from the independent baseline.
Catalog operations and unrelated ReviewDiagnosis Entity, Aggregate, generic View,
Summary and Detail operations provide positive controls. Entity descriptors/codecs
remain for internal persistence; task/system classification and mcpReadyServices
alone do not hide generated history routes. Authorized internal history access
continues through the exact-selection, role and retention policy of its adapter.

[Persistence specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryPersistenceSpec.scala)
authors codec variants/strictness, an injective UUID bridge property preserving
canonical selections, independent SQLite reopen, claims, exact selections/revisions,
retention and supersession. [API specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelContinuationApiSpec.scala)
authors the closed role/security matrix, server attribution/evidence admission,
history-independent resume, the independently derived 28-operation generated
inventory, actual factory protocol listing/resolution exclusions with unrelated
Summary/Detail positive controls and unchanged package/cursor/CML source state.
The recorded Step 02 acceptance does not establish whole-Phase acceptance.

## Default consuming-project output exclusion

Step 03 is independently accepted. The non-aggregated
`internalModelExclusionFixture` consumes the repository's normal Cozy plugin,
project-identity helper, default source/resource roots and Scala/Cozy/CNCF
configuration. Its public CML and ordinary resource are siblings of private
`src/main/internal-model` inputs, which include a strict schema 2.0 manifest,
same-basename CML, valid private Scala API, description and explicit private
prompt evidence. Their presence is a negative control, not selection, admission
or permission. Public API, record, operation, resource and publication markers
are positive controls.

| Boundary | Actual owner consumed by the fixture |
| --- | --- |
| Ordinary CML generation | `cozyGenerate` over the scoped default `cozySourceDir`, with generated Scala/model/API metadata |
| Runtime JAR | Normal `Compile / packageBin`, including the ordinary public resource and generated classes |
| Public API | Generated nonempty provided/publicTypes descriptor and actual `cozyComponentApiJar` |
| Documentation and local publication | Actual `Compile / doc` through `cozyScaladocArchive`, and `cozyPublishProject` metadata/page generation |
| CAR and SAR | Actual `cozyBuildCar` runtime/SPI/descriptors and `cozyBuildSar` loader-selected public CML |

At the task boundary, fixture effective Scala, Cozy runtime command and CNCF
dependency must match the root's typed configuration. Both typed configurations
must explicitly disable `publication.source_manifest.enabled`. The public
`project.yaml` declarations are metadata, not operation defaults. Actual
publication operation defaults come from root `conf/cozy/config.yaml` and fixture
`src/test/fixtures/internal-model-output-exclusion/conf/cozy/config.yaml`; both
must be regular, non-symlink files whose existing YAML parser admits the flag as
false before the fixture task DAG runs. This selected default profile has no
higher-priority project-local `.cozy` override. These readiness checks do not
replace final-output exclusion evidence. Public project and declared page
metadata remain required.
Cozy's generic fallback enables source-manifest publication and does not exclude
internal-model in its generic exclusion set; this proof makes no safety claim
for that different profile.

Root `Test / resourceGenerators` depends on the actual fixture production task,
so ordinary `testOnly` and `test` consume that DAG. Root Compile has no fixture
dependency. Fixture publication tasks are skipped; `cozyPublishProject` here
only generates local `target/publish.d` output and distributes no artifact.
The minimal fixture `build.sbt` supplies literal public metadata to that compiler;
the parent subproject retains the generator/packager settings.

X4 consumes the actual final
`target/publish.d/fixture-internal-model-output-control.json` publication bundle,
with schema `cozy.publish-project.v1`, type `publication-bundle` and exact
publication name `fixture-internal-model-output-control`. Nonempty entries must
contain exactly one project key
`projects/fixture-internal-model-output-control/metadata` with path
`metadata/projects/fixture-internal-model-output-control/metadata.json`, and
exactly one page key `publication-pages/fixture-internal-model-output-control`
with path `metadata/publication-pages/fixture-internal-model-output-control.json`.
Their embedded metadata supplies project/page schema, type, public marker and
`public-control` page controls; the staged metadata files are not final-output
locators. Entry paths and metadata types must lack `source-manifest`, and project
publication metadata must have no `sourceManifest` reference. Complete publication
and Scaladoc scans remain independent evidence, including index/search and public
API symbol controls.

The closed `cbdsupport.output-exclusion.v1` Properties receipt contains only
`schema`, `projectRoot`, `mainJar`, `apiJar`, `generatedRoot`, `metadataRoot`,
`scaladocRoot`, `car`, `sar` and `publicationRoot`. Each output locator is an
absolute normalized actual task-returned file or directory under fixture target.
It is neither a PASS stamp nor identity, approval, content snapshot or transit proof.
Missing, extra, duplicate, unsupported-schema, invalid or unavailable fields reject.

The [output-exclusion executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOutputExclusionSpec.scala)
authors X1–X5 over all five actual boundaries, X6 as secondary detector properties,
and X7 as fail-closed evidence admission. Every output name and content is scanned,
including all nested JAR/ZIP/CAR/SAR members without extraction or evidence copies.
Private marker, API/record names and `src/main/internal-model/` paths are forbidden;
benign fixture names and production InternalModel classes are not forbidden.
Maximum archive depth is 8, each uncompressed entry is at most 256 MiB and each
scanned boundary totals at most 2 GiB. Missing, unreadable, corrupt, symlink or
over-limit evidence fails the proof. Every stream, archive and walk is closed.
Small temporary synthetic archives support X6/X7 only and do not replace producers.

Parent execution and independent review passed for Step 03. The sole full
Phase review and focused ABI closure review are recorded in the
[Phase checklist](../phase/phase-10.6-checklist.md).
Phase acceptance is effective only with its distinct validated release binding.
