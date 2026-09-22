# ADR-0001: Contact role becomes a first-class field
Date: 2026-07 · Status: Accepted

## Context
Docs 01–18 model contacts with `name`, `designation` (free text), `is_primary` and `is_md_owner` (boolean). Sales needs to filter and report by role — "show me companies with an HR contact" — which is impossible against free-text `designation`. `is_md_owner` covers only one role.

## Decision
Add `contact_roles` reference table (Admin-editable) and `contacts.role_id` FK.
Backfill: `is_md_owner=true` → `role='MD_OWNER'`. Everything else → `Other`.
Retain `is_md_owner` and `designation` for backward compat; keep `is_md_owner` in sync when role is set to `MD_OWNER`.

## Consequences
**Easy:** filter/report by role; extensible role list.
**Hard:** two fields (`role_id` and legacy `is_md_owner`) must stay consistent — enforced in service layer.
**Given up:** free-text designation as the primary filter dimension (still stored for display).

## Affected
- Migration V<next+2>__contact_roles.sql
- `contact/` module — entity, service, DTOs, mapper
- Contact create/edit UI (role picker required)
- Companies List (new filter: `has_contact_role`)
- Contact Roles Admin settings screen
