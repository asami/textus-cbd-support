# Phase 8 P8-52: Textus AI surface provider

status=completed
phase=Phase 8 P8-52
updated_at=2026-07-24

## Decision

CAR Review implements Textus-provided MCP and standard Skill support as a
CBD-owned deterministic provider, not as a hard-coded Report mutation and not
as evidence that an AI provider happened to invoke a tool. The provider accepts
only already admitted, bounded metadata: Textus-runtime compatibility, effective
MCP projection policy, standard Skill-set identity, and optional
component-published MCP/Skill descriptors. It emits the normal v1 descriptor,
request, Evidence bundle, and limitations through the existing provider
admission boundary.

## Result

MCP and Skill have distinct capability mappings and distinct support
Assurances. MCP support requires compatible Textus runtime plus effective
projection policy; standard Skill support requires compatible runtime plus an
admitted standard Skill set. A component-specific publication is not required
for framework support.

Content is separate from support. A supplied MCP/Skill publication missing its
identity/version/digest, summary, authority boundary, limitations, or operation
metadata produces a deterministic medium Finding. Structurally complete content
is explicit `unknown`, with an advisory/human semantic-review limitation. This
prevents automatic Textus support from asserting that a surface is safe, useful,
or gate-ready.

## Boundary hardening

Provider-bundle admission now validates optional Observation mappings. A mapped
quality capability must be a CBD catalog capability and must have been declared
by the provider descriptor. Reconciliation retains admitted mappings in the
canonical Observation instead of replacing them with empty mappings. Existing
v1 providers may omit mappings and remain compatible.

## Evidence

`TextusAiSurfaceCarReviewProviderRunnerSpec` proves the complete support/content
case and the incomplete-content Finding. `CarReviewProviderBundleAdmissionSpec`
proves undeclared mappings are refused; `CarReviewBundleReconcilerSpec` proves
admitted mappings survive reconciliation.
