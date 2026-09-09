# Phase 10 Checklist: Durable Model Development and Continuation

Status: OPEN
phase=[Phase 10](phase-10.md)
predecessor=[Phase 9](phase-9.md)
development-item=DEV-CBD-002

This checklist is the authoritative Phase 10 state ledger. Phase 10 consumes
accepted Phase 9 canonical-model, projection, candidate-design, and semantic-diff
contracts; it must not create parallel semantics merely for persistence.

## P10-01: Internal-model source-root contract

- [ ] Promote the project-owned `src/main/internal-model/` role and its separation
  from canonical CML, CBD retained state, and disposable output into design/spec.

## P10-02: Package identity and schema

- [ ] Define stable package identity, project identity, revision, lifecycle
  state, schema/version compatibility, and deterministic serialization rules.

## P10-03: Manifest and artifact inventory

- [ ] Define `manifest.yaml` inventory roles, required/optional artifacts,
  package-relative references, dependency order, and exact content hashes.

## P10-04: Integrity executable contract

- [ ] Prove fail-closed behavior for missing, mismatched, incompatible,
  duplicated, or unresolved required package artifacts.

## P10-10: Scenario source snapshot

- [ ] Define the self-contained selected Scenario snapshot with canonical source
  identity, revision/hash, normalized content, and Phase 9 trace identities.

## P10-11: Model-context and glossary/BoK snapshots

- [ ] Define bounded model-context and glossary/BoK snapshots containing the
  semantic basis actually used by the selected candidate and its provenance.

## P10-12: CML baseline snapshot

- [ ] Define the exact CML baseline needed to interpret and safely apply a
  candidate projection without replacing live canonical CML authority.

## P10-13: Snapshot freshness checks

- [ ] Implement deterministic comparison of live Scenario, model, glossary/BoK,
  and CML sources with recorded baselines and surface drift without silent rebase.

## P10-20: Durable realization/design state

- [ ] Define the selected realization/design artifact using stable semantic
  identities compatible with the Phase 9 Canonical Component Design Model.

## P10-21: Phase 9 projection continuity

- [ ] Prove that persisted semantic identities can reconstruct Mono-Koto,
  Use Case, Entity, Event, Structure, Classification, Workflow, and StateMachine
  projections without storage-specific reinterpretation.

## P10-22: Decision and alternative record

- [ ] Persist accepted decisions, rationale, accountable actor, affected
  identities, relevant rejected alternatives, assumptions, and supersession.

## P10-23: Open-issue record

- [ ] Persist unresolved questions, evidence/options, decision role, impact, and
  whether each issue blocks semantic approval, projection, application, or
  validation.

## P10-30: Candidate CML projection artifact

- [ ] Define exact target CML identities, semantic mappings, expected
  compatibility/migration effects, and candidate source baseline.

## P10-31: Durable semantic diff

- [ ] Persist/reconstruct the Phase 9 semantic diff independently of textual Git
  diff while retaining traceability to exact proposed CML changes.

## P10-32: Review binding

- [ ] Bind candidate review evidence to exact package, realization/design,
  projection, rules/provider versions, and content hashes.

## P10-33: Human approval artifact

- [ ] Define separate approval records bound to exact candidate identity,
  revision/hash, accountable human, decision, rationale, and unresolved items.

## P10-34: Supersession and approval invalidation

- [ ] Define deterministic rules for changes-requested, rejected, approved,
  superseded, and invalidated states without mutating the approved model hash.

## P10-40: Resume cursor contract

- [ ] Define `resume.yaml` as a derived cursor containing current workflow stage,
  last completed action, next permitted action, blockers, preconditions,
  invalidation checks, and acceptance criteria.

## P10-41: Package-only state reconstruction

- [ ] Reconstruct the selected semantic state from `src/main/internal-model/`
  without requiring prior chat/provider session, `target/`, or CBD retained DB.

## P10-42: Fresh-process Phase 9 rehydration

- [ ] Start a fresh CBD Support process and reconstruct the Canonical Component
  Design Model needed by the Phase 9 Dashboard/projection architecture.

## P10-43: Continuation action gate

- [ ] Permit only the recorded next action whose preconditions hold; report
  incomplete/inconsistent state instead of inventing missing decisions/mappings.

## P10-44: Durable handoff executable specifications

- [ ] Prove stop/commit/transfer/resume behavior across process/session
  boundaries with stable semantic and projection results.

## P10-50: Semantic drift invalidation

- [ ] Invalidate or require re-review when Scenario, realization/design,
  referenced model, glossary/BoK basis, mapping rules, or CML projection changes.

## P10-51: Baseline drift handling

- [ ] Detect live CML drift and require explicit reconciliation/review rather
  than silently applying a proposal against a different baseline.

## P10-52: Exact approval gate

- [ ] Reject CML mutation unless an applicable human approval binds the exact
  selected realization/design and candidate projection hashes.

## P10-53: Skill-applied CML change boundary

- [ ] Define the dedicated skill/repository mutation contract so approved CML
  changes preserve canonical-source ownership and Git-governed acceptance.

## P10-54: Post-change validation and re-projection

- [ ] Run Cozy/CBD validation after the CML change and regenerate the Phase 9
  Canonical Component Design Model and stakeholder/engineering projections.

## P10-60: CBD retained-state integration

- [ ] Retain richer review/proposal/alternative/supersession/evidence history
  while proving it is not required to resume the current selected state.

## P10-61: MCP/API continuation surface

- [ ] Define bounded authorized operations for reading, proposing, reviewing,
  resuming, and recording continuation state without exposing an implicit
  approval or arbitrary repository mutation surface.

## P10-62: Sensitive-data and provider evidence policy

- [ ] Define retention/redaction for prompts, responses, provider/model/tool
  identity, hashes, CallTree/evidence references, and non-reconstructable
  sensitive evidence.

## P10-63: Build and publication exclusion

- [ ] Prove internal-model source is excluded by default from runtime packaging,
  public APIs, ordinary CML generation, documentation publication, and CAR/SAR
  artifacts.

## P10-70: Phase 9 -> Phase 10 end-to-end scenario

- [ ] Prove stakeholder/model review -> Candidate Design -> Semantic Diff ->
  durable package -> fresh-process resume -> approval -> CML change -> validation
  -> Phase 9 re-projection.

## P10-71: Compatibility and reproducibility validation

- [ ] Run selected executable, static, schema, integration, repository, and
  representative project validation for the accepted Phase 10 boundary.

## P10-72: Documentation and development-item synchronization

- [ ] Synchronize design/spec, source-layout reference, skill contract, strategy,
  Phase 9 successor references, DEV-CBD-002 journal status, and user/developer
  documentation with accepted evidence.

## P10-73: Final review and closure

- [ ] Complete final review and close Phase 10 only after every required item is
  checked or explicitly relocated under successor authority.
