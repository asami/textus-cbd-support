---
status: authored-unvalidated
decision_scope: P106-STEP-02
updated_at: 2026-10-04
---

# Internal-model Retained-state Security Design

The [retained-state contract](../spec/internal-model-retained-state-security-contract.md)
owns Step 01 fields, policy matrix, finite limits, trust boundary and failure
semantics. This design records the frozen responsibility decision. Source and
executable specification authoring do not establish validation or acceptance.

## Three storage responsibilities

Project packages retain complete selected state and independent resume evidence.
Optional Entity-backed audit history retains richer review, proposal, alternative,
supersession and evidence accounts. Disposable target caches retain derived
outputs. This separation permits selected-state resume when history is absent,
unavailable, different or stale. History cannot become a reverse dependency of
package admission, continuation, action gates or CML application.

Historical artifact references can remain after current inventory removal.
Reusing the existing logical/package/artifact references and ReviewSubject records
their original identities and independent positive revisions without duplicating
CCDM Entity/Event/Workflow meaning. Structural validation deliberately cannot
establish current semantic admission or complete-subject evidence. Actor fields
attribute an account; they do not authenticate it.

## Closed input/output evidence boundary

Separate Scala 3 input and retained-output enums make raw evidence unrepresentable
as a retained payload. Validation precedes policy exclusion so invalid or oversized
data cannot bypass admission by being destined for omission. The independent
retained-value validator also guards downstream constructed values. The policy
preserves exact reference/kind and uses explicit omission reasons rather than
inventing metadata or reconstructing lost evidence.

Raw prompt/response bodies remain excluded from both project-source and history
defaults, even with a purported safe or redacted label. Only bounded safe
provider/model/tool tokens survive as identity metadata. CallTree and external
evidence remain opaque logical references without body access or retrieval grants.
Unavailable is distinct from policy exclusion.

Narrative RedactedText retains exact Unicode and the upstream owner's independent
redaction reference. This is an explicit claim rather than a secret-removal proof;
the Step 02 authorized caller owns admitting that claim. No automatic scrubber,
raw opt-in, content-derived control token, digest or byte-equality protocol is
part of the design. Redaction may destroy evidence, so omission and unavailability
remain first-class values.

## Pure structural admission

Immutable history payloads derive their kind, preventing contradictory stored
tags. Review acceptance and selected alternative dispositions remain review and
proposal facts with no human-approval/cursor transition. Evidence encounter order
survives as documentation, never as selection authority.

Null graphs and every bounded supplied dimension are checked safely before
dependent dereference. Existing opaque-type constructors retain their declared
grammar; logical IDs preserve Unicode and whitespace. Structured invalid-operation
diagnostics identify only dimensions, preventing raw or retained text disclosure.
Independent failures compose applicatively; dependent relations are checked only
after their records have been admitted.

Supersession accepts an explicitly supplied previous/successor pair with matching
non-Supersession kind, package and scope. Different subject revisions preserve
historical change. No endpoint lookup, newest-record inference or global graph is
needed. Tombstones preserve only typed record/package/scope attribution, kind,
retention action and time, avoiding residual body/provider/actor prose after expiry
or deletion. Step 01 defines these values without implementing retention mutation.

## Entity document and authenticated adapter

Step 02 authors optional durable history and the authenticated runtime-internal
adapter. The normative contract owns the exact schema, limits, operation matrix
and retention behavior. The generated InternalModelHistoryEntry/Document codecs
remain the only persistence owner through Entity/UnitOfWork. The domain codec
wraps that generated value rather than reimplementing framework serialization.
Independent storage UUID, logical references and Entity concurrency revision
serve distinct responsibilities. After whole-selection admission, the physical
EntityId bridge uses the generated collection and StableTimestamp with all 32
lowercase UUID hexadecimal digits as entropy, removing only the four fixed
separators. This injective presentation bridge preserves the canonical hyphenated
UUID in selections, documents and results; it issues no timestamp and derives no
identity from content or logical references. Changing a logical reference with
the same UUID leaves the physical ID unchanged, while exact logical selection
is cross-checked separately. Immutable claim rejects occupied identities;
snapshot revision controls one whole-document tombstone replacement without
retry. Minimal tombstones avoid residual actor/body/evidence in the current row.

