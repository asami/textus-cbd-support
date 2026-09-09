# Evidence-Backed Component Composition Contract

## Scope and status

This specification defines the shared evidence, responsibility, authority, and
no-hidden-winner contract for Phase 9 composition and Component Dashboard work.
It is the normative boundary for P9-01 through P9-03 and the shared foundation
for the later composition and Dashboard contracts. It specifies semantics; it
does not claim that the composition plan, Dashboard, or any new runtime surface
is implemented.

The contract applies to evidence used by application-component composition and
to evidence projected by a Component Dashboard. It does not replace the
existing ComponentReference, Semantic Requirement Matching, or Component
Knowledge Carrier contracts.

## Responsibility and ownership

Each responsibility has one owner. An owner may consume another responsibility's
evidence, but consumption does not transfer source authority or permit the
consumer to create a fact for the source.

| Responsibility | Owns | Must not own |
| --- | --- | --- |
| CBD Support | Admission of bounded evidence into the shared inventory; attribution, availability, authorization, redaction, absence, and limitation accounting; deterministic composition and Dashboard projections over admitted evidence | A source's unpublished facts; a hidden component winner; semantic or model meaning not present in admitted evidence; a new authority for CML, BoK, runtime, or Review |
| Semantic provider | An attributable interpretation, candidate, grouping, or proposal for the current request, including its provider identity, evidence basis, and limitations | Catalog facts, component selection, a human decision, a replacement for deterministic evidence, or an un-attributed ranking |
| Human decision | An explicit decision that selects an existing component or promotes a proposal, with decision identity, rationale, actor, and provenance | Silent inference from evidence, names, CML, diagrams, Dashboard layout, or provider output |
| Dashboard | A human-oriented projection for one exact Component from admitted evidence, with source attribution and explicit absence/limitation visibility | Component discovery, catalog facts, composition selection, Review conclusions, or an independent model source of truth |
| Discovery | Retrieval and presentation of attributable candidate/component evidence and its explicit ambiguity or absence | Selection, ranking, composition disposition, Dashboard conclusion, or conversion of semantic evidence into a component fact |
| Review | Its existing canonical evaluation, findings, assurances, unknowns, limitations, and conclusions | Composition selection, Dashboard content authority, catalog facts, or a competing Review conclusion from a projection |

CBD Support owns the shared projection boundary, not the authority of every
source admitted into that boundary. The semantic provider and human decision
boundaries remain distinct even when the same user or process supplies both.

## Shared evidence inventory

The shared inventory is an attributed collection of evidence records. An
inventory is not a merged profile and is not a selection result. Every record
retains enough metadata for a consumer to distinguish what was observed, who
asserted it, whether it can currently be used, and why it may be incomplete.

An evidence record has the following required semantic dimensions:

- **Source:** the stable source identity, source kind, observation/reference
  location, and evidence URI or other bounded locator. Source identifies where
  the statement came from; it is not itself a preference order.
- **Authority:** the source-owned subject and scope of the statement. Authority
  answers what the source is allowed to assert, such as a catalog's published
  Component metadata, a BoK provider's terminology, a semantic provider's
  current-query interpretation, or Review's canonical evaluation. Authority is
  not permission to select a component.
- **Availability:** whether the evidence is available for this projection,
  absent, stale, inaccessible, withheld, malformed, or otherwise unavailable.
  Availability is observable independently of the statement's content.
- **Authorization:** the policy or admission basis that permits the evidence to
  be consumed in this context. Missing, expired, denied, or insufficient
  authorization is recorded as a limitation or unavailable state; it is not
  silently treated as evidence absence.
- **Redaction:** the fields or content withheld from a consumer and the reason
  for withholding them. Redaction preserves the existence and attribution of a
  bounded evidence record without exposing secrets, credentials, protected
  content, or disallowed paths.
- **Absence:** an explicit statement that expected evidence is not present or
  cannot be established, including no-match, ambiguous selection, missing
  metadata, unavailable provider, or an unadmitted carrier. Absence is not an
  empty successful observation and must remain attributable to its scope.
- **Limitations:** provider, source, freshness, compatibility, authorization,
  redaction, bound, and projection constraints that prevent a stronger claim.
  A limitation remains visible beside the evidence it qualifies.

The inventory also retains the evidence statement, stable subject identity when
one exists, observation time/freshness when supplied, and conflict relations
between records. A consumer may project a bounded view, but it must not discard
the metadata needed to explain an absent, inaccessible, redacted, stale, or
conflicting result. Evidence from different authorities is not silently merged
to complete a missing field.

## Source and authority rules

The following source roles remain separate:

- a catalog owns the Component identity and catalog-published Component facts;
- a canonical design source owns design semantics where its contract says it
  does;
