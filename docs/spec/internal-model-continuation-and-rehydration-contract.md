---
status: authored-unvalidated
decision_scope: P10-40–P10-44 / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-02
---

# Internal-model Continuation and Rehydration Contract

This required target defines `ccdm-resume-v2` / `schemaVersion="2.0"`
and package-only reconstruction for P10-40–P10-44. Its companion is the
[Continuation and Rehydration Design](../design/internal-model-continuation-and-rehydration.md).
The [Typed Control Contract](internal-model-typed-control-contract.md) owns
management versions and the subject/carrier distinction. The typed cursor,
one-capture reconstruction, and distinct-JVM executable specifications are
authored. Parent validation and independent review remain pending; this is no
current PASS, accepted Step, release, or Phase closure.

## 1. Authority and derived cursor

The [Package Contract](internal-model-package-contract.md) governs the consuming
project's `src/main/internal-model/`, project identity, closed safe
inventory, explicit revisions, and exact dependencies. The
[Semantic Realization Contract](internal-model-semantic-realization-contract.md)
and [Projection Continuity Contract](internal-model-projection-continuity-contract.md)
retain CCDM ledger/reconstruction semantics through their current typed
references. The [Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
remains the sole semantic authority for all eight Phase 9 projections.

`resume.yaml` SHALL be derived over admitted records and exact versions.
It cannot authorize CML mutation, a human approval, a source fact, repository
acceptance, or an external operation. The local single-writer producer advances
explicit carrier/artifact versions when control meaning changes; the cursor
does not authenticate content or prove undeclared mutation/freshness.

Continuation requires exactly one present manifest `role=resume` entry
at the exact path `resume.yaml`. Missing, absent, duplicate, or differently
located selection fails this contract. This is a content/readiness constraint,
not a change to structural package role multiplicity.

## 2. Strict cursor schema

The cursor uses strict UTF-8 JSON under its YAML filename. Malformed syntax or
UTF-8, BOM, duplicate members at any depth, YAML-only syntax, missing/extra
fields, wrong types, unsupported schema/profile, and trailing non-JSON data
reject. Writers may use deterministic keys and one LF. Readers SHALL accept
harmless object-key order/insignificant JSON whitespace variation.
No terminal LF is required, and multiple terminal JSON whitespace characters
are valid. All member names and string values must contain Unicode scalars;
escaped unpaired surrogates reject before any nested reference is serialized
for admission. Equivalent valid scalar escapes remain valid presentation.
Canonical re-encoding or whole-file byte equality establishes no admission,
identity, eligibility, or continuation permission.

The root has exactly these required fields:

```text
acceptanceCriteria, blockers, currentStage, invalidationChecks,
lastCompletedAction, nextPermittedAction, packageId, packageRevision,
preconditions, profile, schemaVersion, selectedArtifacts
```

| Field | Type and required meaning |
| --- | --- |
| `schemaVersion` | String exactly `"2.0"`. |
| `profile` | String exactly `"ccdm-resume-v2"`. |
| `packageId` | Lowercase UUID exactly equal to the admitted manifest's package ID. |
| `packageRevision` | Positive producer-supplied JSON integer fitting `Long`, equal to the exact current carrier revision; no string, sign, exponent, decimal point, leading zero, or default. |
| `selectedArtifacts` | Unique array of exact non-resume `ArtifactReference` values under section 3, ordered by artifact ID. |
| `currentStage` | ASCII token describing recorded progress, not readiness authority. |
| `lastCompletedAction` | ASCII token or JSON null; null means none established. |
| `nextPermittedAction` | ASCII token or JSON null; a token grants no action before P10-43's independent check. |
| `blockers` | Unique ordered array of section 4 condition records. |
| `preconditions` | Unique ordered array of section 4 condition records. |
| `invalidationChecks` | Unique ordered array of section 4 condition records. |
| `acceptanceCriteria` | Unique ordered array of section 4 condition records. |

ASCII tokens use `[A-Za-z0-9][A-Za-z0-9._:-]*`; empty, whitespace-containing,
and non-ASCII tokens reject. Stage/action syntax alone is not semantic
admission. P10-43 supplies the closed vocabulary and complete eligibility
matrix in section 7; neither token nor an empty blocker list grants permission.
There is no `ccdm-resume-v1` / schema `1.0` reader, migration, inferred
version, compatibility alias, or fallback.

## 3. Exact selections and dependency ownership

Each selected item has exactly `artifactId`, `artifactRevision`, and
`role`, using the distinct ArtifactReference type. Its revision is a
positive producer-supplied `Long`. The array has unique IDs and is ordered
by exact UTF-8 artifact ID. Each reference resolves to a present artifact with
the exact declared revision and closed role. Unknown, absent, wrong-version,
or wrong-role selections remain attributed inconsistent state.

Selections bind every artifact consumed by the later P10-43 action matrix,
including the selected realization, continuity, source basis, and any consumed
decision/open-issue/review/approval evidence. P10-43 owns the action-specific
set; this contract does not choose a sibling approval or invent missing mappings.

The reconstruction core requires the exact continuity projection, selected
realization, and every source snapshot in that realization's typed dependencies.
Unrelated optional snapshots, decisions, and approvals need not be consumed by
that core. Extra selected artifacts must satisfy exact version/role/presence,
but their bytes remain raw and semantically unadmitted until the corresponding
validator is independently composed. The core set is not the later action matrix.

The cursor selects no `resume` artifact and contains no complete manifest,
hash, or encoded-content control token. Dependency direction is explicit:
semantic subject -> review -> approval; the cursor selects required existing
records. Review/approval bind their exact subject/evidence rather than recursively
reviewing the carrier or cursor. A cursor-only carrier revision change leaves
an unchanged subject intact. A changed subject reference or selected semantic
artifact revision requires new subject/review evidence and invalidates approval
applicability to that changed basis; old approval is never rewritten/rebased.

## 4. Bounded condition records

A condition record has exactly `code`, `detail`, and optional
`artifactId`. Code is a stable ASCII token. Omission of artifactId means
no artifact reference; explicit null is not that form and rejects. A supplied
ID resolves to one exact selected ArtifactReference and admitted inventory
entry, not an inferred version, path, label, or sibling.

Within each list, codes are unique and ordered by exact UTF-8 code. Detail is
string-valued explanatory text, including blank/Unicode text, without an
invented length/blankness restriction. It is no evidence payload, identity,
selection key, executable instruction, or eligibility authority. Prose cannot
supply a missing mapping or choose an evidence winner.

An explicit empty list records no item, without proving completeness,
precondition satisfaction, absence of blockers, current freshness, or acceptance.
Missing, conflicting, stale, unauthorized, ambiguous, or malformed evidence
remains explicit and attributable. No condition is erased, inferred, promoted,
or replaced by preferred evidence.

## 5. One capture and package-only reconstruction

P10-41/P10-42 reconstruction SHALL first admit the consuming-project identity,
typed manifest, inventory, declared revisions, and dependencies. It then admits
the exact selected realization/source basis and continuity binding through
existing semantic owners. A recognizable cursor cannot replace this admission.

The authored implementation preserves these internal boundary responsibilities:

- `InternalModelResumeCursorCodec.decode(bytes: Vector[Byte])` returns
  `Either[String, InternalModelResumeCursor]`; `encode(cursor)` returns
  strict JSON using deterministic keys/LF. The cursor has only its twelve
  declared fields, with typed `InternalModelPackageId` and
  `Vector[InternalModelArtifactReference]`; it has no byte cache or sidecar.
  Ordinary raw resume payload stays in the captured artifact.
- `InternalModelPackageValidator.verifiedContinuation(projectRoot: Path)`
  captures once, deriving package context, existing continuity handoff, and all
  inventoried artifact contexts/optional raw payload from that capture.
- `InternalModelRehydrationValidator.validate(projectRoot: Path)` obtains
  that handoff once and calls pure `validateVerified(handoff)`.
  Both return `Consequence[InternalModelRehydratedState]`, preserving
  attributed structured boundary failures, including
  `Consequence.operationInvalid` for invalid admission.

Pure validation checks captured typed context/inventory correspondence,
the single resume entry, exact package identity/carrier revision, selections,
and the core consumed set; it delegates semantic reconstruction to
`InternalModelProjectionContinuityValidator.validateVerified`.
It performs no control-byte equality check or second file read.

`validateVerified` first rejects null or invalid typed graphs with structured
`operationInvalid`, validates package/identity/reference grammar, and invokes
`InternalModelPackageValidator.validateCapturedContext`. The package context's
complete typed inventory must match every retained artifact context, including
reference, path, requiredness, dependencies, order, and presence. Presence must
equal raw payload availability and all required entries must be present.

`InternalModelPackageValidator.selectCapturedContinuity` derives the actual
semantic handoff from captured inventory. The caller's duplicated continuity
handoff must match the selected projection and realization's full reference,
path, requiredness, and exact dependencies, plus the complete source snapshot
metadata and availability. Payload bytes of these duplicated handoffs are not
compared or used as authority. Semantic reconstruction receives the handoff
derived from captured inventory, so a changed duplicate byte sidecar cannot
replace the actual captured source. Malformed or witness-inconsistent captured
semantic payload is rejected by its existing semantic owner.

This pure construction is metadata admission, not authenticity, source
reauthorization, live freshness, or detection of undeclared content mutation.
Producer-declared revisions are not inferred from content. Changed raw
unadmitted payload with unchanged metadata remains raw; valid presentation
changes need no content-derived identity. A retained capture survives actual
filesystem removal or replacement. A fresh capture of missing or malformed
input rejects for that attributed absence or semantic failure, not a digest
mismatch.

`InternalModelVerifiedContinuationArtifact` retains inventory context and
optional raw bytes; `InternalModelVerifiedContinuationPackage` retains
`packageContext`, `continuityPackage`, and the artifact vector.
`InternalModelRehydratedState` retains package context, admitted cursor,
complete existing continuity value, and captured artifacts. Neither pure entry
point reopens project/package/source/provider paths after capture. Selected
non-core review/approval material remains raw and unadmitted until exact
admission with independent evidence; retaining it is no approval claim.

Reconstruction SHALL retain exact Component/projection-context/selected-Use-Case,
element/relationship IDs, endpoints, roles, directions, canonical/enrichment
separation, complete source attribution, conditions, and realization/binding
sidecars. All eight complete Phase 9 values are required:
`MonoKotoProjection`, `UseCaseCommunicationProjection`,
`EntityModelProjection`, `EventModelProjection`,
`StructureViewProjection`, `ClassificationViewProjection`,
`WorkflowProjection`, and `StateMachineProjection`.
Dashboard consumes these admitted values/evidence, not a cursor-local model.

Existing [decision](internal-model-decision-record-contract.md),
[open-issue](internal-model-open-issue-record-contract.md),
[review](internal-model-candidate-review-binding-contract.md),
[human-input](internal-model-candidate-human-approval-contract.md), and
[lifecycle](internal-model-candidate-approval-lifecycle-contract.md) semantics
remain required with typed management migration. Their exact caller selections,
independent rule/provider versions, actual attributable human input, and source
evidence cannot be replaced by persisted claims. Missing required evidence or
source versions stays unknown/incomplete. Rehydration does not read live CML,
mutate source, authorize access, or claim live freshness; Phase 10.5 owns drift.

## 6. Distinct-JVM executable evidence obligation

`InternalModelFreshProcessRehydrationSpec` authors the current typed scenarios
using the unchanged test-only `InternalModelFreshProcessProbe`.
A probe receives only one consuming project root and invokes production
rehydration. The consuming copy contains only `project.yaml`, manifest,
and present inventoried files, without CML, `target/`, chat/provider state,
or retained DB. Child execution uses an explicit `java.home` JVM,
absolute forked Test runtime classpath, private working/home/temp directories,
and cleared environment. Bytecode/jars are executable code, not recovered state.

A different PID and bounded terminal exit are required; the harness waits at
most 30 seconds. Private stdout/stderr files avoid pipe deadlock; cleanup
terminates only its owned child and removes only its test-owned roots.

Test transport may compare complete semantic results against independent
existing continuity admission: full ledger, source/condition attribution,
binding sidecars, all eight projections, typed cursor/context, and retained
payload. Supported scalars, Options, ordered collections, and recursive Product
fields/type names must remain explicit; unsupported values fail the harness.
Counts, labels, or stringified values cannot replace complete semantic evidence.
Report bytes are not compared for admission or identity; complete decoded
semantic values establish the test expectation. The report is test-only
evidence transport, never a public codec, persisted
package artifact, semantic model, production input, or encoded identity protocol.

Authored scenarios retain substantive source-backed witnesses, bounded ScalaCheck label/input permutations
(the existing harness bounds four successful samples), absent required source
snapshots, wrong artifact versions/roles, subject/carrier conflicts, malformed
structure, and harmless JSON presentation variation. Parent-run serial SBT must
still execute the real child scenarios to establish proof.
Production rejection must have an attributed diagnostic and distinct exit
status from harness failure (success 0, rejection 2, harness failure 3).
No hash-based test checks are required. Observe complete semantic/source and
ordinary payload preservation, including original CML and no consuming CML
creation, without making whole-file equality continuation authority.

## 7. Failure categories and P10-43 gate

Invalid serialized shape, unsupported schema/profile, duplicate members/
selections/condition codes, wrong types, invalid tokens, incorrect array
structure, or unsafe/unclosed package inventory fails closed. Harmless JSON
key order/whitespace variation is valid. Missing/duplicate resume selection
fails continuation content admission.

Contradictory package identity/revision, artifact revision/role, scope, subject,
condition references, or recorded state yields attributed inconsistency.
Missing required source version, decision, evidence, prerequisite, or mapping
yields attributed incompleteness. Semantic realization/continuity failure retains
its own attributed reason. No guessing, repair, fallback, latest-record
selection, inferred mapping, or synthetic readiness follows.

### 7.1 Exact recorded stage/action matrix

The runtime-private gate is authored under `P104-TYPED-ACTION-GATE-001` revision
2. Its executable specification is
[InternalModelContinuationActionGateSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelContinuationActionGateSpec.scala).
Parent validation, independent review and Step acceptance remain pending.

| Recorded stage | Exact recorded next action | Required independently derived predicates |
| --- | --- | --- |
| `model-ready` | `inspect-projections` | `model-admitted`, `source-versions-known`, `decisions-complete` |
| `candidate-ready` | `review-candidate` | The model row predicates plus `mappings-complete` |
| `review-ready` | `request-human-decision` | The candidate row predicates plus `review-admitted` |
| `human-decision-recorded` | `handoff-approved-candidate` | The review row predicates plus `human-approved`, `no-approval-blocking-issues` |

Stage and next action SHALL be the same exact row. Unsupported stage/action or
mismatched pair is inconsistent; null next action is incomplete. A nonnull
`lastCompletedAction` SHALL be one of these four action tokens. Retaining it
proves no execution and establishes no inferred preceding action, history or
automatic transition. Every row predicate runs even with empty condition arrays.

Eligibility permits point-in-time inspection, presentation for review, requesting
a human decision, or read-only handoff of recorded work. Calling the gate does
none of those actions and grants no authentication, repository acceptance,
live-source applicability, CML mutation, or successor Phase execution.

### 7.2 Independent request and existing semantic owners

`evaluate(projectRoot: Path, request: InternalModelContinuationRequest)` obtains
`verifiedContinuation` exactly once and calls pure
`evaluateVerified(handoff: InternalModelVerifiedContinuationPackage, request)`.
Both return `Consequence[InternalModelContinuationReport]`. The pure entry first
runs existing rehydration, retaining its complete all-eight state and captured
inventory, and derives actual continuity through `selectCapturedContinuity`.
Every candidate/diff/review/human/decision/issue wrapper is composed from that
actual captured metadata and payload; separately mutable Admission-shaped
objects supply no authority. No path, provider, DB, chat or live source is read
after capture.

The immutable, nonserialized runtime-private request has exactly these fields:

```text
packagereference: InternalModelPackageReference
carrierrevision: Long
realizationreference: InternalModelRecordReference
scope: InternalModelSemanticScope
candidateartifact, semanticdiffartifact, reviewartifact, approvalartifact:
  Option[InternalModelArtifactReference]
executionbasis: Option[InternalModelCandidateReviewExecutionBasis]
humandecision: Option[InternalModelCandidateHumanApprovalInput]
requireddecisions: Option[Vector[InternalModelContinuationDecisionRequirement]]
requiredmappings: Option[Vector[InternalModelContinuationMappingRequirement]]
```

The caller separately admits package/project/carrier/realization/scope,
requirements, rule/provider versions and actual human input. The gate neither
authenticates that caller nor proves undeclared requirement completeness.
`None` means unsupplied; `Some(Vector.empty)` explicitly records the supplied
empty requirement set and cannot be deduced from an empty persisted ledger.

A decision requirement has exact full Decision `artifactreference`, logical
`recordreference`, `topicidentity`, `choiceidentity`, and distinct
`affectedtargets`. Every row requires an independently supplied set. The exact
record/topic must exist in that exact admitted ledger with the exact logical
revision, choice and affected targets, Current basis and Accepted state.
Missing artifact/record/topic is incomplete; different versions/choices/targets,
historical/superseded required records and contradictory current basis are
inconsistent. No sibling record supplies a missing requirement.

A mapping requirement has `targetid`, `mappingid`, and exact existing
`semantictarget`. Candidate/diff rows require an independently supplied set;
missing target/mapping is incomplete and different semantic kind/identity is
inconsistent. Explicitly empty requirements still run existing complete
candidate/diff assertion, condition, effect and target admission.

Null graphs/options/collections/elements, malformed typed references/revisions,
scope/requirement strings, duplicate requirements, wrong selected roles and
aliased selected reference roles fail with structured `operationInvalid`.
Unneeded independent evidence may be omitted. Every supplied full selection
must resolve uniquely to present exact captured metadata and be cursor-selected.
Unknown ID, wrong version/role, or absence contradicting selection is
inconsistent. Every actually consumed artifact and its full dependency closure
must be cursor-selected by its entire reference, never a bare ID subset.

Candidate/diff admission calls existing `InternalModelSemanticDiffValidator`
over the actual selected candidate/diff; optional candidate-only evidence calls
the existing candidate validator. Review admission composes the same actual
diff/context and calls its owner with independent `executionbasis`; missing
basis is incomplete and invalid/mismatched supplied basis is inconsistent with
the original owner diagnostic. Stored rules/providers cannot substitute for it.
Human admission composes the same actual review and calls its owner with the
separately supplied actual `humandecision`. Missing input is incomplete;
mismatched input is inconsistent. Approved handoff requires an independently
admitted Approved decision with empty unresolved items. Rejected,
ChangesRequested, or nonempty unresolved items block; provider target state
cannot synthesize a human answer.

Every present Decision and OpenIssue role entry is consumed, cursor-selected and
semantically admitted through its existing owner over actual captured payload
and realization. An unselected or malformed present ledger cannot hide blockers.
Absent optional unrequired ledgers remain inert. Approved handoff blocks every
admitted issue whose `blocking.semanticApproval` is true; cmlProjection,
application, validation, evidence, alternatives, conditions and annotations
remain retained. Inspection/request rows can retain unresolved issues without
resolving them.

Every consumed SourceSnapshot in the full dependency closure passes the existing
`validatedSnapshotKind` owner. The gate then reads that already admitted
envelope's explicit `source.revision`; null is incomplete with exact snapshot
reference and dimension. Source, artifact, logical and carrier versions remain
independent. No fabricated live observation, freshness comparison, byte-control
identity, cache, hash, default or compatibility adapter is introduced.

### 7.3 Closed condition interpretation

Only the seven positive codes in the matrix may appear in preconditions,
invalidationChecks and acceptanceCriteria. Only `approval-blocking-issues` and
`human-unresolved-items` may appear in blockers. Unknown/wrong-list codes are
attributed inconsistent records. Detail text is retained and never interpreted
or executed. Every positive list uses the same derived predicate; additional
recorded predicates add their actual independent evidence requirements.

`model-admitted` means complete actual rehydration; `source-versions-known`
means a known declared version for every consumed source; `decisions-complete`
and `mappings-complete` mean actual owner admission plus the exact independent
requirements; `review-admitted` means actual review with independent basis;
`human-approved` means independently admitted Approved input with empty
unresolved items; `no-approval-blocking-issues` means every present issue ledger
is admitted and no issue blocks semantic approval.

`approval-blocking-issues` is true if any admitted issue blocks semantic approval.
`human-unresolved-items` is true if independently admitted actual human input
retains unresolved items. A true recorded blocker makes the action inconsistent;
unavailable prerequisite evidence is incomplete or retains its contradictory
owner failure. A derived false blocker may remain recorded as cleared without
rewriting cursor history.

Optional `artifactId` only attributes the complete global predicate. It must
name a relevant consumed exact cursor selection: realization/continuity for
model, SourceSnapshot for source, Decision for decisions, selected candidate
for mappings, selected review for review, selected approval for human, OpenIssue
for approval-issue predicates. Wrong owner/selection is inconsistent. Omitted
ID still evaluates the complete predicate; an ID cannot narrow it.

### 7.4 Typed lossless outcome

`InternalModelContinuationEligibility` is exactly Eligible, Incomplete or
Inconsistent. The immutable report retains eligibility, exact optional action
(`Some` iff Eligible), reconstructed state, complete successful candidate/diff/
review/approval admissions, decision/open-issue admissions with their external
full references, and all problems. Counts or diagnostic strings cannot replace
those semantic values.

Each problem retains its closed kind, dimension, optional full artifact and
logical record references, diagnostic and optional original condition/list
attribution. The kinds are MissingPrerequisite, IdentityMismatch,
RevisionMismatch, ScopeMismatch, SelectionMismatch, UnsupportedStage,
UnsupportedAction, StageActionMismatch, UnsupportedCondition,
ConditionOwnerMismatch, DecisionMismatch, MappingMismatch,
SemanticAdmissionFailed, BlockingIssue, HumanDecisionNotApproved and
HumanUnresolvedItems. MissingPrerequisite distinguishes incomplete facts from
contradictions without diagnostic-substring classification.

All independently observable missing and contradictory problems remain in
deterministic traversal order. Any contradiction yields Inconsistent; otherwise
any missing problem yields Incomplete; otherwise the exact recorded action is
Eligible. Missing problems survive contradictory precedence. Dependency failure
suppresses only impossible downstream admission and retains unrelated checks.
Invalid request/capture syntax and original core admission failures propagate
structured Consequence failures. Later semantic owner failures retain the
original owner diagnostic as SemanticAdmissionFailed inconsistent problems;
the gate does not reconstruct failures from strings or invent success.

## 8. P10-44 proof and acceptance boundary

P10-44 SHALL prove actual stop, Git commit, transfer to another checkout of the
same project, and fresh-process resume using only project-owned package state.
The proof retains stable semantic identities, complete source/condition ledger
and sidecars, and all eight projection results, while rejecting incompatible,
missing, ambiguous, contradictory, or unauthorized evidence. It covers changed
cursor facts, identity/carrier/artifact revisions and roles, semantic subject
changes, strict structural failures, and use-time action rechecks without
CML mutation or approval effects.

The authored `InternalModelResumeCursorCodecSpec`,
`InternalModelRehydrationValidatorSpec`, source-backed
`InternalModelContinuationFixture`, and fresh-process harness are
current-format executable obligations awaiting validation. The fixture uses
snapshot `2.0`, realization `ccdm-realization-v3` / `3.0`, continuity
`ccdm-projection-binding-v3` / `3.0`, manifest `2.0`, and resume `2.0`.
Logical record, artifact, carrier, and source-owned revisions are allocated
independently. Full ledger/sidecar and all-eight semantics remain mandatory.
Captured-value reconstruction after filesystem removal/replacement specifies
that pure validation does not reopen paths; it is not fresh-JVM proof.
A copied fixture and terminated child do not prove actual Git commit or
checkout transfer. Prior compatibility/noncanonical-byte/hash scenarios are
not target acceptance criteria or a current PASS.

### 8.1 Authored actual local Git and recorded-action proof

The test-only
[InternalModelDurableHandoffSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelDurableHandoffSpec.scala)
authors D1–D12 under `P104-TYPED-DURABLE-HANDOFF-001` revision 1.
The five separate Fixture, Input, Support, Probe and Spec files own package
preparation, independent request transport, bounded private process/Git setup,
fresh rooted evaluation and observable behavior respectively. This mechanism
is authored-unvalidated; actual parent-selected execution remains required.

The source-backed all-eight inspection family and rich two-target approved
handoff family remain separate obligations. The former independently declares
the continuity project/package, realization logical revision and scope and
compares all eight values, complete source/condition ledger, associations,
sequence keys, enrichment and sidecars with existing continuity admission.
The latter uses independently authored human input, rule/provider basis,
exact current decision and target/mapping requirements, complete subject,
binary candidate CML and rich review/issue evidence. Stored package approval
and review are never read to manufacture those external expected inputs.

Each test creates a fresh private work root under
`target/internal-model-durable-handoff/work/`. The producer contains only
consuming `project.yaml`, manifest and actual present inventoried files,
including optional present raw evidence. Original CML, build outputs, chat,
provider and retained DB state are excluded. A producer JVM evaluates to a
terminal report before the private Git repository is initialized and those
exact paths are staged and committed. Native Git HEAD is obtained directly;
the harness computes no product/workflow hash. An independent local clone
uses `--no-local --no-hardlinks --no-checkout`, then checks out that exact
native commit detached. Same commit, exact complete tracked path set and
absence of object alternates are required. The owned producer repository is
removed and confirmed absent before the distinct consumer JVM evaluates.
These ephemeral commits are proof fixtures and no repository acceptance or
release commits. No network URL, push or global/user Git mutation is used.

Absolute `/usr/bin/git` receives argument arrays, cleared environment, private
home/temp directories, empty hooks, disabled signing, explicit test author
identity and isolated system/global configuration. Both Git and JVM subprocesses
use private stdout/stderr files, wait at most 30 seconds, and terminate only
their owned child on timeout. JVM invocation supplies explicit `java.home`,
available absolute forked Test classpath and separate cwd/home/temp with empty
environment. Containment checks bound every mutation/removal to strict
descendants of the captured private root with no-follow ancestry/walks.
Unavailable capability or containment failure fails the specification; no
skip or directory-copy-only substitution can establish proof.

### 8.2 Independent test input and complete action evidence

The separate test Input helper transports exactly the twelve current request
fields from section 7.2. Every field is required; JSON null transports None
and an empty array transports Some(empty) requirement vectors. Scope retains
its exact three camelCase fields; decision and mapping requirements retain
their full artifact/logical references and explicit semantic target fields.
Existing typed-reference codecs own reference syntax, and the existing human
codec transports a current record constructed from independently supplied
actual human input. Rules/providers retain exact two-field identities/versions.
Strict UTF-8 JSON rejects duplicate/extra/missing/wrong-type fields and
malformed transport while accepting harmless current JSON presentation.
This is no production/public codec, package artifact, semantic model or human
authentication. The independent input file is outside both project roots and
is never committed. Persisted claims cannot supply it.

The durable Probe requires exactly two absolute arguments: consuming root and
external input path. It decodes caller input and calls production rooted
`InternalModelContinuationActionGate.evaluate` once. Its exact report fields
are `status`, `pid`, `result`, `failure`. Eligible exits 0; typed Incomplete or
Inconsistent exits 2 and retains the complete structural report. Structured
production rejection exits 2 with null result and original attributed
origin/conclusionType/diagnostic. Harness failure exits 3 with null result and
origin/errorType/diagnostic. The existing fresh-process serializer adds only
`actionReport`; its one-argument rehydration behavior and complete recursive
Product/scalar/Option/ordered-collection transport remain intact.

Decoded full reports are compared as structural values, preserving ordinary
binary payload and every successful admission. Producer reports can be test
regression expectations but never consumer authority: every actual consumer
reevaluates its own rooted package with separately supplied input. Negative
scenarios assert explicit production outcome/problem kinds and dimensions,
including missing snapshots/source versions, changed cursor actions/conditions,
full carrier/artifact contradictions, complete subject/input contradictions,
missing independent human/basis/requirements, nonapproved/duplicate unresolved
human records, semanticApproval issues, malformed/old evidence and harmless
current layout. Four bounded ScalaCheck samples preserve generated labels and
opposite encounter permutations through actual Git and distinct JVMs.
No action execution, CML application, lifecycle/live-source permission or
successor authority follows from an eligible point-in-time report.

All P10-40–P10-44 executable obligations remain: cursor, package-only
reconstruction, fresh CBD process with full Phase 9 state, independent recorded
action eligibility, and durable stop/commit/transfer/resume proof. Given/When/
Then, `should` grouping, and active ScalaCheck remain required. Required
parent validation, independent review, accepted Step commits, one complete
Phase review, and final Phase release remain pending. No checklist item,
Step acceptance or Phase is completed by this documentation or authoring.

Public/Web APIs, canonical CML writes, retained DB/provider recovery, packaging,
publication, deployment, and successor Phase implementation remain outside
this boundary. Phase 10.5 owns live-source change/CML gating; Phase 10.6 owns
policy/exclusion. No legacy reader/adapter/default/version inference is added.
