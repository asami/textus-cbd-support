# Aggregate / View / Workflow Dashboard Alignment

status=decision
date=2026-09-20
phase=[Phase 9](../../../phase/phase-9.md)

## Decision

Use **Aggregate / View (Read Model) / Workflow** as the three primary navigation axes of the Textus CBD Support Model Viewer.

- Aggregate is the write/update and consistency-boundary perspective.
- View is the read/projection perspective.
- Workflow is the process/progression perspective.

Entity, Structure, Classification, Event, StateMachine, Use Case, and Traceability remain first-class drill-down and cross-cutting projections. The three axes organize navigation; they do not redefine the canonical model.

## CNCF alignment

The same three axes are used by the CNCF Dashboard for runtime observation:

- CBD Support presents the **Designed Model**.
- CNCF Dashboard presents the **Observed Model**.
- CNCF Job Management remains a cross-cutting asynchronous execution-management and diagnostic layer, not a fourth application-model axis.

Representative semantic navigation is:

```text
Aggregate -> Event -> View
Workflow -> Operation/Command -> Aggregate
Workflow -> Query -> View
```

Where stable identities and authorization permit, navigation is bidirectional between CBD Support model projections and CNCF runtime projections. Missing or ambiguous mappings remain explicit; neither surface reconstructs identity from names, timestamps, labels, or diagram layout.

## Planning consequence

Phase 9 gains explicit Aggregate View, View (Read Model) View, three-axis navigation, and Designed/Observed cross-surface navigation checklist items. CNCF Phase 87 owns the corresponding runtime-management surface.
