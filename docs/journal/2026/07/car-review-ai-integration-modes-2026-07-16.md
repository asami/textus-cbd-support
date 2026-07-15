# CAR Review AI Integration Modes — 2026-07-16

status=consideration
updated_at=2026-07-16
tag=textus-cbd-support, car-review, ai, codex, textus-ai, ollama, openai, gemini

## Context

CAR Review needs semantic AI assistance for questions that deterministic Cozy,
sbt, CNCF, catalog, and runtime providers cannot settle reliably. At the same
time, making OpenAI or Google Gemini API calls part of every Review Run would
introduce recurring cost, network dependence, credential handling, and
non-deterministic CI behavior.

The manual ChatGPT export/import workflow remains useful but is deferred. The
near-term design therefore considers four AI collaboration paths:

1. a Codex Skill that orchestrates CBD Support from an interactive Codex task;
2. a CBD Review Provider that invokes the local Codex CLI;
3. a Textus AI provider backed by local Gemma/Ollama; and
4. a Textus AI provider backed by a commercial OpenAI or Google Gemini API.

This entry records the direction agreed on 2026-07-16. It is a chronological,
non-normative record. Stable contracts still need promotion to `docs/design`
and `docs/spec` with executable examples.

The paired Textus AI runtime requirements are recorded in
`textus-ai/docs/journal/2026/07/2026-07-16-car-review-ai-runtime-requirements.md`.

## Confirmed Baseline

- CBD Support owns the Review Application, Review Run lifecycle, canonical
  Review Report, provider orchestration, cross-provider reconciliation, and
  gate policy.
- AI is optional and receives bounded structured Evidence rather than an
  unstructured repository dump.
- AI output cannot override a deterministic Finding or turn absence of a
  Finding into an Assurance.
- Standard CI remains deterministic and offline unless external providers are
  explicitly enabled.
- The local Codex CLI supports non-interactive `codex exec`, read-only sandbox
  execution, ephemeral sessions, and JSON-Schema-constrained final output.
- A local Codex CLI signed in through ChatGPT can use the developer's Codex
  account allowance instead of an OpenAI API key. API-key authentication uses
  API billing instead.
- Textus AI already provides a provider-neutral CNCF `AiRunner` with Gemma /
  Ollama, OpenAI, and Google Gemini runtime adapters.

## Direction

CAR Review should expose one AI Review contract with multiple execution modes.
The canonical Review model must not contain separate ChatGPT, Codex, Ollama,
OpenAI, or Gemini result types.

```text
ReviewProviderRequest
        |
        +--> Codex Skill / interactive orchestration
        +--> local Codex CLI Review Provider
        +--> Textus AI / Gemma-Ollama
        +--> Textus AI / OpenAI-Gemini
        |
        v
ReviewEvidenceBundle
        |
        v
CBD canonical Review Report
```

Each path must preserve provider identity, effective model and engine,
prompt-contract identity, input and output digests, limitations, and available
usage information. A provider result is attributable Evidence; it is not a
competing complete Review Report.

## Mode 1: Codex Skill

A reusable Codex Skill may orchestrate an interactive CAR Review. The Skill
can:

- call authorized CBD Support MCP report queries or the CBD CLI;
- obtain a bounded Review request and Evidence;
- inspect only the additional repository context authorized for the task;
- perform semantic review within the active Codex task; and
- submit or materialize a schema-valid `ReviewEvidenceBundle` for admission by
  CBD Support.

The Skill is an orchestration surface, not the canonical Review engine and not
an independent report owner. The AI execution is performed by the active
Codex task under its current account, model, sandbox, and approval settings.

This mode is human-initiated and is suitable for exploratory review, follow-up
questions, and developer-guided investigation. It must not silently acquire
authority to start cost-bearing providers or write Review state through a
read-only MCP query.

## Mode 2: Local Codex CLI Review Provider

CBD Support may invoke the local Codex CLI through an authorized CNCF
provider/driver boundary. The direction is the reverse of the Skill mode:

