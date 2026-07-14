# Information Source Security Contract

## Authorization Boundary

Every Phase 3 input source receives authority from one explicit policy:

- the built-in simplemodeling.org catalog uses `built-in` authority;
- configured catalogs and BoK sites require `exact-origin-allowlist` authority;
- SIE BoK input requires its exact origin plus the fixed `/mcp` public
  `component-route-allowlist`;
- configured development directories use `explicit-path-allowlist`;
- default local/cache CAR roots use `canonical-storage-root`, while configured
  replacements use `explicit-path-allowlist`.

HTTP(S) source and allowlist values must be absolute, credential-free, and
free of query and fragment data. Allowlist entries are origins rather than
paths. A catalog-derived URI is not fetchable merely because its origin
matches: outbound sidecar fetches must also contain no user information.
Cross-origin and credential-bearing references remain unfetched evidence with
a sanitized diagnostic.

Local roots are converted to absolute normalized real paths before inspection.
Symbolic-link roots and paths whose real path contradicts the normalized path
are rejected. Directory discovery uses no-follow checks; a nested symbolic link
cannot turn an explicitly authorized CAR root into authority over another
directory. BoK resource paths likewise reject absolute, root-relative,
cross-origin, traversal, query-bearing, and fragment-bearing references.

## Diagnostic Boundary

Configuration rejection identifies an entry by source kind and ordinal rather
than echoing invalid input. Provider, transport, publisher, and HTTP failures
are sanitized before they enter source state or usage warnings:

- HTTP(S) user information, query data, and fragments are removed;
- bearer credentials, authorization values, and common secret assignments are
  replaced with `[redacted]`;
- control characters become spaces and repeated whitespace is collapsed;
- one diagnostic is bounded to 2048 characters plus a truncation marker.

Sanitization does not turn a rejected source or reference into authority. It
retains a credential-free origin/path and non-secret reason so operators can
identify the failing boundary.

## Executable Evidence

`InformationSourceSecuritySpec` verifies secret and URI redaction, bounded
diagnostics, provider-failure sanitation before unified source state,
credential-bearing same-origin sidecar rejection, credential-bearing SIE route
rejection, canonical roots, and nested local symlink escape prevention.
`CatalogRuntimeSpec`, `BokSourceRuntimeSpec`, `SieBokRuntimeSpec`, and
`LocalSourceRuntimeSpec` retain the source-specific exact-origin, traversal,
credential, canonical-path, and root-symlink cases.
