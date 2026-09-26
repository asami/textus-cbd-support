---
status: stable
decision_scope: P10-01--P10-03
updated_at: 2026-09-27
---

# Internal-model Package Contract

This is the normative V1 structural package contract for P10-01--P10-03. Its
rationale and authority boundaries are in the [Internal-model Package
Design](../design/internal-model-package.md). P10-04 owns the later executable
proof; this document defines the behavior that proof must cover.

## 1. Scope and authority

The package root SHALL be a consuming project's
`src/main/internal-model/`. It SHALL be portable, project-owned source. It
SHALL NOT be interpreted as CBD Support's own requirement CML, canonical CML,
a runtime resource, a retained CBD Support database, or a `target/` cache.

The package SHALL retain admitted Phase 9 identities and source relationships
without creating a parallel semantic model. The [Canonical Component Design
Model Contract](canonical-component-design-model-contract.md) remains
authoritative for the meaning of CCDM identities, canonical facts, projections,
candidate design, semantic diff, review, and Git-governed acceptance.

Structural validity SHALL NOT imply semantic approval, current freshness, safe
CML mutation, repository acceptance, or complete rehydration. Build and
publication exclusion are reserved for Phase 10.6.

## 2. Root and canonical V1 serialization

The root SHALL contain exactly one regular file at the relative path
`manifest.yaml`. V1
manifest bytes SHALL be UTF-8 without a byte-order mark and SHALL be valid JSON
syntax, which is the admitted strict subset of YAML 1.2. YAML-only syntax is
not admitted.

The V1 canonical byte form SHALL satisfy all of the following:

1. it contains no insignificant whitespace;
2. it ends in exactly one LF (`0x0A`) and has no other trailing byte;
3. every object uses its keys in ascending UTF-8 byte order;
4. it uses only the JSON punctuation and escaping required to represent its
   values; and
5. it has no duplicate object member name at any depth.

A validator SHALL parse the bytes without normalizing them, reject a duplicate
field even if its parser would otherwise retain one value, serialize the parsed
value under this contract, and accept only when the resulting bytes are exactly
equal to the supplied bytes. A missing BOM check, a non-JSON YAML feature,
noncanonical key order, excess whitespace, duplicate field, or any other byte
difference SHALL be rejected rather than repaired.

Except for JSON punctuation, the manifest's strings SHALL be constrained ASCII
tokens. A token has the regular-language form
`[A-Za-z0-9][A-Za-z0-9._:-]*`; no empty, whitespace-containing, non-ASCII, or
escaped Unicode envelope string is admitted. `path` uses the more restrictive
rule in section 5. Artifact *contents* are outside the envelope and MAY contain
arbitrary raw bytes, including Unicode.

## 3. Manifest schema

The manifest root object SHALL contain exactly these fields, in this canonical
order:

```json
{"artifacts":[],"lifecycleState":"...","packageDigest":"sha256:...","packageId":"...","projectId":"...","projectNamespace":"...","revision":1,"schemaVersion":"1.0"}
```

The ellipses illustrate values only; they are not V1 syntax. Unknown, omitted,
or additional root fields SHALL be rejected. Field requirements are:

| Field | Required V1 meaning and validation |
| --- | --- |
| `schemaVersion` | Exactly the string `"1.0"`. Any other version is incompatible and SHALL fail closed. |
| `packageId` | A canonical lowercase hyphenated UUID: `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`. It is stable package identity and SHALL NOT be derived from manifest or artifact contents. |
| `projectNamespace` | An owning-project ASCII token that SHALL exactly equal `project.namespace` in the consuming project's `project.yaml` validation context. |
| `projectId` | An owning-project ASCII token that SHALL exactly equal `project.id` in the consuming project's `project.yaml` validation context. |
| `revision` | A JSON integer greater than zero. It SHALL have no decimal point, exponent, sign, leading zero, or surrounding string representation. Evolution SHALL advance revisions monotonically; standalone validation does not claim to prove prior-package history. |
| `lifecycleState` | An ASCII token describing lifecycle state only. It is neither approval nor CML-mutation permission. |
| `artifacts` | The complete, canonically ordered V1 artifact inventory in section 4. |
| `packageDigest` | The lowercase `sha256:` digest token in section 6. |

Project-bound structural validation SHALL use the consuming project's root
`project.yaml` as its only identity comparison context. Its
`project.namespace` and `project.id` values SHALL both be present valid ASCII
tokens, and `projectNamespace` and `projectId` SHALL exactly equal them. A
missing or malformed source identity, an invalid manifest identity token, or a
mismatch SHALL be rejected. A validator SHALL NOT infer the identity from a
directory name, CML, CBD retained state, provider output, package claim, or any
other source. A manifest-only checksum check SHALL NOT claim project-bound
structural validity without this context. This comparison preserves package
portability across checkouts of the same project.