```text
CBD Review Job
  -> CodexReviewProvider
  -> authorized process driver
  -> codex exec
  -> schema-constrained result
  -> ReviewEvidenceBundle
```

The initial adapter should use an isolated review workspace containing only a
bounded AI Review Capsule rather than granting Codex an unrestricted repository
view. The invocation should be read-only and ephemeral and should constrain the
final response with the Review output JSON Schema.

The local provider belongs to the developer workflow. It may reuse the user's
saved ChatGPT-managed Codex authentication, in which case it consumes that
user's Codex allowance and credits. CBD Support must never copy, persist,
display, or transmit Codex authentication state. Authentication class may be
recorded as non-secret provenance, but credentials and account identifiers may
not enter the report or CallTree.

This mode is not the default for shared servers or standard CI. Login expiry,
usage limits, unavailable models, process refusal, timeout, or malformed output
must produce an attributable provider limitation or `Unknown`, without causing
deterministic Review results to disappear.

The first adapter should use one Codex agent. Multi-agent review is deferred
because every delegated agent consumes additional model and tool work. It may
later be enabled only for independently bounded review slices where the quality
gain justifies the additional usage.

## Mode 3: Local Gemma/Ollama through Textus AI

CBD Support may bind its AI review socket to Textus AI and select a local
Gemma/Ollama provider through a Review-specific purpose profile.

This is the preferred automated low-cost mode for:

- Evidence classification and prioritization;
- terminology mismatch candidates;
- deterministic Finding explanation;
- duplicate or related observation grouping;
- report summaries; and
- questions that should be escalated to a stronger model or a human.

Local inference avoids per-call API billing and can remain offline, but its
quality and latency depend on the installed model and local resources. A local
provider must fail explicitly when unavailable and must not silently fall back
to a commercial remote provider unless an operator has configured that exact
fallback.

## Mode 4: Commercial OpenAI or Google Gemini through Textus AI

Commercial providers remain available for high-value semantic review that
requires stronger models or unattended server execution. CBD Support should
use Textus AI's provider-neutral `AiRunner`; it should not contain provider wire
APIs, credentials, model aliases, or provider-specific response parsing.

Commercial execution must be explicitly enabled. Selection should be scoped by
Review purpose and policy, with bounded input, output, retries, concurrency,
and invocation count. A standard development, CI, or release profile must not
imply commercial AI execution merely by selecting that profile.

The first CAR Review AI purposes do not require web search or URL-context
tools. All relevant Evidence should be supplied by the Review Application. A
later rule that genuinely requires external knowledge must define separate
network, source, citation, and cost policy before enabling provider tools.

## Common AI Review Capsule

All automated modes should consume the same logical request. A transport may
serialize it as an AI Review Capsule containing:

- Review schema version, Review ID, and target digest;
- selected semantic rule IDs and rule-set version;
- prompt-contract ID and version;
- bounded Evidence with Evidence and subject identities;
- Evidence digest and redaction declaration;
- expected output JSON Schema;
- input, output, time, retry, and invocation limits; and
- requested provider capability without embedded credentials.

The result must retain:

- effective provider, mode, engine, and model when available;
- prompt-contract, input, raw-response, and normalized-output digests;
- provider response or request identity when safe;
- structured observations with source Evidence identities;
- confidence and explicit limitations;
- available token, retry, latency, and cost metadata; and
- enough version information to identify stale or incomparable results.

The Capsule is also the future basis of the deferred manual ChatGPT
export/import workflow, so manual and automated AI results do not require
separate canonical contracts.

## AI Authority and Report Semantics

AI review is initially advisory. It is intended for semantic interpretation,
not deterministic compatibility or release-gate authority.

Appropriate initial AI outputs include:

- candidate Finding;
- interpretation or explanation linked to existing Evidence;
- ambiguity or question requiring human confirmation;
- grouping or prioritization suggestion; and
- `Unknown` or limitation.

AI alone should not establish final Assurance. A proposed positive conclusion
must be supported by deterministic or accepted runtime Evidence, or explicitly
confirmed through a future human-review disposition workflow. AI output cannot
lower the severity of, suppress, or replace a deterministic Finding.

