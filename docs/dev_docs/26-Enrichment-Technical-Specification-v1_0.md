<!--
Document: 26-Enrichment-Technical-Specification-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Developers / Claude Code
Companion to: Enrichment Requirements v1.0 (doc 25), Technical Design Spec v1.0
-->
# ProspectSoul — Phase 1 Enrichment: Technical Specification

**Version:** 1.0
**Status:** Implementation contract for this scope

Doc 25 defines *what*; this defines *how*. Conventions from Technical Design Spec v1.0 §2 apply unchanged (base path `/api/v1`, UUIDs, snake_case JSON, RFC-7807 errors, `AuditService.record(...)` on every mutation, paginated envelope, Flyway).

**Directive from product:** "do the company details scraping like we did in the file code base." Every provider spec here says *what* the port must do; Claude Code reads vyoog-Diagnostic in Phase 0 and confirms the *exact behavior* to reproduce. Any deviation requires an ADR.

---

## 1. Architecture

Preserve the existing modular monolith:
- Spring Boot backend, feature-first packages
- React frontend with existing app shell
- PostgreSQL with Flyway migrations
- Keycloak / OIDC
- Existing object-storage abstraction (raw provider responses go here if large)

One new backend module, one extended:

```text
com.vyoog.prospectsoul/
  enrichment/                       -- NEW top-level module
    framework/
      controller/   EnrichmentController, EnrichmentJobController,
                    ProviderConfigController (Admin)
      service/      EnrichmentOrchestrator, EnrichmentJobService,
                    ProviderRegistry, IdempotencyService, RateLimiter,
                    CostTracker, EvidenceRecorder, FactUpdater
      repository/   EnrichmentJobRepository, EnrichmentCandidateRepository
      entity/       EnrichmentJobEntity, EnrichmentCandidateEntity
      dto/          EnrichmentRequestDto, EnrichmentResultDto,
                    EnrichmentJobDto, ProviderConfigDto
      spi/          EnrichmentProvider (interface), ProviderResult,
                    ProviderRequest, ProviderCapability
    google/
      GooglePlacesProvider (implements EnrichmentProvider)
      GooglePlacesClient (HTTP)
      GooglePlacesFieldMapper
      GooglePlacesMatcher (name+pincode → name+city → name)
    website/
      WebsiteProvider (implements EnrichmentProvider)
      FirecrawlClient (HTTP)
      WebsiteFactExtractor  (regex-based, ports vyoog patterns)
      WebsiteSocialDetector
    phone/
      PhoneProvider (implements EnrichmentProvider)
      PhoneValidatorClient (or libphonenumber wrapper — see doc 25 §13)
      PhoneFieldMapper

  evidence/                         -- EXTEND (nullable columns for provider observations)
  company/                          -- EXTEND (fact_updated helpers, timeline event contribution)
  contact/                          -- EXTEND (same)
  research/                         -- LEAVE AS-IS (bridging shim in framework/;
                                       migration deferred to Phase 1.5)
```

If existing package names differ from the above, use them — do not restructure unrelated modules. Controllers must never accept or return JPA entities.

---

## 2. Provider Abstraction — the `EnrichmentProvider` SPI

```java
public interface EnrichmentProvider {
    String key();                    // "GOOGLE_PLACES", "WEBSITE", "PHONE"
    Set<ProviderCapability> capabilities();
    // COMPANY_ENRICHMENT, CONTACT_ENRICHMENT, PHONE_VALIDATION, WEB_SCRAPE, etc.

    ProviderResult execute(ProviderRequest request);
    // Providers ONLY implement:
    //   - API call
    //   - Response → structured result (facts + candidates + raw payload)
    // Framework handles everything else.
}

public record ProviderRequest(
    UUID companyId,
    UUID contactId,          // nullable
    Map<String, Object> input,  // provider-specific input (phone number, website URL, etc.)
    String idempotencyKey    // computed by framework
) {}

public record ProviderResult(
    ProviderStatus status,   // SUCCESS | PARTIAL | FAILED_TRANSIENT | FAILED_PERMANENT
    List<FactChange> facts,  // { entity, field, oldValue, newValue, overwritesHuman }
    List<Candidate> candidates,  // items requiring human confirmation
    Object rawPayload,       // raw provider response — evidence records this verbatim
    BigDecimal costUsd,      // 0 for free
    String errorCode,        // nullable
    String errorMessage      // nullable
) {}
```

