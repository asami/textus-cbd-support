---
status: stable
decision_scope: P10-10--P10-13
updated_at: 2026-09-27
---

# Internal-model Source Snapshots Design

## Purpose and authority flow

This design explains the authority and responsibility boundaries for the source
snapshot contract in [Internal-model Source Snapshot
Contract](../spec/internal-model-source-snapshot-contract.md). That specification
is normative. It extends the manifest-led portable package design in
[Internal-model Package Design](internal-model-package.md) without changing the
package's inventory or integrity responsibilities.

The source owner supplies the authoritative source identity, authority,
available revision, exact raw bytes, and any bounded locator. One snapshot
artifact records one exact contributing source's selected, self-contained basis
and provenance; it is never a merged multi-source winner. The Phase 10 manifest
binds each artifact's serialized bytes as one inventory item. These roles remain
separate:

| Responsibility | Owner and boundary |
| --- | --- |
| Meaning of Component, projection-context, element, and relationship identities | The source owner and the Phase 9 [Canonical Component Design Model Contract](../spec/canonical-component-design-model-contract.md). |
| Selected source identity, raw bytes, raw-byte hash, and availability | The source owner; a snapshot records rather than invents them. |
| Full normalized Scenario content, model assertion, or glossary/BoK definition necessary to resume the selected interpretation | The source snapshot artifact. It preserves selected meaning without replacing the raw source evidence. |
| Existing canonical CML target, source-owner-supplied project-relative path, and exact raw bytes | A read-only CML-baseline source snapshot; it neither creates nor changes CML. |
| Inventory role/path/dependencies and serialized artifact-byte integrity | The Phase 10 [Internal-model Package Contract](../spec/internal-model-package-contract.md) and its manifest. |
| Comparison against a live source | P10-13, which detects/reports freshness only. |
| Drift policy, invalidation/reconciliation, approval, and canonical CML application | Phase 10.5 alone. |

An external locator helps identify a bounded source location but is not a
credential, source ranking signal, equality proof, or access grant. An absent
locator or revision remains explicit. No consumer may replace it with a
plausible identifier, revision, source, or link.

The manifest can contain several `source-snapshot` artifacts. Path conventions
such as `snapshots/model-context/<artifactId>.json`,
`snapshots/glossary-bok/<artifactId>.json`, and
`snapshots/cml-baseline/<artifactId>.json` make the artifact kind legible but do
not authorize a source or determine semantic completeness. A later realization
or projection must use manifest `dependsOn` to identify its exact selected
snapshot artifact IDs. The inventory's closed file set by itself is not proof
that every used fact, definition, or CML target was captured.

## Selected Scenario representation

A selected Scenario is retained as a `source-snapshot` artifact because later
work must resume the same interpretation without relying on chat history,
provider state, or live source access. Its full selected content is normalized
only for line endings and is distinct from the exact raw bytes whose digest
records source provenance. The manifest artifact digest then protects the
canonical JSON serialization of that normalized representation.

Phase 9 trace links preserve navigation to exact Component, bounded
projection-context, and semantic element/relationship identities. They are not
display labels, a parallel semantic model, or a permission to reinterpret a
missing semantic fact. An empty link set makes trace absence visible; it never
permits a consumer to infer a relation from wording, ordering, layout, or
similarity.

The package snapshot is immutable for one package revision. When a source is
newer or differs, the workflow creates an explicit candidate package revision;
it does not silently overwrite the basis of an existing candidate.

## Model-context and glossary/BoK representation

A model-context artifact retains the nonempty source-owned context identity and
the exact nonempty set of source-attributed facts actually used by a candidate.
Each fact preserves its Component, projection-context, and semantic identity,
element-or-relationship kind, stable source anchor, complete source assertion,
and explicit limitations. The source assertion is retained verbatim apart from
line-ending normalization; it is not a label, summary, locator, or inferred
model. Facts are canonically ordered by their identity-and-anchor tuple.