## 4. Artifact inventory schema and order

Each item in `artifacts` SHALL be an object with exactly these fields in this
canonical order:

```json
{"artifactId":"...","dependsOn":[],"path":"...","required":true,"role":"...","sha256":"sha256:..."}
```

| Field | Required V1 meaning and validation |
| --- | --- |
| `artifactId` | An ASCII token unique in the inventory. |
| `role` | An ASCII token describing the artifact's semantic role. The closed V1 role values are `resume`, `source-snapshot`, `decision`, `open-issue`, `realization`, `projection`, `approval`, and `validation`; any other role SHALL be rejected for `schemaVersion` `"1.0"`. Their content schemas and readiness rules are not defined here. |
| `path` | A unique package-relative POSIX path as defined in section 5. It SHALL NOT be `manifest.yaml`. |
| `required` | A JSON Boolean, never a string. `true` requires a present regular file; `false` permits explicit absence. |
| `sha256` | The lowercase exact-raw-byte digest token in section 6. |
| `dependsOn` | An array of unique `artifactId` tokens in ascending UTF-8 byte order. Each ID identifies a listed artifact. |

An artifact that depends on another artifact SHALL appear after that dependency.
The `artifacts` array SHALL be the deterministic topological ordering of its
dependency graph: among all currently dependency-free remaining IDs, the
ascending UTF-8-byte `artifactId` is selected first. Duplicate IDs, duplicate
paths, unresolved dependency IDs, a self-dependency, a cycle, noncanonical
dependency ordering, or a non-topological artifact order SHALL be rejected.

The inventory may contain no artifact for any closed V1 role and may contain more
than one artifact with the same role. V1 does not use role presence or
multiplicity to establish semantic readiness, approval, or rehydration.

## 5. Closed filesystem inventory

An artifact entry SHALL NOT use `path = "manifest.yaml"`. The set of paths for
present inventory entries SHALL equal exactly the set of present regular package
files other than `manifest.yaml`. For every inventory entry whose path is
present, the path SHALL resolve within the package root to a regular file and
its raw bytes SHALL match that entry's digest. An unlisted package file, a
listed file that is missing when `required` is true, a listed-present file with
a mismatched digest, a symlink, or a nonregular filesystem object SHALL be
rejected.

An optional artifact is absent only when its `required` value is `false` and no
regular file exists at its listed path. An absent optional artifact SHALL NOT
satisfy a dependency of any present artifact; that present artifact is rejected.
An optional artifact that is present remains subject to every normal inventory,
path, and digest rule.

A V1 path consists of one or more slash-separated segments matching
`[A-Za-z0-9][A-Za-z0-9._-]*`. It SHALL NOT begin or end with `/`, contain an
empty segment, `.`, `..`, `\\`, a colon, a control/whitespace character, or a
non-ASCII byte. Resolution SHALL reject all path escapes, including an escape
introduced by a symlink. Directories used only to contain listed regular files
are not artifacts themselves.

## 6. Digests

An artifact `sha256` value SHALL be exactly
`sha256:` followed by 64 lowercase hexadecimal characters. It is the SHA-256 of
the artifact's exact raw bytes; text decoding, line-ending conversion, Unicode
normalization, and parser reserialization SHALL NOT precede this digest.

`packageDigest` SHALL use the same lowercase token form and SHALL equal SHA-256
of the canonical UTF-8 JSON serialization, including its exactly one terminal
LF, of the manifest root object with only the `packageDigest` member omitted.
The remaining root keys and every
artifact entry, including all artifact digests, remain in that hashed object.
A missing, malformed, or mismatched package digest SHALL be rejected. A Git
commit, provider output, review record, or individual artifact digest SHALL
NOT substitute for it.

## 7. Fail-closed result and reserved work

Any violation in sections 2--6 makes the package structurally invalid. A
validator SHALL return rejection and SHALL NOT repair bytes, invent an
artifact, infer an omitted dependency, choose a cycle-breaking order, accept an
unknown schema version, or normalize malformed input into acceptance.

P10-04 owns executable integrity proof for these structural failures. Later
Phases own source-snapshot contents/freshness, Phase 9-compatible semantic
state, decisions, projections, approval, rehydration, CML mutation gates,
retained-state/security policy, build exclusion, and end-to-end validation.
This contract introduces none of those behaviors.
