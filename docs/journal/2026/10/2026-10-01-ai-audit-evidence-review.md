# AI Audit evidence for CBD Support review

Date: 2026-10-01

Decision: CNCF Phase 97 AI Audit evidence is a future input to CBD Support's KPI-oriented human review. The authoritative interaction store remains CNCF/Service Bus; CBD Support projects and analyzes it.

The motivation is business AI lifecycle improvement: detect deviation/runaway behavior, observe human correction/retry/rejection/downstream failure, tune prompts/context/guards, compare agent/provider/model quality and cost, and identify candidates for deterministic Workflow/rule/program implementation.

This extends the existing CBD Support direction of presenting component-level evidence/KPIs to support human intuition and review; it does not make CBD Support the AI execution or audit authority.

The review projection should eventually support Agent x Provider/Model x Work Type views. Candidate KPIs include validation/test success, Candidate-Admission pass/reject, human correction/modification, retry/escalation, downstream failure, latency, usage/cost and cost per admitted result. Dot, OpenClaw, Codex and future agents should therefore be comparable on actual workflow outcomes rather than self-reported confidence.

These KPIs are decision support. CBD Support may expose trends and evidence to humans or routing-policy review, but does not autonomously change application/workflow routing authority.
