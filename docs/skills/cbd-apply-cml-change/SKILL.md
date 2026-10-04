---
name: cbd-apply-cml-change
description: Apply an exactly approved CBD CML candidate to its explicitly owned existing consuming-project targets using current actual-human and source-owner inputs through a reviewed internal adapter.
---

# Apply an approved CBD CML change

This is repository-versioned instruction source, not an installed skill or
deployed public command. The engine is package-private
`InternalModelCmlChangeApplication.applyApproved(projectRoot, request,
applicationReference)` in Textus CBD Support. A reviewed consuming-project
internal adapter must invoke that actual rooted engine through registered
runtime execution. No consuming adapter, public CLI, MCP endpoint or UI is
deployed by this source. Phase 10.7's accepted Steps 01/02 establish test-only
composition and real external lint/generation evidence, not a consuming adapter.

## Required exact inputs

- Independently admitted current continuation request, original actual human
  approval and complete original source inventory, separately admitted current
  review, and current source-owner live observations with explicit versions.
- Current actual independent human decision binding the complete selected
  subject, exact candidate/realization/diff/review/approval references, accepted
  decisions and mappings, with no blocking issue or unresolved human item.
- Exact normalized absolute consuming-project root, independently admitted
  source-owner mutation authority, complete existing candidate target set and
  opaque owner-supplied next revisions, plus explicit application RecordReference.
- A reviewed consuming-project internal adapter and its admitted execution
  scope for that root and those targets.

Keep the reference domains distinct: `ReviewSubject(subjectId,
subjectRevision, packageReference, scope, artifacts)` contains the complete
reviewed semantic input/dependency set and exact component/context/selected
Use Case scope. Its stable package reference names package/project identity;
carrier revision is separate. Every selected `ArtifactReference` binds
`artifactId`, positive `artifactRevision` and exact role; every logical
`RecordReference` independently binds `recordId` and `recordRevision`.
Source authority/identity/current/next revision belongs to the source owner,
not any artifact, logical-record or subject revision. The
[typed contract](../../spec/internal-model-typed-control-contract.md) governs
complete admission. No hash, whole-file byte comparison or stored approval
claim supplies control or authentication.

If an exact input, owner authority or adapter is missing, stop and name that
dependency. The skill text authenticates no person/source and supplies no
filesystem permissions. Do not invent a CLI, shell overwrite, generated test
source or inferred source version to obtain an application path.

## Application

1. Establish the exact caller-admitted inputs and source-owner scope. Preserve
   original approval and explicit supersession; changed/missing basis requires
   reconciliation and new review through its owner.
2. Invoke the reviewed consuming adapter through registered runtime execution.
   It calls `applyApproved` with the explicit root, request and application
   reference. The engine freshly reevaluates the rooted gate immediately before
   actual native mutation. A stored Eligible report or plan grants no permission.
3. Preserve the complete returned gate, exact subject/reference domains,
   mappings/conditions, original approval, source versions and every native
   target/failure/cleanup outcome. The existing-only native writer performs all
   target preflight before any effect; it creates, renames and deletes nothing.

## Result and stops

- A structured failure means admission could not produce an application report;
  retain its cause and stop.
- `Rejected` retains every current gate cause and has no write report. Obtain
  required owner reconciliation/re-review; no inferred approval or implicit rebase.
- `Failed` discloses real partial effects: earlier Applied targets, a possibly
  changed Failed target and later NotAttempted targets. Retain primary and cleanup
  causes. Stop without workflow retry, rollback, backup or acceptance claims.
- `AppliedPendingValidation` is applied evidence only. Hand off actual required
  Cozy/CBD validation and independently refreshed source/realization/continuity
  evidence matching the exact targets and owner next versions, then shared Phase 9
  CCDM and all eight views. Source-owner Git acceptance remains a subsequent owner
  action. Do not claim validation, canonical acceptance, commit or cursor advance.

The local single-writer execution model has no transaction, crash durability or
racing-writer promise. Owner next revisions remain intent/applied evidence;
the engine does not allocate versions or persist them into an owner store.

## Validation handoff

The reviewed adapter hands the actual complete application to the package-private
`InternalModelCmlChangePostValidation.validate(projectRoot, request)`, with an
explicit independent post-validation RecordReference, independently admitted
actual terminal Cozy observations for every applied target, and independently
refreshed package/source/realization/binding selectors. Observations must bind
that exact application/root/path/source/approved next revision and the exact
runtime 0.3.3-SNAPSHOT lint argv. Preserve full stdout/stderr/exit and findings.
The validator executes no CLI and cannot infer a successful observation.

Missing evidence returns Incomplete; incompatible evidence, nonzero exit,
Cozy FAIL or malformed findings returns Failed. Existing owner structured
failures propagate. Only complete current source freshness and actual shared
CCDM/all-eight construction with mapped lanes/attribution/conditions retained
return ReprojectedPendingAcceptance. Stop on every other result and preserve
the actual changed files and evidence for owner direction. Repository Git
acceptance remains separate.

The [end-to-end reproduction guide](../../developer-guide-internal-model-cml-change.md#end-to-end-reproduction)
and [actual evidence report](../../validation/phase-10.7-end-to-end-evidence.md)
describe test-only proof, not a consuming adapter or deployed command.
As of 2026-10-04, Phase 10.6 is released and Phase 10.7 Steps 01/02 are accepted
under the [canonical ledger](../../phase/phase-10.7.md#accepted-step-evidence--2026-10-04).
Step01 passed six representative/185 accumulator tests; Step02 passed 16 tests,
three completed external sessions, six real lint and four generation results.
Positive outcomes remain pending acceptance; negative Failed retains effects.
Refresh and human/source-owner inputs are independent typed test evidence, not
authenticated interaction. Generated Scala compilation and new live ABI proof
are unverified. Final Phase gates remain pending. This instruction source remains
uninstalled; documentation preparation is review-pending.

See the [contract](../../spec/internal-model-cml-change-contract.md),
[design](../../design/internal-model-cml-change.md), and
[developer guide](../../developer-guide-internal-model-cml-change.md).
