# Staged quality review matrix for CAR lint and semantic Review

Date: 2026-10-13
Status: quality-review direction
Related: CAR lint, Review, excessive defensive checking, sm-workflow, Cozy, CNCF Admission

## Decision

textus-cbd-support quality evaluation is lifecycle-context-sensitive. CAR lint and semantic Review may be requested for Step Close, Phase Review, or Phase Final, but each request has a different purpose and scope.

## Shared review matrix

| Concern | Step Close | Phase Review | Phase Final |
|---|---|---|---|
| CAR lint: CNCF/Textus rules | changed scope, primary | whole Phase, primary | unresolved/blocking confirmation |
| CAR lint: model/ABI consistency | changed scope, primary | integrated state, primary | freshness/blocker confirmation |
| CAR lint: prohibited/risky patterns | primary | primary | unresolved confirmation |
| Review: Step requirements | primary | supporting evidence | normally no new search |
| Review: Usage Contract | affected Slice | whole Phase, primary | blocking completion |
| Review: implementation quality | changed scope, primary | whole-system perspective | repair delta when needed |
| Review: excessive checking/assurance | changed scope, primary | cross-Step/system-wide, primary | repair delta when needed |
| Review: unnecessary complexity/abstraction | changed scope, primary | cross-Step/system-wide, primary | repair delta when needed |
| Review: duplicate implementation/state | relevant scope | primary | only unresolved/delta |
| Review: component integration | relevant connected scope | primary | blocking completion |
| Review: architecture fitness | relevant scope | primary | only affected delta |
| Review: cross-Step assumption conflicts | normally out of scope | primary | unresolved confirmation |
| Acceptance Boundary compliance | primary | primary | primary |
| Finding resolution | after repair | primary | primary |
| Evidence coverage/freshness | primary | primary | primary |
| New issue discovery | primary within Step | primary across Phase | normally not exploratory |
| Release blockers absent | not a Step concern | identify/classify | primary |

Interpretation:

- **Step Close** asks: is the delivered Step/Slice locally sound and admissible?
- **Phase Review** asks: is the integrated result sound as a whole? This is the broadest semantic quality review.
- **Phase Final** asks: is closure/release justified by fresh, sufficient evidence? It should not restart an unconstrained exploratory review.

CAR lint and textus-cbd-support Review are therefore not three unrelated implementations and should not blindly rerun an identical full scan. The lifecycle context selects scope, checks, evidence reuse, and blocking policy. Phase Final preferentially reuses admitted evidence and examines repair deltas/unresolved blockers.

## textus-cbd-support responsibility

CAR lint remains the deterministic architectural/Textus programming-model diagnostic. Semantic Review handles judgments that require meaning and design context, including unjustified validation/verification, integrity/recovery/synchronization machinery, duplicate/shadow state, repeated observation, unnecessary abstraction and other excessive defensive checking.

The review request should carry enough context to choose an appropriate profile/scope rather than forcing callers to invoke three separate implementations.

- Step context: inspect the changed Slice deeply and connected code needed to judge it.
- Phase context: inspect integration and cross-Step/system effects; this is where duplication, architectural distortion and accumulated overchecking are especially important.
- Final context: confirm unresolved blocking findings and affected repair deltas; normally do not rediscover the entire system from zero.

A future deterministic rule promoted from repeated confirmed semantic findings may move into CAR lint, but semantic overchecking detection must not be forced into CAR lint merely to make every quality concern deterministic.

## Cross-project contract

- sm-workflow determines lifecycle timing, Admission use and repair/re-review progression.
- Cozy provides CML/model-derived scope and domain structure useful to lint/review.
- CNCF provides generic requirement/evidence/admission semantics without encoding sm-workflow lifecycle names.

Review findings should remain typed evidence with scope, revision/freshness, severity/blocking disposition and enough identity to support focused re-review without custom integrity/hash machinery.
