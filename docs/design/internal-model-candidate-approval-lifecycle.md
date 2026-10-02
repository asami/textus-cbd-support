---
status: working
decision_scope: P104-TYPED-APPROVAL-LIFECYCLE-001
updated_at: 2026-10-01
---

# Internal-model Candidate Approval Lifecycle Design

The normative [lifecycle contract](../spec/internal-model-candidate-approval-lifecycle-contract.md)
keeps four authorities separate: original actual human admission, independently
admitted current review, source-owner observations and explicit replacement.
Typed V2 lifecycle and executable migration are authored; validation, independent
review, integrated continuation conversion and acceptance remain pending.

An immutable original approval plus current review/independent basis, complete
original snapshots/caller observations and optional exact predecessor-to-successor
link produces one pure report. Superseded or Invalidated never rewrites history.
Every changed AND missing dimension remains visible to distinguish a new subject,
changed execution basis, changed source fact and still-unknown source revision.

Artifact, logical-record, subject and carrier revisions are different facts.
Full artifact ID/revision/role selects; candidate/approval logical references
control lineage; subject ID/revision/package/scope/full input set define review.
A newer carrier or inert approval/resume control transports unchanged subject
evidence without rebasing. No content token or whole-file equality establishes
these meanings.

The portable link names exact external Approval references; caller separately
actually admits the successor using independent human input. This adds no latest/
ranking/timestamp policy, storage topology or transitive history engine. Same
logical candidate revision requires equal full candidate reference; same approval
logical ID requires higher logical revision. Different logical IDs have no
numeric ranking. Link retires only its predecessor, granting no successor
currentness or CML permission.

Inventory correspondence preserves every original source full reference/path/
required/dependency/presence field, including optional absence. Source V2 grammar
remains mandatory. Existing source-owner freshness comparison maps unknown
baseline/observed revisions to SourceIncomplete with BOTH dimension vectors.
Ordinary payload is not a revision/drift token; undeclared content changes under
the same producer-owned version are not detected.

Pure structural helpers reject malformed/null and cross-binding-inconsistent
retained graphs without simulated Verified captures, control-byte caches,
path rereads or repeat human authentication. Actual admission alone checks
independent human input. Package lifecycle ASCII descriptive-token syntax stays
with the package owner; no new state enum is introduced.

All basis reasons precede source reasons sorted by kind and unsigned artifact ID.
Supersession wins state selection only, not evidence accumulation. Container
insertion order does not confer authority.

The evaluator follows no locators, reopens no paths, executes no providers and
consults no Git/clock/network. Link storage/selection, authentication,
reconciliation, continuation/action authority, actual fresh-process recovery
and canonical CML application belong to later separately admitted work.
