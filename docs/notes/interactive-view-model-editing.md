# Interactive View and Model Editing

## Direction

CBD Support views remain primarily reference projections of the canonical object model. Editing is introduced as an interaction layer over a reference view rather than by turning each view into an independent editor.

The interaction model distinguishes three responsibilities:

- **View**: inspect a projection of the model.
- **Review**: diagnose omissions, inconsistencies, weak traceability, and insufficient model strength without implicitly changing the model.
- **Edit**: intentionally develop a candidate model through semantic model-edit operations.

The same projection can therefore remain stable while Review and Edit are invoked as separate interactions.

## Provisional-update rule

An Edit operation does **not** directly mutate canonical CML. Editing first updates a provisional/candidate model. The user inspects that candidate through the normal View machinery and explicitly approves it before it is promoted through the canonical change gate.

```text
Edit Request
    |
Provisional Update
    |
Candidate Object Model
    |
View + Semantic Diff + optional Review
    |
User Confirmation / Approval
    |
Canonical Change Gate
    |
Canonical Object Model / CML
```

Direct palette commands, built-in chat, ChatGPT, Codex, and future editing clients all use the same provisional-update lifecycle. A modeling session may contain multiple provisional edits before approval; approval normally applies to an exact candidate revision/hash.

## Update Palette

When an update is requested, the reference view exposes an **Update Palette**. Two primary interaction styles are required: direct commands for frequent deterministic operations and conversational instructions for compound/exploratory changes. Both converge on the same semantic edit-operation boundary and update candidate state first.

Representative direct operations now include Actor Goal List operations such as adding/refining a Goal and linking a Goal to a Use Case, in addition to Mono/Koto, Event, Command, and relationship operations. Actor Goal operations must reuse Use Case Actor identity rather than create a view-local Actor.

## Model Edit Service

CBD Support provides services for updating candidate object-model state. Clients must not depend on textual CML rewriting as the primary editing mechanism.

Representative semantic operations include:

- add/update/remove model elements where permitted;
- add/refine Actor Goals and Goal-to-Use-Case relationships using shared Actor identity;
- add Mono/Koto concepts and glossary links;
- add Event/Command/Policy/Aggregate-related semantics;
- connect Actor, Goal, Use Case, Workflow, Event, Entity, and other existing semantic identities;
- modify structural relationships such as association/composition where supported;
- query applicable edit operations and validation requirements;
- return semantic diff and validation/review consequences;
- identify the exact candidate revision/hash presented for approval;
- promote only an approved candidate through the established canonical change gate.

## External editing clients

The Update Palette is generalized as a **Model Editing Client**. Supported clients include CBD Support direct UI, built-in conversational interaction, ChatGPT through Plugin/MCP, Codex through MCP, and future clients.

External AI clients may combine CBD Support model context with other authorized context such as repository contents, BoK/glossary information, design documents, implementation code, tests, and ongoing reasoning. They may develop provisional candidate state within authorization but do not thereby gain authority to commit canonical CML.

## Actor Goal List as an editing surface

Actor Goal List is a useful early-stage interactive surface because it exposes stakeholder intent before detailed behavioral modeling.

```text
Actor Goal List
  -> Goal refinement
  -> Goal / Use Case linkage
  -> Candidate Actor Goal List View
  -> navigate to Mono-Koto / Event Storming
```

The same candidate can then be explored through other projections. Review can detect, for example, a Goal with no realizing Use Case or an important Goal with no Event Storming behavioral realization. These findings can transition into explicit candidate Edit operations.

See `docs/notes/actor-goal-list-view.md` for the projection-specific semantics.

## Solo Event Storming as the primary validation scenario

Solo Event Storming remains the first strong end-to-end use case. Actor Goal List strengthens its starting context:

```text
Actor -> Goal -> Use Case
                 |
                 v
          Event Storming
```

A user may establish or inspect Actor Goals first, select a Goal/Use Case context, and then develop Event Storming semantics through direct or AI-assisted provisional edits. Event Storming and Actor Goal List project the same candidate model from different stakeholder perspectives.

The architecture must subsequently be reusable by Actor Goal List, Mono-Koto Analysis, Workflow, Entity/Event, Structure, StateMachine, and other model views.

## Architectural rule

Do not implement a separate editor and mutation model for every view. Views provide projections and contextual affordances; Model Edit Service owns semantic candidate mutation. Direct UI, conversational UI, Plugin, and MCP clients are alternative front ends to that same service boundary. Canonical mutation is a separate approval-gated promotion step.
