# CAR Review Security and Reproducibility Contract v1

status=specified
checklist=P5-04
updated_at=2026-07-16

## Purpose

This specification fixes the security boundary for CBD-owned CAR Review before
the Review Application begins runtime implementation. It applies equally to
Web, CLI, `sbt-cozy`, CI, report projection, and MCP entry points. A transport
or UI cannot enlarge the authority granted by the admitted policy.

Normative machine-readable artifacts are:

- `docs/spec/schema/car-review-security-policy-v1.schema.json`
- `docs/spec/examples/car-review-security-policy-development-v1.json`
- `docs/spec/examples/car-review-security-policy-ci-v1.json`

Both examples use `schemaVersion=textus.cbd.review-security.v1` and
`documentType=review-security-policy`. Unknown versions and fields are denied.

## Deny-by-Default Authority

Every Review action, target, filesystem root, process, network origin,
credential reference, provider class, retained artifact, and MCP operation
requires an explicit policy entry. Missing, incompatible, or contradictory
policy produces a failed admission or attributable Unknown; it never selects a
more permissive fallback.

Authorization is checked at command/query admission and again at the resource
boundary. The initial roles are `viewer`, `reviewer`, `operator`, and `admin`.
Read access does not imply execution, cancellation, provider enablement,
retention deletion, filesystem access, network access, or AI-cost authority.

## Target Admission

The admitted target modes are:

| Mode | Executor | Authority |
| --- | --- | --- |
| `local-development-directory` | client | An explicitly configured canonical development root. |
| `local-car` | client | A CAR under an admitted local warehouse/cache root. |
| `server-development-root` | server | A server-configured stable root reference, never a caller path. |
| `uploaded-car` | server | A bounded uploaded CAR admitted by artifact digest. |

All modes require a target digest. Directory roots are normalized to absolute
real paths before traversal, remain read-only, and never follow symbolic
links. A local client may supply a path only when its canonical real path is
contained by an admitted root. A server request identifies
`server-development-root` by an admitted root reference and a bounded relative
target ID; `acceptsCallerPath` is false for every server mode. Uploaded
artifacts are placed in an isolated staging root, checked
against the configured byte bound, and addressed by digest. A server never
interprets an uploaded archive entry as host filesystem authority.

Working tree, built CAR, published baseline, catalog, provider bundle, and
runtime Evidence retain distinct origins. One origin cannot authorize another.

## Filesystem and Process Containment

Filesystem access is denied or bounded read-only. An enabled boundary has
explicit root references and finite traversal depth, file count, total byte,
and per-file byte limits. Symbolic-link following is always false. Writes are
limited to an isolated staging/output root owned by the Review Run and never
reuse an admitted source root.

Process execution is disabled or uses an exact command allowlist and fixed
operation templates with finite invocation, duration, stdout, and stderr
limits. Arbitrary caller arguments and shell fragments are not templates. The
child receives only named environment configuration keys; ambient environment
inheritance is false.
Cancellation and timeout terminate the provider work and record a limitation
or failed provider outcome. A process cannot expand filesystem or network
authority.

## Network and Credentials

Network access is either `disabled` or `exact-origin-allowlist`. An enabled
policy contains credential-free HTTP(S) origins and finite request, response,
and duration limits. Redirect following is false; a provider must re-admit any
derived origin explicitly. Catalog, BoK, SIE, runtime, and commercial AI
providers are external providers and remain disabled until both their provider
class and exact origin are admitted.

Only `config-key` credential references are accepted. Values are resolved once
at the outbound provider boundary after target/origin authorization. Raw
credentials, environment names, header values, and secret-store responses are
not Review request or report fields. Credential values and references are not
persisted or projected.

## Redaction and Output Boundary

The same redaction policy applies before content enters canonical reports,
attestations, text, HTML, SARIF, logs, CallTree properties, MCP output, or AI
input. It:

- removes URI user information, query data, and fragments;
- excludes credentials, tokens, request/response bodies, raw source content,
  absolute local paths, ambient environment, and provider wire payloads;
