# Phase 8 Checklist: Review Delivery, CI/CD, and Quality Rule Execution

Status: in progress (Stages 8.1–8.2 complete)
phase=[Phase 8](phase-8.md)

## P8-01 to P8-09: Delivery and diagnosis contract

- [x] `P8-01` Define canonical dashboard, diagnosis, baseline/trend, and
  report-document identities without renderer-local conclusion logic. Evidence:
  `CarReviewDeliveryProjection` and
  `docs/spec/car-review-delivery-contract.md` define a deterministic,
  canonical-Report-only dashboard/document/diagnosis model.
- [x] `P8-02` Define authorization, redaction, pagination, retention, and
  missing-record behavior for dashboard and item diagnosis. Evidence:
  `car-review-delivery-contract.md` preserves exact-report authorization and
  history bounds, defines 1–100 cursor-bound pagination for future endpoints,
  and makes absent items explicit; `CarReviewDeliveryProjectionSpec` proves
  redaction and no synthetic fallback.
- [x] `P8-03` Define deterministic Markdown/PDF document structure, ordering,
  accessible PDF requirements, and omission/redaction representation. Evidence:
  `car-review-delivery-contract.md` defines the common ordered document model,
  Markdown/PDF renderer boundary, redaction-or-omission section, searchable
  PDF text, semantic headings/table headers, title/language, text labels, and
  non-colour-only severity/gate presentation.
- [x] `P8-04` Prove one report-document projection retains canonical
  Evidence/Observation/capability/provider/rule/location identities. Evidence:
  `CarReviewDeliveryProjectionSpec` proves deterministic dashboard and item
  diagnoses retain Report, Observation, Evidence, capability, provider, gate,
  baseline, and capability-derived location identities without report mutation.

## P8-10 to P8-19: Web dashboard and item diagnosis

- [x] `P8-10` Implement authorized Review dashboard projection and Web surface.
  Evidence: `CarReviewWebDeliveryApplication`, private
  `CbdReviewAdmin.getReviewDashboard`, authenticated `form.yaml`, and the
  generated/packaged ABI surface expose one exact retained Report dashboard.
- [x] `P8-11` Implement authorized Finding/Assurance/Unknown/capability item
  diagnosis with bounded next-action guidance and cross-view navigation.
  Evidence: `CarReviewWebDeliveryApplication.diagnosis` admits only exact
  `observation` or `capability` IDs, and its spec proves Finding, Assurance,
  Unknown, and capability diagnoses with deterministic guidance.
- [x] `P8-12` Prove Web presents Unknowns, limitations, dispositions, and
  baseline deltas without hiding or reclassifying them. Evidence:
  `CarReviewWebDeliveryApplicationSpec` retains canonical Unknown count,
  baseline identity, report limitations, and the Unknown deferred disposition;
  `CarReviewDeliveryProjectionSpec` proves the shared projection redacts only
  sensitive text without inventing conclusions.
- [x] `P8-13` Prove Web authorization and redaction match canonical report-read
  policy. Evidence: `CarReviewWebDeliveryApplicationSpec` proves denied,
  missing, unsupported, and absent requests fail without fallback, and the
  private service is not MCP-ready; `CarReviewDeliveryProjectionSpec` proves
  redaction and Report identity retention.

## P8-20 to P8-29: Markdown and PDF artifacts

- [ ] `P8-20` Generate deterministic Markdown from the common report-document
  projection.
- [ ] `P8-21` Generate accessible PDF from the same projection without
  modifying canonical report state.
- [ ] `P8-22` Prove Markdown/PDF/Web/JSON identity, ordering, redaction, and
  omission consistency for representative reports.
- [ ] `P8-23` Prove PDF generation is reproducible and reports renderer
  limitations explicitly.

## P8-30 to P8-39: CI/CD

- [ ] `P8-30` Define CI artifact paths, retention, report/attestation digest
  binding, and profile/exit-code contract.
- [ ] `P8-31` Materialize JSON, Markdown, PDF, HTML, SARIF, and attestation
  artifacts through authorized sbt-cozy/CI integration.
- [ ] `P8-32` Prove pass/fail/unknown behavior, offline determinism, and
  attributable provider limitations in CI.
- [ ] `P8-33` Prove review artifacts do not silently alter publish,
  distribution, or deployment tasks.

## P8-40 to P8-49: Diagnosis persistence, reuse, and CAR evolution

- [ ] `P8-40` Define database entities and immutable retention for CAR lineage,
  target digest, Review Run, Report, attestation, reuse key, and comparison.
- [ ] `P8-41` Define the exact diagnosis reuse key and invalidation inputs,
  including CAR digest, profile, rules, providers, runtime evidence, and
  policy.
- [ ] `P8-42` Reuse compatible completed Reports and coalesce concurrent
  identical diagnosis requests without rerunning providers.
- [ ] `P8-43` Preserve failed, cancelled, expired, incompatible, and
  policy-different Runs as attributable history without reusing them as
  successful diagnosis.
- [ ] `P8-44` Implement CAR evolution/baseline history projections for
  comparable version/digest/report/gate/capability/finding changes.
- [ ] `P8-45` Prove database authorization, retention, expiry, comparison, and
  MCP history bounds preserve attribution and deny arbitrary enumeration.

## P8-50 to P8-59: Quality-attribute rule execution

- [ ] `P8-50` Publish the quality-attribute rule matrix with concrete rule ID,
  applicability, Evidence, authority, outcome, limitation, and maturity
  semantics for every supported attribute.
- [ ] `P8-51` Implement deterministic checks for the initial Security, Domain,
  Documentation, Resilience, Testability, Evaluability, Observability, and UX
  attributes.
- [ ] `P8-52` Implement AI View checks for Textus MCP/standard Skill support
  and for component-provided MCP/Skill content appropriateness.
- [ ] `P8-53` Implement Cost View checks for Static Web App and Gemma plus MCP
  optimization evidence, estimates, measurements, and trade-offs.
- [ ] `P8-54` Add runtime/provider/advisory checks only with explicit
  authority, cost, redaction, and Unknown behavior.
- [ ] `P8-55` Prove every supported attribute has executable coverage or an
  explicit visible Unknown for the absent check/provider.

## P8-60: Human confirmation

- [ ] `P8-60` Present the complete cross-surface, persistence/history, and
  CI/CD result to a human
  reviewer. Stop the phase here until the reviewer explicitly accepts or
  returns it for revision.
