# P5-60 Phase 5 Executable Coverage

status=complete
phase=5
checklist=P5-60
updated_at=2026-07-16

## Decision

`docs/spec/phase-5-executable-coverage.json` is the machine-readable ledger
for every behavioral Phase 5 item, P5-01 through P5-55. It records the owning
area and at least one exact executable anchor for each item. Evidence may be a
CBD Scala specification, a sibling Cozy/sbt-cozy/Textus AI Scala specification,
or a repository integration gate.

`Phase5ExecutableCoverageSpec` makes that ledger fail closed: it derives the
behavioral IDs from the authoritative Phase 5 checklist, requires the complete
and duplicate-free expected set, validates each area assignment, and verifies
that every evidence path and exact anchor exists. Sibling paths remain relative
to the CBD Support checkout, avoiding an embedded machine-specific path.

## Evidence

- `sbt 'testOnly org.simplemodeling.textus.cbdsupport.Phase5ExecutableCoverageSpec'`
  passes two specifications: complete coverage and navigable executable
  evidence validation.

## Consequence

P5-60 is complete. P5-61/P5-62 separately establish the full-suite execution
evidence; the coverage ledger does not treat an anchor's existence as proof
that every dependent repository test has been run in this closure cycle.
