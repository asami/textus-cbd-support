CAR Review Specification Study Handoff

1. Background

We are considering a feature for reviewing CAR implementations not only from the perspective of source code quality, but also as components that run on Textus / CNCF.

Many quality attributes of a CAR are provided by CNCF mechanisms such as the execution platform, authorization, job management, observability, and resilience.

However, the purpose of this feature is not simply to score whether a CAR uses CNCF correctly.

The feature should collect CNCF usage, CAR-specific implementation, configuration, tests, documentation, and related artifacts as a common body of evidence, and allow the review results to be viewed from the following perspectives:

1. CNCF perspective
2. Implementation perspective
3. Quality attribute perspective

The same analysis results should be projected and displayed through different views.

⸻

2. Core Concept

CAR Review collects common evidence from the CAR contract, model, source code, configuration, tests, documentation, and CNCF usage, and projects that evidence into multiple views.

CAR
 ├─ CML / Domain Model
 ├─ CAR Metadata
 ├─ Source Code
 ├─ Configuration
 ├─ Tests
 ├─ Documentation
 ├─ AI Metadata
 └─ CNCF Runtime Usage
          ↓
    Analysis Engine
          ↓
 Evidence / Finding / Assurance / Unknown
          ↓
 ┌──────────────┬─────────────────────┬────────────────────────┐
 │ CNCF View    │ Implementation View │ Quality Attribute View │
 └──────────────┴─────────────────────┴────────────────────────┘

Each view should not perform a separate analysis. Instead, common analysis results should be projected into a view at presentation time.

⸻

3. Objectives

The feature should answer the following questions.

CNCF Perspective

* Does the CAR use CNCF mechanisms correctly?
* Does it make sufficient use of the CNCF capabilities available to it?
* Does it contain custom implementations that bypass CNCF conventions?
* Are the declarations and configurations required for CNCF to provide quality attributes present?

Implementation Perspective

* Are there problems in the source code?
* Are types, safety, effects, concurrency, I/O, and exception handling implemented appropriately?
* Are transactions, resources, and external dependencies handled correctly?
* Can issues be identified at the file, class, method, and line level?

Quality Attribute Perspective

* To what extent are quality attributes such as security, performance, and resilience established?
* Which provider and mechanism establish each quality attribute?
* Which parts are handled by CNCF, and which responsibilities remain with the CAR?
* What evidence, gaps, and unassessed areas exist for each quality attribute?

⸻

4. View Structure

4.1 CNCF View

The CNCF View displays review results organized around CNCF features, subsystems, and conventions.

Candidate areas include:

Component Model
Service / Operation
Command / Query / Event
Entity Runtime
Entity Realm / Entity Space
Collection
Aggregate / View
Job Management
Execution Context
Transaction
Compensation
Authorization
Capability / Guard
Observability
Resilience Policy
Configuration
Runtime Lifecycle
Web Tier

Example:

Job Management
✓ Async Commands are executed as Jobs
✓ JobContext is bound to ExecutionContext
△ Retry policy is not explicitly defined
× Compensation is required but not defined

The CNCF View should show adoption status, problems, assurances, and unassessed items for each CNCF feature.

⸻

4.2 Implementation View

The Implementation View displays review results organized around the CAR source code and implementation structure.

Candidate targets include:

Module
Package
File
Class
Trait
Method
Operation implementation
Dependency
Configuration file
Test file

Candidate review areas include:

Type safety
Exception handling
Effect handling
Blocking I/O
Concurrency
Resource management
Transaction boundary
External calls
Timeout
Retry
Idempotency
Complexity
Duplication
Dependency direction
Null / unsafe cast
Logging
Secret handling
Test code

Example:

PaymentClient.scala:82
Severity: Warning
Confidence: High
No timeout is configured for the external API call.
Impacts:
- Resilience
- Performance
- Reliability
Related CNCF feature:
- Command Execution Policy / Timeout

The Implementation View should resemble an IDE or GitHub code review experience, allowing users to inspect findings at concrete implementation locations.

⸻

4.3 Quality Attribute View

The Quality Attribute View shows how far each quality attribute is established across the combined CAR and CNCF execution system.

Initial candidates include:

Security
Performance
Domain
Documentation
AI Readiness
I18N
Resilience
Testability
Observability
Reliability
Availability
Scalability
Maintainability
Compatibility
Portability / Deployability
Usability / Developer Experience
Accessibility
Efficiency / Sustainability

