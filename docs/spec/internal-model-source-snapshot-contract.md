---
status: stable
decision_scope: P10-10--P10-13
updated_at: 2026-09-27
---

# Internal-model Source Snapshot Contract

This is the normative V1 contract for an internal-model source-snapshot
artifact. It defines one common envelope and the closed Scenario,
model-context, glossary/BoK, and CML-baseline bases for P10-10--P10-12. P10-13
defines the deterministic pure freshness comparison of one supplied V1
snapshot with one source-owned live observation.

The [Internal-model Package Contract](internal-model-package-contract.md)
remains authoritative for the package boundary, artifact inventory, and
manifest digest. The [Canonical Component Design Model
Contract](canonical-component-design-model-contract.md) remains authoritative
for the meaning and source attribution of Component, projection-context, and
semantic identities. The Phase 10.1 [source-snapshot and freshness
scope](../phase/phase-10.1.md) owns this contract's Phase boundary.

## 1. Scope and authority

A source snapshot is one `source-snapshot` artifact in the Phase 10 manifest
inventory. A Scenario artifact is conventionally `snapshots/scenario.json`;
model-context, glossary/BoK, and CML-baseline artifacts may conventionally use
`snapshots/model-context/<artifactId>.json`,
`snapshots/glossary-bok/<artifactId>.json`, and
`snapshots/cml-baseline/<artifactId>.json` respectively, subject to the
Internal-model Package Contract path rules. A path convention is not a second
authority. The manifest permits more than one `source-snapshot` artifact and
remains the only package inventory and exact artifact-byte integrity envelope.

One artifact represents one exact contributing source. It SHALL retain that
source's own identity and raw hash; artifacts from distinct sources SHALL NOT
be merged into a preferred or composite source. Its manifest `role`, `path`,
`sha256`, and `dependsOn` obey the Internal-model Package Contract.

Manifest validity proves neither source authenticity nor source freshness. A
snapshot records the selected source basis for one package revision. It is
immutable within that revision: replacing or refreshing it requires an explicit
new package revision. A consumer SHALL NOT silently rebase a candidate to a
different source revision, identity, raw hash, or selected content.

This contract records source-owned claims; it does not grant authority over a
source, rank sources, manufacture a missing identity or trace link, authorize
access through a locator, approve semantic meaning, or authorize CML mutation.

## 2. Canonical V1 artifact bytes

The V1 snapshot artifact is canonical UTF-8 JSON. Its supplied bytes SHALL:

1. contain no byte-order mark;
2. contain no insignificant whitespace;
3. use object member keys in ascending UTF-8-byte order at every depth;
4. use only JSON punctuation and string escaping required to represent their
   values;
5. contain no duplicate object member name at any depth; and
6. end in exactly one LF (`0x0A`) with no other trailing byte.

A validator SHALL parse without first repairing, normalizing, or reserializing
the supplied bytes. It SHALL accept only when the parsed value, serialized
under these rules, has bytes exactly equal to the supplied bytes. A BOM,
malformed JSON, duplicate member, noncanonical key order, optional whitespace,
or any other byte difference is rejected rather than repaired.

V1 has exactly these top-level fields. Their required canonical byte order is
`basis`, `schemaVersion`, `snapshotKind`, `source`.

| Field | V1 requirement |
| --- | --- |
| `schemaVersion` | Exactly the JSON string `"1.0"`. Any other version is rejected. |
| `snapshotKind` | Exactly one of `"scenario"`, `"model-context"`, `"glossary-bok"`, or `"cml-baseline"`. |
| `source` | The exact common source envelope in section 3. |
| `basis` | The exact closed basis schema selected by `snapshotKind` in sections 4--7. |

Unknown, omitted, or additional top-level fields are rejected. The same closed
field rule applies to every object defined by this contract.

## 3. Common source envelope

The `source` object has exactly these fields in this canonical byte order:
`authority`, `identity`, `locator`, `revision`, `sha256`.

| Field | V1 requirement |
| --- | --- |
| `authority` | A nonempty source-owned opaque identifier. It identifies the authority that owns the source claim. |
| `identity` | A nonempty source-owned opaque identifier for the selected source. It is never inferred, reconstructed, or replaced by a consumer. |
| `locator` | Either `null`, when the source-owned reference is unavailable, or a nonempty bounded source-owned opaque reference. It is provenance/navigation only: it is neither a credential nor an automatic access grant. |
| `revision` | Either a nonempty source-owned opaque revision string or explicit `null` when the source does not supply a revision. A consumer SHALL NOT infer one. |
| `sha256` | The lowercase token `sha256:` followed by exactly 64 lowercase hexadecimal characters. It is SHA-256 of the live selected source's exact raw bytes. |

