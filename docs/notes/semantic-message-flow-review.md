# Semantic Message Flow as review projection

## Status

Planning note for cbd-support. The normative Semantic Message Flow specification belongs to Cozy/CML. cbd-support is a consumer of that model.

## Review role

Message Flow is a complementary architecture-review projection, not the primary model for dynamic behavior. Workflow and State Machine remain the deductive models for dynamic behavior.

cbd-support should consume structured Message Flow data derived from SAR/CAR/CML and use it to support Human-in-the-Loop review.

Primary review uses:

- architecture and responsibility review
- data-flow review
- integration/dependency review
- AI / Continuation-IoC control review
- SAR intended architecture vs CAR observed architecture comparison
- detection of unexplained or missing component relationships

Examples of useful review questions:

- Is the logical control direction consistent with component responsibility?
- Is a data flow present without an architectural reason?
- Does an AI-agent connection use normal control where Continuation/IoC is intended?
- Does CAR contain a relationship absent from SAR?
- Does SAR require a relationship not represented by CAR?
- Is a subsystem coupled to a resource outside its declared responsibility?

## Input contract

cbd-support should not infer semantics from a rendered image when structured data is available. It should consume Cozy's normalized Message Flow Model, including at least:

- source / target
- logical control direction
- data direction
- synchronous / asynchronous
- normal / continuation-IoC
- protocol/message metadata where available
- provenance (SAR, CAR, inferred, explicit)

## Review outputs

The same semantic model should support:

1. machine-readable review facts and findings,
2. deterministic review diagrams,
3. human confirmation / disposition workflows,
4. optional AI-produced infographic decoration.

The deterministic diagram is the semantic authority for visualization. AI decoration may add layout polish, grouping, headings, icons, explanatory text, and emphasis, but must not modify relationships or message-flow symbols.

## SAR / CAR comparison

Treat Message Flow as a useful review IR:

```text
SAR -> expected Message Flow
CAR -> observed Message Flow
          |
          v
      comparison
          |
          +-- findings
          +-- questions
          +-- review diagram
```

Do not assume every difference is an error. Findings should be surfaced for human judgement, consistent with cbd-support's Human-in-the-Loop review policy.

## Ownership boundary

- Cozy owns the normative metamodel/CML semantics and reference notation.
- cbd-support owns review projections, comparison, findings, review UX, and review-oriented rendering.
- The diagram notation itself is informative; review logic operates on structured semantics.

## Follow-up

Integrate this into the review roadmap after the current phase work rather than introducing an ad-hoc diagram-specific implementation. Reuse the existing Human-in-the-Loop review/disposition mechanisms.

## Cozy grammar v4 alignment

The Cozy-owned reference grammar is now fixed around ordinary Draw Editor primitives:

- one semantic relationship = one line;
- filled arrowhead = Control + Data;
- open arrowhead = Control only;
- hollow arrowhead = Data only;
- synchronous = solid line;
- asynchronous = dashed line;
- Continuation/IoC = circled `I` on the same solid or dashed relationship line;
- timing and IoC are orthogonal, so asynchronous IoC is valid;
- color is supplementary only;
- arrowheads do not overlap component shapes.

cbd-support renderers and review views must consume these semantics from structured Message Flow data rather than infer them from SVG/image appearance. Generated review/infographic views must not introduce new flow symbols.
