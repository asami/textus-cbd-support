# Project-local internal model storage direction

**Date:** 2026-08-17
**Scope:** CBD Support / project-local Use Case Realization review artifacts / CML update workflow
**Status:** AGREED DIRECTION

## Purpose

This non-normative journal records the agreed direction for retaining the
intermediate models that CBD Support reviews while a project moves from a Use
Case Scenario to approved CML changes.

The proposed project-local source root is:

```text
src/main/internal-model/
```

The root is also the portable continuation boundary for CBD Support work. A
fresh CBD Support process must be able to determine the current semantic state,
the completed decisions, the unresolved work, and the next permitted action
from this root alone. The process may use CBD Support retained state and live
project sources to enrich or refresh that state, but it must not require a
previous conversation, a provider session, `target/` output, or an available
CBD Support database merely to resume the work.

The name remains provisional until its source-layout, schema, lifecycle, and
build-exclusion contracts are promoted into design and specification.

This journal does not establish executable behavior by itself. Stable behavior
must be promoted into CBD Support design/specification, the CML contract, and
the skill contract that applies approved changes.

## Related direction

The following journals provide the surrounding methodology and CBD Support
modeling context:

- `/Users/asami/src/dev2025/simplemodeling-org/docs/journal/2026/08/2026-08-17-use-case-realization-review-and-cml-workflow.md`
- `docs/journal/2026/08/2026-08-15-usecase-driven-component-modeling-direction.md`

The SimpleModeling journal defines the methodological route from Scenario,
through a reviewable Use Case Realization Model, to approved CML changes. The
existing CBD Support journal describes Story-to-Activity extraction and
comparison with Component, Service, Operation, CAR, and SAR evidence. This
journal records where the project-owned intermediate artifacts sit between
those concerns.

## Source-root role

`src/main/internal-model/` is a new source kind for project-owned,
machine-readable intermediate models that developers, skills, and CBD Support
review together.

It is distinct from:

- the project's canonical CML sources;
- generated Scala or other executable sources;
- transient AI output and analysis caches;
- CBD Support's retained review/run history; and
- public documentation or runtime resources.

An internal model is not made authoritative merely because a tool wrote it.
Its lifecycle state and a separate review decision determine whether it may be
used to prepare a CML change.

## Provisional layout

The initial layout direction is:

```text
src/main/internal-model/
  manifest.yaml
  resume.yaml
  sources/
    scenario.yaml
    model-context.yaml
    glossary.yaml
    cml-baseline.yaml
  decisions.yaml
  open-issues.yaml
  usecase-realization/
    <usecase-id>/
      realization.yaml
      cml-projection.yaml
      approval.yaml
      validation.yaml
```

The exact serialization format, filenames, and directory names remain open.
The separation of roles is more important than this provisional spelling.

## Self-contained continuation contract

`src/main/internal-model/` must carry a complete, machine-readable handoff for
the selected current revision. A continuation process must not need to infer
state from chat transcripts, reconstruct intent from Git history, or fetch an
opaque review record before it can understand what to do next.

The continuation package must contain at least:

- the package schema/version, stable package identity, revision, lifecycle
  state, and project identity;
- an inventory of every required artifact, its role, relative path, and exact
  content hash;
- the objective, scope, non-goals, current workflow stage, last completed
  action, next permitted action, blockers, and acceptance conditions;
- self-contained snapshots of the Use Case Scenario and the relevant model,
  glossary/BoK, and CML baseline information used for the current decision;
- the selected realization, trace links, decisions, assumptions, rejected
  alternatives whose rationale remains relevant, and unresolved issues;
- the proposed CML projection and its expected compatibility or migration
  effects;
- the current review and approval state, including the exact content hash to
  which each decision applies; and
- available validation results and the commands, tool/rule versions, inputs,
  outputs, and hashes needed to interpret or reproduce them.

An external source reference is not sufficient by itself. When an external
artifact contributes to the meaning of the current realization, the package
must include the normalized content or a bounded snapshot needed to continue
the review. The external identity, revision, and hash are retained as
provenance and as a freshness check.

Large or sensitive raw evidence may remain outside the project source. In that
case, the package still contains a sanitized conclusion, the evidence identity
and digest, the decision it supports, and an explicit statement of what cannot
be independently reconstructed without the retained evidence. Missing optional
history must not obscure the current selected state or next action.

