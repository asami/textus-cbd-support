# Repository Rules

These repository-local rules supplement the shared `RULE.md`. Shared root
directives remain authoritative symlinks and are not repository-local copies.

## Management and control data: no hashes

Attributable direction: the repository owner explicitly requested on
2026-10-01 that management/control hash logic be removed, and that any
unavoidable hash use be confirmed before implementation.

- Do not use a hash, checksum, digest, content-addressed name, or hash equality
  as an identity, revision, state, decision, approval, dependency binding,
  applicability check, or continuation permission in product management data.
- Define those concepts as explicit typed data. Validate their declared
  identities, revisions, state transitions, dependencies, evidence, and
  preconditions according to the corresponding semantic contract.
- Do not infer a missing identity, revision, mapping, decision, or approval
  from bytes, a hash, collection order, names, or a default. Preserve an
  explicit incomplete or inconsistent result when required data is missing
  or contradictory.
- Existing management/control hash logic is removal scope, not an approved
  exception. A legacy lower-level specification containing a hash does not
  grant permission to preserve that mechanism or add another hash check.
- Do not replace a removed hash mechanism with a byte-comparison protocol,
  encoded-content identity, or another content-derived control token. Model
  the control data and its ownership instead.
- Hash use for a separately specified communication-integrity or corruption
  guarantee is not automatically authorized. Before implementing or retaining
  such a use, explain the concrete failure model, required guarantee, exact
  scope, why simpler mechanisms do not suffice, and compatibility effects;
  obtain explicit user confirmation for that exact use.
- Do not treat prior generic instructions to continue, a passing test, an
  assistant recommendation, or the mere existence of hash code as that
  confirmation. Record an approved exception with its purpose and scope.

This rule governs this repository's product implementation and specifications.
It does not authorize edits to shared ai-directive, common skills, unrelated
cryptographic protocols, or another repository.


## Excessive assurance is a quality defect

Attributable direction: on 2026-10-11 the repository owner clarified that excessive
quality assurance is not harmless extra safety. It can consume substantial
development cost, prevent completion and subsequent work, increase normal-path
complexity, and induce real defects when the resulting state space becomes too
complex to handle reliably.

- Do not add concurrency control, CAS/version arbitration, recovery/replay,
  intermediate-state consistency, winner-selection, duplicate-response handling,
  verification, or corresponding broad tests unless an authoritative requirement
  or declared Failure Model requires that guarantee.
- Operational misuse and low-value accidents need not preserve a clean
  intermediate execution. When they are outside the Failure Model, failure or
  abandonment followed by a fresh execution is acceptable if authoritative
  external assets are not catastrophically destroyed and the specified restart
  path remains available.
- Do not promote constructible abnormal states into major Use Cases or Phase
  acceptance criteria merely because they can occur.
- A test is not free documentation. Tests for unnecessary guarantees create
  continuing specification, execution, review, and maintenance cost.
- Prefer connection/wiring evidence over repeated large tests when a common
  lower-level mechanism already owns the relevant guarantee.
- Review must treat assurance overhead, completion obstruction, main-route
  intrusion, and complexity-induced defect risk as negative quality findings.
- Removing unnecessary defensive state, branches, synchronization, recovery,
  verification, and tests is a quality improvement when authoritative
  requirements remain satisfied.
- Review findings must not silently enlarge the current Phase/task completion
  boundary. New guarantees belong in separate explicitly approved work unless
  required by the current authoritative specification.
