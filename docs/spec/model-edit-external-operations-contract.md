# Model Edit external Operations

Stage 12.6 extends the accepted session, source projection, Palette and mutation
contracts. `CbdModelEdit` is an ordinary generated service, declared in
`src/main/cozy/textus-cbd-support.cml`. Plugin and MCP dispatch use its generated
requests and the same handwritten ActionCall/application implementation.
The seven read request types are declared under CML `QUERY`;
`ApplyModelEditRequest` is declared under `COMMAND`. The response remains a
`VALUE`. Input declaration kinds match each Operation kind and are consumed by
the selected source generator; they are not transport authorization.

The embedding host constructs `ModelEditApplication` from an already admitted
session, source selection, project finding policy, bounded resolution/behavior
evidence and per-principal bindings. It constructs `impl.ComponentFactory(app)`
and calls the normal `create(ComponentCreate(...))` route. One factory/application
owns one local single-writer editing session for its lifetime. The default
zero-argument plugin constructor has no admitted session and returns unavailable;
loading a plugin does not manufacture model facts or authorization. Rehydration
and durable history remain the existing package/continuation owners. Restarting
requires a newly admitted session, not recovery of this in-memory application.

Every call authenticates the current ExecutionContext principal against exactly
one server binding. Anonymous/unspecified and expired sessions are denied. The
host explicitly selects read access and an optional existing typed mutation grant;
no client brand or invented role grants authority. Requests contain exact session
and current candidate references. Contextual inspection and editing additionally
select the same Palette context; edits carry the existing typed Operation and a
supplied successor reference. Clients cannot supply source facts, grants, policy,
resolution evidence, a canonical writer or a ticket-system mutation.

The service exposes semantic inspection, applicable operations, candidate data,
shared Views, exact development traces, deterministic Review and diff inspection.
Edits delegate to `ModelEditModelMutation`, hence the accepted authorization,
subject, precondition and complete external-reference checks. A successful edit
replaces only the application-owned immutable session and returns candidate
confirmation. ChatGPT and Codex use identical requests and principal admission.

Diff inspection retains the operation history and separately reports a supplied
Phase10 `InternalModelSemanticDiffAdmission` together with its validated
`ModelEditCmlProjection.Mapping`. Only an admission for the exact
current candidate, scope and dependency is exposed; absent or stale evidence is
explicitly unavailable. It never invents a semantic diff or approval. A trusted
runtime-package host can attach that existing mapping/admission through the application
factory. Ordinary external input cannot install or refresh it.

`CbdRetrieval` keeps its read-only policy. `CbdCatalogAdmin` and `CbdReviewAdmin`
remain private. The dedicated Model Edit service is MCP-ready, including its
ordinary authorized candidate command; it exposes no canonical promotion,
approval, filesystem configuration, AI provider or arbitrary ticket mutation.
Unknown Operations and unknown/missing JSON fields fail explicitly.

Executable specifications: `ModelEditExternalOperationsSpec`,
`ModelEditApplicationSpec`, `ModelEditAuthorizationSpec`. Native qualification
and independent review are pending for this authored Stage12.6 candidate.

Read request example (references are explicit producer-allocated examples):

```json
{"session":{"recordId":"session","recordRevision":1},"candidate":{"recordId":"candidate","recordRevision":6}}
```

The string value of that JSON is `requestJson`. Context discovery uses
`{"selection": <read selector>, "context": {"surface": "ActorGoal", "scope":
{"context": "context-edit", "component": "component-edit"}, "subject":
{"Goal": {"identity": "repeat-purchase"}}}}`. An edit uses the same `selection`
and `context`, adding `operation` in the accepted closed Operation codec and
`successorCandidate` in the same exact record-reference format. Clients retain
supplied semantic/external identities; labels and list order never resolve them.
