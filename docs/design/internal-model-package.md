---
status: stable
decision_scope: P10-01--P10-03
updated_at: 2026-09-27
---

# Internal-model Package Design

## Purpose and authority

This design fixes the portable package boundary consumed by the Phase 10.1--10.7
plans and by the later Phase 10 integrity implementation. Its normative companion
is the [Internal-model Package Contract](../spec/internal-model-package-contract.md).
It promotes the stable package boundary from the agreed direction recorded in the
[project-local internal-model storage direction](../journal/2026/08/2026-08-17-project-internal-model-storage-direction.md).

The package records, but does not reinterpret, the semantic identities and source
relationships owned by the Phase 9 [Canonical Component Design Model
Contract](../spec/canonical-component-design-model-contract.md). Phase 9 remains
authoritative for CCDM semantic facts, identity meaning, projections, candidate
design, semantic diff, review, and Git-governed acceptance. A package is a
portable record of those admitted facts; it is not a second Entity, Event,
Workflow, Mono-Koto, or CML model.

## Source-root and storage boundary

`src/main/internal-model/` denotes a *consuming project's* versioned,
project-owned portable source root. It is not this CBD Support repository's
requirement CML (`src/main/cml/...`), a consuming project's canonical CML,
a runtime resource, a CBD Support retained database, or a `target/` cache.

The root enables a selected package to move between a checkout, process, or
developer without requiring chat/provider history, disposable output, or CBD
Support retained state merely to identify the package. Retained state can keep
richer audit and collaboration history, but must not be the sole locator for
the package's structural contents. Structural portability does not yet prove
complete semantic rehydration; that is owned by later Phase 10 work.

Phase 10.6, not this design, owns proof that this source kind is excluded by
default from runtime packaging, public APIs, ordinary CML generation,
documentation publication, and CAR/SAR artifacts. This design does not claim
that exclusion has been implemented or verified.

## One manifest-led package

Each package has exactly one root `manifest.yaml`. The manifest identifies the
package, its owning project, the schema, revision, descriptive lifecycle state,
complete artifact inventory, and package-wide digest. All other regular files
under the root are artifacts governed by that inventory.

Project-bound structural validation compares the manifest identity only with
the consuming project's `project.yaml`: `projectNamespace` with
`project.namespace` and `projectId` with `project.id`. Both source values and
both manifest values must be valid ASCII tokens, and each pair must exactly
match; a missing, malformed, or mismatched identity rejects validation. This
uses neither directory/CML/CBD/provider/package inference nor checksum-only
validation, while retaining portability across checkouts of the same project.

The manifest is deliberately an exact JSON serialization kept under a `.yaml`
name: JSON is a YAML 1.2 subset, while canonical JSON bytes make cross-process
integrity reproducible. A validator rejects rather than reformats malformed,
duplicate-field, or noncanonical input. This prevents a permissive parser from
turning an ambiguous source package into an accepted package.

The package identifier is a stable UUID allocated for the package and never
derived from content. The revision advances monotonically across the package
lifecycle, but an isolated validator can validate only its positive syntax; it
cannot prove history that the package does not contain. `lifecycleState` is
descriptive status only. Neither it, a matching digest, nor an artifact role is
approval, a permission to mutate CML, or repository acceptance.

## Inventory and structural integrity

The manifest inventory names every non-manifest package file by a stable ID,
semantic role, POSIX-relative path, required flag, exact raw-byte SHA-256, and
explicit dependency IDs. `manifest.yaml` is never an artifact path. It creates
a closed package boundary:

- every existing non-manifest file is listed exactly once;
- every listed present file has the listed exact raw-byte digest;
- required artifacts are present;
- an optional artifact may be absent only explicitly, and an absent optional
  artifact cannot satisfy a dependency of an artifact that is present; and
- package-relative references, IDs, and ordering are deterministic.

Paths cannot escape the root. Symlinks, nonregular files, duplicate IDs or
paths, unlisted files, unresolved dependencies, and dependency cycles are all
structural failure states. The artifact order is a deterministic topological
order, with the artifact ID as the tie-break. This lets independent validators
produce the same structural ordering without treating filesystem enumeration
order as authority.

`packageDigest` hashes the canonical manifest object with only its own field
omitted. Since that object includes every artifact digest, it binds the complete
inventory without a self-reference. A Git commit, provider output, review record,
or a hash-valid individual artifact is not a substitute for the package digest.

## Role vocabulary and deferred semantics

The V1 inventory has the following closed structural role vocabulary from the
provisional Phase layout; any other role rejects V1 validation:

| Package concern | Closed V1 role |
| --- | --- |
| continuation cursor | `resume` |
| Scenario, model-context, glossary/BoK, or CML-baseline basis | `source-snapshot` |
| settled choice | `decision` |
| unresolved question | `open-issue` |
| selected semantic mapping | `realization` |
| candidate CML projection | `projection` |
| attributable human decision | `approval` |
| recorded checking evidence | `validation` |

V1 does not require every role, prescribe artifact content schemas, or decide
which artifacts are semantically ready. Phase 10.1 owns source snapshots and
freshness; Phase 10.2 owns durable Phase 9-compatible semantic state and
decisions; Phase 10.3 owns projection, review, and approval; Phase 10.4 owns
rehydration; Phase 10.5 owns drift and the CML change gate; Phase 10.6 owns
retained-state/security integration and build exclusion; Phase 10.7 owns
end-to-end validation and closure.

Consequently, structural integrity never implies semantic approval, safe CML
mutation, current freshness, or complete rehydration. Those claims require their
separately owned contracts and evidence.

## Explicit non-implementation

This design creates no package, parser, validator, runtime, CLI, HTTP, MCP,
SPI, CML mutation, public API, build rule, fixture, executable specification,
or approval mechanism. P10-04 remains the separately owned executable
fail-closed proof for this contract.
