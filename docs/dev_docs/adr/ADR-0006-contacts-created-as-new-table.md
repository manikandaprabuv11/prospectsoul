# ADR-0006: Create the `contacts` table as part of C2, not extend it
Date: 2026-09-21 · Status: Accepted

## Context
ADR-0001 assumes `contacts` already exists (`name`, `designation`, `is_primary`, `is_md_owner`) from docs 01–18 and adds `role_id` on top. Direct inspection of the repository at the start of the Sales-Intelligence build shows the table has never been created:

- no Flyway migration creates it (V1–V8 cover audit_log, companies, imports, auth, roles, users, activities, verification);
- there is no `Contact` entity, repository, service, or controller in `com.vyoog.prospectsoul_backend`;
- the Company Management Implementation Plan (doc 03) §Scope Control explicitly excludes "full contact lifecycle" from that vertical slice.

ADR-0001's claim that `is_md_owner=true` rows migrate to `role='MD_OWNER'` therefore has no rows to migrate. Proceeding with the doc-21 `ALTER TABLE` migration verbatim would fail on the first `flyway migrate`.

## Decision
Migration `V11__create_contacts.sql` **creates** `contacts` with the exact set of columns the addenda reference: `id`, `company_id`, `name`, `designation`, `phone`, `email`, `is_primary`, `is_md_owner`, `association_start`, `association_end`, `verification_status`, `created_by`, `created_at`, `updated_by`, `updated_at`. Migration `V12__contact_roles.sql` then adds `contact_roles` and `contacts.role_id` per doc 21 §5.1 unchanged, and its backfill UPDATE statements execute as no-ops on the empty table.

`role_id` is not `NOT NULL` at the DB level so the backfill can succeed on any historical row (per doc 21). The service layer requires it on create.

The partial unique index `uq_contacts_one_primary_per_company` (from doc 21's intent) is emitted from V11 so Domain Model Addendum Invariant 13 is enforced at the DB, not only in the service.

## Consequences
**Easy:** the addenda-specified `contacts` schema now exists exactly once; ADR-0001's migration semantics still hold for anyone reading the docs in isolation; the invariant is enforced at DB and service layers.
**Hard:** callers that assumed contacts existed have to be built (`contact/` module lands with C2, which is exactly the plan).
**Given up:** the ability to claim "we only ALTER, never CREATE" for pre-existing tables — contacts is new and is documented as new here.

## Affected
- `backend/src/main/resources/db/migration/V11__create_contacts.sql` (new)
- `backend/src/main/resources/db/migration/V12__contact_roles.sql` (unchanged in intent from doc 21)
- `contact/` module (new — entity, repository, service, controller, DTOs, mapper)
- C2 AC 5 in doc 23 is restated as: the backfill executes and is correct against a seeded fixture (see `ContactServiceIntegrationTest`).
