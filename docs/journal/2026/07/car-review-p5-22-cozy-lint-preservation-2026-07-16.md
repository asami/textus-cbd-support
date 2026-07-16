# P5-22 Cozy lint preservation

date=2026-07-16
status=complete
checklist=P5-22

## Decision

Cozy's Review Provider does not implement a second lint ruleset. It invokes
the existing integrated `CozyCarLint.lint` boundary and turns each returned
finding into provider-owned Evidence. Every retained fact preserves the source
category, code, level, project-relative path, line, and message exactly.

`cozy car lint` remains an independent focused command. It runs the same
`CozyCarLint` result boundary, retains its normal JSON projection and exit
policy, and does not call CBD Support or the Review Provider.

## Evidence and next work

`cozy.review.CozyCarReviewProviderSpec` creates one CAR fixture, then compares
the adapter Evidence records to the direct lint collection field by field. It
also captures the independent `CozyCarLint.execute --format json` output and
proves it is the direct collection's JSON projection with the same fail/no-fail
exit decision.

P5-23 remains: CBD Support must invoke the Cozy provider through the generic
protocol after target admission while Cozy remains free of a CBD dependency.
