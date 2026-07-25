# Phase 8 P8-42: SQLite persistence decision

date=2026-07-23
phase=Phase 8
status=approved-for-planning
decision_id=P8-42-review-storage-01

## Decision

The initial physical persistence implementation for CBD Support CAR Review uses
the CBD Support Entity layer through the CNCF protected internal DSL. SQLite
is only the development profile's configured datastore backend; CBD Support
must not use SQLite, JDBC, SQL, a JDBC URL, a database connection, or a raw
datastore handle directly.

The development launcher may bind the CBD Support component datastore to
`~/.cncf/textus-cbd-support/review.sqlite`. That path is launcher
configuration, not a runtime concern. CBD Support receives the configured
`DataStoreSpace`/component datastore through its execution context; it does not
discover a home directory, environment variable, filesystem path, or database
configuration itself. CI and integration tests bind an independently admitted
temporary datastore profile through the same abstraction.

## Boundary

P8-42 introduces CBD-owned `ReviewDiagnosis` Entity records, their aggregate,
and a rebuildable exact-key View. Review ActionCalls reach them only through
the internal DSL (`entity_create`, internal Entity load/update/search, and
aggregate operations). Domain services depend on typed Review records, never
on `DataStore`, JDBC, SQL, or SQLite types. This keeps the logical P8-40 entity
model and P8-41 reuse key unchanged and permits a later SQLite, PostgreSQL, or
other provider binding without changing Review semantics.

The adapter must atomically retain a completed compatible Run/Report and use
the exact P8-41 definition ID plus digest for lookup. It must reserve one
bounded in-progress identity so identical concurrent requests join the same
Run. P8-43 still owns failed/cancelled/expired/incompatible outcomes, and
P8-45 owns storage authorization, administration, and history enumeration.

## Rationale

The CNCF Entity/internal-DSL boundary already provides component-scoped
persistence, authorization, CallTree, and observability integration used by
Textus components. Using it prevents storage-provider details from leaking
into Review logic and makes the initial SQLite-backed local workflow and a
future common datastore the same application integration. SQLite keeps the
initial server, local workflow, and CI topology lightweight without requiring
a separately provisioned database server before Review can be used.

## Shared Database Evolution

The Entity layer is the lasting application boundary; `datastore` is its
infrastructure binding and SQLite is merely the first configured backend.
P8-42 uses the execution-context `DataStoreSpace` binding and CBD Support
Entity collections. Domain services receive no connection, datastore handle,
or vendor type. The P8-40 logical entity names map to CBD-owned Entity
collections and their versioned record model, rather than SQL table names or
DDL.

The default SQLite file contains an isolated component datastore. A future
common datastore binds multiple components into one `DataStoreSpace`, while
CBD Support continues to read and write only its own component datastore and
owned collections. This prevents name collisions, cross-component reads, and
one component applying another component's storage migration. Record-model
evolution is versioned and performed only through supported datastore
operations; direct schema DDL is prohibited. Authorization and
cross-component history exposure remain P8-45 work.

## Direct SQLite Prohibition

CBD Support production code and its Review persistence tests must not import
`java.sql`, `org.sqlite`, or an SQLite driver, construct SQL strings, open a
database connection, encode a SQLite path/URL, or obtain `DataStore` directly.
The P8-42 executable coverage must prove Entity persistence through the
internal DSL and a configured CNCF datastore, including the SQLite-backed
development profile where needed. Backend provisioning and vendor-specific
verification belong to CNCF/launcher infrastructure, not to CBD Support Review
code.

## Exact-key admission boundary

The P8-41 key is created before provider work by the Review execution planner,
which owns selected provider/rule identities, admitted Evidence snapshot
identities, and the four required policy bindings. The existing P5
`startReview` operation has only target and profile and therefore cannot
construct this key safely. It remains a job-admission compatibility boundary;
it must neither synthesize a partial key nor write a reusable diagnosis.

The first P8-42 ActionCall that possesses a complete
`CarReviewReuseKeyInput` claims or joins the `ReviewDiagnosis` Aggregate using
the internal DSL. Its View is then the bounded exact-key lookup for completed
reuse or one in-progress owner. This keeps incorrect cache hits out of the
system while the provider execution planner is connected in the same slice.

## Initial model evidence

`textus-cbd-support.cml` now defines the CBD-owned `ReviewDiagnosis` root,
its `ReviewTargetSnapshot`, `ReviewRunSnapshot`, `ReviewReportSnapshot`, and
`ReviewAttestationSnapshot` composition members, Aggregate state, and a
rebuildable exact-key View containing both key definition ID and digest. The
source-managed CAR ABI exports those five Entity types but keeps the existing
19 service operations unchanged; Entity CRUD is not an MCP-ready Review API.
The root retains only an optional active Run identity; immutable Run history
belongs to its `ReviewRunSnapshot` members. This lets one exact reuse identity
represent an active owner or a completed reusable Report without overwriting
prior Run attribution. Failed/cancelled successor behavior remains P8-43.

On 2026-07-23, `sbt "Test / compile"` passed and
`scripts/check-car-abi.sh` reported `CAR_ABI_SURFACE_OK` with 19 operations
and 5 entities. This verifies model generation and packaging only. P8-42 does
not become complete until the complete-key execution planner claims/joins the
Aggregate through the internal DSL and executable tests prove provider work is
not duplicated.

`CarReviewExecutionPlan` is the first implementation of that planner boundary.
It binds a `ReviewStartRequest` to exactly one validated P8-41 key before
provider work. It rejects a target/profile mismatch and a provider selection
whose rule-set is absent from the frozen rule-set set. The plan remains an
internal server value: an MCP or HTTP caller cannot supply a partial or
untrusted reuse identity. The next implementation step is an internal
ActionCall that claims or joins the `ReviewDiagnosis` Aggregate from this
value.

The implemented claim uses the CNCF protected
`entity_claim_or_load_internal` DSL primitive. `entity_upsert` remains
unsuitable because an existing exact-key row would be updated with a different
active Run. The internal variant is explicit because a diagnosis derived from
already-admitted server input is a component-owned coordination record, not a
user-owned history resource: its create/load authorization uses the CNCF
`ServiceInternal` mode and remains observable in UnitOfWork/CallTree.

`ReviewDiagnosisPersistenceSpec` proves two distinct Review IDs for one
complete `CarReviewExecutionPlan` produce one `Owner` and one `Joined` result
through the Entity DSL. The deterministic diagnosis Entity ID uses the P8-41
definition ID and digest with the CNCF stable timestamp/entropy components;
ordinary generated Entity IDs would include request-time entropy and could not
coalesce work.

At the time of this decision, the scalar round-trip concern was recorded as a
generator follow-up rather than a reason to expose datastore access in CBD
Support.  As of 2026-07-24, the generated scalar `ValueReader` accepts the
scalar datastore form as well as the record form, and
`ReviewDiagnosisPersistenceSpec` proves the resulting Entity round trip.
CBD Support therefore has no `PersistedReviewDiagnosis` adapter or other
review-local persistence codec.  Any remaining cross-component scalar
round-trip regression scope belongs to a proposed future Cozy phase; it is not
assigned to a nonexistent CBD Support Phase 16.
