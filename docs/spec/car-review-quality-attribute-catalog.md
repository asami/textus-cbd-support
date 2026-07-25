# CAR Review Quality Attribute Catalog

status=stable
updated_at=2026-07-25

## Contract

This catalog promotes the February 23, 2026 quality-attribute mind maps
into stable CAR Review capability identities. The repository catalog and
this specification, rather than the external XMind files, are normative.
Each item requires attributable Evidence and an explicit Observation; its
presence or declaration alone never establishes Assurance.

## Reference Basis

The catalog uses the following standards and public frameworks as vocabulary
and gap-analysis inputs. Their inclusion does not assert product certification
or replace profile-specific control mapping.

- ISO/IEC 25010:2023 product quality model.
- ISO/IEC 25012 data quality model.
- W3C Web Content Accessibility Guidelines (WCAG) 2.2.
- NIST Secure Software Development Framework (SSDF).
- NIST AI Risk Management Framework trustworthiness characteristics.
- Green Software Foundation Software Carbon Intensity specification.

## Runtime

- `quality.performance.latency`: Request and operation latency is bounded, measured, and attributable to an admitted workload.
- `quality.performance.throughput`: Sustainable throughput and saturation behavior are measured for an admitted workload.
- `quality.performance.concurrency`: Concurrent execution limits, isolation, and contention behavior are explicit and verified.
- `quality.performance.resource-efficiency`: CPU, memory, and I/O use is measured against useful work and declared limits.
- `quality.security.confidentiality`: Sensitive information is classified and protected in transit, at rest, in diagnostics, and across provider boundaries.
- `quality.security.integrity`: Data, artifacts, messages, and decisions retain verifiable integrity across storage and execution boundaries.
- `quality.availability.uptime`: Service uptime objectives, measurement windows, and admitted exclusions are explicit.
- `quality.availability.failover`: Failover behavior is bounded, tested, and preserves required identity and state semantics.
- `quality.availability.redundancy`: Redundancy removes declared single points of failure without creating hidden consistency hazards.
- `quality.availability.recovery-time`: Failure and repair evidence supports attributable MTBF and MTTR or equivalent recovery objectives.
- `quality.reliability.failure-rate`: Failure frequency is measured against a declared workload and observation window.
- `quality.reliability.correctness`: Runtime outcomes satisfy declared domain and protocol contracts under normal and failure conditions.
- `quality.reliability.data-consistency`: Data consistency boundaries, conflict behavior, and recovery semantics are explicit and verified.
- `quality.reliability.rest-request-idempotency`: REST mutation routes prevent duplicate execution through principal-scoped idempotency keys, normalized request fingerprints, in-progress coordination, bounded result replay, expiry, and conflicting-key rejection.
- `quality.reliability.web-form-submission-idempotency`: Web Form mutation routes prevent duplicate submission through principal/session-scoped one-time tokens, Post/Redirect/Get, bounded result or redirect replay, expiry, and conflicting-token rejection.
- `quality.scalability.scale-up`: Vertical scaling behavior and resource ceilings are measured and documented.
- `quality.scalability.scale-out`: Horizontal scaling preserves identity, state, ordering, and authorization semantics.
- `quality.scalability.elasticity`: Elastic capacity changes respond to bounded signals without instability or semantic degradation.
- `quality.resilience.retry`: Retry policy is bounded, attributable, idempotency-aware, and does not amplify failure.
- `quality.resilience.circuit-breaker`: Circuit breaking has explicit thresholds, states, recovery, and diagnostics.
- `quality.resilience.bulkhead`: Resource and failure isolation prevents one workload or dependency from exhausting unrelated work.
- `quality.resilience.self-healing`: Automated recovery is bounded, observable, and proven not to hide persistent faults.

## Operational

