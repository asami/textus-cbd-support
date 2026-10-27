# Model edit source and shared projections

Stage 12.3 consumes Cozy's `cozy.cml.model-edit-production.v1` through its
documented JSON boundary. The Scala 2 producer remains a separate JVM; its
classpath is selected independently of the existing CAR generator. CBD keeps
source owner, source revision, producer identity, coverage and explicit endpoint
types. Caller-supplied mappings associate source references with CBD semantic
and record references; neither spelling nor collection position allocates them.

The build helper owns the uniquely named `selectedModelEditProducerRoot` key.
It cannot reuse the Capability helper's `selectedProducerRoot` label: SBT key
identity does not include the declaring Scala object. The independent selection
adds only the model-edit test JVM metadata and does not activate unrelated
Capability source/resource generators or CNCF source dependencies.

The source publication and the immutable editing candidate are separate values.
Actor Goal, Event Storming and development trace projections use one selected
candidate and its source binding. Context and external dependency links remain
distinct from causal/reaction links. Source fields that commands do not edit
remain in the source document and survive candidate rendering.

Candidate v2 requests carry explicit replacements, additions, removals and
complete proposed coverage. Rendering returns proposed CML only. Admission into
Phase 10 requires exact source, candidate, mapping, captured payload dependencies
and the existing realization/continuity/diff/review/approval validators. Rendering
and a View never grant canonical application authority. Unsupported or incomplete
source mappings retain inspection and block promotion.

Review uses a complete project finding policy and explicit per-family scope
evidence. Complete empty families can prove absence; unavailable, unsupported,
unauthorized and unresolved observations cannot. Phase 11 demand queries are
read-only inputs and retain their existing unavailable Use Case boundary.

The Phase 11 adapter inspects typed diagnosis gap reasons before deriving Review
scope: `ProjectionNotSupplied` remains Unavailable even inside a successful query
result, while `AbsentSelectedReference` retains complete absence. Its original
observations and derived `CapabilityDemand` values are returned together. Shared
View projection consumes these same typed values and returns one observation per
selected Use Case beside its legacy reference vector. Missing input has an
explicit Unavailable observation, not a synthetic complete-empty observation.
No session wire profile or upstream query meaning changes.

Implementation and native qualification are pending for EDIT12-03. This document
does not close the Stage, Phase or projection follow-ups.

Independent Actor records are necessary for an Actor-declared, Goal-empty canonical
basis. V2 session payloads carry them and typed Slice fields explicitly; v1 is preserved
for the accepted Actor/field-empty shape. Source Actor-Goal paths are never inserted as inferred direct
Use Case communication edges.

Confirmed views retain the canonical source selection separately from the rendered
candidate source. Candidate Goal intent and Slice values override the display of
those records; immutable behavioral/causal/context evidence remains source-owned.
The normal canonical adapter emits existing model-context and realization records,
including explicit external endpoint mappings. Assertion/source-reference identities
are supplied by the caller. The source-mapping capsule is a typed Projection in the
existing package and complete review subject, not a second store.
