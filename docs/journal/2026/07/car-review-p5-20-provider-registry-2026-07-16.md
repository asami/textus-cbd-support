# P5-20 Provider registry foundation

date=2026-07-16
status=complete
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

`CarReviewProviderExecutionCoordinator` now selects a runner only from a
registry registration whose provider identity and parsed descriptor exactly
match the execution request. An unregistered identity or a descriptor mismatch
is an attributable refused provider outcome and cannot execute a caller-supplied
runner. The selection specification also proves the normal admitted path.

`CarReviewProviderExecutionApplication` is the CBD-owned application execution
entry point. It invokes the coordinator with an ActionCall core, so the
descriptor-selected runner is constructed as `CncfCarReviewProviderRunner`
only after exact registry admission. Its executable specification proves a
canonical registered exchange reaches `ProviderCall`, preserves the selected
provider identity, and does not publish descriptor, request, or bundle text in
CallTree.

P5-20 is complete. Cozy adapter and lint preservation begin at P5-21 and
P5-22 respectively.