`source.sha256` is not the hash of the snapshot artifact. It is calculated from
the raw source bytes before text decoding, line-ending conversion, Unicode
normalization, parser reserialization, or JSON escaping. Even when a source
lacks a revision, its available raw bytes require this baseline digest.

An unavailable locator or revision is represented only by its explicit `null`.
When source authority, source identity, or the raw bytes needed for
`source.sha256` are unavailable, the source evidence is unavailable and no
conforming selected-source snapshot can be admitted. The failure SHALL be
reported explicitly; a consumer SHALL NOT use a null, guessed, replacement, or
locator-derived identity, revision, or digest.

## 4. Selected Scenario basis

For `snapshotKind` `"scenario"`, `basis` has exactly these fields in this
canonical byte order: `content`, `scenarioId`, `traceLinks`.

| Field | V1 requirement |
| --- | --- |
| `scenarioId` | The nonempty source-owned opaque identity of the selected Scenario. |
| `content` | A JSON string containing the full selected Scenario content required to resume interpretation, not a locator or summary. Before JSON serialization, normalize only CRLF and bare CR to LF. Reject a BOM; preserve every other Unicode code point and whitespace without semantic rewriting. JSON escapes this normalized content only as required by JSON. |
| `traceLinks` | An array of exact trace-link objects. `[]` is permitted and explicitly means that no traceability is asserted; it does not invite inferred traceability. |

The normalized `basis.content` is self-contained interpretation material. It is
intentionally different from the raw source bytes represented by
`source.sha256`. The manifest inventory's artifact `sha256`, in turn, binds the
complete serialized snapshot bytes, including the JSON representation of this
normalized content.

Each `traceLinks` element has exactly these fields in this canonical byte order:
`componentIdentity`, `projectionContextIdentity`, `scenarioAnchor`,
`semanticIdentity`, `semanticIdentityKind`.

| Field | V1 requirement |
| --- | --- |
| `scenarioAnchor` | A nonempty source-owned stable Scenario step or section identity. |
| `componentIdentity` | The nonempty exact Component identity owned by the source/Phase 9 CCDM boundary, not a display label. |
| `projectionContextIdentity` | The nonempty exact bounded projection-context identity owned by the source/Phase 9 CCDM boundary, not a display label. |
| `semanticIdentityKind` | Exactly `"element"` or `"relationship"`. |
| `semanticIdentity` | The nonempty exact Phase 9 CCDM element or relationship identity for the declared kind, not a display label. |

Trace links are unique by the complete tuple
`(scenarioAnchor, componentIdentity, projectionContextIdentity,
semanticIdentityKind, semanticIdentity)`. The array SHALL be sorted
lexicographically by that tuple, comparing each string by ascending UTF-8-byte
order. A trace link is structurally an exact recorded identity tuple; its truth,
authority, and semantic meaning remain with its source and the Phase 9 CCDM
contract.

## 5. Model-context basis

For `snapshotKind` `"model-context"`, `basis` has exactly these fields in this
canonical byte order: `contextIdentity`, `facts`. `contextIdentity` is the
nonempty source-owned identity of the selected model context, never a display
name. `facts` is a nonempty array containing exactly the source-attributed
semantic assertions actually used by the selected candidate. It includes every
material conflict or limitation that the selected source reports; it neither
invents a missing fact nor resolves a conflict.

Each fact has exactly these fields in this canonical byte order:
`componentIdentity`, `content`, `limitations`, `projectionContextIdentity`,
`semanticIdentity`, `semanticIdentityKind`, `sourceAnchor`.

| Field | Model-context fact requirement |
| --- | --- |
| `componentIdentity` | A nonempty exact Component identity owned by the source/Phase 9 CCDM boundary. |
| `content` | The complete selected source assertion or passage needed to resume interpretation, verbatim except CRLF and bare CR are normalized to LF and a BOM is rejected. It is not a paraphrase, label, locator, or inferred semantic model. |
| `limitations` | An explicit array of nonempty source-attributed qualifier or uncertainty text, unique and sorted by ascending UTF-8 bytes. `[]` means only that this source reports no limitation; it does not resolve a known conflict. |
| `projectionContextIdentity` | A nonempty exact Phase 9 projection-context identity. |
| `semanticIdentity` | A nonempty exact Phase 9 semantic identity. |
| `semanticIdentityKind` | Exactly `"element"` or `"relationship"`. |
| `sourceAnchor` | A nonempty stable source-owned identity for the asserted passage. |

