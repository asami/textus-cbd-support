# AI-generated business application lint candidates

**Date:** 2026-10-03
**Status:** exploratory / journal
**Scope:** CBD Support lint candidates derived from observed Codex implementation behavior

## Context

An observed implementation replaced a prohibited hash-based comparison with a
more expensive mechanism: complete artifacts were copied, encoded (including
base64 / integer-array representations), stored in a ledger, propagated into
review handoffs, read again, and compared.

The important observation is not the individual use of hash, copy, base64, or
diff. Treating each technique as a separate prohibition invites equivalent
workarounds. The working assumption for CBD Support is therefore:

- do not attempt to eliminate strongly recurring Codex implementation patterns
  by growing an AI prohibition list;
- detect structures that are unnatural or unjustified for the target
  application model;
- treat model-unjustified mechanisms as lint/review candidates;
- keep the reason for a mechanism explicit so legitimate infrastructure,
  security, serialization, or release-integrity uses are not rejected merely
  by syntax.

This is especially important for business applications, where authoritative
domain state normally exists and should not be reconstructed indirectly from
copies of artifacts.

## Candidate lint family: shadow state management

Flag implementation mechanisms that reconstruct, duplicate, or infer
application state outside the declared domain/workflow model.

Candidate detections:

1. **Artifact-derived state**
   - Application/business decisions are made by comparing serialized files,
     generated artifacts, rendered output, or other materialized products
     instead of consulting authoritative domain state.

2. **Snapshot-as-state**
   - A complete object/file/result is retained only so a later execution can
     infer whether something changed.
   - Especially suspicious when no corresponding snapshot/version concept
     exists in the domain, use-case, workflow, or persistence model.

3. **Duplicate authoritative state**
   - A private ledger/cache/copy mirrors state already represented by an
     Entity, Aggregate, View, workflow state, revision, event, or repository
     mechanism.
   - The duplicate introduces its own consistency/equality protocol.

4. **Private change-detection protocol**
   - Hash/checksum/fingerprint/digest or whole-content equality is introduced
     as an application-level substitute for explicit revision/state/event
     semantics.
   - Detection should focus on purpose, not merely the presence of a hash API.

5. **Diff-derived domain semantics**
   - A business state transition or eligibility decision is inferred from a
     textual/binary diff rather than represented as an explicit state or
     operation result.

6. **Model-unjustified state machinery**
   - New version markers, caches, ledgers, snapshots, fallback state, retry
     state, duplicate lifecycle flags, or reconciliation machinery appear
     without a traceable requirement/model concept.

General review question:

> What model concept requires this state, and why can the authoritative model
> not answer the same question directly?

Absence of a convincing answer is a strong lint/review signal.

## Candidate lint family: AI harness cost amplification

The same generated patterns become particularly harmful inside Skills,
workflows, review harnesses, and AI handoffs because redundant data can be fed
back into later model calls.

Candidate detections:

1. **Whole-source duplication**
   - Complete source files/artifacts are copied into a secondary ledger,
     receipt, review record, or handoff when the original remains directly
     retrievable.

2. **Context-expanding encoding**
   - Text/source is transformed to base64, integer arrays, escaped blobs, or
     verbose JSON solely for preservation/comparison and is later exposed to an
     AI model.
   - This can consume more model context than reading the original source.

3. **Retrievable-data handoff**
   - A handoff embeds complete content that the next step can retrieve by
     stable identity/path/reference.
   - Prefer identity + required semantic result; retrieve source only when
     needed.

4. **Repeated full reads**
   - The same complete file/artifact is repeatedly read, transformed, stored,
     reread, and compared within one logical review/verification flow.

5. **AI-mediated deterministic comparison**
   - Large content is sent to an AI model to answer an equality/integrity
     question that deterministic code or an existing repository/model
     mechanism can answer.

6. **Redundant review payload propagation**
   - Review evidence recursively carries previous complete evidence/material
     rather than a bounded result, reference, or decision.

7. **Self-created validation/retry loop**
   - An unnecessary private consistency mechanism creates failures that trigger
     additional AI investigation, retry, explanation, or review calls.

These should initially be warnings/review findings rather than unconditional
errors. Some forms are legitimate at serialization, transport, security,
release-integrity, backup, or infrastructure boundaries.

## Candidate lint family: speculative defensive machinery

Observed AI-generated code often adds mechanisms that are plausible in generic
software but unsupported by the business/application model.

Candidates include:

- fallback behavior without a specified failure/consequence;
- retries without a declared transient failure model;
- defensive caches without a performance requirement;
- duplicate consistency checks where the storage/model already owns
  consistency;
- synthesized lifecycle/version state;
- recovery/reconciliation logic without a corresponding use case;
- "just in case" preservation of complete inputs/outputs.

The lint should ask for a model/requirement justification rather than maintain
an ever-growing blacklist of implementation techniques.

## Model-to-code lint principle

A stronger long-term CBD Support rule is:

> Significant application mechanisms should be traceable to the application
> model, use case, domain model, workflow, failure model, non-functional
> requirement, or an explicitly recognized infrastructure boundary.

This makes the lint useful beyond Codex. Codex-specific recurring patterns can
supply high-value heuristics, but the finding should be expressed as an
application-design problem rather than as "Codex used technique X."

Potential severity:

- **INFO:** suspicious technique detected; context needed.
- **WARN:** no obvious model justification / duplicated mechanism.
- **ERROR:** business behavior depends on shadow state that conflicts with or
  bypasses the authoritative model.

## Cost evidence to collect

Do not claim a cost increase rate without measurement. To make these lint
findings empirically useful, correlate findings with available execution
metrics:

- input tokens;
- cached input tokens;
- output tokens;
- number of AI calls;
- retry/review call count;
- handoff/prompt payload size;
- tool I/O bytes where available;
- repeated reads of the same resource.

This would allow CBD Support to show not only a structural smell but, where
evidence exists, its observed AI execution cost.

## Follow-up

This journal is a candidate inventory, not yet a normative lint specification.
Before promotion:

1. map candidates to existing CBD Support review/lint architecture;
2. separate business-application rules from AI-harness/Skill cost rules;
3. define suppressions/recognized boundaries for legitimate infrastructure and
   security cases;
4. create fixtures from real observed examples, including the
   hash-to-whole-copy comparison workaround;
5. prefer semantic/model-aware checks over token-level forbidden-word rules.
