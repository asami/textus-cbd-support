# CAR Review AI View: Component MCP and Skill Availability — 2026-07-21

## Context

CAR Review's AI View should answer whether a reviewed component is ready for
AI-agent and AI-assisted developer consumption. The relevant question is not
whether an AI Review provider called an MCP tool or executed a Skill while
creating a report. It is whether the component uses a compatible Textus
environment and can therefore use the framework-provided MCP and standard
Skill paths.

## Decision

Add `MCP` and `Skill` as first-class AI View sections.

`MCP` is **○ Supported** by default when the reviewed component has an admitted
compatible Textus runtime and effective MCP projection policy. Component tool
declarations refine the displayed tool detail but are not a prerequisite for
the framework support result. The view retains only safe runtime/policy and
service/operation/tool identities; endpoints, credentials, raw requests, raw
results, and general invocation history are excluded.

`Skill` is **○ Supported** by default when the component's compatible Textus
environment admits the standard Skill set. A component-specific Skill adds
domain guidance but is not required for baseline support. The view retains
standard skill-set identity/version and compatibility; a development `cncf-*`
Skill and later published `text-*` counterpart remain separate evidence.

## Content Review

Baseline framework support does not establish that a provided MCP surface or
Skill is useful or safe for the component's intended AI consumption. AI View
therefore reviews their content as normal CAR Review material.

MCP review covers task coverage, operation granularity, command/query/action
boundary, typed input/output descriptions, error/limitation contract,
authorization/readiness policy, versioning, and bounded agent usability. Skill
review covers component purpose, compatible versions and prerequisites, MCP
selection, authority boundaries, workflow/diagnostic usefulness, and explicit
limitations. A present but stale, unsafe, over-broad, or mismatched MCP/Skill
can therefore create a Finding even though the framework support row is **○**.

Deterministic metadata/schema/package checks provide the initial evidence.
Semantic usefulness may add advisory AI evidence, but an AI-only conclusion
cannot establish final Assurance or alter a deterministic Finding.

## Evidence and Maturity

The view distinguishes `Absent`, `Supported`, `Verified`, and `Operational`.
`Supported` comes from compatible Textus/CNCF metadata and effective policy.
Verification requires a deterministic package/projection or installation
result. Operational status requires accepted live runtime evidence. An
installed but unrelated Skill, an available CBD Support endpoint, or
documentation mentioning MCP does not strengthen the baseline result.

Missing, disabled, incompatible, or unverified evidence remains an explicit
Unknown or limitation. The view does not grant deterministic Finding,
Assurance, suppression, or gate authority to an AI provider, MCP, or Skill.

## Consequences

- The future canonical report needs Textus MCP and standard Skill support
  Evidence/assessment values, separate from AI provider execution provenance.
- CNCF runtime compatibility and MCP policy, Cozy CAR packaging, and the
  future standard skill-bundle manifest/install evidence become attributable
  providers for the same component AI View.
- AI View rules must assess MCP/Skill content and contract fit separately from
  their Textus baseline support status.
- `cncf-cbd-catalog` is currently a manually developed development skill. It
  can be assessed as a component-consumption Skill only when a component
  explicitly declares its compatibility and the review has admitted that
  evidence.
- Existing v1 schemas and Phase 5 implementation remain unchanged until a
  promoted canonical contract and executable examples define these values.

## Documentation Result

`docs/notes/car-review-design-proposal.md` now frames AI View MCP and Skill as
component availability rather than AI execution provenance. This entry records
the decision and does not mark implementation complete.
