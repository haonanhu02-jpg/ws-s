# Visitor Platform Agent Guide

## Source of truth

Before business changes, read:

1. `docs/requirements/visitor-requirements-baseline.md`
2. `docs/requirements/open-questions.md`
3. Relevant files under `docs/contracts/`

Confirmed `RULE-*` items are mandatory. Do not turn unresolved `Q-*` items into fixed behavior.

## Boundaries

- Keep one unified frontend while preserving backend service ownership.
- Never place business entities in `common/*`.
- Never expose identity-card data in guard DTOs or ordinary events.
- Services must not write another service's schema.
- OA result is read-only for guards and never blocks manual entry.
- Phone-notification details must not enter guard APIs.

## Quality gate

Run `mvn clean verify`, frontend tests/build, and Compose configuration validation for affected areas.

