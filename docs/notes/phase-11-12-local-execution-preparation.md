# Phase 11 and Phase 12 local execution preparation

Status: planning guidance; implementation not started
Date: 2026-10-03
Repository: textus-cbd-support

## Imported planning and current position

The user requested local incorporation, checking and execution preparation of
the GitHub Phase 11/12 refinements. Clean `main` advanced from `0931403` to
`a3c3de2` by fast-forward: `65196ec`, `b9972fc`, `0c78e17`, and `a3c3de2` change
only the two plans and their checklists. Both original histories are retained.

The [Phase 10.5 checklist](../phase/phase-10.5-checklist.md) is CLOSED.
[10.6](../phase/phase-10.6-checklist.md) and
[10.7](../phase/phase-10.7-checklist.md) are OPEN; finish their accepted handoffs
before Phase 11 implementation. Phase 12 consumes Phase 11 plus the specific
Actor Goal/Event Storming follow-up inputs. The refinement does not start or
close any Phase. Checklist ledgers own progress even where older design/spec
frontmatter still describes a pre-acceptance authoring state.

## Existing integration points

Paths below are verified existing code/contract entry points, not a claim that
the new Capability, Slice or ticket contracts are already implemented.

| Planned work | Existing owner / inspection entry | Preparation outcome |
| --- | --- | --- |
| Capability acquisition and current/history state | [CatalogRuntime](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CatalogRuntime.scala), [ComponentKnowledgeProjection](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/ComponentKnowledgeProjection.scala) | Admit a versioned upstream projection adapter while preserving source authority and availability. |
| Capability presentation and Web/MCP semantics | [ComponentDashboardProjection](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/ComponentDashboardProjection.scala), [ComponentDashboardWebEntry](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/ComponentDashboardWebEntry.scala), [service CML](../../src/main/cozy/textus-cbd-support.cml) | Share one typed query result; freeze and generate public operations through existing project tooling. |
| Candidate edits and cross-view trace | [CCDM contract](../spec/canonical-component-design-model-contract.md), [CandidateDesignSemanticDiffIntegration](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/CandidateDesignSemanticDiffIntegration.scala) | Preserve source/relationship identity and canonical/enrichment ownership; define Slice/trace extensions before authoring. |
| Durable candidate and review state | [typed control](../spec/internal-model-typed-control-contract.md), [continuation](../spec/internal-model-continuation-and-rehydration-contract.md) | Version the required payload/codecs and admit every new consumed dependency into the typed subject; verify fresh-process reconstruction. |
| Promotion | [CML change contract](../spec/internal-model-cml-change-contract.md), [InternalModelCmlChangeApplication](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangeApplication.scala), [post-validation](../../src/main/scala/org/simplemodeling/textus/cbdsupport/runtime/InternalModelCmlChangePostValidation.scala) | Compose actual-human approval, owned application and actual re-projection through an admitted consuming adapter; a test probe is not a deployed UI/MCP route. |

`CarReviewCapabilityCatalog` defines CBD review-quality capabilities. Its
`ReviewCapabilityId` values are not CNCF Component Capability identities.
The [composition CML](../../src/main/cml/usecase/application-component-composition.cml)
also cannot substitute for an admitted upstream demand projection.

## Inputs to resolve at execution entry

- CAP-11-01: record the real CNCF/Cozy projection contracts, producer revisions,
  schema versions and fixtures. The original development item names CNCF Phase
  78 and Cozy Phase 64, but this preparation has not verified their current
  implementation or a real Capability/demand fixture. A project dependency
  version alone does not establish this capability.
- Current local build declarations are Cozy `0.3.3-SNAPSHOT`, CNCF
  `0.5.3-SNAPSHOT`, and sbt-cozy `0.1.20-SNAPSHOT`. Recheck at execution entry;
  this task changes none of them and performs no publication or regeneration.
- Phase 12's initial scenario consumes FU-01 (Actor Goal), FU-03 (cross-model
  Event Storming) and their applicable FU-07 admission/validation evidence in
  [projection follow-up](../phase/phase-9-projection-followup.md). Select other
  follow-up inputs only for the views that actually require them.
- EDIT-12-01: establish the canonical owner and supported projection of Slice
  and trace relationships. The existing CML application boundary accepts exact
  existing targets; it does not by itself admit new syntax, new source-file
  creation, or external ticket mutation. Missing producer support remains an
  explicit dependency.
- Use current workflow/model selection when a bounded unit starts. The older
  Phase 10 Terra recommendations are historical planning evidence, not a fixed
  execution profile for Phase 11 or 12.

## Review adjustments reflected in the plans

Phase 11 now separates unavailable upstream data from admitted empty results
and scopes no-provider diagnostics to the queried catalog. Individual trace
edges retain their provenance so multiple demand/provider/realization paths
cannot generate false joins. Its real Component Capability fixture remains a
closure requirement; optional demand availability and the actual branch tested
are recorded explicitly rather than claiming unperformed positive integration.

Phase 12's graph now shows Slice-to-Specification and Slice-to-Ticket edges
independently. It distinguishes unresolved external evidence from proven
dangling references, and requires a declared policy for blocking findings.
New semantic state must be admitted into the existing durable/approval model,
with unsupported canonical projections kept explicit. Both plans retain the
[repository no-management-hash rule](../rules/repository-rules.md).

These are preparation decisions for design/specification promotion at the named
work boundaries, not new implemented contracts. Keep Given/When/Then and
property-based executable specifications for cardinality, reference resolution,
round trips, stale-state rejection, cross-surface consistency and fresh-process
continuation. Real producer integration and applicable Phase closure validation
remain required. This documentation adjustment needs static link, checklist,
scope and whitespace checks; it does not supply new product test evidence.
