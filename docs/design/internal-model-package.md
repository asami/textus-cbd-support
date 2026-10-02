---
status: target
decision_scope: P10-01--P10-03 / P104-TYPED-PRODUCT-CONTRACT
updated_at: 2026-10-01
---

# Internal-model Package Design

## Purpose and authority

This design fixes the portable carrier boundary under the
[Package Contract](../spec/internal-model-package-contract.md) and the
[Typed Control Contract](../spec/internal-model-typed-control-contract.md).
It records the settled schema `2.0` target; executable migration and verification
are pending. The [project-local storage direction](../journal/2026/08/2026-08-17-project-internal-model-storage-direction.md)
provides historical rationale only.

Phase 9's [Canonical Component Design Model Contract](../spec/canonical-component-design-model-contract.md)
owns semantic facts and source relationships. A portable carrier records those
admitted values and explicit versions; it creates no second Entity, Event,
Workflow, Mono-Koto, or CML model.

## Project-owned source root

`src/main/internal-model/` is the consuming project's portable source root.
It is separate from CBD Support requirement CML, consuming canonical CML,
runtime resources, retained databases, and disposable `target/` output.
A selected package can move across process or checkout without recovering
identity from chat/provider history or retained CBD state.

Project-bound admission compares manifest project namespace and ID only to
the consuming root's `project.yaml`. Missing source identity remains a failure.
It is not inferred from directories, CML, provider output, or package claims.
Phase 10.6 separately owns default exclusion from runtime packaging, public
API, ordinary CML generation, documentation publication, and CAR/SAR artifacts.

## One carrier and explicit versions

One `manifest.yaml` names the allocated package UUID, owning project, schema,
positive carrier revision, descriptive lifecycle state, and complete inventory.
The stable PackageReference deliberately omits carrier revision. The
single-writer producer advances carrier and artifact versions when their
control meaning changes. A standalone reader checks declared versions rather
than proving history or undeclared content changes.

Strict JSON under the YAML filename keeps a closed, unambiguous shape.
Duplicate keys, YAML-only syntax, missing/extra fields, unsupported schema, and
wrong types remain errors. Deterministic writer keys/LF help reproducibility;
reader key order and harmless whitespace do not establish control identity.

## Inventory and dependency ownership

Every non-manifest file has one unique ID/path, positive artifact revision,
closed role, required flag, and exact typed dependencies. Paths stay within
the root and resolve to regular files; symlinks and unlisted files reject.
Absent optional entries remain explicit and cannot satisfy present consumers.

Dependencies name artifact ID, revision, and role, within the admitted package.
They are acyclic and precede consumers in deterministic topological order, with
artifact ID as tie-break. This ordering is declared graph structure rather
than an identity recovered from enumeration or serialized object keys.

| Package concern | Closed role |
| --- | --- |
| Derived continuation cursor | `resume` |
| Scenario, model-context, glossary/BoK, or CML-baseline evidence | `source-snapshot` |
| Settled choice | `decision` |
| Unresolved question | `open-issue` |
| Selected semantic mapping | `realization` |
| Continuity, candidate CML, or semantic diff | `projection` |
| Attributable human decision | `approval` |
| Recorded checking/review evidence | `validation` |

No role's presence/multiplicity implies semantic readiness. Source snapshots,
realization/continuity, decision/issue, candidate/diff, review/approval,
rehydration, drift/CML gating, policy, and end-to-end proof retain their
separately defined responsibilities.

## Payload, subject, and compatibility boundaries

Payload bytes remain available to their content decoders and semantic validators.
Explicit artifact versions replace management hashes; they provide no checksum
integrity or authentication claim. The local single-writer model adds no locks,
retries, backups, rollback, or content-derived substitute tokens.

Review binds an explicit semantic subject, not the complete carrier or a
historical manifest. Later cursor/approval controls can advance carrier revision
without changing an unchanged subject. A semantic input change requires explicit
new subject version and review; it cannot be hidden through a subset.

There is no schema `1.0` reader, adapter, migration, default version, or fallback
for this target. Existing implementation and specifications are pre-redesign
evidence, with coordinated migration still required. The documentation creates
no parser, validator, test PASS, CML permission, Step acceptance, or Phase closure.
