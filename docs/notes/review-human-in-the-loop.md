# Review as Human-in-the-Loop Decision Support

status=design-principle
updated_at=2026-09-30

## Purpose

Textus CBD Support Review is not primarily an automated judge. Its primary role is to assemble trustworthy evidence, component-level indicators, structural views, and change context so that a human reviewer can make a better engineering judgment.

The central principle is:

> Measure what machines can measure, and let humans concentrate on judgment.

Automated gates remain useful for deterministic policy and reproducible admission conditions. They do not replace the broader human review surface.

## Review Model

The Review experience is organized around four responsibilities.

1. **Evidence and KPI collection**
   - collect deterministic findings, test/specification results, model structure, dependencies, external surfaces, runtime evidence, and provider observations;
   - derive component-oriented indicators and deltas without pretending that every indicator is itself a quality judgment;
   - preserve provenance, limitations, Unknown, and before/after context.

2. **Review surface for human intuition**
   - present Component / Service / Operation / Aggregate / View / Workflow and related model projections at a level where structural shape is visible;
   - emphasize change, imbalance, unexplained mechanism growth, missing traceability, and other review candidates;
   - make it easy for an experienced reviewer to notice that something "looks wrong" even when tests and deterministic checks pass.

3. **Focused AI assistance**
   - AI explains a selected suspicious area, traces why a mechanism or structure appeared, investigates impact, and proposes alternatives or simplifications;
   - AI findings remain advisory unless independently supported by admitted deterministic evidence;
   - AI should reduce the cost of investigation, not replace the reviewer as design authority.

4. **Human judgment and admission**
   - the reviewer decides whether the candidate is acceptable, requires investigation or modification, or should be withdrawn;
   - the decision and its evidence can feed a Candidate-Admission workflow;
   - deterministic gates may block mechanically invalid candidates, while design admission can explicitly remain a human judgment.

Conceptually:

```text
Candidate / Change
       |
       v
Evidence + KPI + Model-up + Diffs
       |
       v
CBD Review Surface
       |
       +----> Human intuition / suspicion
       |              |
       |              v
       |       focused AI investigation
       |              |
       +--------------+
       |
       v
Human judgment
       |
       v
Admission / Revision / Withdrawal
```

## Component KPI

A Component KPI is a review indicator, not automatically a score of component quality.

Useful families include:

- executable specification and test coverage/status;
- capability/use-case-to-implementation traceability;
- model and structural deltas;
- dependency and external-library deltas;
- DSL versus direct/non-DSL usage;
- external I/O/effect surface;
- low-level/control mechanism diversity;
- state/persistence changes;
- Failure / Consequence coverage;
- runtime/operational observations when admitted.

Absolute values matter, but **delta is a first-class review signal**. A small capability change accompanied by a large increase in implementation mechanisms is particularly useful for directing human attention.

CBD Support should avoid collapsing heterogeneous KPIs into a single opaque quality score. The goal is to expose the shape and reasons behind the indicators.

## Human Intuition as a Review Sensor

Engineering intuition is treated as a legitimate review sensor rather than noise to eliminate.

A reviewer can often detect accidental complexity, an unnatural responsibility split, an unnecessary layer, an odd dependency direction, or excessive defensive machinery before there is a precise rule for it. CBD Support should therefore optimize views for recognition and comparison rather than attempt to encode every such judgment as a static rule.

The loop is:

```text
observe -> notice discomfort -> select -> investigate -> understand -> decide
```

This complements deterministic validation. Deterministic checks answer questions that can be specified in advance; the human review surface helps discover questions that were not known in advance.

## Relationship to CAR Review

The existing canonical CAR Review architecture remains valid:

- CBD Support owns the canonical Review Run, admitted Evidence, report, deterministic gate semantics, and projections.
- Providers remain attributable evidence sources.
- AI remains advisory.
- Reproducible gates remain appropriate for mechanically decidable admission rules.

