# Contextual Update Palette

The Palette is a deterministic editing client of `ModelEditService`. It selects
an exact session, candidate, scope and typed Actor, Goal, Use Case, Slice or
relationship subject. Actor Goal, Mono-Koto, Event Storming and Use Case detail
surfaces share the same semantic commands. A label, layout position or surface
name grants no authority and cannot establish a relationship.

Palette inspection requires an active session and the exact caller-admitted
authorization session/scope. Entries are the intersection of authorized command
kinds and operations applicable to the selected existing subject. The ordering
and input descriptions are deterministic. Unknown or out-of-scope subjects are
rejected. Commands requiring a current Slice or relationship are offered only
when that candidate contains one for the selected subject.

Actor context supports adding a Goal. Goal context supports refinement and exact
Goal/Use Case link/unlink. Use Case context supports Slice creation/refinement,
Goal link/unlink and Specification/Ticket link/unlink for its own Slices. Slice
context supports its refinement and its independent Specification/Ticket edges.
An exact relationship context exposes only its own unlink operation. Refined
Slices retain semantic identity, owning Use Case and logical record identity;
the caller supplies their successor revision. External link resolution and all
command admission remain owned by the accepted candidate service.

Mono-Koto selection uses an existing typed projection and an exact semantic
identity present as its subject or reference. Event Storming selection requires
the same candidate/scope and an exact editable node identity. These adapters
never derive editable subjects from display text. Source-only event elements
and causal/context relationships have no new mutation command in this contract;
their existing source-owner read-only boundary is retained.

Direct submission supplies the normal typed operation, successor candidate,
authorization and bounded reference resolution. It must match both an offered
kind and the selected subject's actual command endpoints. The adapter invokes
`ModelEditService.applyOperation` and projects that returned candidate through
the shared View. Failure returns no applied result. Existing operation/history,
source dependency, resolution, authorization and successor rules are unchanged.

The rendered Palette presents applicable inputs. Its separate confirmation
shows candidate versus canonical basis, exact candidate revision, accumulated
operations and independent Slice links. Rendering supplies no approval or
canonical application authority. HTML values are escaped; no endpoint, script,
external ticket operation or alternative persistence store is introduced.
Linking/unlinking a TicketRef changes candidate relationships only. Actual
external ticket creation/update requires a separate explicitly authorized
adapter and is not an implicit link side effect.

Executable specifications: `ModelEditPaletteSpec`, `ModelEditModelMutationSpec`.

Stage 12.4 implementation is authored; native validation and independent review
are pending. Authorship alone does not close the checklist.
