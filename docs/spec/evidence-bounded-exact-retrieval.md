# Evidence-Bounded Exact Retrieval Contract

## Exact Candidate Selection

`getComponent`, `getUsage`, and `resolveDependencies` apply the same exact
component identity, organization, kind, version, and optional catalog-ID
constraints. The resulting catalog candidate set has three interpretations:

- zero candidates returns `no-match`, no selected profile or reference, and a
  `component-not-found` absence;
- one candidate returns `matched` and that candidate alone is selected;
- multiple candidates returns `ambiguous`, no selected profile or reference,
  and an `ambiguous-selection` absence.

Catalog priority orders alternatives for stable presentation but never selects
one of several exact candidates. An explicit `catalogId`, or stricter identity
constraints that leave one candidate, is required before detail, usage, or
dependency evidence is read.

## Bounded Alternatives

An ambiguous response includes at most 20 `ComponentReference` alternatives.
`candidateCount` reports the full number before truncation, and a warning states
when only the bounded prefix is returned. The ambiguity absence cites the
source IDs, selected versions, and catalog evidence URIs of that same bounded
alternative set. No unbounded citation list or fields merged from alternatives
become a synthetic profile.

## Explicit Absence

`ComponentEvidenceAbsence` has a stable code, subject, message, source IDs,
versions, and evidence URIs. The Phase 3 codes are:

- `component-not-found` and `ambiguous-selection` for exact selection;
- `source-attribution-absent` and `selected-version-absent` for usage identity;
- `operation-evidence-absent`, `intent-rejected`, and `intent-match-absent` for
  usage guidance;
- `dependency-metadata-absent` for a selected version whose catalog does not
  publish dependency metadata.

An empty dependency array is authoritative only when
`dependencyMetadataVersion` identifies the selected version. Otherwise the
empty response is accompanied by `dependency-metadata-absent`. Likewise, no
inferred usage operation is not silently interpreted as authoritative advice:
the applicable usage absence is returned separately from observed facts.

## Ownership Boundary

Exact retrieval alternatives are catalog-backed references. Development,
local-published, and cached observations remain source-aware search evidence;
they are not promoted into catalog profiles by exact retrieval. BoK and SIE
semantic evidence cannot fill a missing operation, dependency, version, or
catalog selection.

## Executable Evidence

`EvidenceBoundedSelectionSpec` verifies ambiguous selection without a priority
winner, explicit catalog selection, bounded alternatives with a full count,
not-found absence, and missing dependency metadata. `IntentAwareUsageGuidanceSpec`
verifies operation, intent, source, and input-bound absences.
`ComponentFactorySpec` verifies the machine-readable absence projection.
