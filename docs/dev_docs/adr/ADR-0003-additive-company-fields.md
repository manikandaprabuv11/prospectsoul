# ADR-0003: Additive company field extension, not table recreation
Date: 2026-07 · Status: Accepted

## Context
Sales needs 12 additional fields on `companies`. The table already exists with production data.

## Decision
All new fields added via `ALTER TABLE ... ADD COLUMN`, all nullable. `companies` is not recreated. No existing column is renamed, retyped, or dropped. Every existing row remains valid post-migration.

## Consequences
**Easy:** zero-downtime migration; existing queries and code paths continue to work; incremental adoption per field.
**Hard:** nothing.
**Given up:** the temptation to "clean up" existing columns while we're in there.

## Affected
- Migration V<next+1>__company_sales_fields.sql
- Company entity, DTO, mapper (new fields exposed)
- Companies filter API (new filter params)
- Companies List UI (new default columns, filter panel)
