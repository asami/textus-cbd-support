# Phase 5 Checklist: CBD-Led CAR Review Platform

This checklist is the authoritative Phase 5 state ledger. Items are never
deleted; a checked item requires observable evidence. Supporting-repository
work remains part of Phase 5 only when it satisfies a CBD Support-owned Review
contract.

## Normative Contract

- [x] `P5-01` Stable design fixes CBD Support as the CAR Review product owner and Cozy, `sbt-cozy`, Textus AI, CNCF, SIE, catalog, runtime, and other AI integrations as attributable providers without competing canonical reports. Evidence: `docs/design/car-review-architecture.md`.
- [ ] `P5-02` Static specification fixes versioned Review Provider descriptor, request, evidence-bundle, capability, rule-selection, limitation, digest, and compatibility contracts with representative canonical JSON.
- [ ] `P5-03` Static specification fixes canonical Review Report, Review Run, Finding, Assurance, Unknown, location, severity, confidence, disposition, applicability, maturity, coverage, provider, baseline, and attestation contracts.
- [ ] `P5-04` Security specification fixes target admission, path/process/network bounds, authorization, credential resolution, redaction, AI input, MCP exposure, retention, and deterministic/offline CI behavior.

## Review Application Core

- [ ] `P5-10` CBD Support implements a canonical Review Report model and deterministic codec whose identity and digest are stable for equivalent normalized evidence.
- [ ] `P5-11` CBD Support implements Review Run command/query lifecycle through CNCF Job execution with authorized start, progress, cancellation, completion, failure, and limitation state.
- [ ] `P5-12` CBD Support admits provider bundles only after schema, capability, target, digest, and compatibility validation and preserves incompatible, unavailable, disabled, and failed providers as attributable Unknown or run failures.
- [ ] `P5-13` CBD Support reconciles Evidence and Observations without losing provider identity, fabricating Assurance, choosing an implicit source winner, or rerunning an already admitted provider bundle.
- [ ] `P5-14` Review Run persistence, retention, comparison, immutability, and baseline behavior preserve target/report attribution and reject stale or mismatched gate evidence.

## Provider Framework and Cozy

- [ ] `P5-20` CBD Support implements provider registration, capability discovery, bounded invocation, timeout/cancellation, CallTree-safe observability, and limitation reporting through CNCF provider/driver boundaries.
- [ ] `P5-21` Cozy emits a generic `ReviewEvidenceBundle` for CAR/CML/model/build/package/ABI/documentation analysis with Cozy, rule-set, CML, CAR, and supported CNCF version identity.
- [ ] `P5-22` Cozy's existing CAR lint findings are preserved exactly through the provider adapter, and `cozy car lint` remains a focused independent command backed by the same rule results.
- [ ] `P5-23` CBD Support can invoke Cozy through the provider protocol for an admitted CAR while Cozy has no dependency on or call to CBD Support.
- [ ] `P5-24` Executable provider specifications prove compatible admission, incompatible refusal, explicit limitation, target-digest mismatch, cancellation, timeout, duplicate-bundle prevention, and provider-identity preservation.

## sbt-cozy CI/CD Bridge

- [ ] `P5-30` `sbt-cozy` emits attributable generation, compilation, test, dependency-resolution, CAR-build, and task-result evidence without implementing CBD quality assessment.
- [ ] `P5-31` `sbt-cozy` invokes Cozy locally, submits Cozy and sbt evidence bundles to the CBD Review Application, and receives one canonical report and gate result without granting a server arbitrary workspace access.
- [ ] `P5-32` Sbt Review tasks produce or obtain the canonical report, apply the CBD gate result, and materialize selected JSON, HTML, and SARIF projections with documented stable task and setting names.
- [ ] `P5-33` CI writes report, HTML, SARIF, and attestation artifacts under a stable target path, and attestation binds target digest, report digest, profile, providers, rule sets, and gate result.
- [ ] `P5-34` Standard CI runs deterministic providers offline with external and AI providers disabled unless explicitly configured, and secrets never enter task output or Review artifacts.
- [ ] `P5-35` Any connection from a successful Review attestation to publish, distribution, or deployment is explicit and opt-in; existing sbt publication tasks retain their default behavior.

