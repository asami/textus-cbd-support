---
status: target
decision_scope: P10-22A--P10-22B / P104-TYPED-DECISION-RECORD
updated_at: 2026-10-01
---

# Internal-model Decision Record Contract

This normative target defines current V2 durable decision and alternative
record admission. Original P10-22A--P10-22B acceptance remains historical;
current authoring establishes neither compilation, validation, independent
acceptance nor Phase 10.4 closure. Its companion is the [Internal-model Decision
Records Design](../design/internal-model-decision-records.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for the V2 package manifest, inventory, explicit artifact
versions and exact dependency graph. The [Typed Control
Contract](internal-model-typed-control-contract.md) owns distinct logical and
artifact references; the [Source Snapshot
Contract](internal-model-source-snapshot-contract.md) owns V2 source metadata. The [Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains authoritative
for the selected realization, its scope, semantic identities, source references,
and conditions. The [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) remains the sole source
of CCDM meaning.

This contract adds no canonical semantic fact, CCDM lane, public API, approval,
CML-write permission, rehydration behavior, or proposal-promotion behavior. It
specifies a retained decision record only. The current grammar is exactly
ccdm-decision-records-v2 / schemaVersion 2.0 with realization V3, package V2 and
source V2. There is no legacy reader, migration, compatibility alias, overload,
inferred version, default or fallback.

## 1. Scope and selected admission

A decision ledger SHALL be represented by one present package artifact with the
existing `decision` role. It is separate from realization and projection
artifacts. Existing package role vocabulary, role multiplicity, structural
validity, and public package validation behavior remain unchanged.

A package with no decision artifact can be structurally valid. It is not,
therefore, evidence that decision readiness is met. Decision admission requires
exactly one selected present decision artifact and exactly one selected present
realization artifact. Multiple or absent selections fail decision admission
explicitly. The selected decision artifact SHALL directly depend on the selected
current realization by its exact ArtifactReference in the manifest; source
snapshots remain its transitive basis. A record SHALL not bind a package hash,
require a historical artifact to remain present, or infer historical versions.

Admission begins from one project-bound inventory/version authority pass. A
future reader consumes the captured immutable decision bytes, selected current
realization, and selected source snapshots yielded by that pass. It SHALL not
reopen or rescan the manifest or artifacts, fetch live sources, consult Git or
an archive, or repair/reconstruct missing information.

## 2. Durable logical record content

The ledger has an explicitly allocated ledgerReference (recordId and positive
recordRevision). Each retained record has an explicitly allocated
decisionReference of the same distinct RecordReference type, a bounded topic
identity, and the exact admitted Component,
projection-context, and selected-Use-Case scope. Decision, topic, selected
choice, and alternative identities are record-local attribution/navigation
identities. They are not CCDM element or relationship identities and SHALL not
be promoted to CCDM facts.

Every accepted or superseded record SHALL retain all of the following logical
content:

| Required retained content | Admission meaning |
| --- | --- |
| State | The explicit state is `accepted` or `superseded`; no implicit state is derived from placement, time, or content. |
| Human actor and provenance claim | An accountable human identity and role, plus attributable human-decision source identity, supplied locator, and supplied source-owned revision. |
| Choice and rationale | The selected choice identity and description and a nonblank rationale. |
| Affected identities | A nonempty set of exact affected CCDM element and/or relationship identity references. |
| Considered evidence | Evidence identities with their own source kind, authority, identity, supplied locator/revision, and stated conditions. |
| Decision context | Explicit assumptions, conditions, and limitations. |
| Relevant alternatives | Each retained rejected alternative's identity, description, and rejection rationale. |
| Basis binding | Exact realizationArtifactReference (artifactId, artifactRevision, role=realization), realizationReference (recordId, recordRevision), and the exact record scope. |
| Supersession link | An optional exact predecessor RecordReference, subject to section 4. |

An explicitly empty optional list means that none was recorded. It does not
prove absence, establish exhaustive analysis, or allow a reader to invent an
unstated fact. Labels, paths, hashes, array position, timestamps, confidence,
scores, provider ranking, and input/presentation order never establish record
identity, currentness, or a selected choice.

## 3. Human attribution, evidence, and current basis

The actor and human-decision provenance are retained attributable claims. They
do not authenticate a human, prove semantic truth, demonstrate approval, grant
permission, or reconstruct Phase 9's transient `HumanDecision` admission.
Likewise, a provider suggestion can be retained only as considered evidence; it
cannot be the accountable human or substitute for explicit human-decision
attribution. External human or provider evidence remains external attributable
evidence and never becomes canonical CML evidence merely by being stored here.

Every accepted terminal record SHALL bind the selected current realization's
entire ArtifactReference, logical realizationReference, and scope. Its
affected semantic identities and required current source/condition references
SHALL resolve against that selected realization and its admitted source
snapshots. Existing realization/source limitations remain observable; decision
retention does not erase them. A different or old basis, unknown target,
cross-scope target, or unresolved required reference fails current admission;
the reader SHALL not rebind, infer, or repair it.

An explicit human acceptance may later select an alternative that a prior record
rejected. Provider score, rank, suggestion, or ordering cannot perform that
promotion.

## 4. Explicit supersession and retained history

Supersession is an explicit relation within one ledger scope and one bounded
topic. It is never inferred from timestamps, artifact array position, labels,
or hashes. Decision record IDs are unique within the ledger even when their revisions
differ; two versions of one ID cannot coexist and no latest version is selected.
Successive
decisions about the same bounded topic share that topic identity. Choice and
alternative identities are unique within their owning record, so retained
history may reference the same topic and choice across successive records. A
predecessor link must resolve the entire exact recordId/recordRevision reference
to a retained record in the same scope and topic.

- A superseded record has exactly one successor.
- An accepted terminal record has no successor.
- A predecessor link cannot be self-referential, dangling, cyclic, or forked.
- A topic has no duplicate accepted terminal record.
- Independent topics may each have an independent accepted terminal record.

Successive records may have different explicit realization references without requiring
multiple present realization artifacts or changing package multiplicity. The
old record retains its old artifact and logical realization references, affected identities,
evidence, rationale, and provenance. When that old basis is not the verified
current basis, its basis status SHALL be explicitly **historical-unverified**:
only record metadata and the supersession chain were checked; the unavailable
historical artifact, targets and source facts were not reverified. Historical-unverified
does not make a record current, valid for the current basis, or an approval.
The reader shall neither require historical bytes nor silently retarget or drop
the predecessor.

## 5. Logical admission matrix

| Situation | Required result |
| --- | --- |
| One selected decision artifact directly depends on one selected realization; a terminal accepted record has complete retained content, exact current basis, resolvable targets/references, and valid topic chain | Admit the current decision record. |
| Package has no decision artifact | Structural package validity may remain valid; decision readiness is not established. |
| Missing or multiple selected decision or realization artifacts, or no direct decision-to-current-realization dependency | Fail decision admission. |
| Missing required ledger, decision, topic, choice, or alternative identity; duplicate decision identity within the ledger; duplicate choice or alternative identity within one owning record (reuse of a bounded topic across successive records is not a duplicate identity error); implicit state/currentness; blank rationale; missing actor/provenance; or no affected identity | Fail decision admission. |
| A provider suggestion/rank/label/order is treated as the human actor, decision, winner, approval, or canonical evidence | Fail decision admission. |
| Current accepted record has a wrong realization artifact ID/revision/role, logical realization ID/revision or scope, or affected/current source/condition references are unknown, cross-scope, or unresolved | Fail decision admission without rebinding or repair. |
| Explicit same-topic predecessor chain has self, dangling, cyclic, forked, duplicate-successor, or duplicate-terminal relation | Fail decision admission. |
| Retained predecessor has a different old basis and is explicitly historical-unverified while the current terminal meets current-basis rules | Admit the current terminal and retain the predecessor as historical attribution only. |
| Retained old basis is presented as current, reverified, approval, or a waiver of current-basis validation | Fail decision admission. |
| Manifest/artifact content is malformed, absent, ambiguous, duplicated, or semantically inconsistent in the captured authority pass | Fail closed. |

## 6. Exclusions and executable boundary

This contract neither authenticates an actor nor decides truth, approval,
proposal promotion, CML mutation, source freshness, rehydration, runtime/API
behavior, publication, deployment, or a history archive. It introduces no
source fetching, live lookup, Git lookup, or historical-basis inference.

The closed portable artifact grammar and executable admission proof below show
preservation of retained logical content and fail closed for malformed,
ambiguous, malformed, and current-mismatch cases without rereading the verified
authority inputs. Historical P10-22 acceptance remains historical. Current V2 authoring does not
establish compilation, validation, independent acceptance, any Step completion
or Phase 10.4 closure.

## 7. Closed `ccdm-decision-records-v2` artifact

The `decision` artifact is strict UTF-8 JSON despite the conventional
`decisions.yaml` filename. BOM, invalid UTF-8, duplicate object members at
every depth, malformed JSON and trailing non-JSON data reject. Harmless
object-key order, insignificant whitespace and equivalent JSON escaping are
admitted. Writers may emit deterministic keys and one terminal LF; neither
byte comparison nor typed re-encoding equality is admission authority.
Every supplied string is a well-formed Unicode scalar sequence; required
identity and prose strings are nonblank and retain their supplied spelling
without trim or normalization. `locator` and `revision` are required nullable
fields: each is either `null` or an exact nonblank source-owned string.

The root has exactly `ledgerReference`, `profile`, `records`,
`schemaVersion` and `scope`. The profile is exactly
`ccdm-decision-records-v2` and schemaVersion exactly `2.0`.
`ledgerReference` has exactly `recordId` and `recordRevision`.
The producer explicitly allocates its logical ID and revision independently
of the package carrier, decision artifact and individual decision record.
Each record reference is closed, with a nonblank opaque recordId and positive
lexical JSON Long recordRevision: no sign, fraction, exponent, leading zero,
string, overflow, missing value or default. An ArtifactReference has exactly
`artifactId`, `artifactRevision` and `role`, using the existing strict
typed token, positive lexical Long and closed role factories.
`scope` has exactly the existing realization fields `componentIdentity`,
`projectionContextIdentity` and `selectedUseCaseElementIdentity`.
There are no root hashes or cached canonical bytes.

Each record has exactly `decisionReference`, `topicIdentity`, `state`,
`actor`, `provenance`, `selectedChoice`, `rationale`, `affectedTargets`,
`consideredEvidence`, `assumptions`, `conditions`, `limitations`,
`realizationConditionIds`, `rejectedAlternatives`, `basis` and `supersedes`.
`decisionReference` is an exact RecordReference. `state` is `accepted` or
`superseded`; the required nullable `supersedes` is `null` or an exact
predecessor RecordReference, never a bare ID. `actor` has exactly `kind`,
`identity` and `role`, with kind exactly `human`. `provenance` has exactly
`authority`, `identity`, `locator` and `revision`, with authority exactly
`human-decision`. Hash-bearing, bare, old, missing and additional shapes reject.
These fields are attributable claims, not authentication or approval.

`selectedChoice` has nonblank `choiceIdentity` and `description`.
`affectedTargets` is nonempty and uses the existing exact
`semanticIdentityKind` (`element` or `relationship`) and `semanticIdentity`.
Each `rejectedAlternatives` entry has nonblank `alternativeIdentity`,
`description`, and `rejectionRationale`; it has no rank, score, or winner
field. A selected choice identity and a rejected alternative identity cannot
collide within one record.

Each `consideredEvidence` entry has `evidenceIdentity`, `kind`, `source`,
`sourceReferenceId`, `conditionIds`, `conditions`, and `limitations`. Its
closed source shape retains a nonblank authority claim; it is not forced to
`human-decision`. Kinds are `realization-source`, `external-human`,
`provider-proposal`, or `external-other`. A realization source has a non-null
reference ID. External kinds have null reference ID and empty condition IDs.
Provider evidence remains considered evidence only and is never an actor,
provenance, or position-derived selected choice.

`basis` has exactly `realizationArtifactReference`, `realizationReference`,
`scope` and `status`. The artifact reference requires role `realization`.
Its scope equals the ledger scope even for retained history. Status is
`current` or `historical-unverified`. `records` is nonempty and sorted
uniquely by exact decisionReference.recordId, independently of revisions.
Affected targets sort uniquely by `(semanticIdentityKind, semanticIdentity)`;
evidence, alternatives, and condition-ID collections sort uniquely by their
respective identities. All opaque identity comparison uses unsigned UTF-8
bytes. Assumptions, conditions, and limitations—including those on evidence—are
ordered nonblank prose and preserve both supplied order and duplicates.

`InternalModelDecisionRecordCodec` parses this grammar with duplicate-key
rejection and admits decoded values independently of presentation. Its pure
encoder deterministically sorts identity collections on demand while preserving
ordered prose. It neither repairs semantic collections nor selects a current decision, winner,
approval, or source authority.

## 8. Captured admission and executable specification

`InternalModelPackageValidator.verifiedDecisionRecords` performs one package
authority pass, selects exactly one present decision and realization, and
requires their exact direct manifest dependency. It supplies immutable ordinary decision bytes
and the captured realization package to
`InternalModelDecisionRecordValidator.validateVerified`; that path first
admits the captured semantic realization and then decodes the captured decision
bytes. It does not reopen package files, fetch a source, inspect Git or an
archive, mutate, rebuild a projection, or approve a decision.

Every basis scope equals the selected realization scope. Exact-current means
that the entire selected captured realization ArtifactReference, admitted
realization.realizationReference and scope all equal the basis. Status is `current` if and only if that tuple is
exact-current. A different basis is retained only as a `superseded`,
`historical-unverified` record; it does not resolve old targets, sources,
conditions or unavailable artifacts. An accepted terminal is exact-current. A superseded predecessor whose basis
still equals the current realization remains permitted with current status.

For an exact-current basis, affected targets and realization condition IDs must
resolve in the selected realization. The record retains every condition that
targets an affected identity and every evidence condition ID. A
`realization-source` evidence reference resolves exactly and its full source
object and complete source-reference condition set must match the realization.
External evidence is not resolved or promoted by its authority text. The chain
requires unique decision IDs, same-topic predecessors, no self/dangling/cycle/
fork, one successor for each superseded record, no successor for accepted
records, and exactly one accepted terminal per represented topic.

The executable specification is
[`InternalModelDecisionRecordValidatorSpec`](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelDecisionRecordValidatorSpec.scala).
It covers strict UTF-8/Unicode/JSON and semantic array-order failures, harmless
presentation variations, exact logical/artifact/predecessor revision boundaries,
malformed and null capture, unknown source revisions, complete retained content,
current and historical basis admission, source-condition visibility,
actor/provider separation, chain failures, package inventory/readiness separation, legacy rejection, captured
no-reread behavior, and identity/prose property checks. Parent-owned validation
and acceptance remain separate. Before inspecting decision byte content, the
codec rejects null decision/reference/path/bytes/dependencies, invalid declared
IDs/revisions/roles, wrong decision role, null dependency entries, duplicate or
unsorted dependency IDs and self-dependency. The captured validator rejects
null handoff/decision/realizationpackage via structured operationInvalid, admits
the realization from that same capture, decodes the decision, then requires its
exact selected realization dependency before basis checks. It retains the full
realization and all source/condition visibility checks.

Unknown source revisions remain None, including when every producer-owned
artifact and record revision is known. They are never substituted from those
versions. Declared revisions provide producer provenance, not authentication,
live freshness, historical verification or proof of undeclared payload changes.
No hash or byte-control substitute is computed or compared.
