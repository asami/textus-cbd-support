# Usecase-driven component modeling direction

**Date:** 2026-08-15
**Scope:** CBD Support application-component discovery / usecase-driven modeling trial
**Status:** AGREED DIRECTION

## Purpose

This non-normative journal records the agreed model structure for deriving
implementation-part candidates from an application story. It is the working
direction for the dedicated CML use-case model in
`src/main/cml/usecase/application-component-composition.cml`.

The journal does not establish executable behavior by itself. Stable semantics
must later be promoted into the appropriate design, specification, and CML
language contracts.

## Artifact boundary

`src/main/cml/usecase/application-component-composition.cml` is the CBD Support
requirement model.  It contains the product vision, domain and system context,
capabilities, constraints, use cases, and—when modeled—use-case slices, slice
instances, and executable specifications.

The CML file does not carry notes about the modeling trial, evolving grammar,
modeling depth, or unresolved ownership and syntax decisions.  Those concerns
remain in this journal until a stable part is promoted into a dedicated design,
specification, or CML language contract.

The requirement model remains separate from
`src/main/cozy/textus-cbd-support.cml` while loading, validation, and generation
policy for the dedicated use-case model are being established.

## Product boundary

CBD Support manages attributable CAR and SAR information and provides humans or
MCP-connected generative-AI clients with structured information needed to
select and compose components.

CBD Support does not, merely by providing this information, automatically make
the final component-selection decision, generate missing components, assemble
the application, or execute it. Existing catalog facts, deterministic
inference, model inference, proposals, and human decisions must remain
distinguishable.

## Agreed model flow

The modeling flow is:

```text
Story
  Actor / Stakeholder / Goal / UseCase / Scenario / Step
                                      |
                                      v
                            ActivityCandidate
                                      |
                               normalize and merge
                                      |
                                      v
                                  Activity
  requires Capability
  governedBy Rule
  concerns Aspect
  consumes / produces DomainObject
  emits / receives Event
  changes State
  performedBy Actor or Component
  realizedBy Operation
                                      |
                                      v
                  Component / Service / Operation
                                      |
                                      v
               catalog matching, coverage, and gaps
                                      |
                                      v
       existing components / alternatives / proposed components
```

The story model and implementation model are both required. The story preserves
why the application behavior is needed; the implementation model provides the
structure needed to compare that behavior with CAR and SAR evidence.

## Step and Activity boundary

A narrative `Step` is not converted directly into one `Task` or one
`Operation`. A Step first yields one or more `ActivityCandidate` values.
Candidates can then be split, combined, normalized, or rejected to form stable
`Activity` elements.

This intermediary is required because:

- one Step can contain several activities;
- several Steps can describe one activity;
- an activity can require several operations; and
- a Step can express a decision, rule evaluation, state transition, event wait,
  or exception rather than an executable task.

`Activity` or `UseCaseActivity` is preferred as the abstract-model name. A bare
`Task` risks confusion with runtime job or task semantics.

Initial Activity kinds are:

- `UserTask`
- `SystemTask`
- `Interaction`
- `Decision`
- `RuleEvaluation`
- `StateTransition`
- `EventWait`
- `ExceptionHandling`

## Implementation-part model

The initial extraction target contains:

- `Component`, `Service`, `Operation`, and dependency relations;
- `Capability` for required or provided ability;
- `Aspect` for a concern that constrains or modifies capability realization
  across structural elements;
- `Rule` for explicit behavioral or structural conditions;
- `DomainObject`, including entity, value, and aggregate candidates;
- `Event`; and
- `State` and `Transition`.

Initial Rule kinds are:

- `ConstraintRule`
- `DerivationRule`
- `DecisionRule`
- `ReactionRule`
- `TransitionRule`
- `ObligationRule`

Constraint is therefore one kind of Rule, not the complete Rule model.

## Requirement and catalog separation

Implementation parts extracted from the story form a logical requirement-side
model. They are not catalog facts. CBD Support compares this logical model with
attributable CAR and SAR evidence and produces explicit coverage results:

