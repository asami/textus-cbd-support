# Phase 13 - CAR Lint Human-in-the-Loop Resolution

Stage Status:
- Current status: PLANNED
- Predecessor: Phase 12 Interactive View and Model Editing
- Required inputs: CAR lint structured findings, goldenport-cncf Phase 99 Finding Disposition ABI, Cozy Phase 77 CML properties
- Owner: Textus CBD Support development

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
