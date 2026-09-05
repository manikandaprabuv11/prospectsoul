# ProspectSoul — Product Requirements Document
**Version:** 2.0 · **Date:** August 2026 · **Owner:** Senthil, COO — Vyoog Information Private Limited
**Status:** Design Complete, Ready for Build (Phase 1)
**Supersedes:** PRD v1.1 (July 2026). Companion documents (Domain Model v1.0, Tech Spec v1.0, UI/UX Spec v1.0, Implementation Roadmap v1.0) remain valid except where amended in Section 14.

---

## Changelog — v1.1 → v2.0

Driven by two months of GTM work: discovery of bulk government datasets (Udyam district extracts, RoC master dump), the CODISSIA member extraction, the statutory-source acquisition plan, and the three-way design review (Product Owner + Claude + ChatGPT). Core v1.1 architecture (company anchor, pipeline states, evidence model, append-only history, timeline) is unchanged. What changed:

1. **Government registries become the primary import sources.** Udyam, RoC/MCA master data, and CODISSIA replace generic-CSV-first thinking. NIC codes are the deterministic industry classification — no AI guessing at verticals in Phase 1.
2. **NIC Ontology table** — Admin-editable mapping of NIC codes → Vyoog verticals → priority weight. The ICP is now defined empirically (seeded from the NIC signature of existing clients).
3. **Geographic access is a first-class feature** (new Section 8): pincode/area/region filtering plus **adjustable radius search** — the field-sales killer feature.
4. **Industrial Estate extraction** from address text + Estate Density view for field-route planning.
5. **Two computed scores** (Conversion Likelihood, Support Risk) + three designed-but-null score dimensions (Implementation Risk, Expansion Potential, Strategic Fit). Facts stored as fields; scores are recomputable formulas.
6. **Keep everything, queue selectively:** no record is deleted for being below turnover threshold; the Call Queue only surfaces records above threshold. Revenue slab is a score input, not a delete rule.
7. **`turnover_slab` and `phone_status`** fields with checked-at dates (GST AATO; HLR/DND validation).
8. **Capability tags** — controlled vocabulary (CNC_TURNING, SAND_CASTING, FABRICATION, PROJECT_ERECTION, …) on companies; tags in v2.0, first-class entity deferred.
9. **Structured call-outcome capture** — telecaller and field-visit outcomes as enum fields, not free text. This is the feedback loop's day-one foundation.
10. **Phasing restructured** (Section 3): Phase 1 is a complete, production-usable system with zero AI dependency; AI enrichment moves to Phase 2.
11. AI website research, GST/HLR API automation, diagnostic-tool (Supabase) integration, relationship harvest, CRM API sync: **explicitly phased, not dropped** (Sections 3, 12).
12. **Graph-shaped schema, no graph technology:** relationship tables designed into the data model; only estate/association/NIC relations built in Phase 1.

---

## 1. Overview

### 1.1 Problem Statement (updated)
Vyoog now possesses the raw population data it lacked: Udyam district extracts (800+ Coimbatore manufacturers with NIC codes), the RoC company master dump, and 1,400 parsed CODISSIA members with contacts. The bottleneck has moved from *finding companies* to:
- Merging overlapping sources into one canonical record per company
- Qualifying by ICP (NIC vertical + geography + turnover band) so telecallers dial only fit companies
- Filling gaps (website, phone) for shortlisted companies only, and verifying phones before dialing
- Routing field sales efficiently by estate/pincode/radius (98% of MD meetings happen via unannounced visits, not appointments)
- Remembering every touch so the 300 historical quotations and all future calls compound instead of evaporating

### 1.2 What ProspectSoul Does (v2.0 statement)
The single source of truth for every manufacturing company Vyoog has looked at, is calling, or will call — imported from government registries and association lists, deduplicated into canonical records, classified by NIC-driven ICP, scored, geographically searchable, and feeding a prioritized Call Queue whose outcomes are captured back into the record.

### 1.3 What ProspectSoul Is NOT (unchanged from v1.1, plus)
- Not a CRM / opportunity manager — it ends where the CRM begins
- Not a scraper of IndiaMART/JustDial/LinkedIn
- Not an AI research platform in Phase 1 — zero LLM calls in Phase 1
- Not a data-collection project: a field exists only if it improves the call/visit/route decision