The 30-day age begins at trusted server storage time. Viewer reads hide due bodies
without writes; explicit authorized maintenance materializes expiry. This models
logical-current-record retention, with no scheduler or physical erasure claim for
backups or transaction pages. Exact supersession endpoints remain historical
relations after later endpoint deletion, never a head-selection mechanism.

The embedding trusted server independently selects subject, capture, principal
and redaction references. Factory admission reuses validateVerified and exact
reference selection. Per-call safe security graph validation precedes role reuse
and any storage work. Actor and time derive from that graph/clock, preventing
request metadata from granting identity or approval. Evidence policy precedes
Entity work, with explicit upstream redaction-reference admission for narrative.

Resume delegates directly to the existing verified action gate and never probes
the optional store. This dependency direction preserves the full typed report
with absent, unavailable, unrelated or stale history and missing independent
decision/approval inputs. The adapter has no source/provider/execution/CML-write
capability and registers no transport endpoint.

The actual Factory protocol assembly retains six generated services in their
original order, followed by the framework's unchanged meta and system defaults.
It copies only generated Entity, Aggregate and View service specifications,
excluding the history Entity's exact 28 operations: 12 Entity, 6 Aggregate and
10 View. Four View routes are generic; six come from Summary and Detail views
generated by default even without authored named views. Those six routes are
`loadInternalModelHistoryEntrySummary`, `searchInternalModelHistoryEntrySummary`,
`searchInternalModelHistoryEntrySummaryRecord`, `loadInternalModelHistoryEntryDetail`,
`searchInternalModelHistoryEntryDetail` and `searchInternalModelHistoryEntryDetailRecord`.
They can otherwise expose history_document without the internal adapter's exact
selection, role or expiry policy, so the same Factory boundary excludes them.
The authorized internal adapter and Entity persistence remain unchanged.

All other exact operation definitions, service content/metadata/useDefault,
service order and Entity descriptors remain. An independent Component.Core.create
baseline using the same component identity, default ComponentInstanceId and
Protocol.empty supplies exact default-service specifications, including their
operations and metadata. The authored API specification independently derives
history operation names from each original generated owner using the
InternalModelHistoryEntry token and requires exact equality with its frozen
inventory. It checks all 28 against actual constructed protocol listing and
service-qualified resolution, plus unqualified resolution for each distinct
excluded name; missing routes require Taxonomy.operationNotFound. Exact preserved
definitions and metadata, catalog operations and unrelated ReviewDiagnosis
Entity/Aggregate/generic View/Summary/Detail operations are positive controls.
This specifies the generated history bypass correction at its consumer;
task/system features or mcpReadyServices are not substitutes for that boundary.

Step 01's 3 suites / 36 tests, independent PASS and accepted commit
`3b24ec7bb3fc914a6bc782d9fc684e8682e61aaa` are historical evidence. Step 02 and
its [persistence](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryPersistenceSpec.scala)
and [API](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelContinuationApiSpec.scala)
specifications are authored-unvalidated; parent generation/validation and fresh
independent review remain pending.

## Deferred integration owned by subsequent Steps

Step 03 exclusion proof is **NOT YET IMPLEMENTED**. P10-63 needs distinguishable
sentinels and positive controls exercised through actual generation/publication/
packaging owners for runtime JARs, public APIs, ordinary CML, documentation and
CAR/SAR artifacts. Step 01 defines the pure policy without installing unused
packaging filters or changing upstream owners.

The [evidence executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEvidencePolicySpec.scala)
and [history executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryValidatorSpec.scala)
cover the accepted pure boundary. Step 02 validation and independent review remain
pending; remaining P10-60–63 acceptance evidence, checklist closure and
[Phase 10.6 acceptance](../phase/phase-10.6-checklist.md) remain outstanding.
