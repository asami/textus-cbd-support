# CAR Review Execution Selection and Declared Plan Reference

status=accepted
acceptance_owner=PHASE-14
updated_at=2026-10-16

Current acceptance is the [Phase 14 closure](../phase/phase-14.md#current-phase-closure-and-successor-handoff--2026-10-16),
with its selected source-mode inputs and explicit recovery limits. This current
projection takes effect after the verified local Phase release.

## Historical authoring metadata — 2026-10-06

status=authored_review_pending
phase=Phase 8 / PHASE-69.7
checklist=P8-41 / JM69-10CBD2A
updated_at=2026-10-06

## Supersession and Ownership

P8-41 remains the historical checklist identity. Its earlier v1 calculated
reuse-key authority is superseded by this explicit reference and selection
contract. The nominal `CarReviewReuseKey` source/API name now denotes validation
only. It has no calculated key or content identity.

The server supplies `CarReviewExecutionPlanReference(diagnosisId, revision)`.
`diagnosisId` is the complete native `ReviewDiagnosis` EntityId; its collection
must match the generated collection. `revision` is a positive integer owned by
the server's immutable plan declaration. Creation and admission never issue a
plan reference. Review IDs, timestamps, transport target payloads and vector
order cannot supply a missing reference or select a different plan.

A new configuration is provisioned by the server with its explicit distinct
root reference. Existing root data is never overwritten, migrated or replaced
because a selected dependency changed. An existing root at another declared
revision refuses with `review-plan-revision-conflict`. A changed selection at
one reference/revision refuses with `review-plan-selection-inconsistent`.

## Complete Typed Selection

`CarReviewReuseKey.DEFINITION_ID` and the persistence codec's `schemaVersion`
are exactly `textus.cbd.review-execution-selection.v2`.

| Field | Required typed declaration |
| --- | --- |
| `definitionId` | exact supported selection definition |
| `reviewSchemaVersion` | supported canonical Review report schema |
| `targetReference` | declared target ID and positive integer revision |
| `targetKind`, `organization`, `name`, `version` | semantic target identity; optionals are explicit `null` |
| `profile` | selected Review profile |
| `baselineReference` | declared baseline ID/revision or explicit `null` |
| `ruleSets` | unique rule IDs and versions |
| `providerSelections` | unique provider ID/version, member rule ID/version and `availabilityPolicy` ID/revision |
| `evidenceSnapshots` | evidence class, declared `snapshot` ID/revision and provider ID/version |
| `policyBindings` | exactly one scope each for profile, gate, reconciliation and suppression; each declares policy ID/version and positive revision |

Declared dependency IDs are nonempty bounded identifiers; revisions are positive
integers. Dependency publishers advance revisions for every conclusion-affecting
change. CBD does not inspect content to invent those revisions. Baseline absence
and an empty evidence vector are valid explicit declarations. Missing fields,
duplicate rules/providers/evidence/policy scopes, unbound provider rules,
unsupported schema and malformed typed values refuse with attributed codes.

The plan validates semantic target kind/organization/name/version and profile
against the admitted request. Existing transport target, bundle and report digest
fields retain their payload roles and do not participate in management selection.

## Typed Persistence and Compatibility

`CarReviewExecutionSelectionCodec.encode` and `decode` store and restore ordinary
structured selection data. Root and nested objects have closed field sets. All
fields, null optionals and empty arrays are explicit. Unknown, duplicate, missing
or malformed data is rejected; there is no legacy decoder or default revision.
Missing persisted declarations use `review-plan-selection-incomplete`; malformed
typed values retain their attributed validation code.

`compatible` first validates both inputs, then compares scalar/reference fields
and rule/provider/evidence/policy sets semantically. Array order is ignored only
after duplicate refusal. A valid differing selection uses
`review-plan-selection-inconsistent`. No serialized document equality, byte seal,
content certificate, sorted JSON identity or hash determines compatibility.

## Executable Evidence and Remaining Integration

[CarReviewReuseKeySpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewReuseKeySpec.scala)
covers typed restoration/reordering, malformed references and scopes, independent
dependency changes, and closed/legacy document refusals.
[CarReviewExecutionPlanSpec](../../src/test/scala/org/simplemodeling/textus/cbdsupport/CarReviewExecutionPlanSpec.scala)
covers exact native reference retention, semantic request matching and refusal.
The suites belong to `org.simplemodeling.textus.cbdsupport.runtime`.

[The persistence contract](car-review-persistence-contract.md) owns Entity claim,
owner leases, compatible completed reuse and conditional successor histories.
These authored prerequisites leave actual server provider selection, native Job
binding and independently fresh full CBD acceptance pending under original JM69-10.
