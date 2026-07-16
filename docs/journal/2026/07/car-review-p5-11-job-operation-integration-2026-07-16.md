# CAR Review P5-11 Job Operation Integration — 2026-07-16

## Work

The typed Review Run lifecycle was connected to the CBD Support application
and generated component operation surfaces. `CarReviewRunApplication` now
owns authorized start, read, and cancellation admission while
`CncfCarReviewJobGateway` owns the CNCF Job submission, query, and control
boundary.

The CML surface adds:

- `CbdRetrieval.getReviewRun` as the only current MCP-ready Review query; and
- private `CbdReviewAdmin.startReview` and `cancelReview` commands for Web,
  CLI, `sbt-cozy`, and internal component callers.

The application creates a CBD Review ID independently from the CNCF Job ID and
retains their one-to-one `ReviewRunJobBinding`. Start submits a persistent
asynchronous Job. Read projects the actual Job status through
`CarReviewRunLifecycle`. Cancellation becomes visible as `cancelling` only
after the Job control boundary accepts the request, and a later read settles
the terminal state.

## Decisions

Review start is a synchronous admission command that submits the separate
Review Job and returns both identities. It is not modeled as an outer generated
async command, because that surface would return only the framework Job ID and
would hide the CBD-owned Review ID.

Authorization is checked twice: `CarReviewRunApplication` checks the P5-04
action roles at operation admission, and the CNCF query/control policies check
the same roles again at the resource boundary. The initial roles remain:

- start: `reviewer`, `operator`, or `admin`;
- read Run: `viewer`, `reviewer`, `operator`, or `admin`; and
- cancel: `operator` or `admin`.

MCP exposure follows the service split rather than an accidental command
allowlist inside one MCP-ready service. `CbdRetrieval` remains MCP ready and
contains the bounded read query; `CbdReviewAdmin` is not MCP ready. The live
MCP and CAR ABI expectations were updated from six to seven CBD read tools and
from seven to ten total component operations.

The current Job task is the P5-11 execution shell. P5-12 supplies provider
bundle admission and canonical report production. Until a Job returns both a
report ID and report digest, success is not inferred: a succeeded Job without
that identity becomes the explicit `review-report-missing` failed Run.

## Evidence

- `CarReviewRunApplicationSpec` proves Review-to-Job binding, progress,
  canonical completion, cancellation intent, role denial before the Job
  boundary, and actual submission/cancellation through a held
  `InMemoryJobEngine`.
- `ComponentFactorySpec` proves all three service factories, `getReviewRun`
  MCP publication, and the private status of both Review commands.
- Focused execution passed 21 scenarios across `CarReviewRunApplicationSpec`,
  `CarReviewRunLifecycleSpec`, and `ComponentFactorySpec` before final
  repository validation.

## Boundary

P5-11 does not admit a provider descriptor/request/evidence bundle, reconcile
Evidence, persist Runs across process restart, or render a report. Exact bundle
admission begins at P5-12; reconciliation is P5-13; persistence and retention
are P5-14; broader user/report surfaces remain later Phase 5 checklist work.
