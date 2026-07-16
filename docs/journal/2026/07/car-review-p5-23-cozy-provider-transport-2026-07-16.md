# P5-23 Cozy Provider Transport

status=complete
phase=5
checklist=P5-23
updated_at=2026-07-16

## Context

CBD Support owns Review admission and must be able to request Cozy evidence for
an admitted CAR. Cozy must remain a provider: it cannot depend on, invoke, or
construct a CBD Support application.

## Decision

The boundary is the public, neutral command:

```text
cozy review car-evidence --project-root <admitted-root> --provider-version <version> --request-stdin
```

CBD's `CozyCarReviewProviderRunner` binds the target identity and digest to a
configured local root before provider work. Its process transport accepts only
the configured command prefix plus the fixed command template, passes the v1
provider request through standard input, clears the child environment, applies
the request timeout, and confines the response to a bounded CBD-owned output
root. Request and evidence-bundle payloads are not CallTree or log metadata.

Cozy's `CozyCarReviewProviderCommand` reads that request, validates the fields
it consumes, recomputes `requestDigest`, and delegates only to
`CozyCarReviewProvider`. The Cozy implementation contains no CBD Support
class or application dependency.

## Evidence

- `cozy.review.CozyCarReviewProviderSpec` proves neutral request admission and
  bundle digest binding through the bounded command (5 tests passed).
- `CozyCarReviewProviderRunnerSpec` proves CBD registry selection calls the
  selected Cozy transport once, rejects a target mismatch before provider work,
  rejects a configured provider-version mismatch before transport work, and
  executes the fixed local transport with stdin and an empty child environment
  (4 tests passed).

## Consequence

P5-24 can now extend the provider behavior matrix across this real transport
boundary. `sbt-cozy` integration remains P5-31 and must use these public
provider/application surfaces rather than an implementation dependency.