### 1.4 Success Criteria (Phase 1)
- All current datasets (Udyam TN districts, RoC TN, CODISSIA, existing clients, historical quotations) loaded, deduplicated, and searchable — one canonical record per company
- A telecaller opens the Call Queue and every entry is ICP-fit, above the turnover threshold (where known), phone-status ACTIVE where validated, with MD name where known — "which 50 companies do we call this week" answered by a sort
- A field-sales user selects a pincode + radius (or an estate) and gets a ranked visit list in under 5 seconds
- No existing client or open quotation is ever cold-called (dedup guarantee)
- Every call/visit outcome is recorded as structured fields on the timeline
- Zero AI spend; total external spend limited to GST slab lookups + HLR checks on the shortlist

---

## 2. Users and Roles (unchanged from v1.1)
Research Analyst · Sales Lead · Admin · Viewer · COO — Keycloak realm `vyoog`, roles `PS_ANALYST`, `PS_SALES_LEAD`, `PS_ADMIN`, `PS_VIEWER`, `PS_COO`. Plus one addition:

| Role | Addition in v2.0 |
|---|---|
| **Telecaller** (`PS_TELECALLER`) | Sees Call Queue and company detail; records call outcomes; cannot import, merge, configure, or export |

---

## 3. Phasing

**Phase discipline:** each phase is production-usable on its own. Phase 1 is not a POC — it is the complete daily working tool for the sales team. Later phases add enrichment and automation around it.

### Phase 1 — The Working Tool (build now)
Import & merge (government sources + CSV) · NIC Ontology & ICP classification · turnover_slab + phone_status as manually/CSV-updated fields · Estate extraction · **Geographic search with radius** · Scoring (2 computed) · Call Queue · Call/visit outcome capture · Company timeline · Search · Exports · Admin config · Reports (pipeline, source funnel, estate density).

### Phase 2 — Enrichment Automation
GST AATO lookup via GSP API (bulk + quarterly re-run with slab-movement triggers) · HLR/DND validation via provider API (batch + pre-export re-check) · AI website research with evidence (v1.1 Stage 3, incl. non-HTML extraction reusing diagnostic-tool logic) · Review Queue · AI qualification suggestions · quotation-revival trigger flags.

### Phase 3 — Intelligence & Integration
Diagnostic-tool (Supabase) feedback loop — saved analyses become verified activities · relationship harvest (clients/suppliers named on websites, one-hop, gated) · similarity ("companies like our best clients") · CRM API sync (replaces CSV export) · scores 3–5 computed once worksheet/CustSoul data exists.

### Parked (recorded, not scheduled)
Manufacturing knowledge-graph infrastructure · ML-trained scoring · ERP-embedded diagnostic add-on · SQLE-style automated collection.

---

## 4. Phase 1 — Functional Requirements

### 4.1 Import Framework (extends v1.1 Stage 0)
All v1.1 import behavior stands (raw row capture, batch report, nothing silently dropped, batch-of-one manual entry). Changes:

**Import Sources enum (updated):**
| Value | File reality |
|---|---|
| `UDYAM` | data.gov.in district extract — TSV/CSV; `Activities` column is JSON array of `{NIC5DigitId, Description}` |
| `ROC_MCA` | RoC company master dump — CIN, CompanyName, CompanyClass, PaidupCapital, CompanyStatus, nic_code, Registered_Office_Address, … |
| `CODISSIA` | Parsed member CSV — company, products, contact_persons, emails, icp_verticals |
| `TNPCB` | Consented industries list (foundry census) |
| `TENDER_AWARD` | GeM / TN eProcurement award rows |
| `EXHIBITOR` | INTEC / IMTEX catalogue data entry |
| `ASSOCIATION` | IIF / SIEMA / EEPC directories |
| `CLIENT_LIST` | Existing Vyoog clients — flags `is_existing_client` |
| `QUOTATION_HISTORY` | Historical quotations — creates company + QUOTATION_SENT activity with date/value |
| `EXCEL_CSV` / `MANUAL_ENTRY` | As v1.1 |

**Import templates** ship pre-configured for UDYAM, ROC_MCA, CODISSIA, CLIENT_LIST, QUOTATION_HISTORY formats (column mappings from the actual files already in hand). Template mapping must support: JSON-array column parsing (Udyam Activities), pincode extraction from address, and per-source default tags.

