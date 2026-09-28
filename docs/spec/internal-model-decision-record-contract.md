---
status: accepted
decision_scope: P10-22A--P10-22B
updated_at: 2026-09-28
---

# Internal-model Decision Record Contract

This normative logical and executable contract defines P10-22A--P10-22B's
durable decision and alternative record admission. Its companion is the [Internal-model Decision
Records Design](../design/internal-model-decision-records.md). The
[Internal-model Package Contract](internal-model-package-contract.md) remains
authoritative for the package manifest, inventory, artifact digest, and
dependency graph. The [Internal-model Semantic Realization
Contract](internal-model-semantic-realization-contract.md) remains authoritative
for the selected realization, its scope, semantic identities, source references,
and conditions. The [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) remains the sole source
of CCDM meaning.

This contract adds no canonical semantic fact, CCDM lane, public API, approval,
CML-write permission, rehydration behavior, or proposal-promotion behavior. It
specifies a retained decision record only. P10-22B fixes its closed artifact
grammar, schema/profile/version choices, raw-byte syntax, decoding boundary,
structured failures, and executable proof.

## 1. Scope and selected admission

A decision ledger SHALL be represented by one present package artifact with the
existing `decision` role. It is separate from realization and projection
artifacts. Existing V1 package role vocabulary, role multiplicity, structural
validity, and public package validation behavior remain unchanged.

A package with no decision artifact can be structurally valid. It is not,
therefore, evidence that decision readiness is met. Decision admission requires
exactly one selected present decision artifact and exactly one selected present
realization artifact. Multiple or absent selections fail decision admission
explicitly. The selected decision artifact SHALL directly depend on the selected
current realization in the manifest; source snapshots remain its transitive
basis. A record SHALL not bind its own package digest, require a historical
artifact to remain present, or create a hash cycle.

Admission begins from one manifest-verified inventory/digest authority pass. A
future reader consumes the captured immutable decision bytes, selected current
realization, and selected source snapshots yielded by that pass. It SHALL not
reopen or rescan the manifest or artifacts, fetch live sources, consult Git or
an archive, or repair/reconstruct missing information.

## 2. Durable logical record content

The ledger has a stable ledger identity. Each retained record has a stable
decision identity, a bounded topic identity, and the exact admitted Component,
projection-context, and selected-Use-Case scope. Decision, topic, selected
choice, and alternative identities are record-local attribution/navigation
identities. They are not CCDM element or relationship identities and SHALL not
be promoted to CCDM facts.

Every accepted or superseded record SHALL retain all of the following logical
content:

| Required retained content | Admission meaning |
| --- | --- |
| State | The explicit state is `accepted` or `superseded`; no implicit state is derived from placement, time, or content. |
| Human actor and provenance claim | An accountable human identity and role, plus attributable human-decision source identity, exact content digest, supplied locator, and supplied revision. |
| Choice and rationale | The selected choice identity and description and a nonblank rationale. |
| Affected identities | A nonempty set of exact affected CCDM element and/or relationship identity references. |
| Considered evidence | Evidence identities with their own source kind, identity, exact content digest, supplied locator/revision, and stated conditions. |
| Decision context | Explicit assumptions, conditions, and limitations. |
| Relevant alternatives | Each retained rejected alternative's identity, description, and rejection rationale. |
| Basis binding | Exact realization artifact ID, realization identity, raw-realization-byte SHA-256, and the exact record scope. |
| Supersession link | An optional explicit predecessor decision identity, subject to section 4. |

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
exact artifact ID, realization identity, raw-byte SHA-256, and scope. Its
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
or hashes. Decision identities are unique within the ledger; successive
decisions about the same bounded topic share that topic identity. Choice and
alternative identities are unique within their owning record, so retained
history may reference the same topic and choice across successive records. A
predecessor link must refer to a retained record in the same scope and topic.

- A superseded record has exactly one successor.
- An accepted terminal record has no successor.
- A predecessor link cannot be self-referential, dangling, cyclic, or forked.
- A topic has no duplicate accepted terminal record.
- Independent topics may each have an independent accepted terminal record.

Successive records may have different realization-byte hashes without requiring
multiple present realization artifacts or changing package multiplicity. The
old record retains its old realization identity/hash, affected identities,
evidence, rationale, and provenance. When that old basis is not the verified
current basis, its basis status SHALL be explicitly **historical-unverified**:
only record metadata and the supersession chain were checked; the absent old
bytes, targets, and source facts were not reverified. Historical-unverified
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
| Current accepted record has a wrong realization artifact/identity/hash/scope, or affected/current source/condition references are unknown, cross-scope, or unresolved | Fail decision admission without rebinding or repair. |
| Explicit same-topic predecessor chain has self, dangling, cyclic, forked, duplicate-successor, or duplicate-terminal relation | Fail decision admission. |
| Retained predecessor has a different old basis and is explicitly historical-unverified while the current terminal meets current-basis rules | Admit the current terminal and retain the predecessor as historical attribution only. |
| Retained old basis is presented as current, reverified, approval, or a waiver of current-basis validation | Fail decision admission. |
| Manifest/artifact content is malformed, absent, ambiguous, duplicated, tampered, or mismatched in the captured authority pass | Fail closed. |