The `EnrichmentOrchestrator`:
1. Looks up provider by key
2. Checks idempotency cache (same `(entity_id, provider, input_hash)` within window → return cached)
3. Acquires rate-limit slot (blocks or defers if hit)
4. Creates `enrichment_jobs` row in state `RUNNING`
5. Calls `provider.execute(request)` with timeout
6. On result:
   - Persists raw payload as evidence
   - Applies each `FactChange` that doesn't overwrite human data
   - Queues candidates for human review (`enrichment_candidates`)
   - Records cost
   - Emits timeline event
   - Updates job row to `SUCCESS` / `PARTIAL` / `FAILED`
7. On failure (transient): retry per policy; on permanent: mark job `FAILED`, record error

---

## 3. Database — Additive Migrations

### 3.1 `V<next>__enrichment_jobs.sql`

```sql
CREATE TABLE enrichment_jobs (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_id          UUID,                              -- groups jobs from one Bulk Enrich
  company_id        UUID REFERENCES companies(id) ON DELETE CASCADE,
  contact_id        UUID REFERENCES contacts(id) ON DELETE CASCADE,
  provider_key      VARCHAR(40) NOT NULL,              -- 'GOOGLE_PLACES', 'WEBSITE', 'PHONE'
  input_hash        VARCHAR(64) NOT NULL,              -- for idempotency
  status            VARCHAR(20) NOT NULL,              -- QUEUED | RUNNING | SUCCESS | PARTIAL | FAILED
  attempt           SMALLINT NOT NULL DEFAULT 1,
  max_attempts      SMALLINT NOT NULL DEFAULT 3,
  scheduled_at      TIMESTAMPTZ,
  started_at        TIMESTAMPTZ,
  completed_at      TIMESTAMPTZ,
  facts_added       SMALLINT NOT NULL DEFAULT 0,
  facts_updated     SMALLINT NOT NULL DEFAULT 0,
  candidates_added  SMALLINT NOT NULL DEFAULT 0,
  cost_usd          NUMERIC(10,6) NOT NULL DEFAULT 0,
  error_code        VARCHAR(60),
  error_message     TEXT,
  triggered_by      UUID,                              -- user id
  triggered_via     VARCHAR(20) NOT NULL,              -- 'USER' | 'BULK' | 'SYSTEM'
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (company_id IS NOT NULL OR contact_id IS NOT NULL)
);

CREATE INDEX idx_ej_company     ON enrichment_jobs(company_id);
CREATE INDEX idx_ej_contact     ON enrichment_jobs(contact_id);
CREATE INDEX idx_ej_batch       ON enrichment_jobs(batch_id);
CREATE INDEX idx_ej_provider    ON enrichment_jobs(provider_key);
CREATE INDEX idx_ej_status      ON enrichment_jobs(status);
CREATE INDEX idx_ej_created     ON enrichment_jobs(created_at DESC);
CREATE UNIQUE INDEX idx_ej_idempotency
  ON enrichment_jobs(provider_key, input_hash, COALESCE(company_id, contact_id))
  WHERE status IN ('QUEUED', 'RUNNING', 'SUCCESS');
```

### 3.2 `V<next+1>__evidence_extension.sql`

```sql
-- evidence table NOT recreated. Additive only.
-- Existing AI-shaped rows (URL + excerpt + ai_model) remain valid.
ALTER TABLE evidence
  ADD COLUMN observation_type    VARCHAR(30),           -- 'AI_CLAIM' (existing) | 'PROVIDER_OBSERVATION'
  ADD COLUMN provider_key        VARCHAR(40),           -- 'GOOGLE_PLACES', 'WEBSITE', 'PHONE', ...
  ADD COLUMN enrichment_job_id   UUID REFERENCES enrichment_jobs(id) ON DELETE SET NULL,
  ADD COLUMN raw_payload_ref     TEXT,                  -- object-storage key when payload is large
  ADD COLUMN raw_payload_inline  JSONB,                 -- when small enough to inline
  ADD COLUMN observed_at         TIMESTAMPTZ;

-- Backfill for existing rows
UPDATE evidence SET observation_type = 'AI_CLAIM' WHERE observation_type IS NULL;
ALTER TABLE evidence ALTER COLUMN observation_type SET NOT NULL;

CREATE INDEX idx_ev_provider    ON evidence(provider_key);
CREATE INDEX idx_ev_job         ON evidence(enrichment_job_id);
CREATE INDEX idx_ev_observed    ON evidence(observed_at DESC);
```

