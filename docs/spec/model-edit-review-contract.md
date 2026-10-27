# Model edit review

A project supplies a policy reference and a disposition for every finding kind.
Review emits all applicable findings in deterministic subject/kind order, with
exact candidate, history, policy and evidence references. Missing links require
complete subject scope. Unavailable or unsupported scope is an unresolved result,
not a missing relationship. Complete empty scope is meaningful absence.

Findings cover Goal without Use Case, Use Case without Goal/Slice, Slice without
Specification/Ticket, proven dangling Specification/Ticket, uncovered behavior
and missing capability demand trace. External resolution is read-only. Phase 11
query failures and unavailable Use Case evidence remain visible. Review never
creates a capability, ticket, approval or canonical write.

A successful Phase 11 diagnosis is not by itself complete evidence. Its existing
`MissingComponentUsageCapabilityProjection` gap with reason
`ProjectionNotSupplied` retains unresolved Unavailable scope, including a mixed
selection with available and unavailable packages. `AbsentSelectedReference` in
fully available scope and an available empty demand observation remain complete
absence. The adapter retains the exact original query result, selection and source
attribution read-only, alongside its derived capability observation. It does not
introduce a Use Case-to-demand mapping.

The shared development trace View carries each Use Case's typed capability
observation beside its demand references. Missing input becomes explicitly
unresolved Unavailable; explicitly complete empty input stays complete empty.
Supplied references and evidence retain their attribution even when that evidence
is unresolved. View projection does not infer completeness from an empty vector.

Executable specifications: `ModelEditReviewSpec`, `ModelEditViewProjectionSpec`.
