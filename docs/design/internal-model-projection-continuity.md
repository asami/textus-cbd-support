---
status: draft
decision_scope: P10-21A--P10-21B1
updated_at: 2026-09-28
---

# Internal-model Projection Continuity Design

This design explains the closed P10-21B1 binding defined by the
[Internal-model Projection Continuity Contract](../spec/internal-model-projection-continuity-contract.md).
That contract is normative. The [Internal-model Package
Contract](../spec/internal-model-package-contract.md) and accepted
[Internal-model Semantic Realization
Contract](../spec/internal-model-semantic-realization-contract.md) retain their
structural and semantic authority. The Phase 9 [Canonical Component Design
Model Contract](../spec/canonical-component-design-model-contract.md) remains
the only semantic authority.

P10-21A established logical continuity admission. B1 makes its persisted
binding grammar closed so that B2 can implement one unchanged interpretation.
P10-21B2A adds an explicit V2 interpretation when that binding consumes a V2
realization; it does not reinterpret V1 bytes. This slice does not implement
or prove the eight values, close P10-21, or close the Phase.

## One pass, one ledger, eight constructors

The package manifest is read once through its verified inventory/digest pass.
That pass selects one present `role=projection` artifact whose `dependsOn`
names the selected `role=realization` artifact. The realization already binds
the accepted source snapshots, exact Component/context/selected-Use-Case scope,
elements, relationships, assertion lanes, source references, and conditions.
The projection binding consequently adds only closed references to this one
ledger.

```text
verified package manifest/inventory pass
                 |
                 v
selected realization <--- dependsOn --- selected present projection binding
                 |
                 v
accepted Phase 9 CCDM identity and complete evidence ledger
                 |
                 v
one closed binding of references (no semantic copies)
                 |
                 +--> MonoKotoProjection
                 +--> UseCaseCommunicationProjection
                 +--> EntityModelProjection
                 +--> EventModelProjection
                 +--> StructureViewProjection
                 +--> ClassificationViewProjection
                 +--> WorkflowProjection
                 +--> StateMachineProjection
```

This one-pass route matters: a consumer can independently reproduce the
admitted input from exact package bytes and manifest context. It never requires
a previous chat, provider response, database cache, filesystem encounter order,
label, display layout, or an inferred reconstruction. Missing, duplicate, or
non-dependent selections fail before projection construction.

The binding repeats only the exact realization artifact ID and its three scope
identities. Each view record then references one exact in-scope element or
relationship, its complete assertion-lane and condition ID links, and one
directly witnessed Phase 9 role. It deliberately does not carry a label, path,
hash, endpoint, direction, tie key, navigation target, copied source, copied
condition, or mutable payload. Those omissions ensure that the persisted view
cannot become a competing semantic authority.

For an accepted V1 realization, the binding remains
`ccdm-projection-binding-v1` / `schemaVersion="1.0"` with its unchanged
historical grammar and semantics. An accepted V2 realization is consumed only
by `ccdm-projection-binding-v2` / `schemaVersion="2.0"`; the root, scope,
family, record, complete link-array, gap, and sequence shapes remain closed and
unchanged. The version pair keeps a consumer from silently recasting prior
binding bytes or treating an association as a view-local fact.

## Exact roles without view conversion

The eight families share a ledger but do not share meanings. The closed table
in the contract admits only the existing case-sensitive Phase 9 constructor
tokens, plus the selected `use-case` subject and the narrow existing Use Case
flow/grouping tokens. A record passes only when its realization element `kind`
or relationship `role` is the exact same token and a canonical assertion on
that identity carries the source-snapshot-backed `kind:<token>` or
`role:<token>` witness.

This is deliberately stronger than a label match or a view-local category.
Mono does not turn into Entity, Koto does not turn into Event, Structure does
not turn into Classification, and a Workflow flow cannot become either a
StateMachine transition or a transition adjunct. The selected Use Case remains
the exact `use-case` subject; it does not acquire another role because a name
looks compatible. Relationship endpoints and direction are likewise read only
from the realization. Because the binding has no endpoint or direction fields,
it cannot reverse or rewrite a relation; a forged endpoint is rejected as an
unknown field.

## Evidence sidecar and representability gate

The projection binding keeps identifiers, not reduced evidence. Every normal
record's canonical, enrichment, and condition arrays equal the realization
target's complete sorted link arrays. Canonical and enrichment lanes therefore
remain separate, and all source/condition records needed to interpret their IDs
remain in the adjacent realization ledger. The continuity result retains this
realization plus the complete binding beside the eight Phase 9 values: that
pair is the full evidence sidecar.

