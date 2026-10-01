# AI Hash-Design Deviation Review

## Decision

CBD Support treats newly introduced explicit hash-based mechanisms as a high-signal AI design-deviation review target.

The governing assumption is deliberately asymmetric: generative AI has a strong learned tendency to reach for hashes as a convenient technical mechanism. Therefore CBD Support must not treat a technically plausible hash as evidence that the design is appropriate.

## Review policy

For application, middleware, infrastructure, and library code:

- Explicit hash/checksum/digest/fingerprint/content-derived identity introduced by AI is a review trigger.
- The default review result is non-admission until an authoritative external justification is identified.
- AI-generated rationale is not sufficient justification.
- A lower-level specification produced or modified by AI is not sufficient merely because it already contains the hash mechanism.
- Review traces the mechanism upward to an approved architecture/specification, protocol or format requirement, approved security design, approved Failure Model that selects the mechanism, or explicit human authorization.
- If the hash substitutes for identity, state, causality, lifecycle, cycle management, deduplication semantics, version/control data, or another missing semantic model, the finding is a model-design deviation. The expected correction is to repair the model, not to improve the hash implementation.
- Passing tests and local technical correctness do not clear the finding.

Standard collection implementations and other encapsulated platform internals are not findings merely because their implementation uses hashing. The review target is explicit project-level introduction or selection of a hash-based mechanism.

## Admission interpretation

Hash introduction should be represented as a strong admission signal:

1. detect the explicit mechanism;
2. locate external authorization;
3. verify that the authorization applies to the exact purpose and scope;
4. reject admission when authorization is absent or when hash is being used as a semantic substitute.

Legitimate examples include externally specified cryptographic/integrity protocols and content-addressed formats. These are narrow permissions; they do not authorize reuse of the digest as unrelated application or control identity.

## KPI / observation

CBD Support should retain this as an observable AI-behavior category rather than only a style violation. Useful measurements include:

- number of AI-introduced hash mechanisms;
- externally authorized vs unauthorized introductions;
- cases where review identifies missing semantic/control data;
- recurrence by model/reasoning configuration;
- defects attributable to unauthorized hash-based design.

This supports the broader Human-in-the-Loop goal: expose concrete implementation behavior so a human can apply architectural intuition, while deterministic checks catch strong known deviation patterns.

## Relationship to ai-directive

The authoritative behavioral prohibition lives in `ai-directive` Hash Prohibition Rules. CBD Support observes and enforces the review/admission side of that contract. The intended division is:

`Directive -> Candidate -> deterministic/assisted review -> Admission`

The purpose is not to teach the model to choose hashes more carefully. The purpose is to remove the design decision from AI unless the mechanism has already been authorized externally.
