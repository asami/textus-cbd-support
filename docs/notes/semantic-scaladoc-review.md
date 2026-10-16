# Semantic ScalaDoc Review

Date: 2026-10-16
Status: planning note
Owner: Phase 17

ScalaDoc is useful review evidence because it can preserve intent that cannot be reconstructed reliably from implementation syntax. This is especially valuable for AI-assisted development.

Review should prefer semantic value over coverage. Important evidence includes role, contract, constraints, lifecycle, relationships and non-obvious usage assumptions. Missing comments on trivial/private helpers are not findings merely because they are missing.

For generated Scala, CML is authoritative and ScalaDoc is a projection. A semantic defect should normally be repaired in CML or the generator. For hand-written Component implementation, meaningful ScalaDoc is maintained with the source.

When inspecting dependencies, prefer structured Component Knowledge/Help and Documentation Subcomponent ScalaDoc before requesting SourceCode. This supports progressive disclosure and reduces unnecessary source analysis.
