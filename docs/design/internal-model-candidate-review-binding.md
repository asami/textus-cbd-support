---
status: working
decision_scope: P10-32A
updated_at: 2026-09-29
---

# Internal-model Candidate Review Binding Design

The review binding is a narrow durable adapter between the accepted [P10-31
semantic-diff handoff](../spec/internal-model-semantic-diff-contract.md), the
[P10-30 candidate projection](../spec/internal-model-candidate-cml-projection-contract.md), and P10-33's separate human-approval work. Its normative behavior is fixed by the [Candidate Review Binding Contract](../spec/internal-model-candidate-review-binding-contract.md).

The adapter keeps three contexts deliberately separate:

1. the exact historical V1 manifest that was reviewed;
2. the current carrier package that contains the selected review artifact; and
3. a caller-admitted rules/providers execution basis.

The historical manifest is retained as raw canonical V1 bytes because a V1
package digest describes the entire inventory, including explicitly absent
optional artifacts. Replacing it with a selected subset or a new digest would
silently change the reviewed subject. Its raw-byte digest checks transport
fidelity, while the V1 package digest continues to identify the package
content; neither substitutes for the other.

The package validator captures the carrier exactly once. It validates the
historical manifest against this capture without reopening paths and permits
only the selected validation review artifact and inert approval-role additions
outside the reviewed inventory. This makes a carrier revision extension
inspectable without rebasing or treating later approval material as review
evidence. The review validator then reuses the captured P10-31 semantic-diff
admission and compares every retained candidate/continuity/realization/diff/
target/evidence value to the exact reviewed inventory.

Rules and providers are input independently from the record to avoid
self-authentication. They establish only the caller-admitted identity/version/
content-hash basis. This design neither executes nor authenticates providers,
retrieves source, parses CML, establishes live freshness, makes a review
conclusion, or grants human approval. P10-33 and P10-34 remain the exclusive
owners of approval and lifecycle/invalidation behavior.
