# AI Audit Evidence and Human Review

textus-cbd-support consumes CNCF AI Audit evidence as an analysis and Human-in-the-Loop support source. CNCF remains authoritative for AI Interaction records.

Useful projections are Component/Service/Operation/Workflow/AI Action level rates and trends such as human correction, validation failure, retry, escalation, Admission rejection, downstream failure, latency and cost. These are evidence for human review, not automatic quality verdicts.

The main engineering use is to help identify whether a problem belongs to prompt/context, guard/constraint, provider/routing, Workflow design, or a task that should be converted into deterministic code. cbd-support should therefore preserve traceability from KPI/findings back to the relevant AI Interaction evidence.