- `quality.operability.logging`: Operators receive bounded and actionable logs without sensitive payload leakage.
- `quality.operability.tracing`: Execution traces preserve causal structure and stable correlation across component boundaries.
- `quality.operability.metrics`: Metrics have stable semantics, bounded cardinality, and operationally useful labels.
- `quality.operability.admin-api`: Administrative operations are discoverable, authorized, bounded, and diagnostically useful.
- `quality.observability.structured-logging`: Logs use structured, stable fields that retain severity, identity, correlation, and diagnostic meaning.
- `quality.observability.distributed-tracing`: Distributed traces preserve context, causation, sampling limits, and provider attribution.
- `quality.observability.metrics-visualization`: Metrics can be interpreted through a documented dashboard or equivalent projection without hidden calculations.
- `quality.observability.state-visibility`: Relevant runtime, Job, Task, provider, and resource states are visible with explicit staleness and authority.
- `quality.deployability.rollback`: Rollback is defined, bounded, and tested together with schema and state compatibility.
- `quality.deployability.blue-green`: Blue/Green deployment has explicit traffic, state, compatibility, and rollback semantics.
- `quality.deployability.canary`: Canary deployment uses bounded admission, comparison, abort, and promotion evidence.
- `quality.configurability.external-configuration`: Configuration is externalized through typed, validated, attributable sources rather than hidden constants.
- `quality.configurability.dynamic-change`: Dynamic configuration changes define admission, consistency, rollback, and audit semantics.
- `quality.configurability.feature-toggle`: Feature toggles are typed, scoped, observable, lifecycle-managed, and fail safely.

## Design-time

- `quality.maintainability.modifiability`: Expected changes can be made through explicit extension points with bounded regression scope.
- `quality.maintainability.understandability`: Responsibilities, boundaries, and behavior are understandable from model, code, and documentation.
- `quality.maintainability.local-change`: A local requirement change remains localized rather than propagating through unrelated modules.
- `quality.extensibility.plugin-structure`: Plugin extension points are explicit, versioned, isolated, and lifecycle-managed.
- `quality.extensibility.spi-design`: SPI contracts separate provider-neutral semantics from replaceable implementations with deterministic compatibility.
- `quality.extensibility.api-stability`: Public APIs have explicit compatibility, versioning, and removal policy.
- `quality.reusability.component-independence`: The component owns a coherent contract and avoids application-specific or provider-specific coupling.
- `quality.reusability.generality`: Reusable abstractions express demonstrated common semantics without erasing required domain distinctions.
- `quality.testability.mockability`: External effects can be replaced by bounded deterministic test providers without changing business semantics.
- `quality.testability.isolation`: Behavior can be exercised with explicit dependencies and controlled state boundaries.
- `quality.testability.deterministic-execution`: Equivalent admitted inputs and execution context produce reproducible decisions and evidence.
- `quality.readability.naming`: Names consistently express domain and framework concepts without hidden aliases or ambiguous abbreviations.
- `quality.readability.dsl-expressiveness`: DSL constructs express intent and constraints directly without requiring implementation inference.
- `quality.readability.documentation-linkage`: Rules, specifications, design, implementation, and executable evidence remain navigably linked.
- `quality.consistency.model`: Domain, protocol, storage, and projection models preserve one declared semantic contract.
- `quality.consistency.api`: Related APIs use consistent naming, envelopes, lifecycle, authorization, and error semantics.
- `quality.consistency.error-model`: Failures use one structured error model across execution, transport, observability, and user surfaces.

## Business

- `quality.portability.environment-dependency`: Environment dependencies are declared, bounded, and replaceable rather than inferred from an ambient host.
- `quality.portability.container-fitness`: The component supports container lifecycle, configuration, resource, signal, and state boundaries.
- `quality.portability.cloud-independence`: Cloud-specific implementation remains behind explicit provider contracts with a viable portable core.
- `quality.interoperability.api-protocol`: OpenAPI, gRPC, or equivalent API projections retain the canonical operation contract.
- `quality.interoperability.standard-protocol`: External protocols follow admitted standards and make extensions and compatibility explicit.
- `quality.evolvability.versioning`: Artifacts, APIs, schemas, and providers have an explicit coordinated versioning strategy.
- `quality.evolvability.backward-compatibility`: Compatibility promises are executable, scoped, and distinguished from intentional breaking changes.
- `quality.evolvability.migration`: State, configuration, API, and deployment migrations have bounded, reversible procedures and evidence.
- `quality.cost-efficiency.infrastructure`: Infrastructure cost drivers are attributable to measured capacity, utilization, and service objectives.
- `quality.cost-efficiency.operations`: Operational effort and continuing service cost are attributable without hiding quality trade-offs.
- `quality.cost-efficiency.development`: Development and maintenance efficiency is supported by bounded workflow evidence rather than unsupported productivity claims.

## AI-aware

