# Phase 8 Checklist: Review Delivery, CI/CD, and Quality Rule Execution

Status: in progress (Stages 8.1–8.4 complete; P8-30 through P8-33 and P8-40 complete; P8-41 is next in Stage 8.5)
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

- [x] `P8-20` Generate deterministic Markdown from the common report-document
  projection. Evidence: `CarReviewDeliveryArtifactRenderer` renders fixed
  common-document headings, tables, lists, IDs, and redaction/omission text
  without any provider, repository, clock, or filesystem work.
- [x] `P8-21` Generate accessible PDF from the same projection without
  modifying canonical report state. Evidence: the renderer produces a
  self-contained tagged PDF with title, language, searchable text, headings,
  and `Table → TR → TH/TD` structure; no external converter or host font is
  used.
- [x] `P8-22` Prove Markdown/PDF/Web/JSON identity, ordering, redaction, and
  omission consistency for representative reports. Evidence:
  `CarReviewDeliveryArtifactRendererSpec` retains the canonical Report ID,
  digest, gate, Unknown identity, common section order, and delivery-safe
  text; P8-10/12 Web and delivery-projection specs prove the same source model
  and redaction boundary.
- [x] `P8-23` Prove PDF generation is reproducible and reports renderer
  limitations explicitly. Evidence: repeated rendering asserts byte identity;
  unsupported printable characters become visible omission markers and the
  stable `pdf.unsupported-character` renderer limitation. `pdfinfo`, pypdf,
  and Poppler inspection confirm the representative tagged two-page PDF.

## P8-30 to P8-39: CI/CD

- [x] `P8-30` Define CI artifact paths, retention, report/attestation digest
  binding, and profile/exit-code contract. Evidence:
  `car-review-ci-artifact-contract.md` plus its manifest schema/example fix the
  attestation-digest attempt directory, seven artifact paths, byte digests,
  exact profile/gate identity, workspace retention, `pass=0`/`fail=2`/
  `unknown=3`, and no implicit publish/distribute/deploy behavior;
  `CarReviewCiArtifactContractSpec` proves the executable contract and schema
  path constraints.
- [x] `P8-31` Materialize JSON, Markdown, PDF, HTML, SARIF, and attestation
  artifacts through authorized sbt-cozy/CI integration. Implementation evidence:
  `CarReviewArtifactBundle` makes the private CBD response carry a
  Report-bound Markdown/PDF bundle without provider work, and
  `SbtReviewCiArtifactMaterializer` atomically writes canonical response,
  Report, attestation, Markdown, PDF, HTML, SARIF, and manifest under the
  attestation digest. `CarReviewArtifactBundleSpec` and
  `SbtReviewReportArtifactsSpec` verify the bound content and fixed attempt
  directory; `CBD_STANDALONE_SBT_COZY_REVIEW_PROBE=true
  scripts/check-cbd-standalone.sh` verifies a real loopback CBD HTTP exchange,
  `sbt-cozy` task materialization, and a failing CBD gate. Post-implementation
  clean re-review verified bounded output before temporary materialization,
  pre-decode PDF Base64 limits, multiline credential redaction, manifest
  limitations, and the generated CI task surface. Full validation passed:
  103 sbt-cozy tests and 246 CBD Support tests.
- [x] `P8-32` Prove pass/fail/unknown behavior, offline determinism, and
  attributable provider limitations in CI. Evidence:
  `SbtReviewCiGateSpec` admits only a complete manifest with exact
  pass=0/fail=2/unknown=3 pairs, fixed artifact paths, retention, identity,
  digest, and limitation shape; `SbtReviewCiPolicySpec` proves the standard-CI
  local provider boundary; `CarReviewArtifactBundleSpec` proves delivery-safe,
  attributable provider limitation propagation. The isolated loopback probe
  verifies real CBD/sbt-cozy materialization and an expected `fail (exit code
  2)` gate failure. Final validation passed: 105 sbt-cozy tests and 247 CBD
  Support tests; the independent re-review was clean after manifest-shape
  validation was added.
- [x] `P8-33` Prove review artifacts do not silently alter publish,
  distribution, or deployment tasks. Evidence:
  `SbtReviewPublicationBoundarySpec` limits the Review task surface to its
  explicit publish/distribution aliases and refuses deployment. The
  `project-yaml-car` scripted fixture makes `cozyReviewGate` fail, then proves
  ordinary release `publish`, ordinary `cozyDistribute`, and SNAPSHOT
  `publishLocal` keep their normal CAR paths. The focused specification,
  fixture, and 108 sbt-cozy tests passed; the independent re-review was clean.

## P8-40 to P8-49: Diagnosis persistence, reuse, and CAR evolution

- [x] `P8-40` Define database entities and immutable retention for CAR lineage,
  target digest, Review Run, Report, attestation, reuse key, and comparison.
  Evidence: `car-review-persistence-contract.md` and its machine-readable
  model define vendor-neutral entity keys, relationships, immutable snapshots,
  opaque reuse identity, comparison references, and append-only digest-safe
  retention events. `CarReviewPersistenceContractSpec` verifies entity/table/
  key/relation integrity and retention coverage for every retained entity.
  Focused validation and 249 CBD Support tests passed; independent re-review
  was clean.
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
