<!--
Document: 16-Verification-Claude-Kickoff-Prompt-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Verification Module (Twilio Lookup)
Audience: Claude Code
Position in doc set: docs/dev_docs/16 — copy/paste implementation prompt for the module
defined in 13 (Requirements), 14 (Technical Specification), 15 (Implementation Plan), and
17 (UI/UX Design).
-->
# ProspectSoul — Verification Module Claude Implementation Kickoff Prompt
**Version:** 1.0 · **Status:** Approved for Implementation
**Doc index:** 16 of the `docs/dev_docs` set

You are the senior full-stack engineer implementing the ProspectSoul Company Verification vertical slice.

## AUTHORITATIVE DOCUMENTS
Before coding, read, in order:
1. ProspectSoul PRD v1.1
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. `CLAUDE.md`
7. Technical-Team Developer Guide
8. `docs/dev_docs/01`–`12` (existing project-specific requirement/spec/plan/prompt documents already implemented)
9. `docs/dev_docs/13-Verification-Requirements-v1_0.md`
10. `docs/dev_docs/14-Verification-Technical-Specification-v1_0.md`
11. `docs/dev_docs/15-Verification-Implementation-Plan-v1_0.md`
12. `docs/dev_docs/17-Verification-UI-UX-Design-v1_0.html`

If documents conflict, stop and identify the conflict. Do not silently choose.

## OBJECTIVE
Implement the complete Verify module across the existing Spring Boot backend, React frontend and PostgreSQL database.

This is a real end-to-end feature, not a mock.

## FIRST: INSPECT
Before changes:
- inspect repository structure
- inspect existing Company verification implementation
- inspect Company entity fields
- inspect phone normalization
- inspect Activity and AuditService
- inspect Keycloak/RBAC
- inspect async/background job patterns (Research Worker is the closest existing analog)
- inspect Flyway migrations
- inspect frontend sidebar/router/API/query patterns
- inspect existing design system
- inspect `CLAUDE.md`

Return a concise gap assessment before implementation.

## REQUIRED USER EXPERIENCE

Add sidebar entry:
```text
Verify
```

Route:
```text
/verify
```

The page must contain:
1. Verify New Companies
2. Current Verification
3. Previous Verification Jobs
4. Verified Companies

## VERIFY NEW COMPANIES
The selection flow must support:
- Added By filter
- Date From
- Date To
- Verification = Unverified by default
- pagination
- multi-select
- selected count
- confirmation
- Start Verification

If the user selects `Added By = Mani`, only companies added by Mani in the selected date range are eligible.

The bottom main table is verified companies only.

## BACKGROUND JOB
Starting verification must:
- create a persistent batch
- return `202`
- process companies asynchronously
- never depend on the browser remaining open
- continue if one company fails
- survive browser navigation
- survive page refresh
- recover after backend restart

Use DB-backed queue processing consistent with existing ProspectSoul architecture.

Use `FOR UPDATE SKIP LOCKED` or the repository's existing equivalent.

## TWILIO
Use Twilio Lookup v2 Line Type Intelligence from the backend.

Required environment configuration:
```text
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_LOOKUP_BASE_URL
```

Do not put credentials in React.

Do not hardcode any credential supplied in any conversation, sample, or prior session — treat any such value as compromised and require rotation before production use.

Use a provider interface so Twilio can be replaced later.

Recommended success rule:
```text
valid == true AND line type == mobile
```

Do not claim ownership verification.

## RESULT HANDLING
Persist:
- normalized phone
- provider
- provider reference where available
- valid
- line type
- carrier
- MCC/MNC where available
- error code/message
- attempts
- timestamps

Successful result:
```text
item → VERIFIED
company → VERIFIED
```

Invalid/non-mobile:
```text
item → FAILED
company remains UNVERIFIED
```

Transient provider failure:
retry up to configured maximum.

## PERSISTENT PROGRESS
The database is source of truth.

The frontend must retrieve:
```text
GET /api/v1/verifications/active
```

and poll the active batch while processing.

Show:
- total
- queued
- processing
- verified
- failed
- skipped
- percentage
- current company
- start time
- elapsed time

When completed, stop polling and show final statistics.

## HISTORY
Implement previous verification jobs with:
- requester
- filter snapshot
- total
- verified
- failed
- skipped
- start/completion time
- status

Clicking a job opens detailed results.

## VERIFIED COMPANY TABLE
Only return companies with canonical verification status `VERIFIED`.

Support:
- search
- pagination
- sorting
- verified-by filter
- verified date range
- added-by filter

## API
Implement exactly:
```text
POST /api/v1/verifications
GET /api/v1/verifications/active
GET /api/v1/verifications
GET /api/v1/verifications/{id}
GET /api/v1/verifications/{id}/items
GET /api/v1/verifications/eligible
GET /api/v1/verifications/companies
```

Follow existing pagination and RFC-7807 contracts.

Use DTOs everywhere. Never expose entities.

## RBAC
Use existing role constants and security model.

Analyst/Sales Lead/Admin:
- can start verification

Viewer/COO:
- read only

Enforce permissions with `@PreAuthorize`.

## AUDIT + TIMELINE
Use existing `AuditService`.

Create existing `VERIFICATION` activities.

Do not create a second timeline store.

## DATABASE
Create a Flyway migration for:
```text
verification_batches
verification_batch_items
```

Use UUIDs, UTC timestamps, foreign keys and indexes. Never edit an already-applied migration.

## FRONTEND
Use existing:
- React
- TypeScript
- React Router
- TanStack Query
- existing component library/design system

Do not introduce Redux.

## TESTS
At minimum:
- filter eligibility
- Added By filter
- inclusive date range
- unverified-only selection
- duplicate prevention
- valid mobile → verified
- invalid → failed
- non-mobile → failed
- retryable error
- permanent error
- batch progress
- worker recovery
- active job after reload
- history
- verified-only table
- audit
- timeline
- RBAC 403
- missing token 401

## DEFINITION OF DONE
Do not claim PASS without evidence.

Report:
1. files created/changed
2. migration
3. backend
4. Twilio provider
5. worker
6. frontend
7. APIs
8. RBAC
9. audit/timeline
10. tests with exact results
11. acceptance criteria PASS/FAIL/BLOCKED
12. deviations
13. remaining work

## SCOPE CONTROL
Do not implement unrelated modules. In particular, do not implement:
- automated scraping
- AI research
- ICP qualification
- CRM outreach
- company relationships
- OTP/ownership verification
- sales calling/outreach

unless an existing dependency makes a minimal implementation strictly necessary — and if so, stop and propose it rather than building it silently.
