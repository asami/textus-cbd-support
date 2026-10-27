# Model Edit Source Production Usage Contract

status=producer delivered / CBD integration pending
identity=cbd-phase12-model-edit-source-production
revision=2
consumer_step=EDIT12-03
updated_at=2026-10-27

This is CBD Support's source-independent Usage Contract for the proposed Cozy
delivery. It specifies required meaning and acceptance; it declares no already
implemented Cozy grammar, namespace, wire schema or producer permission. The
[accepted session contract](model-edit-session-contract.md) owns candidate editing;
[projection follow-up](../phase/phase-9-projection-followup.md) owns FU-01/FU-03/FU-07.

## Required source-owned delivery

The selected canonical CML source must explicitly represent stable shared Actor,
independent Goal and intent, Use Case and implementation Slice identities, Slice
fields/lifecycle, and attributed versioned Actor-Goal, Goal-UseCase, UseCase-Slice,
Slice-Specification and Slice-Ticket relationships. Cardinalities remain independent;
labels, co-occurrence and names cannot infer relationships. Specification endpoints
retain owner, stable identity and required source revision. Ticket identity retains
provider, owning repository/project and immutable external identity; observations
remain separate and linking performs no external issue-tracker operation.

A supported Cozy production entry must parse the selected actual source, admit a
versioned typed publication and expose a supported read boundary. Existing admitted
Use Case and behavioral/causal semantics needed for FU-01/FU-03 must be available
from that same source production, including Actor, Goal/Use Case, Command/Operation,
Aggregate/Entity, Domain Event, Policy/Reaction, external Component/dependency and
Query/View evidence where declared. Present-empty, missing, unavailable, unsupported
and external evidence must remain distinguishable. Contextual references never
become Cause/Reaction facts. Unknown namespaced JSON proves no capability.

Source-owned candidate rendering must return proposed CML for explicit existing
owned targets without writing canonical files. Actual parse/edit/render/parse must
retain stable identities, explicit relationships, Slice fields and attribution;
unsupported source forms return explicit limitations instead of reconstructing
missing meaning. Existing accepted v1 contracts retain their declared meaning;
new source grammar/publication behavior needs its own explicit producer contract.

### Creation and relationship additions

Revision 2 clarifies the required creation path for the next normal producer
planning boundary. The existing owned target is the selected source document;
new Goal/Slice records and new explicit relationships must also be renderable
within that ownership. The accepted CBD commands `AddGoal`, `CreateUseCaseSlice`,
`LinkGoalUseCase`, `LinkSliceSpecification` and `LinkSliceTicket` supply proposed
identities and references; a renderer limited to existing-record replacements
and relationship removal cannot implement them.

The producer must admit caller-supplied new record references, types, values and
source attribution, plus explicit Actor-Goal, Goal-UseCase, UseCase-Slice,
Slice-Specification and Slice-Ticket additions. Existing ownership and graph
admission govern these additions. No producer identity allocation, inferred
edge, new canonical file or external ticket effect is required. Actual source
production, candidate rendering and re-production must retain the new records
and relationships while leaving the original canonical source unchanged.

Earlier producer work and checks remain evidence for their original scope.
The initial replacement-only candidate surface must retain its declared meaning;
any necessary new wire/API version is explicit. This clarification does not
retroactively accept creation support, change an active execution's contract,
or answer the retained revision-1 dependency-scope question.

## Required qualification and consumer boundary

Qualification must execute the real selected Cozy producer against actual CML,
retain input source identity/revision and actual executing producer version/revision,
and read/re-project the produced candidate through that same supported route.
Constructor-only metadata fixtures do not establish this integration. Existing
local runtime classpath export can supply qualified output. Cozy owns any required
local publication and coordination of its shared artifacts. CBD consumes the exact
returned version and artifact location; it must not republish Cozy or overwrite
shared local artifacts. The producer delivery must identify its committed revision,
usable version/location, actual validation and independent review results, remaining
limitations, and concrete consumption commands. Remote publication, push and runtime
installation are outside this requested boundary.

CBD remains responsible for mapping admitted typed output to its candidate Views,
Actor Goal List, cross-model Event Storming, independent development trace and
policy-based deterministic findings. Incomplete source scopes cannot prove missing
links or coverage; Phase 11 capability-demand evidence remains read-only. The
existing Phase 10 exact subject, actual-human approval, freshness and canonical
application gate remain mandatory. Cozy rendering supplies no approval or write
permission. Local single-writer declarations require no hashes, CAS, replay or
new persistence, Job, AI-provider or query framework.

EDIT12-03 accepts only actual source-owner delivery plus qualified CBD consumption.
EDIT12-07 and EDIT12-08 retain their separate actual promotion and continuation
proofs. This target contract, dependency proposal and file inventory confer no
checkout, edit, execution, publication or acceptance authority.

## Delivered producer

Cozy independently delivered revision 2 of this Usage Contract at commit
`da61cb223a1df16a9b6cbacd4498ba8134cf3a22`, version `0.3.3-SNAPSHOT`.
Its [producer contract](../../../cozy/docs/spec/cml-model-edit-production.md)
defines source/publication v1 and distinct candidate v1/v2 readers and renderers.
The public JVM main is `cozy.modeler.CmlModelEditCli`; the normal exported
classpath is `cozy/target/cozy.d/runtime-classpath.txt` in that producer checkout.
Candidate v2 includes explicit new record and relationship additions.

The producer reports actual compilation, focused validation, separate-JVM
source/render/re-production/read qualification and independent accepted review.
Its final shipment identifies the executing committed revision with
`modified=false`. No model-edit publication was required. These facts establish
supplier delivery only; CBD integration, View mappings, findings, canonical
promotion and continuation still require their own native acceptance.

## CBD consuming implementation boundary

Stage 12.3 now has an authored typed reader, source-to-CBD binding, source-owned
canonical facts adapter, candidate-v2 planner and confirmed mapping capsule.
The existing Phase 10 candidate/diff validators and complete review subject consume
those values. Each source mapping retains exact source/edit/candidate artifact
references and the complete typed confirmation. An unsupported or missing mapping
cannot promote. The source reference's declared identity is its mapping anchor;
assertion, source-reference, CBD semantic, record and patch identities remain explicit
caller inputs. No identity is allocated from a name or content.

The optional `textus.cbd.modelEditProducerRoot` selects the independently delivered
export for the effectful producer-focused check. It does not change
`textus.cbd.cozyGeneratorRoot`, the CAR ABI producer, the runtime assembly selection
or any native workflow program. `project.yaml` records the qualified producer
version/revision. The focused Spec invokes actual produce/read/render-candidate-v2/
re-production against owned CML, applies five creation commands through the normal
candidate service, and connects the result to existing Phase 10 admission.

The SBT selection key is `selectedModelEditProducerRoot`, distinct from the
Capability producer's existing `selectedProducerRoot`. Selecting model-edit
consumption alone must leave Capability production and CNCF source mode unselected.
The build key label is part of this separation because SBT keys share a project
scope even when their declarations belong to different Scala helper objects.

This is authored validation, not an execution claim. Native validation and
independent review remain pending. Existing Phase 11 Use Case demand queries still
return their typed unavailable projection; the read-only adapter retains this
boundary rather than manufacturing an empty demand path. Broader CNCF Observed/
lifecycle and Phase 87 mappings are outside this consuming Step.
