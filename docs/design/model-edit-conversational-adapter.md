# Conversational adapter

`ModelEditConversationalAdapter` separates a client's interpretation from the
host's authority. Request retains original text and typed Intent. Goal refinement
and addition select existing Goal/Actor references from the exact Palette context;
Slice refinement resolves its exact existing candidate record. Relationship
unlinking reads the full exact existing edge rather than accepting modified
endpoint data. Proposed new values remain explicit client inputs.
Distinct communication references for the same semantic subject require an
explicit exact client selection; the adapter retains ambiguity instead of taking
the first reference in an input collection.

Translation uses `ModelEditPalette.inspect` and `ModelEditSession.admit` and
returns either an immutable Proposal or retained UnresolvedIntent. Existing
command violations are carried as typed reasons with the original request.
The direct adapter's shared read-only subject predicate checks exact contextual
endpoints for both clients; it grants no application or authorization authority.
Clarification is explicit data, not a guessed repair or a default identity.
Proposal preparation owns no persistence, candidate application or external effect.

`applyProposal` passes the exact operation, successor and context through the
accepted direct mutation adapter with fresh ordinary authorization and host-owned
bounded resolution evidence. That adapter still enforces subject correspondence,
the same candidate service, source dependency and shared View confirmation. A
proposal object is never accepted as evidence of authorization or source existence.

The design is an adapter over the existing command vocabulary, not another
editing model or a provider orchestration framework. It does not parse prose for
identities, mutate external tickets, rewrite capability evidence, render CML or
grant canonical approval. External AI clients and genuine E2E proof remain with
later stages.