- `quality.ai.model-interpretability.dsl-structure`: DSL structure exposes typed intent, relationships, and constraints for bounded AI interpretation.
- `quality.ai.model-interpretability.structured-natural-language`: Structured natural-language descriptions remain linked to canonical model identities and constraints.
- `quality.ai.operability.mcp`: MCP publication is bounded, authorized, described, versioned, and aligned with canonical operations.
- `quality.ai.operability.skill`: Skill publication is bounded, versioned, task-accurate, authority-aware, and aligned with canonical component operations and MCP guidance.
- `quality.ai.operability.spec-componentization`: Specifications are componentized and discoverable as stable task and contract context.
- `quality.ai.operability.automatic-tooling`: Tool projection is deterministic, bounded, and safe rather than inferred from arbitrary implementation methods.
- `quality.ai.context-stability.phase-structure`: Development phase state and completion evidence provide stable bounded context for AI-assisted work.
- `quality.ai.context-stability.responsibility-boundary`: Component, provider, framework, and application responsibilities are explicit and stable.
- `quality.ai.generatability.dsl-clarity`: DSL generation rules are explicit enough to avoid implementation-specific guesswork.
- `quality.ai.generatability.determinism`: Equivalent model input produces deterministic generated contracts and artifacts.
- `quality.ai.generatability.syntax-constraints`: Syntax constraints and invalid forms are explicit, machine-checkable, and diagnostically useful.

## Extended Quality Model

### Functional Suitability

- `quality.functional-suitability.completeness`: Functions cover the declared user and domain tasks without material omissions.
- `quality.functional-suitability.correctness`: Functions produce correct results with the required precision for admitted inputs.
- `quality.functional-suitability.appropriateness`: Functions facilitate the declared task rather than forcing unnecessary or misleading work.

### Accessibility

- `quality.accessibility.perceivable`: Information and controls are available through perceivable alternatives for supported users and assistive technologies.
- `quality.accessibility.operable`: Supported interactions are operable by keyboard and assistive input without traps or inaccessible timing assumptions.
- `quality.accessibility.understandable`: Content, navigation, input expectations, and errors are understandable and predictable.
- `quality.accessibility.robust`: Semantic output remains compatible with supported user agents and assistive technologies.

### Software Supply Chain

- `quality.security.supply-chain.artifact-provenance`: Released artifacts retain verifiable source, build, signer, and distribution provenance.
- `quality.security.supply-chain.dependency-governance`: Dependencies are admitted through explicit version, origin, license, and trust policy.
- `quality.security.supply-chain.reproducible-build`: Equivalent admitted sources and toolchains can reproduce artifact identity or explain bounded differences.
- `quality.security.supply-chain.vulnerability-response`: Known component vulnerabilities have attributable triage, remediation, exception, and release procedures.
- `quality.security.supply-chain.sbom-completeness`: The software bill of materials covers distributed runtime and build-relevant components with stable identities.

### Privacy and Data Governance

- `quality.privacy.data-minimization`: Collection, transfer, retention, and exposure are limited to data necessary for a declared purpose.
- `quality.privacy.purpose-limitation`: Personal or sensitive data use remains bound to declared purposes and authorized processing contexts.
- `quality.privacy.retention-deletion`: Retention, expiry, deletion, and legal-hold semantics are explicit, attributable, and testable.
- `quality.privacy.consent-user-agency`: Required consent and user choices are informed, revocable, and enforced across relevant processing.
- `quality.privacy.sensitive-data-disclosure`: Sensitive data disclosure is prevented or explicitly authorized across APIs, logs, traces, reports, and providers.

### Safety

- `quality.safety.hazard-identification`: Foreseeable harmful states, affected parties, triggers, and controls are identified for the admitted scope.
- `quality.safety.fail-safe-behavior`: Failures move the system to a bounded state that does not create avoidable harm.
- `quality.safety.human-override`: Authorized humans can understand, interrupt, or override safety-relevant automated behavior where required.
- `quality.safety.safe-degradation`: Degraded operation preserves explicit safety invariants and communicates lost capability.
- `quality.safety.blast-radius-containment`: Faults, misuse, and harmful actions are contained within declared resource, tenant, and operation boundaries.

### Data Quality

