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

`canonical-review-response` contains the canonical `review-report`, the
CBD-owned `review-attestation`, and the matching CBD Gate result. The result
must equal `report.gate.result`; the attestation binds the exact Report,
target, profile, providers/rule sets/bundles, and Gate. A client must not infer
or replace either the Gate or attestation locally.

The contract is transport-neutral. A later private CBD HTTP/CLI adapter and
the `sbt-cozy` transport use this exact JSON body; neither gains server-side
workspace authority.

## HTTP and CLI Adapters

The HTTP adapter accepts only `POST` with
`Content-Type: application/json`, an already authenticated/authorized caller,
and at most 128 MiB of UTF-8 request data. It returns the exact canonical
response document and does not proxy, follow redirects, fetch source, or
resolve credentials.

The CLI adapter accepts one UTF-8 request document on stdin, emits one exact
canonical response document on stdout, and has the same 128 MiB input bound.
Its command syntax never accepts a project path, arbitrary command, environment
assignment, template, or gate option. Process identity supplies authorization;
JSON never carries caller roles or credentials.
