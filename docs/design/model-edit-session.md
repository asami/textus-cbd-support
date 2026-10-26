# Model Edit Session Design

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
canonical projection contract. Implementation remains pending native validation.
