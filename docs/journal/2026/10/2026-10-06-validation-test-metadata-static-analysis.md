# CNCF Validation/Test Metadata ABI static-analysis consumer

Date: 2026-10-06
Status: design decision

CNCF will own a portable Validation/Test Metadata ABI attached directly to executable specifications/test operations. sm-workflow consumes it dynamically; textus-cbd-support consumes the same normalized metadata statically.

This avoids a cbd-support-specific test annotation model and avoids duplicated TestSuite management data. Hand-written Scala annotations and CML properties are normalized by the CNCF ABI boundary; CBD Support analyzes the resolved semantic model.

The initial review value is Test Architecture shape and delta: purpose/feature coverage, missing routine validation, FULL/HEAVY bias, coverage-reducing annotation changes, expected-duration declarations, and correlation with sm-workflow runtime evidence when available.

The existing Human-in-the-Loop review principle remains authoritative. These values are KPIs/findings for human judgment, not a universal quality score. Static analysis must not automatically mutate annotations to improve counts.

A metadata-only change can be architecturally significant. In particular, removing ADMISSION or FOCUSED membership can reduce routine validation coverage even if test code is unchanged; CBD Support should surface such changes explicitly.

This consumer role complements Phase 13 Finding Disposition work: CNCF owns common annotation ABI semantics, while CBD Support provides developer-facing review/resolution workflows. Validation/Test Metadata is declarative Test Architecture, not a finding disposition and should remain a separate semantic ABI.