### 3.3 `V<next+2>__enrichment_candidates.sql`

```sql
-- Candidates that require human confirmation before becoming facts
-- (e.g., Google's phone when a human-set primary_phone_normalized already exists,
--  or emails/phones detected from website scraping)
CREATE TABLE enrichment_candidates (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  contact_id        UUID REFERENCES contacts(id) ON DELETE CASCADE,
  enrichment_job_id UUID NOT NULL REFERENCES enrichment_jobs(id) ON DELETE CASCADE,
  candidate_type    VARCHAR(30) NOT NULL,      -- 'PHONE_OVERWRITE', 'EMAIL_NEW_CONTACT',
                                               -- 'PHONE_NEW_CONTACT', 'ADDRESS_OVERWRITE',
                                               -- 'WEBSITE_OVERWRITE', 'PRODUCTS_OVERWRITE'
  field_name        VARCHAR(60),               -- when candidate targets a specific field
  proposed_value    TEXT,
  current_value     TEXT,
  provider_key      VARCHAR(40) NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | ACCEPTED | REJECTED
  resolved_by       UUID,
  resolved_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cand_company   ON enrichment_candidates(company_id);
CREATE INDEX idx_cand_status    ON enrichment_candidates(status);
CREATE INDEX idx_cand_type      ON enrichment_candidates(candidate_type);
```

### 3.4 `V<next+3>__company_google_fields.sql`

```sql
-- Google-specific facts on companies
-- Some may already exist (verify in Phase 0); this migration only adds missing columns
ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS google_place_id           VARCHAR(120),
  ADD COLUMN IF NOT EXISTS google_name               TEXT,
  ADD COLUMN IF NOT EXISTS google_business_category  VARCHAR(120),
  ADD COLUMN IF NOT EXISTS google_business_types     TEXT,    -- comma-separated types[]
  ADD COLUMN IF NOT EXISTS google_maps_url           TEXT,
  ADD COLUMN IF NOT EXISTS google_lat                NUMERIC(10,7),
  ADD COLUMN IF NOT EXISTS google_lng                NUMERIC(10,7),
  ADD COLUMN IF NOT EXISTS google_business_status    VARCHAR(30),
  ADD COLUMN IF NOT EXISTS google_last_enriched_at   TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS website_reachable         BOOLEAN,
  ADD COLUMN IF NOT EXISTS website_title             TEXT,
  ADD COLUMN IF NOT EXISTS website_description       TEXT,
  ADD COLUMN IF NOT EXISTS website_last_enriched_at  TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS social_linkedin           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_facebook           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_x                  VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_instagram          VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_youtube            VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_company_google_place ON companies(google_place_id);
CREATE INDEX IF NOT EXISTS idx_company_google_lat_lng ON companies(google_lat, google_lng);
```

### 3.5 `V<next+4>__contact_phone_fields.sql`

```sql
ALTER TABLE contacts
  ADD COLUMN IF NOT EXISTS phone_normalized       VARCHAR(20),  -- E.164
  ADD COLUMN IF NOT EXISTS phone_country          VARCHAR(3),
  ADD COLUMN IF NOT EXISTS phone_region           VARCHAR(60),
  ADD COLUMN IF NOT EXISTS phone_carrier          VARCHAR(60),
  ADD COLUMN IF NOT EXISTS phone_type             VARCHAR(20),  -- MOBILE | LANDLINE | VOIP | UNKNOWN
  ADD COLUMN IF NOT EXISTS phone_status           VARCHAR(20),  -- VALID | INVALID | UNREACHABLE | DND
  ADD COLUMN IF NOT EXISTS phone_dnd_registered   BOOLEAN,
  ADD COLUMN IF NOT EXISTS phone_last_enriched_at TIMESTAMPTZ;

-- Same fields on companies for the primary phone
ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS primary_phone_country        VARCHAR(3),
  ADD COLUMN IF NOT EXISTS primary_phone_region         VARCHAR(60),
  ADD COLUMN IF NOT EXISTS primary_phone_carrier        VARCHAR(60),
  ADD COLUMN IF NOT EXISTS primary_phone_type           VARCHAR(20),
  ADD COLUMN IF NOT EXISTS primary_phone_status         VARCHAR(20),
  ADD COLUMN IF NOT EXISTS primary_phone_dnd_registered BOOLEAN,
  ADD COLUMN IF NOT EXISTS primary_phone_last_enriched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_contact_phone_status ON contacts(phone_status);
CREATE INDEX IF NOT EXISTS idx_company_phone_status ON companies(primary_phone_status);
```

