<!--
Document: 25-Enrichment-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: Phase 1 — Enrichment infrastructure ported from vyoog-Diagnostic (Phone · Website · Google Places)
Audience: Product, Engineering, QA, Claude Code
Companion to: docs 01–24
-->
# ProspectSoul — Phase 1 Enrichment: Requirements

**Version:** 1.0
**Status:** Ready for implementation
**Owner:** Senthil, COO — Vyoog Information Private Limited

---

## 1. Purpose

Port the **enrichment infrastructure** currently living inside the vyoog-Diagnostic Lovable application into ProspectSoul, as a first-class architectural module.

Phase 1 only:
- **Phase 1A** — backend enrichment framework + three providers (Phone, Website via Firecrawl, Google Places)
- **Phase 1B** — Company → Enrich UI + data-flow verification

Phase 2 (benchmark logic, pain library, observations, scenarios, product modifiers, narrative reports, PDF generation, diagnostic UI) is **out of scope here** and will be specified separately after Phase 1 lands and Sales confirms enrichment data quality.

**Core direction from the product team** (informing every decision in this doc):

> "I wanna do the company details scraping like we did in the file code base."

Read: this is a **port**, not a redesign. Where vyoog-Diagnostic already implements a scraping behavior, Claude Code reproduces it in ProspectSoul's architecture. Different HOW (Spring Boot, Postgres, ProspectSoul modules) — same WHAT.

---

## 2. Authoritative Context

Docs read in order:

1. ProspectSoul PRD v1.1 (and PRD v2.0 amendments)
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. Developer Guide / CLAUDE.md
7. Docs 01–18 (Company Management vertical slice — implemented)
8. Docs 19–22 (Sales Intelligence Extension — implemented, with current-state deltas in §3 below)
9. **This document (23) and its siblings 24, 25, 26**

If any base doc conflicts with this one for enrichment concerns, this document is authoritative for the enrichment scope and the affected base doc gets an ADR-recorded amendment at implementation time (see doc 28).

---

## 3. Current-State Acknowledgments — What Is Already Built

Several things in the recent Sales Intelligence work landed and evolved beyond what docs 19–22 described. Claude Code must **not rebuild these**:

| Area | State | Implication for Phase 1 |
|---|---|---|
| Company Default Filter Settings + `company_default_filter_settings` table | Built | Bulk enrich just consumes current filter (which already includes defaults via `apply_defaults`) — no changes needed |
| Expanded Companies List (contact phone, person, role, location, address, region, products, email, website, turnover, GST, employees, pincode, NIC) | Built | Enrichment just produces facts that populate these columns — no list-rendering changes |
| Bulk-loaded primary contact + NIC on the list | Built | Enrichment must be efficient at the same scale — don't N+1 the list |
| Sticky headers + horizontal scroll on the list | Built | Skip |
| All new filter fields (region, district, pincode, turnover range, employee range, GST-present, NIC parent, NIC descendants, contact-role) | Built | Bulk enrich filter input matches these |
| Improved import processing: pincode dedup, name+pincode dedup, in-memory dedup, NIC master cache during imports, raw-code preservation | Built | Enrichment runs POST-import; nothing to change in import path in Phase 1 |
| Import dedup indexes (phone / website domain / source ref / name+city / name+pincode) | Built | Reused when "+ Add" or enrichment produces candidate matches |
| Company Map — **owned-only**, external Google/OpenStreetMap/Overpass removed | Built (reversed from doc 20) | See §4 below — Google Places is enrichment-only in Phase 1, never map-integrated |

### 3.1 The Company Map reversal — important architectural implication

Doc 20 §7 originally specified `/companies/map` with two tabs (owned + external via Google Places) and a "+ Add external" flow. That external integration has since been removed. The map is owned-only and stays that way.

**What this means for Phase 1:**
- Google Places IS still needed — as an **enrichment provider**, not a map feature
- Per-company: Google returns place_id, formatted_address, phone, website, category, coordinates, business status
- Coordinates from Google Places feed into the owned map (pins appear once a company has been Google-enriched)
- No "+ Add from map" flow, no external tab, no external result cards
- One purpose per feature: map = look at owned data, enrich = pull fresh data per company

---

## 4. Architectural Amendments (Prerequisites)

