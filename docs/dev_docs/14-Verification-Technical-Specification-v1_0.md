<!--
Document: 14-Verification-Technical-Specification-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Company Verification Module (Twilio Lookup)
Audience: Product, Engineering, QA, Claude Code
Position in doc set: docs/dev_docs/14 — companion to 13-Verification-Requirements-v1_0.md
-->
# ProspectSoul — Verification Technical Specification
**Version:** 1.0 · **Companion to:** PRD v1.1, Domain Model v1.0, Technical Design Spec v1.0, `13-Verification-Requirements-v1_0.md`
**Doc index:** 14 of the `docs/dev_docs` set

## 1. Architecture
Verification is a feature module in the existing modular monolith (Technical Design Spec §1):

```text
React
  ↓ REST
Spring Boot Verification API
  ↓
Verification Service
  ↓
DB-backed Worker
  ↓
Twilio Lookup Provider
  ↓
PostgreSQL
```

No message broker is required for v1 — consistent with the existing Research Worker's DB-backed queue pattern (Technical Design Spec §6).

## 2. Backend Package
```text
com.vyoog.prospectsoul.verification/
├── controller/
├── service/
├── provider/
├── worker/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
└── config/
```

This sits alongside the existing feature packages (`company/`, `contact/`, `imports/`, `activity/`, `research/`, …) per the Developer Guide's "one capability, one package" rule. It must not become a dumping ground for unrelated logic.

Follow DTO-first architecture:
`Request DTO → Controller → Service → Repository → Entity`
and
`Entity → Service → Response DTO → Controller`.

Controllers never expose JPA entities.

## 3. Database

### verification_batches
```text
id UUID PK
requested_by UUID NOT NULL
status VARCHAR NOT NULL
filter_added_by UUID NULL
filter_date_from DATE NULL
filter_date_to DATE NULL
total_count INT NOT NULL
queued_count INT NOT NULL
processing_count INT NOT NULL
verified_count INT NOT NULL
failed_count INT NOT NULL
skipped_count INT NOT NULL
started_at TIMESTAMP NULL
completed_at TIMESTAMP NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

### verification_batch_items
```text
id UUID PK
batch_id UUID FK NOT NULL
company_id UUID FK NOT NULL
status VARCHAR NOT NULL
phone_number VARCHAR NULL
normalized_phone_number VARCHAR NULL
provider VARCHAR NULL
provider_reference VARCHAR NULL
phone_valid BOOLEAN NULL
line_type VARCHAR NULL
carrier_name VARCHAR NULL
mobile_country_code VARCHAR NULL
mobile_network_code VARCHAR NULL
failure_code VARCHAR NULL
failure_message TEXT NULL
attempt_count INT NOT NULL DEFAULT 0
started_at TIMESTAMP NULL
completed_at TIMESTAMP NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

Constraints:
- unique `(batch_id, company_id)`
- indexes on batch/status/company/created_at

Reuse existing `companies.verification_status`, `verified_by`, and `verified_at` — do not add a parallel verification-state field on the company table. This is a new Flyway migration only; never edit an applied migration (Technical Design Spec §1, Developer Guide §11.2).

## 4. Provider Abstraction
```java
public interface PhoneVerificationProvider {
    PhoneVerificationResult verify(String e164Phone);
    String providerName();
}
```

Implementation:
`TwilioPhoneVerificationProvider`.

The domain service must not depend directly on Twilio SDK details — same "domain talks to the capability interface" principle already used for the AI provider abstraction (Technical Design Spec §7 / Developer Guide §19). This permits a later provider swap without touching business logic.

## 5. Twilio Configuration
Environment only:
```text
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_LOOKUP_BASE_URL
```

Never log credentials.

Any secret supplied during development or in a conversation with an AI assistant must be treated as compromised and rotated before production use, and must never be committed to source control or exposed to the frontend.

## 6. Provider Result
```json
{
  "valid": true,
  "phoneNumber": "+918131281281",
  "lineType": "mobile",
  "carrierName": "Example Carrier",
  "mobileCountryCode": "404",
  "mobileNetworkCode": "xx"
}
```

Provider errors are normalized into application-level failure codes.

## 7. Result Mapping
| Provider result | Item | Company |
|---|---|---|
| valid + mobile | VERIFIED | VERIFIED |
| invalid | FAILED | unchanged |
| valid + non-mobile | FAILED | unchanged |
| timeout | retry/FAILED | unchanged |
| rate limit | retry/FAILED | unchanged |
| auth/config error | FAILED | unchanged |

Supported provider line types must be mapped from the actual Twilio response contract, not guessed.

## 8. APIs

### Start batch
```http
POST /api/v1/verifications
```

```json
{
  "companyIds": ["uuid1", "uuid2"],
  "addedBy": "uuid",
  "dateFrom": "2026-09-01",
  "dateTo": "2026-09-09"
}
```

Response:
`202 Accepted`