### 3.6 `V<next+5>__provider_config.sql`

```sql
CREATE TABLE provider_configs (
  provider_key         VARCHAR(40) PRIMARY KEY,      -- 'GOOGLE_PLACES', 'WEBSITE', 'PHONE'
  enabled              BOOLEAN NOT NULL DEFAULT true,
  rate_limit_per_sec   INTEGER,
  rate_limit_per_day   INTEGER,
  timeout_ms           INTEGER NOT NULL DEFAULT 30000,
  max_retries          SMALLINT NOT NULL DEFAULT 3,
  idempotency_window_hours SMALLINT NOT NULL DEFAULT 24,
  cost_per_call_usd    NUMERIC(10,6) NOT NULL DEFAULT 0,
  options              JSONB,                        -- provider-specific options
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by           UUID
);

INSERT INTO provider_configs (provider_key, rate_limit_per_sec, rate_limit_per_day, cost_per_call_usd) VALUES
  ('GOOGLE_PLACES', 10, 5000, 0.017),
  ('WEBSITE',        5, 2000, 0.002),
  ('PHONE',         20, 10000, 0.005);
```

---

## 4. Endpoints

### 4.1 Enrichment

```text
POST /api/v1/enrichment/companies/{id}/run
  Body: { providers: ["GOOGLE_PLACES", "WEBSITE", "PHONE"], force: false }
  → { batch_id, jobs: [ { id, provider_key, status } ] }
  # ANALYST, SALES_LEAD, ADMIN

POST /api/v1/enrichment/bulk
  Body: { filter: { …current companies-list filter… },
          providers: [...],
          force: false }
  → 202 Accepted { batch_id, jobs_queued }
  # ANALYST, SALES_LEAD, ADMIN
  # respects COMPANIES_DOWNLOAD_ROW_CAP
  # 422 if over cap with narrow-filter guidance

GET  /api/v1/enrichment/jobs
  ?provider_key=&status=&batch_id=&company_id=&from=&to=&page=
  → paginated list
  # any read role

GET  /api/v1/enrichment/jobs/{id}
  → job detail with facts changed, candidates created, evidence rows

GET  /api/v1/enrichment/batches/{batch_id}
  → { batch summary: total, succeeded, failed, cost_usd_total, ... }
```

### 4.2 Candidates

```text
GET   /api/v1/companies/{id}/enrichment-candidates?status=PENDING
POST  /api/v1/enrichment-candidates/{id}/accept
POST  /api/v1/enrichment-candidates/{id}/reject
POST  /api/v1/companies/{id}/enrichment-candidates/accept-all
  Body: { candidate_ids: [...] }        # bulk-accept subset
```

Accepting applies the proposed_value as a fact (with new evidence row linking to the originating job). Rejecting marks resolved; nothing changes.

### 4.3 Provider config (Admin)

```text
GET   /api/v1/admin/enrichment/providers
PATCH /api/v1/admin/enrichment/providers/{provider_key}
  Body: { enabled?, rate_limit_per_sec?, rate_limit_per_day?, timeout_ms?,
          max_retries?, idempotency_window_hours?, cost_per_call_usd?, options? }
```

Config changes take effect on next call (no restart).

---

## 5. Google Places Provider

### 5.1 Behavior — port from vyoog-Diagnostic

Claude Code reads vyoog's Google Places integration and reproduces:
- The exact fields requested
- The matching algorithm (order of attempts)
- The confidence-scoring logic
- Any special handling (closed businesses, ambiguity)

**If vyoog's implementation is missing details, defaults are:**