- `quality.data-quality.accuracy`: Data values correctly represent the admitted real-world or domain facts within declared tolerance.
- `quality.data-quality.completeness`: Required records, fields, relations, and temporal coverage are present for the declared use.
- `quality.data-quality.currentness`: Data freshness, effective time, expiry, and staleness are explicit for the declared use.
- `quality.data-quality.credibility`: Data claims retain attributable source authority, confidence, and contestability.
- `quality.data-quality.lineage`: Derivation, transformation, source, and publication lineage remain traceable across data boundaries.
- `quality.data-quality.semantic-consistency`: Equivalent concepts, identifiers, units, and relations retain one declared meaning across representations.

### AI Trustworthiness

- `quality.ai-trustworthiness.valid-reliable`: AI behavior is valid and reliable for a declared purpose, population, context, and evaluation boundary.
- `quality.ai-trustworthiness.safe`: AI use identifies and controls foreseeable harmful outcomes with bounded fallback and human authority.
- `quality.ai-trustworthiness.secure-resilient`: AI inputs, models, tools, context, and outputs resist admitted attacks and recover from failure.
- `quality.ai-trustworthiness.accountable-transparent`: AI ownership, purpose, provider, model, policy, evidence, and consequential decisions are attributable and transparent.
- `quality.ai-trustworthiness.explainable-interpretable`: Relevant AI outputs and limitations can be interpreted and explained at the level required by affected users and operators.
- `quality.ai-trustworthiness.privacy-enhanced`: AI data, context, prompts, training, retention, and provider exchange apply declared privacy-enhancing controls.
- `quality.ai-trustworthiness.fairness-harmful-bias`: Relevant harmful bias and fairness risks are measured, bounded, monitored, and governed for the admitted context.

### Compatibility and Coexistence

- `quality.compatibility.runtime-coexistence`: The component coexists with declared runtime peers without semantic or lifecycle interference.
- `quality.compatibility.resource-coexistence`: Shared CPU, memory, I/O, network, and storage use remains within declared isolation and fairness limits.
- `quality.compatibility.dependency-compatibility`: Dependency and platform version ranges are explicit and verified without accidental classpath or ABI coupling.

### Business Continuity

- `quality.business-continuity.backup-recoverability`: Authoritative state can be backed up, verified, restored, and reconciled through tested procedures.
- `quality.business-continuity.recovery-point-objective`: Maximum admitted data loss is explicit and supported by measured recovery evidence.
- `quality.business-continuity.recovery-time-objective`: Maximum admitted restoration time is explicit and supported by measured recovery evidence.
- `quality.business-continuity.regional-continuity`: Declared regional or site loss scenarios preserve required service and state semantics.

### Internationalization and Localization

- `quality.internationalization.locale`: Locale-sensitive behavior is explicit and does not depend on an ambient process locale.
- `quality.internationalization.timezone`: Instants, local times, zones, daylight transitions, and display policy are modeled explicitly.
- `quality.internationalization.character-encoding`: Character encoding and normalization are explicit across storage, protocol, and rendering boundaries.
- `quality.internationalization.translation`: User-facing messages and content have explicit translation ownership, fallback, and completeness policy.
- `quality.internationalization.cultural-format`: Names, addresses, numbers, currencies, units, and calendars follow declared cultural formatting rules.

### Supportability

- `quality.supportability.diagnosability`: Operators can isolate a failure from bounded diagnostics without exposing sensitive payloads.
- `quality.supportability.reproducibility`: A reported problem can be reproduced from versioned inputs, configuration, environment, and execution identity.
- `quality.supportability.repairability`: Repair, rollback, replacement, and data correction procedures are bounded and verifiable.
- `quality.supportability.operator-guidance`: Diagnostics link operators to accurate next actions, authority requirements, and escalation paths.

### Compliance and Governance

- `quality.compliance.policy-traceability`: Applicable policy obligations trace to controls, implementation, Evidence, and accountable owners.
- `quality.compliance.evidence-retention`: Required compliance Evidence has explicit retention, integrity, access, and deletion semantics.
- `quality.compliance.exception-governance`: Policy exceptions are time-bounded, approved, attributable, reviewed, and prevented from becoming silent defaults.
- `quality.compliance.regulatory-mapping`: Regulatory claims identify jurisdiction, scope, version, interpretation, and supporting controls without implying unsupported certification.

### Sustainability

