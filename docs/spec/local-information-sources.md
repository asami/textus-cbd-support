# Local Information Source Contract

## Stable Semantics

Local inspection is explicit, read-only, and bounded. It does not scan the
user's home directory or sibling repositories implicitly.

Development directories are admitted only from an explicit configuration list.
Each entry becomes a `development-directory` source authorized by
`explicit-path-allowlist`. The configured root must exist, be a directory, and
have no symbolic-link root or canonical-path redirection. The adapter issues a
bounded exact-leaf `project.yaml` query and records every returned descriptor's
component identity and version as `working` evidence. The query returns only
logical relative paths and bytes, never the configured physical root; it does
not follow symbolic links. Development-source IDs must be unique and cannot
reuse the stable `local-car` or `cache-car` IDs.

CAR storage uses two distinct sources:

- `local-car` reads `<local-root>/repository/car` and records
  `local-published` state;
- `cache-car` reads `<cache-root>/car` and records `cached` state.

The canonical roots are `~/.cncf/local` and `~/.cncf/cache`; an explicitly
provided equivalent root is authorized as an explicit path. Discovery does not
follow symbolic links and is bounded by artifact count, directory count,
entries per directory, depth, metadata byte size, and artifact bytes read for
checksum calculation.

No-follow checks apply to every discovered entry as well as the configured
root. A nested symbolic link to an artifact outside the authorized root is
ignored and cannot become an observation.

Each returned inventory records the completion time as `observedAt`. The local
adapter keeps no snapshot cache or last-known-good fallback: every invocation
performs a new bounded, read-only inspection, so an earlier inventory is never
relabeled as current after local files change or become unavailable.

Reaching any discovery bound, including the maximum directory depth, produces a
truncation warning. SHA-256 calculation reads at most the configured artifact
byte limit plus one byte used only to detect overflow, even if a mutable artifact
grows after inspection begins.

Every CAR observation retains:

- storage source and state;
- descriptor component name and version when present;
- repository-path component name and version as separate evidence;
- the selected observed version and whether it came from the descriptor or
  repository path;
- artifact URI and computed SHA-256;
- missing, unreadable, or conflicting evidence diagnostics.

`component-descriptor.json` is authoritative when it contains a version. Older
CARs without a descriptor version may retain the repository-path version as
explicit `repository-path` evidence, but the adapter reports that the descriptor
did not establish that version. This is the only supported legacy CAR fallback.
A missing or malformed descriptor, a descriptor without component identity, or
a descriptor/path component or version conflict rejects the artifact and emits
a bounded diagnostic; the adapter does not guess from the path or select one
side of contradictory evidence. A local or cached artifact is not treated as a
remote publication or recommendation.

The version-state reconciliation contract preserves these storage states as
availability evidence and derives snapshot, release, unknown, or conflicting
maturity independently. It does not rewrite local state from a catalog
observation with the same component and version identity.

## Configuration Boundary

The pure parser accepts already-resolved runtime settings for development
directories and optional local/cache roots. The default runtime loads those
settings from `TEXTUS_CBD_DEVELOPMENT_DIRECTORIES`,
`TEXTUS_CBD_LOCAL_CAR_ROOT`, and `TEXTUS_CBD_CACHE_CAR_ROOT`, then applies the
same parser and inspection policy. Development-directory source IDs cannot
collide with any configured catalog, BoK, SIE, local, or cache source ID.

Retrieval readiness performs the bounded inspection. Source-aware search
projects local observations separately from catalog profiles, while
`listCatalogs` and `status` expose per-source observation count, `observed`
freshness, and source-specific diagnostics. The runtime does not retain a
last-known-good local inventory.

## Executable Evidence

`LocalSourceRuntimeSpec` covers canonical path authorization, symlink rejection,
working `project.yaml` evidence, local/cache state separation, descriptor/path
version separation, the supported missing-descriptor-version transition,
malformed/conflicting CAR rejection, artifact checksums, and bounded discovery
diagnostics.
`LocalSourceRuntimeQuerySpec` proves that every bounded nested `project.yaml`
query result becomes an independent logical working observation.
`VersionStateReconciliationSpec` covers the independent availability and
maturity projection of these local observations.
`SourceAwareRetrievalSpec` covers runtime configuration integration, source
state projection, filters, and the no-fabricated-profile boundary.