**Matching (fallback ladder):**
1. If `google_place_id` already on the company → refresh that place
2. Text Search: `{canonical_name} {pincode}` → take top result if confidence ≥ threshold
3. Text Search: `{canonical_name} {city}, {state}` → same
4. Text Search: `{canonical_name}` biased to `{city}` centroid → same
5. No match → job status PARTIAL, no facts written, evidence row records attempts

**Fields requested (Places API v1 or Legacy — match vyoog):**
- `place_id`, `displayName`, `formattedAddress`, `internationalPhoneNumber`,
  `websiteUri`, `types`, `googleMapsUri`, `location.latitude`, `location.longitude`,
  `businessStatus`

### 5.2 Field mapping

| Google field | ProspectSoul fact | Overwrite policy |
|---|---|---|
| `place_id` | `companies.google_place_id` | Silent overwrite (enrichment is authority) |
| `displayName` | `companies.google_name` | Silent (informational) |
| `formattedAddress` | `companies.address_line` | Silent if empty; candidate if human-set |
| `internationalPhoneNumber` | `companies.primary_phone_normalized` | Silent if empty; candidate if human-set |
| `websiteUri` | `companies.website_domain` | Silent if empty; candidate if human-set |
| `types` | `companies.google_business_types` (comma-separated) | Silent |
| `types[0]` | `companies.google_business_category` | Silent |
| `googleMapsUri` | `companies.google_maps_url` | Silent |
| `location.latitude/longitude` | `companies.google_lat/lng` | Silent overwrite |
| `businessStatus` | `companies.google_business_status` | Silent |

`google_last_enriched_at` = `now()` on every run.

### 5.3 Cost + quota
- Config: `provider_configs.GOOGLE_PLACES.cost_per_call_usd = 0.017` (Text Search + Place Details)
- Rate: 10/sec, 5,000/day defaults
- Idempotency window: 24h (re-run within window returns cached result at zero cost)

---

## 6. Website Provider — Firecrawl

### 6.1 Behavior — port from vyoog-Diagnostic

Claude Code reads vyoog's Firecrawl integration and reproduces:
- Exact Firecrawl API options used (`waitFor`, `timeout`, `formats`, `onlyMainContent`, etc.)
- Exact extraction patterns used (regex/parsers for phone, email, address, products)
- Exact page-selection logic (home page vs specific paths)
- Error handling (retries on transient network errors, permanent-fail on 404/robots-block)

**If vyoog uses specific extraction heuristics** (e.g., prioritizes `/contact` page for phone/email, `/about` for company description, `/products` or `/services` for products list) — those port over verbatim.

### 6.2 Extraction rules (port confirms actual patterns from source)

**Deterministic extraction only.** Regex/parser-based. No AI interpretation in Phase 1.

- **Emails:** RFC-5321 email regex; deduplicated; capped at 20 per page
- **Phones:** libphonenumber's `PhoneNumberMatcher` with IN default region; deduplicated; capped at 20
- **Addresses:** heuristic — look for pincode patterns (6 digits) with surrounding text; pass extracted blocks to a light parser
- **Products:** `<li>` or `<h2>/<h3>` text under a Products/Services section
- **Social handles:** URL patterns for LinkedIn, Facebook, X, Instagram, YouTube

### 6.3 Field mapping

| Extracted | ProspectSoul fact / candidate |
|---|---|
| Page reachable | `companies.website_reachable` (fact) |
| Page title | `companies.website_title` (fact) |
| Meta description | `companies.website_description` (fact) |
| Detected emails | Candidate: `EMAIL_NEW_CONTACT` — creates enrichment_candidate per unique email; **no automatic contact creation** |
| Detected phones | Candidate: `PHONE_NEW_CONTACT` similarly |
| Detected address blocks | Candidate: `ADDRESS_OVERWRITE` if `address_line` empty and one address found → silent fact; else candidate |
| Detected products | Candidate: `PRODUCTS_OVERWRITE` if `products` empty → silent; else candidate |
| Social handles | `companies.social_*` (fact — silent overwrite; informational) |

`website_last_enriched_at` = `now()`.

### 6.4 Cost + quota
- Firecrawl free tier is generous; default `cost_per_call_usd = 0.002` (adjust per actual plan)
- Rate: 5/sec, 2,000/day defaults
- Idempotency window: 24h

---

## 7. Phone Provider

### 7.1 Behavior — port from vyoog-Diagnostic

