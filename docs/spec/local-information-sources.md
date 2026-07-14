# Local Information Source Contract

## Stable Semantics

Local inspection is explicit, read-only, and bounded. It does not scan the
user's home directory or sibling repositories implicitly.

Development directories are admitted only from an explicit configuration list.
Each entry becomes a `development-directory` source authorized by
`explicit-path-allowlist`. The configured root must exist, be a directory, and
have no symbolic-link root or canonical-path redirection. The adapter reads only
a bounded `project.yaml` in this slice and records its component identity and
version as `working` evidence. Development-source IDs must be unique and cannot
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
did not establish that version. A local or cached artifact is not treated as a
remote publication or recommendation.

The version-state reconciliation contract preserves these storage states as
availability evidence and derives snapshot, release, unknown, or conflicting
maturity independently. It does not rewrite local state from a catalog
observation with the same component and version identity.

## Configuration Boundary

The parser accepts already-resolved runtime settings for development directories
and optional local/cache roots. Reading CNCF runtime configuration and exposing
the inventory through search/MCP are later integration work; the adapter does
not access environment variables directly.

## Executable Evidence

`LocalSourceRuntimeSpec` covers canonical path authorization, symlink rejection,
working `project.yaml` evidence, local/cache state separation, descriptor/path
version separation, artifact checksums, and bounded discovery diagnostics.
`VersionStateReconciliationSpec` covers the independent availability and
maturity projection of these local observations.
