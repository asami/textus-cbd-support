# Model Edit Session Contract

status=target
decision_scope=EDIT-12-01
updated_at=2026-10-27

This contract promotes the Phase 12 editing proposal for the bounded Stage
12.1 foundation. The [CCDM contract](canonical-component-design-model-contract.md),
[Use Case communication contract](use-case-communication-projection-contract.md),
and [typed control contract](internal-model-typed-control-contract.md) retain
authority over semantic scope, Actor/Goal identity, attribution and versioned
references. [Executable behavior](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/ModelEditSessionSpec.scala)
defines pure admission; Stage 12.2 owns subsequent candidate mutation.

## Session, candidate and operations

An Edit Session names one immutable producer-selected logical record version,
one bounded CCDM context/Component scope, and a candidate record version.
Candidate, operation, Slice and relationship versions reuse
`InternalModelRecordReference`; consumed artifacts reuse
`InternalModelArtifactReference`. Producers allocate identities and positive
versions explicitly. Consumers never increment them, choose a latest value or
derive them from bytes, order, names or AI suggestions. These declarations are
provenance under the local single-writer model, not authentication or undeclared
payload-change detection.

An operation carries its own exact reference, expected candidate reference,
required exact artifact versions, attribution, and a closed typed command.
Subject references distinguish existing Use Cases, Actors, Goals and Slices
from producer-proposed new Slice/Goal endpoints. Commands are create/refine
Slice, link/unlink Slice-Specification, link/unlink Slice-Ticket, add/refine Goal,
and link/unlink Goal-UseCase. Actor/Goal operations reuse admitted Use Case
communication references; no view-local Actor identity is allocated. A Goal
and Use Case remain distinct and may have multiple explicit relationships.

Pure admission constructs an operation only when the session is active, its
expected candidate and artifact dependencies match the supplied admitted basis,
the exact required endpoints exist (or are explicitly proposed for creation),
and relationship references bind their exact endpoints. Refine preserves stable
identity and requires a producer-supplied higher record revision. Creation and
linking reject already occupied semantic/relationship identities. Unlinking
selects the entire exact edge version and endpoints, rather than deleting all
edges with a matching label or endpoint. Failures return typed violations and
no operation; supplied immutable candidate state is unchanged.

Admission returns a typed proposed effect, not a computed semantic difference
or a mutated model. A later service must apply admitted operations, allocate
the next candidate version explicitly, project Views and produce a semantic
diff. The foundation describes validation/diff/approval dependency records
with exact session, candidate, operation, review, diff and artifact versions.
Changing any dependency requires fresh validation/review and actual human
approval through Phase 10. A matching dependency value cannot approve itself.
Abandonment is an explicit session state; later admission rejects it. Neither
failure nor abandonment writes canonical CML. Restart is a fresh session, with
no recovery/replay/rollback or concurrency arbitration guarantee.

## Slice and development trace

A `UseCaseSlice` is a stable CCDM semantic identity for an implementation slice
of one explicit Use Case, with its own logical record version, title, intent,
scope and acceptance references, source attribution and lifecycle
(`Proposed`, `InDevelopment`, `Implemented`, `Abandoned`). It is independent of
UI grouping and Ticket identity. Lifecycle is producer-declared model state;
`Implemented` proves neither executable-specification execution nor ticket
completion. Refinement may revise lifecycle without promising an external
workflow transition.

One Use Case may have many Slices. Each Slice may have many Specification and
Ticket relationships. Each edge has its own exact record version, Slice
endpoint, typed external endpoint and source attribution. Specification and
Ticket edges remain separate; no Specification-to-Ticket relation follows
from flattened endpoint lists. `UseCaseDevelopmentTrace` retains the exact Use
Case, admitted Goal references, Slice records and both independent edge sets.
Capability-demand references remain read-only supplied evidence.

`SpecificationReference` contains owning repository/project, stable
specification identity and required source revision. A path/title is a locator
or label only. `TicketRef` contains provider, repository/project and immutable
external ticket identity, with optional observed state/version. Observations
are not identity; different observed states do not create different tickets.
Linking creates no issue-tracker action and requires no live connector.

## Bounded resolution and findings

Resolver evidence names an exact typed endpoint (including Specification source
revision), one exact source artifact and attribution. Only one matching
`Resolved` evidence item admits a new external link. `Missing` proves dangling
only for that exact target and admitted bounded source. `Unavailable`,
`Unauthorized`, `Redacted`, and `Unsupported` remain distinct unresolved
outcomes; no evidence is also unresolved. Conflicting/duplicate exact evidence
is an ambiguous resolution failure. No fallback lookup or name matching occurs.

Review findings carry kind, exact subject/related references, evidence and a
project-supplied severity/blocking disposition. Finding kinds include
GoalWithoutUseCase, policy-required UseCaseWithoutGoal, UseCaseWithoutSlice,
SliceWithoutExecutableSpecification, SliceWithoutTicket, dangling Specification
or Ticket, UncoveredUseCaseBehavior and MissingCapabilityDemandTrace.
Missing-link findings require explicit complete relationship-scope evidence;
unavailable scopes cannot prove absence. Coverage findings additionally require
the admitted complete behavioral/capability projection for that scope. The
foundation records this vocabulary and evidence; Stage 12.3 owns the complete
deterministic Review algorithm and projection. There is no default severity or
automatic blocking policy supplied by an external provider.

## Canonical and continuation boundary

CCDM and the owning canonical CML source remain semantic authorities. Phase
10.7, 11.1, 14, 15 and 15.1 are closed owners, not new editing authorization.
The current realization V3 can carry attributed generic element/relationship
records, but the existing candidate CML profile only binds existing targets
and explicit source-owned mappings. That does not admit Slice syntax or new
trace semantics. The exact mapping, CML producer/parser contract, candidate
payload/typed subject, codecs and fresh-process continuation for these values
are not yet admitted. They remain explicit Stage 12.2/12.3 and 12.8 integration
obligations before promotion, not a Stage 12.1 claim of serialization support.

This in-memory candidate foundation supplies no package artifact profile,
sidecar, canonical serializer, approval bypass, Job/controller, UI/MCP adapter,
or external effects. Unsupported data MUST remain candidate-only and MUST NOT
be promoted or persisted via an opaque parallel sidecar. Phase 10's existing
review, actual-human exact approval and canonical application gate remain
mandatory. FU-01/FU-03/FU-07 must be admitted by their own owners before Views
consume those inputs; foundation operation construction does not complete them.
