---
status: authored-unvalidated
decision_scope: P106-STEP-01
updated_at: 2026-10-03
---

# Internal-model Retained-state Security Design

The [retained-state contract](../spec/internal-model-retained-state-security-contract.md)
owns Step 01 fields, policy matrix, finite limits, trust boundary and failure
semantics. This design records the frozen responsibility decision. Source and
executable specification authoring do not establish validation or acceptance.

## Three storage responsibilities

Project packages retain complete selected state and independent resume evidence.
Optional Entity-backed audit history retains richer review, proposal, alternative,
supersession and evidence accounts. Disposable target caches retain derived
outputs. This separation permits selected-state resume when history is absent,
unavailable, different or stale. History cannot become a reverse dependency of
package admission, continuation, action gates or CML application.

Historical artifact references can remain after current inventory removal.
Reusing the existing logical/package/artifact references and ReviewSubject records
their original identities and independent positive revisions without duplicating
CCDM Entity/Event/Workflow meaning. Structural validation deliberately cannot
establish current semantic admission or complete-subject evidence. Actor fields
attribute an account; they do not authenticate it.

## Closed input/output evidence boundary

Separate Scala 3 input and retained-output enums make raw evidence unrepresentable
as a retained payload. Validation precedes policy exclusion so invalid or oversized
data cannot bypass admission by being destined for omission. The independent
retained-value validator also guards downstream constructed values. The policy
preserves exact reference/kind and uses explicit omission reasons rather than
inventing metadata or reconstructing lost evidence.

Raw prompt/response bodies remain excluded from both project-source and history
defaults, even with a purported safe or redacted label. Only bounded safe
provider/model/tool tokens survive as identity metadata. CallTree and external
evidence remain opaque logical references without body access or retrieval grants.
Unavailable is distinct from policy exclusion.

Narrative RedactedText retains exact Unicode and the upstream owner's independent
redaction reference. This is an explicit claim rather than a secret-removal proof;
the Step 02 authorized caller owns admitting that claim. No automatic scrubber,
raw opt-in, content-derived control token, digest or byte-equality protocol is
part of the design. Redaction may destroy evidence, so omission and unavailability
remain first-class values.

## Pure structural admission

Immutable history payloads derive their kind, preventing contradictory stored
tags. Review acceptance and selected alternative dispositions remain review and
proposal facts with no human-approval/cursor transition. Evidence encounter order
survives as documentation, never as selection authority.

Null graphs and every bounded supplied dimension are checked safely before
dependent dereference. Existing opaque-type constructors retain their declared
grammar; logical IDs preserve Unicode and whitespace. Structured invalid-operation
diagnostics identify only dimensions, preventing raw or retained text disclosure.
Independent failures compose applicatively; dependent relations are checked only
after their records have been admitted.

Supersession accepts an explicitly supplied previous/successor pair with matching
non-Supersession kind, package and scope. Different subject revisions preserve
historical change. No endpoint lookup, newest-record inference or global graph is
needed. Tombstones preserve only typed record/package/scope attribution, kind,
retention action and time, avoiding residual body/provider/actor prose after expiry
or deletion. Step 01 defines these values without implementing retention mutation.

## Deferred integration owned by subsequent Steps

Step 02 Entity persistence and authenticated internal API are **NOT YET IMPLEMENTED**.
P10-60 needs durable optional history, independent UnitOfWork reads, exact revisions,
expiry/deletion and package-only resume proof. P10-61 needs bounded authorized
read/propose/review/resume/record operations with no implicit approval or arbitrary
repository mutation. P10-62 needs admission of pre-redacted narrative by the
authorized caller and enforcement through persistence/API. No operational DB,
transport/MCP endpoint or auth integration is claimed.

Step 03 exclusion proof is **NOT YET IMPLEMENTED**. P10-63 needs distinguishable
sentinels and positive controls exercised through actual generation/publication/
packaging owners for runtime JARs, public APIs, ordinary CML, documentation and
CAR/SAR artifacts. Step 01 defines the pure policy without installing unused
packaging filters or changing upstream owners.

The [evidence executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelEvidencePolicySpec.scala)
and [history executable specification](../../src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelRetainedHistoryValidatorSpec.scala)
cover the authored pure boundary. Parent validation and independent review remain
pending; all P10-60–63 integration evidence, checklist closure and
[Phase 10.6 acceptance](../phase/phase-10.6-checklist.md) remain outstanding.
