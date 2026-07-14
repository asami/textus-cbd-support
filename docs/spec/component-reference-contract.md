# ComponentReference Contract

## Purpose

`ComponentReference` is the small, evidence-bearing handoff contract between
SIE component existence discovery and `textus-cbd-support` component detail
retrieval. It prevents component-development metadata from polluting SIE's BoK
knowledge model while keeping the two MCP servers composable by generative AI.

## Fields

| Field | Required | Meaning |
|---|---:|---|
| `sourceId` | no | SIE provider dataset identity when the reference came from BoK knowledge. |
| `catalogId` | no | CBD catalog source identity when the reference came from CBD Support. |
| `organization` | no | Published component organization. |
| `name` | yes | Exact CAR/SAR artifact identity. |
| `title` | yes | Human-readable title from source evidence. |
| `kind` | yes | `car` or `sar`. |
| `version` | no | Published selected version; absent when the source has no version evidence. |
| `evidenceUri` | yes | Authoritative source URI proving the reference. |

At least one of `sourceId` and `catalogId` may be present. Neither identifier is
portable across providers, so consumers must use `name`, `kind`, optional
`organization`/`version`, and `evidenceUri` for cross-server handoff.

## Ownership

SIE may expose only existence-level fields. It must not add dependencies,
services, operations, compatibility matrices, artifact lists, manuals,
examples, or reuse recommendations to this response.

CBD Support may return the same reference beside a detailed `ComponentProfile`.
The profile is catalog-derived and belongs to CBD operations, not to SIE BoK
grounding.

## Failure Contract

- `no-match` returns no synthetic reference.
- Missing optional evidence remains absent and may produce a warning.
- A reference does not assert that the component is compatible with the
  caller's runtime. Compatibility is evaluated by CBD Support.