An architecture review by the product team identified six additions ProspectSoul needs before enrichment ports in. These are **not optional** and apply to all future enrichment work too.

### 4.1 Enrichment is a first-class module

**Wrong** — external calls inside `CompanyService` or `research/`:
```
company/CompanyService.scrapeWebsite()
company/CompanyService.callGoogle()
research/ResearchWorker.callPlaces()  ← contaminated
```

**Right** — dedicated module with provider abstraction:
```
enrichment/
  framework/     provider interface, job runner, retry, rate-limit, cost, provenance
  google/        Google Places provider
  website/       Website provider (Firecrawl)
  phone/         Phone provider
  ...            future: GST, HLR, LinkedIn, IndiaMART, TradeIndia
```

`CompanyService` never calls an external API. It asks the enrichment orchestrator, which asks a provider, which returns a result the framework turns into facts + evidence + activity.

### 4.2 Generalize `research_queue` → `enrichment_jobs`

External calls share the same operational shape: external dependency, potentially slow, failure/retry, rate limits, asynchronous execution, provider-specific errors, status, provenance. One mechanism, not five.

**Migration path:**
- New `enrichment_jobs` table is created and used for ALL new providers
- Existing `research/` AI flow continues to work unchanged in Phase 1 via a bridging shim
- Migrating existing AI research to `enrichment_jobs` is a Phase 1.5 follow-up track (small, safe, standalone) — an ADR records the plan

### 4.3 Facts, Evidence, Scores — three distinct concepts

The current architecture blurs these. Phase 1 formalizes them.

| Concept | Where it lives | Owned by | Example |
|---|---|---|---|
| **Fact** | Column on `companies` / `contacts`, OR row in `company_facts` for per-provenance tracking | Company/Contact module | `phone_status = VALID`, `google_place_id = ChIJ…`, `turnover_slab = 5-10 Cr` |
| **Evidence** | `evidence` table (extended) | Enrichment framework | "Google Places returned this JSON on 2026-09-23 at 14:22 IST" |
| **Score** | Derived at read time from facts + rules; snapshotted for reports | Scoring/qualification module (unchanged; not touched by Phase 1) | `Conversion Likelihood = 72`, `Tier = A` |

Rule: **a fact must always trace to at least one evidence row.** A score must always be recomputable from current facts.

### 4.4 Evidence extends to any observation, not just AI claims

The current `evidence` table is AI-shaped (URL + excerpt + AI model). It must extend to observations from Google Places, Firecrawl, phone providers, Udyam, GST, human calls — anything that produces a claim.

Additive extension only. Existing AI-evidence rows remain valid.

### 4.5 Provider abstraction with operational plumbing built in

Every provider implements the same interface. The framework provides: rate limiting per provider, retry with exponential backoff, timeout per call, idempotency (cached result within a window), cost tracking, failure state (transient vs permanent), raw response persistence, audit + activity.

Providers write the API call and the result mapping. Everything else is framework.

### 4.6 Enrichment contributes to the existing Company Timeline

No parallel history system. Every run creates a timeline event: *"Google Places enrichment ran — 3 facts added, 1 fact updated, 1 evidence row"*. Timeline composition already exists.

---

## 5. Phase 1 Scope

### 5.1 Enrichment framework (Phase 1A)

- `enrichment/` module (§4.1)
- `enrichment_jobs` table (§4.2)
- Provider abstraction (§4.5)
- Evidence extension (§4.4)
- Fact tracking pattern (§4.3)
- Enrichment orchestrator: given `(company_id, providers)` runs each provider, persists facts + evidence, contributes timeline events

### 5.2 Google Places provider (Phase 1A)

Port from vyoog-Diagnostic. Produces:

- `google_place_id` (fact)
- `google_name` — Google's canonical business name (fact, informational)
- `google_formatted_address` (fact → surfaces `address_line` if empty; candidate otherwise)
- `google_phone` (fact → candidate for `primary_phone_normalized` if empty; never silent overwrite)
- `google_website` (fact → candidate for `website_domain`)
- `google_business_category` / `types[]` (fact)
- `google_maps_url` (fact)
- `google_lat`, `google_lng` (fact — feeds owned map pins)
- `google_business_status` — OPERATIONAL / CLOSED_TEMPORARILY / CLOSED_PERMANENTLY (fact)
- Raw Google response persisted as evidence