The first semantic rules should focus on:

- CML, implementation, and documentation consistency;
- domain terminology and BoK alignment;
- test-scenario adequacy candidates;
- documentation clarity and missing rationale; and
- developer-facing explanation of capability gaps.

Build success, CML validity, ABI compatibility, package correctness, test
execution, credential handling, and version compatibility remain owned by
deterministic providers.

## Cost and Execution Policy

AI cost is controlled before provider selection rather than inferred after the
call. The eventual policy needs to distinguish:

- no AI;
- interactive Codex account use;
- local inference;
- commercial API invocation; and
- explicit escalation from a lower-cost mode.

The cache identity should include at least target digest, Evidence digest,
semantic rule-set version, prompt-contract version, provider, model, and
relevant generation settings. An admitted result for the same identity should
be reused instead of silently invoking the provider again.

Budget, quota, or account-limit exhaustion produces `Unknown` or a provider
limitation. It does not turn an unassessed capability into success and does not
invalidate already admitted deterministic Evidence.

## Recommended Adoption Order

1. Fix the bounded AI request, response, provenance, redaction, and limitation
   contracts within the generic Review Provider protocol.
2. Implement the local Codex CLI provider as the first automated high-quality
   developer path.
3. Add the Codex Skill as the interactive orchestration and investigation
   surface over the same contract.
4. Add Textus AI Gemma/Ollama for low-cost automated triage and summarization.
5. Add OpenAI and Google Gemini only as explicitly enabled commercial
   escalation providers.
6. Defer manual ChatGPT export/import until the automated paths and common
   Capsule have stabilized.

Standard CI remains deterministic and offline throughout the initial slices.
AI results remain advisory until rule-specific acceptance evidence supports a
stronger gate policy.

## Ownership Boundary

- CBD Support owns AI Review rule selection, bounded Evidence construction,
  provider admission, canonical observations, report reconciliation, cache and
  cost policy, and user-facing Review behavior.
- A Codex Skill owns reusable interactive instructions and orchestration, not
  Review policy or report semantics.
- The CBD Codex adapter owns local agent-process invocation and translation to
  the generic provider bundle.
- Textus AI owns provider-neutral generation, structured record generation,
  provider selection, local and commercial adapters, and AI execution
  telemetry.
- CNCF owns the AI runner SPI, provider/driver execution boundaries,
  ExecutionContext, CallTree, cancellation, and structured-failure mechanisms.

## Promotion and Open Questions

Before implementation, this direction needs promotion into stable design and
specification covering:

- the exact AI Review Capsule and output Schema;
- Codex Skill and Codex CLI adapter authority boundaries;
- allowed account-authentication posture for local developer execution;
- AI result cache identity and invalidation;
- normalized provider/model/usage provenance;
- cancellation, timeout, retry, and partial-result behavior;
- the first advisory semantic rules and acceptance examples; and
- the criteria, if any, under which an AI observation may later participate in
  a CI or release gate.

## Phase 5 Plan Synchronization

The Phase 5 dashboard and checklist were synchronized with the active local
Textus AI development plan on 2026-07-16. Phase 5 now explicitly reuses the
released Textus AI baseline (`AiRunner`, `generateRecord`, purpose profiles,
and Gemma/Ollama, OpenAI, and Google Gemini adapters) and admits the following
Textus AI Phase 1 work only after its executable evidence is available:

- normalized execution facts for provider, model, mode, purpose, usage, and
  limitations;
- deterministic CAR Review provider fixtures and structured-output scenarios;
- restrictive digest-safe CallTree and response-metadata publication; and
- explicit structured failure, timeout, retry, cancellation, and no-implicit-
  fallback behavior.

This synchronization does not transfer Review ownership. CBD Support still
owns Evidence construction and redaction, prompt/output contracts, result
admission, cache and cost policy, canonical reporting, and gate decisions.
Unavailable or incompatible Textus AI contracts remain attributable
limitations or `Unknown`; they are not treated as successful assessment.

No implementation, configuration change, provider invocation, checklist
completion, or publication was performed as part of this consideration entry.
