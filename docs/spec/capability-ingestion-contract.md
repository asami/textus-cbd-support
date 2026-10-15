# Capability Ingestion Consumer Contract

Status: CAP-11-PKG01 accepted after focused validation and independent whole-Step conformance; Phase acceptance pending.
Scope: PHASE-11 / CAP-11-01 baseline + CAP-11-PKG01 package acquisition amendment

## Current Step acceptance — 2026-10-15

The current Component-owned Usage implementation and real Directory/normal CAR
consumer query path are accepted after focused validation and independent
whole-Step conformance; see
[CAP-11-PKG01 acceptance](../phase/phase-11.md#cap-11-pkg01-acceptance--2026-10-15).
This acceptance supersedes earlier pending/authored-only annotations only for
CAP-11-PKG01. The existing normative text and historical records below remain
preserved. Participant own PKG01/Phase acceptance, full validation, reviews and
releases, CBD full Phase review/release and authorized Cozy main integration
remain pending. CBD aggregate full-suite validation remains deferred-not-run to
Phase 11.1; no successor acceptance is established.

## CAP-11-PKG01 current authored contract — 2026-10-15

`Selection(scope, packages, useCase)` and `Provenance(scope, packages, useCase)`
have no loose family input. Each `PackageExpectation(input, resources, usage)`
selects `usage: Option[ComponentUsageExpectation]`; Some names exact producer,
schema/ABI and ordered document Sources, while None selects actual absence.
`ComponentFactory.acquireUsageCapabilities(input)` supplies the original optional
CNCF acquired witness for every selected package. CBD consumes its content,
selected input and canonical `meta/component-usage-capability/usage.json` path;
it does no file/archive/process acquisition. CNCF retains transport/descriptor
and resource guarantees and its original Left Diagnostic is propagated unchanged.
Absent + None retains an Unavailable package family; absent + Some is Unavailable;
present + None is Incompatible. Present malformed or semantically invalid data
retains its actual failure instead of being converted into optional absence.

`ComponentUsageProjection` has exactly schemaVersion, generatedAbiVersion,
producer, component coordinate, package coordinate and ordered documents. Its ABI
is `cozy.component-usage-capability.v1`, schema is 1, document collection is
`componentUsageCapabilities`, and every `ComponentUsageDefinition` has the explicit
canonical ComponentId `component` plus kind `COMPONENT_USAGE` and all original
identity/version, Presence, source and edge fields. The definition owner equals
the envelope owner, and envelope component/package equal the selected input.
Document/header, definition, edge, producer, release, package and contract revisions
remain independent. Local references bind against the complete selected Component
and Usage candidate set; exact competing candidates remain Ambiguous and External
evidence remains attributed. Old Application capability APIs/wire/readers are
removed without aliases or conversion; unrelated Application Model/Tier remains.

For nonempty expected Component resources, original public native discovery and
the original provider/Discovered objects remain mandatory. For empty resources,
`ComponentCapabilityPackageLoader.load` must establish an empty index/projection
selection. `ComponentPackage.MetadataOnly(acquired)` preserves that original
witness without calling a provider or fabricating native metadata;
`ComponentPackage.Native(discovered)` preserves native discovery. Candidate and
Snapshot retain ordered `componentPackages` and a once-derived native-only
`components` vector. Their ordered `usage: Vector[ComponentUsagePackage]` retains
input, canonical resourcePath and Available/Unavailable family once per package.
Immutable attempt/current/history semantics and original failures remain intact.

The real schema-2 requests explicitly select Store's One/Two, UsageOnly's
Independent and secondary Remote Usage from owner-partitioned normal generation.
UsageOnly has resources=[] and no Scala output/provider; Archive selects no Usage.
First/Second retain independent headers. A fifth primary Contracts production
uses header:primary-r46 and empty Usage selections, with the original native
contracts/contexts and StoreContractsMetadata/ArchiveContractsMetadata accessors.
All eight normal generated accessors are compiled through the configured Test
graph. The source-free intake copies full canonical Directory images only.
Normal package-car packages the same First Store/Remote images and optional bytes;
raw codec inspection uses CNCF's acquired witness. Reading an image starts no
producer, CAR build or publication.

Authored executable obligations are
[C11A01–11](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityComponentUsageProjectionCodecSpec.scala),
[C11K01–11](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityIngestionSpec.scala),
[C11S01–07](../../src/test-current-cncf/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityIngestionStateSpec.scala)
and [C11R01–08](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityProducerIntegrationSpec.scala).
Actual Given/When/Then surround operations and assertions; all existing active
properties retain at least 100 successful applicable examples and zero discards.
These authored changes await parent validation, fresh independent Step review
and separate participant/Phase acceptance. They claim no new executed result,
release, main merge or positive forward UseCase integration.

## Historical baseline and authoring record

The remaining sections preserve the original CAP-11-01/02/03 designs, dated
authoring decisions and acceptance history. Old Application descriptions, old
module signatures and earlier suite/case counts describe those prior boundaries;
they are superseded for current implementation by CAP-11-PKG01 above and do not
claim acceptance of changed current inputs or provide compatibility behavior.

This contract records the frozen consumer decisions C11I-D01–16. The
[design](../design/capability-ingestion.md) explains their structure. Progress
and acceptance belong to the [Phase 11 checklist](../phase/phase-11-checklist.md).
Package-private ingestion, complete separate Application input, current/history
semantics and real normal Cozy -> generated Scala 3 native -> public Directory/CAR
Factory -> source-free CBD interoperability are accepted after focused validation,
independent whole-Step review and mechanical naming closure; see
[CAP-11-01 acceptance](../phase/phase-11.md#cap-11-01-acceptance--2026-10-15).

The original S01 source/fixture inventory and planned executable closure assertions,
and dated S02/S03 authoring sections, retain their historical pending/unchecked
states at their 2026-10-14 authoring point. Current acceptance supersedes those
states only for primary CAP-11-01. Participant acceptance/Phase closures, CAP-11-02/03
and CBD Phase closure remain outstanding. Forward UseCase demand stays
not-yet-admitted, with its positive path unexercised; full-suite validation remains
deferred-not-run to Phase 11.1 and Phase 14 failed Job behavior remains unaccepted.

## Package acquisition amendment — 2026-10-15 (C11I-D17)

Status: corrected target specification; CAP-11-PKG01 implementation pending.
The [Component Usage adoption contract](component-usage-capability-adoption.md)
supersedes the Application family, its standalone JSON handoff and the withdrawn
Application-owned package proposal. `ComponentUsageCapability` is the inductive
usage model of the reviewed Component; `ComponentCapability` remains the separate
deductive provided/required contract model. Neither family name denotes an
Application Model/Tier. The new contract owns the source/wire/typed API migration.

Acquire `meta/component-usage-capability/index.json` and its `usage.json` sibling
from the reviewed Component's Directory/CAR through CNCF. The index is
`{"schemaVersion":1,"projections":["usage.json"]}`; payload ABI is
`cozy.component-usage-capability.v1`. Preserve explicit Component/package ownership,
source, derivation evidence and attributed many-to-many mappings. No wholesale
copy of a multi-Component model into every package, no Application-only owner.

The generated Directory is the normal development input before CAR creation.
The same image is packaged for distribution. Missing optional family is unavailable;
present but invalid/unreadable data retains the upstream failure. No loose JSON
fallback, archive reader, process-based file read or duplicate resource validation
is added in CBD. Component/native discovery retains its independent requirements.

This pre-release change removes old Application types/APIs and readers. Do not
keep aliases or forwarders: stale typed callers must fail compilation. Update
source/fixtures/JSON/consumer codecs together; old serialized data is not silently
reinterpreted. Prior accepted Steps remain historical evidence only for their
original boundary. The original implementation descriptions below are retained
as baseline, with changed naming/ownership/wire/query requirements superseded
by the adoption contract. See the [handoff](../journal/2026/10/2026-10-15-capability-package-metadata-handoff.md).

## Authority and failure model — C11I-D01

CBD owns a package-private read-only ingestion and query model for CAP-11-01–03.
CNCF owns Component admission, native binding and public discovery. Cozy owns
source normalization, generation and the separate Component Usage format. Capability
is a non-instantiated contract; references and metadata observations do not
execute it. Existing CatalogRuntime cache, Job/Review programs, CAR Review
capability taxonomy and Phase 10 durable approval retain their separate owners.

The execution model is local explicit single-writer value transitions. Its
declared failures are malformed, duplicate, ambiguous, incompatible and stale
inputs, plus unavailable or unauthorized acquisition. No network, new cache/TTL,
concurrency arbitration, CAS, retry, recovery, replay, byte seal, activation or
durable-store mechanism is introduced. Apply the
[repository rules](../rules/repository-rules.md) and existing
[typed control contract](internal-model-typed-control-contract.md).

## Supported inputs and independent versions — C11I-D02

| Family / carrier | Supported contract | Input status and authority |
| --- | --- | --- |
| Component projection | Integer `schemaVersion=1`; exact `generatedAbiVersion=cncf.component-capability.v1` | Original CNCF public Discovery result only. |
| Canonical package index | Integer `schemaVersion=1`; exact keys `schemaVersion,projections` | `meta/component-capability/index.json`; ordered flat sibling JSON resource names; existing schema-3 canonical CAR descriptor. |
| Component Usage projection | Integer `schemaVersion=1`; exact `generatedAbiVersion=cozy.component-usage-capability.v1` | C11I-D17: the reviewed Component’s Usage metadata from its Directory/CAR through CNCF, or explicit unavailable/rejected acquisition; implementation pending. |
| Forward Use Case demand | No admitted producer or schema | `UnavailableUpstreamProjection`, reason `not-yet-admitted`; positive path unexercised. |

Producer identity/version must equal the independently observed caller selection.
Projection/schema, Capability contractVersion, Component release, package
coordinate/version, sourceRevision, target contractVersion/ABI, CNCF library
version and CBD adapter/local record revisions are separate domains. Opaque
strings have no inferred chronology, latest value, semantic version range or
cross-domain alias. CAR ABI integer 1 and library versions are not Capability
projection ABI values.

## Actual upstream sources and fixture availability — C11I-D14

Historical source/fixture inventory as at 2026-10-14: pending assertions below
are superseded for primary CAP-11-01 by the current acceptance above. Exact source
revisions and participant closure obligations are retained.

The selected dedicated checkouts retain the original participant authority.
CNCF CAP-78-01–04 is accepted at HEAD
`167a70ee6b96dc0b8ec3099b2b2a605c8b1bbba9`. Cozy CAP-64-01–04 is accepted at
HEAD `60eca52f2e38de5dc373d30e10e2b0c50084c9ce`; additional uncommitted
CAP-64-05 producer/publication authoring and its bounded repair conformance are
accepted prerequisites, not whole-Step acceptance.

| Repository input | Actual source / symbol | What is available / pending |
| --- | --- | --- |
| CNCF semantic and public contract | `docs/spec/component-capability.md` R11/R12/R13/R14; `docs/design/component-capability.md`; `docs/phase/phase-78-checklist.md` | Closed wire, admission, actual target adapter and static package/public discovery contracts accepted; CAP-78-05 real interoperability pending. |
| CNCF typed transport | `component/capability/ComponentCapabilityProjection.scala`, `ComponentCapabilityReference.scala`, `ComponentCapabilityAdmissionScope.scala`, `ComponentCapabilityProjectionValidationPart.scala` under `src/main/scala/org/goldenport/cncf` | Original qualified references, complete records and Diagnostic domains; scalar/source validity authoritative. |
| CNCF acquisition | `ComponentCapabilityPackageSource.scala`, `ComponentCapabilityMetadataProvider.scala`, `ComponentCapabilityDiscovery.scala`; public `ComponentFactory.discoverCapabilities` | Explicit Directory/CAR input, public NativeInput and original Discovered/Entry/ResolvedTarget readout. |
| Cozy contract and model | `docs/spec/cml-capability-model.md` R13; `docs/design/cml-capability-model.md`; `docs/phase/phase-64-checklist.md`; `src/main/scala/cozy/modeler/CmlCapabilityModel.scala` | Full separate Application and Component families; CAP-64-05 remains IN_PROGRESS with four unchecked criteria. |
| Cozy export | `CmlCapabilityApplicationProjection.encode`, `CmlCapabilityProjectionReferenceJsonPart`, `CmlCapabilityProjectionJsonPart`, `CmlCapabilityGenerationScala` under `src/main/scala/cozy/modeler` | Lossless Application JSON, Component R11 JSON and pure generated `projectionJson` accessor authoring; actual Scala 3/native consumer proof pending. |
| Cozy normal producer | Normal `modeler-scala` / `modeler-scala-value` opt-in; `CmlCapabilityPackagePublication` | Canonical package metadata plus separate Application product input authored; native facts are independently supplied, never extracted by CBD. |
| Canonical real CML fixtures | `src/test/resources/modeler/capability-model.cml`, `src/test/resources/modeler/capability-model-secondary.cml` in Cozy | Real parsed/generated source inputs retain Application definitions, Unicode/delimiters, Presence, grounding, edge provenance, provider/direction keys and Operation/Workflow/StateMachine references. Native compilation/Factory/CBD roundtrip pending. |
| Build domains | Cozy `org.simplemodeling.cozy.BuildInfo.version`, selected build `0.3.3-SNAPSHOT` / Scala `2.12.18`; CNCF `0.5.3-SNAPSHOT` / Scala `3.3.8` | BuildInfo supplies actual producer version. These build versions do not supply source or projection revisions. |
| CBD build binding | [build.sbt](../../build.sbt), [project.yaml](../../project.yaml) | Existing explicit source-root mode binds actual admitted CNCF checkout; S02 kernel/specifications authored, source-mode focused validation and normal producer proof pending. |

Authored definition/edge source revisions in the fixtures remain their own
values. Package header, acquisition selection and native target revisions must
be independently supplied and observed by the later real path. This inventory
does not invent missing revisions or native objects. A handcrafted public-client
projection, synthetic negative fixture, dependency version or private review
report is not a real admitted producer fixture. Canonical documents retain
compact contract/status authority without command logs or ignored evidence links.

## Closed explicit selection and public intake — C11I-D03, C11I-D04

The consuming facade receives one explicit complete ordered vector of Selected
package inputs paired with public ComponentCapabilityMetadataProvider values,
and caller-selected scope/provenance. It calls public
`ComponentFactory.discoverCapabilities` for each package, consuming only
successful `ComponentCapabilityDiscovery.Discovered` entries. It cannot construct
Admitted, call private[cncf] adapters, reflect providers, parse CML, substitute a
package, activate a Component or invoke a procedure.

These are required typed selection roles, not a new public JSON schema. No role
has an inferred default or arbitrary field bag.

| Selection role | Complete explicitly supplied basis |
| --- | --- |
| Snapshot management reference | Exact `InternalModelRecordReference` in the snapshot role. |
| Attempt management reference | Exact `InternalModelRecordReference` in the attempt role. |
| Queried/admitted scope | Literal scopeId and original explicit finite scope/provenance. Every package belongs to that scope. |
| Each package | Original `Selected(scopeId,location,component,packageCoordinate)`; public provider; exact independently expected ordered resources. |
| Component coordinate | Canonical namespace/local-ID ComponentId plus separate release version. |
| Package coordinate | Literal coordinate plus independently supplied package version. |
| Each resource expectation | Literal canonical resource path; producer id/version; schema/ABI; full `Source(modelAuthority,path,sourceRevision,line)` and coordinate context. |
| Acquisition provenance | Selected Directory root or CAR location, retained separately from model Source. |
| Component Usage family selection | C11I-D17: reviewed Component/package/location and canonical Usage resource, expected producer/schema/ABI and ordered source headers; acquired through CNCF or explicit unavailable/rejected branch. |
| Use Case family | Explicit not-yet-admitted unavailable projection; no fabricated forward demand records. |

Compare required expected declarations with actual discovered data before
selecting any successful candidate. Identity, coordinates, scope and producer
mismatches are Incompatible; unsupported schema/ABI is Unsupported; missing
required declarations are Malformed/incomplete; opaque source revision mismatch
is Stale. Retain supplied lines without offset guesses. Model identities do not
come from filenames/order; revisions do not come from mtime or content.

Duplicate full selected package/context or projection snapshot keys are
Ambiguous even for equal data. The upstream resource snapshot key retains
ComponentId plus model authority/path/revision, excluding optional line.
Per-package Discovery uniqueness remains authoritative. Distinct Components may
observe the same exact Capability definition: retain every context/provenance
witness without flattening or display-name deduplication.

Any package/native failure rejects the whole requested candidate before state
selection. Preserve original Diagnostic kind, detail, scopeId, component, source
and location, including nested resource/native pointers. Partial successes are
not a selected snapshot. Missing package/index/provider is Unavailable rather
than empty; upstream malformed supplied carriers retain their Malformed result.
Only a successfully acquired empty index with successful native acquisition and
Ready scope is admitted empty.

## Complete separate Application wire — C11I-D05

Every object below has exactly its listed mandatory keys. Arrays retain all
entries in supplied order, including empty/app-only documents. `text` has the
literal validity below. Component wire remains the upstream R11 contract and
is consumed through Discovery; Application records never enter its envelope.

| Application carrier | Exact keys and value domains |
| --- | --- |
| Projection | `schemaVersion: integer 1`, `generatedAbiVersion: cozy.application-capability.v1`, `producer: Producer`, `documents: array[Document]` |
| Document | `source: Source`, `applicationCapabilities: array[ApplicationDefinition]` |
| ApplicationDefinition | `kind: APPLICATION`, `identity: ModelElementId`, `contractVersion: text`, `name: text`, `summary: Presence[text]`, `description: Presence[text]`, `source: Source`, `demandReferences: Presence[array[ModelLink]]`, `discoveryReferences: Presence[array[ModelLink]]`, `componentMappings: Presence[array[ComponentMapping]]`, `dependencies: Presence[array[ApplicationDependency]]`, `grounding: Presence[array[Grounding]]`, `traceability: Presence[array[Traceability]]` |
| ComponentMapping | `target: ComponentCapabilityReference`, `boundary: LOCAL \| EXTERNAL`, `source: Source`, `origin: Origin` |
| ApplicationDependency | `target: ApplicationCapabilityReference`, `boundary: LOCAL \| EXTERNAL`, `source: Source`, `origin: Origin` |
| ComponentCapabilityReference | `kind: COMPONENT`, `identity: ModelElementId`, `contractVersion: text` |
| ApplicationCapabilityReference | `kind: APPLICATION`, `identity: ModelElementId`, `contractVersion: text` |

The following common records mirror actual Cozy/CNCF projection helpers exactly.
No omitted Presence, renamed key, reduced list or label-derived reference is valid.

| Common carrier / branch | Exact keys and value domains |
| --- | --- |
| Producer | `id: text`, `version: text` |
| ModelElementId | `modelId: text`, `elementId: text` |
| TermId | `vocabularyId: text`, `termId: text` |
| RelationId | `vocabularyId: text`, `relationId: text` |
| ProfileId | `vocabularyId: text`, `profileId: text` |
| Source | `modelAuthority: text`, `path: canonical project-relative POSIX path`, `sourceRevision: opaque text`, `line: null or positive integer` |
| Presence / Present | `state: PRESENT`, `value: A` |
| Presence / Absent | `state: ABSENT`, `reason: NotDeclared \| NotRepresented \| Unsupported \| NotApplicable`, `detail: text` |
| Origin / Declared | `kind: DECLARED` |
| Origin / Derived | `kind: DERIVED`, `ruleId: text`, `inputs: nonempty ordered array[Source]` |
| ModelReference | `identity: ModelElementId`, `boundary: LOCAL \| EXTERNAL` |
| SemanticTarget / Model | `kind: MODEL`, `identity: ModelElementId` |
| SemanticTarget / Term | `kind: TERM`, `identity: TermId` |
| LocalizedLabel | `text: text`, `language: null or text` |
| ModelLink | `role: GOAL \| USE_CASE \| SCENARIO \| USE_CASE_SLICE \| COMPONENT \| SPECIFICATION \| EVIDENCE`, `target: ModelReference`, `source: Source`, `origin: Origin` |
| Traceability | `role: SPECIFICATION \| EVIDENCE`, `target: ModelReference`, `source: Source`, `origin: Origin` |
| Grounding | `target: SemanticTarget`, `boundary: LOCAL \| EXTERNAL`, `relation: Presence[RelationId]`, `profile: Presence[ProfileId]`, `context: Presence[text]`, `preferredLabel: Presence[LocalizedLabel]`, `source: Source`, `origin: Origin` |

Only Source.line and LocalizedLabel.language allow explicit null. Present(empty)
is distinct from every Absent reason/detail. Application demand roles retain the
producer's Goal/UseCase/Scenario/UseCaseSlice restriction; discovery retains all
seven roles. Qualified vocabulary domains remain distinct from model identities.

## Identity, source and finite-scope integrity — C11I-D06

Use strict duplicate-key-rejecting JSON before map conversion. Text is nonnull,
nonempty, unpadded at either edge by Character.isWhitespace or
Character.isSpaceChar, and free of ISO controls. Preserve exact case, Unicode
composition and delimiters. Paths have no leading slash, drive/URI scheme,
backslash, empty segment, dot or dot-dot segment or ISO controls. Do not normalize
invalid paths. Supplied line is a positive integer; absent line is explicit null.

Malformed syntax, duplicate JSON keys, missing/extra keys, null mandatory values,
wrong scalar/array types or mixed branch keys are Malformed. Unknown well-formed
closed tags or unsupported schema/ABI are Unsupported. Check header schema/ABI
before decoding nested v1 fields; boolean/fractional schema remains Malformed.

Document header key is exact authority/path/revision, excluding line. Duplicate
headers and exact Application identity/contract keys are Ambiguous. Each nested
Application definition source belongs to its enclosing document authority/path;
its independent authored revision and line are retained. Definition modelId equals
source.modelAuthority. Owned edge sources use the enclosing definition authority;
ordered Derived inputs can retain valid independently supplied foreign sources.
Header and definition/edge revisions are not aliases.

Resolve Local references only in the explicitly admitted finite scope plus
selected Application/Component definitions, using full identity, kind and version
where versioned. Missing Local target is Malformed, wrong kind/version is
Incompatible, multiple exact candidates are Ambiguous. Model roles, traceability
roles and model/term/relation/profile domains bind separately. Independent
Component provenance witnesses remain retained; duplicate exact candidates in a
reference's selected resolution context cannot be resolved by choosing a winner.
External references retain their explicit boundary and unresolved-external
evidence; missing facts outside selected scope do not prove dangling references.
Cozy owns normalization, cycle and procedure semantics. CBD adds no CML parser,
second generator validator or invented global cycle guarantee.

## Demand direction and optional availability — C11I-D07

Component mappings are enclosing versioned Application -> versioned Component
Capability edges with original boundary/source/origin. Dependencies are logical
Application requirements and imply no execution order. Application demand and
discovery links point from Application to authored model targets. Neither these
links nor Component discovery establishes the inverse UseCase -> requiredApplication
relationship. No selected upstream forward Use Case projection exists.

`traceUseCaseDemand` must return UnavailableUpstreamProjection with reason
not-yet-admitted and identify the unexercised positive path. Absence is not
Present(empty), NoProvider or evidence of no demand. Explicit unavailable
Application acquisition retains valid independently admitted Component data and
marks that family unavailable. Acquired invalid, stale or unauthorized Application
data rejects the whole selected candidate; it is not optional absence.

## Typed control and current/history transition — C11I-D08, C11I-D09

Use existing InternalModelRecordReference, InternalModelRecordId and
InternalModelRecordRevision factories for exact attempt and snapshot roles,
with distinct role wrappers if needed. Producer supplies positive revisions and
exact identities. No implicit counter, UUID policy, content token, latest-record
selection or approval is introduced. Opaque upstream Source revisions stay in
Source and never become local record revisions. Construction records declared
provenance, not authenticity, permission, source-currentness or undeclared-change
detection.

The pure domain transition takes explicit prior immutable ingestion state and
one explicitly referenced Attempt with selected provenance. Accepted snapshots
and attempts remain available by their exact references.

| Input / transition | Typed outcome | Current and history effect |
| --- | --- | --- |
| All Component packages successful; Application acquired valid or explicitly unavailable; Use Case unavailable | `Accepted(snapshotReference)` | Append Attempt and successful snapshot; select that new current snapshot with exact scope/provenance and family availability. |
| Successfully acquired empty Component index with Ready/acquired native data | Accepted scoped empty, subject to all other family checks | Same successful transition; no fabricated empty fallback. |
| Any original rejected package/native acquisition | `Rejected(original Diagnostic)` | Append rejected Attempt with selected provenance; preserve previous current pointer, accepted snapshot and prior history. |
| Malformed shape, incomplete required declaration, duplicate JSON key or dangling Local target | Rejected Malformed | Same rejected transition; expose no partial snapshot. |
| Well-formed unsupported schema/ABI/tag | Rejected Unsupported | Same rejected transition. |
| Duplicate full selected context/snapshot/declaration or ambiguous exact target | Rejected Ambiguous | Same rejected transition, including equal duplicate data. |
| Identity/coordinate/scope/producer or Local kind/version mismatch | Rejected Incompatible | Same rejected transition; no name repair. |
| Expected opaque sourceRevision differs | Rejected Stale | Same rejected transition; never relabel retained old data as fresh. |
| Missing acquisition/package/index/provider or explicit unavailable required source | Rejected Unavailable | Same rejected transition; no empty/NoProvider conversion. |
| Unauthorized acquisition, including acquired Application rejection | Rejected Unauthorized | Same rejected transition; no downgrade to absent optional input. |
| Historical query | Exact selected accepted snapshot reference required | Read that snapshot explicitly; no newest attempt/winner search. |
| Current query | Exact declared snapshot, scope and provenance required | Changed expected versions or original Stale/Unavailable/Unauthorized observation yields that typed result, without freshness fallback to retained history. |

Original diagnostics retain kind/detail/scope/component/source/location rather
than being collapsed into these summary rows. This is a logical read model and
value transition, not filesystem persistence, replay or cache behavior.

## Catalog fact families — C11I-D10

Successful ingestion retains ordered original admitted Component entries,
definitions/declarations and matched native realization observations; complete
Component/package/source/ABI metadata; independent Application documents; and
the explicit unavailable Use Case family. CAP-11-02 derives CapabilityRecord,
CapabilityRealizationRef and CapabilityDemandTrace views retaining declaration
keys, context witnesses and edge-specific provenance.

Grounding, Presence, Specification links and Evidence links retain independent
meaning and original order. Native Operation/Workflow/StateMachine objects remain
original metadata observations; their procedures do not execute or become a new
portable contract. Later portable catalog codecs roundtrip semantic references
and provenance, not executable native objects.

## Exact provider and path query handoff — C11I-D11, C11I-D12

CAP-11-03 retains six domain operations: getCapability, findProviders,
findRequirements, traceApplicationCapability, traceUseCaseDemand and
diagnoseCapabilityDemand. Inputs use typed qualified identity/version and explicit
admitted snapshot/query scope. Missing required version or snapshot is incomplete
input, never latest-version selection. Search text is presentation assistance.

Provider matching requires exact qualified Capability identity and equal
contractVersion or one explicit admitted directed (requiredVersion,providedVersion)
Compatibility edge for that identity. Preserve edge source/origin and catalog
context. Do not reverse, transitively close, parse semver or match labels. Return
all matching Provided declarations/Components in explicit deterministic catalog
order. A Component can provide and require one Capability; no provider wins by
priority. NoProvider means none in a successfully acquired/admitted complete
queried scope, not universal nonexistence or acquisition failure.

Each path retains enclosing versioned Application and mapping edge -> exact
matching Required declaration -> compatible Provided declaration -> that Provided
declaration's own realization edges. Retain every enclosing key and edge's
source/origin and context; separate lists cannot form Cartesian products.
Unsupported, Unavailable, Unauthorized, Stale, Ambiguous, Incompatible and
Malformed stay distinct from successful empty vectors, unresolved external
references and typed gaps.

## Gap and public presentation handoff — C11I-D13

| Later typed gap | Fact boundary to retain |
| --- | --- |
| MissingApplicationCapabilityProjection | Explicit missing Application projection; retain family availability rather than asserting missing definitions from failed acquisition. |
| MissingRequiredComponentCapability | Selected Application mapping and required declaration boundary. |
| NoProvider | Complete admitted queried scope and exact requirement key. |
| IncompatibleProviderVersion | Exact demand/requirement/provider identities and independently supplied versions. |
| MissingRealization | Compatible provider remains present; retain its declaration key and realization boundary. |
| MissingSpecification | Separate Specification edge/coverage absence on the applicable path. |
| MissingEvidence | Separate Evidence edge/coverage absence on the applicable path. |

Attach applicable demand, required, provider and realization path keys and
provenance. Unsupported/unavailable/unauthorized/stale/ambiguous acquisition
cannot prove a gap over missing facts. Specification/Evidence gaps are separate
from execution success. Runtime Availability, Authorization and Guard are
independent observations, unknown unless explicitly supplied.

Phase 11.1 Web/MCP must map the same typed outcomes, identity/provenance and
distinct unavailable/authorization semantics, without manufactured trace links
or new authorization policy. Public surfaces are outside this Phase's code scope.

## Settled implementation modules — C11I-D16

The authored package-private runtime modules are CapabilityIngestionModel (typed
selection, family, snapshot, attempt and outcomes), CapabilityApplicationProjectionCodec
(strict full Application transport and selected finite-scope integrity) and
CapabilityIngestion (public CNCF acquisition facade and pure full-candidate
transition). CAP-11-02/03 plan separate catalog/query modules against this handoff;
they remain unstarted. Existing CatalogRuntime, service CML, CarReviewCapabilityCatalog,
Job, durable approval and upstream programs require no edits for this boundary.

Existing `TEXTUS_CBD_SUPPORT_CNCF_SOURCE_ROOT` / `textus.cbd.cncfSourceRoot`
source mode binds the admitted CNCF checkout. It requires no publishLocal,
shared warehouse write or dependency version substitution.

## Planned executable behavior and closure — C11I-D15

Historical S01/S02 authoring status as at 2026-10-14: the pending and unchecked
assertions below are superseded only for current primary CAP-11-01 acceptance.

These remain complete-Step obligations, including actual producer coverage.
S02 authors the bounded kernel cases below; their validation remains pending. Meaningful
AnyWordSpec/GivenWhenThen/should scenarios and applicable active properties with
at least 100 successful samples and zero discards are required at behavior authoring.

| Case | Future observable obligation |
| --- | --- |
| C11I01 | Actual Cozy canonical Component package through public Factory to CBD, exact source/version metadata and all three original native realization kinds; CBD reads no CML. |
| C11I02 | Complete actual Application fields, Presence and edge provenance survive semantic portable roundtrip with no inverse Use Case links. |
| C11I03 | Opaque revision mismatch is Stale and preserves accepted current state. |
| C11I04 | Unsupported schema/ABI is distinct from malformed shape and duplicate JSON keys. |
| C11I05 | Duplicate full context/declaration and exact Local ambiguity reject with no winner. |
| C11I06 | Coordinate/scope/producer/Local kind/version mismatch is Incompatible without name repair. |
| C11I07 | Original Unavailable/Unauthorized/Stale preserves complete Diagnostic without empty/NoProvider. |
| C11I08 | Ready/acquired empty index yields true scoped empty. |
| C11I09 | Rejected attempts preserve accepted history; current/historical reads require explicit matching selection. |
| C11I10 | Unavailable forward Use Case demand coexists with retained Application-to-Component mapping. |
| C11I11 | Explicit unavailable Application differs from acquired invalid/unauthorized failure; valid Component survives only the former. |
| C11I12 | Unicode, delimiters, case, order, Presence and origins survive semantic reference roundtrip without encoded control identity. |

All seven CAP-11-01 criteria remain unchecked; CAP-11-02/03 remain OPEN and
unstarted. All four Cozy CAP-64-05 criteria remain open. Actual producer ->
Scala 3/native CNCF Factory -> CBD no-reparse consumption is required before
CAP-11-01 and participant CAP-64-05 acceptance. Separate complete Step acceptance
and commit, participant Phase full validation/review/local releases, authorized
Cozy main integration and CBD Phase 11's own review/release remain owed.
CBD repository full-suite validation is `deferred-not-run` to Phase 11.1;
this Slice executes no successor work.

## S02 ingestion kernel authoring — 2026-10-14

Kernel authoring is IN_PROGRESS at the Step boundary. The nine private runtime
sources implement the frozen C11I-D01–16 intake handoff without starting the
CAP-11-02 catalog or CAP-11-03 query operations. Parent-owned focused validation
and independent review remain pending; authoring records no executed results.

| Source | Authored responsibility |
| --- | --- |
| CapabilityIngestionModel | Distinct AttemptReference/SnapshotReference factories delegate to existing local RecordReference domains; complete selection, provenance, Application, availability, external evidence, candidate and immutable history carriers. |
| CapabilityApplicationProjectionCodec | decode(String), encode(ApplicationProjection), admit(ApplicationProjection, Scope, Vector[CapabilityDefinition]); strict duplicate-key parser and one structural decoder for supplied-carrier encoding. |
| CapabilityApplicationJsonPart | Closed objects, ordered vectors, JSON pointers, lexical Source/qualified IDs, Presence, Origin, boundary and labels. |
| CapabilityApplicationReferenceJsonPart | Independent complete ModelReference, SemanticTarget, ModelLink, Grounding, Traceability and vocabulary records. |
| CapabilityApplicationDefinitionJsonPart | Header-before-v1 dispatch, every ordered document/Application field, versioned mapping/dependency, source ownership and duplicate owning keys. |
| CapabilityApplicationJsonEncodingPart | Complete semantic transport and nullable-invalid branch representation through the same strict decoder; no byte-derived control identity. |
| CapabilityApplicationBindingPart | Finite exact Local model/kind/version and independent vocabulary resolution; typed unresolved External evidence. |
| CapabilityIngestionSelectionPart | Explicit scope/package/resource/Application declarations, semantic observed comparison, typed stale/mismatch/coverage outcomes and duplicate context/snapshot checks. |
| CapabilityIngestion | Public Factory acquire(Selection), ingest(State, Attempt), pure transition(State, Attempt, Either[Diagnostic, Candidate]), exact readHistorical(State, SnapshotReference) and readCurrent(State, CurrentSelection). |

Selection pairs each original Selected package with its optional public provider,
full ordered ResourceExpectation values and explicit Ready Scope. Acquired
Application has independent producer/schema/ABI and ordered header expectations.
Provenance retains these structured declarations and original family availability;
it is not a provider/JSON-byte identity or freshness guarantee. Candidates retain
original Discovered/Entry/ResolvedTarget readouts. Snapshot construction and state
mutation stay in the ingestion kernel; accepted snapshots add only the explicitly
supplied local management reference. Rejected AttemptRecords retain the original
Attempt and Diagnostic. CurrentSelection carries an exact snapshot reference,
declared Provenance and independent observation; original observation failures
precede selection and retained history never supplies a freshness fallback.

The two new fixture sources are explicitly synthetic kernel conformance inputs.
The canonical descriptor/index/resources and public native Operation carrier
exercise Factory consumption; the literal full Application fixture includes
empty/app-only documents, every Presence reason and independent edge origins.
Neither is the normal Cozy modeler -> generated Scala 3 accessor -> canonical
Directory/CAR Factory -> actual CBD producer proof required by C11I01/C11I02.
That proof remains mandatory later same-Step work before acceptance.

| Case | Authored observable scenario |
| --- | --- |
| C11A01 | Complete Application semantic decode/admit/encode/decode roundtrip. |
| C11A02 | Full closed shapes and ordinary/escaped duplicate JSON keys. |
| C11A03 | Header dispatch, unsupported tags and literal text/path/line validity. |
| C11A04 | Document/identity/owned-edge authority and independent valid revisions. |
| C11A05 | Duplicate header/application keys versus distinct ordered versions. |
| C11A06 | Exact versioned Local Component mapping and Application dependency binding. |
| C11A07 | Demand/model roles and separate Specification/Evidence binding. |
| C11A08 | Independent qualified model/term/relation/profile grounding. |
| C11A09 | External source/origin evidence and authored Application direction. |
| C11A10 | Empty/app-only documents, Present(empty) and four Absent reasons. |
| C11A11 | Active complete literal semantic roundtrip property: minimum 100 successes, zero discards. |
| C11K01 | Public Factory original native/readout objects retained; one provider read. |
| C11K02 | Complete literal resource coverage and exact index order. |
| C11K03 | Independent coordinates, selected finite scope, producer and supported headers. |
| C11K04 | Opaque stale Sources versus path/line mismatches. |
| C11K05 | Repeated selected contexts/global snapshot keys and public declaration diagnosis. |
| C11K06 | Distinct Component witnesses for one exact Capability definition. |
| C11K07 | Original later acquisition/native/scope failure rejects whole candidate. |
| C11K08 | Acquired scoped empty differs from missing root/index/resource/provider. |
| C11K09 | Explicit optional Application unavailable versus acquired invalid/stale/unauthorized. |
| C11K10 | Separate complete ordered Application expected provenance. |
| C11S01 | Accepted/rejected/accepted attempts and original immutable history. |
| C11S02 | Exact explicit historical accepted snapshot selection. |
| C11S03 | Declared current reference and complete structured provenance. |
| C11S04 | Original current observation failure before freshness selection. |
| C11S05 | Reused accepted snapshot reference records Ambiguous rejection. |
| C11S06 | Honest unavailable forward UseCase with authored Application edges retained. |
| C11S07 | Active finite explicit transition sequence property: minimum 100 successes, zero discards. |

Application cases live in CapabilityApplicationProjectionCodecSpec, intake cases
in CapabilityIngestionSpec and history cases in CapabilityIngestionStateSpec under
src/test-current-cncf. Each authored behavior uses semantic AnyWordSpec grouping,
adjacent Given/When/Then clauses and should matchers. These are authored executable
specifications awaiting parent validation, not a test-pass assertion. All seven
CAP-11-01 and four participant CAP-64-05 criteria remain unchecked. CAP-11-02/03
are unstarted and CBD repository-full validation remains deferred-not-run to
PHASE-11.1. Participant Step/Phase acceptance and releases, authorized Cozy main
integration, and original CBD Phase review/release remain separate obligations.

## S02 build prerequisite contract — 2026-10-14

The root and internal-model output-exclusion fixture share the same explicit
CNCF selection. Without `textus.cbd.cncfSourceRoot` or
`TEXTUS_CBD_SUPPORT_CNCF_SOURCE_ROOT`, both retain their declared artifact
dependencies and effective artifact-version checks. With an explicit source
directory, both depend on its RootProject Compile/Test scopes, omit the CNCF
artifact and require the source project's effective version to equal the
unchanged declared CNCF version. Root/fixture source selections, declared
versions, effective Scala versions and Cozy runtime commands must agree.

`textus.cbd.cozyGeneratorRoot` optionally selects an existing read-only Cozy
build-tool runtime for ordinary CBD service generation. Root and fixture share
that selection; the supported delegate route supplies `--runtime-dev-dir` and
uses the existing Java classpath at the unchanged declared Cozy version. An
absent selector retains ordinary artifact-runtime behavior. Explicit CNCF and
generator directories must be nonblank canonical directories with regular,
non-symlink `build.sbt` files; invalid explicit selections fail without fallback.
The normal generation, package, API JAR, Scaladoc, CAR, SAR, publication and Test
resource prerequisites and actual-output checks remain required.

### Source package and shared Job/UOW compatibility

In source mode, root and fixture both apply
`InternalModelExclusionBuild.sourceDependencySettings`. Its
`selectedCncfSourceJars` task dynamically selects the same CNCF RootProject,
compares its effective `version` with the unchanged declared CNCF dependency
version, and obtains its actual `Compile / packageBin` product. Only a regular,
non-symlink `.jar` is valid. The selected project's `Compile / exportJars` must
be true, and the normalized absolute file sequence of its actual
`Compile / exportedProducts` must be exactly that single `packageBin` path.
The task returns the original attributed products, retaining native incremental
analysis metadata. The existing RootProject Compile dependency supplies this
main JAR; the shared `Compile / dependencyClasspath` setting checks its inclusion
and returns the native classpath unchanged. Strict ordinary generation and CAR
resolution therefore see one CNCF runtime descriptor owner. Compile/Test source
mapping and transitive dependencies retain their native graph. Artifact mode
selects no source products and leaves its classpath and dependency checks
unchanged. No JAR rewriting, descriptor fabrication, artifact fallback or
publication supplies this input.

The dependency direction is CBD root Test resource prerequisite -> output-
exclusion fixture package tasks -> selected CNCF `Compile / packageBin` -> CNCF
generated inputs. CNCF has no dependency back to CBD. Existing Compile/Test
source dependencies, generation and real output-exclusion products remain in
this graph.

The selected CNCF
Job/UOW compatibility contract (`docs/spec/job-unit-of-work-compatibility-contract.md` in the selected CNCF checkout)
supplies native commit-gated `JobEngine.stageSubmit` and public
`ExecutionContext.withFreshUnitOfWorkContext`. CBD's transferred execution task
uses this public transform and public `unitOfWork` access while retaining its
existing fresh-reference/self-context and committed-result checks. The gateway
passes the submitting context's public `unitOfWork` to native staging. CNCF
`RuntimeContext` remains private; no runtime exposure or replacement is added.
These two consumer API adjustments establish shared compile compatibility.
The transferred CAR Review work and every
[Phase 14 criterion](../phase/phase-14-checklist.md) remain planned and unaccepted.

This ordinary service generator is separate from the Capability producer on the
dedicated admitted Cozy Phase 64 branch. Only that branch supplies the later
real Capability proof: normal production, generated Scala 3 compilation,
independent native metadata, Directory/CAR public CNCF Factory discovery and CBD
consumption without CML reparsing. These build prerequisites are authored with
parent validation pending; the 28-case map, all seven CAP-11-01 criteria and all
four Cozy CAP-64-05 criteria retain their existing open status.

## S03 real producer interoperability authoring — 2026-10-14

The explicit Test-only `textus.cbd.capabilityProducerRoot` selector binds the
dedicated admitted Cozy producer. It requires an existing canonical source root
with regular non-symlink `build.sbt` and the existing explicit CNCF source
selection; blank or invalid selection fails. Absence retains ordinary build and
S02 fixture behavior. This selector is independent of the ordinary service
generator's `textus.cbd.cozyGeneratorRoot` / `cncf.cozyGeneratorRoot`.

[CapabilityProducerIntegrationBuild](../../project/CapabilityProducerIntegrationBuild.scala)
authors four sequential, bounded child Java calls to the actual dedicated
`cozy.Cozy modeler-scala` entry. Each request and positional source selects exactly
one unchanged original CML: primary Store/Archive or secondary Remote. First
headers are `header:primary-r42` / `header:secondary-r43`; Second headers are
`header:primary-r44` / `header:secondary-r45`. The six resulting Store/Archive/Remote
First/Second accessors are Test source-generator products compiled by the
configured Scala 3.3.8. The executing Cozy runtime export supplies its selected
producer version; its BuildInfo supplies SimpleModeling model version. The
runtime descriptor comes from the actual selected CNCF source package JAR.
No generated projection, source, index, CAR or execution result is committed as
fixture input. The two complete CML files and closed request templates are
deliberate authoring inputs.

The independent public native fixture supplies three Store snapshots/bindings
and original Operation, Workflow and normalized StateMachine definitions. Target
sources `target:operation:save`, `target:workflow:save`, `target:state:store`, target
contracts/ABIs, authored definition/edge revisions, `scope:r1`, Component release
`1.2.3-SNAPSHOT`, package `package:r9` and local record revisions remain separate.
Ready scope has no synthetic target contracts. Procedure/async/job/registration/
trigger/guard/action/source graph metadata remains solely on the original native
objects; intake invokes no procedures or Component activation. Archive and Remote
have honest acquired empty native vectors with nonempty Capability definitions.

The Test intake copies actual canonical bundles and separate complete Application
products without CML. Normal Store/Remote `package-car` fixture commands use a JAR
of the actual six compiled Test accessors plus the actual CNCF package JAR, complete
generated project copies, original selected CML and authored normal project/ABI
inputs. Their own copied CML is removed after packaging and before public
discovery/CBD acquisition. Only public `ComponentFactory.discoverCapabilities`
acquires package metadata. Every child process uses an argument array, a fixture
cwd, ignored target log and a 180-second bound; failure has no fallback or retry.
Product locators use ordinary properties schema `cbd.capability-producer-fixture.v1`.

Primary and secondary Application envelopes remain four separate complete
one-document products. Store+Remote admits the complete primary Application;
Archive+Remote preserves Archive Provided `Present(empty)` versus Store Required
`Absent`. Store+Archive+Remote retains both same-definition witnesses when
Application is unavailable, but the complete primary Local mapping is Ambiguous:
no winner, deduplication or manufactured combined Application is permitted.

The [real producer executable specification](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityProducerIntegrationSpec.scala)
authors exactly these eight scenarios, each with actual Given/When/Then and
should clauses:

| Case | Authored observable behavior |
| --- | --- |
| C11R01 | Six compiled accessor products agree semantically with public Directory discovery, independent headers/versions and original three Store native kinds. |
| C11R02 | Source-free Store+Remote Directory intake preserves the complete primary Application, Provided/Required declarations, native originals, External evidence and unavailable forward UseCase. |
| C11R03 | Actual normal Store/Remote CARs using compiled carrier/CNCF JARs agree with Directory products and ingest without CML or activation. |
| C11R04 | Every complete primary/secondary First/Second Application independently decodes/admit/encodes/decodes and survives storage with full fields, Presence, ordered origins and exact identities. |
| C11R05 | Archive provided Present(empty), Store required Absent and missing-provider Unavailable remain distinct. |
| C11R06 | Both Store/Archive definition witnesses remain discoverable; complete primary Local ambiguity rejects the whole candidate and retains prior accepted state. |
| C11R07 | Explicit Second current selection retains exact First historical snapshot/candidate/native/Application objects and independent local references. |
| C11R08 | Stale produced-header/current expectations and original Unauthorized/Unavailable native failures preserve accepted current/history without partial success or fallback. |

This section records authoring only. Parent-owned focused validation selects this
eight-case suite and the preserved five-suite 49-case kernel/control/output
accumulator, including complete configured Compile/Test source compilation.
Existing active properties and participant failure histories retain their owners.
Forward UseCase remains `not-yet-admitted`, with its positive path unexercised.
All seven CAP-11-01 criteria remain unchecked; CAP-11-02/03 remain OPEN. Separate
primary/participant Step acceptance/commits, participant Phase full validation/
review/local releases, authorized eventual Cozy main integration and original CBD
Phase review/release remain owed. CBD aggregate full-suite validation stays
deferred-not-run to Phase 11.1; successor execution has not begun. Transferred
Job/Provider/Entity execution and failed evidence remain unaccepted Phase 14 work.
