# CAR Review Delivery and Diagnosis Contract v1

status=specified
checklist=P8-01,P8-02,P8-03,P8-04
updated_at=2026-07-23

## Purpose

This contract defines the common read-only document model used by future CAR
Review dashboard, item-diagnosis, Markdown, and PDF surfaces. The source is
one already-admitted immutable `CarReviewReport`. The model is not a provider,
repository, renderer, or policy owner.

## Identity and Non-derivation

Every document contains the exact `reviewId`, `reportId`, `reportDigest`,
target kind/version/digest, profile, and gate from its canonical Report. Target
organization and name are delivery-safe projections and are redacted when they
would expose prohibited output. Observation diagnoses
retain their Observation, Rule, Evidence, capability, provider, location, and
disposition identities. Capability diagnoses retain their assessment,
Observation, Evidence, and provider identities. A baseline is displayed only
when the canonical Report contains its immutable baseline identity and delta.

Projection is deterministic: reports, observations, capabilities, providers,
Evidence IDs, locations, strengths, gaps, limitations, and baseline IDs use
their defined stable ordering. Projection does not invoke a provider, inspect
filesystem or report history, change a Report digest, calculate a new gate,
promote maturity, suppress an Unknown, or create a Finding or Assurance.

## Document Model

The common model consists of:

- **dashboard**: report identity, target, profile, exact gate, Finding /
  Assurance / Unknown counts, and an optional canonical baseline delta;
- **observations**: a redacted, bounded representation of each canonical
  Observation and its Rule, Evidence, capability, provider, location, and
  disposition identities;
- **capabilities**: a redacted representation of each canonical assessment,
  including applicability, maturity, coverage, confidence, provider,
  Evidence, Observation, strength, and gap identities; and
- **limitations**: canonical limitations with safe messages.

An item diagnosis is exact-Report addressing only. It is either an Observation
or capability from that Report, and returns no synthetic fallback for a missing
item. A later Web surface maps this absence to its authorized missing-record
response; the projection itself does not query another Report or history.

## Authorization, Redaction, and Pagination Boundary

The projection is authorization-neutral but not publication-neutral. A caller
must be authorized to read the exact immutable Report before a delivery surface
may project it. Web and MCP policy continue to own authorization and report
visibility; this model does not widen read authority or introduce report-history
enumeration.

The delivery model applies the established review output boundary: diagnostic
text is sanitized, URI user information/query/fragment data and absolute paths
are removed, and Evidence facts, Observation rationale, credentials, provider
wire data, raw source, and ambient environment are excluded. Redaction never
turns missing Evidence into Assurance.

Future collection endpoints use an exact Report identity and stable order. They
must require an explicit page size in the inclusive range 1–100 and bind any
cursor to the report digest and collection order. Pagination cannot span
Reports, discover history, or change an item diagnosis. P8-10 implements those
transport rules; no endpoint is introduced by this contract.

## Private Web Surface

`CbdReviewAdmin.getReviewDashboard` and
`CbdReviewAdmin.getReviewDiagnosis` are authenticated private Web queries. They
admit only the existing `review.read-run` role, address one exact Report ID,
and use the common delivery model. Diagnosis accepts only `observation` or
`capability` plus one exact item ID; an absent or unsupported item is a missing
operation result, never a fallback Report or synthetic diagnosis. The response
keeps the Report/gate/baseline/Unknown identities and exposes only redacted
locations, limitations, and deterministic navigation guidance. The queries do
not invoke providers, execute a rule, choose a disposition, or enumerate
history. They are in the `CbdReviewAdmin` service and therefore remain private
to MCP even though their static Web forms are authenticated.

## Renderer Boundary

Markdown and PDF are later deterministic renderers over this same document
model. They must expose report identity, gate, canonical omissions, and
limitations without mutating the Report or evaluating a rule. JSON remains the
canonical interchange representation; HTML and SARIF keep their existing
specialized contracts.

The common document order is fixed as: report identity; gate and gate reasons;
dashboard counts; optional baseline delta; capabilities by capability ID;
Observations by Observation ID; diagnosed item when the caller selected an
exact item; limitations by scope/code/subject/message; and an explicit
redaction-or-omission section. Markdown uses that order as headings and tables
or lists without renderer-generated conclusions. PDF uses the same heading and
reading order, exposes searchable text, document title/language, semantic
headings, table headers, and text labels for non-text content, and never relies
on colour alone for gate or severity. A renderer records unavailable PDF
accessibility features as a limitation rather than silently omitting them.

## Executable Evidence

`CarReviewDeliveryProjectionSpec` proves deterministic same-Report projection,
identity preservation for dashboard and diagnoses, baseline retention,
redaction, exact missing-item behavior, and no report mutation.