Each quality attribute contains a set of Quality Capabilities.

Example:

Resilience
 ├─ Timeout
 ├─ Retry
 ├─ Backoff
 ├─ Circuit Breaker
 ├─ Bulkhead
 ├─ Idempotency
 ├─ Failure Isolation
 ├─ Compensation
 ├─ Job Recovery
 ├─ Restart Recovery
 └─ Failure Testing

Example display:

Resilience
Maturity: Established
Coverage: 78%
Confidence: High
✓ Timeout                  Verified
△ Retry                    Partial
× Idempotency              Missing
△ Compensation             Partial
✓ Job recovery             Verified
× Failure injection test   Missing

⸻

5. Major Quality Views

5.1 Security

Main capabilities:

Identity
Authentication
Authorization
Capability
Guard
Resource-level access control
Input validation
Data protection
Secret handling
Audit trail
Dependency security
Security testing

Areas mainly handled by CNCF:

SecuritySubject
Authentication integration
Authorization Engine
Capability
Guard
Operation boundary enforcement
Audit execution records

Areas that remain the responsibility of the CAR:

Appropriate Capability declarations
Domain-specific Guards
Sensitive data handling
External service credentials
Content-dependent input constraints
Security tests

⸻

5.2 Performance

Main capabilities:

Latency
Throughput
Resource consumption
Concurrency
Caching
Batching
Data access
External I/O
Scalability
Performance testing

Areas supported by CNCF:

Operation execution management
Async / Background Job
Entity Memory Realm
Partitioning
Timeout
Metrics
Execution tracing

CAR-specific evaluation areas:

N+1 access
Unnecessary Entity loading
Oversized Aggregates
Blocking I/O
Caching strategy
Algorithmic characteristics
Number of external API calls

⸻

5.3 Domain View

The Domain View is not a conventional quality attribute. It shows how the domain model is realized in the implementation.

Main capabilities:

Ubiquitous Language
Entity realization
Value realization
Aggregate boundary
Invariant enforcement
Command / Query separation
Domain behavior placement
Identity
Lifecycle
Domain Event
External model isolation
CML-to-code traceability

Example:

Domain concept    CML declaration    Implementation     Test    Document
Order             order.cml          Order.scala        ✓       ✓
Order total       invariant          calculateTotal    ✓       △
Cancellation      operation          CancelBehavior    ×       ✓

The Domain View should not ask AI to subjectively score whether the domain model is good. Instead, it should visualize traceability among model elements, implementation, tests, and documentation.

⸻

5.4 Documentation View

Documentation View should be preferred over Document View.

Main capabilities:

Purpose
Domain overview
Public operations
Input / output
Error behavior
Usage examples
Configuration
Deployment
Security
Operations
Troubleshooting
Compatibility
Change history

The evaluation should consider not only whether documentation exists, but also:

Consistency with implementation
Coverage of public Operations
Executability of examples
Freshness
Clarity of target audience

⸻

5.5 AI View

The AI View evaluates how well AI can discover, understand, select, use, and modify the CAR.

Main capabilities:

Machine-readable purpose
Capability description
Operation semantics
Input / output schema
Preconditions
Postconditions
Side effects
Error semantics
Idempotency
Usage examples
Selection guidance
Non-goals
Constraints
AI catalog exposure
Structured metadata
Terminology linkage

Example:

purpose: Cancel an order
preconditions:
  - The order has not been shipped
effects:
  - Changes the order status to cancelled
  - May start a refund job
idempotency: idempotent
use_when:
  - A customer requests cancellation before shipment
do_not_use_when:
  - Processing a return for an already shipped item

The focus is not the volume of human-readable documentation, but the availability of structured information that AI can use for decision-making.

⸻

5.6 I18N View

Main capabilities:

Externalized messages
Locale handling
Time zone
Date / time format
Number format
Currency
Character encoding
Collation
Localized validation messages
Localized documentation
Locale-independent identifiers
Translation coverage

Even when CNCF provides an I18N infrastructure, the quality attribute is not established if the CAR contains hard-coded user-facing strings.

Example:

I18N infrastructure: Provided by CNCF
Adoption coverage: 92%
Hard-coded user messages: 4
Locale-sensitive tests: Missing

