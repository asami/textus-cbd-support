# P5-31 CBD Submission Boundary

status=in-progress
phase=5
checklist=P5-31
updated_at=2026-07-16

## Context

The local and CI route must submit Cozy and `sbt-cozy` evidence to CBD without
giving a CBD server a client workspace path, a process command, or authority
to acquire source files. CBD remains the only owner of the canonical Review
Report and Gate.

## Implementation Event

`CarReviewPairedBundleReviewApplication` now receives a canonical report
template and one or more `SuppliedProviderBundleSubmission` values. Each value
contains only Review/Target identity, availability, descriptor, request, and
bundle document text. The boundary requires every supplied Review and Target
to match the report template, admits documents through
`CarReviewSuppliedBundleApplication`, and delegates reconciliation, assessment,
and report construction to `CarReviewCanonicalResponseApplication`.

The canonical response rebuilds completed provider executions from admitted
provider identity, rule set, digest, and limitations. It rejects an ambiguous
capability policy, an unrecalculated baseline, or two distinct bundles from
the same provider identity rather than selecting or discarding data silently.
Provider limitation scopes that are valid in provider-v1 but not report-v1 are
retained as provider-attributed report limitations.

Follow-up: `CarReviewProviderDocumentSubmissionApplication` separates the
client-facing payload from the internal paired-bundle assembly. It accepts a
`SuppliedProviderBundleSet` with only provider documents and derives the
single Review/Target binding. A CBD-owned `CarReviewCanonicalTemplateProvider`
then resolves canonical policy before the paired boundary is called. This
prevents an sbt/CI client from supplying a Report template or choosing a gate
policy, and authorization occurs before policy resolution.

Follow-up: both selected transport forms now have CBD adapters. The HTTP
adapter requires `application/json` and a bounded body; the CLI adapter reads
one bounded JSON stdin document. Both delegate to the identical wire and Review
Application boundary and use resolved caller roles, so their choice cannot
alter canonical Report/Gate ownership or admit workspace/process authority.

The generated private `CbdReviewAdmin.submitReviewDocuments` operation remains
the component-level handoff. The companion generated `CbdReviewAdmin.post`
operation makes the HTTP form executable through the normal CNCF REST route:
`POST /rest/v1/cbd-support/cbd-review-admin/post`. It accepts the generated
outer object with one `submissionDocument` JSON-string field and returns one
`canonical_response` JSON-string field. The inner string is exactly the
versioned provider-document submission/canonical-response wire contract.
`ComponentFactory` sends both operations to the same bounded CBD-owned
application and derives the development template from the CNCF execution clock
and ID generator. The operation stays private to MCP and retains
`reviewer`/`operator`/`admin` submission authorization.

`sbt-cozy` binds its explicit `review.cbd.endpoint` setting and
`cozyReviewSubmit` task to that outer HTTP contract. It still runs the fixed
Cozy provider commands locally and sends only the two provider document sets;
the CBD endpoint cannot receive a workspace path, process command, Report
template, or Gate decision. The local CLI adapter remains available over the
identical inner wire contract. A user-facing standalone CLI executable is
deferred to P5-40 rather than creating a second submission protocol now.

`sbt-cozy` does not supply a role, credential, or caller-chosen header. Review
authority is resolved only by the configured CBD deployment authentication
boundary. The standalone integration fixture uses a SAR-descriptor trusted
local subject with the `reviewer` role, making the development authorization
path explicit without treating an HTTP header as an identity assertion.

Follow-up: the sbt-side HTTP and canonical-response transports now decode JSON
structurally. The generated outer response must contain exactly one non-empty
`canonical_response` string; the inner response must have the exact v1
canonical-response shape, object Report/attestation documents, and a declared
Gate value. This removes string-scanning ambiguity before any Review artifact
is written. `review.cozy.provider_version` is also an explicit setting: the
Cozy provider runtime version is no longer inferred from the version of the
CAR being reviewed.

Follow-up: normal CNCF REST dispatch now decodes an `application/json` object
body into operation arguments. Before that runtime correction, the generated
`submissionDocument` request reached the private `post` operation as an opaque
body and was rejected as absent. The standalone SAR verifier now starts an
actual CBD server with an explicitly configured trusted local `reviewer`
subject, posts the generated HTTP envelope, and checks the generated
`canonical_response` envelope, canonical Review ID, attestation, and Gate.
This proves deployment-owned authorization and the HTTP adapter without
reintroducing a client role header. The local CLI continues to invoke the same
submission application with process-resolved roles.

## Evidence

- `CarReviewCanonicalResponseApplicationSpec` proves canonical report/gate
  identity, provider execution attribution, ambiguous-capability refusal, and
  stale-baseline refusal.
- `CarReviewPairedBundleReviewApplicationSpec` proves path-free client
  admission, canonical response construction, and Review identity refusal.
- `CarReviewBundleReconcilerSpec` proves provider limitation scope projection.
- `CarReviewProviderDocumentSubmissionApplicationSpec` proves that CBD resolves
  its own Report template and refuses an unauthorized caller before resolution.
- `ComponentFactorySpec` proves that the generated gateway is `POST` in the
  OpenAPI projection and remains private to MCP.
- `SbtCarReviewClientSpec` runs a loopback HTTP gateway and proves that
  `sbt-cozy` sends the generated envelope and receives the exact canonical
  response document.
- `Http4sHttpServerDispatchSpec` proves JSON-object REST bodies become named
  operation arguments; its full focused suite passes 23 specifications.
- `scripts/check-cbd-standalone.sh` with `CNCF_RUNTIME_DEV_DIR` starts the
  representative SAR, probes the private HTTP Review route, and reports
  `CBD_REVIEW_SUBMISSION_OK` with the canonical Review ID and Gate.
- `sbt --batch test` passes 225 CBD Support specifications after the endpoint
  operation; CAR lint reports only pre-existing ambient-boundary and missing
  released ABI-baseline warnings.
- `sbt --batch test` passes 96 `sbt-cozy` specifications, including HTTP
  envelope validation without client-supplied roles.

## Remaining Work

P5-31 remains open until an authorized running CBD server accepts an actual
paired Cozy and `sbt-cozy` exchange. The local CLI adapter already shares the
same contract, but its standalone user command belongs to P5-40.
Provider-refusal-to-Unknown workflow coverage remains part of P5-63.

The installed `cozy 0.2.25` launcher does not yet expose `review
car-descriptor` / `review car-evidence`. The maintained Cozy development source
at `/Users/asami/src/dev2025/cozy` does expose and test those commands at
`0.3.0-SNAPSHOT`; P5-31's live exchange therefore uses that development
runtime until the corresponding Cozy release is installed. Publishing that
runtime is a Cozy release task, not a CBD Support compatibility fallback.