Claude Code identifies which phone provider vyoog uses:
- If it's libphonenumber (Node lib in Lovable) → port to `libphonenumber-java` (google/libphonenumber)
- If it's a paid service (Twilio Lookup, Numverify, etc.) → port as an HTTP client
- If it's a custom implementation → port the logic

**If unclear or absent:** default to libphonenumber-java for format + basic validation. `phone_status = VALID` if parseable + valid, else `INVALID`. Carrier/DND left NULL. Document as placeholder pending Admin decision on a paid provider (per doc 25 §13 item 1).

### 7.2 Field mapping

| Provider output | ProspectSoul fact | Notes |
|---|---|---|
| E.164 normalized number | `contacts.phone_normalized` (or `companies.primary_phone_normalized` if company-level) | Silent overwrite (normalization is authoritative) |
| Country code | `phone_country` | Silent |
| Region (state/circle for IN) | `phone_region` | Silent |
| Carrier | `phone_carrier` | Silent (informational) |
| Type | `phone_type` | Silent |
| Validity | `phone_status` | Silent |
| DND registered | `phone_dnd_registered` | Silent |

`phone_last_enriched_at` = `now()`.

### 7.3 Cost + quota
- libphonenumber (offline): $0, no rate limit
- Paid provider: config-driven

---

## 8. Framework — Rate Limiting / Retry / Idempotency / Cost

### 8.1 Rate limiting
- Per-provider token bucket, backed by database counters (no in-memory-only limiter — survives restarts)
- `rate_limit_per_sec` for short-burst protection; `rate_limit_per_day` for quota enforcement
- Hit limit → job stays QUEUED with `scheduled_at` = next available slot

### 8.2 Retry
- On `FAILED_TRANSIENT`: retry with exponential backoff (1s, 4s, 15s), up to `max_retries`
- On `FAILED_PERMANENT`: no retry; job → FAILED with error details
- Transient errors: network timeout, 5xx responses, rate-limit responses from provider
- Permanent errors: 4xx (except 429), invalid input, provider-specific "not found" for text search (still SUCCESS with zero facts if the API call itself succeeded)

### 8.3 Idempotency
- Key = `SHA256({provider_key} + {entity_id} + {canonicalized_input})`
- Within `idempotency_window_hours`, a matching key with `status IN ('SUCCESS', 'RUNNING')` short-circuits: return existing job's result, cost = 0, no timeline event
- `force: true` on the endpoint bypasses idempotency (Admin/Sales Lead only)

### 8.4 Cost tracking
- Every job records `cost_usd`
- Batch summary aggregates
- `/reports/enrichment-cost` (Admin, Sales Lead, COO) — per-provider, per-user, per-month totals

---

## 9. Frontend Additions

### 9.1 Company Detail — Enrich control

Top of Company Detail page, next to existing action buttons:

```
[ Enrich ▾ ]
   ├── Google Places        (last: 2 days ago)
   ├── Website (Firecrawl)  (last: 5 hours ago)
   ├── Phone (all)          (last: never)
   ├── ─────────────
   └── Run all
```

Selection immediately queues jobs; toast shows "Enrichment queued — 3 jobs" with a link to the batch detail.

### 9.2 Companies List — Bulk Enrich

Top-right, next to Download:

```
[ Bulk Enrich ▾ ]
```

Same items as single-company version. Uses current filter selection. Row-cap enforced. Batch id returned; user can view progress from a notifications bell or navigate to `/settings/enrichment-jobs`.

**Implementation Note (2026-09-24):** the backend still exposes only the single-company `POST /companies/{id}/enrich` — no server-side bulk/batch endpoint exists yet. `frontend/src/features/enrichment/components/BatchEnrichPanel.tsx` (wired into `EnrichmentJobsPage.tsx` at `/enrichment/jobs`) delivers the bulk-enrich UX by orchestrating N single-company calls client-side at concurrency 3, mirroring the existing `VerifyNewCompaniesPanel` pattern: a table with per-row checkboxes and select-all-on-page, provider checkboxes (Google Places / Website / Phone), a selected-count badge, a confirm dialog, and a "Run Enrichment" button disabled while in-flight. Each company's status (queued / running / success / partial / failed) is tracked client-side with a running "X of Y done" summary. The screen is role-gated via `usePermissions().canMutate`. This satisfies the acceptance intent of Track E6 (see doc 27) without the `Bulk Enrich ▾` dropdown-plus-server-batch-id design originally specified; a true server-side bulk endpoint (batch id, notification on completion, jobs-list filter by batch) remains open — see doc 27 Track E6 acceptance criteria 1–4, 9.

