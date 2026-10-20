# Phase 15.1 Checklist: ReviewRequest and External Execution

Status: IN_PROGRESS

- [ ] P15.1-RR01 One structured ReviewRequest builder reuses Phase 15 semantics.
- [ ] P15.1-RR02 Normal Operation preparation returns the request with zero AI invocation and no conversation capture.
- [ ] P15.1-RR04 External typed result acceptance preserves review/subject attribution, coverage and deterministic findings, with zero AI invocation.
- [ ] P15.1-RR06 Bounded external-route handoff for sm-workflow Phase 10 is accepted after the RR05 external portion passes; its repair/Final routes reuse it.
- [ ] P15.1-RR03 Explicit standalone execution uses the same request/acceptance contract through the existing runner.
- [ ] P15.1-RR05 Representative external and standalone execution, focused specs and applicable regressions pass with honest execution limits.
- [ ] Independent Phase review has no unresolved blockers.
- [ ] Required Phase validation and closure/release evidence are recorded.

All original P15-RR obligations were unchecked when relocated. Their prior IDs
are historical planning references only, not runtime aliases or completed work.

Execute RR01/02/04 and the external portion of RR05 as one connected slice before
RR06. Record that portion's actual execution and focused evidence in the normal
handoff; leave RR05 unchecked until standalone and applicable regressions also
pass. Then complete RR03 and remaining RR05 as the second package. These are work
packages within the existing Phase, not new subphases or extra acceptance cycles.
No checkbox is completed by this optimization.
