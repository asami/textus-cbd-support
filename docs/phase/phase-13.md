# Phase 13 - CAR Lint Human-in-the-Loop Resolution

Stage Status:
- Current status: IN_PROGRESS
- Predecessor: Phase 12 Interactive View and Model Editing
- Required inputs: CAR lint structured findings, goldenport-cncf Phase 99 Finding Disposition ABI, Cozy Phase 77 CML properties
- Owner: Textus CBD Support development

## Current execution plan — 2026-10-31

Phase 12 remains CLOSED at completion record `6239082`, with its accepted
implementation, validation, review and Ticket-operations follow-up retained.
Phase 13 uses native GoalPhase Session `82224a6340ff32f987271a2bf69ca53b`,
Plan `cbd-phase13-finding-resolution-20261031`, revision `1`.

| Step | Scope | Current boundary |
| --- | --- | --- |
| LINT13-01 | Import CNCF findings and existing dispositions; group exact occurrences; propose reasoned choices; explicitly admit/reject selected scope | In progress; pure core and executable specification, not yet accepted |
| LINT13-02 | Ordinary generated Operation/ActionCall and authenticated host bindings | Pending LINT13-01 acceptance |
| LINT13-03 | Apply admitted work through CML properties or handwritten Scala annotations | Pending qualified CNCF Phase 99 / Cozy Phase 77 delivery |
| LINT13-04 | Actual re-lint, deterministic disposition projection and integrated review | Pending source application and actual producer/consumer evidence |

The installed local CNCF `0.5.3-SNAPSHOT` artifact contains the public
`FindingDispositionV1` type, so core development can reuse that ABI immediately.
Class presence is not producer delivery, consumer qualification or successful
source application. Existing Phase 12 projection/review/authorization machinery
and Phase 15/15.1 evaluation contracts remain intact.

CNCF must return qualified Phase 99 source/artifact selection, actual checks and
review, and integration examples for exact finding keys, annotations and
reinspection evidence. Cozy must deliver Phase 77 CML properties that map to
that ABI, preserve CML authority through generation, and expose actual structured
lint findings/coverage. Each producer owns its development and local publication.
As of this planning update, neither request has been dispatched: the installed
DelegateTask Start rejected missing execution profiles/origin. Prepared request
text and the actual failed invocation are retained; no transport acknowledgment
or completed producer delivery is claimed.

See the [finding resolution contract](../spec/finding-resolution-contract.md)
and [design](../design/finding-resolution.md). Foundational checks qualify only
their actual scope; ordinary transport, real Human provenance and end-to-end
source/reinspection evidence remain later acceptance obligations. The original
Phase acceptance criteria below remain unchanged.

## Purpose

Turn CAR lint findings into an interactive developer decision and resolution workflow. The objective is not to drive warning count to zero; it is to make unresolved findings visible, propose appropriate dispositions, obtain developer admission, apply the admitted action, and verify the result.

## Resolution lifecycle

`Finding -> AI Analysis -> Resolution Proposal -> Developer Admission -> AI/Deterministic Work -> Re-lint -> Review -> Close`

AI analysis may group repeated findings by rule and semantic remediation pattern so that hundreds of occurrences do not require hundreds of identical approvals. Group admission must remain explicit about its scope.

## Resolution proposals

Initial proposal kinds:

- Fix source/model;
- Accept with durable CML property or Scala annotation;
- Defer;
- Configure the relevant deterministic tool/rule.

The UI presents meaning-level choices. Developers approve a design decision such as "intentional host-filesystem adapter access"; they are not required to author annotation/property syntax. After admission, the implementation layer chooses CML property for CML authority or CNCF Scala annotation for hand-written Scala.

## Safety and authority

- CAR lint remains deterministic and independently runnable.
- cbd-support does not redefine CNCF disposition semantics.
- CML property and Scala annotation are persistence representations, not approval mechanisms.
- AI must not insert annotations/properties merely to silence findings.
- Bulk resolution is an explicit developer-approved scope, not a default.
- Re-lint and review verify the admitted resolution.

## Review model

Expose at least:

- unresolved;
- proposed;
- admitted/in progress;
- fixed;
- explicitly accepted;
- deferred;
- configuration-resolved;
- failed/reopened.

Report both raw finding counts and disposition state. The primary actionable metric is unresolved findings rather than total warnings.

## Acceptance criteria

1. Structured CAR lint findings can be grouped without losing exact occurrence identity.
2. AI can produce one or more reasoned Resolution Proposals without applying them.
3. Developer can admit/reject a proposal and can explicitly choose bulk scope.
4. Accepted CML findings write Cozy Phase 77 properties; accepted hand-written Scala findings write CNCF Phase 99 annotations.
5. Fix proposals change the underlying source/model rather than suppressing the warning.
6. Re-lint/review records whether the admitted action actually resolved the finding.
7. Existing accepted/deferred dispositions are visible and auditable.
8. A full-auto option, if later provided, is an explicit developer policy built on the same admission semantics rather than a separate bypass path.
