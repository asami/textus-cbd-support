---
status: authored-unvalidated
decision_scope: P10-40–P10-44 / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-02
---

# Internal-model Continuation and Rehydration Design

The [Continuation and Rehydration Contract](../spec/internal-model-continuation-and-rehydration-contract.md)
owns the `ccdm-resume-v2` / `2.0` target, fields, failures, and
executable obligations. The [Typed Control Contract](../spec/internal-model-typed-control-contract.md)
owns versions and the semantic-subject/carrier boundary. This design records
settled responsibility and dependency decisions. Typed core and distinct-JVM
specifications are authored; parent validation, independent review, and all
acceptance/release transitions remain pending.

## Purpose and authority flow

A project-owned package supplies recorded evidence to a fresh process. A
derived cursor records where work stopped and which facts must be checked.
Its authority comes from exact admitted records and independently required
evidence rather than cursor prose or serialization.

```text
consuming project.yaml + typed manifest + captured inventoried payload
  -> project-bound package/inventory/version admission
  -> selected source snapshots + realization + continuity binding
  -> existing CCDM ledger/sidecars + all eight Phase 9 projections
  + exact admitted decision/open-issue/review/approval evidence when required
  + derived cursor naming exact carrier revision/non-resume artifact versions
  -> P10-43 independent check of the recorded next action
  -> eligible recorded action or attributed incomplete/inconsistent state
```

CCDM meaning stays with Phase 9. Rehydration retains canonical/enrichment
separation, exact identities/endpoints/roles/directions, source witnesses,
all condition facets, and complete sidecars. It creates no parallel Entity,
Event, Workflow, Mono-Koto, storage-specific, or view-local semantic model.
The [storage-direction journal](../journal/2026/08/2026-08-17-project-internal-model-storage-direction.md)
is historical rationale, not behavioral authority.

## Cursor shape and explicit revisions

Continuation requires one present `resume.yaml` with role `resume`.
The closed fields are acceptance criteria, blockers, current stage, invalidation
checks, last completed action, next permitted action, package ID, exact current
carrier revision, preconditions, profile, schema version, and selected artifacts.
The normative spec fixes their serialized names and types.

Selected artifacts are exact ArtifactReferences with ID, positive producer
revision, and closed role. Each resolves to a present inventory entry; the
cursor selects no resume artifact. Stage/action tokens are only recorded hints
until P10-43 checks its closed vocabulary and complete action matrix.
Condition code/detail/optional artifact ID retain their existing bounded form:
ID resolves to an exact selection; explanatory prose grants no permission.
Empty lists and null actions establish no readiness.

The local single-writer producer allocates identity and advances versions when
control meaning changes. Explicit versions are control provenance, not
cryptographic authenticity or proof of undeclared payload mutation.
Strict JSON rejects ambiguous/missing/extra fields and unsupported schemas,
while harmless key order and whitespace remain acceptable.
Deterministic writer output and retained raw payload are not control identity
or byte-equality protocols. There are no locks, retries, backups, rollback,
legacy readers/adapters, version defaults, or fallback.

## Acyclic subject and control dependencies

The semantic ReviewSubject selects the complete contributing source,
realization, continuity, candidate, semantic-diff, and review-evidence versions.
Review references that subject; approval references the exact admitted review
and subject identities/revisions. The cursor selects required existing
non-resume records. Neither review nor approval recursively reviews the carrier
or cursor.

This direction separates semantic review from later carrier control changes.
A cursor-only carrier revision leaves an unchanged subject intact. Changing
the subject reference or selected semantic input version requires new explicit
subject/review evidence and invalidates approval applicability to that changed
basis. Old approval remains immutable; rebasing it or concealing a changed input
through a subset would change its meaning.

Caller-admitted rule/provider versions and actual independent human input remain
separate evidence. Review `approved` cannot become a human decision.
Persisted approval does not authenticate itself. Explicit supersession cannot
choose the latest sibling or grant successor applicability. None of these
records grants CML write permission.

## Ownership map

