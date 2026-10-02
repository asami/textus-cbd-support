---
status: target
decision_scope: P10-01--P10-03 / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Package Contract

This is the required schemaVersion `2.0` structural package contract.
The [Typed Control Contract](internal-model-typed-control-contract.md) owns
management identity/version semantics; the [Package Design](../design/internal-model-package.md)
explains this carrier boundary. Implementation and executable verification of
the new format are pending. Prior V1 implementation/proof is historical only;
there is no schemaVersion `1.0` reader, adapter, migration, or fallback in this target.

## 1. Scope and authority

The root SHALL be a consuming project's `src/main/internal-model/`:
portable, project-owned source, separate from CBD Support's requirement CML,
canonical CML, runtime resources, retained databases, and `target/` caches.
The local single-writer producer allocates stable identities and positive
revisions and advances versions when control meaning changes.

The [Canonical Component Design Model Contract](canonical-component-design-model-contract.md)
owns CCDM identities, source relationships, canonical facts, projections,
candidate/diff meaning, and Git-governed acceptance. The package carries those
values without creating a parallel model. Structural validity SHALL NOT imply
source authenticity, freshness, semantic/human approval, safe CML mutation,
repository acceptance, or complete rehydration. Phase 10.6 owns build/publication
exclusion; it is not established here.

## 2. Strict JSON envelope

The root SHALL contain exactly one regular `manifest.yaml`. Its content
SHALL be strict UTF-8 JSON, the admitted JSON subset under the existing YAML
filename. BOM, invalid UTF-8, YAML-only syntax, duplicate object members at any
depth, wrong types, missing/extra fields, unsupported schemas, and trailing
non-JSON data SHALL reject. A duplicate field cannot be admitted by a parser
that silently keeps one occurrence.

Writers may use deterministic JSON, sorted object keys, and one terminal LF.
Readers SHALL accept valid harmless object-key order and insignificant JSON
whitespace variation. They SHALL NOT use canonical re-encoding equality,
encoded-content tokens, or byte comparison for identity, validity, or permission.

Envelope tokens use `[A-Za-z0-9][A-Za-z0-9._:-]*`, with no empty,
whitespace-containing, or non-ASCII token. Tokens are validated as decoded JSON
values; equivalent JSON escaping is not a distinct identity. Paths have the
stricter section 5 grammar. Artifact payload may contain arbitrary raw bytes.

## 3. Closed manifest schema

The root has exactly these fields; displayed order is writer presentation only:

```text
artifacts, lifecycleState, packageId, projectId, projectNamespace,
revision, schemaVersion
```

| Field | Required meaning |
| --- | --- |
| `schemaVersion` | Exactly string `"2.0"`; all other schemas fail closed. |
| `packageId` | Allocated stable lowercase hyphenated UUID matching `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`; never content-derived. |
| `projectNamespace` | Valid ASCII token exactly equal to `project.namespace` from the consuming root's `project.yaml`. |
| `projectId` | Valid ASCII token exactly equal to `project.id` from that same context. |
| `revision` | Positive JSON integer fitting `Long`, producer-supplied and monotonically advanced on carrier control changes; no string, sign, exponent, decimal point, leading zero, inferred/default value. Standalone admission does not prove prior history. |
| `lifecycleState` | ASCII descriptive-state token, never approval or CML permission. |
| `artifacts` | Complete closed artifact inventory under section 4. |

`PackageReference(packageId, projectNamespace, projectId)` is distinct
from the carrier's `revision`. Both project source identity values must be
present valid tokens. Missing/malformed identity or mismatch rejects. The sole
project comparison context is the consuming project's `project.yaml`;
directory name, CML, retained state, provider output, or the package's own
claims cannot supply a missing source identity. This supports transport across
checkouts of the same project without transferring source authority.

## 4. Versioned inventory and dependency graph

Each artifact object has exactly these fields:

```text
artifactId, artifactRevision, dependsOn, path, required, role
```

| Field | Required meaning |
| --- | --- |
| `artifactId` | ASCII token unique in the package inventory. |
| `artifactRevision` | Positive producer-supplied JSON integer fitting `Long`; no default or derivation from bytes. |
| `role` | Exactly `resume`, `source-snapshot`, `decision`, `open-issue`, `realization`, `projection`, `approval`, or `validation`. |
| `path` | Unique safe package-relative POSIX path under section 5, never `manifest.yaml`. |
| `required` | JSON Boolean; true requires a present regular file, false permits explicit absence. |
| `dependsOn` | Unique array of closed `ArtifactReference` values, each exactly `artifactId`, `artifactRevision`, and `role`. |

Each dependency resolves to the exact inventory ID, revision, and role. A bare
ID, missing reference, wrong version/role, self-dependency, or cycle rejects.
An absent optional artifact cannot satisfy a present artifact's dependency.

The deterministic topological inventory order is retained: select the ascending
UTF-8 artifact ID among currently dependency-free remaining entries, so every
dependency appears before its consumer. Dependency arrays are ordered by exact
artifact ID, with no duplicate ID. Non-topological/incorrect array ordering
rejects as declared inventory structure, independent of object-key/whitespace
presentation. Filesystem enumeration never chooses this order.

The inventory may omit any role or contain multiple artifacts of the same role.
Content contracts independently establish exact selections and readiness.
The allocated artifact version/role is external control context, separate
from logical record identity and source-owned revision.

## 5. Closed safe filesystem inventory

Present inventoried paths SHALL equal exactly all present regular package files
other than `manifest.yaml`. Required files must be present. Optional absence
is explicit only when `required=false` and no file exists at the path.
Unlisted files, duplicate paths, symlinks, nonregular objects, path escapes, and
missing required files reject. An optional present file obeys the same normal
path, role, version, and dependency rules.

A path consists of slash-separated segments matching
`[A-Za-z0-9][A-Za-z0-9._-]*`. It cannot begin/end with a slash or contain
an empty segment, `.`, `..`, backslash, colon, control/whitespace, or
non-ASCII character. Resolution rejects symlink escapes. Containing directories
are not artifacts.

Raw payload bytes are retained data for the relevant decoder. Inventory
admission supplies no checksum-integrity guarantee or cryptographic authenticity.
The producer must advance an artifact revision when its control meaning changes;
explicit versions do not prove that undeclared payload mutation never occurred.
Removing hashes does not remove content decoding or semantic validation.

## 6. Failure and unfinished executable obligations

Invalid serialization, filesystem inventory, or declared dependency structure
rejects without repair, version inference, fabricated artifact/dependency,
cycle-breaking selection, or legacy fallback. Content admission distinguishes
attributed semantic incompleteness from contradictory identity/revision/role/
scope/state under the typed contract.

`InternalModelPackageValidatorSpec`, `InternalModelPackageFreshnessSpec`,
and the source/realization/continuity/candidate/review/approval/continuation
specifications listed in the typed contract require coordinated migration and
validation. Existing tests establish no current PASS for schema `2.0`.
Source content/freshness, semantic state, decisions, projections, approval,
rehydration, CML gating, security/retained-state policy, build exclusion, and
end-to-end closure retain their separate owners. No Step or Phase is closed here.