⸻

5.7 Resilience

Main capabilities:

Timeout
Retry
Backoff
Circuit breaker
Bulkhead
Idempotency
Failure isolation
Transaction boundary
Compensation
Job recovery
Restart recovery
Degraded operation
External dependency handling
Failure testing

CNCF can provide execution mechanisms, but the business validity of retries, prevention of duplicate updates, and compensation semantics remain CAR responsibilities.

⸻

5.8 Testability

The correct spelling is Testability.

Main capabilities:

Dependency isolation
Deterministic behavior
Clock abstraction
ID generation abstraction
External I/O abstraction
Fixture creation
In-memory execution
Unit tests
Property-based tests
Integration tests
Contract tests
Failure tests
Observability assertions
Public operation coverage

The evaluation should focus not only on code coverage, but also on whether the implementation structure is inherently testable.

⸻

5.9 Observability

Main capabilities:

Operation tracing
Call tree
Structured logs
Technical metrics
Business metrics
Correlation ID
Job visibility
Error classification
Domain event visibility
Sensitive-data masking
Dashboard readiness
Alert readiness
Diagnostic documentation

CNCF may provide technical tracing, metrics, and job visibility, but domain-specific attributes and business metrics must still be defined by the CAR.

⸻

6. Quality Attribute Delegation to CNCF

The evaluation should explicitly identify which provider establishes each quality attribute.

enum AssuranceProvider:
  case Cncf
  case CarImplementation
  case Configuration
  case DeploymentPlatform
  case ExternalService
  case HumanProcess
  case Unknown

For each evaluation item, distinguish:

Provider of the quality attribute
Mechanism being used
Declarations or implementation required from the CAR
Actual adoption status
Evidence that the capability is established
Missing elements

Example:

Observability / Operation tracing
Provider:
- CNCF
Mechanism:
- Structured CallTree
- OpenTelemetry export
Adoption:
- Complete
Evidence:
- All public Operations are executed through CNCF Action
- Trace exporter is configured
Remaining responsibility:
- Domain-specific span attributes

⸻

7. Findings, Assurances, and Unknowns

A warning list alone cannot distinguish between an item that has no problem and an item that has not been evaluated.

The analysis results should contain at least the following three types:

enum ObservationType:
  case Finding
  case Assurance
  case Unknown

Finding

A problem, gap, convention violation, or quality degradation factor.

Assurance

Positive evidence showing that a quality capability is established.

Unknown

An item that cannot be assessed because of insufficient information or analysis limitations.

Example:

✓ Authentication    Use of the standard CNCF mechanism confirmed
✓ Authorization     Capability declared for all Operations
△ Guard             Two Operations could not be assessed
× Secret handling   A possible hard-coded token was detected

⸻

8. Common Evidence Model

Analysis rules should not be implemented separately for each view.

The core flow should be:

Rule
 ↓
Evidence
 ↓
Observation
 ↓
Capability Assessment
 ↓
View Projection

The same Evidence and Observation should be reusable from multiple views.

Example finding:

No timeout is configured for an external API call

The same finding is projected as follows:

CNCF View
  Resilience Policy
    CNCF timeout mechanism is not being used
Implementation View
  PaymentClient.scala:82
    External call has no timeout
Quality Attribute View
  Resilience > Timeout
    Partial
  Performance > Latency control
    Partial
  Reliability > External dependency control
    Partial

⸻

9. Candidate Data Model

9.1 Review Report

case class CarReviewReport(
  car: CarIdentity,
  observations: Vector[ReviewObservation],
  evidence: Vector[ReviewEvidence],
  capabilityAssessments: Vector[QualityCapabilityAssessment],
  generatedAt: Instant
)

9.2 Observation

case class ReviewObservation(
  id: ObservationId,
  observationType: ObservationType,
  ruleId: ReviewRuleId,
  severity: Option[ReviewSeverity],
  confidence: ReviewConfidence,
  message: String,
  rationale: String,
  evidence: Vector[EvidenceId],
  locations: Vector[ReviewLocation],
  cncfMappings: Vector[CncfFeatureRef],
  implementationMappings: Vector[ImplementationCategoryRef],
  qualityMappings: Vector[QualityImpact]
)

9.3 Quality Impact