- a model or runtime source owns only its admitted model or runtime evidence;
- a BoK or semantic source owns its terminology or current-query semantic
  evidence;
- a semantic provider owns its attributable interpretation or proposal;
- Review owns its canonical evaluation and conclusions; and
- a human decision owns only the decision explicitly recorded by the human
  decision contract.

An evidence source may describe a candidate or support a decision, but no source
becomes a selection authority merely because it is available, fresh, preferred
for retrieval, more detailed, named earlier, or displayed first. Purpose-specific
source authority describes the scope of a statement and never supplies an
implicit winner.

## No-hidden-winner and no-inference contract

No component winner, canonical ranking, or selected disposition may be derived
from any of the following:

- CML source text, syntax, source order, or an unimplemented interpretation of
  CML;
- a component name, title, namespace, naming similarity, lexical order, or
  naming convention;
- diagram layout, line style, visual proximity, grouping, size, color, or
  presentation order;
- catalog/source priority, freshness, availability, provider identity, or
  Dashboard ordering;
- a semantic provider's suggestion, score, confidence, candidate order, or
  proposal; or
- a Discovery, Dashboard, or Review projection.

An explicit human decision is the only selection authority. The decision must
identify the selected existing Component or proposal, its decision scope, the
rationale, and the decision provenance. Until that decision is admitted, a
candidate remains a candidate, a competing candidate remains an alternative,
and an unsupported responsibility remains a gap or unresolved item according to
the applicable composition contract. A provider-supplied order may be retained
only as attributed provider output; it is never promoted to a canonical ranking
or selection by presentation.

Conflicting or insufficient evidence is projected as conflict, ambiguity,
absence, or limitation. It is not resolved by choosing the most plausible
record. A Dashboard may organize evidence for comprehension, and Discovery may
return candidates, but neither may turn organization or retrieval order into a
decision.

## Composition boundary

Composition consumes the shared inventory to account for application intent,
required capabilities, Component evidence, alternatives, gaps, proposals,
unavailable providers, and explicit human decisions. CBD Support may perform a
deterministic coverage projection from admitted evidence and an admitted human
decision. The projection retains the evidence and disposition provenance for
each capability.

The composition projection is transient and read-only for this contract. It does
not generate, assemble, build, publish, execute, or persist an application. A
composition candidate may link to an exact Dashboard Component identity, but
the link does not change the candidate into a selected Component fact.

## Dashboard, Discovery, and Review boundary

Dashboard consumes an exact Component identity and the evidence admitted for
that identity. It may project content, usage, operation, quality, model-view
navigation, attribution, explicit absence, redaction, and limitations only when
the corresponding authority and contract are admitted. A Dashboard projection
does not infer model semantics from CML source, names, or diagrams and does not
replace a canonical Review conclusion.

Discovery retrieves and exposes attributable evidence, candidates, source
diagnostics, ambiguity, and absence. Discovery does not choose among candidates
or fill missing Component fields from semantic evidence.

Review remains the owner of its existing canonical Review evidence and
conclusions. A Dashboard or composition projection may link to an authorized
Review result, but it may not reinterpret a Finding, Unknown, Assurance, gate,
or limitation as a new conclusion.

## Contract preservation

This specification preserves the following existing contracts:

- `docs/spec/component-reference-contract.md` remains the boundary for the
  existence-level SIE/CBD ComponentReference handoff. A reference does not
  become a selection decision or compatibility assertion.
- `docs/spec/semantic-requirement-matching.md` remains the boundary for
  independent semantic evidence, current-query scope, explicit catalog-term
  association, citation identity, and no-profile-merging behavior. Semantic
  evidence does not become a Component fact or selection authority.
- `docs/spec/component-knowledge-carrier-consumer.md` and
  `docs/design/component-knowledge-carrier-integration.md` remain the
  boundaries for declared carrier admission, exact selection, bounded safe
  projection, explicit absence/rejection, and usage-reference withholding.

Nothing in this shared contract changes those documents, their ownership, or
their implementation status.

## Required consumer behavior

Any future composition or Dashboard consumer of the shared inventory MUST:

1. preserve source and authority attribution for every projected statement;
2. expose availability, authorization, redaction, absence, and limitations
   whenever they qualify the result;
3. keep evidence from distinct authorities distinct unless an existing contract
   explicitly defines an attributable association;
4. represent ambiguity and conflict without selecting a hidden winner;
5. use an explicit human decision before exposing a selected disposition; and
6. avoid claims about runtime behavior, public APIs, CML semantics, or
   executable specifications that have not been separately implemented and
   admitted by their owning contract.

## Deferred work

This contract does not define persistence, approval history, continuation,
durable internal-model storage, a Dashboard Web route, a composition runtime,
provider administration, CML parsing, or executable specifications. Those are
separate, explicitly scoped work and must not be inferred from this document.
