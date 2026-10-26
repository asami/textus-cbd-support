# Phase 15 scoped provider reconciliation and acceptance

Date: 2026-10-26
Status: bounded SQR-04 Step accepted; first whole-Phase validation failed, release repair open

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

## Accepted Step and first whole-Phase check

Native validation `171` passed the three selected CBD suites (18 tests) and
the two selected producer suites (26 tests). It retained producer source
`ee51acf593c361568b9910700fa1781a39a2ab8a`, candidate tree
`b4d5e93128fd214cced42485224f5d6e1c635048`. Independent reviewer
`/root/cbd15_independent_review_20261026` returned accept with no blockers in
`cbd-phase15-independent-step-review-20261026@1`. The native closure committed
the accepted Step as `15665338ed69abae2f9456c4e595c5d6d330efd4`. This accepts
bounded SQR-01..04 delivery; it does not qualify actual external execution.

The following native full check, validation `184`, ran 136 suites and 1088 tests:
1076 passed and 12 failed. Its actual candidate was Step commit `15665338`,
tree `1a0657897f42a3cd19b6e978cbb7d938cb2bb811`. Native release RepairStep
retains all accepted Steps and opened `issue-3`, at automatic count 1/10.
No independent Phase review has occurred.

## Release diagnosis and bounded corrections

The pre-edit manifest is retained in the original ignored native task as
`release-diagnostic-repair-01-manifest.json`, bound to validation `184`.

- Nine ReviewDiagnosisPersistenceSpec failures result from four domain fields
  named `created_at` colliding with the managed SimpleEntity creation column.
  CNCF correctly initializes that managed column. The four domain fields and
  actual handwritten callers now use `review_created_at`; public review
  documents, `completed_at` and the existing ReviewInstant application codec
  retain their contract. G1 still preserves the distinct lifecycle/domain times
  and full nested framework values. The existing SQLite and memory scenarios
  remain the required behavioral validation.
- ComponentFactorySpec requires absent guidance when exact selection fails.
  Both unselected projections now omit the empty `guidance` field; selected
  component guidance and other fields retain their semantics.
- Phase5ExecutableCoverageSpec already supports explicit sibling source roots.
  The normal native full-check environment now selects the existing dedicated
  Cozy Phase64 checkout, sbt-cozy and textus-ai-core using those parameters.
  Its manifest, required anchors and check remain unchanged.
- The first CbdHttpSecuritySpec case uses a bare execution context and fake
  driver, without current host HTTP admission or Component scope. Inspection
  of the consumed CNCF jar confirms admission precedes driver execution. The
  retained failure only reports `None` versus `Some("ok")`; no returned cause
  is claimed observed. The passing cross-origin case rejects before HTTP.
  Required correction is an isolated public host bootstrap with an explicit
  finite HTTP plan and actual fixture Component scope, preserving every
  credential/header/CallTree assertion and cleanup. Only
  `src/test/scala/org/simplemodeling/textus/cbdsupport/runtime/CbdHttpSecuritySpec.scala`
  needs this fixture migration; production HTTP/authentication code does not.

The HTTP specification is outside the original Plan-owned path union and has
not been edited. The current phase-level repair requires unchanged ownership.
Ordinary Plan-revision adoption cannot extend a closed Step or that ownership.
A runtime-supported scope admission is required before this remaining file can
be changed and the owning full check completed. Source inspection and these
corrections are not fresh test success: `issue-3` remains open, with no new SBT
execution, Phase acceptance or final closure claimed. The accepted producer
source and original validation/review/history remain unchanged.

## Actual native disposition of the scope gap

The ordinary result `cbd-phase15-release-scope-gap-20261026@1` was accepted
as `decision-needed`; its following Advance issued actual Human continuation
`b2fdd77d89d1e298d42cd65a97e34215:ordered-human:190`. The saved choices are
`ResumeAssignedWork`, `Defer` and `Cancel`; none changes Plan ownership. The
repair window remains 1/10. No new Human answer was inferred.

A separate passive call to public `describeGoalPhaseFailedPhaseRepair` supplied
a complete concrete corrective Plan proposal preserving the accepted Step and
adding the required HTTP fixture. The installed runtime returned status 409:
"Phase repair adoption requires the exact original terminal Failed or confirmed
timeout Phase Decision." The response and exact stderr remain retained in
`failed-phase-corrective-description-01-response.json` and
`failed-phase-corrective-description-01-stderr.log` under the same ignored native
task. This is an observed eligibility refusal at the current frontier, not a
permission denial or a failed SBT attempt. No scope adoption occurred.

The runtime producer needs a supported way to admit the concrete additional
primary specification at this existing repair/Human frontier while preserving
the accepted Step, original base, actual failed check, independent review and
repair count. Ordinary resume alone cannot supply it. The exact proposal is
retained in `http-security-fixture-scope-gap-01.json`; no runtime store, accepted
Plan, captured source or required check was rewritten. All eight owned release
files remain uncommitted and unvalidated, ready for that continuation.