**Matching rules** (preserve source behavior — Claude Code confirms from vyoog-Diagnostic):
- If `google_place_id` already exists on the company → refresh only that place
- Otherwise: name + pincode → name + city → name only (with city bias)
- Multiple candidates → top-scored by Google confidence + local name similarity; flag ambiguity in evidence

### 5.3 Website provider — Firecrawl (Phase 1A)

Port from vyoog-Diagnostic **exactly as implemented there.** Given a website URL (from `website_domain`, or freshly discovered via Google Places), Firecrawl fetches and returns structured content.

**Facts extracted (deterministic only):**
- `website_reachable` — true/false (fact)
- `website_title` — page title (fact)
- `website_description` — meta description (fact)
- Detected email addresses — candidates for contact creation
- Detected phone numbers — candidates for contact creation
- Detected address blocks — candidates for `address_line`
- Detected product mentions if Products/Services page exists — candidates for `products`
- Detected social handles (LinkedIn, Facebook, X, YouTube, Instagram) — stored as facts, no follow-up scraping in Phase 1

**Not extracted in Phase 1** (Phase 2 territory):
- Pain points, competitive positioning, operational maturity, sentiment, AI-interpreted insight

Raw Firecrawl response persisted as evidence.

**Preserve from source** (Claude Code confirms and reproduces):
- Firecrawl call options (crawl depth, wait for JS, respect robots, etc.)
- Extraction heuristics for email, phone, address
- Error handling on 404 / timeout / redirect chains

### 5.4 Phone provider (Phase 1A)

Port from vyoog-Diagnostic. Claude Code identifies which provider vyoog uses (libphonenumber-based validator, third-party API like Twilio Lookup, or a custom implementation) and ports it.

Given a phone number, produces:
- `phone_normalized` — E.164 (fact)
- `phone_country` (fact)
- `phone_region` — state/circle for Indian numbers (fact)
- `phone_carrier` — where provider returns it (fact)
- `phone_type` — MOBILE / LANDLINE / VOIP / UNKNOWN (fact)
- `phone_status` — VALID / INVALID / UNREACHABLE / DND (fact)
- `phone_dnd_registered` — where provider returns it (fact)

Applied to company's `primary_phone_normalized` and every contact's phone.

Raw provider response persisted as evidence.

### 5.5 Enrichment UI on Company Detail (Phase 1B)

Company Detail page gains an **Enrich** control:

```
[ Enrich ▾ ]
   ├── Google Places
   ├── Website (Firecrawl)
   ├── Phone (all)
   └── Run all
```

Each fact on the page shows a small evidence indicator; click opens the evidence viewer showing the source (provider, timestamp, raw payload snippet).

Timeline shows each enrichment run.

### 5.6 Bulk Enrich on Companies List (Phase 1B)

Top-right of Companies List, next to Download:

```
[ Bulk Enrich ▾ ]
   ├── Google Places (340 companies)
   ├── Website (340)
   ├── Phone (340)
   └── Run all (340)
```

- Operates on the current filter selection
- Row-count cap enforced (default: same as `COMPANIES_DOWNLOAD_ROW_CAP`)
- Rate-limited per provider (framework handles)
- Returns immediately with a batch ID; user watches progress on `/settings/enrichment-jobs` (Admin) or a lightweight per-user "recent enrichments" widget

### 5.7 Enrichment jobs view (Phase 1B)

`/settings/enrichment-jobs` for Admin — table of recent jobs with filter (provider, status, date range, company), links to detail (which facts changed, evidence rows created, error if failed, cost consumed).

### 5.8 Data-flow verification (Phase 1B)

Verified in integration tests:
- All facts written to `companies` / `contacts` are visible in the normal list and detail views
- Every fact traces to at least one evidence row
- Every enrichment run contributes exactly one timeline event
- Every enrichment job row shows status, cost, provider, started_at, completed_at, error
- Bulk enrich respects the row-count cap and rate limits

---

## 6. What Is Explicitly Out of Scope

From vyoog-Diagnostic, **do not port in Phase 1:**