Multiple claims remain distinct when their anchors differ, including conflicting
claims for the same semantic identity. Limitations remain attributed to their
source and an empty limitation list does not settle a conflict. This carries the
Phase 9 no-hidden-winner boundary into a portable artifact without replacing
CCDM source authority or creating another semantic model.

A glossary/BoK artifact retains the nonempty actual set of used definitions.
Each definition carries its source-owned term identity and source anchor, a
display-only term label, full source definition, and explicit limitations.
Entries sort by term identity and anchor. Definitions from different authorities
or revisions are distinct artifacts even when the spelling matches. An
unavailable, unauthorized, or ambiguous essential definition is visible as an
admission failure, or as a retained source-attributed limitation when a complete
definition can still be recorded; neither case permits a synthetic definition
or a hidden preferred authority.

## Exact CML baseline representation

A CML-baseline artifact records one existing project-owned canonical CML target
as exact raw bytes, with its nonnegative byte length, canonical padded RFC 4648
Base64 representation, and source-owner-supplied project-relative POSIX path.
The byte digest is calculated over those raw bytes before decoding, parsing,
normalization, or reformatting. The snapshot also keeps the source identity and
available revision as provenance separate from the target path.

The source owner, not a filename or the snapshot consumer, establishes that the
target is canonical. The target must be an existing readable regular file below
the consuming-project root; a symlink, path escape, nonregular file, absent
file, or unidentified/noncanonical target has an explicit failure condition and
cannot produce a V1 present-file baseline. This does not create an absence or
zero-byte baseline. A future creation/absence case needs a separately decided
contract.

The baseline is strictly read-only. Capturing it does not parse or rewrite CML,
grant CML mutation authority, or say that an application is safe. For a
candidate to make that latter claim in later work, every existing target it
might modify must have its own exact baseline artifact, and every declared used
fact and definition must be retained in a source-attributed artifact. An
unknown target set, omitted source, unresolved unavailable basis, or unbaselined
target leaves the candidate incomplete.

## Deterministic freshness comparison

P10-13 adds only a supplied-byte, supplied-observation comparison seam. It
first validates the entire closed canonical V1 snapshot, including all four
basis shapes and the CML Base64/length/raw-hash relationship, so that an
invalid baseline cannot gain meaning from a live source. That malformed
baseline outcome has precedence over an unavailable, unauthorized, malformed,
or ambiguous live observation.

For a valid baseline, one `Observed` value carries source-owned authority,
identity, explicit optional revision, and immutable exact raw bytes. The
comparison derives only the raw-byte SHA-256; it does not use a locator as an
access route or equality substitute. It retains all differences across source
authority, identity, revision including null/value transitions, and digest.
For CML, the observed safe project-relative path, byte length, and exact raw
bytes are also compared with the recorded read-only baseline. A non-CML source
cannot attach a CML path, and a CML comparison cannot proceed from a missing or
unsafe current path.

The report exposes status, source identity/provenance metadata, digests,
sorted dimension names, CML paths, and a non-byte-bearing evidence reason. It
does not carry the Scenario, model-context, glossary/BoK, or CML payloads, a
locator, or a credential. `Unavailable`, `Unauthorized`, `Malformed`, and
`AmbiguousOrConflicting` remain separate report states. `Changed` reports
detected divergence only; Phase 10.5 remains the owner of invalidation,
reconciliation, approval, and any canonical CML application.

This seam is deliberately pure. It does not open a project, inspect the
filesystem, contact a provider, choose an authority, mutate a snapshot, apply
CML, expose a route, or retain lifecycle state.

## Package freshness orchestration

P10-13 adds a deliberately narrow package entry around that unchanged pure
seam. It requests source-snapshot entries only from the one completed
project-bound manifest validation pass. The validator has already established
the closed manifest inventory, regular-file boundary, and every artifact digest
before exposing manifest-order entries; present entries carry the exact
verified bytes and optional absence carries no bytes. This avoids a competing
manifest parser, inventory walk, or integrity authority.

