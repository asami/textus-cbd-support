# CAR Review Provider-Document Submission Contract v1

status=specified
checklist=P5-31
updated_at=2026-07-16

## Purpose

This contract connects an authorized local or CI client such as `sbt-cozy` to
the CBD Review Application without giving CBD workspace, process, or source
acquisition authority. It carries provider documents only; CBD owns Review
policy, canonical Report construction, and the Gate.

The normative artifact is
`docs/spec/schema/car-review-submission-v1.schema.json`.

## Request

`provider-document-submission` has one Review ID and Target plus one through
eight provider document submissions. Each submission includes only provider
availability and the exact descriptor, provider request, and evidence bundle
JSON documents. The nested provider request and bundle must bind the same
Review and Target before CBD admits them.

The request has no `workspacePath`, `projectRoot`, `command`, `environment`,
`source`, `report`, `template`, `gate`, credential, or arbitrary options
field. CBD resolves its own canonical template after authorization.

## Response

`canonical-review-response` contains the canonical `review-report` JSON and
the matching CBD Gate result. The result must equal `report.gate.result`; a
client must not infer or replace it locally.

The contract is transport-neutral. A later private CBD HTTP/CLI adapter and
the `sbt-cozy` transport use this exact JSON body; neither gains server-side
workspace authority.
