---
status: working
decision_scope: P10-33A
updated_at: 2026-09-29
---

# Internal-model Candidate Human Approval Design

P10-33A adds a narrow persistence boundary after P10-32: a caller-admitted,
closed record retains one attributable human decision about one exact complete
candidate/review basis. The normative grammar is in the [human approval
contract](../spec/internal-model-candidate-human-approval-contract.md).

The package validator captures the carrier once and composes the exact selected
review with the exact selected approval. The review dependency reaches all
present historical basis artifacts through the P10-32 binding. A pure validator
then reuses that captured review handoff, validates independent rule/provider
evidence and independent human input, and compares every persisted field
exactly. The result retains both external approval metadata and the complete
P10-32 admission context.

Three authorities remain separate. P10-32 records provider/rule review
evidence, not a human decision. Actor/provenance record attributable claims,
not authentication. The historical reviewed package identifies the reviewed
subject, while the newer carrier only transports the selected artifacts.

This boundary deliberately does not choose an approval, authenticate a human,
fetch provenance, read a provider, infer latest/currentness, parse or apply
CML, or write anything. P10-34 exclusively owns lifecycle applicability,
supersession, and invalidation; later work owns any actual CML authority.
