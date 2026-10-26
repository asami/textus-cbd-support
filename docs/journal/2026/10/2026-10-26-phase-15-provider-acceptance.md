# Phase 15 scoped provider reconciliation and acceptance

Date: 2026-10-26
Status: primary focused validation passed; producer fixture correction and acceptance pending

## Actual focused validation and dependency decision

Native ordered validation revision `20` selected `scoped-quality-focused`
(`test-only`) against candidate tree `0d18ac1319004efe0df22bf75a36364d43f12743`
at the original checkpoint. SBT exited with code 1 during `Compile`; the three
selected review suites did not execute. Generated `ReviewDiagnosis.scala:540`
requires Circe Encoder/Decoder instances for nested `ReviewTargetSnapshot`.
Automatic derivation reaches `org.goldenport.datatype.Slug` and fails to find
instances or a derivable Mirror. Stdout lines 71–177 contain the two compiler
diagnostics; lines 178–180 retain the compilation-failure outcome.

Source inspection identifies SimpleModeler's declared-store generator as the
producer of the failing binding, outside the CBD review-provider Component.
No generated file, dependency source or provider production code was patched.
The ordinary RepairStep result reports `issue-1` as BLOCKED, without claiming a
fix. The runtime accepted dependency proposal
`cbd-phase15-nested-entity-codec-restoration@1` and issued the genuine ordered
Human question `b2fdd77d89d1e298d42cd65a97e34215:ordered-human:25`.
Its choices are `ApproveDependencyScope`, `RejectDependencyScope`, `Defer` and
`Cancel`. The question was initially unanswered. The original Session/base,
accepted implementation, failed validation and repair counts (1/10) remain
preserved.

The proposed producer outcome is usable nested-Entity codecs with the existing
declared-store semantics, a meaningful generated-code compilation/roundtrip
regression, producer validation and independent review, followed by normal local
dependency consumption and the same native CBD check. SQR-04 acceptance, full
Phase validation, independent review and Phase closure remain pending.

The owner subsequently answered `許可する` in the owning chat. The ordinary
native preparation/ordered-decision route accepted `ApproveDependencyScope`
with reference `cbd-phase15-simplemodeler-user-authorization-20261026@1` and
returned to the same RepairStep without a repair-count increase. The first
preparation failed because its input files were outside the configured read
roots; the next failed because the caller supplied incorrect evidence property
names. Both are retained preparation failures, with no Human Submit. The third
preparation and actual Human Submit succeeded. No previous accepted Submit was
replayed and no Session storage was edited.

The dedicated producer branch is `codex/cbd-phase15-entity-codec`, based on
SimpleModeler commit `ee51acf593c361568b9910700fa1781a39a2ab8a`. Its admitted
binding selects the declared-store generation specification and structured-data
generation regression through native TestOnly. The normal local-publication
prerequisite and CBD's existing Cozy development-generator selection are
configured for consumption. Configuration and source edits alone establish no
successful producer publication, generated-code qualification or CBD acceptance.

The producer correction supplies private generated Slug, Charset and URL codecs
while preserving nested product derivation and the existing Record bridge. G1
in CBD's existing Operation specification exercises the actual generated
`ReviewDiagnosis.targets` binding with populated framework attributes and a
nested media object. It varies valid Slug strings and requires retained payload
plus malformed scalar rejection. Producer and consumer corrections are submitted
for fresh native verification; their presence establishes no test result.
The integrated independent review must cover both repositories.

Native validation revision `53` followed successful native local publication,
but the consumer reused 219 generated sources because its existing timestamp
cache did not observe the changed producer SNAPSHOT. That failure repeats the
old Slug diagnostic and does not qualify newly generated code. The ignored
`target/sbt-cozy/generation-state.properties` marker was backed up in the task
artifacts and removed; no generated Scala was edited.

Revision `82` then generated 219 sources normally and confirmed the scalar
codecs were present. Main compilation next failed to derive the nested
`Vector[ContentReferenceOccurrence]` Encoder and Decoder. The admitted producer
repair added explicit full-product element codecs for content references and
the four media element types. CBD G1 now populates image, audio, video and
attachment content, preserving the existing model's `atathments` field spelling.

Revision `111` again generated 219 sources normally, but main compilation of
397 sources exhausted the native JVM's 4 GiB heap during compiler RefChecks.
Stdout lines 51–85 and stderr lines 1–34 retain `Java heap space`; no selected
test bodies ran. Repair progress is 4/10, with all earlier evidence retained.
The same producer owner is correcting the expansion of recursive full-product
derivation before another ordinary native validation. None of these failures
establishes focused success, producer qualification or SQR-04 acceptance.

Source inspection found 72 mappings emitting the newly added recursive media
codecs although only `ReviewDiagnosis` captured a declared nested Entity. The
follow-up limits those codecs to declarations containing generated Entity types
and adds explicit `SimpleObjectContent` and `MediaAttributes` intermediate
instances within that group. Ordinary mappings retain the scalar, Record and
content-reference codecs. Producer E9 checks both emission and non-emission.
This reduces the predicted recursive groups from 72 to one; that prediction
still requires fresh generated-source and behavioral qualification.

