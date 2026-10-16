# CAR Review Persistence and Declared Ownership Contract

status=accepted
acceptance_owner=PHASE-14
updated_at=2026-10-16

Current acceptance is the [Phase 14 closure](../phase/phase-14.md#current-phase-closure-and-successor-handoff--2026-10-16),
with its selected source-mode inputs and explicit recovery limits. This current
projection takes effect after the verified local Phase release.

## Historical authoring metadata — 2026-10-06

status=authored_review_pending
phase=Phase 8 / PHASE-69.7
checklist=P8-40 / P8-42 / P8-43 / P8-45 / JM69-10CBD2A
updated_at=2026-10-06

## Supersession and Persistence Authority

Historical P8 item IDs remain. Earlier P8-40/P8-41 opaque calculated reuse-key
identity is superseded by an explicit native plan reference and mandatory typed
selection. The [logical example](examples/car-review-persistence-model-v1.json)
retains its historical filename; its current identity fields describe this
contract. Product behavior evidence is the actual generated Entity scenarios in
[ReviewDiagnosisPersistenceSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/impl/ReviewDiagnosisPersistenceSpec.scala),
not Markdown/example field inspection.

CBD Support invokes generated Entity create/load/update/query and ordinary
UnitOfWork routes. Entity owns scalar restoration, persisted revisions,
authorization, lifecycle and CallTree behavior. CBD does not use raw DataStore,
JDBC, SQL tables, vendor DDL, a private store codec or a shadow identity map.
Launcher infrastructure may select SQLite as the Entity datastore backend.
The earlier process-local `CarReviewRepository` is not authoritative persistence.

## Root, Plan Revision and Mandatory Selection

The server supplies the complete `CarReviewExecutionPlanReference`: a native
`ReviewDiagnosis` EntityId and positive declared immutable plan revision. Root
`id` is that exact native identity. `plan_revision:long` and
`execution_selection:ReviewSubmissionDocument` are required in the generated
Entity, Aggregate STATE and rebuildable View. The selection document follows
[the typed selection contract](car-review-reuse-key-contract.md). It is an
ordinary domain document, not a private scalar restoration protocol.

Before join, reuse, successor admission, completion or terminal transition, CBD
validates persisted root ID, exact positive plan revision and decoded semantic
selection against the selected plan. Wrong collection/invalid reference refuses
with `review-plan-reference-invalid`; existing revision mismatch with
`review-plan-revision-conflict`; valid differing typed selection with
`review-plan-selection-inconsistent`. Missing selection declarations refuse with
`review-plan-selection-incomplete`, while malformed typed declarations preserve
their specific validation codes.

Dependency publishers own target, baseline, availability-policy and evidence
IDs/revisions. Policy bindings declare policy versions/revisions. A conclusion-
affecting change requires an explicitly provisioned distinct root reference;
content cannot replace an existing reference or supply a missing revision.
Request Review ID, time and business payload digests cannot select the plan.

## Claim, Owner Lease and Conditional Successor

Internal load followed by `entity_claim_or_load_internal` resolves concurrent
first requests to one declared root. Claim issues a non-transport `Owner` lease
retaining exact reference, diagnosis ID and Review ID. Only claim and a successful
conditional successor may issue it. Matching reference and Review ID are
necessary for ownership; a request or Report alone cannot reconstruct authority.

In-flight compatible requests join the active Review once. Completed compatible
requests reuse the retained Report identity. Terminal failed/cancelled/expired/
incompatible roots admit successors through the existing Entity conditional
transition, comparing persisted revision, state and active Review. One caller
owns and starts the successor; losers join. The immutable predecessor and sole
admitted successor remain attributable. No replay of started native work is
introduced; RecoveryRequired remains unchanged.

Completion and terminal retention read the current persisted root before
transition. Wrong owner, different Review ID, stale lease, or repeated terminal
transition retains the existing `review-diagnosis-completion-not-owner` or
`review-diagnosis-terminal-not-owner` refusal after root/selection validation.
Completion validates canonical Run/Report/attestation payload bindings, creates
composition snapshots and patches the root in one UOW. Existing atomic rollback
and persisted-revision checks remain authoritative.

## Native Composition IDs and Immutable History

| Record | Native ID and retained attribution |
| --- | --- |
| `ReviewDiagnosis` | explicitly supplied root ID; immutable declared plan revision/selection |
| `ReviewTargetSnapshot` | Entity-issued ID; diagnosis composition and semantic target/payload |
| `ReviewRunSnapshot` | Entity-issued ID; diagnosis, exact Review ID, state and immutable Run document |
| `ReviewReportSnapshot` | Entity-issued ID; diagnosis, Review/Report/attestation payload identities and canonical Report |
| `ReviewAttestationSnapshot` | Entity-issued ID; diagnosis, Review/Report payload binding and canonical attestation |
| `ReviewRetentionEvent` | Entity-issued ID; append-only exact expired record ID, payload attribution and effective time |

Target, Run, Report, attestation, retention and successor create models leave
`id=None`. Standard Entity creation owns ID issuance, including the existing
conditional successor intent. No name-based UUID, content seed, derived snapshot
ID, random UUID adapter or framework issuance bypass remains. Composition links
use the exact declared root. The rebuildable View remains a read model over the
Aggregate and cannot become a second persistence path or history enumeration API.
Historical lineage and comparison records remain separate P8-44 logical concerns;
this prerequisite does not implement or infer a new lineage identity.

## Bounded Reads, Authorization and Expiry

P8-45 reads select an exact Report ID through bounded generated queries and
require the existing reader role. Missing or ambiguous records refuse; callers
cannot enumerate history or use lineage/target selectors as Report access.
Expiry requires the operator retention role and policy age. It appends attributable
tombstones, removes Report payload and deletes every attestation by the exact
`entity.id` returned by its bounded query, then marks the root expired. IDs are
never reconstructed from Report content. Tombstones retain record IDs and safe
payload attribution without copying canonical content, credentials or Evidence.
A fresh compatible request may claim a conditional successor after expiry.

## Schema Compatibility and Migration Boundary

Setup requires the current compatible generated Entity schema. Original pre-v2
roots missing mandatory fields fail generated restoration explicitly. There is
no legacy digest decoder, derived-ID lookup, table rewrite, implicit migration,
silent fallback, cache substitution or automatic disposal. Migration of existing
pre-v2 data requires a separately owned decision and executable work.

## Preserved Executable Behaviors and Acceptance

The eight persistence scenarios preserve initial claim/join, immutable completion
reuse, exact authorized fresh-UOW Report read, due authorized expiry/tombstone/
successor, all four non-success histories, concurrent sole successor/one start,
issued-owner refusal, and persisted-state repeat refusal. An additional conflict
scenario rejects changed revision/selection before join or start. Composition
queries prove native distinct IDs and exact root/Run attribution.

Authored status does not establish actual Provider-to-Job/full pipeline success
or independently fresh-process CBD reads. Original JM69-10 validation, protected
M2 review, full acceptance and Phase closure remain pending.
