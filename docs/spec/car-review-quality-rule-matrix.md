# CAR Review Quality Rule Matrix v1

status=stable
phase=Phase 8 P8-50
updated_at=2026-07-24

## Purpose

Every capability in `CarReviewCapabilityCatalog` has exactly one stable
provider-neutral check row in `CarReviewQualityRuleMatrix`. A row makes a
check addressable without pretending that it has already executed.

## Generated row contract

For a capability `quality.example`, the row has:

| Field | Rule |
| --- | --- |
| Check ID | `cbd.car-review.quality.example` |
| Applicability | `applicable` when the profile requests that capability |
| Required Evidence | The capability catalog's sorted representative Evidence kinds |
| Authority | `deterministic` without runtime Evidence; `runtime` when runtime Evidence is required; explicitly marked `advisory` for AI content analysis |
| Missing Evidence | An `unknown` Observation and `cbd.car-review.quality.example.evidence-unavailable` limitation |
| Evidence-backed maturity | `partial` for deterministic evidence, `operational` for admitted runtime evidence, and `unassessed` for advisory content pending corroboration |

The rule matrix is a total mapping over the catalog: adding a capability
without a row is impossible because rows are derived from the catalog and the
executable specification checks identity, sorting, Evidence, authority,
Unknown, limitation, and maturity semantics.

## AI View

MCP and Skill are separate AI-operability checks:

- `cbd.car-review.quality.ai.operability.mcp`
- `cbd.car-review.quality.ai.operability.skill`

Each also has an advisory content rule:

- `cbd.car-review.quality.ai.operability.mcp.content`
- `cbd.car-review.quality.ai.operability.skill.content`

Textus-provided MCP and standard Skills may satisfy their respective support
Evidence automatically. Their published descriptions, permitted operations,
authority boundaries, prerequisites, limitations, and recovery guidance remain
the subject of the advisory content rules. Automatic installation never turns
inadequate content into an Assurance or a passing gate; an advisory conclusion
requires deterministic or human corroboration before it can change maturity or
gate state.

P8-52 supplies the deterministic provider boundary for framework support and
metadata completeness. `TextusAiSurfaceCarReviewProviderRunner` emits separate,
attributed MCP and Skill support Evidence only from supplied compatible-runtime,
projection-policy, and standard-Skill metadata. A component-published surface
with missing required version, digest, summary, authority, limitation, or
operation metadata is a deterministic Finding. Structurally complete content
remains `unknown`: advisory AI or human review must establish semantic
usefulness and safety. The provider retains no endpoint, credential, raw
content, or invocation history in a Report.

## Boundary

P8-50 defines the common rule rows. P8-52 implements the initial Textus AI View
provider and P8-53 the initial Cost View provider; P8-51 and P8-54 remain
responsible for the other deterministic, runtime, and advisory providers. A
provider must emit attributed Evidence and canonical Observations through the
normal provider-bundle boundary; it must not update a Report directly.

P8-53 implements the initial Cost View provider and projection for Static Web
App and Gemma + MCP optimization. It retains expected reduction separately from
measured reduction, and requires a normalized unit plus comparison period
before a measured reduction becomes an Assurance. Missing measurement is
Unknown; a supplied measured claim lacking either comparison condition is a
deterministic Finding. The Cost View projects only canonical Evidence and
Observations, including quality constraints and operational trade-offs.

P8-51 implements the initial fixed static-quality provider. Its scope is
authorization, bounded domain text, domain identity, documentation rationale,
resilience contract, executable-test contract, evaluation corpus, structured
logging schema, and Web/CLI/Skill/cross-surface UX contracts. The provider
requires one bounded static source digest. A reported pass is an Assurance, a
reported failure a deterministic Finding, and absent static evidence an explicit
Unknown; callers cannot supply a free-form rule or capability mapping.

P8-54 applies an explicit authority policy before and after provider execution.
Every quality provider has a finite cost preflight. Deterministic providers
cannot submit advisory or runtime Evidence; Runtime Assurance requires declared
`runtime-observation` Evidence; Advisory providers can emit only
`ai.advisory.*` Finding/Unknown observations. Bundle Evidence facts reject raw
payload and secret-bearing keys, and Evidence kinds must be declared in the
provider descriptor. A violation is an attributable provider refusal and never
an implicit fallback or passing result.

P8-55 makes quality coverage total without pretending every provider has run.
`CarReviewQualityCoverageProjection` has exactly one base-rule entry for every
catalog capability: it links canonical Evidence/Observation IDs when present,
or emits the matrix's explicit retryable Unknown limitation when no admitted
check/provider exists. The projection is read-only and cannot change gate or
maturity.