This sidecar prevents a subtle loss at the existing Phase 9 constructor
boundary. Its DTOs carry one `ComponentDashboardSourceAttribution` and one
compressed `ComponentDashboardCondition`, while a realization may preserve
multiple distinct assertion/source/condition records. B2 must use a DTO only
when it can represent the bound evidence without selecting or collapsing a
source or condition. If it cannot, B2 rejects that record or uses only an
attributable bounded gap supported by the existing DTO. It may not choose the
first or last source, concatenate condition details into a synthetic winner, or
claim that a presentation ordering makes evidence representable.

The V1 representability gate is concrete. For every bound normal record, B2
collects source-reference IDs from all canonical assertions, all enrichment
assertions, and all linked conditions. For a gap, it collects the ID from its
one admitted condition. All collected references must resolve to exactly one
distinct source-reference ID, and that reference's `target` must be exactly
`Some(kind, identity)` of the bound target. Zero, missing, distinct,
wrong-target, or ambiguous references reject; B2 cannot select a first or last
source or borrow another relation's source. It maps the one reference to
`ComponentDashboardSourceAttribution(sourceId = source.identity,
authorityScope = source.authority, sourceLocator = sourceAnchor)`, while the
full reference and ledger remain in the sidecar.

The V2 gate has a narrower, explicit attribution rule: one DTO receives the
source reference of its one direct canonical same-identity
`kind:<viewRole>` or `role:<viewRole>` witness. A missing, multiple,
different-source, wrong-target, or conflicting role witness rejects rather
than selecting one. Association and sequence source references are still
complete evidence, but remain in the retained V2 realization sidecar rather
than being collapsed into the DTO's one attribution. The exact B1 condition
compression and one-condition gap rules continue unchanged.

`ComponentDashboardCondition` has adapter sentinels
`availability="unverified"` and `authorization="unverified"`; these are not
CCDM facts and never authorize navigation (`navigationTarget=None`). All other
optional fields start as `None`, with limitations empty. In ascending
condition-ID order, B2 maps each exact condition `kind` and `detail` without
coercion: `absence` to `explicitAbsence=Some(detail)`; `ambiguity` to
`ambiguity=Some(detail)`; `conflict` to `conflict=Some(detail)`; `malformed` to
`malformedEvidence=Some(detail)`; and `limitation` to one exact detail appended
to `limitations`. `authorization-redaction` and `availability-staleness` append
the typed literal `kind:detail` to `limitations`, while `redaction` and
`staleness` remain `None` because their combined source condition cannot be
split into a narrower claim. More than one condition for a single-valued
optional slot rejects rather than choosing or concatenating, and empty
conditions leave all optional fields empty. Each nested Phase 9 DTO endpoint
and record must use its own exact target evidence; it cannot copy attribution
or condition from its parent. Full condition IDs, kinds, and sources remain in
the sidecar. Unknown or nonrepresentable conditions or DTO shapes reject, and
the existing bounded gap is admitted only by the frozen one-condition gap rule.
There is no default to available or admitted.

A gap is not an absence claim. It points to one exact in-scope target with the
target's complete condition links, admitted only when there is exactly one
condition. That condition's `affectedKind` and `affectedIdentity` must exactly
match the target's kind and identity; a shared opaque identity string never
permits kind inference. The binding's `requestedFieldOrScope` is exactly that
condition's `detail`, not paraphrased or invented prose, and is the bounded
requested field or scope. It carries no canonical or enrichment assertion. This
is how unavailable, ambiguous, conflicting,
authorization/redaction, availability/staleness, malformed, and limitation
state can remain visible without inventing a fact, source, endpoint, role, or
sequence.

## Source-owned sequence versus presentation order

Most view output retains the existing constructors' identity-first deterministic
order. That order is reproducibility only; it supplies neither chronology nor
causality, authority, priority, or an evidence winner. The profile fixes its
eight family positions and record identity tuple order so canonical re-encoding
is byte-stable, but neither order is semantic input.

Only `UseCaseCommunicationFlowStep` and `WorkflowProjectionFlowRelation`
relationships have source-owned order. Each must name a canonical assertion on
the same relationship whose source-snapshot-backed literal content is
`sequence-key:<opaque-key>` with a nonempty exact suffix. The constructor may
use that key for the ordered flow; it cannot recover it from a JSON array
position, manifest order, prose, label, stable tie key, or another view. A
missing or malformed witness rejects the ordered relation, except for the
separately attributable single-condition gap case.