The entry returns one result for every manifest `source-snapshot` entry in that
same order. The caller/source owner supplies inputs keyed by artifact identity,
so input-map insertion order has no semantic effect. Unknown or non-source IDs
are rejected rather than interpreted. A present baseline without an input is
explicitly unavailable. An optional absent baseline is `MissingBaseline`, does
not invoke a source owner or CML reader, and is not turned into a synthetic
zero-byte baseline. Empty source-snapshot inventory is an empty report only;
the manifest's syntactic closure still does not establish semantic completeness.

Scenario, model-context, and glossary/BoK retain the existing closed caller
observation family. CML separates the observed request because a caller must
not inject CML bytes: its source owner supplies authority, identity, optional
revision, and the current project-relative path, while the entry reads that one
target under the consuming project root. Supplying an ordinary observed value
for CML, or a CML request for another snapshot kind, is malformed. Non-observed
CML statuses remain explicit and do not trigger a file read.

The CML reader first rejects unsafe paths and symbolic components, requires
containment below a non-symbolic project root and a final regular file, and
preserves the supplied absolute normalized root without resolving arbitrary
ancestors. Before descriptor descent it rewrites only the current macOS system
aliases `/var` to `/private/var` and `/tmp` to `/private/tmp`, and only when
each alias currently has exactly that target. A capable `SecureDirectoryStream`
provider starts at `/`, descends root and target-directory components with
no-follow links, and opens the final target through a no-follow relative
channel; an arbitrary symbolic ancestor or final component is rejected. If
that provider is unavailable on
macOS, the internal Darwin LP64 adapter loads the declared JNA 5.13.0 core
dependency lazily, acquires `/`, descends normalized-root and target components
by no-follow directory descriptors, opens the final target with no-follow,
close-on-exec, and nonblocking read flags, checks its `fstat` regular-file type
before byte consumption, retries only interrupted safe calls, and closes every
descriptor while preserving the initial outcome. An unsupported provider or
platform, incompatible ABI, or native failure remains fail-closed; there is no
pathname check-then-open fallback.

Its only outputs are the existing typed availability state or the bytes and
source-owner metadata passed to the pure comparator. The recorded CML basis
path and source locator are never read authority. Changed target paths remain
comparison evidence even where their bytes are equal. The report contains no
raw bytes, locator, or credential.

## Failure and lifecycle separation

Canonical byte and structural failures stop source-snapshot admission rather
than being repaired. The same applies to a wrong kind/basis shape, missing or
empty required fact or entry, synthetic identity, noncanonical ordering or
duplication, invalid text normalization, Base64, byte length, raw hash, or
target path. Unavailable or unauthorized source authority, identity, raw bytes,
or essential content is visible as an explicit condition, never converted to an
inferred value. This preserves the Phase 9 no-hidden-winner and explicit
availability boundaries.

Passing the structural contract means only that a particular captured artifact
is well formed and package-bound. It does not prove that the snapshot is fresh,
that source assertions are semantically approved, that a CML change is safe, or
that any CML change has been authorized. P10-13 compares each source's
authority, identity, available revision, and raw hash with its own live source;
for CML it also compares the declared target path and raw bytes or absence. It
reports changed, unavailable, unauthorized, malformed, ambiguous/conflicting,
or unchanged observations without ranking sources or deciding their disposition.
Phase 10.5 separately decides whether detected drift invalidates, can be
reconciled, or can ever precede an approved canonical CML application.

This P10-10--P10-12 design deliberately did not create a parser, validator, or
live comparator. P10-13 adds the pure supplied-byte/supplied-observation
comparator and its validated package orchestration described above; it does not
add a transport runtime, retained-state record, CML mutation, approval
mechanism, automatic adoption, or new-file absence baseline. It is the
source-snapshot basis for Phase 10.1, whose checklist and status remain
[OPEN](../phase/phase-10.1-checklist.md) until the complete Phase evidence is
recorded.
