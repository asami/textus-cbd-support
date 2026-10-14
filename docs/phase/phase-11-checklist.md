# Phase 11 Checklist: Capability Ingestion, Catalog, and Queries

Status: OPEN / IN_PROGRESS (CAP-11-01 accepted; CAP-11-02/03 OPEN)
phase=[Phase 11](phase-11.md)
predecessor=[Phase 10.7](phase-10.7.md)
successor=[Phase 11.1](phase-11.1.md)
development-item=DEV-CBD-003

This authoritative ledger owns CAP-11-01–03 after the applied 2026-10-04 split.
CAP-11-01 is accepted; see the
[accepted Step](phase-11.md#cap-11-01-acceptance--2026-10-15).
CAP-11-02/03 remain OPEN and not started. CAP-11-04–05 retain their IDs in the
[Phase 11.1 checklist](phase-11.1-checklist.md); no acceptance is inferred.
Execution preparation: [local evidence and integration points](../notes/phase-11-12-local-execution-preparation.md).

## CAP-11-01: Projection ingestion and integrity

Stage Status:
- Current status: DONE
- Owner: CBD Support ingestion/model owner
- Update rule: Close only after supported versions, integrity checks, and
  current/history snapshot semantics have executable specification coverage.

Current acceptance — 2026-10-15: all seven original criteria below are DONE
after focused validation, independent whole-Step review and mechanical naming
closure; see [CAP-11-01 acceptance](phase-11.md#cap-11-01-acceptance--2026-10-15).
The following contract handoff, validation-ownership and S02/S03 authoring notes
are historical as at 2026-10-14. Their pending/unchecked statements are superseded
only for current CAP-11-01 status. Participant acceptance and closures, CAP-11-02/03,
Phase closure and Phase 14 failed Job behavior retain their separate boundaries.

Contract authoring handoff:
[consumer specification](../spec/capability-ingestion-contract.md) and
[ingestion design](../design/capability-ingestion.md) record the frozen
supported-version/selection/wire/failure/current-history and catalog/query
semantics. S01 supplies documentation only; all seven criteria below remain
unchecked. Actual producer -> Scala 3/native CNCF Factory -> CBD no-CML-reparse
ingestion, full Application consumer roundtrip and typed unavailable forward
Use Case demand proof remain pending. Cozy CAP-64-05 retains four unchecked
criteria; participant and CBD Step/Phase acceptance, validation, review and
release remain separately owed. CBD repository full-suite validation stays
deferred-not-run to Phase 11.1; successor execution has not started.

S02 kernel authoring handoff — 2026-10-14:
[private API and 28-case map](../spec/capability-ingestion-contract.md#s02-ingestion-kernel-authoring--2026-10-14)
records nine production sources, two synthetic fixture sources and three
AnyWordSpec/GivenWhenThen/should suites. C11A11/C11S07 properties are authored
with minimum 100 successes and zero discards; no validation result is asserted.
Full Application roundtrip, exact provenance, original public Factory readouts
and accepted/rejected history are authored kernel behaviors awaiting parent
focused validation/review. Synthetic transport/native fixtures do not replace
actual normal Cozy -> Scala 3/native CNCF Factory -> CBD producer proof, which
remains mandatory later same-Step work. All seven criteria below remain open.

Validation ownership — 2026-10-14: complete configured Compile/Test source
compilation includes preserved transferred Job sources/specifications. The original
Capability/typed-control/output-exclusion behavior, exact projection ingestion,
current/history semantics and real normal Cozy -> generated Scala 3/native
Directory/CAR public Factory -> CBD no-CML-reparse proof, including full Application
roundtrip, remain owed. Transferred Job/Provider/Entity runtime execution and
correction belong to [Phase 14](phase-14.md) / DEV-CBD-004 and remain unaccepted;
native Job success adds no CAP-11-01 criterion. Preserve failed evidence without
skipping tests, accepting unavailable input as empty, substituting a producer or
reducing property obligations. All criteria and statuses remain unchanged.

S03 real producer authoring handoff — 2026-10-14:
[actual Test harness and eight-case map](../spec/capability-ingestion-contract.md#s03-real-producer-interoperability-authoring--2026-10-14)
records the explicit `textus.cbd.capabilityProducerRoot` opt-in, four separate
single-source dedicated normal CLI products, six configured Scala 3 accessor
products, independent original three-kind native metadata and actual Directory/
normal CAR public Factory intake. Complete primary/secondary Application products
remain separate; source-free CBD intake retains full fields, Provided/Required,
External evidence, current/history and original failure semantics. Store+Remote
and Archive+Remote are positive selections; complete primary Application with
Store+Archive+Remote is the explicit Local Ambiguous branch with both witnesses
retained. C11R01–08 have actual Given/When/Then and should clauses. Fixture commands
are authored and await parent-owned validation; no executed result or acceptance
is asserted. All seven criteria remain unchecked. The original five-suite
49-case accumulator/properties, participant Step/Phase closures, authorized
eventual Cozy main integration, Phase 14 unaccepted failed evidence and aggregate
full-suite deferral to Phase 11.1 retain their existing owners.

- [x] Define supported CNCF Component Capability, Application Capability, and
      Use Case demand projection versions independently.
- [x] Record each producer/schema/source revision and real fixture availability;
      promote the consumer design/specification before implementation, using
      explicit typed control under repository rules.
- [x] Preserve exact Use Case, Application Capability, Component, package,
      source, Capability, realization, and ABI identity supplied upstream.
- [x] Reject or quarantine ambiguous, duplicate, malformed, stale, and
      incompatible projections without repairing identity by name.
- [x] Separate current selected state from historical ingestion attempts.
- [x] Represent a missing/not-yet-supported upstream Use Case demand projection
      explicitly; do not reconstruct it from CML, names, descriptions or AI.
- [x] Add executable fixtures for valid, stale, incompatible, duplicate,
      ambiguous and unavailable-upstream cases.

## CAP-11-02: Internal catalog and demand/realization traceability model

Stage Status:
- Current status: OPEN
- Owner: CBD Support catalog/model owner
- Update rule: Close only after internal projections preserve upstream meaning
  without introducing competing Use Case/Application/Component Capability identity.

- [ ] Model provided and required `CapabilityRecord` values with typed qualified
      references and exact source/version/ABI provenance.
- [ ] Model `CapabilityDemandTrace` (or repository-equivalent) joining only
      explicit upstream Use Case -> Application Capability -> required Component
      Capability references.
- [ ] Model explicit Operation, Workflow, and StateMachine realization links as
      typed `CapabilityRealizationRef` values; prohibit Operation-name inference.
- [ ] Model Specification and Evidence references separately from Capability,
      demand and realization state.
- [ ] Keep Availability, Authorization, Guard and execution result distinct.
- [ ] Preserve one-to-many demand/provider/realization cardinalities; do not
      assume one Use Case, one Capability, one provider or one realization.
- [ ] Preserve explicit edge attribution across multiple trace paths; prove
      independent endpoint lists cannot manufacture provider/realization joins.
- [ ] Add codec/validator/executable specifications proving round-trip identity,
      cardinality preservation and rejection of dangling/ambiguous explicit refs.

## CAP-11-03: Search, matching, demand trace, and gap analysis

Stage Status:
- Current status: OPEN
- Owner: CBD Support query owner
- Update rule: Close only after exact-identity search/matching, trace and typed
  gap behavior are specified and executable.

- [ ] Provide deterministic query operations equivalent to `getCapability`,
      `findProviders`, `findRequirements`, `traceApplicationCapability`,
      `traceUseCaseDemand`, and `diagnoseCapabilityDemand`.
- [ ] Search by qualified Capability identity and non-authoritative semantic
      metadata while keeping semantic matching identity/compatibility based.
- [ ] Find Components providing or requiring a Capability.
- [ ] Trace an admitted Use Case demand through Application Capability and
      required Component Capability to all explicit provider/realization refs.
- [ ] Return a typed unavailable/unsupported result when Use Case demand input
      is not supplied by an admitted upstream projection.
- [ ] Preserve unauthorized, unavailable, stale and ambiguous source outcomes;
      establish an empty/no-provider result only within an admitted query scope.
- [ ] Distinguish at least: missing Application Capability projection, missing
      required Component Capability, no provider, incompatible provider version,
      missing realization, missing Specification, and missing Evidence.
- [ ] Prohibit semantic matching, provider selection, compatibility or trace
      construction by display-name/description similarity alone.
- [ ] Add executable specifications for multi-provider, multi-realization,
      incompatible-version, partial-evidence and zero-provider cases.

## Closure

- [ ] Close only CAP-11-01–03 after their executable specifications, focused
      validation, independent Step acceptance and full Phase review pass.
- [ ] Bind the admitted producer/schema/source revisions and fixture evidence;
      hand off the settled typed query/result/gap and presentation contract.
- [ ] Record repository-full validation as deferred-not-run to Phase 11.1,
      retaining focused validation, review, closure ledger and release commit.
- [ ] Save the exact split planning set with normal Phase acceptance or
      management-data commits; preserve unrelated work and planned successors.