## 6. Exclusions and executable boundary

This contract neither authenticates an actor nor decides truth, approval,
proposal promotion, CML mutation, source freshness, rehydration, runtime/API
behavior, publication, deployment, or a history archive. It introduces no
source fetching, live lookup, Git lookup, or historical-basis inference.

The closed portable artifact grammar and executable admission proof below show
preservation of retained logical content and fail closed for malformed,
tampered, ambiguous, and current-mismatch cases without rereading the verified
authority inputs. This draft does not claim P10-22, Phase 10.2, or any
implementation step is complete.

## 7. Closed `ccdm-decision-records-v1` artifact

The `decision` artifact is canonical UTF-8 JSON despite the conventional
`decisions.yaml` filename. Its bytes SHALL have no BOM, duplicate object key,
insignificant whitespace, or byte after exactly one final LF. Every object key
is ASCII and emitted in ascending UTF-8-byte order. Every supplied string is a
well-formed Unicode scalar sequence; required identity and prose strings are
nonblank and retain their supplied spelling without trim or normalization.
`sha256` is exactly `sha256:[0-9a-f]{64}`. `locator` and `revision` are required
nullable fields: each is either `null` or an exact nonblank string.

The root has exactly `profile`, `schemaVersion`, `ledgerIdentity`, `scope`, and
`records`. `profile` is `ccdm-decision-records-v1`, `schemaVersion` is `1.0`,
and `ledgerIdentity` is a nonblank opaque stable identity. `scope` has exactly
the existing realization fields `componentIdentity`,
`projectionContextIdentity`, and `selectedUseCaseElementIdentity`. There is no
package digest or self digest in this artifact.

Each record has exactly `decisionIdentity`, `topicIdentity`, `state`, `actor`,
`provenance`, `selectedChoice`, `rationale`, `affectedTargets`,
`consideredEvidence`, `assumptions`, `conditions`, `limitations`,
`realizationConditionIds`, `rejectedAlternatives`, `basis`, and `supersedes`.
`state` is `accepted` or `superseded`; `supersedes` is `null` or an exact
predecessor decision identity. `actor` has `kind`, `identity`, and `role`, with
`kind` exactly `human`. `provenance` is the closed source shape `authority`,
`identity`, `locator`, `revision`, and `sha256`, with authority exactly
`human-decision`. These fields are attributable claims, not authentication or
approval.

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

`basis` has exactly `realizationArtifactId`, `realizationIdentity`, `sha256`,
`scope`, and `status`. Its scope equals the ledger scope even for retained
history. Status is `current` or `historical-unverified`; its digest is the raw
selected realization artifact bytes, never the package or source-content
digest. `records` is nonempty and sorted uniquely by decision identity.
Affected targets sort uniquely by `(semanticIdentityKind, semanticIdentity)`;
evidence, alternatives, and condition-ID collections sort uniquely by their
respective identities. All opaque identity comparison uses unsigned UTF-8
bytes. Assumptions, conditions, and limitations—including those on evidence—are
ordered nonblank prose and preserve both supplied order and duplicates.

`InternalModelDecisionRecordCodec` parses this grammar with duplicate-key
rejection and accepts only an exact typed canonical re-encoding. Its pure
encoder deterministically sorts identity collections while preserving ordered
prose. It neither repairs bytes nor selects a current decision, winner,
approval, or source authority.

## 8. Captured admission and executable specification

`InternalModelPackageValidator.verifiedDecisionRecords` performs one package
authority pass, selects exactly one present decision and realization, and
requires their direct manifest dependency. It supplies immutable decision bytes
and the captured realization package to
`InternalModelDecisionRecordValidator.validateVerified`; that path first
admits the captured semantic realization and then decodes the captured decision
bytes. It does not reopen package files, fetch a source, inspect Git or an
archive, mutate, rebuild a projection, or approve a decision.

Every basis scope equals the selected realization scope. Exact-current means
that artifact ID, realization identity, raw-byte SHA-256, and scope all equal
the captured realization. Status is `current` if and only if that tuple is
exact-current. A different basis is retained only as a `superseded`,
`historical-unverified` record; it does not resolve old targets, sources,
conditions, or hashes. An accepted terminal is exact-current.

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
It covers canonical byte/Unicode/order failures, complete retained content,
current and historical basis admission, source-condition visibility,
actor/provider separation, chain failures, package compatibility, captured
no-reread behavior, and identity/prose property checks. Parent-owned validation
and acceptance remain separate.