### 9.3 Enrichment Candidates panel (Company Detail)

New section below Contacts:

```
Enrichment candidates (3 pending)
  ┌ Phone from Google Places       +91 98430 12345          [Accept] [Reject]
  │   Current: +91 98430 99999 (human-entered)
  ├ Email from Website              info@abcpumps.in         [Accept] [Reject]
  │   Would create new contact
  └ Address from Website            123 Industrial Estate…   [Accept] [Reject]
       Current: (empty)
                                                   [ Accept all ] [ Reject all ]
```

### 9.4 Evidence viewer

Small indicator next to each fact. Click opens a modal:

```
Fact: Primary phone  →  +91 98430 12345
Source: Google Places (2026-09-18 14:22 IST)
Job: EnrichmentJob #a1b2c3
Raw payload: { "place_id": "ChIJ...", "phone": "+91 98430 12345", ... }  [View full]
```

### 9.5 Admin — Enrichment Jobs `/settings/enrichment-jobs`

Standard list: provider, company, status, cost, timestamps, error. Filters: provider, status, date range, batch id.

### 9.6 Admin — Provider Config `/settings/enrichment-providers`

Standard form per provider: enabled toggle, rate limits, timeout, retries, idempotency window, cost per call, options (JSON editor).

---

## 10. Configuration Additions

| Setting | Purpose |
|---|---|
| `GOOGLE_PLACES_API_KEY` | Server-side Places calls; never in frontend |
| `FIRECRAWL_API_KEY` | Server-side Firecrawl calls |
| `PHONE_PROVIDER_API_KEY` | Optional, if paid phone provider is used |
| `PROVIDER_USD_TO_INR_RATE` | For INR display of costs |
| `ENRICHMENT_BULK_CAP` | Bulk-enrich row cap (defaults to `COMPANIES_DOWNLOAD_ROW_CAP`) |

Per-provider settings live in `provider_configs` table (§3.6), not env vars.

---

## 11. Testing Baseline

### Unit
- `ProviderRegistry` resolves providers by key
- `IdempotencyService` produces stable input hashes; returns cached results correctly
- `RateLimiter` blocks/defers correctly on token exhaustion
- `EvidenceRecorder` handles inline JSONB vs object-storage split by size
- `FactUpdater` respects the human-overwrite rule (candidate vs silent overwrite)
- Google Places field mapper: full response → correct facts, missing fields tolerated
- Website extractors: regex patterns match/reject correctly on fixture pages
- Phone validator: E.164 normalization for IN + international numbers
- Cost tracker accumulates correctly

### Integration (Testcontainers + mocked providers)
- Full run: single company, all three providers → 3 jobs, 3 evidence rows, N facts, 1 timeline event per provider
- Idempotency: same run within window → 0 API calls, cached results returned
- `force: true` bypasses idempotency
- Human-set field: enrichment creates candidate, not silent overwrite
- Candidate accept → fact updated, new evidence row linking back to job
- Bulk enrich with filter → correct company set, jobs queued, cap enforced
- Provider transient failure → retry per policy
- Provider permanent failure → job FAILED, no facts changed, evidence records error
- 403 paths for non-authorized roles
- Existing `research/` AI flow continues to work (bridging shim in place)

### Functional-equivalence tests (against vyoog-Diagnostic)
- Given a fixture company, ProspectSoul's Google Places enrichment produces the same fact set as vyoog-Diagnostic's version (Claude Code writes a comparison harness in Phase 0)
- Same for Website and Phone

### AI
- No changes; existing MockAiProvider baseline unchanged

---

## 12. Non-Functional

- Single-company all-providers run completes <20s (Google + Firecrawl + phone)
- Bulk enrich of 1,000 companies (single provider) completes <30 min at default rate limits
- Enrichment jobs list renders <300ms with 10k rows in `enrichment_jobs`
- Raw payloads >100KB go to object storage; <100KB inline in `evidence.raw_payload_inline`
- No API key ever appears in a frontend bundle or network response
