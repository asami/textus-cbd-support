# Capability Catalog and Traceability Proposal

Status: exploratory
Date: 2026-09-16
Development item: DEV-CBD-003

## Purpose

Extend Textus CBD Support so users and AI clients can discover Component
Capabilities, compare their realizations, and trace them back to requirements
and forward to Specifications and Evidence.

Capability is a non-instantiated model IR. cbd-support reads the public CNCF
Component Capability projection generated from Cozy sources; it does not
execute Capability, reparse CML, or infer identity from names.

## Consumer boundary

The consumer path is:

```text
Cozy Capability Model IR
  -> CNCF admitted Component Capability projection
  -> CBD Support catalog ingestion
  -> Capability search / comparison / view
  -> realization and evidence traceability
```

Textus CBD Support owns ingestion snapshots, indexing, query semantics,
presentation, provenance display, and stale/incompatible diagnostics. CNCF
owns the external semantic contract. Cozy owns source authoring and generation.

## Proposed views

### Capability Overview

- provided and required Capabilities by Component and version;
- stable qualified identity and semantic description;
- source/component/package provenance;
- Application Capability to Component Capability relationships when published;
- status of realization and evidence links without treating them as Capability
  runtime state.

### Capability Detail

- model references and provided/required direction;
- realizing Operation, Workflow, and StateMachine identities;
- Specification and Evidence links;
- compatibility/version information;
- separate Availability, Authorization, and Guard information when present.

### Search and comparison

- find Components providing a required Capability;
- compare alternative realizations without equating endpoint names;
- identify required Capabilities with no admitted provider;
- trace Use Case/Application Capability demand to Component realizations;
- expose the same semantics through Web and MCP/API surfaces.

## Integrity rules

- Ingest only supported CNCF Capability projection versions.
- Preserve exact Component, package, source, and ABI identity.
- Reject or quarantine ambiguous, duplicate, malformed, stale, or incompatible
  projections.
- Do not merge Capabilities by display name or description similarity.
- Do not imply runtime availability, authorization, or guard satisfaction from
  the presence of a catalog record.
- Do not treat a realization link as proof of successful execution; Evidence
  remains a separately referenced artifact.

## Development slices

1. Consume and validate the CNCF Capability projection.
2. Extend internal catalog/snapshot schema and indexing.
3. Add Capability search and provider/requirement matching.
4. Add overview/detail and realization/traceability views.
5. Expose matching semantics through Web and MCP/API.
6. Prove a real Cozy -> CNCF -> cbd-support fixture.

## Open design questions

- Whether Application Capability relationships are embedded in the CNCF
  projection or obtained through a separate model projection.
- How incompatible or historical Capability versions appear in current versus
  historical catalog views.
- Whether provider matching is identity-only in v1 or permits explicit
  compatibility relations.
- Which existing Component Dashboard surface owns the first Capability view.
