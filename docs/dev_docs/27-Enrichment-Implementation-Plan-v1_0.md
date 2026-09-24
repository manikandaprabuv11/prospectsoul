<!--
Document: 27-Enrichment-Implementation-Plan-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Engineering, QA, Claude Code
Companion to: docs 25 (Requirements), 26 (Technical Specification)
-->
# ProspectSoul — Phase 1 Enrichment: Implementation Plan

**Version:** 1.0
**Execution model:** vertical slices, six sequential tracks
**Owner:** Engineering / Claude Code

---

## Phase 0 — Repository, Specification, and Source Audit

**Before any code changes**, produce three artifacts:

### 0.1 Gap assessment against target (ProspectSoul repo)

Inspect and report:
- Current `companies` and `contacts` table shape (which of the 12 new fields in doc 26 §3.4/§3.5 already exist)
- Current `evidence` table shape (confirm AI-shaped columns)
- Current `research/` module and `research_queue` mechanics
- Current Company Timeline composition logic
- Current authentication / Keycloak role handling
- Existing tests
- Object-storage abstraction (for raw payloads)
- `CLAUDE.md`

### 0.2 Migration map against source (vyoog-Diagnostic zip)

Inspect and report per capability:

| Vyoog capability | Source file(s) | ProspectSoul target | Port / Discard / Defer | Notes |
|---|---|---|---|---|
| Firecrawl integration | `src/…` | `enrichment/website/` | Port | Preserve API options |
| Google Places integration | `src/…` | `enrichment/google/` | Port | Preserve matching logic |
| Phone validation | `src/…` | `enrichment/phone/` | Port | Identify validator |
| Website AI intelligence | `src/…` | — | Defer (Phase 2) | |
| Benchmarks | `src/…` | — | Defer (Phase 2) | |
| Pain library | `src/…` | — | Defer (Phase 2) | |
| Diagnostic scoring | `src/…` | — | Defer (Phase 2) | |
| Report generation | `src/…` | — | Defer (Phase 2) | |
| PDF | `src/…` | — | Defer (Phase 2) | |
| Diagnostic UI | `src/…` | — | Defer (Phase 2) | |
| Supabase schema | `supabase/…` | — | Discard | New PG/Flyway |
| Diagnostic auth | `src/…` | — | Discard | Keycloak |
| Lovable UI shell | `src/…` | — | Discard | Existing React |

### 0.3 Functional-equivalence harness plan

For each of the three providers, list:
- What input(s) the vyoog implementation takes
- What output(s) it produces
- Fixture companies/URLs/phones to use as test inputs
- How the equivalence comparison will be scripted (unit test comparing ported output against captured vyoog output)

**Exit criteria for Phase 0:**
- Gap table complete
- Migration map complete with per-file source references
- Equivalence harness plan complete
- Any specification conflicts identified and flagged (not silently resolved)
- All ten "sensible defaults" from doc 25 §13 confirmed applicable or overridden with an ADR

---

## Sequencing Rule

**Never start track N+1 until track N's acceptance criteria all pass.**

```
Phase 0 audit + migration map
        ↓
E1 — Enrichment framework (jobs table, provider SPI, evidence extension, orchestrator)
        ↓
E2 — Google Places provider
        ↓
E3 — Website (Firecrawl) provider
        ↓
E4 — Phone provider
        ↓
E5 — Company Detail Enrich UI + timeline + evidence viewer + candidates panel
        ↓
E6 — Bulk Enrich + jobs list + provider config admin + verification
```

E2, E3, and E4 are technically independent once E1 lands. If capacity allows and E1 is stable, they can run in parallel — but each still has its own acceptance gate.

---

## Track E1 — Enrichment Framework

**Goal:** The provider abstraction exists, the jobs table exists, evidence supports provider observations, one dummy provider proves the orchestrator works end-to-end.

**Scope**
- Migrations `V<next>__enrichment_jobs.sql`, `V<next+1>__evidence_extension.sql`, `V<next+2>__enrichment_candidates.sql`, `V<next+3>__company_google_fields.sql`, `V<next+4>__contact_phone_fields.sql`, `V<next+5>__provider_config.sql`
- `enrichment/framework/` package: SPI interfaces, orchestrator, job service, provider registry, idempotency service, rate limiter, cost tracker, evidence recorder, fact updater
- A minimal `MockProvider` implementing `EnrichmentProvider` for integration testing
- Bridging shim so existing `research/` AI flow continues to work unchanged
- ADR-0001 (in `/adr`): "Generalize research_queue → enrichment_jobs; existing AI research bridged, migration deferred to Phase 1.5"