case class QualityImpact(
  qualityAttribute: QualityAttributeId,
  capability: QualityCapabilityId,
  impact: ImpactLevel,
  direction: ImpactDirection
)
enum ImpactDirection:
  case Positive
  case Negative
  case Neutral

Positive is required so that the system can represent not only problems, but also quality capabilities established through CNCF usage.

9.4 Quality View Definition

case class QualityCapability(
  id: QualityCapabilityId,
  title: String,
  description: String
)
case class QualityViewDefinition(
  id: QualityViewId,
  title: String,
  capabilities: Vector[QualityCapabilityRef]
)

Capabilities should not be embedded directly in a View. They should be reusable across multiple Views.

Example:

Idempotency
  → Resilience
  → Reliability
  → Testability
  → Domain

9.5 Quality Assessment

case class QualityViewAssessment(
  view: QualityViewId,
  maturity: QualityMaturity,
  coverage: Coverage,
  confidence: ReviewConfidence,
  capabilities: Vector[QualityCapabilityAssessment],
  strengths: Vector[AssessmentNote],
  gaps: Vector[QualityGap]
)
case class QualityCapabilityAssessment(
  capability: QualityCapabilityId,
  maturity: QualityMaturity,
  providers: Vector[AssuranceProvider],
  evidence: Vector[EvidenceId],
  gaps: Vector[QualityGap]
)

⸻

10. Evaluation Metrics

10.1 Maturity

The system should show quality maturity stages rather than relying only on a numeric score.

enum QualityMaturity:
  case Unassessed
  case Missing
  case AdHoc
  case Partial
  case Established
  case Verified
  case Operational

Definitions:

Unassessed
  Not evaluated
Missing
  The required mechanism does not exist
AdHoc
  Individual implementation exists, but it is inconsistent
Partial
  The capability is established for some targets or use cases
Established
  The capability is consistently established in design and implementation
Verified
  The capability has been confirmed through tests or static analysis
Operational
  The capability has also been confirmed through runtime metrics or operational evidence

10.2 Coverage

Coverage indicates how much of the applicable scope has the quality mechanism applied.

Security coverage: 90%
Observability coverage: 100%
I18N coverage: 62%

10.3 Confidence

Confidence indicates how certain the analysis result is.

enum ReviewConfidence:
  case Low
  case Medium
  case High

Severity and Confidence should be separate.

Critical / High confidence
  A Query modifies an Entity
Critical / Medium confidence
  An Operation may be non-idempotent

⸻

11. UI Proposal

The top-level UI should allow users to switch views.

CAR Review: order-car
[ Overview ] [ CNCF ] [ Implementation ] [ Quality Attributes ]

Overview

Critical findings        2
Warnings                11
CNCF adoption           87%
Quality coverage        76%
Unknown assessments      8

CNCF View

Display results by CNCF subsystem.

Component
Service / Operation
Entity Runtime
Collections
Job Management
Authorization
Observability
Configuration

Implementation View

Display results by implementation structure.

Module
Package
File
Class
Method
Dependency

Quality Attribute View

Display results by quality attribute.

Security
Performance
Domain
Documentation
AI
I18N
Resilience
Testability
Observability
...

⸻

12. Cross-View Navigation

Users should be able to navigate from an item in one View to related items in other Views.

From the Quality Attribute View:

Resilience > Timeout
  Partial
Related CNCF feature:
  Command Execution Policy > Timeout
Related implementation:
  PaymentClient.scala:82
  ShippingClient.scala:104

From the Implementation View:

PaymentClient.scala:82
Impacts:
- Resilience
- Performance
- Reliability
Related CNCF feature:
- Timeout Policy

From the CNCF View:

Job Management > Compensation
Affected implementation:
- CancelOrderBehavior.scala
Affected qualities:
- Resilience
- Reliability
- Domain

⸻

13. CLI Proposal

cozy car review ./order-car

View selection:

cozy car review --view cncf
cozy car review --view implementation
cozy car review --view quality

Multiple Views:

cozy car review \
  --view cncf,quality

Specific quality attributes:

cozy car review \
  --view quality \
  --quality security,resilience,observability

Compatibility review against a baseline:

cozy car review \
  --baseline com.example:order-car:1.2.0 \
  ./order-car

CI usage:

cozy car review \
  --profile release \
  --format sarif \
  --fail-on error

Candidate output formats:

text
json
html
sarif