Facts are unique and sorted by
`(componentIdentity, projectionContextIdentity, semanticIdentityKind,
semanticIdentity, sourceAnchor)`, comparing strings by ascending UTF-8 bytes.
Genuinely distinct claims for one semantic identity remain distinct when their
anchors differ. The snapshot neither chooses a winner nor creates a separate
semantic model. If the full necessary assertion or any required identity is
unavailable, admission is rejected and the unavailable evidence is reported;
it is never fabricated.

## 6. Glossary/BoK basis

For `snapshotKind` `"glossary-bok"`, `basis` has exactly one field,
`entries`. It is a nonempty array of the terms and definitions actually used by
the selected candidate, retaining every materially relevant alternate or
conflicting definition and limitation.

Each entry has exactly these fields in this canonical byte order: `definition`,
`limitations`, `sourceAnchor`, `termIdentity`, `termLabel`.

| Field | Glossary/BoK entry requirement |
| --- | --- |
| `definition` | The full selected source definition needed to resume meaning, verbatim except CRLF and bare CR are normalized to LF and a BOM is rejected. It is not a summary or semantic rewrite. |
| `limitations` | An explicit array of nonempty source-attributed qualifier or uncertainty text, unique and sorted by ascending UTF-8 bytes; it follows the model-context fact rule. |
| `sourceAnchor` | A nonempty stable source-owned identity for the definition. |
| `termIdentity` | A nonempty source-owned stable term identity and the equality key, not inferred from spelling. |
| `termLabel` | Nonempty display text only; it is never an identity or equality key. |

Entries are unique and sorted by `(termIdentity, sourceAnchor)`, comparing
strings by ascending UTF-8 bytes. Different glossary or BoK authorities or
revisions are separate artifacts; identical spelling never merges them. An
essential definition that is missing, unavailable, unauthorized, or ambiguous
is an explicit admission failure unless an actual source-attributed definition
and its condition can be retained as a limitation. A consumer SHALL NOT create
a synthetic definition or hide a preferred source.

## 7. CML-baseline basis

For `snapshotKind` `"cml-baseline"`, `basis` has exactly these fields in this
canonical byte order: `byteLength`, `projectRelativePath`, `rawBytesBase64`.
It captures one existing, project-owned canonical CML target as a read-only
exact-byte baseline.

| Field | CML-baseline requirement |
| --- | --- |
| `byteLength` | A nonnegative integer equal to the number of decoded raw bytes. |
| `projectRelativePath` | The nonempty source-owner-supplied project-relative POSIX path of the target. It uses safe segments under the consuming-project root: it has no absolute path, empty, `.` or `..` segment, backslash, control or whitespace character, or symlink/path escape. A filename alone never establishes CML authority. |
| `rawBytesBase64` | Canonical RFC 4648 padded Base64 with no whitespace or alternate alphabet. Its decoded bytes have `byteLength` and SHA-256 exactly equal to `source.sha256`. |

The source owner establishes that the supplied path is the canonical CML target.
The snapshot preserves the target's exact raw bytes, path, source identity, and
available revision separately; it SHALL NOT decode, normalize, reformat, or
write CML before calculating the digest or as a result of this snapshot. A
symlink, nonregular, unreadable, absent, noncanonical, or unidentified target
cannot yield a V1 present-file baseline. That condition is explicit and the
candidate cannot claim safe application. A future creation or absence case
requires a separate explicit baseline contract; it SHALL NOT be invented as a
zero-byte present file.

## 8. Candidate completeness and later comparison

Completeness for an actual candidate is relative to its declared consumed model
facts, term definitions, and CML mutation-target paths. Every used fact and
definition SHALL be retained in a source-attributed artifact, and every
existing CML target the candidate might modify SHALL have an exact baseline
artifact. An unknown target set, omitted source, unresolved unavailable basis,
or unbaselined target makes the candidate incomplete and unable to claim safe
application. Manifest inventory closure alone never proves that semantic
completeness.

The later realization or projection artifact must reference the exact selected
snapshot artifact IDs through the Phase 10 manifest `dependsOn` relation. This
is a prerequisite only: P10-10--P10-12 do not implement the later Phase 10.3
or Phase 10.4 realization/projection decisions.

P10-13 compares every artifact's source `authority`, `identity`, source-owned
`revision` when available, and exact raw-source hash against its own live
source. For CML it also compares the declared project-relative target path and
raw bytes or absence. A locator alone is never equality evidence. The
comparator distinguishes changed, unavailable, unauthorized, malformed,
ambiguous/conflicting, and unchanged observations, but it does not select a
source, silently rebase a candidate, apply CML, or decide drift disposition.