**Acceptance criteria**
1. All six migrations apply cleanly from an empty database
2. Existing `companies`, `contacts`, `evidence`, `research/` rows and code paths are untouched
3. `MockProvider` end-to-end test: request → job row created → provider called → facts applied → evidence row created → timeline event emitted → job SUCCESS
4. Idempotency: second identical request within window returns cached result, no second provider call, cost 0
5. `force: true` bypasses idempotency
6. Rate limiter blocks correctly; job stays QUEUED with `scheduled_at` set
7. Transient failure → retry per policy; permanent failure → FAILED, no partial writes
8. Human-set field is not silently overwritten; candidate row created instead
9. Non-authorized role gets 403 on `/enrichment/*` endpoints
10. Existing `research/` AI flow continues to work (integration-asserted)
11. ADR-0001 written explaining the bridging plan

---

## Track E2 — Google Places Provider

**Goal:** Enriching a company via Google Places produces the correct facts, evidence, and timeline event; behavior matches vyoog-Diagnostic.

**Scope**
- `enrichment/google/` package: `GooglePlacesProvider`, `GooglePlacesClient`, field mapper, matcher
- API key management via `GOOGLE_PLACES_API_KEY` env var, never in frontend
- Preserve vyoog-Diagnostic's matching ladder and field-request set (Phase 0 confirms exact behavior)
- Provider registered with the framework
- Config row seeded in `provider_configs`

**Acceptance criteria**
1. Given a fixture company with name + pincode, provider returns Google Places result matching vyoog-Diagnostic output byte-for-byte on the field set (equivalence harness passes)
2. All 10 mapped fields land on `companies` correctly per doc 26 §5.2 policy (silent vs candidate)
3. `google_last_enriched_at` updated on every successful run
4. Raw response persisted as evidence with `provider_key='GOOGLE_PLACES'` and `enrichment_job_id` linking back
5. Timeline event created per successful run
6. Ambiguous match (multiple candidates same confidence) → picks vyoog's chosen candidate; flags ambiguity in evidence
7. No match → job SUCCESS with zero facts, evidence records attempts
8. API key never appears in frontend bundle or network response (asserted in build check)
9. Cost recorded correctly per call
10. Idempotency: re-run within 24h → cached result, 0 cost, no API call
11. Rate limiter respects `GOOGLE_PLACES.rate_limit_per_sec` and `rate_limit_per_day`
12. Provider transient failure (network) → retried per policy
13. Provider 403/API-key-invalid → FAILED_PERMANENT, no retry, clear error
14. Any behavioral deviation from vyoog documented in an ADR

---

## Track E3 — Website Provider (Firecrawl)

**Goal:** Website scraping via Firecrawl produces the same facts and candidates as vyoog-Diagnostic; behavior preserved.

**Scope**
- `enrichment/website/` package: `WebsiteProvider`, `FirecrawlClient`, `WebsiteFactExtractor`, `WebsiteSocialDetector`
- API key management via `FIRECRAWL_API_KEY`
- Preserve vyoog's Firecrawl call options, extraction patterns, page-selection logic (Phase 0 confirms)
- Provider registered with framework
- Config row seeded

**Acceptance criteria**
1. Given fixture URL, provider returns fact set matching vyoog-Diagnostic (equivalence harness passes)
2. `website_reachable`, `website_title`, `website_description` populated
3. Detected emails → candidates with `candidate_type='EMAIL_NEW_CONTACT'`
4. Detected phones → candidates with `candidate_type='PHONE_NEW_CONTACT'`
5. Detected address (when field empty) → silent fact; (when field set) → candidate
6. Detected products similarly
7. Social handles → silent facts on `social_*` columns
8. **No AI-interpreted insights produced** (Phase 2)
9. **No silent contact creation** — emails and phones only become candidates
10. Raw response persisted as evidence
11. Timeline event created per successful run
12. Firecrawl 404 / robots-block → FAILED_PERMANENT
13. Firecrawl timeout / 5xx → FAILED_TRANSIENT, retried
14. API key never in frontend
15. Any behavioral deviation from vyoog documented in an ADR

---

## Track E4 — Phone Provider

**Goal:** Phone enrichment produces normalized number + validity + region/type/carrier per vyoog-Diagnostic's implementation.

**Scope**
- `enrichment/phone/` package: `PhoneProvider`, `PhoneValidatorClient` (libphonenumber-java default), `PhoneFieldMapper`
- Provider registered with framework
- Config row seeded
- Applies to `companies.primary_phone_normalized` and each `contacts.phone`
- If vyoog uses a paid provider, implement its HTTP client here

**Acceptance criteria**
1. Given valid Indian mobile number, provider produces E.164 normalized value, `phone_country='IN'`, `phone_region` set, `phone_type='MOBILE'`, `phone_status='VALID'`
2. Given invalid number, `phone_status='INVALID'`, other fields set as best-effort
3. Given international number, correct country + region
4. All fields land on both `companies` (for primary) and `contacts` (for individual)
5. `phone_last_enriched_at` updated
6. Evidence row per call (even for offline libphonenumber — records "validated by libphonenumber v8.x")
7. Timeline event created
8. If paid provider used: cost recorded, rate-limited, retried on transient
9. Functional equivalence with vyoog confirmed (equivalence harness passes)
10. Any behavioral deviation from vyoog documented in an ADR

---

## Track E5 — Company Detail Enrich UI

