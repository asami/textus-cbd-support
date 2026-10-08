# Semantic Message Flow review projection

Date: 2026-10-09

Semantic Message Flow will be used by cbd-support as a review projection rather than owned as a separate diagram specification.

Cozy/CML owns the normative semantic model and reference notation. cbd-support consumes structured Message Flow data derived from SAR/CAR and uses it for architecture, responsibility, data-flow, integration, and AI/IoC reviews.

A particularly useful target is SAR-vs-CAR comparison: expected message relationships from SAR can be compared with observed relationships from CAR. Differences become review findings/questions rather than automatic errors.

The same model can generate a deterministic review diagram for human intuition and machine-readable facts for AI review. AI infographic generation is downstream decoration only and must preserve the deterministic relationship semantics.

This direction fits the existing Human-in-the-Loop review policy: AI identifies suspicious structures and presents them visually; a human confirms or dispositions the finding.

See `docs/notes/semantic-message-flow-review.md`.