The JSON output should contain a common Review Report rather than separate reports for each View.

Projection into Views should be handled by the renderer.

⸻

14. Review Rule Types

Deterministic Rule

Rules that can be evaluated reliably by a program.

A Query calls an update API
A public Operation type has changed
An Enum value has been removed
A timeout is not configured
A Capability declaration is missing

Heuristic Rule

Rules evaluated using patterns or metrics.

An Aggregate may be too large
An Operation may have too many responsibilities
Dependencies may be too tightly coupled
A View may be expensive to construct

AI Rule

Rules requiring semantic understanding.

Whether an Operation name matches its implementation semantics
Whether Command / Query classification is appropriate for the domain
Whether compensation behavior is valid from a business perspective
Whether the CML intent matches the Scala implementation
Whether AI-facing documentation is sufficient
Whether tests adequately express the specification

AI should not receive the entire codebase as unstructured input. It should receive structured information produced by deterministic analysis.

CML
+ CAR Metadata
+ Public API
+ Call Graph
+ Entity Access Graph
+ Effect Summary
+ Tests
+ Documentation
+ Deterministic Findings
        ↓
     AI Review

AI should not be the sole reviewer. Its role is to interpret semantic areas that cannot be fully evaluated through static analysis and structured evidence.

⸻

15. Proposed Implementation Phases

Phase 1: Common Evidence Model

* Review Report model
* Evidence model
* Finding / Assurance / Unknown
* Location
* CNCF / Implementation / Quality mappings
* JSON output

Phase 2: CNCF View

* Service / Operation
* Command / Query / Event
* Entity / Aggregate / View
* Job Management
* Authorization
* Observability
* Basic timeout and retry configuration

Phase 3: Implementation View

* Scala AST analysis
* Call Graph
* Effect / I/O analysis
* Entity access analysis
* Blocking I/O
* Exception handling
* Timeout
* Resource management
* SARIF output

Phase 4: Quality Attribute View

Initial targets:

Security
Domain
Documentation
AI
Resilience
Testability
Observability

Later targets:

Performance
I18N
Reliability
Compatibility
Maintainability
Availability
Scalability

Phase 5: AI Semantic Review

* CML and implementation consistency
* Domain semantics
* Documentation consistency
* AI readiness
* Test adequacy
* Compensation semantics

⸻

16. Open Questions

The following items require further specification.

1. Whether the external feature name should be CAR Review, CAR Quality Review, or CAR Quality Views
2. Whether CNCF View should remain an internal term and appear externally as Textus View or Framework View
3. The standard set of Quality Capabilities
4. Maturity evaluation rules for each Capability
5. Coverage calculation method
6. How CNCF version differences should be reflected in View definitions
7. How runtime Evidence should be collected
8. How static and runtime analysis should be integrated into one Report
9. AI Rule execution cost and CI usage policy
10. False-positive suppression, exclusions, and review suppression comments
11. The division of responsibility between SARIF and the custom JSON format
12. Whether Quality View definitions should be hard-coded or defined through a DSL

⸻

17. Core Design Principles

Principle 1

CAR Review is not merely a CNCF compliance linter.

Principle 2

It visualizes how far each quality attribute is established across the combined CNCF and CAR implementation system.

Principle 3

The CNCF View, Implementation View, and Quality Attribute View are different projections of common Evidence, not separate analysis results.

Principle 4

The system handles not only problems, but also Assurances that show established quality and Unknowns that indicate assessment gaps.

Principle 5

The same Evidence can be referenced from multiple CNCF features, implementation locations, and quality attributes.

Principle 6

The provider of a quality capability should be decomposed into CNCF, CAR implementation, configuration, deployment platform, external service, and other relevant providers.

Principle 7

AI should complement semantic areas that cannot be determined through static analysis and structured Evidence. It should not replace the analysis engine.

⸻

18. Summary

CAR Review generates common Evidence from the CAR model, contract, implementation, configuration, tests, documentation, and CNCF usage, and projects it into the following three Views:

CNCF View
  Which CNCF mechanisms are used, and how they are used
Implementation View
  Where problems and assurances exist in source code and configuration
Quality Attribute View
  How far each quality attribute is established through the combined result of CNCF and the CAR

The core of the feature is not to implement separate review logic for each View, but to build a common Evidence Model that allows the same facts to be examined from multiple perspectives.
