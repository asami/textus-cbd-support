# P5-20 Provider registry foundation

date=2026-07-16
status=in-progress
checklist=P5-20

## Decision

CBD Support now owns an explicit in-process registration boundary for local
CAR Review providers. `CarReviewProviderRegistry` admits only a strict v1
provider descriptor through the shared bundle-admission parser. Registration is
idempotent only for the same immutable descriptor; a changed descriptor under
the same provider identity is refused. Discovery requires distinct requested
capability IDs and a positive result bound, returns only providers that satisfy
every requested capability, sorts identities deterministically, and has no
implicit provider fallback.

The shared descriptor parser now projects capability version, Evidence kinds,
and Observation kinds as typed values. This keeps registry discovery and later
bundle admission on one descriptor-validation contract rather than creating a
second permissive parser.

## Evidence and remaining work

`CarReviewProviderRegistrySpec` proves strict registration, exact capability
discovery, immutable registration conflict refusal, runner lookup, and bounded
discovery failure. `CarReviewProviderBundleAdmission` remains the authority for
descriptor shape and compatibility validation.

`CncfCarReviewProviderRunner` now places local runner execution and cancellation
behind `ProviderCall`, `ProviderEngine`, and one UnitOfWork provider step. Its
CallTree attributes contain only Review ID, provider identity/version, and
target digest; descriptor, request, Evidence-bundle, and provider exception
content are never supplied as observability properties. The executable spec
proves both execution and cancellation behavior.

P5-20 remains open. The next slice must connect capability selection from the
registry to the execution coordinator and construct production runners through
authorized CBD Review Application wiring. Cozy adapter and lint preservation
are then P5-21 and P5-22 respectively.
