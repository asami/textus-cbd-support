# CNCF Safety Sandbox review integration

Date: 2026-10-15
Status: planned

CNCF now separates Capability-Oriented Execution Architecture from Safety Sandbox Architecture. They coexist: the former defines managed External Effect execution; the latter combines CAR Lint, CAR Loading Admission and Runtime Sandbox to prevent unintended effects.

CBD Support Phase 16 consumes CNCF Phase 110's External Effect / escape classification for deterministic lint and review. CBD Support does not become the runtime safety boundary and does not define a second sandbox policy model.

A developer-approved lint disposition is not a runtime grant. Capability declaration authorizes use of the managed CNCF route; it does not authorize direct java.io/nio, socket/HTTP, ProcessBuilder/shell, JDBC/raw persistence or native access.

The primary Safety Sandbox objective is accident prevention, including bugs, mistaken input/configuration and unexpected AI-generated code. Security benefit comes from attack-surface reduction for hostile application input. Deliberately hostile CAR containment remains Docker/process isolation responsibility.
