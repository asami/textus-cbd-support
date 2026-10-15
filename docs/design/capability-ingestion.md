# Capability Ingestion Design

Status: CAP-11-PKG01 accepted after focused validation and independent whole-Step conformance; Phase acceptance pending.
Scope: PHASE-11 / CAP-11-01 / S01-consumer-contract + S02-ingestion-kernel + S03-real-producer-interoperability

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

## CAP-11-PKG01 current authored responsibility flow — 2026-10-15

The [current ingestion contract](../spec/capability-ingestion-contract.md#cap-11-pkg01-current-authored-contract--2026-10-15)
now implements per-Component Usage. Selection/Provenance contain scope, ordered
packages and the unavailable UseCase family. Each package independently selects
optional Usage producer/schema/ABI and ordered document Source expectations.
None means expected absence; supplied malformed/incompatible content rejects the
whole candidate. Public CNCF `ComponentFactory.acquireUsageCapabilities` owns
Directory/CAR acquisition and supplies the original content/input/resource witness.
CBD's `CapabilityComponentUsageAcquisitionPart` compares semantic ownership and
expectations and delegates strict decoding/binding; it performs no filesystem,
ZIP, descriptor, process or duplicate transport checks.

```text
ordered explicit package selections
  -> nonempty Component resources: original public native discovery/provider
  -> empty Component resources: public package load, empty index/projections
       -> ordered Native/MetadataOnly ComponentPackage witnesses
  -> public optional Usage acquisition for every selected package
       -> exact owner/envelope/package/header checks + complete selected binding
       -> ordered ComponentUsagePackage Available/Unavailable witnesses
  -> complete immutable candidate -> accepted Snapshot or original rejection
```

Candidate derives its native-only components vector once from componentPackages.
MetadataOnly keeps its original acquired package without a fake native input,
provider, declaration or Discovered. Usage has schema 1 and
cozy.component-usage-capability.v1, explicit ComponentId owners and separate
Component/package coordinates. Qualified model identity, opaque contract/source/
producer/release/package/ABI domains, Presence and every edge Source/Origin remain
independent. Local binding considers the complete finite selected Component and
Usage candidate set and preserves ambiguity; External evidence remains unresolved
and attributed. Current/history transitions preserve their existing authority.

Normal Test generation uses schema-2 requests, owner-partitioned Store (One/Two),
UsageOnly (Independent, resources=[], no Scala output/provider), Archive (no Usage)
and Remote. First/Second remain independent; fifth primary Contracts with
header:primary-r46 generates StoreContractsMetadata/ArchiveContractsMetadata from
empty Usage selections and unchanged native contracts/contexts. Eight accessors
flow through ordinary Scala 3 compilation. Full canonical images alone enter
source-free intake. Actual normal package-car uses the same First Store/Remote
images; codec inspection uses the public acquired witness. The updated
[producer suite](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityProducerIntegrationSpec.scala)
and [CAR query proof](../../src/test-capability-producer/scala/org/simplemodeling/textus/cbdsupport/runtime/CapabilityQueryProducerIntegrationSpec.scala)
are authored pending parent validation, fresh independent Step conformance and
separate participant/Phase acceptance. No producer is executed merely to read
metadata, and no runtime procedure, release or main merge is claimed.

## Historical baseline and authoring record

The remaining sections preserve the original CAP-11-01/02/03 designs, dated
authoring decisions and acceptance history. Old Application descriptions, old
module signatures and earlier suite/case counts describe those prior boundaries;
they are superseded for current implementation by CAP-11-PKG01 above and do not
claim acceptance of changed current inputs or provide compatibility behavior.

The [consumer contract](../spec/capability-ingestion-contract.md) owns all
C11I-D01–16 decisions. This design explains their settled responsibility flow.
The private ingestion kernel and real normal Cozy producer -> generated Scala 3
native -> public Directory/CAR Factory -> source-free CBD discovery/intake are
accepted after focused validation, independent whole-Step review and mechanical
naming closure; see
[CAP-11-01 acceptance](../phase/phase-11.md#cap-11-01-acceptance--2026-10-15).
The [Phase checklist](../phase/phase-11-checklist.md) owns progress and acceptance.

The original delivery-order/closure prose and dated S02/S03 authoring sections
retain historical pending/unchecked states as at 2026-10-14. Current acceptance
supersedes those states only for CAP-11-01. Participant acceptance/Phase closures,
CAP-11-02/03 and full CBD Phase closure remain outstanding; CBD full-suite validation
is deferred-not-run to Phase 11.1. Forward UseCase demand remains not-yet-admitted,
and Phase 14 failed Job behavior remains unaccepted.

## Directory/CAR acquisition amendment — 2026-10-15

The [Component Usage adoption contract](../spec/component-usage-capability-adoption.md)
replaces Application semantics/ownership and the earlier external JSON delivery.
The reviewed Component owns its image and both distinct metadata families.

```text
Cozy source / validated Usage model with explicit Component ownership
  -> Component-partitioned Directory image -> CNCF acquisition -> CBD Usage codec
                         |
                 normal CAR packaging
                         v
                        CAR -------------> CNCF acquisition -> CBD Usage codec
```

Component Capability keeps its own native discovery/admission and deductive
contract model. Usage evidence and mappings are separate inductive records;
co-location does not merge their semantics. Develop directly against Directory.
The new source/wire, codec, catalog, query and generated APIs change together.
Delete old symbols with no aliases, forwarders or old-format readers so stale
typed calls fail compilation. Earlier accepted sections below describe the
historical implementation, not supported compatibility paths.

## Accepted upstream entry points and separate families

CNCF owns Component projection/schema/ABI, admission and native realization
binding. The consumer uses public ComponentFactory.discoverCapabilities with
explicit PackageInput.Selected and ComponentCapabilityMetadataProvider values.
Discovered retains original acquisition, native input, admitted entries and
resolved targets, so CBD can retain actual source/descriptor/target observations
without constructing admission or reaching private[cncf] adapters. Static
discovery supplies metadata independently of runtime Component activation.
These are the settled C11I-D01–04 boundaries.

Cozy owns the separate CmlCapabilityApplicationProjection.encode product format.
Its complete ordered documents preserve every Application definition, empty or
app-only document, Presence and edge-specific source/origin. A separate strict
consumer decoder mirrors this contract instead of reducing Application facts
to an embedded Component field. C11I-D05/06 retain the full upstream wire and
selected finite reference scope; normalization, procedure and cycle policy
remain with the producer.

Application-to-model demand/discovery and Application-to-Component mappings
remain authored directions. They cannot supply forward UseCase -> requiredApplication
data. The Use Case family is explicitly UnavailableUpstreamProjection,
not-yet-admitted, with its positive path unexercised (C11I-D07). Valid Component
intake can coexist with explicitly unavailable Application input. Acquired
invalid/stale/unauthorized Application data rejects the candidate.

```text
Cozy original parsed and validated source documents
  -> Component R11 canonical package resources + existing schema-3 descriptor
  -> public CNCF Factory(package selection, independently observed native provider)
  -> original Discovered / Entry / ResolvedTarget
                                                        \
Cozy full separate Application JSON -> strict consumer codec -> complete candidate
Explicit unavailable Use Case family --------------------/          |
                                                                  v
prior immutable state + explicitly referenced attempt -> value transition
                                                                  |
                                               accepted snapshot or rejection
                                                                  |
                                                later catalog/query handoff
```

## Minimal package-private modules and responsibility flow

These three runtime modules are parent-decided architecture (C11I-D16).
Their authored kernel preserves the consumer contract without expanding
the existing public service surface.

| Module | Responsibility and boundary |
| --- | --- |
| CapabilityIngestionModel | Typed complete selection, independently available families, snapshot, Attempt and result carriers; existing local RecordReference factories in distinct attempt/snapshot roles. |
| CapabilityApplicationProjectionCodec | Strict duplicate-aware portable Application decoding and complete record preservation; Local identity/kind/version resolution against explicitly selected finite scope and definitions. |
| CapabilityIngestion | Public-CNCF acquisition orchestration for the ordered complete package vector, expected/observed provenance checks and pure all-or-nothing candidate transition. |

Selection names snapshot, scope, coordinates, resources, producer/version,
schema/ABI and full Source independently. Acquisition location stays separate
from model location. Complete expected-versus-observed checks cannot be replaced
by filename, collection order, mtime, byte tokens or dependency versions. Different
Component contexts observing one definition retain separate witnesses; repeated
full context keys fail rather than being deduplicated. The codec retains
External unresolved evidence without treating absent outside-scope facts as
dangling Local references.

Local attempt/snapshot identities and positive producer-allocated revisions reuse
the [typed control domains](../spec/internal-model-typed-control-contract.md).
Upstream opaque source revisions remain separate. These references record
provenance rather than authenticity, permission or undeclared-change detection
(C11I-D08). Source mode in existing [build.sbt](../../build.sbt), selected by
TEXTUS_CBD_SUPPORT_CNCF_SOURCE_ROOT / textus.cbd.cncfSourceRoot, binds the actual
admitted CNCF checkout without publication or dependency substitution.

CatalogRuntime's network/cache and existing Job/Review and durable approval
programs retain separate responsibilities. The CAR Review taxonomy does not
supply Component Capability identities. No public service CML, upstream code,
CatalogRuntime or existing approval path changes are required by this design.

## Explicit full-candidate transition and read selection

The transition has only explicitly supplied prior immutable state and one
referenced Attempt (C11I-D09). Acquisition completes before a successful
snapshot is selected. Original failure kind/detail/scope/component/source/location
is retained; package/native failure or invalid acquired Application data cannot
become partial success. A valid acquired empty index with Ready/acquired native
metadata is a successful scoped fact, distinct from unavailable acquisition.

An accepted Attempt appends the successful snapshot and selects its exact
reference as current. A rejected Attempt appends the original diagnostic with
selection provenance while preserving previous current pointer, accepted
snapshots and history. The state describes logical value history. It introduces
no persistence, cache, transaction, concurrency, retry or replay subsystem.

Historical reads explicitly select an accepted snapshot reference. Current
reads must match declared snapshot/scope/provenance; a changed expected version
or original stale/unavailable/unauthorized observation retains that typed
outcome instead of presenting retained historical data as fresh. Opaque versions
cannot establish chronology or a newest winner.

## Catalog, query and presentation handoff

The accepted snapshot retains original ordered Component entries and native
observations, independent complete Application documents and Use Case family
availability (C11I-D10). CAP-11-02 derives separate CapabilityRecord,
CapabilityRealizationRef and CapabilityDemandTrace views with full enclosing
declaration/context keys and provenance. Portable semantic codecs retain
references and attribution; actual native procedures remain original objects
and are not serialized into a new portable execution contract.

C11I-D11/12 preserve all deterministic exact-identity providers and only
equal-version or explicitly directed same-identity compatibility. Each trace
path joins its Application mapping to its Required declaration, compatible
Provided declaration and that declaration's own realization edges. Retaining
path ownership prevents independent endpoint lists from producing false joins.
Specification and Evidence remain distinct reference families.

The six planned queries retain explicit identity/version/snapshot/scope input,
the original typed failures, admitted-empty distinction and seven typed gap
variants. MissingRealization retains the compatible provider. NoProvider is
limited to a complete successfully admitted query scope. C11I-D13 keeps runtime
Availability/Authorization/Guard unknown without explicit observations and
passes identical result/identity/provenance and authorization/availability
semantics to later Phase 11.1 Web/MCP presentation. No presentation or new
authorization-policy implementation begins here.

## Real fixture delivery order and outstanding closure

C11I-D14 identifies the real Cozy fixtures capability-model.cml and
capability-model-secondary.cml, accepted CNCF CAP-78-01–04 and Cozy CAP-64-01–04
contracts, and the additional authored CAP-64-05 normal producer/publication
prerequisite. Their source revisions, BuildInfo producer version, projection
schema/ABI, Component/package release and target contract/ABI stay independent.
Real parsed/generated fixtures do not yet prove native Scala 3/Factory/CBD intake.

The remaining real delivery chain is actual Cozy normal production -> generated
Scala 3 accessor compilation and independently observed native metadata ->
canonical Directory/CAR public Factory discovery -> actual CBD ingestion without
CML reparsing. Complete Application consumer roundtrip and the honest unavailable
Use Case path accompany that chain. Synthetic negative inputs and handcrafted
public client projections can test rejections but cannot replace its positive
producer evidence.

C11I-D15 keeps C11I01–12 executable scenarios pending, with meaningful
AnyWordSpec/GivenWhenThen/should assertions and applicable properties requiring
100 successes and zero discards when implemented. All seven CAP-11-01 and four
Cozy CAP-64-05 criteria stay unchecked. CAP-11-02/03 remain unstarted; their
catalog/query modules are separately planned against this handoff. Complete
Step acceptance/commits, participant Phase full validation/review/local release,
authorized Cozy main integration and original CBD Phase review/release remain
owed. The applied Phase 11 -> 11.1 split, predecessor, successor, budgets and
final-only policy remain as recorded in [Phase 11](../phase/phase-11.md).
CBD full-suite validation stays deferred-not-run to Phase 11.1; successor
Web/MCP/views, Phase 12/14 and aggregate execution are outside this Slice.

## S02 kernel responsibility flow — 2026-10-14

The three settled domain modules are now authored with six bounded helpers:
JsonPart handles common shape/scalars; ReferenceJsonPart handles semantic edges;
DefinitionJsonPart handles complete documents/ownership; JsonEncodingPart emits
complete semantic transport through the same structural decoder; BindingPart
resolves only explicit finite Local candidates; SelectionPart validates declared
scope and complete observed provenance. The
[S02 API and case map](../spec/capability-ingestion-contract.md#s02-ingestion-kernel-authoring--2026-10-14)
records all nine sources and C11A01–11/C11K01–10/C11S01–07.

acquire uses public Factory once per selected package and retains original
Discovered/native/entry/readout objects. It does not separately reread providers.
Only after every Component succeeds does it decode/admit acquired Application
or retain its explicitly unavailable branch. Original rejected observations
reject the whole candidate. The immutable transition appends AttemptRecord and
changes current only on complete success; exact historical/current reads retain
the frozen C11I-D09 distinction. Current observation failures remain original.

The synthetic canonical package/native Operation and complete literal Application
fixtures exercise those consumer boundaries, including both active properties
configured for at least 100 successes and zero discards. Authoring claims no run
or review result. Actual normal Cozy generation, Scala 3 accessor/native intake,
Directory/CAR Factory discovery and CBD producer roundtrip remain mandatory
later same-Step proof. All acceptance criteria and separate participant closures
remain open; this authoring does not start catalog/query or successor execution.

## S02 build prerequisite wiring — 2026-10-14

The existing explicit CNCF selector now binds root and the internal-model
output-exclusion fixture to the same RootProject Compile/Test dependency. Both
remove the CNCF artifact in source mode. The fixture evidence task checks equal
source selections and declared versions, then reads the selected source
project's effective version through a dynamically selected SBT task before
using its normal output prerequisites. Artifact mode retains both effective
artifact-version checks. The existing Test resource generator and actual fixture
generation, packaging and publication products remain part of the graph.

The optional `textus.cbd.cozyGeneratorRoot` selector canonicalizes an explicitly
supplied existing generator directory and requires a regular non-symlink
`build.sbt`; blank or invalid input fails. Root assigns this directory to
`cozyDelegateProjectDir`, and the fixture inherits it and checks equality.
Absence keeps the ordinary declared artifact runtime. The supported sbt-cozy
delegate adds `--runtime-dev-dir` to the unchanged declared-version command and
invokes the existing Java classpath without nested SBT.

### Actual source package and public context boundary

The shared `sourceDependencySettings` applies to root and fixture. Its dynamic
`selectedCncfSourceJars` task checks the selected CNCF effective version against
the declared dependency version and requests that project's actual
`Compile / packageBin` JAR. It requires a regular non-symlink `.jar`, selected
`Compile / exportJars` enabled, and an actual `Compile / exportedProducts`
normalized absolute file sequence containing exactly that single package path.
It returns those original attributed products with native incremental analysis
metadata. SBT's existing RootProject Compile dependency supplies the main JAR;
the shared `Compile / dependencyClasspath` wrapper checks inclusion and returns
the native classpath unchanged. Ordinary strict generation and CAR inputs share
one CNCF runtime descriptor owner. The existing Compile/Test source mapping and
transitive graph are retained; artifact mode selects no source products and
leaves the classpath unchanged. Neither a copied descriptor nor local publication
substitutes for the selected project product.

Root's Test resource generator depends on the fixture's existing output tasks;
the fixture depends on selected CNCF packaging and its generated inputs. There
is no CNCF -> CBD edge or generator recursion. Main/API JARs, Scaladoc, CAR, SAR,
local project outputs and their private-data exclusion checks retain their
ordinary task owners.

The selected CNCF
compatibility design (`docs/design/job-unit-of-work-compatibility.md` in the selected CNCF checkout)
owns native staged reservation/publication and the purpose-named public fresh
UOW transform. The transform delegates the existing profile-preserving private
rebinder. CBD uses only the returned context's public UOW and preserves the
transferred action core, authorization, Provider cancellation, exact completion
and committed-result checks. The gateway uses actual native staging with the
submitting context's public UOW. CNCF runtime facilities and lifecycle ownership
remain private. These precise API edits do not start or accept the separately
planned [Phase 14](../phase/phase-14.md) integration.

That read-only ordinary CBD service generator input does not establish Cozy
Phase 64 acceptance or replace the dedicated branch's real Capability producer
and native proof. The ingestion architecture, independent source/version domains,
unavailable Use Case path and remaining same-Step delivery order stay as defined
above. Build authoring awaits parent validation; all separate Step/Phase closure,
release and authorized Cozy main-integration obligations remain open.

## S03 actual producer Test wiring — 2026-10-14

The [S03 contract and case map](../spec/capability-ingestion-contract.md#s03-real-producer-interoperability-authoring--2026-10-14)
records the authored real interoperability harness. Its explicit
`textus.cbd.capabilityProducerRoot` opt-in adds only Test sources/resources and
source generators, requires the selected CNCF source graph and leaves the
ordinary service generator/root+output-exclusion fixture behavior intact when
absent. Build prerequisites select the actual CNCF package JAR and extract its
runtime descriptor, then run four sequential single-source dedicated normal CLI
commands. Six real generated accessors flow into configured Scala 3 Test compile.
Ordinary resource properties pass selected locations/versions to Test; they carry
no acceptance state, content identity or seal.

The build helper owns generation; IntegrationFixtures owns compiled accessor
calls, source-free bundle/Application intake copies, real compiled-carrier JAR,
normal Store/Remote package-car inputs and explicit record references.
NativeFixtures owns independent immutable public finite scope, historical facts,
three Store native definitions/snapshots/bindings and counting providers. The
eight-scenario IntegrationSpec consumes these products through public Factory
and the original private CBD kernel. None of these responsibilities changes
production ingestion, upstream source or runtime lifecycle contracts.

Application products retain separate complete primary/secondary one-document
envelopes and every authored field/origin. Store+Remote is the positive primary
selection. Archive+Remote proves honest empty realizations; Store+Archive+Remote
is the explicit Local-ambiguity negative, retaining both provenance witnesses
when Application is unavailable. First/Second opaque headers change independently
of native contracts, definition/edge sources and explicit local record references.
The original native procedure graph remains on retained objects and is not
activated or serialized as a competing portable execution contract.

Child Java processes are finite argument-array commands in owned target fixture
directories, bounded at 180 seconds with logs retained under the production target
root. Source-free intake starts after legitimate CAR provenance packaging. Tests
remove only their temporary intake/project copies; actual producer/CNCF inputs
remain available. No child SBT, alternate producer, merged Application, retry or
historical-current fallback is introduced.

Authoring claims no run or acceptance. The selected eight-case realproducer suite
and preserved five-suite 49-case accumulator await parent validation and independent
Step review. Original separate participant Step/Phase validation/review/releases,
authorized eventual Cozy main integration, CAP-11-02/03 and original CBD Phase
review/release remain owed. Aggregate full validation stays deferred-not-run to
Phase 11.1; transferred runtime execution/correction remains unaccepted Phase 14.
