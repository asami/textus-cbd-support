# Textus Security Review Responsibility

status=initial
updated_at=2026-10-14
canonical_architecture=goldenport-cncf/docs/design/textus-security-architecture.md

CBD Support owns the canonical CAR Review policy for security-relevant
development findings. It does not own Textus authentication, runtime
authorization, deployment perimeter products, or incident response.

Security Review should assess evidence for architectural violations including
unexpected public exposure, excessive authority, authorization bypass,
direct/unsafe resource access, secret or sensitive-data leakage,
injection-prone boundaries, unsafe external execution and material
dependency/supply-chain risk.

Findings must identify the violated contract and practical consequence where
possible. Absence of evidence remains Unknown/limitation rather than Assurance.

Security review is also responsible for resisting security-themed
overengineering. Management hashes, repeated artifact comparisons, speculative
corruption checks, broad rollback machinery and duplicate validation are
findings/candidates for simplification unless a concrete threat model and
contract justify them.

CAR lint should own deterministic local rules where possible; CBD Review owns
cross-cutting interpretation, reconciliation and quality/security assessment.
sm-workflow decides when the evidence is required in development progression.