All internal references must be stable identities or package-relative paths.
Absolute workstation paths may be recorded as non-authoritative provenance,
but must not be required to resolve the package.

### `manifest.yaml`

The manifest is the entry point. It identifies the current selected package
revision and inventories the files needed to reconstruct it. It records their
hashes, dependency order, required/optional status, and the package-wide
integrity digest. CBD Support starts by validating this manifest and must fail
closed if a required artifact is absent or its hash does not match.

### `resume.yaml`

The resume artifact is an explicit continuation cursor. It records:

- the current workflow stage and lifecycle state;
- the last completed action and resulting artifact identities;
- the next permitted action and its preconditions;
- pending human decisions and other blockers;
- invalidation/freshness checks required before proceeding; and
- the acceptance criteria for completing the next action.

It is a projection of the package state, not an independent authority. Its
content must agree with the realization, projection, approval, and validation
artifacts inventoried by the manifest.

### `sources/`

The source snapshots preserve the semantic basis needed to continue without
the original producer session. They contain the selected Scenario content,
relevant existing model elements and stable identities, the glossary terms and
definitions actually used, and the relevant CML baseline content. Each
snapshot also records its canonical source identity and hash so that a later
process can detect drift when the live project is available.

The snapshots do not replace the canonical project sources. They establish
what the current proposal meant and make review resumable. A CML mutation still
requires validating the live target against the recorded baseline or explicitly
reviewing the drift.

### `decisions.yaml` and `open-issues.yaml`

The decision record contains the accepted semantic choices, rationale,
accountable actor, affected identities, and supersession links. It includes a
rejected alternative only when that rejection is necessary to avoid reopening
or repeating a settled branch without new evidence.

The open-issue record identifies each unresolved question, its impact, owner or
required decision role, available options/evidence, and whether it blocks
semantic approval, CML projection, application, or validation.

### `realization.yaml`

The realization artifact records the reviewable semantic mapping from a Use
Case Scenario to elements such as:

- participating Objects and roles;
- responsibility allocation;
- Collaboration and Interaction;
- Services and Operations;
- Events;
- StateMachines and Transitions;
- Constraints and expected Outcomes; and
- source Scenario steps and their trace links.

It also identifies its schema, stable RDF-compatible identity, revision,
lifecycle state, and exact source Scenario identity/hash.

### `cml-projection.yaml`

The projection artifact describes the proposed relationship between an
approved realization and exact CML additions or updates. It records target CML
artifacts/elements, semantic mappings, expected compatibility impact, and
migration considerations.

It is not itself permission to mutate CML.

### `approval.yaml`

The approval artifact is separate from the semantic model so that approving a
model does not change the hash being approved. It identifies:

- the exact realization identity, revision, and hash;
- the decision and decision time;
- the accountable human approver;
- rationale and unresolved items; and
- the corresponding retained CBD Support review identity.

Detailed review/run history remains in CBD Support. The project-local approval
is the minimal attributable attestation required to bind the repository state
to that retained history.

## Three storage layers

The workflow distinguishes three storage layers.

### Project-owned source

`src/main/internal-model/` contains the proposal or selected model revision that
the project intentionally reviews and versions, together with the minimal
source basis, continuation state, decisions, projection, approval, and
validation artifacts needed to continue the development workflow without CBD
Support's retained state.

### CBD Support retained state

CBD Support retains review runs, proposals, alternatives, differences,
decisions, supersession history, evidence references, and attribution. Current
selected state, historical attempts, and review state remain distinguishable.

This retained state is a richer audit and collaboration store, not the sole
resume authority. CBD Support should be able to reconstruct or rehydrate the
current selected working state from `src/main/internal-model/`. Information
that exists only in retained state is either optional history or must be
materialized into the project package before it becomes a prerequisite for the
next workflow action.

### Disposable working output

`target/cbd-support/` is the provisional location for caches, tentative
extraction results, previews, and other reproducible working output. Disposable
output is not a review authority and is not committed as project source.

Raw prompts/responses or other sensitive provider payloads do not belong in
`src/main/internal-model/` by default. Project source may retain provider,
model/engine, tool/rule version, run identity, input/output hashes, and a
CallTree/evidence reference. Payload retention, masking, and access control
belong to the CBD Support persistence and security contracts.