```json
{
  "id": "batch-uuid",
  "status": "QUEUED",
  "totalCount": 100,
  "queuedCount": 100,
  "processingCount": 0,
  "verifiedCount": 0,
  "failedCount": 0,
  "skippedCount": 0
}
```

### Active
```http
GET /api/v1/verifications/active
```

### List jobs
```http
GET /api/v1/verifications?status=&requestedBy=&from=&to=&page=&size=&sort=
```

### Detail
```http
GET /api/v1/verifications/{id}
```

### Items
```http
GET /api/v1/verifications/{id}/items?status=&q=&page=&size=&sort=
```

### Eligible companies
```http
GET /api/v1/verifications/eligible?addedBy=&dateFrom=&dateTo=&page=&size=&sort=
```

### Verified companies
```http
GET /api/v1/verifications/companies?q=&verifiedBy=&verifiedFrom=&verifiedTo=&addedBy=&page=&size=&sort=
```

All list endpoints follow the existing pagination envelope (`content`, `page`, `size`, `total_elements`, `total_pages`) and RFC-7807 error contract (Technical Design Spec §2). Field casing in request/response bodies should match the project-wide `snake_case` JSON convention even though the examples above are shown camelCase for readability — confirm the actual convention in the current codebase before implementing (see §7 of `15-Verification-Implementation-Plan-v1_0.md`, Phase 1).

## 9. Worker
Use Spring scheduling and database claiming with `FOR UPDATE SKIP LOCKED`.

Conceptual loop:
```text
claim queued items
→ mark PROCESSING
→ normalize/use existing E.164 normalization
→ Twilio lookup
→ persist provider result
→ update company
→ create VERIFICATION activity
→ audit mutation
→ mark item terminal
→ update batch counters
```

A transaction must not remain open during a remote Twilio request — the same isolation discipline the Research Worker already applies per-company (Technical Design Spec §6).

## 10. Retry
Retry transient:
- timeout
- 429/rate limit
- temporary 5xx

Do not retry permanent:
- invalid number
- unsupported/non-mobile line type
- malformed request
- authentication/configuration failure

Default maximum attempts: 3, configurable.

## 11. Company State
Successful automated verification updates:
```text
companies.verification_status = VERIFIED
companies.verified_by = system/user actor according to approved business rule
companies.verified_at = current UTC timestamp
```

The implementation must preserve the distinction between the **user who requested the batch** and the **system/provider that performed the automated check**. If the existing schema cannot represent actor type, document the chosen mapping before coding — do not invent a silent convention (Developer Guide §33, "propose it, do not invent it silently").

## 12. Activity + Timeline
Create an existing `VERIFICATION` activity for successful verification and, where appropriate, failed verification attempts.

Do not create a new timeline/event table. The Company Timeline remains a query-time composition over existing domain tables (PRD §3.6, Domain Model Invariant 7) — verification batches must compose into it, not sit beside it as a second source of truth.

## 13. Audit
Use existing `AuditService.record()` in the same transaction as the domain mutation.

Record:
- batch creation
- verification state change
- completion/failure where represented as a mutation

Actor IDs must come from the validated security context.

## 14. Security
Use `@PreAuthorize`.

Read:
`CAN_READ`

Start verification:
`CAN_MUTATE`

The frontend hides unavailable actions, but the backend remains the enforcement point (Technical Design Spec §3, Domain Model Invariant "authorization must be enforced server-side").

## 15. Error Contract
Use RFC-7807 `application/problem+json`.

Examples:
- 400 invalid date range
- 403 role denied
- 404 batch not found
- 409 company already processing
- 422 no eligible companies

## 16. Configuration
Recommended:
```text
VERIFICATION_WORKER_ENABLED=true
VERIFICATION_POLL_INTERVAL_MS=5000
VERIFICATION_BATCH_SIZE=10
VERIFICATION_MAX_RETRIES=3
VERIFICATION_PROVIDER=twilio
```

Naming should follow whatever env-var convention the existing `RESEARCH_BATCH_SIZE` / `AI_DAILY_CAP` settings use (Technical Design Spec §9) so operators see one consistent pattern across queues.

## 17. Observability
Log:
`batch_id`, `item_id`, `company_id`, provider, duration, result, attempt, error code.

Never log Authorization headers or credentials.

## 18. Testing
Unit:
- eligibility
- mapping
- retry classification
- progress calculation
- duplicate prevention

Integration/Testcontainers:
- create batch
- worker claim
- mocked Twilio success/failure
- company update
- audit
- activity
- restart/recovery

Security:
- allowed roles → success
- Viewer/COO start → 403
- missing token → 401

## 19. Scope Boundary
This spec covers phone **validity/line-type verification only**. It intentionally does not cover:
- OTP/ownership confirmation
- outbound calling
- SMS delivery
- any CRM-bound outreach

If a future requirement needs one of the above, it is a new PRD decision, not an extension silently added here (per PRD §1.6 governance and Developer Guide §47, "architecture extensibility is not feature authorization").
