# Phase 8 Deferred Work

**Date:** 2026-08-15
**Phase:** Phase 8 / P8-RQH scope narrowing

## DFW-P8-001 — restart-safe Review Job integration

- **Status:** OPEN, nonblocking after developer scope decision.
- **Source review identity:** the consumed Phase 8 full review and the later
  invalid second-review spike.
- **Why nonblocking:** The developer rejected all CBD-local substitutes and
  restored the established direct provider submission and P5 Review Run
  compatibility path. Phase 8 retains Entity persistence/read models,
  redaction hardening, and total quality coverage without claiming production
  restart recovery.
- **Owner:** cloud-native-component-framework.
- **Target:** CNCF Phase 69.
- **Dependency:** a durable Job result payload, exact indexed/cursor Job
  lookup, and process-restart recovery.
- **Resume condition:** CNCF Phase 69 acceptance followed by a freshly scoped
  CBD Support Phase.
- **Prohibited local workarounds:** bounded full scans; timeout synchronous
  adapters; RunSnapshot reservation/outbox substitutes; current-process Job
  result dependence; synthesized terminal lease/lifecycle; and private digest
  protocols.
- **Reference, not edited:** CNCF Phase 69 compatibility-spike journal
  /Users/asami/src/dev2025/cloud-native-component-framework/docs/journal/2026/08/2026-08-15-cbd-review-job-compatibility-spike-for-phase-69.md.

This Deferred Work entry neither starts CNCF Phase 69 nor authorizes a
cross-repository change. Hygiene remains separately tracked.