**Normalization additions** (extends v1.1 Stage 1 / Tech Spec §5):
- NIC codes: extract all codes per row; store as array; unknown/malformed kept + flagged
- Pincode: extract 6-digit pincode from address when the column is absent; validate against pincode reference table
- **Estate extraction:** detect industrial estate/area/park names in address text (patterns incl. SIDCO/SIPCOT, "X Industrial Estate/Area/Park", with survey-number prefix stripping); store normalized `estate` field; unmatched → blank, address preserved
- Company status: RoC rows with STRIKE OFF / DISSOLVED / LIQUIDATED are imported but auto-tagged `defunct` and excluded from queue by default

**Dedup (extends v1.1 Stage 2):** match rules in order — ① CIN exact ② normalized phone ③ website domain ④ normalized name + pincode ⑤ normalized name + district. Cross-source merge is the primary Phase 1 triage workload; the triage UI must show per-source field provenance. `source_count` (how many independent sources confirm this company) is stored and score-relevant.

**Absolute rule:** rows matching `CLIENT_LIST` or open `QUOTATION_HISTORY` records merge into those records and are excluded from the Call Queue (clients permanently; quotations routed to the Revival view instead).

### 4.2 NIC Ontology & ICP Classification
**`nic_ontology` (Admin-editable):** nic_code · vyoog_vertical (FOUNDRY / MACHINE_SHOP / FABRICATION / PUMPS_VALVES / MACHINE_BUILDER_PROJECT / TOOLING_DIES / FORGING_STAMPING / ELECTRICAL_EQUIP / OTHER) · priority_weight (0–10) · active. Seeded from the cross-reference script's ICP_NIC set **plus every NIC code carried by existing clients** (the empirical client signature). 3-digit prefix fallback rows supported for tagging only.

Every company's `icp_verticals` derives from its NIC codes via this table (plus CODISSIA-declared verticals on merge). Editing the ontology re-derives on demand (Admin action, audited).

**Capability tags:** controlled vocabulary (~25 terms, Admin-editable list) applied from products/NIC-description text by deterministic keyword rules at import; manually editable per company. Tags, not entities.

### 4.3 Qualification Fields (facts before scores)
On `companies`:
- `turnover_slab` enum: UNKNOWN / LT_1_5CR / SLAB_1_5_TO_5CR / SLAB_5_TO_25CR / SLAB_25_TO_100CR / GT_100CR + `slab_checked_at`. Phase 1 population: bulk CSV update screen (paste GSTIN→slab results from manual/GSP lookups); Phase 2 automates.
- `einvoice_enabled` bool + checked_at (crossed-₹5-Cr signal)
- `current_systems` multi-select: TALLY / EXCEL / WHATSAPP_COORD / LEGACY_ERP / OTHER_ERP / NONE (same vocabulary as the diagnostic tool)
- `website_status`: HAS_WEBSITE / NO_WEBSITE / DEAD / UNKNOWN

On `contacts`:
- `phone_status`: ACTIVE / INACTIVE / DND / LANDLINE / UNKNOWN + `phone_checked_at`. Phase 1 population: bulk CSV update (HLR provider batch results); Phase 2 automates. Export and Call Queue display staleness of the check.

### 4.4 Scoring
Stored as columns; computed by a formula versioned in Admin settings (weights editable without code change). Recompute is idempotent and re-runnable.

- **`score_conversion` (computed):** source_count, ICP vertical match & priority weight, turnover_slab ≥ configured threshold, contactability (MD-named contact, email, ACTIVE phone), estate density bonus, exhibitor/tender/association tags. 0–100.
- **`score_support_risk` (computed, heuristic v1):** baseline 50; − if incorporated (CIN); + if ≥4 mixed capabilities; + if PROJECT_ERECTION capability; ± overrides from the client-profitability worksheet weights when loaded (Admin-uploadable weight set). 0–100, higher = riskier.
- **`score_implementation_risk`, `score_expansion_potential`, `score_strategic_fit`:** columns exist, null, not computed in Phase 1. UI hides null scores.
- **`priority_rank`** = f(conversion, support_risk) — the Call Queue sort key. Formula in settings.

**Queue threshold rule:** companies with `turnover_slab` below the configured minimum (default: SLAB_5_TO_25CR) or UNKNOWN-and-flagged-micro are **retained, scored, and excluded from the default Call Queue** — visible via filter, never deleted. Slab changes (Phase 2 re-runs) can promote them automatically.

### 4.5 Geographic Access — the Field-Sales Feature
**Requirement:** field team plans routes by area, not by company. The system must answer "who should I visit around here today?"

