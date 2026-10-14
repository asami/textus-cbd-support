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
- credential/secret values projected to logging/Display/serialization where
  metadata makes this deterministically knowable.

### CNCF Safety Sandbox integration

Consume CNCF Phase 110 External Effect / escape classifications rather than defining a CBD-private sandbox taxonomy. CAR lint is the early developer-feedback layer; CNCF CAR Loading Admission is the runtime-entry enforcement layer; CNCF Runtime Sandbox is the effect enforcement layer. A lint disposition or developer acceptance does not by itself grant runtime authority or bypass Loading Admission.

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
