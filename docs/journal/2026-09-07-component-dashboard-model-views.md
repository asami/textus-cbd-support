# Component Dashboard Model Views

Date: 2026-09-07

The Component Dashboard direction was refined around three product functions: Discovery, Review, and Dashboard. Existing Review dashboard work remains important, but Dashboard should become the general entry point for understanding a component rather than a Review-only presentation surface.

The Dashboard needs both operational overview and content overview. The Content View should summarize the meaning of the component and lead to DomainModel View and Use Case View.

DomainModel View was organized into Static Model and Dynamic Model. Static Model contains Structure View and Classification View. Structure View must treat composition, aggregation, and association rigorously. In particular, composition and aggregation should carry lifecycle semantics that can be consumed by implementation and validation rather than remaining UML drawing conventions. Ownership, independent existence, reassignment, deletion propagation, aggregate boundaries, persistence behavior, and API/command access are candidate consequences.

Classification View should combine generalization, trait, and powertype. Powertype is therefore not primarily a standalone view; the useful visualization is the complete classification structure, including inheritance/specialization, cross-cutting traits, and one or more explicit classification dimensions.

For Dynamic Model, Workflow should be the overview axis. It shows the progression of domain work and the participating domain elements. StateMachine is a deep-dive from Workflow or an affected domain element and explains the lifecycle of an individual subject. This creates a useful semantic chain from Use Case through Workflow and StateMachine to Entity, with reverse navigation as well.

Use Case View remains actor/goal oriented. It should connect an externally meaningful goal to the Workflow realizing it, the Domain Model elements it affects, and the operations/events involved.

The intended result is a Component Dashboard that behaves as a knowledge page for an executable component. It should let a developer move from an overview to structural ownership, classification, process, lifecycle, and use-case semantics without first reading implementation source.

Detailed design notes are recorded in `docs/notes/component-dashboard-content-model.md`.