**Goal:** Analysts can trigger enrichment on any company, see results in the timeline, and view evidence for any fact.

**Scope**
- Company Detail page: `Enrich ▾` dropdown with Google / Website / Phone / Run all
- Toast + link to batch on submit
- Timeline integration: enrichment events render correctly
- Evidence viewer: indicator next to enriched facts; modal shows provider, timestamp, job link, raw payload snippet
- Enrichment Candidates panel below Contacts: pending list with Accept / Reject buttons + "Accept all" / "Reject all"
- "Last enriched" timestamp per provider shown in the dropdown

**Acceptance criteria**
1. Enrich → single provider queues correctly; toast appears; timeline updates after job completes
2. Enrich → Run all queues one job per provider; timeline shows each event separately
3. Evidence indicator appears on every enriched fact
4. Clicking indicator opens modal with correct provider, timestamp, job link
5. Raw payload displays (inline for small, "View full" for large)
6. Candidates panel shows PENDING items only by default; toggle to show resolved
7. Accept → fact updated, new evidence row, candidate marked ACCEPTED, panel refreshes
8. Reject → candidate marked REJECTED, no facts changed
9. Accept all → all selected candidates applied atomically (single transaction; any failure rolls back all)
10. UI never shows API keys or secrets
11. Non-authorized user cannot see Enrich dropdown (feature-flagged by role)

---

## Track E6 — Bulk Enrich + Jobs List + Admin Config

**Goal:** Bulk enrichment works with the current filter; Admin can see all jobs and configure providers.

**Scope**
- Companies List: `Bulk Enrich ▾` next to Download button
- Bulk endpoint respects current filter; row-cap enforced
- `/settings/enrichment-jobs` — job list with filters (provider, status, batch, company, date range)
- Job detail: facts changed, candidates created, evidence, error
- `/settings/enrichment-providers` — Admin edit rate limits, timeouts, retries, idempotency window, cost per call, options JSON
- Notifications: user gets in-app notification when their batch completes

**Acceptance criteria**
1. Bulk Enrich for filter with N companies queues N jobs per selected provider (up to cap)
2. Over cap → 422 with narrow-filter guidance
3. Batch id returned; jobs list filterable by batch id
4. Batch completion notification appears
5. Jobs list renders <300ms with 10k rows
6. Job detail shows all relevant info per job
7. Admin can edit provider config; changes take effect on next call (no restart)
8. Non-admin gets 403 on `/settings/enrichment-providers`
9. Bulk enrich respects rate limits per provider (spread over time as slots free up)
10. Existing AI research flow still works alongside (bridging shim tested one more time)

**Implementation Note (2026-09-24):** delivered so far is `BatchEnrichPanel.tsx` on `/enrichment/jobs` — client-side multi-select + concurrency-3 orchestration over the existing single-company `POST /companies/{id}/enrich` (detail in doc 26 §9.2). This covers acceptance criteria 6 (job detail via existing single-company job records) and the general "bulk enrich works" intent, but criteria 1–4 and 9 (server-assigned batch id, over-cap 422, batch-filterable jobs list, batch-completion notification, provider-side rate-limit spreading across a batch) are not yet implemented — there is no server-side bulk endpoint. Track E6 stays open until those are addressed.

---

## Release Gate

Do not mark Phase 1 complete until:

- backend build passes
- frontend build passes
- clean migration from empty database passes
- All migrations from Phase 0 audit confirmed additive (no existing table recreated)
- All six tracks' acceptance criteria pass with evidence
- Functional-equivalence harnesses pass for all three providers (byte-for-byte or documented-difference-with-ADR)
- No changes to Company Map (still owned-only, no Google/Overpass reintroduced)
- No changes to Companies List columns beyond what enrichment fills (list rendering untouched)
- No changes to ICP Qualification, Tier engine, or scoring
- No changes to Company Default Filter Settings behavior
- Existing `research/` AI flow untouched (bridging shim verified)
- API keys never in frontend (build-time check + integration test)
- Every mutation audited
- 403 tests pass for every mutation
- OpenAPI accurate for all new endpoints
- README updated with enrichment section
- `/adr` folder holds one file per behavior-changing decision (empty if none beyond ADR-0001)

---

## Scope Control

Do not implement in Phase 1:

- Any Phase 2 diagnostic capability (benchmarks, pain library, observations, scenarios, product modifiers, reports, PDF, diagnostic UI, diagnostic scoring)
- AI interpretation of website content
- Automatic contact creation from website scraping (candidates only)
- Scheduled/automated enrichment sweeps
- Providers beyond Google Places, Website, Phone (framework supports; don't build)
- Adding external results back to the Company Map (map stays owned-only)
- Migrating existing `research/` AI flow onto `enrichment_jobs` (Phase 1.5 follow-up)
- Changes to Company Default Filter Settings, Companies List rendering, or import processing
- ICP Qualification, Tier, or scoring changes
- Company relationships

unless an existing dependency makes a minimal implementation necessary, or the Requirements doc is formally changed.