- `quality.sustainability.energy-efficiency`: Energy use is attributable to useful work and optimized without hiding reliability or service trade-offs.
- `quality.sustainability.carbon-intensity`: Carbon-impact claims state measurement scope, location/time factors, and uncertainty.
- `quality.sustainability.hardware-efficiency`: Hardware utilization, lifetime, overprovisioning, and replacement are considered against delivered service.
- `quality.sustainability.work-avoidance`: Unnecessary computation, transfer, retention, polling, retries, and generation are identified and bounded.

## Cross-cutting CNCF Extensions

- `quality.security.boundary`: Declared security and admission boundary.
- `quality.security.authentication`: Identity verification and trust lifecycle.
- `quality.security.authorization`: Deny-by-default access decision and enforcement.
- `quality.security.auditability`: Attributable bounded security audit evidence.
- `quality.security.domain.bounded-text-datatype`: Purpose-specific bounded text domain values.
- `quality.security.domain.constrained-numeric-datatype`: Purpose-specific constrained numeric domain values.
- `quality.security.infrastructure.immutable`: Immutable Infrastructure.
- `quality.security.infrastructure.non-persistent`: Non-persistent Architecture.
- `quality.security.infrastructure.disposable`: Disposable Infrastructure.
- `quality.security.infrastructure.volatility`: Deliberate infrastructure Volatility.
- `quality.security.cyber-resilience`: Cyber Resilience.
- `quality.security.moving-target-defense`: Moving Target Defense.
- `quality.domain.identity-consistency`: Identity consistency across model and artifact boundaries.
- `quality.documentation.rationale`: Public documentation rationale.
- `quality.ai-readiness`: Bounded structured evidence for AI-assisted review.
- `quality.resilience`: Explicit failure and recovery behavior.
- `quality.testability`: Executable coverage of admitted behavior.
- `quality.evaluability.corpus-first-experiment`: Corpus-first experiment correlation.
- `quality.observability.runtime-evidence`: Accepted operational runtime evidence.
- `quality.reliability.rest-request-idempotency`: REST request replay protection.
- `quality.reliability.web-form-submission-idempotency`: Web Form resubmission protection.
- `quality.ux.web`: Web task usability.
- `quality.ux.cli`: CLI task usability and automation safety.
- `quality.ux.skill-assisted`: Skill-assisted task guidance and safe authority.
- `quality.ux.cross-surface-consistency`: Web, CLI, and Skill semantic consistency.

## Review Item: REST and Web Form Transport Idempotency

CAR Review MUST evaluate transport-level duplicate prevention independently
from Entity revision, optimistic concurrency, and business-state equality.
Revision detects stale mutation intent and `WriteIfChanged` may suppress an
equal single-Entity state write; neither identifies one transport request or
proves that its external side effects execute at most once.

REST review covers principal- and route-scoped idempotency keys, normalized
request fingerprints, concurrent in-progress coordination, completed-result
replay, conflicting reuse, expiry, bounded retention, authorization isolation,
structured diagnostics, and restart-safe runtime evidence. Merely accepting an
`Idempotency-Key` header is not Assurance without execution and replay evidence.

Web Form review covers principal/session- and form-scoped one-time submission
tokens, hidden framework metadata, Post/Redirect/Get behavior, concurrent
double-submit handling, prior result or redirect replay, conflicting reuse,
expiry, authorization isolation, and actionable user feedback. Disabling a
button in client-side code is not Assurance because retry, refresh, back, and
network replay remain possible.

Both capabilities require attributable evidence for first execution, duplicate
replay, conflict, expiry, and storage failure. Review must also confirm that
idempotency metadata is not decoded as a business operation parameter and does
not expose one principal's recorded result to another principal.

## Review Item: Datastore and Internal DSL Boundary

CAR Review MUST evaluate durable database access as a cross-cutting component
boundary, not merely as an implementation preference. The expected route is
component domain/application logic → purpose-specific internal DSL or typed
persistence port → admitted CNCF datastore. A Review MUST distinguish that
route from framework/infrastructure adapters that implement the datastore
backend.

