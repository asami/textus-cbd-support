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

This is a core Phase 11 rule, not merely an AI safety convention. Direct palette commands, built-in chat, ChatGPT, Codex, and future editing clients all use the same provisional-update lifecycle.

A modeling session may contain multiple provisional edits before approval. Approval should normally apply to an exact candidate revision/hash rather than interrupt exploratory modeling after every small operation.

```text
Edit Session
  +-- provisional edit
  +-- provisional edit
  +-- provisional edit
          |
          v
     View confirmation
          |
       Approval
          |
  Canonical promotion
```

The candidate View must make provisional state distinguishable from the current canonical state and should allow semantic diff/review to be inspected before approval. Rejecting or abandoning the candidate leaves the canonical model unchanged.

## Update Palette

When an update is requested, the reference view exposes an **Update Palette**. The palette is a model-editing client, not the owner of model semantics.

Two primary interaction styles are required:

1. **Direct commands** for frequent and deterministic operations, for example adding a Mono/Koto, Event, Command, relationship, or other known model element.
2. **Conversational instructions** for ambiguous, compound, exploratory, or context-dependent changes.

Both paths converge on the same semantic edit-operation boundary and update the candidate model first.

```text
Reference View
      |
Update Palette
   /       \
Direct    Chat/AI
   \       /
   Edit Intent
       |
Model Edit Operation
       |
CBD Support Model Edit Service
       |
Candidate Object Model
       |
Candidate View / confirmation
       |
Approval -> canonical change gate
```

A UI command such as `AddThing(Customer)` and a conversational instruction such as "add Customer as a thing" must ultimately use the same model operation.

## Model Edit Service

CBD Support provides services for updating candidate object-model state. Clients must not depend on textual CML rewriting as the primary editing mechanism.

Representative semantic operations include:

- add/update/remove model elements where permitted;
- add Mono/Koto concepts and glossary links;
- add Event/Command/Policy/Aggregate-related semantics;
- connect Actor, Use Case, Workflow, Event, Entity, and other existing semantic identities;
- modify structural relationships such as association/composition where supported;
- query applicable edit operations and validation requirements;
- return semantic diff and validation/review consequences;
- identify the exact candidate revision/hash presented for approval;
- promote only an approved candidate through the established canonical change gate.

Exact service names are implementation details. The contract is semantic: editing develops candidate state, confirmation is performed through projections, and only approved exact candidate state is reflected into canonical CML through established Phase 9/10 boundaries.

## External editing clients

The Update Palette is generalized as a **Model Editing Client**. CBD Support must not assume that the most capable editing client is embedded in CBD Support itself.

Supported client classes include:

- CBD Support direct Update Palette;
- CBD Support built-in conversational client when useful;
- ChatGPT through Plugin/MCP integration;
- Codex through MCP integration;
- future AI or automation clients.

External AI clients are valuable because they may combine CBD Support model context with other authorized context such as repository contents, BoK/glossary information, design documents, implementation code, tests, and ongoing reasoning. CBD Support remains responsible for model semantics and mutation safety; the external AI remains responsible for interpretation and orchestration.

ChatGPT and Codex may freely develop provisional candidate state within their authorization, but they do not thereby gain authority to commit that state to canonical CML. Canonical promotion remains behind explicit confirmation/approval and the existing change gate.

## Review-to-Edit transition

Review and Edit remain distinct but composable. A review finding can open the Update Palette with the finding as context, or an AI client can be asked to resolve the finding. No review result implicitly mutates the model.

A fix first changes the candidate model; the resulting View and semantic diff are then confirmed before canonical promotion.

Existing candidate-review, semantic-diff, approval, and canonical-source rules from Phases 9 and 10 remain applicable. Interactive editing is not a bypass around them.

## Solo Event Storming as the primary validation scenario

Solo Event Storming is the first strong use case for the architecture.

- Event Storming elements are represented through CML/object-model semantics rather than an isolated Event Storming data model.
- Event Storming View can project both canonical and clearly marked candidate state.
- A user can make multiple provisional changes through direct palette operations.
- A user can conduct the session conversationally with an AI facilitator.
- ChatGPT/Codex can act as an external editing palette and use additional authorized context.
- The user confirms the accumulated candidate through Event Storming View, semantic diff, and Review where useful.
- Only the approved exact candidate is promoted to canonical CML.

The same architecture must subsequently be reusable by Mono-Koto Analysis, Workflow, Entity/Event, Structure, StateMachine, and other model views.

## Architectural rule

Do not implement a separate editor and mutation model for every view. Views provide projections and contextual affordances; Model Edit Service owns semantic candidate mutation. Direct UI, conversational UI, Plugin, and MCP clients are alternative front ends to that same service boundary. Canonical mutation is a separate approval-gated promotion step.