## Workflow and gate

The intended route is:

```text
Use Case Scenario
  -> AI-assisted realization proposal
  -> src/main/internal-model/usecase-realization/...
  -> CBD Support review
  -> human approval bound to the exact realization hash
  -> approved CML projection
  -> skill-applied CML change
  -> Cozy generation and validation
  -> CBD Support validation evidence
  -> final human acceptance
```

At every durable handoff, CBD Support updates the project-local continuation
package before reporting the step complete. A new process resumes as follows:

1. locate and validate `manifest.yaml` and all required hashes;
2. load `resume.yaml`, the selected source snapshots, realization, decisions,
   open issues, projection, approval, and validation records;
3. reconstruct the current trace graph from stable identities;
4. if live sources are available, compare them with the recorded baselines and
   mark stale or invalidated state rather than silently rebasing it;
5. execute only the recorded next action whose preconditions hold; and
6. write the new selected state and continuation cursor back into the package
   before the next handoff.

If the package is incomplete or inconsistent, the valid next action is to
report the missing/inconsistent artifacts. CBD Support must not invent the
missing decision, approval, or semantic mapping from context outside the
package.

The central gate is:

> A skill must not apply a CML change unless the exact Use Case Realization
> revision/hash has an applicable human approval record.

An approval may require re-review when the Scenario, realization, referenced
model, glossary/BoK basis, mapping rule, or CML projection changes.

## CBD Support, skill, and human boundaries

CBD Support assists with discovery, proposal comparison, semantic differences,
traceability, impact analysis, review state, and retained evidence. It does not
silently select the final domain meaning or authorize a repository mutation.

The skill orchestrates repository inspection, CBD Support calls, presentation
of reviewable changes, and application/validation of explicitly approved CML
changes. It does not bypass the approval gate by translating prose directly
into CML.

Humans remain accountable for Scenario intent, responsibility allocation,
business validity, compatibility decisions, realization approval, and final
acceptance of the resulting implementation.

## Build and publication boundary

`src/main/internal-model/` must be excluded by default from:

- application runtime packaging;
- public APIs and generated service contracts;
- ordinary CML transformation unless explicitly selected by the approved
  projection workflow;
- documentation/site publication; and
- CAR/SAR publication artifacts.

CBD Support and the dedicated skill access the source root explicitly. Merely
adding a file below the root must not trigger implementation generation or
publication.

## Relationship to CBD Support's own requirement model

CBD Support's own requirement model remains:

```text
src/main/cml/usecase/application-component-composition.cml
```

That artifact describes CBD Support as a product. A consuming project's
`src/main/internal-model/` contains that project's intermediate realization and
review artifacts. The two model scopes must remain separate.

## Open decisions

1. the final source-root name and whether it is shared across every
   SimpleModeling project type;
2. the serialization format and schema/version identifiers;
3. the minimum Use Case Realization metamodel;
4. the exact RDF-node identity and revision contract;
5. lifecycle states such as `proposed`, `under-review`, `approved`,
   `changes-requested`, `rejected`, and `superseded`;
6. the approval and invalidation rules;
7. the CML projection and semantic-diff contract;
8. the CBD Support MCP/API surface for reading, proposing, reviewing, and
   retaining these artifacts;
9. the dedicated skill name, repository permissions, and mutation boundary;
10. sensitive-data, prompt/response retention, and CallTree policy; and
11. build-tool and packaging rules that guarantee the internal source root is
    not accidentally published or executed; and
12. the exact self-contained continuation schema, integrity digest, freshness
    checks, and CBD Support rehydration behavior.

## Promotion boundary

The next step is to promote only the stable portion of this direction into:

- CBD Support design for storage, review, persistence, and API boundaries;
- a schema/specification for project-local internal-model artifacts;
- CML projection rules;
- build and packaging exclusions; and
- the skill contract for reviewed CML additions and updates.

Until that promotion is complete, this journal preserves the agreed direction
without acting as an executable or normative contract.

Candidate Triage: COMPLETED
Canonical ID: DEV-CBD-002
Disposition: STRATEGY_ITEM
Strategy Record: docs/strategy/textus-cbd-support-development-strategy.md#9-development-item-status
Target Phase: -
Triaged On: 2026-09-09
