# P5-40 CBD Review CLI Contract

status=complete
phase=5
checklist=P5-40
updated_at=2026-07-16

## Decision

`CarReviewCliMain review submit` is the user-facing CLI entry point for the
same CBD Review submission application used by the private HTTP gateway. It
reads one bounded `textus.cbd.review-submission.v1` provider-document
submission from stdin. It has two intentionally distinct transports:

- Local mode invokes `CarReviewSubmissionCliAdapter`; its roles are supplied
  only by the surrounding process boundary through
  `TEXTUS_CBD_REVIEW_PROCESS_ROLES`, never by a CLI option.
- Server-backed mode invokes only the private `post` endpoint and sends the
  generated `submissionDocument` envelope. It forwards neither a synthetic
  role nor credentials and relies on the selected server authentication
  boundary.

Both modes retain the CBD-owned canonical response. The CLI projects its
Review ID as `runId`, retains the whole response and gate, and returns exit
codes `0` pass, `2` fail, `3` unknown, and `1` for command/transport/admission
errors.

## Evidence

- `CarReviewCliSpec` proves the local Review Application path, the
  server-transport path, same Review/gate identity, absence of workspace
  authority, stable output, response-identity refusal, and a real loopback
  private HTTP `POST` exchange. The loopback server resolves `reviewer` at its
  own boundary and receives only the bounded `submissionDocument` envelope.
- `CarReviewCliHttpTransport` accepts only bounded HTTP(S) endpoints with no
  user info, query, fragment, redirect following, credential header, or
  non-JSON response.

Web UI and report/MCP projections remain separate P5-41–45 work.
