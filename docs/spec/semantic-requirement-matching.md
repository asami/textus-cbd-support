# Semantic Requirement Matching Contract

## Evidence Separation

`CbdRetrieval.searchComponents` returns BoK requirement evidence in the
independent `semanticEvidence` collection. A semantic citation identifies its
source, source kind, term, title/definition/category when published, aliases,
dataset when supplied by SIE, match classification, score, rationale,
freshness, observation time, and exact evidence URI.

The citation does not become a component observation and does not complete a
catalog profile or local CAR observation. Component versions, runtime ranges,
dependencies, operations, artifacts, checksums, and usage statements remain
CBD catalog/local evidence only.

## Matching Authority

Configured BoK-site terms are matched locally using only bounded term identity,
title, slug, reading, alias, summary, and requirement tokens. Exact label or
alias equality is `exact`; otherwise the score is the fraction of requirement
tokens explicitly present in the published term metadata. The rationale says
which lexical evidence matched. This is deterministic CBD matching, not model
inference.

SIE evidence is accepted only from the successful response for the current
query. CBD preserves SIE's term identity, definition, dataset, `match_kind`,
score, rationale, and evidence URI without recomputing them. A retained SIE
snapshot from another query remains source-state evidence only and cannot be a
requirement citation.

## Component Association

A catalog component may cite semantic evidence only when one of its explicitly
published `terms` or `tags` equals the citation term ID, title, or alias after
case/whitespace normalization. Semantic evidence can therefore discover a
catalog profile when the raw requirement has no direct catalog token match,
but the returned profile remains catalog-owned.

`ComponentMatch.semanticEvidenceIds` lists the exact independent citations
used by that match. No fuzzy component-to-term relation is inferred. Local
development and CAR-storage observations do not acquire semantic fields or a
fabricated component profile.

The response limit bounds semantic citations to 1 through 100. BoK-site
citations report `fresh` or `stale` from their source snapshot lifetime. A
current SIE response reports `observed` freshness.

## Executable Evidence

`SemanticRequirementMatchingSpec` covers BoK-site lexical evidence, SIE-owned
semantic evidence, query-scope isolation, freshness, explicit catalog-term
association, citation IDs, and the no-profile-merging boundary.