- **`pincode_reference` table:** pincode · office_name · district · state · lat · lng. Seeded for Tamil Nadu (+ configurable states) from the open All-India pincode directory with coordinates. Admin can upload/refresh the reference CSV.
- Every company gets `pincode` (from data or address-extracted) and derived `lat`/`lng` via pincode centroid. Precision is pincode-level in Phase 1 — sufficient for routing; per-address geocoding is Phase 3.
- **Radius search:** on Companies and Call Queue screens — center = pincode, area name, estate, or an existing company; **radius slider adjustable** (1–100; unit km/miles as an Admin/user setting, default km); results ranked by `priority_rank`, distance shown per row. Implemented with PostgreSQL `earthdistance`/`cube` (or PostGIS if preferred — implementer's choice, no external geo service).
- **Region/area filters:** state → district → area/taluk (from pincode reference office names) → pincode → estate, combinable with all other filters.
- **Estate Density view:** estates ranked by ICP-fit prospect count; columns: estate, district, prospect count, vertical mix, avg conversion score, # visited (from activities), # never-visited. Row click → company list pre-filtered to that estate. This is the field-route planning screen.
- **Field Day export:** any geographic result set → CSV/print view ordered for visiting (grouped by estate/pincode), with MD name, phone, products, last-touch date.

### 4.6 Call Queue & Outcome Capture
- **Call Queue screen:** default = ICP-fit ∧ above slab threshold ∧ not client ∧ not defunct ∧ not DND ∧ no touch in N days (configurable), sorted by `priority_rank`. Filters: vertical, district/radius, estate, phone_status, source, tags. Position indicator + keyboard advance (UX rules from UI/UX Spec §4 apply).
- **Log Call (telecaller):** one screen, ≤15 seconds to complete. Structured enums: `reached` (MD / GATEKEEPER / WRONG_NUMBER / NO_ANSWER / DEAD_NUMBER) · `uses` (multi: TALLY / EXCEL / ERP_NAMED / NONE) · `owner_in_operations` (Y/N/UNK) · `headcount_band` · `interest` (INTERESTED / CALLBACK / NOT_NOW / NEVER / DISQUALIFY→reason) · `next_touch_date` · free-text note (optional). DEAD_NUMBER auto-sets contact `phone_status=INACTIVE`.
- **Log Visit (field sales):** same pattern + `met_md` (Y/N) · `gift_material_left` · `demo_agreed` (Y/N/date). 
- Outcomes are activities on the timeline (types extended: `TELECALL`, `FIELD_VISIT` alongside v1.1's set) and update `last_touch_at` + queue eligibility. Structured fields are filterable and reportable.
- **Quotation Revival view:** all `QUOTATION_HISTORY` companies with quote date/value, phone_status, slab, last touch — worked like a queue with the same outcome capture.

### 4.7 Search, Timeline, Exports, Reports
As v1.1 (Sections 3.6, 7, Stage 5) with additions: search filters for slab, phone_status, estate, capability, radius; timeline renders call/visit outcomes; export columns include slab, phone_status(+date), estate, distance-from-center when exported from a radius search; export warns on phone checks older than the staleness threshold.

**Reports (Phase 1):** Pipeline · Source funnel (per source: imported → merged → ICP-fit → queued → called → interested) · Estate density · Call outcomes (reach rate, MD-reach rate, interest rate — per week, per caller, per source) · Quality (slab coverage %, phone-validated %, stale checks).

### 4.8 Admin (Phase 1)
NIC ontology editor · capability vocabulary · score weight sets (versioned) · queue threshold & eligibility rules · import/export templates · disqualification reasons (deactivate-only) · pincode reference upload · radius unit default · users mirror.

---

## 5. Phase 1 Data Model (deltas from v1.1 Section 10)
- `companies` + : nic_codes text[] · icp_verticals text[] (derived) · capabilities text[] · estate · pincode · lat/lng (derived) · turnover_slab + slab_checked_at · einvoice_enabled + checked_at · current_systems text[] · website_status · source_count int · is_existing_client bool · defunct bool · score_conversion · score_support_risk · score_implementation_risk (null) · score_expansion_potential (null) · score_strategic_fit (null) · priority_rank · last_touch_at
- `contacts` + : phone_status + phone_checked_at
- New: `nic_ontology` · `pincode_reference` · `score_weight_sets` (versioned JSON) · `capability_vocabulary`
- `activities`: type enum + TELECALL, FIELD_VISIT; structured outcome fields as JSONB `outcome` with enforced enum validation server-side
- Relationship tables **designed** (in migration comments / ADR): `company_relationships` (v1.1), `company_estate` implicit via field, `shared_reference` (Phase 3). Graph-shaped, Postgres-native.
- All v1.1 invariants stand: append-only qualifications/merges, aliases resolve, reasons on terminal actions, audit_log on every mutation, timeline composed at query time.

## 6. Phase 1 Non-Functional
Scale envelope raised: 200k companies (full-TN Udyam is large), 500k contacts, import batches to 50k rows (streamed). Radius query < 2s over 200k rows (indexed earthdistance). All other NFRs per v1.1 §11. **No LLM calls; external spend = zero in the application itself** (GST/HLR results enter via CSV in Phase 1).

## 7. Phase 1 Acceptance (Definition of Functional)
1. Real files load: Udyam Coimbatore + Chennai, RoC TN dump, CODISSIA parsed CSV, client list, quotation history — through templates, with batch reports, into canonical records; Suriya Equipments exists exactly once, flagged as client, carrying Udyam NIC codes + RoC data + estate "Rainbow Industrial Estate"
2. Telecaller logs in (role-restricted), opens Call Queue, top entries are multi-source, ICP-fit, slab ≥ threshold, ACTIVE phone; logs an outcome in ≤15s; company drops out of queue per eligibility rules; outcome visible on timeline and in the outcomes report
3. Field user: center = pincode 641021, radius 10 km → ranked list with distances in <2s; switches to Estate Density, opens top estate, exports a Field Day list
4. Slab CSV update runs; a company crossing the threshold appears in queue; one below it remains findable but not queued
5. Existing client and an open-quotation company never appear in the Call Queue (tested)
6. Admin edits an NIC ontology weight → re-derive → scores and queue order change accordingly; audit trail complete

## 8–13. Phase 2 / Phase 3 Detail, Open Items
**Phase 2 acceptance sketch:** GSP API wired with per-lookup cost cap + quarterly scheduled re-run producing slab-movement trigger flags; HLR batch endpoint + pre-export re-check gate; AI research per v1.1 Stage 3 spec (evidence rule, review queue, MockAiProvider in CI) with the non-HTML extraction approach ported from the diagnostic tool.
**Phase 3 acceptance sketch:** Supabase saved-analyses sync (inputs only — estimates never enter scores) as verified activities; relationship harvest depth-1 with ICP gate; similarity ranking against a named client set; CRM sync replacing CSV.

**Open Items before Phase 1 build:**
1. Confirm queue slab threshold default and radius unit default (km assumed)
2. Pincode reference source file selected and licensed-clean (open government pincode directory with lat/lng)
3. GSP provider shortlist (needed only for the CSV format Phase 1 ingests; API in Phase 2)
4. HLR/DND provider shortlist (same)
5. Historical quotation list: obtain as CSV (company, date, value, contact, status)
6. Client-profitability worksheet: fill to convert support-risk heuristics into evidence weights (can land mid-Phase 1 as a weight-set upload)
7. Keycloak realm addition: `PS_TELECALLER` role
8. Name/keep "ProspectSoul" — confirm before repo rename considerations

## 14. Amendments to Companion Documents
- Tech Spec §4: add endpoints — `/api/v1/call-queue` · `/api/v1/companies/{id}/log-call` · `/api/v1/companies/{id}/log-visit` · `/api/v1/geo/radius` · `/api/v1/estates` + density · `/api/v1/admin/nic-ontology` · `/api/v1/admin/score-weights` · bulk-update endpoints for slab/phone CSVs. Naming/pagination/error conventions unchanged.
- Tech Spec §5: normalization table gains NIC-array, pincode-extraction, estate-extraction rows (reference implementation: `vyoog_crossref.py`)
- Tech Spec §7 (AI abstraction): **moves to Phase 2** — not scaffolded in Phase 1
- UI/UX Spec: new screens — Call Queue, Log Call/Visit modal, Estate Density, Radius search controls on Companies; Telecaller role in nav map
- Implementation Roadmap: re-cut sprints against Phase 1 scope (Sprint 1: skeleton + import + search as-is; Sprint 2: dedup/triage + NIC/estate normalization; Sprint 3: geo + scoring + queue; Sprint 4: outcomes + reports + admin + hardening). Working rules and Definition of Done unchanged.
