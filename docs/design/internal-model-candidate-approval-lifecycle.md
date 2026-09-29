---
status: working
decision_scope: P10-34A
updated_at: 2026-09-29
---

# Internal-model Candidate Approval Lifecycle Design

P10-34A introduces a narrow derived-evidence adapter after P10-33. The
[lifecycle contract](../spec/internal-model-candidate-approval-lifecycle-contract.md)
is normative. This design records why the original decision, historical review
subject, current review evidence, and explicit replacement evidence remain
separate.

```text
immutable original P10-33 admission
  + explicit current P10-32 admission / independent rule-provider basis
  + complete historical source snapshots / caller-owned observations
  + optional portable predecessor -> successor link
  -> pure applicability report
```

The original approval record remains historical evidence even if a valid link
derives `Superseded` or current evidence derives `Invalidated`. It is never
rewritten into a current record. The report preserves every mismatch because a
single selected reason would hide whether a future reevaluation needs new
candidate, review, package, rule/provider, or source evidence.

The link is deliberately content-only and portable. It names exact external
approval artifact tuples, while a caller separately admits the successor. That
avoids introducing a storage topology, a latest-record policy, a clock, or an
implicit transitive history. Its only effect is retiring the exact predecessor;
it cannot make the successor current or authorize application.

The evaluator treats the historical reviewed package—not a later carrier—as
the comparison subject. A carrier can transport approval artifacts without
rebasing the selected review. Full source-inventory equality closes the same
gap for source evidence: every original model, scenario, glossary, and CML
baseline remains visible, including explicit optional absence.

This remains a pure boundary. Source-owner observations are evidence supplied
to the existing freshness comparator; the evaluator never follows locators,
reopens package paths, reads CML, executes providers, consults Git, or mutates
CML. P10-34 does not decide actual approval/authentication, link storage or
selection, reconciliation, canonical CML permission, or application.