## Authorized producer continuation

The user explicitly authorized the sm-workflow correction and validation while
preserving the existing CBD Session, accepted Step, histories and repair counts.
The actual Human resume was admitted through the ordinary caller; its successor
RepairStep still could not own the additional HTTP specification. A truthful
second decision-needed result retained the gap and issued unanswered Human
`b2fdd77d89d1e298d42cd65a97e34215:ordered-human:194`. The repair window remains
1/10. That question is retained for the supported RemainingPlan adoption route;
no private Session state or accepted Plan has been rewritten.

The separately commissioned producer owner works in the isolated
`.codex-workflow/checkouts/sm-workflow-phase15-release-scope` checkout. Its
pre-existing tracked and untracked source state was preserved as the baseline.
The bounded correction extends the existing RemainingPlan description/adoption
route for this unanswered initial release-repair Human. CBD's additional HTTP
fixture remains an ignored draft until native scope adoption succeeds.

Producer validation `cbd15-smworkflow-release-scope-validation-01` actually
terminated with SBT/wrapper exit 1 before any of its nine selected suites ran.
Six compile errors in existing publication integration show that the declared
CNCF JAR lacks the `PublishLocal` APIs used by that preserved source baseline.
This is retained producer qualification failure, not a new CBD check result or
repair charge. The producer owner is reconciling the normal source dependency
selection before fresh focused validation. The task retains the typed result,
actual log, terminal marker and consumed permission identity.

The normal source-build selection now uses a separate clean CNCF checkout at
committed revision `8d5885412dc00dd6945b4e43b5e76f8e674cc076`, which contains
the required publication APIs. No CNCF source or local artifact was replaced.
Producer validation 02 compiled production and executable specifications but
stopped in the existing Human-preparation fixture because its explicit normal
SBT executable was not selected. Validation 03 supplied that existing property:
six suites ran 176 cases, with 175 passing and one new native-fixture failure
caused by an incomplete unresolved-issue report. Three selected suites had not
run. Both actual failures and their terminal evidence remain retained.

Independent review identified two concrete continuation blockers: historical
initial-Phase repair provenance incorrectly remaining active inside the appended
Step, and ordinary Step closure resetting the originating Release window's
consumed allowance. The producer owner is correcting these interactions and
extending the persisted fixture through Step check failure, review repair,
accepted corrective commit and a subsequent failing full-Phase check. These
results do not yet qualify a new runtime or close CBD's outstanding issue.

The producer's two continuation findings were independently closed. Its final
focused execution passed four suites and 15 cases, with actual SBT/wrapper exits
0 and terminal native-lock evidence. Applicable results from the preceding
executions remain reused: 230 successful case observations across ten suites,
rather than a claimed single successful whole run. Earlier failed executions
retain their original outcomes. The last fixture correction used the accepted
Step commit as the corrective candidate's parent while preserving originalBase.

Normal LocalPublicationService execution then published the isolated producer
and activated installation `856ca11b-ce7a-4192-a221-d55551c9aa54`, update
`da09284c-dee1-4ec4-a306-ab178965eae9`. The typed SBT process actually exited 0
after 77,779 ms; the composite receipt records Completed and Activated. Normal
host registration adds the exact HTTP path, the existing full check to the Step
policy, and corrective closure ordinal 3. Accepted Step1's original programs and
17-path selections remain exact; the reserved future Phase release stays at
ordinal 2 with the final 18-path union.

The installed public RemainingPlan description succeeded for actual Human194
with the original accepted commit, original base and 1/10 allowance intact.
Its complete target is about 1.8 MB. Existing passive evidence preparation also
succeeded with that exact target and the attributable user direction, returning
one normal StartupImport row. No import, Plan adoption or Human answer occurred.
The remaining producer integration gap is the inline-only RemainingPlan CLI
input: it needs the existing shared Resource-input transport already used by
other workflow Operations. The same producer owner is connecting that transport
without changing the business DTO, admission, authority or persistence contract.

## Qualified Resource transport and admitted corrective Step

Producer validation 06 passed all 153 cases in five suites, including the
Resource reader, generated ABI/help and persisted RemainingPlan continuation.
The registered runner and parent qualification retained actual terminal
SBT/wrapper exits 0. Independent review accepted the transport increment without
new findings. Normal LocalPublicationService publication 02 then completed and
activated installation `d658bc54-984a-49f1-a71d-fdd9f1fd7927`, update
`282e48d5-6914-4faf-91bb-ac80054ab532`; its typed process exited 0.