## Web, CLI, Report, and MCP Surfaces

- [ ] `P5-40` The CBD Support CLI runs local and authorized server-backed Review through the same Review Application contract and returns stable run, report, gate, and exit behavior.
- [ ] `P5-41` The Web UI displays Review Run progress, target/provider state, limitations, and completed report overview without deriving conclusions outside the canonical report.
- [ ] `P5-42` CNCF, implementation, and quality views project shared Evidence and Observation identities and support cross-view navigation to provider and implementation locations.
- [ ] `P5-43` Text, canonical JSON, HTML, and SARIF projections are deterministic and consistent; SARIF remains an explicitly lossy location-bearing Finding projection.
- [ ] `P5-44` Authorized, bounded, redacted queries can retrieve Review Run, summary, report, Finding, and Assurance information through MCP without exposing sensitive evidence or arbitrary report history.
- [ ] `P5-45` Review start, cancellation, retention administration, filesystem access, external-provider enablement, and AI-cost-bearing operations remain private to MCP unless an explicit later policy admits them.

## Quality, AI, and Runtime Assessment

- [ ] `P5-50` Reusable capability definitions map one Evidence or Observation into multiple CNCF, implementation, and quality views without rerunning analysis.
- [ ] `P5-51` Applicability, maturity, coverage, confidence, provider attribution, strengths, gaps, and Unknown accounting are deterministic and do not collapse into an unexplained aggregate quality score.
- [ ] `P5-52` Security, Domain, Documentation, AI Readiness, Resilience, Testability, and Observability views have specified capabilities and executable representative assessments.
- [ ] `P5-53` CBD Support AI Review uses Textus AI's provider-neutral `AiRunner`, structured `generateRecord`, and Review purpose profiles for admitted Gemma/Ollama, OpenAI, or Google Gemini execution; it receives only bounded structured evidence, remains opt-in, and contains no provider wire API, credential, or provider-specific response parsing.
- [ ] `P5-54` Runtime evidence is attributable and bounded, static analysis cannot claim `Operational`, and accepted runtime evidence is required for Operational maturity.
- [ ] `P5-55` Integration admits the Textus AI Phase 1 contracts under development for normalized provider/model/mode/purpose/usage/limitation provenance, deterministic CAR Review fixtures, restrictive digest-safe CallTree/metadata, structured output failure, timeout/retry/cancellation behavior, and no implicit provider fallback; missing or incompatible contracts produce an attributable limitation or Unknown and AI output cannot override deterministic findings.

## Documentation, Verification, and Closure

- [ ] `P5-60` Every behavioral Phase 5 checklist item maps to an executable specification, scripted gate, or representative integration scenario with a machine-checked coverage record.
- [ ] `P5-61` Focused and full CBD Support tests pass for report, run, provider, persistence, security, CLI, Web, MCP, quality, AI, and runtime contracts that are implemented in Phase 5.
- [ ] `P5-62` Focused and full Cozy, `sbt-cozy`, and Textus AI tests pass for their admitted Phase 5 provider, lint-preservation, task, artifact, attestation, CI, structured-generation, execution-fact, confidentiality, deterministic-fixture, and lifecycle contracts.
- [ ] `P5-63` A representative local and CI workflow produces equivalent canonical reports for identical evidence and proves pass, Finding failure, provider Unknown, cancellation, timeout, stale attestation, and incompatible-provider behavior.
- [ ] `P5-64` CBD Support CAR build, CML lint, CAR lint, ABI check, Web packaging, CLI execution, authorized MCP report projection, and representative SAR runtime checks pass without publishing.
- [ ] `P5-65` README, user guide, CAR reference manual, provider/developer guide, strategy, phase ledger, stable design, and static specifications describe the implemented CBD-led Review workflow consistently.
- [ ] `P5-66` Final review has no actionable findings, all required repositories have validated commits with no unexplained Phase 5 dirty work, and every residual limitation or deferred item has an explicit relocation target.
