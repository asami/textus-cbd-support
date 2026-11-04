# Finding Resolution Contract

Status: Phase 13 implementation contract; source application and reinspection
remain pending producer delivery and consumer qualification.

## Authority and imported evidence

CBD owns proposals and developer admission. CNCF
`org.goldenport.cncf.finding.FindingDispositionV1` owns finding keys,
disposition kinds, validation and deterministic interpretation. Cozy owns CML
properties; CNCF owns handwritten Scala annotations. Generated Scala is a
projection, never a second disposition authority.

An import supplies an explicit record identity/revision, a CNCF `Inspection`,
existing `Declaration` records and unavailable/unsupported/failed check scopes.
The adapter supplies truthful completed coverage. A missing finding outside
completed coverage proves no resolution. Imported decisions and incomplete
coverage remain visible unchanged. The CNCF interpreter validates the inspection
and computes its existing disposition report; CBD does not reinterpret it.

## Groups and proposals

Grouping uses a rule and an explicitly supplied semantic remediation pattern.
Each group retains every exact CNCF key. Unassigned findings stay visible; a
pattern is not a wildcard or permission. Duplicate or unknown assignments fail.

A proposal has its own explicit record identity/revision, the exact imported
record, a nonempty list of distinct finding keys, one CNCF kind (Fix, Accept,
Defer or Configure), a reason and a provenance reference. Only currently
unresolved findings may be proposed by this initial path. Proposal generation
performs no admission, source write or AI call. An external AI may supply a
candidate through the same proposal validation; its output grants no authority.

## Explicit developer decision

The host supplies developer authority independently of request JSON. It binds
the authenticated principal, actual direction evidence, exact imported record,
proposal revision, allowed keys and kinds. A request cannot install that
authority. The core consumes it as a trusted host fact; ordinary transport
authentication and actual Human provenance are integration obligations, not
proven by a core fixture or a provenance string.

Admit/reject selects the exact proposal and an explicit, nonempty list of keys.
The list must be a subset of both proposal scope and host authority. Bulk means
that the developer explicitly selected several keys; selecting a group does not
implicitly admit all of it. A decision retains its own record, principal,
direction evidence, reason and exact scope. Rejection produces no admitted
declarations. Admission records intended work, not resolution or source
permission outside the admitted scope.

Each proposal receives one decision. A later design decision uses a new explicit
proposal reference; two admitted proposals cannot claim the same finding key.

The initial implementation is an immutable, host-owned single-writer working
model. It introduces no persistence, concurrency or replay protocol. Explicit
record references select work; content, ordering and hashes grant no permission.

## Subsequent boundaries

LINT13-02 connects these semantics to ordinary generated Operations and the
existing principal boundary. LINT13-03 applies admitted actions through the
actual source owner after qualified CNCF Phase 99 and Cozy Phase 77 delivery.
LINT13-04 obtains actual reinspection and uses CNCF interpretation for closure.
Accept/defer declarations remain auditable; Fix/Configure require actual changed
source/configuration and completed reinspection, rather than inserted metadata.

The actionable metric remains CNCF's unresolved backlog, separately from raw
finding count and deferred follow-up. A proposed/admitted workflow state does
not replace an existing deterministic disposition or reduce that backlog.

## Ordinary operations and authenticated host selection

`CbdFindingResolution` exposes `importFindingInspection`,
`groupFindingResolutions`, `proposeFindingResolution`,
`decideFindingResolution` and `getFindingResolution` through generated
Operation/ActionCall DTOs. Each DTO contains `requestJson`. Closed JSON records
reject duplicate, unknown and missing fields, including nested key/reference
records. Kind and choice are explicit enum strings.

The embedding host installs an application with typed inspection imports and
principal bindings. A binding names allowed import references and independently
supplied developer directions. Import JSON selects only `imported`; it cannot
supply an Inspection, declarations, coverage, principal, grant or Human evidence.
Every operation uses the shared Phase 12 authenticated-principal/time policy,
then selects the exact host principal binding before decoding request JSON.
A valid principal requires a non-anonymous, non-unspecified subject, a nonempty
principal identity, no future authentication time and an unexpired session.

The import operation retains one Working value per explicit imported reference;
re-import cannot replace its proposals or decisions. Grouping returns exact
supplied assignments and unassigned keys without installing a decision. Proposal
JSON selects `imported`, `proposal`, `keys`, `kind`, `reason` and `provenance`.
Decision JSON selects `imported`, `proposal`, `decision`, `keys`, `choice` and
`reason`. The application selects host direction by the exact import and proposal
references, including revisions; the core checks the allowed kind and exact
selected subset. No direction field is accepted from a client.

Responses retain inspection, completed coverage, gaps, original declarations,
CNCF report, raw/actionable counts, proposals and decisions. Group responses also
return the grouping. Admission changes only workflow decisions; the imported
CNCF report and actionable backlog remain unchanged. An unconfigured normal
ComponentFactory returns `UNAVAILABLE`, with no fabricated application or grant.
A configured application reports an import not yet retained as `UNAVAILABLE`.
No operation exposes canonical-source mutation, AI execution or persistence.

For example, an import/get request is:

```json
{"imported":{"recordId":"inspection","recordRevision":1}}
```

A proposal uses the same explicit record-reference shape for `imported` and
`proposal`, a `keys` array of `{"rule":"car.host-access","finding":"host-1","target":{"scope":"operation","identity":"orders:submit"}}`,
`kind: "Accept"`, a reason and provenance. An explicit decision supplies its own
`decision` reference, that proposal reference, the selected `keys`,
`choice: "Admit"` or `"Reject"`, and a reason. Host direction remains independent
of these examples. Fixture wiring proves neither a real Human transaction nor
qualified external producer delivery.

Executable specifications: `runtime.FindingResolutionSpec`,
`runtime.FindingResolutionApplicationSpec`, and `FindingResolutionOperationSpec`.
