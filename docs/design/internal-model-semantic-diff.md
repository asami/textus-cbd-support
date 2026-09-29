---
status: draft
decision_scope: P10-31A
updated_at: 2026-09-29
---

# Internal-model Semantic-Diff Design

P10-31A adds one narrow durable boundary: a canonical projection serializes
the exact Phase 9 diff and patch-trace values already admitted with a selected
candidate CML projection. The [semantic-diff contract](../spec/internal-model-semantic-diff-contract.md)
defines grammar; this document records the stable responsibility split.

`InternalModelSemanticDiff` is content, `InternalModelSemanticDiffAdmission` is
the content plus captured external artifact and candidate-package context, and
`InternalModelSemanticDiffValidator` is the one-pass boundary. The package
validator captures inventory once, selects candidate and semantic-diff profile
families by exact headers, and does not change manifest role vocabulary.

The selected candidate remains the source of complete target membership,
mappings, baseline snapshots, proposed bytes, realization binding, and source
conditions. Diff entries and patch traces are reconstructed as direct Phase 9
typed values rather than converted to summaries or a parallel semantic model.
This preserves opaque identities, multiple conditions, limitations, and
attribution without choosing a winner.

The boundary deliberately admits traceability, not an approved or applicable
change. It neither interprets raw CML/Git bytes nor claims freshness, source
authority, review, approval, compatibility, source mutation, or rehydration.
Later review/approval/invalidation work owns those transitions.