- emits relative target locations or admitted evidence URIs;
- bounds text and location counts; and
- records a safe limitation when content cannot be represented safely.

Redaction cannot turn rejected input into authority or turn missing Evidence
into Assurance.

## AI Input and Execution

AI Review is disabled unless an authorized action explicitly enables the AI
provider class. The input is `structured-evidence-only`, bounded by item and
byte counts, and contains only admitted fact, subject, rule, location, digest,
and deterministic-observation fields. Raw source, credentials, request bodies,
ambient environment, and provider wire responses are excluded.

Textus AI resolves the configured provider/model and performs structured
generation. `web_search` and `url_context` remain disabled. CBD Support admits
the structured result and owns its cache/cost bounds. AI output is advisory:
it cannot suppress, lower, replace, or override a deterministic Finding and
cannot establish a final Assurance by itself.

## MCP Publication

MCP publication is default-deny. The v1 read-only candidate set is:

- `getReviewRun`;
- `getReviewSummary`;
- `getReviewReport`;
- `listReviewFindings`; and
- `listReviewAssurances`.

Publication still requires operation-level MCP readiness, caller
authorization, bounded pagination, report ownership/visibility, and redaction.
Report, Finding, and Assurance queries read admitted immutable reports;
`getReviewRun` may project authorized progress and limitations.

`startReview`, `cancelReview`, `deleteReview`, `configureRetention`,
`enableExternalProvider`, `enableAiProvider`, and filesystem-administration
operations are private to MCP in v1. Web, CLI, `sbt-cozy`, or internal service
calls may invoke an authorized command without making it MCP-ready.

## Retention and Immutability

Run, report, Evidence-bundle, and uploaded-artifact retention use finite day
and per-target/run counts. Reports and attestations are digest-bound and
immutable until expiry or an authorized deletion. Deletion requires the
configured administrative role, produces an audit record without report
content, and cannot mutate a retained report into a different digest.
Baseline comparison refers to immutable report digests; stale or deleted
baselines produce an explicit limitation.

## Deterministic and Offline CI

The standard `ci` profile is offline and deterministic:

- network mode is `disabled`;
- external, runtime-network, and AI provider classes are disabled;
- enabled provider and rule-set versions are pinned;
- locale is `C`, timezone is `UTC`, and the random seed is fixed;
- source ordering and canonical report ordering are deterministic;
- volatile Review IDs and execution timestamps do not contribute to the
  canonical `reportDigest`; and
- credentials are neither required nor resolved.

An opt-in external CI profile is a different explicit policy identity and does
not inherit the standard CI attestation. A report or attestation generated
under a different policy/profile cannot satisfy the standard CI gate.

## Admission Invariants

CBD Support admits a Review security policy only when:

1. schema, document, policy, version, and profile identities are exact;
2. all actions have explicit roles and MCP exposure;
3. every server target mode rejects caller-supplied host paths;
4. path, process, network, output, AI, retention, and pagination bounds are
   finite and positive;
5. symbolic-link following, ambient environment inheritance, redirects, raw
   source AI input, and credential persistence are false;
6. credential references use `config-key` and resolve only at the outbound
   boundary;
7. every persisted/projected surface participates in redaction;
8. MCP-ready operations are a subset of the fixed read-only query set and all
   execution, cancellation, retention, external-provider, AI, and filesystem
   commands remain private;
9. the standard CI profile disables network, external/AI providers, and
   credential resolution while pinning deterministic inputs; and
10. policy authority can only narrow provider-specific bounds, never expand
    them.

Violation fails policy or Review admission without permissive fallback.

## Deferred Runtime Work

P5-04 fixes policy semantics; P5-10 through P5-14 implement report/run models,
authorization, admission, persistence, reconciliation, and retention. P5-20
implements provider containment. P5-34, P5-44, and P5-45 prove CI and MCP
enforcement on their actual runtime surfaces.