## 9. P10-13 deterministic freshness comparison

`InternalModelSourceSnapshotFreshness.compare` accepts supplied snapshot bytes
and exactly one `InternalModelLiveSourceObservation`. It is a pure comparison:
it performs no network access, project or CML file read, file write, source
ranking, CML application, package-state update, approval, or drift
disposition. The caller supplies the live observation under that source's own
authority; the comparator neither derives an observation from a locator nor
manufactures missing source evidence.

Before considering the observation, the comparator SHALL validate the complete
V1 snapshot contract in sections 2--7. It SHALL reject malformed UTF-8/JSON,
duplicate or unknown members, noncanonical bytes, wrong schema or kind, closed
source/basis violations, non-normalized content or definition text,
noncanonical ordering/duplication, and invalid CML path, Base64, length, or
raw digest. A malformed baseline takes precedence over every live observation
variant. The comparator SHALL not repair, normalize, reserialize, or otherwise
turn an invalid supplied snapshot into an accepted baseline.

The closed live-observation family has one `Observed` form and distinct
`Unavailable`, `Unauthorized`, `Malformed`, and `AmbiguousOrConflicting`
forms. `Observed` retains source-owned authority and identity, an explicit
optional revision, immutable exact raw bytes, and an optional
project-relative CML target path. Its source authority and identity and any
present revision are nonempty; its raw bytes are never replaced by a locator or
inferred value. A non-CML observation SHALL NOT claim a CML target path. A CML
observation SHALL supply one safe current project-relative CML path and its
exact current raw bytes. The other forms preserve their own nonempty evidence
reason or ambiguity and never invent an `Observed` value.

The immutable freshness report contains a closed status vocabulary of
`Unchanged`, `Changed`, `Unavailable`, `Unauthorized`, `Malformed`, and
`AmbiguousOrConflicting`; optional snapshot kind; baseline and observed source
authority, identity, and revision; baseline and observed raw-byte SHA-256;
sorted changed-dimension names; optional baseline/current CML paths; and an
optional non-byte-bearing evidence reason. It SHALL NOT retain or expose raw
Scenario, model-context, glossary/BoK, or CML content, a source locator, or a
credential.

For an `Observed` value, comparison SHALL retain every difference rather than
short-circuiting. It compares `source.authority`, `source.identity`,
`source.revision` including null/value transitions, and `source.sha256` over
the exact current raw bytes. For a CML baseline it additionally compares
`basis.projectRelativePath`, `basis.byteLength`, and the exact decoded
`basis.rawBytesBase64` bytes. A same raw hash with changed identity or path is
still `Changed`; a same hash is never a substitute for exact CML byte equality.
The changed-dimension names are lexicographically sorted and `Unchanged` is
returned only when their set is empty. `Unavailable`, `Unauthorized`,
`Malformed`, and `AmbiguousOrConflicting` retain their distinct status rather
than being treated as unchanged. Their reason is evidence only, never an
approval, source selection, or drift disposition.

### Package-wide checked inventory

The package-wide freshness entry SHALL obtain its inventory only from the
Phase 10 manifest validator's one successful project-bound structural,
filesystem, and digest-validation pass. That pass returns the manifest-order
`source-snapshot` entries only: each present entry carries the exact verified
serialized bytes used for its artifact digest check, and an absent optional
entry carries no bytes. A consumer SHALL NOT reparse the manifest, rebuild an
inventory from paths, or perform a second integrity authority pass.

The entry accepts the consuming project root and one caller/source-owner input
per `source-snapshot` artifact ID. Unknown IDs and IDs for non-source artifacts
are rejected. A present baseline with no input yields that snapshot's
`Unavailable` freshness report; an absent optional baseline yields the distinct
`MissingBaseline` package result and performs no source or CML read. A package
with no `source-snapshot` entries returns an empty deterministic report. It
does not thereby claim semantic completeness, source freshness, approval, or
safe application.

For Scenario, model-context, and glossary/BoK entries, the caller supplies the
closed P10-13 observation family. For CML, a caller-supplied `Observed` value
with raw bytes is not accepted: the source owner instead supplies authority,
identity, explicit optional revision, and the current project-relative target
path. `Unavailable`, `Unauthorized`, `Malformed`, and
`AmbiguousOrConflicting` remain explicit CML observation states. A CML request
against a non-CML baseline, or an ordinary supplied `Observed` value against a
CML baseline, produces `Malformed` rather than selecting or reading a source.
Malformed baseline bytes retain precedence over every input form.

