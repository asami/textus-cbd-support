# Phase 8 P8-44 CAR Evolution View Completion

date=2026-07-24
phase=Phase 8
checklist=P8-44
status=completed

`CarReviewEvolutionProjection` is the CBD-owned, read-only evolution View. It
accepts exactly two immutable canonical Report snapshots supplied by an Entity
Aggregate adapter. It rejects a lineage, configuration-compatibility, or CAR
identity mismatch before projecting a result.

The resulting delta preserves each Report, target version/digest, and gate
identity while making sorted Observation (including Finding) and capability
assessment additions, removals, and changes explicit. It does not read an
Entity or DataStore, enumerate history, access the filesystem, or execute a
provider.

The bounded authorized selection of those two snapshots, retention/expiry, and
MCP enumeration boundary remain P8-45 responsibilities. The View therefore
does not become a second persistence or authorization implementation.
