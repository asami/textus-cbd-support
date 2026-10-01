# AI Audit Evidence and Human Review

textus-cbd-support consumes CNCF AI Audit evidence as an analysis and Human-in-the-Loop support source. CNCF remains authoritative for AI Interaction records.

Useful projections are Component/Service/Operation/Workflow/AI Action level rates and trends such as human correction, validation failure, retry, escalation, Admission rejection, downstream failure, latency and cost. These are evidence for human review, not automatic quality verdicts.

The main engineering use is to help identify whether a problem belongs to prompt/context, guard/constraint, provider/routing, Workflow design, or a task that should be converted into deterministic code. cbd-support should therefore preserve traceability from KPI/findings back to the relevant AI Interaction evidence.


## Citizen Development and progressive formalization

cbd-support should treat AI execution evidence not only as quality/audit material but also as evidence for **progressive formalization**.

The target loop is:

```text
Citizen Developer / business user
        |
        v
AI / Skill exploratory execution
        |
        v
CNCF AI Audit + Workflow evidence
        |
        v
cbd-support observation / KPI / Human Review
        |
        v
stable process-pattern discovery
        |
        v
Workflow / StateMachine / rule / deterministic Operation candidate
        |
        v
review / implementation / Admission
        |
        v
more deterministic production execution
```

Useful findings therefore include not only "which AI/model performs better" but also "which part no longer needs AI". Repeated low-variance decisions, stable action sequences, recurring human corrections, common retry paths, stable Admission rules, and repeated deterministic tool use are candidates for formalization.

cbd-support does not own the resulting Workflow/StateMachine semantics and must not automatically rewrite production control flow from KPI data. It exposes evidence, correlations, patterns and candidate opportunities so a human/Citizen Developer or development workflow can decide what to formalize.

This supports a maturity model in which AI is a bootstrap mechanism for initially ambiguous work. As behavior becomes understood, authoritative state and control move to typed CNCF Workflow/StateMachine/runtime assets while AI remains only where nondeterministic semantic judgment is useful.