Before a CML read, the entry SHALL require the current source-owner target path
to be nonempty and safe under the consuming-project root: no absolute,
dot/dotdot, backslash, control, or whitespace segment; no symbolic link in an
existing component; no root escape; and a final regular file only. It reads the
final file's exact bytes without following its final symlink. A missing target
is `Unavailable`; denied access is `Unauthorized`; an unsafe, symbolic,
nonregular, or otherwise unreadable-safe target is `Malformed`. The recorded
baseline `basis.projectRelativePath` and provenance `locator` are comparison
evidence only and SHALL NOT discover or authorize the live target. Only the
captured bytes and source-owner metadata reach the pure comparator, whose
report never exposes those bytes, a locator, or credentials.

The consuming-project root's final component SHALL first be a non-symbolic
directory. The entry preserves the supplied absolute normalized root without
resolving arbitrary ancestors. Before descriptor descent it rewrites only the
current macOS system aliases `/var` to `/private/var` and `/tmp` to
`/private/tmp`, and only when each alias currently has exactly that target. A
capable `SecureDirectoryStream` provider starts from `/`, descends every
root-and target-directory component with `NOFOLLOW_LINKS`, and opens the final
target by a no-follow relative channel; an arbitrary symbolic ancestor or
final component is rejected. On macOS where that provider is
unavailable, the internal Darwin adapter uses the declared
`net.java.dev.jna:jna:5.13.0` dependency lazily and only on that platform. It
requires the Darwin LP64 ABI, acquires `/`, descends the normalized root and
target by descriptor using no-follow directory opens, opens the final target
with no-follow, close-on-exec, nonblocking read flags, verifies its `fstat`
regular-file type before consuming bytes, and closes every descriptor. An
unsupported provider or platform, incompatible ABI, or native-read failure is
fail-closed; it SHALL NOT fall back to pathname check-then-open reading.

## 10. Structural outcomes and freshness boundary

Structural validation is fail-closed. The following are observable rejected
outcomes, and no validator or consumer may repair an input into acceptance:

| Condition | Required outcome |
| --- | --- |
| Non-JSON, malformed canonical bytes, BOM, duplicate field, unknown field, missing field, or noncanonical key/order/whitespace form | Reject as a malformed or noncanonical source-snapshot artifact. |
| Invalid kind, wrong basis shape, invalid digest, synthetic or invalid required identity, locator, or revision | Reject as an invalid source-envelope or kind-basis field. |
| Empty or missing required model fact, glossary/BoK entry, selected content, definition, identity, or source anchor | Record unavailable or invalid source evidence and reject admission; never guess or substitute it. |
| Duplicate or noncanonically ordered trace link, model fact, glossary/BoK entry, or limitation | Reject as noncanonical basis data. |
| Invalid text normalization, Base64, CML byte length, raw hash, target path, target ownership, or target file kind | Reject the baseline or source snapshot; do not repair, decode into a replacement, or create an absence baseline. |
| No source authority/identity, no raw source bytes/hash, unavailable source, or unauthorized required source evidence | Record the explicit condition and reject admission. |

Structural validity proves only that one immutable artifact has the prescribed
bytes and closed shape. Self-contained interpretation additionally requires
the full Scenario content, model assertion, or definition that the candidate
actually uses, and CML safety additionally needs an exact present-file baseline.
None of these properties proves freshness, semantic approval, CML authority,
or repository acceptance.

## 11. Approval and Phase boundaries

P10-10 captures Scenario schema and provenance; P10-11 captures model-context
and glossary/BoK bases; P10-12 captures read-only exact CML baselines; and
P10-13 defines the deterministic supplied-observation comparison in section 9.
The pure comparison performs no live source fetch, package-wide enumeration,
source ranking, snapshot or CML write, approval, or drift disposition. The
P10-13 package entry adds only the validated-inventory orchestration and
project-bound current CML read specified above; its internal local filesystem
adapters do not add a remote source adapter, transport, credential, retained
state, write, or automatic adoption.
Later Phase 10 work owns durable semantic state, rehydration, review, approval,
and validation.
Phase 10.5 alone owns drift policy, invalidation/reconciliation, human approval,
and any canonical CML application boundary.

Accordingly, baseline review inputs CB-CBD-001 through CB-CBD-004 are preserved
as follows: freshness is detected separately from drift disposition; this
contract grants no CML mutation authority; canonical serialization outcomes are
fixed above; and acceptance depends on the reproducible Step and final Phase
evidence recorded in the [Phase 10.1 checklist](../phase/phase-10.1-checklist.md),
not on a prose claim in this contract. The checklist remains open until that
later evidence exists.
