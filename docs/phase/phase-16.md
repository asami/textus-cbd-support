# Phase 16: Safe-by-Construction Application Security Review

Status: PLANNED
Planned: 2026-10-14
Depends on: CNCF Phase 107, Cozy Phase 83
Related: CAR Review Architecture, Textus Security Review Responsibility
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
Unknown rather than PASS.
