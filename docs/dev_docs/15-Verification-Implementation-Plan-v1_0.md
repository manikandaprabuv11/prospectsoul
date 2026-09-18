<!--
Document: 15-Verification-Implementation-Plan-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Claude Code implementation
Scope: Company Verification Module (Twilio Lookup)
Audience: Product, Engineering, QA, Claude Code
Position in doc set: docs/dev_docs/15 — companion to 13 (Requirements) and 14 (Technical Specification)
-->
# ProspectSoul — Verification Implementation Plan
**Version:** 1.0 · **Status:** Ready for Claude Code implementation
**Doc index:** 15 of the `docs/dev_docs` set

## Working Rules
1. Inspect existing company verification before modifying it.
2. Read PRD, Domain Model, Technical Design, UI/UX, Roadmap and CLAUDE.md first — plus `13-Verification-Requirements-v1_0.md` and `14-Verification-Technical-Specification-v1_0.md`.
3. Reuse existing normalization, users, audit, activities, pagination, auth and error infrastructure.
4. Do not silently change existing contracts.
5. Each phase must have passing tests before the next phase — same "never start Phase N+1 until Phase N passes" discipline as the sprint roadmap (Implementation Roadmap §0).

## Phase 1 — Repository Inspection
Tasks:
- inspect Company entity/service/controller
- inspect existing verify endpoint (`POST /api/v1/companies/{id}/verify`)
- inspect users/auth actor handling
- inspect Activity and AuditService
- inspect Flyway migrations
- inspect frontend sidebar/routes/API patterns
- inspect existing async queue implementation (Research Worker is the closest analog)

Exit:
- gap assessment documented, including the actual JSON casing convention in use (confirm snake_case per project standard) and how actor-type (user vs. system) is currently represented, if at all.

## Phase 2 — Database
Tasks:
- add Flyway migration
- create `verification_batches`
- create `verification_batch_items`
- indexes/constraints
- seed/config if needed

Exit:
- clean migration on fresh DB and current DB.

## Phase 3 — Backend Domain
Tasks:
- entities
- repositories
- enums
- DTOs
- mapper
- service
- eligibility query

Exit:
- unit tests pass.

## Phase 4 — Twilio Provider
Tasks:
- provider interface
- Twilio implementation
- configuration properties
- response mapping
- timeout/error classification
- secret-safe logging

Exit:
- mocked provider tests pass.

## Phase 5 — Background Worker
Tasks:
- queued item claim
- `SKIP LOCKED`
- processing state
- provider invocation
- retry
- per-item failure isolation
- batch counter updates
- recovery after restart

Exit:
- 100-item simulated batch completes without losing progress.

## Phase 6 — REST API
Implement:
```text
POST /api/v1/verifications
GET  /api/v1/verifications/active
GET  /api/v1/verifications
GET  /api/v1/verifications/{id}
GET  /api/v1/verifications/{id}/items
GET  /api/v1/verifications/eligible
GET  /api/v1/verifications/companies
```

Exit:
- OpenAPI updated
- authorization annotations tested.

## Phase 7 — Audit + Timeline
Tasks:
- verification activity
- audit rows
- timeline composition
- failure details

Exit:
- successful verification is visible in company history.

## Phase 8 — Frontend Navigation
Tasks:
- Verify sidebar entry
- route `/verify`
- route `/verify/:id`
- role-aware visibility
- API client/types/query hooks

Exit:
- Verify loads from sidebar.

## Phase 9 — Selection UI
Tasks:
- Added By filter
- Date From/To
- Unverified default
- pagination
- row selection
- select all current page
- review selected count
- confirmation modal

Exit:
- user can create a valid batch.

## Phase 10 — Progress UI
Tasks:
- active batch card
- percentage
- counters
- current company
- polling
- navigation persistence
- refresh persistence
- completed state

Exit:
- leaving and returning restores the active job.

## Phase 11 — History + Results
Tasks:
- previous jobs table
- filters
- detail page
- item results
- failure reasons
- summary statistics

Exit:
- completed job is fully inspectable.

## Phase 12 — Verified Companies
Tasks:
- verified-only endpoint/table
- search/sort/pagination
- verified-by/date filters
- company detail navigation

Exit:
- table never includes unverified companies.

## Phase 13 — Tests
Frontend:
- selection
- filters
- polling
- route persistence
- role UI

Backend:
- all business rules
- provider mapping
- worker recovery
- authorization
- audit/timeline

E2E:
```text
select → start → worker → Twilio mock → verified → history → timeline
```

## Phase 14 — Documentation
Update:
- OpenAPI
- README
- local Twilio setup
- developer docs
- environment variables
- verification troubleshooting

## Definition of Done
Do not claim complete until:
- Flyway migration works
- backend/frontend compile
- Twilio provider works with configured secrets
- no secrets in frontend
- background processing works
- progress survives navigation/reload/restart
- history works
- verified-only list works
- audit/timeline works
- RBAC works
- unit/integration/frontend tests pass
- OpenAPI matches implementation

## Scope Control
Do not implement, as part of this module:
- SMS/OTP ownership verification
- outbound calling
- sales outreach logging
- scraping
- CRM sync
- a second timeline/event store
- a new pipeline state

If any of the above appears necessary to satisfy an acceptance criterion, stop and raise it as a specification conflict rather than resolving it silently (Developer Guide §1 / §33).
