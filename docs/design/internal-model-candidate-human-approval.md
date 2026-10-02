---
status: working
decision_scope: P104-TYPED-APPROVAL-LIFECYCLE-001
updated_at: 2026-10-01
---

# Internal-model Candidate Human Approval Design

The normative [approval contract](../spec/internal-model-candidate-human-approval-contract.md)
defines the exact V2 schema. This boundary retains actual independent human
input about one complete candidate/review/diff basis through shared typed
references. Source and executable-spec migration are authored; validation,
independent review, integrated continuation conversion and acceptance remain pending.

The dependency direction is semantic subject -> review -> approval. One real
capture admits explicit full external review and approval references. Approval
has exactly the selected review reference as its dependency. Existing owners
compose source witnesses, realization, continuity, candidate, diff and target
evidence. The explicit complete review subject, not the later carrier or cursor
revision, defines what was reviewed.

The nine-field basis retains candidate artifact/logical references and model
identity, review artifact/logical references, complete subject, scope and diff
artifact/logical references. Logical approval reference is separate from external
artifact reference. Producer-owned versions are preserved, not content-derived
or defaulted. Shared review subject parsing prevents grammar divergence.

Independent rules/providers own execution evidence; actual independent human
input owns actor/decision/provenance/rationale/items; source owners own authority,
identity and available revision. Stored attributable claims are not authentication.
Provider state is not human input; artifact revision is not unknown source revision.

Strict ordinary JSON checks closed semantic grammar, not canonical bytes.
Pure `validateValue` and `validateAdmission` reject malformed/null graphs and
cross-binding metadata contradictions without simulated Verified captures,
control-byte caches, path rereads or repeated human authentication. They never
replace actual admission with independent input.

The retained record, full external approval reference, selected relative path
and actual review admission remain point-in-time historical evidence when
filesystem paths later change. Fresh admission, including copied-carrier admission,
is a separate act and never path/process-local identity.

This boundary does not select latest records, authenticate people, fetch
provenance, evaluate live sources, authorize/apply CML or mutate artifacts.
Lifecycle separately derives applicability and explicit supersession. Undeclared
payload changes under the same declared version remain the producer's obligation;
typed control does not claim content mutation detection.
