# CAR Review CI Artifact Contract v1

phase=Phase 8
checklist=P8-30
updated_at=2026-07-23

## Purpose

This contract fixes the output boundary between a CBD-owned canonical CAR
Review response and an authorized CI client such as `sbt-cozy`. It defines the
paths, identity bindings, retention posture, profile, and gate exit behavior
before a client materializes the artifacts. CBD Support remains the owner of
Report, attestation, gate, and report renderers; a CI client is a bounded
writer and gate consumer.

The normative machine-readable artifacts are:

- `docs/spec/schema/car-review-ci-artifact-manifest-v1.schema.json`; and
- `docs/spec/examples/car-review-ci-artifact-manifest-v1.json`.

## Admission and artifact root

Artifact materialization begins only after the client has admitted one
`canonical-review-response` and verified that its Report and attestation agree
on `reviewId`, `reportId`, `reportDigest`, target digest, profile, gate, and
all provider/rule-set/bundle bindings. A client never derives or repairs one of
these values.

The manifest names one immutable attempt directory:

```text
<artifact-root>/cbd-review/<attestationDigest-with-colon-replaced-by-hyphen>/
```

The default artifact root is the project `target` directory. The attestation
digest, rather than the stable Report digest, scopes one concrete execution and
therefore prevents a later attestation from overwriting a prior attempt whose
Report content happens to be identical. The directory is created atomically
only after every listed artifact has passed bounded output and redaction checks.

One artifact must not exceed 16 MiB and the complete attempt, including its
manifest, must not exceed 64 MiB. A size, redaction, canonical-binding, or
manifest failure leaves no final attempt directory or successful manifest.

## Required files

Each manifest lists byte digests and these fixed relative file names:

| Manifest member | File | Authority |
| --- | --- | --- |
| `canonicalResponse` | `canonical-response.json` | Exact CBD canonical response. |
| `report` | `report.json` | Exact canonical Report. |
| `attestation` | `attestation.json` | Exact CBD attestation. |
| `markdown` | `report.md` | CBD Markdown projection of the exact Report document. |
| `pdf` | `report.pdf` | CBD PDF projection of the exact Report document. |
| `html` | `report.html` | Authorized deterministic HTML projection. |
| `sarif` | `report.sarif` | Authorized lossy SARIF projection. |

`review-artifacts.json` is the manifest itself. A client may not substitute an
HTML, SARIF, Markdown, or PDF projection for either canonical JSON document.
Every artifact is derived from the same admitted response; materialization does
not rerun a provider, read a workspace beyond already-admitted provider
documents, recalculate a Report digest, alter a gate, or change a conclusion.
A CI client does not silently replace an omitted or unavailable artifact with a
different projection or locally derived conclusion.

## Binding, profile, and retention

The manifest copies the exact Report/attestation identities and digests,
profile, CBD-owned renderer/provider limitations, and gate policy identity. Its artifact byte digests detect changes to
the stored projections, while `reportDigest` and `attestationDigest` retain the
semantic and execution bindings defined by the canonical Review contract.

All three JSON documents, and all their renderings, are subject to the normal
Review redaction policy. Missing renderer support is represented by a visible
omission/limitation in the authorized artifact; a client never invents a
replacement conclusion.

Artifacts are retained in the CI workspace for `pass`, `fail`, and `unknown`.
They are intentionally not publication, distribution, deployment, warehouse,
or catalog output. Upload or archival outside the CI workspace is a separate,
explicit CI integration and must preserve the whole digest-bound attempt
directory. It is not implied by artifact creation or by a passing gate.

## Gate and exit behavior

The manifest records the CBD-owned gate and the process-oriented result code:

| Gate | `exitCode` |
| --- | ---: |
| `pass` | `0` |
| `fail` | `2` |
| `unknown` | `3` |

An input, transport, canonical-binding, renderer, redaction, or manifest
validation failure produces code `1` and no successful manifest. A dedicated
CI gate command maps the accepted manifest result to these codes. An sbt task
may surface a non-zero result as task failure, but it must retain the
manifest's explicit CBD gate and exit code instead of deriving a local policy.

Standard `ci` uses the existing offline deterministic policy. Any network,
external-provider, or AI provider use requires a different explicit profile;
its profile and limitations remain present in the manifest and it cannot satisfy
the standard CI gate.

## Integration boundary

P8-31 connects this contract to `sbt-cozy` task outputs. P8-32 proves all gate
results, offline determinism, and provider limitations. P8-33 proves ordinary
publish, distribution, and deployment tasks remain unchanged; only explicitly
named Review-gated tasks may consume the manifest.
