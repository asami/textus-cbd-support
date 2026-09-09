# Evidence-Backed Component Composition and Dashboard Design

status=stable
decision_scope=P9-01A
updated_at=2026-09-10

## Purpose

This design fixes the shared responsibility, evidence, authority, and
no-hidden-winner boundary for Phase 9 composition and Component Dashboard
work. It is an immutable design decision for the documentation contract; it
does not introduce a runtime component, API, CML extension, or executable
specification.

The design treats composition and Dashboard as two projections over attributable
evidence. Composition begins with application intent and required capabilities.
Dashboard begins with one exact Component. Neither projection becomes a source
of catalog truth or a substitute for an existing source authority.

## Ownership decisions

CBD Support owns the bounded evidence inventory and the deterministic,
read-only projections built from admitted records. It owns projection
attribution, explicit absence, authorization/redaction visibility, and
limitation accounting.

The semantic provider owns only its attributable interpretation, candidate,
grouping, or proposal for a request. It cannot create a catalog fact, override
deterministic evidence, rank candidates as a canonical outcome, or select a
Component.

The human decision owns the explicit selection of an existing Component or the
promotion of a proposal. The decision is a separate attributed record with
scope, rationale, actor, and provenance. No source or projection can stand in
for this decision.

Dashboard owns the human-oriented projection of an exact Component's admitted
content, usage, operation, quality, and model-view navigation. Discovery owns
retrieval and presentation of attributable candidates and evidence. Review
continues to own its canonical evaluation, findings, assurances, unknowns,
limitations, reports, and conclusions.

These responsibilities are deliberately separate:

```text
catalog / design / model / runtime / BoK / Review sources
                         |
                         v
              CBD Support evidence inventory
                 |                    |
                 v                    v
          Composition projection   Dashboard projection
                 |                    |
                 +---- exact identity link ----+

semantic provider ---- attributable candidate/proposal
human decision ------- explicit selection/promotion authority
Discovery ------------ retrieval, ambiguity, and absence
Review --------------- canonical evaluation and conclusions
```

The arrows represent evidence flow and attributable projection only. They do
not represent an implicit ranking, selection, or authority transfer.

## Shared evidence inventory design

The inventory is a collection rather than a merged record. Each record keeps:

- stable source identity and bounded source location;
- source-owned authority scope;
- subject/component identity when established by the source;
- evidence statement and evidence URI or equivalent locator;
- availability, freshness, and observation state when supplied;
- authorization basis for this consumer;
- redaction or disclosure limits;
- explicit absence and its scope; and
- provider/source limitations, conflicts, and unresolved dependencies.

The inventory therefore supports a truthful projection when a source is absent,
stale, inaccessible, unauthorized, redacted, incompatible, or limited. It also
prevents a consumer from completing a source-owned profile by copying a field
from another authority. Availability and access state remain visible even when
the statement itself is withheld.

Authority is scoped to the source's contract. Catalog metadata remains catalog
evidence; semantic evidence remains semantic evidence; model and runtime facts
remain their source's evidence; and Review conclusions remain Review-owned.
CBD Support can associate records where an existing contract permits that
association, but association is not reconciliation into a new authority.

## No-hidden-winner decision

There is no automatic winner or canonical ranking. In particular, CML source
text, CML order, component naming, lexical similarity, diagram layout,
diagram style, presentation order, source priority, freshness, provider score,
or provider candidate order cannot select or rank a Component. A provider's
ordering may remain visible as provider-attributed output, but the Dashboard,
Discovery, and composition projections never promote it to a decision.

Only an explicit human decision can expose a selected disposition. Before that
decision, composition retains alternatives, gaps, unresolved evidence, and
proposals. A candidate's link to a Dashboard is an identity/navigation link,
not a selection signal. Conflicts and insufficient evidence remain visible as
ambiguity, absence, or limitation.

## Contract relationships

The design composes with existing contracts by reference:

- [ComponentReference Contract](../spec/component-reference-contract.md)
  remains the existence-level SIE/CBD handoff. It provides stable identity and
  evidence URI semantics, not selection or compatibility authority.
- [Semantic Requirement Matching Contract](../spec/semantic-requirement-matching.md)
  remains the independent semantic-evidence boundary. Its citations can
  explain or discover a candidate only through its declared association rules;
  they do not complete a catalog profile or choose a Component.
- [Component Knowledge Carrier Consumer Contract](../spec/component-knowledge-carrier-consumer.md)
  remains the admission and safe-projection boundary for declared Component
  knowledge. Carrier absence, rejection, availability, authorization, and
  withholding remain explicit.
- [Component Knowledge Carrier Integration Design](component-knowledge-carrier-integration.md)
  remains the ownership and projection design for admitted carrier metadata;
  this document does not broaden its resolver, transport, or runtime scope.

These references are dependency edges, not edits to the referenced contracts.
The shared composition/Dashboard boundary consumes their admitted identities
and evidence while preserving each contract's source authority and limitations.

## Projection boundaries

Composition may account for application intent, required capabilities,
attributed Component evidence, alternatives, gaps, proposals, provider
limitations, and explicit human decisions. Its initial plan remains transient
and read-only; application generation, assembly, build, publication,
execution, and durable approval/continuation storage are outside this design.

Dashboard may organize an exact Component's admitted content, usage, operation,
quality, source attribution, explicit absence, and model-view navigation. It
does not parse CML, reconstruct unsupported semantics from names or diagrams,
create a second mutable design source, or replace Review's conclusion.

Discovery may retrieve candidates and evidence with ambiguity and absence.
Review may be linked as an authorized canonical projection. Neither surface
selects a composition candidate or promotes a proposal.

## Consequences and deferred work

This boundary makes evidence gaps and access limits reviewable, keeps provider
and human authority distinct, and allows a candidate to navigate to an exact
Dashboard identity without silently becoming a selected fact. It also keeps
future view implementations constrained to admitted source contracts.

Persistence, approval history, continuation, CML parsing or mutation, runtime
and API changes, Dashboard Web implementation, composition implementation,
provider administration, and executable specifications require separate frozen
work. This design makes no claim that those capabilities currently exist.
