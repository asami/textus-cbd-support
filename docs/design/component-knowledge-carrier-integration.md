# Component Knowledge Carrier Integration Design

status=partially-implemented
phase=cncf-59.6
updated_at=2026-08-26

## Purpose

CBD Support makes declared Component knowledge available for exact local
Component detail, usage guidance, read-only MCP projection, and deterministic
CAR Review evidence.  The input is a carrier declared by the generated
Component descriptor; it is not a conventional file-discovery facility.

## Data Flow and Ownership

1. A producer writes `component-knowledge.json`, declares its schema, logical
   path, and SHA-256 in `component-descriptor.json`, and records the same
   target-relative path/digest pair in `target/cncf.d/car-runtime-manifest.json`
   for a development directory.  For an exported catalog, Cozy publishes the
   same bytes as the exact version-scoped sidecar and the BOK metadata carries
   both the archive declaration and that one public URI.
2. CBD Support reads only the descriptor-declared carrier path.  A local or
   cached CAR contributes the named archive entry; a development directory
   contributes only fixed exact-leaf evidence queries rooted at its explicitly
   configured project.
3. CBD checks schema, exact Component/release identity, carrier SHA-256, and,
   for development evidence, the generated runtime-manifest path/digest pair.
   A missing, duplicate, malformed, or mismatched item is rejected with a
   bounded diagnostic.  No last-known-good or packaged-CAR fallback occurs.
4. For a catalog profile, CBD performs this fetch only after exact
   Component/version/source selection, requires the URI to be the declared
   same-origin `repository/car/<artifact>/<version>/component-knowledge.json`
   route, and never derives a route from the CAR artifact.  On admission, CBD
   retains the typed consumer contract and the small declared
   carrier value (schema, logical path, and digest), never carrier bytes.  Detail,
   `getUsage`, and MCP project declared logical identities, paths, integrity,
   origin, size, license, disclosure, and authorization metadata.  They never
   project raw resource content, a physical path, credential, resolver output,
   executable operation, or execution authority.
5. The deterministic CAR Review provider consumes that admitted detail.  It
   supplies limited metadata checks and explicitly reports content- and
   BoK-dependent checks as Unknown; it does not read a manual, source file, or
   BoK publication.

The producer owns the declaration and digests.  CBD Support owns admission,
selection, public projections, and its CAR Review provider.  BoK remains an
independent, separately attributable source.

## Selection and Bounds

Carrier-backed detail becomes a profile only after exact Component, version,
kind, organization, and optional source selection.  A carrier-free or
rejected local observation remains an explicit absence and is never upgraded
by an ambient scan.  Public detail contains at most 100 resources and reports
the omitted count.  Usage cites only logical resource URIs whose admitted
availability is `available` and authorization is `granted`; it never derives
operations from a resource membership record.

## Security Boundary

The carrier is descriptive value evidence, not authority.  The consumer
contract cannot authorize an archive read, filesystem traversal, remote
fetch, process launch, secret resolution, component invocation, or MCP
operation.  Declaration paths are exact logical paths; symbolic links,
undeclared sibling files, and externally supplied paths are not inputs to this
feature.

Current coverage is an explicitly configured development directory, local/cache
CAR storage, and the explicit version-scoped published-catalog transport.  A
catalog entry without that transport remains carrier-absent; it must not be
replaced with archive guessing or a new ambient resolver.
