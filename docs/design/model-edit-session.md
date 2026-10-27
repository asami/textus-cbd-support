# Model Edit Session Design

The [contextual Palette adapter](model-edit-palette.md) reuses this service and
shared candidate projection without extending the stored session model. Its
subject checks restrict a direct action to the exact selected candidate context;
normal authorization, admission and canonical approval retain their owners.
The [conversational adapter](model-edit-conversational-adapter.md) reuses this
direct path for explicit interpreted intent; it adds neither a stored editing
model nor an AI-provider framework.

decision_scope=EDIT-12-01 / EDIT-12-02
updated_at=2026-10-27

The [contract](../spec/model-edit-session-contract.md) owns behavior.
`ModelEditSession` and its companion keep one cohesive responsibility: immutable
typed candidate editing inputs and deterministic operation admission. Pure admission does not own candidate mutation or canonical promotion. The
Stage 12.2 service and typed captured payload boundary are described below.

The scope uses existing `MonoKotoProjectionContextIdentity` and
`ComponentDashboardComponentIdentity`; semantic endpoints use
`ComponentDashboardSemanticTargetIdentity`. Existing Use Case communication
subjects/references supply Use Case, Actor and Goal identities and attribution.
New Slices are explicit candidate proposals using those semantic identity
domains, with independent existing typed record versions. Logical references
and artifact references retain distinct `InternalModelTypedControl` domains.
Closed command, lifecycle, failure, resolution and finding alternatives use
Scala 3 enums; structured records carry supplied data without allocating IDs.

Admission compares declared exact versions under the existing local
single-writer model. It checks semantic endpoint roles and edge membership,
then constructs the exact operation plus typed proposed effect. It never
guesses a source, fabricates resolver evidence, advances a version or partially
applies a command. This intentionally bounded construction point is the input
to the Stage 12.2 service, not a second editor implementation.

External target identity and optional ticket observations are separate values.
Resolver records are explicitly bounded by the full target and source artifact.
Unavailable sources remain distinct from proof of missing targets. Separate
edge records prevent a Slice's two lists from accidentally implying a
Specification-to-Ticket mapping. Missing-scope evidence and project-owned
dispositions make the future Review rule boundary explicit.

Validation, semantic diff and approval dependency records reference exact
candidate/operation/evidence versions. Equality describes applicability only;
it conveys no actual human approval. Phase 10's source-backed package,
complete semantic review subject and approval/application pipeline remain the
only promotion route. `CandidateDesignSemanticDiffIntegration` continues to
own its supplied Phase 9 diff; the foundation's proposed effect is not
converted into that diff by text comparison.

Canonical Slice/trace source syntax and mappings remain prerequisites before
these candidate values become promotable. Stage 12.2 adds the typed package
payload/profile, subject inclusion, codecs and captured rehydration below;
Stage 12.8 still owns fresh-process continuation proof. Generic realization V3
support is insufficient by itself. These gaps remain visible in
Phase 12's existing stages; neither an opaque sidecar nor the closed Phase 14
Job or Phase 15/15.1 Review infrastructure supplies missing semantic authority.
No locks, CAS, replay, retries or rollback are added for the local single-writer
execution model.


## Stage 12.2 application boundary

`ModelEditService` composes caller authorization, pure operation admission and one
immutable copy. Explicit successor references and exact applied-operation history
retain local single-writer provenance. Independent Goal state preserves intent and
Actor attribution without allocating communication identities. Historical review
values are retained across edits and tested for applicability against exact session,
candidate, history and artifact references.

`ModelEditCandidateCodec`, `ModelEditCommandCodec` and `ModelEditSessionCodec` own
closed typed JSON encoding. The package projection-family selector recognizes the
edit profile. Rehydration decodes captured bytes, binds exact inventory dependencies
and exposes typed edit sessions. The existing complete-subject algorithm includes
projection roots automatically; no approval mechanism is added. The action gate
keeps this source-unmapped profile at inspection until Stage 12.3 supplies its
canonical projection contract. Native Stage 12.2 accepted this implementation at
`f6f2f354b6e2ef2767ee1737adfc8858bf5eead7`, after 261 tests in 16 selected suites
and independent review resolving original issue-1. This acceptance does not
establish the Stage 12.3 canonical mapping or Stage 12.8 continuation proof.

## Stage 12.3 source-owned bridge

Independent Actor records allow a source-declared Actor to precede Goal creation;
placing it in a Use Case's reference list would invent a relationship. V2 session
payloads add that explicit collection and typed Slice fields, so source-declared
fields are part of candidate command/history state. Actor/field-empty v1 shape and reading remain
supported, and both use the same package inventory and rehydration owner.

`ModelEditSourceProjection` consumes the delivered producer's typed JSON and
caller-supplied source/CBD identity and external-owner mappings. Caller-admitted
conditions remain separate from source production; the bridge creates no roles or
authentication decision. Its canonical adapter emits the existing model-context and
realization records using explicitly supplied assertion/source-reference identities.
External endpoints retain explicit caller mappings rather than inferred local nodes.

The candidate-v2 planner checks proposed meaning against the exact editing candidate.
Source-rendered content then enters existing Phase 10 candidate and semantic-diff
admission. A closed source-mapping Projection retains the typed confirmation and
exact captured dependencies. Continuation consumes that mapping alongside the
existing independent review and approval owners; no new application gate is created.
The bridge, Views and Review are accepted at native Stage 12.3 commit
`a08c7745748d32e7549ebd35da0d3cfa1e9b2d5b` after 448 tests in 33 suites and
independent accepted review. Actual canonical application and fresh-process
continuation retain their later Stage owners. The contextual Palette is accepted
at native Stage 12.4 commit `004e5eb43d1fdce5dec8c97e419b984cc00dde1a` after
73 tests in 16 suites and independent accepted review. Stage 12.5's conversational
adapter is accepted at native commit `63f9ddd08989d42746d2f33ac9a2d89f3f9e93c3`
after 56 tests in 16 suites and independent review01.

The [external application design](model-edit-external-operations.md) defines one
host-admitted session shared by generated Operations. Source CML owns generated
contracts; ComponentFactory owns supported typed host construction and ActionCalls.
Existing source mapping/Phase10 admission retains exact semantic diff binding.
Current Stage12.6 code is authored, pending native checks and independent review.