| Concern | Owner |
| --- | --- |
| Project/package identity, closed inventory, revisions, dependencies | Package validator and typed package contract. |
| CCDM assertions, scope, witnesses, source anchors, conditions | Semantic-realization admission; Phase 9 owns meaning. |
| Complete realization/binding sidecar and eight projections | Projection-continuity admission and existing Phase 9 constructors. |
| Settled choices and unresolved questions | Decision/open-issue contracts, with typed management migration. |
| Subject, review, actual human input, supersession, applicability | Review/approval/lifecycle contracts and independent evidence. |
| Cursor decode and captured reconstruction | Resume codec and rehydration validator, using one package capture. |
| Dashboard consumption | Existing Phase 9 consumers of admitted identities/attribution/conditions. |
| Closed stage/action vocabulary and eligibility matrix | P10-43 action model, evidence composition and gate, authored-unvalidated. |
| Package-only distinct-JVM reconstruction proof | P10-42 redesigned fresh-process specification/probe. |
| Actual stop/commit/checkout-transfer/resume proof | P10-44, still pending. |
| Live-source drift/CML change gating and packaging policy | Phase 10.5 and Phase 10.6, respectively. |

## One capture and raw-unadmitted records

The package entry captures the consuming project, manifest, inventory contexts,
and optional payload once. It derives the selected continuity handoff from that
same capture. Rehydration's filesystem entry delegates to pure
`validateVerified`; pure validation checks typed references and delegates
semantic reconstruction to existing continuity admission without reopening paths.

The captured artifact inventory owns actual payload. Pure validation checks
package grammar and complete inventory correspondence through
`validateCapturedContext`, then derives the current continuity handoff through
`selectCapturedContinuity`. A duplicated caller handoff contributes only
matching full references, paths, requiredness, dependencies, and source metadata/
availability. Its payload bytes are neither compared for control identity nor
passed as the semantic basis. Reconstructed values therefore come from the
actual captured inventory even when a duplicate sidecar is replaced.

Null or invalid typed graphs fail with `operationInvalid`. Missing required
entries, contradictory presence, wrong revisions/roles, unresolved dependencies,
and ambiguous inventory retain attributed failures. Actual malformed or
witness-inconsistent semantic payload is rejected by the existing owner.
No manifest reconstruction, canonical-byte cache, source reauthorization, or
payload authenticity guarantee is introduced. Valid captured state survives
root deletion/replacement; fresh malformed replacement rejects under its own
semantic contract rather than a digest comparison.

The retained state contains package context, cursor, complete continuity
value, and captured artifacts. The core consumes continuity, realization,
and every selected source dependency. Other selected records stay raw and
semantically unadmitted until their exact validator receives independent evidence.
Keeping raw review/approval payload enables later same-capture admission; it
neither selects a sibling record nor admits a stored claim as human input.

An artifact revision is not a source version. Missing source versions remain
unknown/incomplete, and missing decisions or mappings are never reconstructed
from names, labels, order, chat, provider history, `target/`, or retained DB.
Package-only reconstruction preserves the recorded source basis; it cannot
establish live freshness or implement Phase 10.5 CML mutation.

## Fresh-JVM proof and test transport

The authored fresh-process harness starts production rehydration in a distinct
JVM with only one consuming root argument. That root holds consuming
`project.yaml`, manifest, and inventoried files, with no CML, chat/provider
state, `target/`, or retained DB. An explicit JVM/classpath supplies code
rather than state. Private work/home/temp directories and cleared environment
keep the consumed basis explicit.

The harness checks a distinct PID and terminal status within its 30-second
bound, using private stdout/stderr files. Cleanup owns only its child and
test roots. Successful admission, attributed production rejection, and harness
failure remain distinct outcomes.

Test transport can retain and compare complete semantic values, typed contexts,
source/condition ledgers, full sidecars, all eight projections, and ordinary
payload against independent continuity admission. Explicit supported scalar/
Option/collection/Product forms preserve fields; unsupported forms fail.
This test transport is no production input, public codec, semantic model,
package artifact, or encoded identity protocol. Counts or stringified summaries
cannot replace complete semantic results.
Decoded Product/scalar/Option/collection values are compared; serialized report
byte equality is no admission condition.

Substantive witnesses, bounded label/input permutations, missing sources,
wrong artifact versions/roles, subject/carrier conflicts, invalid structure,
and harmless JSON presentation variation have authored executable scenarios.
The all-eight fixture preserves associations, sequence keys, conditions, and
canonical/enrichment lanes under snapshot `2.0` and realization/continuity
`3.0`, with independently allocated logical, artifact, carrier, and source
versions. Execution through the parent's registered serial SBT runner is still
required to establish proof.
Source/ordinary payload preservation, including original CML and no consuming
CML creation, remains an obligation without hash-based test checks.
Whole-file byte equality cannot become production continuation authority.

## Recorded action and durable handoff boundaries

