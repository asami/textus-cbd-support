# CAR Review Provider Contract v1

status=specified
checklist=P5-02
updated_at=2026-07-16

## Purpose

This specification defines the versioned, transport-neutral contract by which
CBD Support discovers a Review Provider, requests bounded evidence, and admits
an attributable evidence bundle. CBD Support owns this contract and the
admission decision. A provider bundle is input to the canonical Review Report;
it is not a competing report.

Normative machine-readable artifacts are:

- `docs/spec/schema/car-review-provider-v1.schema.json`
- `docs/spec/examples/car-review-provider-descriptor-v1.json`
- `docs/spec/examples/car-review-provider-request-v1.json`
- `docs/spec/examples/car-review-evidence-bundle-v1.json`

## Document Identity

Every document has `schemaVersion=textus.cbd.review-provider.v1` and exactly
one `documentType`:

| Document | `documentType` | Authority |
| --- | --- | --- |
| Provider descriptor | `provider-descriptor` | Provider describes its identity and supported capability contract. |
| Provider request | `provider-request` | CBD Support authorizes one bounded Review invocation. |
| Evidence bundle | `evidence-bundle` | Provider reports attributable Evidence, Observations, and limitations. |

An unknown schema identity or document type is incompatible. V1 does not
guess a document type from fields and does not translate legacy field names.

## Provider Descriptor

A descriptor fixes:

- provider ID and implementation version;
- provider-owned rule-set ID and version;
- supported Review Provider schema identities;
- versioned capabilities with admitted Evidence and Observation kinds; and
- known provider or capability limitations.

Capability IDs are stable logical identities. CBD Support requests
capabilities, not provider implementation classes or command names.

## Provider Request

CBD Support creates a request containing:

- one Review ID;
- the admitted target identity and digest;
- requested capability and Evidence kinds;
- an explicit include/exclude rule selection;
- optional baseline report identity and digest; and
- finite evidence, observation, input-byte, and timeout limits.

The request carries no credentials. Target filesystem access, process
execution, remote calls, and credential resolution remain outside this JSON
and behind authorized CNCF provider/driver boundaries.

An included and excluded rule selector must not be identical. Empty include
means all provider rules admitted by the selected profile; exclude still
applies. A provider may return a limitation for an unsupported requested rule
or capability but must not silently substitute another capability.

## Evidence Bundle

A bundle repeats the Review ID, target, provider, and rule-set identities that
actually produced the result. It contains:

- `requestDigest` binding the bundle to the exact normalized request;
- `bundleDigest` binding the normalized bundle content;
- source-owned Evidence with stable IDs, subjects, origins, optional locations,
  and bounded facts;
- provider-owned candidate Finding, Assurance, or Unknown Observations whose
  evidence references resolve inside the same bundle; and
- explicit provider, capability, rule, target, evidence, or observation
  limitations.

CBD Support preserves provider identity when it reconciles the bundle. A
provider Observation is not automatically a canonical Review Observation and
never independently establishes the CBD gate result.

## Digest Contract

Every digest has the form `sha256:<64 lowercase hexadecimal characters>`.

V1 normalization is UTF-8 JSON with:

1. object keys sorted lexicographically at every depth;
2. no insignificant whitespace;
3. array order preserved;
4. integers rendered in base-10 without a leading plus sign; and
5. JSON string escaping preserved by the parser/renderer.

`requestDigest` is SHA-256 over the complete normalized provider request.
`bundleDigest` is SHA-256 over the normalized evidence bundle with the root
`bundleDigest` member omitted. Target and baseline digests are supplied by
their owning evidence producers and use the same textual digest format.

## Compatibility and Admission

CBD Support admits a bundle only when all of these conditions hold:

1. descriptor, request, and bundle use the exact supported schema identity;
2. the descriptor advertises that schema identity;
3. requested capabilities are present in the admitted descriptor;
4. bundle provider and rule-set identities match the admitted descriptor;
5. Review ID and target identity/digest match the request exactly;
6. request and bundle digests recompute exactly;
7. Evidence and Observation IDs are unique and every evidence reference
   resolves inside the bundle;
8. counts, byte size, elapsed time, and other enforced limits do not exceed the
   request; and
9. no required identity or limitation field is missing.

An incompatible descriptor, request, or bundle is refused as attributable
Unknown or failed-run evidence according to the later Review Run contract.
CBD Support does not repair field names, select one side of contradictory
identity, drop an unknown required field, or invoke the provider again through
an implicit fallback.

## Canonical Examples

The three JSON examples form one exchange. The descriptor advertises a Cozy
capability, the request selects it for one target digest, and the bundle binds
its Evidence and Observations back to that exact request. The executable
`CarReviewProviderContractSpec` verifies document identities, capability and
target agreement, reference integrity, strict schema declarations, and both
computed digests.

## Deferred Contracts

P5-02 does not define canonical Review Report persistence, final Finding or
Assurance admission, Review Run state, gate policy, target authorization,
retention, or redaction limits. Those are P5-03 and P5-04 responsibilities.
