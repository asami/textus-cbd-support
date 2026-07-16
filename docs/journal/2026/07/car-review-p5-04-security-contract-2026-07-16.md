# CAR Review P5-04 Security Contract — 2026-07-16

## Work

The security and reproducibility boundaries required before Review Application
runtime implementation were promoted into one strict policy contract.

## Decision

`textus.cbd.review-security.v1` is deny-by-default. Every target mode, action,
filesystem root, process operation template, network origin, credential
reference, provider class, projected surface, retained artifact, and MCP
operation requires explicit authority.

Local CLI targets may name a path only inside a configured canonical root.
Server Review never accepts a caller host path: it uses a configured stable
development-root reference or a bounded digest-addressed uploaded CAR.
Traversal is read-only, canonical, bounded, and no-follow. Process execution
uses exact commands plus fixed operation templates without ambient environment
inheritance. Network is disabled or exact-origin allowlisted with finite work
and no redirects.

Credential values use only `config-key` references and resolve once at the
authorized outbound provider boundary. References and values never enter
reports or projections. One redaction boundary covers canonical report,
attestation, text, HTML, SARIF, log, CallTree, MCP, and AI input.

AI is opt-in and consumes bounded structured Evidence only. Raw source,
credentials, external search tools, and provider wire payloads are excluded;
AI cannot override deterministic Findings or establish final Assurance.

MCP remains default-deny. Its fixed v1 candidate set contains five authorized
read queries. Review start/cancel/delete, retention configuration, external/AI
provider enablement, and filesystem configuration remain private.

Standard CI is offline, deterministic, pinned, credential-free, UTC/C locale,
and restricted to deterministic providers. The determinism audit exposed a
P5-03 defect: `reportDigest` included volatile Review/report IDs and execution
times. P5-03 normalization now omits those values and the baseline report ID,
and recursively canonicalizes arrays. Stable report content is identical for
identical Evidence regardless of local/CI Run identity or arrival order. The
attestation retains concrete execution identity and time.

## Evidence

- `docs/spec/car-review-security-contract.md`
- `docs/spec/schema/car-review-security-policy-v1.schema.json`
- `docs/spec/examples/car-review-security-policy-development-v1.json`
- `docs/spec/examples/car-review-security-policy-ci-v1.json`
- `CarReviewSecurityContractSpec`: 4 scenarios passed
- `CarReviewReportContractSpec`: stable digest scenario refined and passed

## Boundary

This slice specifies policy and representative admission invariants. P5-10
through P5-14 implement the canonical model, authorization, lifecycle,
provider admission, persistence, retention, and comparison. P5-20 implements
provider containment, while P5-34 and P5-44/P5-45 prove enforcement through CI
and MCP runtime surfaces.
