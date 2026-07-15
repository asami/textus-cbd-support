# CAR Review Design Consideration — 2026-07-15

## Request

Use `car-review-spec-study-handoff.md` as the input for a CAR Review design
note, and preserve the investigation history and decisions in the journal.

## Evidence Reviewed

- `docs/journal/2026/07/car-review-spec-study-handoff.md` for the three-view
  concept, evidence model, quality capabilities, rule types, UI/CLI candidates,
  and open questions;
- Cozy `CozyCarLint`, CLI routing, and executable specifications for the current
  build/CML/documentation/ABI lint baseline;
- `docs/spec/mcp-ownership.md` for the CBD Support and SIE publication boundary;
- `docs/spec/component-reference-contract.md` for evidence-bearing handoff and
  non-synthesis rules; and
- the repository strategy for CBD Support's current read-only catalog and
  development-context responsibility.

The resulting exploratory proposal is recorded in
`docs/notes/car-review-design-proposal.md`.

## Confirmed Baseline

1. Cozy already owns the local CAR model, metadata, packaging, ABI, and lint
   entry points.
2. Current CAR lint returns a flat list of deterministic findings. It has no
   first-class Evidence, Assurance, Unknown, capability assessment, confidence,
   or cross-view identity.
3. CBD Support owns evidence-bearing component detail and usage retrieval; it
   does not own local source analysis.
4. SIE owns BoK and semantic terminology evidence and must not synthesize CAR
   implementation facts.
5. The handoff's CNCF, Implementation, and Quality Attribute views need one
   canonical analysis result. Separate per-view analyzers would duplicate rules
   and allow inconsistent conclusions.

## Options Considered

### Extend the existing lint finding directly

This would minimize the first code change, but a `WARN`/`FAIL` finding model
cannot faithfully distinguish positive assurance, non-applicability, and an
unassessed capability. Adding every concern to one finding record would also
couple analysis to the text and JSON renderers.

Decision: do not use the existing finding as the canonical Review model. Adapt
lint findings into the new model so the deterministic baseline is retained.

### Implement one analyzer for each view

This would make each renderer initially simple, but the same timeout or
authorization fact would be analyzed repeatedly. CNCF, implementation, and
quality results could then disagree or carry different evidence.

Decision: use one Evidence → Observation → Assessment pipeline and project it
into views only after analysis.

### Put the complete engine in CBD Support

CBD Support already serves AI-oriented CBD knowledge, but moving local CML,
Scala, configuration, test, and filesystem analysis there would violate its
current read-only retrieval boundary and duplicate Cozy's CAR understanding.

Decision: Cozy owns the local engine and CLI. CBD Support may later supply
explicit catalog evidence or expose attributable review information, but does
not own local analysis or exit policy.

### Make AI the primary reviewer

AI can interpret semantic consistency and adequacy, but unstructured repository
input is expensive, difficult to reproduce, and unsuitable as the sole basis
for release gates.

Decision: deterministic evidence is primary. AI is opt-in, receives bounded
structured evidence, records provider/model/input provenance, and cannot
override deterministic findings.

### Replace `car lint` immediately

Immediate replacement would mix the Review model decision with a CLI migration
decision and could disrupt current release checks.

Decision: retain `car lint` initially and make it share underlying rule results
with `car review` through an adapter. Deprecation remains a separate decision.

## Provisional Decisions

- Use `CAR Review` as the feature and command name in the proposal.
- Use `cozy car review` as the local entry point.
- Make canonical JSON the complete interchange representation.
- Treat text and HTML as views and SARIF as a location-bearing, findings-only
  projection.
- Model `Finding`, `Assurance`, and `Unknown` explicitly.
- Keep severity separate from confidence.
- Require runtime evidence before claiming `Operational` maturity.
- Calculate coverage only over explicit applicable subjects and preserve
  unknowns in the denominator/accounting record.
- Avoid a single aggregate quality score in the first version.
- Make review read-only and deny network access by default.
- Require AI, catalog, BoK, and runtime evidence integrations to be explicitly
  enabled and attributable.
- Start implementation with the common report plus an adapter for existing CAR
  lint, then add CNCF, implementation, quality, AI, and runtime slices.

These decisions remain non-normative until promoted from notes to design and
specification.

## Deferred Decisions

- external naming of the CNCF view;
- ownership and versioning of the CNCF feature vocabulary;
- capability-specific applicability, maturity, and coverage formulas;
- release-profile unknown and failure policy;
- suppression configuration format and lifecycle;
- trusted runtime-evidence format;
- built-in versus DSL-defined quality views;
- persistence and comparison of historical Review Reports; and
- AI execution cost and CI policy.

## Next Handoff

The next design step is to build representative canonical JSON examples from
current `car lint` output and use them to settle stable IDs, rule/version
metadata, projection determinism, redaction, profile exit policy, and the exact
Cozy/CNCF ownership boundary. No implementation, phase registration, or
publication was performed in this consideration step.
