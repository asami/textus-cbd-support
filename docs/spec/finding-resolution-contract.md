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

Executable specification: `runtime.FindingResolutionSpec`.
