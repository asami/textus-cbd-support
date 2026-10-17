# Phase 16: Safe-by-Construction Application Security Review

Status: PLANNED
Planned: 2026-10-14
Depends on: CNCF Phase 107, CNCF Phase 110, Cozy Phase 83
Related: CAR Review Architecture, Textus Security Review Responsibility, CNCF Safety Sandbox Architecture
Consumer: sm-workflow Phase 6 review route

## Goal

Add bounded CAR lint/CBD Review coverage for violations of Textus
safe-by-construction application security without turning static review into the
primary runtime defense.

## Work

### Deterministic CAR lint candidates

Detect where evidence is statically reliable:

- raw/native SQL escape use and SQL-string construction at application
  boundaries;
- direct DataStore/JDBC access outside declared owner boundaries;
- trusted/raw HTML escape use and unsafe ordinary-String projection;
- unsafe external redirect/URL escape use;
- mutation Operation exposed as GET;
- request-to-Entity direct binding / undeclared mass-assignment paths;
- direct filesystem/network/process/native/unsafe-deserialization escape use;
- recognizable Capability Laundering, especially Process Execution used to reproduce denied filesystem/network/datastore effects (for example file utilities, curl-like HTTP escape, database CLI escape) instead of the CNCF-managed capability/DSL;
- Execution Boundary Escape: component/application-defined `main`, Scala `@main`, App-style launcher, or direct invocation of program entry points inside an ordinary CAR;
- credential/secret values projected to logging/Display/serialization where
  metadata makes this deterministically knowable.

### Phase 6 early rule slice

A concrete sm-workflow Phase 6 defect used Process Execution for external file I/O. Add the minimum deterministic classification needed by that correction as an early consumer of CNCF Phase 110: distinguish typed/registered Git/SBT execution from generic process use and flag recognizable Process-to-filesystem/network/datastore Capability Laundering. This bounded rule slice may be delivered before the rest of Phase 16; do not block the sm-workflow fix on unrelated application-security review families.

The bounded early slice is authored as of 2026-10-17; validation, actual
sm-workflow consumer use and independent acceptance are pending. CBD main is an
admitted participant in the existing sm-workflow Phase 6 work. The private
`CbdSupport.CbdReviewAdmin.reviewManagedEffects` QUERY uses generated typed
request/result values and one normal CNCF ProviderCall/UnitOfWork diagnostic,
consuming the shared `ExternalEffectUsage` classification. The
[managed-effect contract](../spec/car-review-managed-effect-contract.md) defines
caller-owned IDs/revisions, attributable findings, managed use and unknown
outcomes, and the supplied-observation limits. No source scanning, observed
command execution, hash-based management control, overall security PASS or
full Phase 16 completion is implied. Phase 16 remains PLANNED overall.

### sm-workflow-driven delivery

Deliver the Safety Sandbox review rules incrementally with CNCF Phase 110 and real sm-workflow adoption. Prioritize classifications actually encountered by the vertical slices (worktree/filesystem, managed Git/SBT process versus arbitrary shell, provider/network, then remaining packaged-CAR escape routes). Each CBD rule must consume the CNCF-owned classification and be re-exercised from the sm-workflow scenario that exposed the need. Do not require completion of unrelated security-review families before a useful Safety Sandbox slice can close.

### CNCF Safety Sandbox integration

Consume CNCF Phase 110 External Effect / escape classifications rather than defining a CBD-private sandbox taxonomy. CAR lint is the early developer-feedback layer; CNCF CAR Loading Admission is the runtime-entry enforcement layer; CNCF Runtime Sandbox is the effect enforcement layer. A lint disposition or developer acceptance does not by itself grant runtime authority or bypass Loading Admission.

Execution Boundary Escape is a structural finding even if the custom entry point has not yet performed another forbidden effect: ordinary CAR execution lifecycle belongs to CNCF Runtime. Reusable behavior must be exposed through Service/Operation/API; a separate executable uses admitted ProcessExec/Docker execution.

Direct JVM filesystem/network/process/native/raw-persistence use remains attributable even when the CAR requests a related CNCF capability: capability declarations authorize the managed CNCF route, not ambient JVM access.

### CBD Review interpretation

Review cross-cutting cases: whether each escape hatch is justified and bounded,
whether authorization can be bypassed, whether generated/public surfaces widen
authority, and whether security controls are proportional to the actual threat.

Do not claim absence of SQL injection/XSS/CSRF merely because a pattern scanner
found nothing. Assurance comes from the CNCF/Cozy safe path plus admitted
executable evidence.

Security-themed overengineering remains reviewable: hashes, repeated artifact
comparison, duplicate validation, broad rollback/integrity machinery and
speculative checks require an explicit threat/contract.

## Acceptance

Representative secure generated applications produce no blocking findings.
Each intentionally introduced escape/boundary violation produces an attributable
finding with rule, source and remediation category. Unknown evidence remains
Unknown rather than PASS. Representative Safety Sandbox violations map to the same CNCF-owned External Effect classification used by Loading Admission; CBD Support does not duplicate the runtime policy model.

## DataStore ABI release classification

Security/quality review consumes the canonical CNCF DataStore ABI rather than
inventing its own schema compatibility model.

Where provider evidence is available, CAR Review should surface whether a
candidate persistent change is compatible with Normal Update or requires a
Maintenance Update. A breaking DataStore ABI change presented as an
automatic-compatible Normal Update is a blocking architectural finding.

Do not recommend generic rollback/dual-write/migration machinery solely to keep
a breaking change in the Normal Update path. Reclassification to Maintenance
Update is a valid and preferred simplification when backward compatibility
cannot reasonably be preserved.

## CNCF authorization chokepoint review

Authorization review assumes CNCF already owns two enforcement boundaries:
Operation authorization and Entity/object authorization at internal DSL ->
UnitOfWork. CBD Support does not require application Actions to add duplicate
authorize calls.

Review focuses on architectural escapes and regressions:

- ordinary Entity access bypassing the internal DSL/UoW chokepoint;
- public/generated paths reaching EntityStore/DataStore directly;
- external input selecting ServiceInternal/System access mode;
- internal-service delegation widening results beyond the caller's admitted
  authority without an explicit bounded proxy contract;
- search/list/batch/relation paths that return objects not visible under the UoW
  authorization policy.

Static evidence does not prove the absence of BOLA/IDOR. Connected executable
CNCF scenarios remain the primary assurance for enforcement behavior.