This principle adds a broader product goal: the canonical report and its evidence should also support an interactive human review surface. A canonical gate result is therefore one input to human review, not the definition of the entire Review experience.

## Relationship to Candidate-Admission

Candidate-Admission gives the review decision an explicit process boundary.

CBD Review prepares the evidence and review context. A human or an explicitly defined admission policy makes the admission decision. This separation allows strict deterministic checks, AI assistance, and human intuition to coexist without confusing their authority.

The long-term objective is not "AI reviews the component." It is:

> CBD Support makes the component understandable enough that a human can review it efficiently, with machines doing the measurable and investigative work around that judgment.


## Review Up, Drill Down on Anomaly

CBD Support should make the normal human review path model-oriented rather than program-oriented.

The review surface connects requirement and realization abstractions through explicit traceability:

```text
Use Case / Use Case Slice
        -> Application Capability
        -> Component Capability
        -> Component / Service / Operation
        -> Aggregate / View
        -> Workflow / State Machine
        -> Executable Specification
        -> Program / Runtime Evidence
```

The reviewer should normally inspect model-up projections, traceability, deltas, and KPIs at the highest useful abstraction. Relationships across models are themselves review evidence. Missing, unexpectedly dense, weak, or disproportionate relationships can expose problems even when deterministic checks and executable specifications pass.

Programming-level correctness is primarily delegated to the programming harness: types and compilation, pure functional constraints, ADT, Algebra / DSL, Free Monad / Interpreter boundaries, and executable specifications. Human program-level review is an exception path, not the default review surface.

When the reviewer notices an anomaly or discomfort, CBD Support should support progressive drill-down from the abstract model to the related detailed model, executable specification, evidence, and only when useful the relevant program location.

The operational principle is:

> **Review Up, Drill Down on Anomaly.**

This preserves the value of a constrained AI-development environment. Routine line-by-line human code review should not become an alternate implementation authority that bypasses or weakens the harness.


## CNCF Validation/Test Metadata ABI as static review evidence

CBD Support consumes the CNCF Validation/Test Metadata ABI as the canonical semantic view of executable-specification/test declarations. It MUST NOT define a competing annotation grammar or independent suite registry.

The same source metadata used by sm-workflow for runtime selection becomes static Test Architecture evidence for Review:

~~~text
Executable Specification/Test Operation
  + CNCF Validation Metadata
       -> sm-workflow: runtime selection/execution/evidence
       -> cbd-support: static analysis/lint/KPI/change review
~~~

Useful static analysis includes:

- executable specification/test operations with missing or invalid purpose/feature metadata where project policy requires it;
- feature areas with weak or absent SMOKE/ADMISSION/FOCUSED coverage;
- suspicious concentration in FULL/HEAVY with little routine validation;
- purpose/feature distribution by Component/Feature/Use Case Slice;
- metadata changes that reduce ADMISSION or FOCUSED coverage without corresponding rationale/evidence;
- HEAVY/long-running declarations without appropriate expected-duration/rationale metadata when required by policy;
- declared expected duration compared with admitted sm-workflow runtime evidence when that evidence is available;
- Test Architecture deltas across Candidate/baseline revisions.

These are review indicators/findings, not an opaque quality score. The existing principle remains: expose measurable shape and delta so a human can judge whether the Test Architecture is appropriate.

CBD Support should present metadata changes as first-class architecture changes even when test logic is unchanged. For example, changing an operation from ADMISSION+FULL to FULL reduces routine admission coverage and should be visible in diff review.

Runtime observations remain owned by their source system. CBD Support may correlate CNCF metadata with sm-workflow execution receipts/duration/warnings, but it does not rewrite annotations from observations or become the runtime Test Suite authority.

This extends Review Up, Drill Down on Anomaly: a reviewer can inspect validation coverage at Component/Feature/Slice level and drill down to the exact executable specification/test operation and metadata only when an imbalance or anomaly deserves attention.
