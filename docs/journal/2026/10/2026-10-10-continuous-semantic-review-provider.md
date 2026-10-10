# Continuous semantic review provider

Date: 2026-10-10
Status: review use-case direction

CBD Support Review should support the use of low-cost persistent AI capacity as a semantic quality sensor.

A Dot, local LLM or other admitted Review provider may repeatedly inspect bounded repository/component/change contexts under rotating review concerns such as defensive complexity, model/implementation mismatch, test architecture, structural drift and simplification opportunities.

Provider findings remain advisory Review evidence. They do not authorize source mutation merely because the provider can run continuously.

Actionable findings may be promoted through explicit policy/human judgment into durable GitHub Issues or bounded Pull Request proposals. Textus Development Center then presents that development work for human triage and possible Codex/sm-workflow resolution.

Confirmed/rejected findings are useful corpus evidence for Review-provider evaluation. Recurring high-confidence mechanically detectable patterns may later become cheap CAR lint rules, preserving the separation between deterministic CAR lint and semantic Review.