P10-43 is authored under `P104-TYPED-ACTION-GATE-001` revision 2, with three
runtime-private responsibilities. `InternalModelContinuationAction` defines
the immutable nonserialized request/report and distinct closed stage, action,
condition, outcome and problem vocabulary.
`InternalModelContinuationEvidenceAdmission` composes the existing semantic
owners over actual same-capture inventory, payload and derived continuity.
`InternalModelContinuationActionGate` owns the static matrix, condition-list/
owner interpretation and typed outcome derivation. The
[executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelContinuationActionGateSpec.scala)
authors A1–A18 with adjacent Given/When/Then and ScalaCheck variation. Parent
validation, independent review and acceptance remain pending.

The matrix fixes four exact pairs: model-ready / inspect-projections,
candidate-ready / review-candidate, review-ready / request-human-decision, and
human-decision-recorded / handoff-approved-candidate. Every row requires model
admission, known consumed-source versions and independently supplied exact
decision requirements. Candidate adds actual candidate/diff admission and
independent exact mappings; review adds independently admitted rule/provider
basis; approved handoff adds independently admitted Approved human input with
empty unresolved items and no semantically blocking issue. Unsupported or
mismatched tokens are inconsistent; null next action is incomplete. Last action
retains a supported recorded token but proves no execution, predecessor or
transition. Evaluation never substitutes another row's action.

The rooted `evaluate(projectRoot,request)` entry obtains verifiedContinuation
once and calls pure `evaluateVerified(handoff,request)`. That entry first calls
existing rehydration, preserving complete all-eight continuity and raw capture,
then derives continuity from the actual inventory. Candidate/diff/review/
approval/decision/open-issue wrappers contain that capture's actual metadata
and payload. The gate accepts no caller-created Admission object or callback
to decide truth and never reopens source, package, provider, DB or chat paths.

The caller independently supplies exact stable package reference, carrier
revision, realization logical reference, scope, optional full candidate/diff/
review/approval selections, rule/provider execution basis, actual human input,
and optional decision/mapping requirement vectors. These inputs are separate
from stored claims. None means not supplied; Some(empty) is an explicit
owner-supplied empty requirement set, never an inference from persisted
emptiness. The gate cannot authenticate the owner or establish undeclared
requirement completeness. Current accepted exact decision record/topic/choice/
targets and exact candidate target/mapping semantic kind/ID must satisfy supplied
requirements. History, siblings, names, ordering or defaults cannot fill gaps.

Every supplied full selection resolves exact present inventory metadata. Every
actually consumed artifact and complete dependency closure must be cursor-
selected by its entire reference. Candidate/diff and review/human admission stay
with existing validators, using separately supplied execution basis and actual
human input. Every present Decision/OpenIssue role is admitted and consumed,
including an unselected ledger, so hidden blockers or malformed accounting
cannot be skipped. Optional absent unrequired ledgers remain inert.
All issue annotations and all four blocking facets remain retained; only
semanticApproval blocks approved handoff. Inspection/request retains unresolved
evidence without resolving it or granting CML permission.

Every consumed source passes validatedSnapshotKind, then the gate reads the
already validated explicit source.revision. Missing source version is incomplete
with full snapshot attribution; artifact/logical/carrier versions cannot supply
it. The gate does not invoke or simulate lifecycle/live-source evaluation.
Approval in a portable package is a point-in-time recorded human input, not
current applicability to live CML or successor execution authority.

The seven positive predicate codes are allowed only in preconditions,
invalidationChecks and acceptanceCriteria. The two blocker codes are allowed
only in blockers. The same positive code has the same predicate everywhere,
and recorded extra predicates add actual requirements. True independently
derived blockers prohibit the action; false blockers retain their recorded
history as cleared. Unknown/wrong-list codes and wrong owner/selection
attribution are inconsistent. Optional artifact IDs attribute a complete global
predicate to its relevant consumed owner; they never narrow it. Detail prose is
inert. Empty arrays never bypass row predicates.

The report retains complete successful state/evidence and every attributable
missing/contradictory problem, including original condition and list. Closed
MissingPrerequisite distinguishes incompleteness; contradiction takes precedence
without erasing missing dimensions. Independent checks accumulate deterministically;
dependency failure suppresses only impossible downstream admission. Invalid
request/capture syntax and original core failures propagate structured Consequence;
later semantic failures retain the original owner diagnostic in typed
SemanticAdmissionFailed problems, without parsing strings for taxonomy.
Only Eligible contains Some(exact recorded action). Calling the gate executes
no action, authenticates no person, accepts no repository, writes no CML and
performs no transition. No hashes, byte-control identities, caches, compatibility
adapters, inferred versions or defaults are added.