- a matching existing component;
- a bounded set of alternatives;
- an uncovered capability or composition gap;
- a proposed component responsibility; or
- an unresolved result requiring more evidence or a human decision.

A proposed component remains distinct from an existing catalog component until
an explicit modeling and human-decision process promotes it.

## Use-case development and verification structure

The agreed canonical route from an application story to verification is:

```text
UseCase
  -> UseCaseSlice
    -> UseCaseSliceInstance
      -> ExecutableSpecification (TestCase)
```

`UseCaseSlice` is both a model element and a development-sizing technique.  It
cuts a UseCase into a development unit whose implementation and verification
do not exceed one iteration.  One iteration may contain multiple
UseCaseSlices.  A slice should remain a meaningful vertical increment of actor
value rather than a UI-only, API-only, persistence-only, or other horizontal
implementation task.

`UseCaseSliceInstance` binds the parameters and relevant initial state of one
slice to concrete values.  It records the corresponding expected output,
events, state changes, or failure independently of the test framework used to
verify them.

`ExecutableSpecification` is the model element and `TestCase` is its familiar
implementation-oriented name; they are not separate stages.  An
ExecutableSpecification realizes and verifies one UseCaseSliceInstance at a
specific technical boundary.  Multiple executable specifications may verify
the same instance through service, MCP, Web, or another delivery boundary.

The full structure is available even when a particular effort does not create
every artifact.  Modeling depth is selected according to development cost,
risk, reuse, and expected benefit:

```text
Level 1  UseCase
Level 2  UseCase + UseCaseSlice
Level 3  UseCase + UseCaseSlice + UseCaseSliceInstance
Level 4  complete route through ExecutableSpecification (TestCase)
```

Lightweight modeling stops at an earlier level; it does not bypass intermediate
elements by linking an ExecutableSpecification directly to a UseCase.  Missing
downstream elements remain absent or planned so that a later rigorous route can
be received without changing the model structure.

## Open product-boundary decisions

The requirement-modeling trial must still determine:

1. which natural-language interpretation responsibilities belong to CBD
   Support, SIE, Textus AI, or another versioned provider;
2. whether CBD Support calculates deterministic coverage and delegates only
   missing-component design, or admits a complete provider-supplied plan;
3. how publishers declare provided and required capabilities, supported use
   cases, constraints, and supporting Evidence;
4. how selection objectives such as stability, freshness, local availability,
   runtime compatibility, quality, component count, and cost are expressed
   without introducing a hidden winner; and
5. whether composition plans are transient projections or retained, versioned
   records requiring explicit human confirmation.

## Open modeling questions

The following remain intentionally open:

1. the final CML syntax for `Activity`, Activity kinds, relationships, and Rule
   kinds;
2. whether `ActivityCandidate` is persisted in CML or exists only as extraction
   provenance;
3. which extraction steps are deterministic and which are delegated to an
   attributable AI or semantic provider;
4. how confidence, alternatives, source spans, and human corrections are
   represented;
5. how logical Component, Service, and Operation candidates map to exact CAR and
   SAR identities and versions; and
6. the exact responsibility split among CBD Support, Textus AI, SIE, Cozy, and
   human review;
7. the final CML syntax and lifecycle states for UseCaseSlice,
   UseCaseSliceInstance, and ExecutableSpecification; and
8. how concrete instances and property-based executable specifications share
   parameter-domain, generator, and invariant semantics.

## Next promotion boundary

The next modeling increment should express this agreed structure in the
dedicated CML use-case file using established CML vocabulary where available.
Syntax that is not yet established should remain explicitly provisional. The
result must preserve the separation between story, extracted logical model,
catalog evidence, inference, proposal, and human decision.

Candidate Triage: COMPLETED
Canonical ID: DEV-CBD-001
Disposition: STRATEGY_ITEM
Strategy Record: docs/strategy/textus-cbd-support-development-strategy.md#9-development-item-status
Target Phase: Phase 9
Triaged On: 2026-09-09
