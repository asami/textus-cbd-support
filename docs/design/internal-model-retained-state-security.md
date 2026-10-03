---
status: accepted
decision_scope: PHASE-10.6
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

Step 02 integrates optional durable history and the authenticated runtime-internal
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
specifications passed six suites / 99 tests and focused review 002 PASS, accepted
at `f82b5e4dfe5638b5334877d9ac99cef844ebf745`. This historical acceptance does not
validate Step 03 by itself. Step 03 separately passed seven suites / 106 tests
and independent review, accepted at
`5f7e7f80760a77291f752faa6d549b1d76456c5a`. Final repository validation passed
129 suites / 1032 tests; the Phase checklist binds its distinct validated release.

## Actual default-output proof

Step 03 is independently accepted. A consuming fixture places distinguishable private
CML, Scala, description and evidence beside ordinary public source/resource roots.
The explicit package manifest records those inputs without authorizing their use.
Source-root separation lets the existing CML loader, generator, API producer and
packagers retain their responsibilities. No new production exclusion filter,
replacement archive or unused runtime helper is introduced.

The actual-owner chain is `cozyGenerate` -> normal `Compile / packageBin` and
generated provided/publicTypes descriptor -> `cozyComponentApiJar`, actual
`Compile / doc` staging and `cozyBuildCar`. `cozyBuildSar` independently consumes
loader-selected public CML. `cozyPublishProject` consumes fixture public identity
and page metadata, plus minimal literal `build.sbt` metadata input, into local
`target/publish.d`. The fixture is not aggregated and both publication skips are
true. This local generation is not artifact publication.

The fixture reuses the normal plugin and typed project-identity helper. Its
effective Scala, Cozy runtime command and CNCF dependency must agree with the
root's typed configuration. Source-manifest publication is explicitly disabled in
both public `project.yaml` declarations and the actual root and fixture
`conf/cozy/config.yaml` operation defaults, reflecting this repository's selected
default. Public metadata is not read as operation defaults by the publication
owner. The preparation task uses the existing YAML parser to require each exact
operational file to be regular, non-symlink and explicitly false before the normal
task DAG runs. The selected profile has no higher-priority project-local `.cozy`
override; these input checks establish readiness, while final output supplies the
independent behavior proof. The generic Cozy enabled
fallback lacks internal-model exclusion; testing this disabled profile cannot
establish a generic upstream guarantee. Public project/page metadata remain
positive controls even with source manifests disabled.

X4 interprets the actual final
`target/publish.d/fixture-internal-model-output-control.json` bundle rather than
staged metadata files. It requires the exact `cozy.publish-project.v1` schema,
`publication-bundle` type, fixture publication name and nonempty entries. Exact
unique project/page keys and paths select their real embedded metadata, preserving
schema/type, public marker and `public-control` page controls. Entry paths and
metadata types exclude source manifests, and the project's publication metadata
has no `sourceManifest` reference. Complete publication scanning and Scaladoc
index/search/public API controls remain necessary.

Root Test resource generation consumes the actual task-returned locators as a
normal prerequisite; root Compile stays independent of the test fixture. A closed
path-only Properties receipt locates complete generated/metadata trees, runtime
and API JARs, Scaladoc, local publication and both final archives. It supplies no
content-derived identity, permission, output replacement or acceptance stamp.

The [output executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelOutputExclusionSpec.scala)
requires present private inputs and public controls across every actual owner.
Names and content are scanned recursively through every nested JAR/ZIP/CAR/SAR,
without extraction or evidence copying. Depth 8, uncompressed entry 256 MiB and
boundary total 2 GiB bound inspection. Invalid locators, missing/unreadable or
symlink evidence, corrupt archives and exceeded limits fail closed. Resource
lifetimes are explicit. Secondary small synthetic ZIP properties and invalid
evidence scenarios exercise the detector only; they cannot replace actual output
evidence. Temporary detector inputs live under target and are removed after use.

The [evidence executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEvidencePolicySpec.scala)
and [history executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryValidatorSpec.scala)
cover the accepted pure boundary. Step 03 execution and independent review passed.
The sole full Phase review, focused ABI closure and P10-60–63 acceptance evidence
are recorded in the [Phase checklist](../phase/phase-10.6-checklist.md).
Phase acceptance is effective only with its distinct validated release binding.