P10-44 proves actual stop, commit, transfer to another checkout of the same
project, and fresh-process resume. Pure reconstruction after removing/replacing
filesystem inputs proves no reopening, not fresh-JVM recovery. A copied fixture
and terminated child prove neither Git commit nor checkout transfer.

### P10-44 authored durable proof mechanism

`P104-TYPED-DURABLE-HANDOFF-001` revision 1 authors the test-only
[InternalModelDurableHandoffSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelDurableHandoffSpec.scala)
and four supporting responsibilities: Fixture prepares separate substantive
all-eight and rich two-target package families; Input transports independent
caller declarations; Support owns local Git/process lifetime and containment;
Probe invokes the existing rooted action gate once. The existing fresh-process
probe adds `actionReport` using its unchanged complete structural serializer.
No production implementation or public codec changes belong to this proof.

```text
independent source fixture declarations + external complete caller request
  -> package-only private producer + terminal producer JVM evaluation
  -> actual private local Git commit of exact present inventory
  -> independent clone / detached checkout of native returned commit
  -> producer repository removal and absence
  -> distinct terminal consumer JVM + separately decoded external request
  -> fresh rooted production gate report and explicit semantic expectations
```

Git supplies a native revision for a test checkout, not a computed management
identity or product admission token. `--no-local --no-hardlinks --no-checkout`
and detached exact revision selection establish independent transfer; exact
tracked paths and absence of object alternates keep its recorded state source
explicit. No source CML, target/provider/chat/DB state or independent input
file is committed. Test commits remain ephemeral proof fixtures with no real
repository staging, publication or acceptance meaning.

Support bounds each child to 30 seconds, redirects output to private files and
terminates only its owned process on timeout. Absolute Git with command-local
author/signing/hooks configuration and private cleared environment keeps user
Git state outside the fixture. Explicit JVM/classpath and separate child
cwd/home/temp with empty environment supply executable code and isolate
retained state. Every mutation/removal is a strict descendant of the captured
private work root, checked with no-follow ancestry; cleanup walks do not follow
links. Failed containment or unavailable Git/JVM is a failed specification,
never a skipped or copied-directory substitute. This is the frozen local
single-producer/consumer model; it adds no locks, retries, backups, crash
consistency or authenticity machinery.

The twelve-field external test transport makes the actual caller boundary
observable. Current reference and human codecs retain their existing syntax;
the human record is constructed from independent actual input, never recovered
from stored approval. Closed JSON preserves None and Some(empty), full scope,
decision and mapping requirements, and rules/providers; strict malformed input
becomes harness exit 3. Well-formed missing or contradictory actual evidence
crosses the production gate and becomes structured rejection or typed
Incomplete/Inconsistent exit 2. Eligible exit 0 preserves the exact recorded
action. The four-field report carries the complete successful state/admissions
or original attributed failure. This transport supplies no public schema,
package evidence, authentication or new semantic model.

D1–D12 retain complete independent all-eight continuity witnesses and rich
candidate/diff/review/human/decision/issue values, including binary CML,
associations, source/condition/sequence/enrichment sidecars, null review facets,
ordered duplicates and all issue facets. Four bounded property samples vary
labels and encounter order. Consumer-only package/input changes demonstrate
actual use-time action and owner rechecks after producer removal. Complete
decoded producer/consumer report comparison is an ordinary regression check;
negative cases also assert explicit outcome/problem kinds/dimensions. The
consumer always admits its own root and external actual inputs. Neither report
bytes nor a producer's earlier successful gate grants current permission.

These are authored mechanisms and executable obligations. Their selected
forked serial SBT execution, independent review, Step acceptance and Phase
release are still pending; the documentation does not claim durable proof has
passed. No CML write, live-source lifecycle check, Web/API boundary or successor
Phase action is authorized.

P10-40 cursor, P10-41 package-only reconstruction, P10-42 fresh Phase 9 process,
P10-43 gate, and P10-44 durable handoff remain executable obligations.
Authored specs/harnesses are not a current PASS or compatibility promise.
Acceptance, one complete Phase review/release, and checklist transitions remain
with the parent. This design records authoring and claims no validated or
accepted Step, release, or Phase closure. Source mutation, Web/API work, retained DB/provider recovery,
packaging, publication, deployment, and successor Phase implementation are
outside this target.
