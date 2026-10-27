# Conversational candidate editing

The conversational adapter consumes original user text plus an explicitly typed
interpretation supplied by a Model Editing Client. The client may be an AI, but
this boundary supplies no model provider, natural-language inference engine or
privileged model authority. Text never allocates an identity, selects a nearby
subject or establishes a source fact. Ambiguous or unsupported intent is retained
with the original request, selected context and typed clarification reason.

Supported interpretations add/refine Goal intent, create/refine a Slice, link an
exact Goal/Use Case edge, link an exact Slice/Specification or Slice/Ticket edge,
and unlink an exact existing relationship. Actor and Goal refinement subjects
come from the selected candidate context, reusing shared communication references.
If several distinct admitted communication references share that semantic subject,
the client must select one exact reference; collection order never chooses it.
Slice refinement obtains the exact existing record for its proposed semantic
identity; its supplied successor retains the existing owner and logical identity
under normal admission. New Goal/Slice references, relationship identities,
operation reference, candidate successor and attribution are explicit inputs.

Proposal preparation retains the original request, context, typed operation and
supplied successor. It uses the existing Palette's ordinary authorization and
candidate admission; the result is a proposal, not an application, approval or
new authorization token. Unsupported intent, absent subjects and non-admitted
external links return typed unresolved intent without a candidate successor.
Application revalidates against the supplied current session through the accepted
`ModelEditModelMutation` route and produces the same shared candidate confirmation
as a direct Palette action. An AI-created Proposal value cannot bypass that route.

Bounded `ResolutionEvidence` is supplied separately by the trusted host, outside
the interpreted intent. Specification owner/project, stable identity and source
revision, and Ticket provider/owner/immutable ticket identity must match it exactly.
A fabricated or merely similar identifier has no resolved observation and cannot
be linked. Missing, unavailable, unauthorized, redacted and unsupported observations
retain their existing typed distinctions; no text or AI statement proves existence.
Ticket observation is optional metadata, not ticket completion or a tracker write.

No session wire schema, source-owner model, canonical writer, external ticket
client, store, Job or AI-provider framework is added. Source-only event/causal
edits remain outside the admitted command vocabulary. Actual external AI authoring
and later end-to-end approval/application proofs keep their existing Phase owners;
the typed executable specs below do not claim a live AI invocation.

Executable specification: `ModelEditConversationalAdapterSpec`.
Implementation is authored; native validation and independent review are pending.