- Benchmark logic
- Calculations engine (any diagnostic scoring)
- Pain libraries
- Observations engine
- Scenarios engine
- Product modifiers
- Narrative / report generation
- Report editing UI
- PDF generation
- Diagnostic UI (questions, workflow, results screens)
- Diagnostic authentication (reuse ProspectSoul Keycloak)
- Any Supabase-specific table or migration (target is PostgreSQL / Flyway)
- Lovable UI shell (target is existing ProspectSoul React)

Also out of scope for Phase 1:

- Scheduled/automated enrichment sweeps (all runs user-triggered)
- GST, HLR, LinkedIn, IndiaMART, TradeIndia providers (framework supports; providers not built)
- AI-interpreted analysis of website content (Phase 2)
- Automatic contact creation from extracted website emails (extracted as candidates only, surfaced in a review panel — see §5.5 / §14 item 10)
- Replacing the existing `research/` AI-research flow (migrates to `enrichment_jobs` in a Phase 1.5 follow-up)
- ICP Qualification, Tier engine, or scoring changes
- Company relationships (parent/subsidiary/plant)
- Adding Google Places / OpenStreetMap / Overpass back to the map — the reversal in §3.1 stands

---

## 7. Vyoog-Diagnostic Migration Map

Claude Code will inspect both codebases and produce a detailed map in Phase 0 (doc 27). Reference expectations:

| Vyoog Diagnostic capability | Phase 1 disposition |
|---|---|
| Firecrawl integration | **Port** as `enrichment/website/` — preserve all options |
| Google Business / Places integration | **Port** as `enrichment/google/` — preserve matching logic |
| Phone validation / lookup | **Port** as `enrichment/phone/` — preserve validator choice |
| API credentials / secrets | **Port** into ProspectSoul config; server-side environment (never frontend) |
| Website "intelligence" (AI-analyzed insights) | **Defer to Phase 2** |
| Benchmarks, pain library, observations, scenarios | **Defer to Phase 2** |
| Report generation, editing, PDF | **Defer to Phase 2** |
| Diagnostic UI | **Defer to Phase 2** |
| Diagnostic authentication | **Discard** — use Keycloak |
| Supabase schema | **Discard** — new PostgreSQL migrations |
| Supabase data | **Not migrated in Phase 1** — decide at Phase 2 boundary |
| Lovable UI shell | **Discard** — use existing ProspectSoul React |

---

## 8. Preserve-Functionality Rule

Phase 1 is a **port**, not a redesign of what enrichment does.

- If vyoog's phone validator accepts a number in a certain format, the ported provider accepts the same format
- If vyoog's Google matching prefers name + pincode over name + city, the port preserves that preference
- If vyoog's Firecrawl call uses specific options (waitFor, timeout, crawl depth), those options are preserved
- If vyoog's website extraction uses specific regex patterns for phone/email/address, those patterns port over
- Changes to how enrichment WORKS are recorded in an ADR and require explicit justification

**What DOES change:**
- Where the code lives (ProspectSoul modules, not Lovable/Supabase)
- Architectural shape (provider abstraction, evidence trail, timeline event)
- Persistence (PostgreSQL via JPA/Flyway, not Supabase)
- Frontend (existing React shell, not Lovable UI)
- Authentication (Keycloak, not Supabase auth)

---

## 9. Business Invariants (Additions)

Extending Domain Model v1.0 §5 and additions from doc 19 §7:

19. **A fact must trace to at least one evidence row.** No fact without provenance.
20. **A score is never persisted as a "current value"** — derived at read time or snapshotted for a report. Facts persist; scores are computed.
21. **External API responses are persisted as evidence** with a retention rule (Google Places raw response kept 90 days minimum; longer for anything referenced by a report snapshot).
22. **An enrichment run is idempotent within its idempotency window** (24h default). Re-running Google Places for the same company within 24h returns the cached result and does not consume additional quota.
23. **Enrichment never silently overwrites human-entered data.** Google's phone becomes a candidate for a human-set `primary_phone_normalized`, not an automatic overwrite.
24. **Every provider call carries a cost dimension.** Even zero-cost calls record the field.
25. **An enrichment run always creates one timeline event on the company**, even when the run adds zero new facts.

---

## 10. Vocabulary