Native revision `140` passed the primary `scoped-quality-focused` check:
three suites, 18 tests, no failures. This includes fresh generated main
compilation at the unchanged 4 GiB heap and G1's complete nested framework
roundtrip, all four media products and malformed Slug/Charset/URL rejection.
Stdout lines 317–320 retain the focused summary. This is consumer qualification
of the corrected producer, with the test-double limitations below unchanged.

The required producer check then ran two suites and 26 tests: 25 passed and E9
failed. Stdout line 3868 reports nine selected products instead of the three
expected persistence mappings. The suffix-based fixture selection also picked
same-named aggregate, operation, read and view products. The producer correction
uses the exact persistent/create/update paths for parent and child assertions,
retaining required/optional/vector binding and codec coverage. No generator
correction or generated-cache invalidation is needed for this fixture-only fix.
Producer qualification, independent review, SQR-04 and Phase closure remain open.

## Scope and retained work

The owner selected Phase 15 through `sm-goal-phase`. Checkpoint
`8955dbcf97878bba943d813b8acc0283e340e229` remains the original base, including
the existing Phase 15.1 preparation/acceptance code and specifications. The
earlier stopped validation attempt remains a preparation failure, not a test
result. No past failure, finding or applicable result is replaced by this run.

The normal installed `sm-workflow` command started Session
`b2fdd77d89d1e298d42cd65a97e34215` and issued `ImplementStep` for
`scoped-quality-provider-handoff`. Its selected Step checks are the existing
provider, Operation and external-Operation suites; its Phase check is CBD's
full test suite. Actual check and review results will be recorded after their
own native execution. No successful validation or acceptance is claimed here.

## SQR-01 through SQR-03 reconciliation

| Obligation | Retained implementation | Actual gap addressed |
| --- | --- | --- |
| SQR-01 | CML request/result and private `CbdReviewAdmin.reviewScopedQuality` QUERY, factory ActionCall and provider UoW | R4 now explicitly documents review identity/revision binding already enforced by the provider. S4 exercises crossed review references. |
| SQR-02 | Bounded CML evaluation and four selected semantic checks through the optional AiRunner socket | No provider rebuild or new internal AI route. Existing S1–S4 and O1–O3 remain the verification surface. |
| SQR-03 | Typed findings, covered/unsupported/failed checks, deterministic finding merge and current-target validation | O4 exercises affected subsets through the normal Operation, current revisions and preservation of applicable supplied finding identity/scope. No reuse engine or management hashes. |

`completed` means every selected check was evaluated; a blocker can still be
present. Consumer CAM remains responsible for Admission, prior-evidence reuse,
independence and commit permission. Unsupported and failed checks cannot become
covered merely because an implementation or result object exists.

## SQR-04 contract for the consumer

Operation: `CbdSupport/CbdReviewAdmin/reviewScopedQuality` (private QUERY).
Input: `ScopedQualityReviewRequest`; output: `ScopedQualityReviewResult`.
The normal invocation supplies named request properties to that Operation;
O1 and O4 in `ScopedQualityReviewOperationSpec` show the generated request,
ActionCall execution and typed result decoding.

Example request properties:

```json
{
  "reviewId": "review-example",
  "revision": 1,
  "subjectId": "candidate-example",
  "subjectRevision": "candidate-revision-1",
  "paths": ["src/Example.scala"],
  "modelSubjects": [],
  "checks": ["semantic-consistency"],
  "requirements": ["The supplied implementation must preserve the declared input contract."],
  "materials": [{"identity":"source-1","kind":"source","path":"src/Example.scala","content":"class Example"}]
}
```

The result repeats all four review/subject references and includes `status`,
`checks`, `findings`, `providerId`, optional `purpose` and `model`,
`ruleRevision` and optional `execution`. Semantic coverage must account for
the exact requested scope. CML coverage applies only to explicitly selected
metadata model subjects, with no inferred source-path coverage. Missing input
metadata, unavailable execution and partial semantic scope remain explicit gaps.

The retained consumer decoder is `GoalPhaseScopedQualityAdmission` in
sm-workflow. Its current standalone result projection reads the ordinary
camelCase fields and separates findings from CAM coverage/Admission. The
consumer's external-execution attribution adaptation remains Phase 10 work;
this owner delivery does not claim that connection or its acceptance.

Recording AiRunner specifications establish normal Operation/provider wiring
and response interpretation with a test double. They do not establish a live
provider invocation, model quality or a genuine external executor. Phase 15.1
still owns real external preparation/execution/acceptance evidence and RR06.
Its existing implementation and unchecked acceptance obligations remain intact.

## Validation, independent review and remaining acceptance

Pending: native focused check, applicable full regression check, independent
Step/Phase review, bounded SQR-04 acceptance and native closure. The owning
checklist remains unchecked until the corresponding evidence is available.
The earlier 16-test CBD and 107-test consumer results retain only their original
scope in the existing handoff; neither is reclassified as the new external route.