The installed `adoptGoalPhaseRemainingPlan` accepted the complete Resource input
and the previously prepared actual user direction as `remaining-plan-adopted`.
Human194 remains superseded without an answer. The original base, accepted
provider Step/commit/review, check histories and consumed 1/10 allowance remain.
The following normal Advance issued `ImplementStep`
`phase15-whole-phase-regressions`, continuation `ordered-implementation:196`,
under Plan revision 2. Its exact nine owned paths include the HTTP specification
and its selected check is the existing `cbd-full`.

The previously ignored HTTP fixture draft is now applied within that issued
scope. It initializes the normal embedded runtime with explicit component/origin
HTTP grants, retains the recording driver and all authentication, sanitization
and cross-origin assertions, and closes the owned runtime/restores prior globals.
The retained timestamp and absent-guidance corrections remain unchanged. These
are semantic corrections of issue-3 pending fresh runtime-owned full validation
and independent review; no passing CBD result or Phase closure is claimed here.

The first corrective full check, native validation `224`, failed Test compilation
with E173: `Subsystem.shutdownOwned` is package-private to CNCF. No tests ran.
The runtime issued the same Step's RepairStep and charged its next ordinary
batch, preserving the existing Release allowance at 2/10. The correction uses
the public Subsystem and SystemNode `shutdownC` operations in nested cleanup,
with the outer embedding/global restoration and every test assertion retained.
The original failed command, issue-4 and pre-edit diagnostic manifest remain
retained; fresh native validation remains required.

Validation `255` exposed a second cleanup access error: `Subsystem.systemNode`
is also package-private. Test compilation failed again without running tests;
the same issue-4 remains open and the runtime allowance is 3/10. The consumed
CNCF JAR was inspected and confirms the public `initializeHandle(BootstrapConfig)`
and `CncfHandle.close` API. The fixture now uses that normal owned embedding
handle, which performs both logical and physical cleanup internally. It retains
the same configuration, component, driver, assertions and prior-global restoration.
Both failed attempts and the revised pre-edit diagnosis are preserved.

Validation `286` compiled and ran all 1088 cases in 136 suites: 1084 passed and
four existing ReviewDiagnosis persistence cases failed. HTTP security, timestamp,
projection and generation coverage checks passed. All four remaining failures
reached terminal successor admission with `review-diagnosis-not-reusable`.
The generated declared mapping stores ReviewRunState and ReviewId using their
application JSON codecs, but the handwritten transition still supplied raw
wrapper `.value` strings as physical expectations. An uncontended transition
therefore mismatched as well as the existing concurrent case.

The bounded correction derives both expected physical fields through the root's
existing `EntityPersistent.toStoreRecordC` and mapped field names. It preserves
the identity/revision/active-run guards, root update, successor intent and atomic
transition. No generated source, producer, datastore mechanism or persistence
test assertion changes. Native runtime repair progress is 4/10; fresh `cbd-full`
and independent review must qualify this correction.

## Accepted corrective Step and Phase completion — 2026-10-27

Native validation `317` passed all 1088 tests in 136 suites and terminated with
SBT exit 0. Independent corrective Step review
`cbd-phase15-corrective-step-independent-review-20261027@1` accepted the nine-path
correction with no blockers. Ordinary native closure committed
`69b71aa78fc3af52717e2bd2fe9c4a4c35544bb4`, retaining the original accepted provider
Step, original base, actual failures and consumed 4/10 repair history.

Fresh whole-Phase validation `328` also passed all 1088 tests in 136 suites with
terminal SBT exit 0. All four reported TEST issues are resolved. The independent
integrated Phase review `cbd-phase15-independent-phase-review-20261027@1` accepted
the combined candidate with no blockers. The ordinary Submit accepted that
review, and its paired Advance returned `Terminal`, outcome `completed`, for
Session `b2fdd77d89d1e298d42cd65a97e34215`. The actual retained local responses are
`call-complete-phase-review-01/response.json` and
`call-complete-phase-review-01/advance/response.json` under this Session's existing
`target/sm-workflow/runtime/target/sm-workflow/tasks/cbd-phase15-20261026-01/`
task directory; actual check and independent review artifacts remain there.

The native completion reports repository synchronization `deferred` because no
current independently granted Phase closure authority was available, and states
that synchronization was not attempted. Phase completion and repository
convergence are separate results. No remote push or successful synchronization
is claimed. SQR-04 remains accepted within its documented provider/Operation
coverage; live external execution and RR06 remain Phase 15.1 obligations.

### Nonblocking Hygiene

`HYG-CBD-P15-COMPONENTFACTORY-SIZE`: the independent Step and Phase reviews
identified the existing 2333-line `ComponentFactory.scala` as high-priority
maintenance debt. It combines wiring, persistence and projections. Its separation
belongs in coordinated future maintenance; it is not a Phase 15 blocker and does
not expand this correction or require another acceptance cycle. No Development
Candidate was introduced by these reviews.