The deterministic source and runtime-evidence check identifies component
domain/application code that directly opens a database connection; imports or
uses JDBC, SQLite, a vendor driver, or vendor query API; builds raw SQL; reads a
backend URL/path/credential; or bypasses the admitted datastore/CallTree route.
An explicit, framework-owned infrastructure exception is not automatically a
Finding, but it requires bounded ownership, rationale, provider-specific
integration evidence, and no leakage into component behavior. Missing evidence
is `Unknown`; it must never be promoted to an Assurance merely because a
database library is absent from one artifact.

One Observation from this item maps to all applicable existing capabilities:

- `quality.security.boundary`, `quality.security.authorization`, and
  `quality.security.auditability`: direct access can bypass credential handling,
  component/tenant scope, admission, redaction, and attributable audit.
- `quality.observability.runtime-evidence`,
  `quality.operability.tracing`, and `quality.operability.metrics`: direct
  access can escape the datastore CallTree/metric/failure chokepoint and break
  operation-to-storage causality.
- `quality.testability`, `quality.testability.mockability`, and
  `quality.testability.deterministic-execution`: direct vendor access weakens
  datastore substitution, controlled fixtures, and reproducible failure paths.
- `quality.ai-readiness`, `quality.ai.context-stability.responsibility-boundary`,
  and `quality.ai-trustworthiness.accountable-transparent`: when a storage
  effect is absent from CallTree Evidence, an AI-assisted developer cannot
  reliably trace, explain, or debug its causal path.
- `quality.reusability.component-independence`,
  `quality.portability.environment-dependency`, and
  `quality.compatibility.runtime-coexistence`: direct backend dependence erodes
  independent component deployment, backend substitution, and safe coexistence
  in a common datastore.

## Review Item: Entity CQRS and Managed Memory Responsiveness

CAR Review MUST evaluate whether durable Entity behavior uses the CNCF
Entity/Aggregate/View separation to provide responsive, reactive behavior
without concealing consistency or state-management risk. The expected route is
command → Aggregate/Entity consistency boundary → admitted change/projection →
reactive View. The component returns its command outcome at the declared
consistency boundary; it does not synchronously wait for unrelated read-model
rendering or use a hand-built database/cache shortcut to appear responsive.

The check verifies that each View declares whether its read is current,
eventually consistent, stale, unavailable, or failed; that projection ordering
and recovery are attributable; and that command handling preserves Entity
identity, authorization, and invariants. It also examines whether the CNCF
managed Entity/working-set memory path is used within admitted limits and has
observable fallback/recovery behavior. A private unbounded cache, a shadow
database, or a View presented as read-your-writes without proof is a Finding.
Absent runtime evidence of freshness, memory bounds, contention, or fallback is
an explicit `Unknown`, not a performance Assurance.

One Observation from this item maps to all applicable existing capabilities:

- `quality.performance.latency`, `quality.performance.throughput`,
  `quality.performance.concurrency`, and
  `quality.performance.resource-efficiency`: CQRS plus managed in-memory
  Entity state can reduce response-path datastore work and improve sustainable
  throughput only when measured under an admitted workload.
- `quality.reliability.data-consistency`, `quality.reliability.correctness`,
  and `quality.resilience.retry`: Aggregate invariants, projection ordering,
  duplicate handling, and recovery must remain explicit across the write/read
  split.
- `quality.observability.state-visibility`,
  `quality.operability.tracing`, and `quality.operability.metrics`: command,
  projection, memory-hit/fallback, staleness, and failure paths must retain
  correlated runtime evidence.
- `quality.testability.isolation`, `quality.testability.mockability`, and
  `quality.testability.deterministic-execution`: controlled Entity/working-set
  and datastore fixtures must reproduce command and reactive-View outcomes.
- `quality.ux.cross-surface-consistency` and
  `quality.supportability.diagnosability`: Web, CLI, MCP, and Skill surfaces
  must communicate current versus projected state consistently and expose a
  bounded explanation when a View is stale or unavailable.

## Cross-view Projection

The complete 154-capability catalog appears in the `quality` collection. It
contains 72 capabilities promoted from the six February 23, 2026 quality
attribute mind maps, 23 prior CNCF cross-cutting capabilities, and 59 gap-closing
capabilities in the extended quality model. The `namedViews` collection is
generated from every distinct catalog `views` value in deterministic name
order. Each named view selects the same canonical Observation and Evidence
identities and remains present with an empty `items` collection when no current
Observation maps to it. Adding a view does not change the response schema. A
projection never reruns a provider or derives a conclusion.