| Term | Means | Never means |
|---|---|---|
| Enrichment | The act of asking an external provider for information about a company or contact | Diagnostic analysis, AI interpretation, or scoring |
| Provider | An external information source (Google Places, Firecrawl, phone validator) | A ProspectSoul-internal service |
| Fact | A current-value attribute on a Company or Contact | An evidence row or a computed score |
| Evidence | A row recording where a fact came from | The fact itself |
| Score | A derived, recomputable value (Tier, Conversion Likelihood) | A fact |
| Enrichment Job | One row in `enrichment_jobs` for one provider run on one entity | A batch — a batch produces N jobs |
| Enrichment Run | An orchestrated set of provider calls for one company | A single API call |
| Diagnostic | The Phase 2 module (benchmarks, pain library, reports) | Enrichment (Phase 1) |

---

## 11. Users and Roles

No new roles. Existing roles gain capabilities:

| Capability | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO |
|---|---|---|---|---|---|
| Run enrichment on one company | ✓ | ✓ | ✓ | – | – |
| Bulk enrichment on filter | ✓ | ✓ | ✓ | – | – |
| View facts, evidence, timeline | ✓ | ✓ | ✓ | ✓ | ✓ |
| View `enrichment_jobs` status | ✓ | ✓ | ✓ | ✓ | ✓ |
| Configure providers, keys, quotas | – | – | ✓ | – | – |
| View cost totals across providers | – | ✓ | ✓ | – | ✓ |

---

## 12. Acceptance Criteria (Summary)

Full per-track in doc 27. High-level:

- All three providers implement the common `EnrichmentProvider` interface
- Every provider call flows through the framework's rate-limit, retry, timeout, idempotency, cost, and provenance plumbing
- One company can be enriched by all three providers in one run; timeline shows one event per provider
- Every fact traces to at least one evidence row
- Enrichment never silently overwrites human-entered facts
- Bulk enrich respects the row-count cap
- API keys never appear in a frontend bundle
- Functional equivalence with vyoog-Diagnostic is demonstrated (Claude Code produces a comparison report as part of the migration map)
- Existing AI research flow continues to work unchanged (bridging shim)
- No changes to ICP Qualification, Tier engine, or reports
- No changes to Company Map (stays owned-only)

---

## 13. Sensible Defaults (Assumed — Documented for Override)

Where a design choice was open, defaults are assumed. Any of these can be overridden with an ADR at implementation time.

| # | Decision | Assumed default |
|---|---|---|
| 1 | Phone provider | Whatever vyoog-Diagnostic uses (Claude confirms from source). If unclear or missing, use libphonenumber for format + basic validation; document as a placeholder pending Admin decision on a paid provider |
| 2 | Firecrawl account & billing | Config-driven; Admin sets `FIRECRAWL_API_KEY` and `FIRECRAWL_DAILY_QUOTA` |
| 3 | Google Places billing | Config-driven; Admin sets `GOOGLE_PLACES_API_KEY` and `GOOGLE_PLACES_DAILY_QUOTA` (separate from any Maps JS key) |
| 4 | Idempotency window | 24 hours default, per-provider configurable |
| 5 | Cost currency | Recorded in USD as returned by providers; `PROVIDER_USD_TO_INR_RATE` config setting used for INR display; both stored |
| 6 | Firecrawl extraction depth | Home page only in Phase 1 (matches "port not redesign"); config knob to expand |
| 7 | Overwrite-confirmation UX | Candidates surfaced in a review panel per company; user accepts individually or bulk-accepts all for that company |
| 8 | Bulk enrich row cap | Same as `COMPANIES_DOWNLOAD_ROW_CAP` |
| 9 | Existing research migration | Bridging shim keeps existing AI flow working in Phase 1; actual migration deferred to Phase 1.5 follow-up track |
| 10 | Website-extracted contact candidates | Land in "Enrichment candidates" panel on Company Detail; promote-to-contact requires human confirmation |

---

## 14. Open Items Still Requiring Decision

Non-blocking for Track E1 but should be resolved before Track E5 (UI):

- Ops confirmation of Firecrawl and Google Places billing owners
- Confirmation that libphonenumber is acceptable as a placeholder if vyoog's phone provider isn't clearly identifiable
- Final decision on whether Phase 1.5 (migrate existing AI research to `enrichment_jobs`) runs before or after Phase 2 begins
