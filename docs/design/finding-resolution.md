# Finding Resolution Design

## Ownership

`FindingResolution` is the CBD-owned immutable working model for an imported
inspection, exact groups, proposals and developer decisions. It uses CNCF
`FindingDispositionV1` directly, including its `interpretC` and `validateC`.
This keeps persistence representations and disposition interpretation with
their existing producers.

The model retains the original inspection, declarations and incomplete coverage
alongside CNCF's report. Grouping and proposing leave that evidence intact.
Proposal and decision references use existing `InternalModelRecordReference`.

## Admission boundary

The runtime package supplies `DeveloperAuthority` independently of untrusted
operation input. It is not a public request payload. The ordinary application
selects the binding using the existing authenticated execution principal.
The pure decision operation verifies the exact import, proposal, kind and keys
and records the actual direction evidence and principal. It does not call AI or
write source. Producer adapters receive admitted declarations only in the later
source-application stage.

## Delivery sequence

1. Qualify the pure import, grouping, proposal and decision core.
2. Connect ordinary Operation/ActionCall and authenticated host bindings.
3. Adopt qualified CNCF Phase 99 and Cozy Phase 77 source contracts.
4. Apply explicit admitted work, re-lint and independently review real results.

Existing Phase 12 source projection, review and canonical editing remain intact.
No new Job, persistence, evidence registry or automatic suppression path is
introduced. The core specification does not establish producer delivery or an
authenticated real-user transaction.

## Application and generated transport

`FindingResolutionApplication` owns the local single-writer map of immutable
Working values. Host-supplied typed import records contain inspections,
declarations and gaps; principal bindings contain allowed imports and separate
developer directions. The application never decodes these host records from
operation JSON. Its closed request codecs reuse the existing Model Edit JSON
vocabulary for exact typed references, closed records and explicit enum values.

`ModelEditAuthorization.authenticatedPrincipalC` owns the existing CNCF
ExecutionContext subject/principal/session-time policy independently of an edit
session. Both Model Edit authorization and finding resolution call it. Model
Edit retains its own canRead, session, scope and mutation-grant checks.

`FindingResolutionActionCall` adapts the generated `CbdFindingResolution`
service factory to the application. ComponentFactory retains no-argument and
Model Edit embedding constructors and supports explicit finding-only and
both-application embedding. Its normal service inventory exposes finding
operations to MCP alongside the existing safe services; generated raw history
operations remain excluded. An absent application returns truthful UNAVAILABLE.

Application state changes only after successful core import, proposal or
decision. Responses project retained evidence and decisions without treating
admission as CNCF resolution. There is no source-writer or AI dependency in this
application. Source application and actual reinspection remain later Steps.
