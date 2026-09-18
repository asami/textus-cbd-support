# Business Model / System Model Alignment Direction

Date: 2026-09-19
Status: future direction

## Purpose

CBD Support should visualize the Business Model as the context in which the target System operates and use that context as scaffolding for System Modeling.

Business Modeling is not introduced to turn CBD Support into a general Business Architecture or Business Consulting tool. The goal is to make the relation between Business and the target System explicit, reviewable, and verifiable.

## Model relationship

```text
Business Model
  Business Process
  Process Capability
  Participant / Participant Capability
  Problem Domain
        |
        | context / scaffold / trace
        v
System Model
  Use Case
  Application Capability
  Workflow
  Domain Model / Bounded Context
  Component Model
        |
        v
Runtime / Verification
```

Business Process groups related Use Cases and Workflows and may operate on multiple Problem Domains.

Bounded Context is a semantic/model boundary on the Domain Modeling axis. Workflow is an executable coordination model. CBD Support must not collapse these boundaries into one.

## Visualization

Candidate views include:

- Business Process View
- Participant / Capability View
- Business Process to Use Case Trace View
- Business Process to Workflow Trace View
- Business Process to Problem Domain View
- Problem Domain / Bounded Context View
- Business Model / System Model Alignment View
- System-supported Business Activity View

The Alignment View should make it possible to inspect Business and System models together rather than as unrelated diagrams.

## Alignment validation

CBD Support should validate traceability and consistency between Business Model and System Model.

Candidate checks include:

- a System-supported Business Action has a related Use Case, Application Capability, or Workflow;
- required Participant Capability is supported by an appropriate Application Capability where the Participant is the target System;
- Business Process related Use Cases and Workflows resolve to existing System Model elements;
- operated Problem Domains have corresponding Domain Model / Bounded Context coverage where required;
- Business concepts used by both sides preserve Glossary / BoK concept identity;
- trace references are not dangling;
- contradictory or unsupported mappings are surfaced for review.

Alignment does not mean identity.

A Business Process need not map to one Workflow or one Bounded Context. A Bounded Context may participate in multiple Business Processes. Validation should therefore test explainable relations and coverage, not force structural equality.

## System Modeling scaffold

Business Model should help a modeler begin System Modeling.

Examples:

```text
Business Process
  -> candidate Use Cases
  -> candidate Workflows

Participant Capability
  -> candidate Application Capability

Business Action
  -> candidate Use Case / Operation responsibility

Problem Domain
  -> candidate Domain Model / Bounded Context

Business Concept / Event
  -> candidate Object / Event Model elements
```

These are modeling suggestions and trace candidates, not automatic derivation rules.

CBD Support can use them in discovery and review workflows so that the modeler can inspect, accept, refine, or reject proposed System Model elements.

## Relationship to existing CBD Support direction

This direction extends the existing Analysis Model Up/Down and derived-view approach.

Business Model is another upstream explanatory model. It should remain connected to the more formal System Model so that stakeholder-oriented Business views and engineering-oriented Domain/Application/Component views can be reviewed against the same model provenance.

The long-term goal is a continuous review path:

```text
Business context
  -> Business Model
  -> System Model
  -> Component / executable specification
  -> runtime evidence
```

CBD Support visualizes this continuity and identifies where the chain is missing or inconsistent.

## Design rule

> Business Modeling clarifies the context in which the target System operates and provides scaffolding for System Modeling. CBD Support visualizes Business/System continuity and validates explainable alignment without requiring the models to have identical boundaries or structure.
