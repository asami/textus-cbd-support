# ScalaDoc AI review strategy

Date: 2026-10-16

CNCF Phase 112 and Cozy Phase 86 establish the runtime/distribution and producer sides of semantic ScalaDoc. CBD Support needs only the review/AI-consumption side, so a separate bounded Phase 17 is planned rather than mixing this concern into the security-focused Phase 16 or CAR-lint disposition Phase 13.

ScalaDoc is treated as semantic evidence, not a documentation-coverage KPI. Review should use it to understand non-obvious Component contracts and identify materially stale or misleading documentation. It should not require comments on every method.

Generated ScalaDoc remains subordinate to CML authority. Hand-written Component ScalaDoc is maintained with source. For dependency understanding, AI should normally descend from Component Knowledge/Help to ScalaDoc and only then to SourceCode when necessary.
