# Phase 17: Semantic ScalaDoc Review Support

Status: PLANNED
Planned: 2026-10-16
Depends on: goldenport-cncf Phase 112, Cozy Phase 86
Related: CAR Review Architecture, Component Documentation/Help
Consumer: sm-workflow review route, AI-assisted Component development

## Goal

Use ScalaDoc as source-level semantic evidence during Component review and AI-assisted source analysis, without turning documentation coverage into a mechanical quality metric.

## Work

1. Consume ScalaDoc/source documentation through the existing Component documentation/resource path where available.
2. During source review, treat non-obvious role, contract, constraints, lifecycle, relationships and design intent in ScalaDoc as useful evidence alongside code and structured Component metadata.
3. Detect high-value documentation problems only where semantics justify them: important public Component/Service/Operation/extension boundaries with missing contract meaning, or ScalaDoc that is materially inconsistent with the implementation/model.
4. Do not require ScalaDoc for every method and do not optimize for comment percentage.
5. Distinguish generated ScalaDoc from hand-written ScalaDoc. For generated code, CML remains authoritative and fixes normally belong in the model/generator. For hand-written Component code, the source is the documentation authority.
6. Allow AI to propose ScalaDoc changes as part of an admitted implementation/review repair, but do not generate filler text solely to silence a finding.
7. Use Documentation Subcomponent/Help before SourceCode when reviewing an external dependency; descend to source only when the available documentation/metadata is insufficient.

## Acceptance

A representative review can use meaningful ScalaDoc to interpret a Component contract, report a materially stale/misleading contract comment, and leave an obvious implementation helper undocumented without a finding. Generated-code findings point back to the CML/generator authority rather than editing generated output.
