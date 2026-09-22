# Architecture Decision Records (ADRs)

Short records of decisions that changed existing code, existing behavior, or deviated from docs 01–18.

## When to write one

**Required** when you:
- Change existing code or flow
- Deviate from docs 01–24
- Add a new invariant or rule not already in the specs

**Not required** for:
- New code that follows the specs as written

## Template

```markdown
# ADR-NNNN: <Title>
Date: YYYY-MM-DD
Status: Proposed | Accepted | Superseded by ADR-XXXX

## Context
What existed before, and what forced a decision.

## Decision
What we're doing.

## Consequences
What this makes easy. What it makes hard. What we gave up.

## Affected
Files / migrations / APIs / specs touched.
```

## Numbering

Sequential 4-digit. Never reuse a number. Superseded ADRs stay in place with `Status: Superseded by ADR-XXXX`.

## Current ADRs

- ADR-0001 — Contact role becomes a first-class field
- ADR-0002 — Multi-NIC via join table (not scalar column)
- ADR-0003 — Additive company field extension (not table recreation)
- ADR-0004 — Extended dedup rules for registry sources
- ADR-0005 — Download and Export are distinct operations

## New in the Sales-Intelligence build

- ADR-0006 — Create the `contacts` table as part of C2, not extend it
- ADR-0007 — Row-batched, chunk-committed import path for registry-scale files
- ADR-0008 — The header alias `Region` maps to `region`, not `state`
- ADR-0009 — Companies carry latitude / longitude as first-class columns
