# CAR Review managed effect diagnostic

Status: normative; bounded Phase 16 early slice, acceptance pending

## R1 — Ordinary typed operation and provider

`CbdSupport.CbdReviewAdmin.reviewManagedEffects` is a QUERY on the existing
private, non-MCP service. Normal CncfMain command selection uses the FQN:

```text
CbdSupport.CbdReviewAdmin.reviewManagedEffects --reviewId=<review-id> --revision=<positive-long> --subjects=<structured-subjects>
```

`subjects` is the normal structured sequence of generated
`ManagedEffectReviewSubject` records, never a JSON-string DTO field. Use the
normal host's structured request binding for this sequence. The QUERY action
envelope is separate from `value.ManagedEffectReviewRequest`; the ActionCall
uses that value's ordinary `createC(record)` and returns
`ManagedEffectReviewResult.toRecord()`.

`CarReviewManagedEffectProvider.reviewC(core, input)` executes a fixed
`ProviderCall` through `ProviderEngine.execute`. Its program owns one
`provider_step`. The existing CNCF UnitOfWork interpreter evaluates that
diagnostic; it does not execute any observed process, Git, SBT or resource
operation. There is no persistence, provider registry or separate lifecycle.
Custom CallTree attributes contain only review ID, revision and subject count;
command and source payloads are excluded.

## R2 — Caller-owned identities and observations

The caller supplies the review ID and positive revision and owns their meaning.
CBD preserves them without inventing identity, advancing state, comparing
content, or creating hashes/digests. Review and subject identities match
`[A-Za-z0-9][A-Za-z0-9_.-]{0,127}` exactly. A request has 1..64 non-null subjects
with unique identities. Each subject has nonblank, NUL-free source text of at
most 2048 UTF-8 bytes. Identity and source are preserved exactly in its result.

Usage facts are explicit and mutually exclusive:

| usage | operation | commandName |
| --- | --- | --- |
| process | absent | required: nonblank, NUL-free, at most 1024 UTF-8 bytes |
| git | exact CNCF `GitOperationKind.wireName` | absent |
| sbt | compile, test or test-only | absent |
| resource-read | absent | absent |

Null, unknown, contradictory and missing required facts fail as structured
input errors before observations are produced. There is no fallback guessing.

## R3 — CNCF-owned classification

CBD maps valid facts to `ExternalEffectUsage` and consumes
`ProcessExternalEffectPolicy.classifyUsageC`. It has no private tool-name list,
effect taxonomy or permission model. CNCF's route diagnostic code and sorted
`effectKinds.map(_.toString)` are returned unchanged.

| CNCF route | outcome | ruleId suffix | remediationCategory |
| --- | --- | --- | --- |
| CapabilityLaundering | finding | capability-laundering | use-managed-effect |
| Direct | finding | direct-process | use-managed-effect |
| Unclassified | unknown | unclassified | supply-effect-evidence |
| Managed | managed-use | managed-route | retain-managed-route |

Every rule ID has prefix `cbd.car-review.managed-effects.`. Each observation
contains the subject identity, source, ruleId, outcome, effectKinds, route,
remediationCategory and a bounded explanation that does not echo commands.

## R4 — Meaning and limits

Source observations are supplied review inputs. This provider does not scan or
verify source bodies, resolve files, or execute commands. A managed-use result
describes the supplied route; an unknown result remains unknown. Neither is an
overall security PASS, attestation, execution grant, release decision or proof
of hostile containment. No legacy digest-bound Review exchange or byte-based
management controls participate. Phase 16 remains PLANNED overall.

## Executable examples

`CarReviewManagedEffectProviderSpec`: E1 original Process-to-file misuse and
HTTP/persistence findings (R2, R3); E2 managed observations (R3, R4); E3 unknown
and direct observations (R3, R4); E4 invalid/contradictory/missing facts (R2);
E5 Provider/UoW evaluation and safe CallTree metadata (R1).

`ManagedEffectReviewOperationSpec`: E1 actual ComponentFactory service hook and
typed result (R1, R3); E2 ordinary incompatible-input validation (R1, R2);
E3 private, non-MCP exposure (R1).
