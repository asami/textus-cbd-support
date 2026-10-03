# Phase 10.6 Checklist: Retained-state and Security Integration

Status: OPEN
phase=[Phase 10.6](phase-10.6.md)
predecessor=[Phase 10.5](phase-10.5.md)
successor=[Phase 10.7](phase-10.7.md)
development-item=DEV-CBD-002

## P10-60: CBD retained-state integration

- [ ] Retain richer review/proposal/alternative/supersession/evidence history
  while proving it is not required to resume the current selected state.

## P10-61: MCP/API continuation surface

- [ ] Define bounded authorized operations for reading, proposing, reviewing,
  resuming, and recording continuation state without exposing an implicit
  approval or arbitrary repository mutation surface.

## P10-62: Sensitive-data and provider evidence policy

- [ ] Define retention/redaction for prompts, responses, provider/model/tool
  identity, hashes, CallTree/evidence references, and non-reconstructable
  sensitive evidence.

## P10-63: Build and publication exclusion

- [ ] Prove internal-model source is excluded by default from runtime packaging,
  public APIs, ordinary CML generation, documentation publication, and CAR/SAR
  artifacts.

## Closure

- [ ] Release the accepted Phase 10.6 contract to Phase 10.7.