For the nested Phase 9 `flows[].steps` shape, the accepted realization supplies
one exact source-backed graph. The selected Use Case element `U` has
`kind=use-case`. A bound flow relationship `F` has
`role=UseCaseCommunicationFlow`, `sourceElementIdentity=U.identity`,
`targetElementIdentity=V.identity`, and `direction=source-to-target`; `V` is a
distinct target element with a directly canonical source-backed
`kind:use-case-flow` witness. A bound flow-step relationship `S` has
`role=UseCaseCommunicationFlowStep`, `sourceElementIdentity=V.identity`,
`targetElementIdentity=W.identity`, and `direction=source-to-target`; `W` is a
distinct target element with a directly canonical source-backed
`kind:use-case-flow-step` witness. `F` and `S` retain their own same-identity
canonical `role:<exact token>` witnesses, and `S` its own
`sequence-key:<nonempty>` witness.

Each selected `V` belongs to exactly one `F`, and each selected `W` belongs to
exactly one `S`. A selected `S` matches exactly one selected `F` solely by
`F.targetElementIdentity == S.sourceElementIdentity`. Missing, duplicate,
ambiguous, cross-scope, or wrongly directed chains reject. `F.identity` and
`V.identity` map to the flow `semanticRelationshipId` and `semanticTargetId`;
`S.identity` and `W.identity` map likewise for the step. `flow.steps` is grouped by
that exact matched `V` and ordered by the exact source-owned sequence key;
duplicate sequence keys within one flow reject. Array order, labels, loose
endpoints, and presentation tie keys never establish membership or order. This
adds no binding field and no realization schema or API.

The later B2B constructor proof uses the closed V2 associations only for the
frozen all-eight mapping: `owner` relates a Structure assertion relation to its
exact parent Structure relation, a Classification dimension assertion to its
exact dimension element, a Workflow flow to its exact Workflow element, a
StateMachine transition to its exact StateMachine element, and a StateMachine
adjunct to its exact transition relationship. `subject` relates a
Classification relationship to its exact subject element; optional
`affected-*` claims identify only the exact Structure or Classification DTO
IDs that need them. Entity metadata subject/target, Use Case flow/step graph,
Mono-Koto relations, and Event relations continue to use their existing exact
endpoint rules. Missing, duplicate, cross-scope, wrong-kind, or contradicted
associations reject; B2B cannot substitute a visible owner or silently leave a
required nested value empty.

The implementation retains the accepted realization and complete binding next
to the eight constructed values. It obtains both through the one verified
package handoff and invokes only existing Phase 9 constructors. It does not
reopen the manifest, read another artifact set, create a projection-local
source record, or transfer source authority to an association or sequence
witness.

For V2, one DTO attribution comes only from a direct same-target role witness.
Association and sequence source references remain evidence in the sidecar, not
attribution shortcuts for a parent or nested endpoint. The adapter fails closed
when endpoint-local evidence, a required owner/subject association, or a
representable source/condition is missing or ambiguous. Navigation is `None`
at this boundary.

## Navigation remains deliberately absent

The B1 artifact has no navigation field. Keeping a relationship reference does
not show that another surface can safely receive it. B2 therefore suppresses
navigation unless a separately implemented exact receiving contract proves the
target, scope, attribution, conditions, and receiver. It may not replace that
contract with a loose query, proxy, dead link, inferred reverse relation,
reauthorization, disclosure, or source fetch.

## B2B implementation proof and excluded work

B2B implements the parser/validator and one-pass
package-to-realization-to-binding path. Its executable fixture constructs
substantive nonempty values through all eight existing constructors and checks
label/order invariance and fail-closed unknown, cross-scope, duplicate,
role/endpoint/dependency, relationship/sequence, lane-promotion,
omitted-condition, and hidden-winner cases. Focused representative and
package/realization accumulator validation passed; P10-21 Step review and
acceptance commit remain pending.

This B1 design creates no new package role, CCDM or Phase 9 public API,
mutable state, CML read/write, live source, approval, rehydration, Web/API
route, Phase 10.3+ behavior, review workflow, publication, deployment, or
checklist closure. P10-21 and Phase 10.2 remain open pending their separately
required review, acceptance, and release gates.
