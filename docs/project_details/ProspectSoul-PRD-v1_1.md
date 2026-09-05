# ProspectSoul — Product Requirements Document
**Version:** 1.1
**Date:** July 2026
**Owner:** Senthil, COO — Vyoog Information Private Limited
**Status:** Design Complete, Ready for Build

---

## Changelog — v1.0 → v1.1

Incorporates external design review (ChatGPT + Claude, reviewed jointly). Five additions, two architecture notes, three explicit deferrals. The six-stage pipeline, roles, ICP engine, and MVP discipline are unchanged.

**Added to v1:**
1. **Evidence model** — every AI-generated claim (manufacturer/trader, certifications, size signals, criterion suggestions) must carry evidence: source URL, excerpt, and capture date. AI conclusions without evidence are not accepted into the record. See Section 3.5.
2. **Company Timeline** — the company detail page is now defined as a chronological timeline of everything that ever happened to the record. No new data; a first-class view over existing tables. See Section 3.6.
3. **Activities** — `research_records` generalized into a unified `activities` table (AI research, manual note, call, visit, verification). Scope guard: research/verification activities only — sales outreach logging belongs in the CRM. See Section 3.6.
4. **Source Analytics** — per-source funnel report (imported → survived dedup → qualified → exported) promoted to a defined report. Conversion-to-customer remains deferred pending CRM sync-back; the export log provides the future join key. See Section 8.
5. **AI Research Queue** — batch research formalized: analyst selects a set → queued asynchronous AI research (configurable batch cap) → review queue of unverified drafts. Cost controls unchanged: explicitly triggered, never automatic on import. See Stage 3.
6. **Research attachments** — files (PDF brochures, catalogues, images) can be attached to activities. Storage open item added (Section 13).

**Architecture notes (designed, not built):**
- **Company relationships** (parent / subsidiary / division / plant — common in Indian manufacturing groups): `company_relationships` table designed, not built. v1 rule remains one company = one entity.
- **Contact employment history:** the existing `association_start/end` fields are the seed of a future person-entity model. No further build in v1.

**Explicitly deferred (moved to Section 12 with reasons):**
- Document intelligence (AI extraction from uploaded catalogues/brochures) — plugs in later as a named AI capability now that attachments exist
- Natural-language search — structured filters already answer the target queries
- Full contact lifecycle / person-entity modeling

---

## 1. Overview

### 1.1 Problem Statement
Vyoog's prospect data lives in scattered Excel sheets, IndiaMART exports, LinkedIn notes, and individual salespeople's heads. The same company appears in multiple lists with different spellings, different phone numbers, and no shared memory of who researched it, what was found, or why it was dropped. Every sales effort starts from zero.

The consequences, confirmed by the GTM Problems analysis:
- Sales targets wrong or mixed segments because the ICP is not applied consistently
- Time is wasted re-researching companies that were already researched and disqualified
- Lead quality entering the CRM is unknown — unverified phones, dead websites, traders mixed with manufacturers
- Qualification depends on individual judgment, so it cannot scale or be audited
- Nobody can say which lead source is worth the money

### 1.2 What ProspectSoul Does
ProspectSoul is an AI-assisted internal platform that becomes the **single source of truth for all prospect companies and contacts before they enter the CRM**. It:

- Imports prospect data from any source through one standard import framework (Excel/CSV first)
- Normalizes and validates every record on entry (phone format, website domain, location)
- Detects duplicates and guides a human through merge decisions
- Enriches companies with AI research — website understanding, summaries — where **every AI claim carries evidence** and a human verifies
- Applies ICP qualification — configurable, versioned criteria producing Tier A / B / C per the sales playbook — with AI suggesting (with evidence) and a human confirming
- Maintains a permanent, chronological **timeline** of every company: every import, merge, activity, qualification, and export
- Measures **which sources produce good leads**, not just how many
- Exports qualified, verified leads to the CRM or Excel

### 1.3 What ProspectSoul Is NOT
- Not a CRM, sales pipeline, or opportunity management tool
- Not calling software, marketing automation, or an email campaign tool
- Not a sales-activity logger — activities recorded here are research and verification, not outreach execution
- Not a web scraping engine (v1 — see Section 1.6, Decision 1)
- Not a commercial SaaS product — no multi-tenancy, no customer-facing surface
- Not responsible for sales execution — it ends where the CRM begins

### 1.4 Success Criteria
- Every prospect company exists exactly once, with one canonical record and a visible timeline
- No lead enters the CRM without passing ICP qualification with a recorded tier and reason
- A salesperson can answer "have we ever looked at this company before, and what happened?" in under 10 seconds — from the timeline, not from asking around
- Every AI conclusion on a record is backed by clickable evidence an analyst can check in seconds
- Re-research of previously disqualified companies drops to zero
- Import of a 500-row Excel list — including dedup, batch AI research, and validation — takes minutes of human review time, not days
- Management can rank lead sources by qualification yield within the first quarter of use
- Every qualification decision is explainable: which ICP version, which criteria, what evidence, who confirmed, why overridden (if overridden)

### 1.5 Strategic Direction (Not v1 Scope)
ProspectSoul is the prospect-intelligence sibling of CustSoul in Vyoog's Operational Intelligence Platform direction. If CustSoul is a Requirement Intelligence Platform, ProspectSoul is a **Company Intelligence Platform** — the organizational memory of every company Vyoog has ever looked at. That identity guides design; it does not expand v1 scope, and the non-goals in 1.3 stand.

Like CustSoul, it is designed from v1 to accept data from multiple **Import Sources** through one contract. Manual file imports come first; an automated collection engine (the SQLE concept), CRM sync-back, and programmatic API sources plug into the same import framework later without architectural change.

If the tool proves itself internally, it may later become an ERP module. v1 does not build toward that explicitly.

### 1.6 Key Design Decisions — Deviations from Input Documents

These decisions were made deliberately during design. Recorded here so they are challenged with context, not rediscovered.

| # | Decision | Rationale |
|---|---|---|
| 1 | **Scraping engine (SQLE core) deferred entirely** | Scraping LinkedIn/IndiaMART is the highest-risk, highest-maintenance component (CAPTCHAs, anti-bot, legal exposure, brittle parsers) and is not required for Day-1 value. The team already has data in files — the bottleneck is organizing and qualifying it, not collecting more of it. The import framework is designed so a scraper becomes just another registered source later. |
| 2 | **SQLE's fixed 100-point scoring replaced by configurable, versioned ICP profiles producing Tier A/B/C** | The ICP playbook's actual insight is that the ICP is "who can buy fast with low friction" — MD access, owner-driven, simple org — not a generic point total. Hardcoding weights repeats the GTM problem of unexplainable value. Criteria and thresholds are configuration; every qualification is stamped with the ICP version that produced it. |
| 3 | **Duplicate detection is deterministic in v1** (normalized phone, website domain, normalized company name) | Same reasoning as CustSoul deferring pgvector dedup: semantic matching needs data volume to tune and is not needed to catch the vast majority of real-world duplicates in Indian SME lists, which are format variations. pgvector is in the stack, unused, ready. |
| 4 | **One ICP profile configured at launch; multiple ICPs supported by data model only** | Vyoog has one active playbook. Building multi-ICP UI now is speculative. The `icp_profile_id` on every qualification means adding a second profile later is configuration, not rework. |
| 5 | **Record-level verification in v1, not field-level** | Field-level verification (each phone/email individually verified) doubles UI and model complexity. v1: a record is Verified when an analyst confirms the core fields are correct, with timestamp and user. Field-level can be added later if the record-level signal proves too coarse. |
| 6 | **CSV export first; CRM API integration deferred** | Which CRM, and its API maturity, is an open item (Section 13). Export must not block launch. The exports table records what left and when, so API sync later replaces the file, not the model. |
| 7 | **AI claims require evidence — no evidence, no claim** *(v1.1)* | An AI summary that says "ISO certified" with no citation forces the analyst to redo the research to verify it — which defeats the purpose. Requiring URL + excerpt per claim makes verification a 10-second check instead of a re-read. Same principle as CustSoul's classification rationale, made structural. |
| 8 | **One company = one entity in v1; group structures designed but not built** *(v1.1)* | Parent/subsidiary/plant structures are real in Indian manufacturing, but modeling them now adds complexity before the data exists to justify it. `company_relationships` is designed so it can be added without touching the company master. |

---

## 2. Users and Roles

Roles are few, because the internal team is small. Permissions are role-based via Keycloak from Day 1 so adding roles later is configuration.

| Role | Who They Are | What They Do in ProspectSoul |
|---|---|---|
| **Research Analyst** | Inside-sales / research team member | Imports files, resolves duplicates, runs AI research batches, verifies evidence, logs activities, confirms qualifications. The primary daily user. |
| **Sales Lead** | Head of sales / senior salesperson | Reviews qualified pool, approves exports, overrides tiers with reason, requests re-research |
| **Admin** | Ops/tech owner of the tool | Configures ICP profiles, import templates, disqualification reasons, activity types, user roles |
| **Viewer** | Founders, marketing, anyone else | Read-only search, timelines, and reports — no edits |
| **COO** | Senthil | Weekly operational dashboard — pipeline throughput, source analytics, data quality; no record-level work |

**Governance note:** In v1 the Research Analyst both resolves duplicates and confirms qualifications. Acceptable at current team size; separate the duties if the team grows.

---

## 3. Core Concepts

### 3.1 Company Master
The company is the anchor entity. One canonical record per real-world company, ever.

- **Identity:** legal/trading name plus normalized identifiers — website domain, primary phone (normalized), city. These drive dedup.
- **Merging, not deleting:** when duplicates are confirmed, records are merged into one survivor. The merged record's data is preserved in the merge log; its ID becomes an alias that redirects to the survivor, so old links and import references never break.
- **History is permanent:** every state change, qualification, activity, and merge is in the audit trail and visible on the timeline. A company disqualified in 2026 and re-imported in 2027 surfaces its full prior history immediately — this is the "never re-research" guarantee.
- **Operational state:** every company has exactly one pipeline state at all times (Section 4). There is no such thing as a company "floating" outside the workflow.
- **Group structures (designed, not built):** `company_relationships` (parent / subsidiary / division / plant, e.g. LGB → LGB Foundry → LGB Pumps) is specified in the data model as a future table. v1 treats each entity as an independent company; an analyst can note group membership with tags in the meantime.

### 3.2 Contact Master
Contacts belong to companies but are entities in their own right.

- A contact has: name, designation, phone(s), email, seniority flag (**MD/Owner — yes/no**, because MD access is the single most important ICP signal per the playbook), verification status, and source.
- A company can have multiple contacts; one is marked **primary**.
- **Movement between companies (v1-simple):** a contact is re-linked to a new company by an analyst; the old association is closed with an end date, not deleted. These closed associations are the seed of a future employment-history model. Automated movement detection is out of scope.
- Contact confidence in v1 is the combination of verification status + source + last-verified date — no separate numeric confidence score (deferred; see Section 12).

### 3.3 ICP Profiles
An ICP profile is a named, versioned set of qualification criteria producing a **Tier (A / B / C) or Disqualified**, mirroring the sales playbook:

- **Core criteria** (from the playbook): MD/owner access, owner-driven operations, org size band, management layers
- **Operational criteria:** visible inefficiency signals, manual processes, ERP maturity
- **Firmographic filters:** industry = manufacturing, cluster (auto components / pumps / foundries), location, exclude traders
- Criteria, weights/thresholds, and tier boundaries are **Admin configuration, not code**
- Editing a profile creates a **new version**; existing qualifications keep their original version stamp

### 3.4 Import Batches
Every record enters through a batch — even a single manually-entered company is a batch of one. A batch records: source type, file/reference, who imported, when, row-level results (created / merged / rejected with reason). This gives complete lineage: any company field can be traced to the batch and row that introduced it — and it is the foundation of Source Analytics (Section 8).

### 3.5 Evidence *(new in v1.1)*
**Every AI-generated claim must carry evidence, or it is not accepted into the record.**

An evidence item is: the claim, the source (URL or attached document), a verbatim excerpt, and the capture date. Examples:

| AI Claim | Evidence |
|---|---|
| "Manufacturer, not trader" | Products page URL + excerpt describing in-house machining |
| "ISO 9001 certified" | Certifications page URL + excerpt |
| "Exports to Europe" | About Us URL + the paragraph stating it |
| "Owner-driven — MD named on site" | Team/About page URL + excerpt naming the MD |

- Evidence renders next to each claim in the review UI — verification is a click-and-check, not a re-research
- Qualification criterion suggestions (Stage 4) link to the evidence items that support them
- Evidence is captured at a point in time; websites change. The capture date is displayed, and staleness rules (Section 6) apply
- Manual research follows the same shape where practical: a call note *is* the evidence for "confirmed no ERP in use"

This is the structural version of CustSoul's "classification includes a rationale — not a black box."

### 3.6 Activities & the Company Timeline *(new in v1.1)*
All human and AI touches on a company are **activities** of a defined type: `AI_RESEARCH`, `MANUAL_NOTE`, `CALL`, `VISIT`, `VERIFICATION`. Activities can carry file attachments (brochures, catalogues, photos, PDFs).

**Scope guard:** activities are research and verification actions. Sales outreach (demo calls, follow-ups, negotiations) is CRM territory and is deliberately not modeled here. If analysts start logging outreach in ProspectSoul, that is the signal the lead should already have been exported.

**The Company Timeline is the default company detail view** — a single chronological stream assembled from existing tables (no new event store): import events, merges, activities with attachments, verification changes, qualifications with tier and ICP version, state changes, exports. The 2026-disqualified / 2027-re-imported story reads top to bottom on one screen.

---

## 4. System Architecture — Six Stages

Every company moves through a defined pipeline. AI drafts with evidence, humans decide, at every gate.

**Pipeline states:** `IMPORTED → TRIAGE → RESEARCH → QUALIFICATION → READY → EXPORTED`, plus terminal states `DISQUALIFIED` and `ARCHIVED` (reachable from any stage, always with a reason).

### Stage 0 — Import (Any Source, One Contract)

**Goal:** Get data in from anywhere with zero data loss and full lineage.

**How it works:**
- Analyst uploads a file (Excel/CSV) and selects a **source template** — a configurable column mapping per source type (IndiaMART export, TradeIndia export, LinkedIn list, generic list, manual entry form)
- Every row is captured as an import row before any processing — nothing is silently dropped
- Row-level validation errors (unparseable phone, missing company name) are reported per row; the batch report shows created / duplicate-flagged / rejected counts
- Manual single-company entry uses the same path — a form that creates a batch of one

**Import Sources (enum, required on every batch):**

| Value | Meaning |
|---|---|
| `EXCEL_CSV` | Generic file upload |
| `INDIAMART` | IndiaMART export file (template mapping) |
| `TRADEINDIA` | TradeIndia export file (template mapping) |
| `LINKEDIN` | Manually assembled LinkedIn list |
| `MANUAL_ENTRY` | Single-record entry by an analyst |
| `API` | Reserved — programmatic sources (scraper, other systems). Not active in v1 |

**v1 scope:** file-based and manual only. The `API` source and a `POST /api/companies/from-source` endpoint (CustSoul Section 4.1 pattern — service JWT via Keycloak client-credentials, idempotency on source + reference) are designed but **not built** until the first programmatic source exists.

---

### Stage 1 — Normalize & Validate (Auto)

**Goal:** Make every record comparable before any human sees it.

**How it works, per row:**
- **Phone:** strip +91/spaces/punctuation → 10 digits; classify mobile (starts 9/8/7/6) vs landline; invalid numbers kept but flagged, not discarded
- **Website:** normalize to registered domain (`www.abc-pumps.com/products` → `abc-pumps.com`)
- **Company name:** normalized form for matching (case-fold, strip punctuation, strip legal suffixes — Pvt Ltd, LLP, Industries treated as configurable stop-suffixes)
- **Location:** city/state normalized against a reference list
- **Completeness score** computed (Section 6)
- Deterministic duplicate check against the Company Master: exact match on normalized phone, or domain, or normalized name + city → row flagged as a **duplicate candidate**, never auto-merged

Rows with no duplicate candidate and no blocking errors create a company in `TRIAGE`. Rows with candidates go to Stage 2 attached to their candidate match.

---

### Stage 2 — Triage & Dedup (Human + AI)

**Goal:** A human confirms identity decisions. No record is merged or rejected by a machine.

**How it works:**
- Analyst works a triage queue: duplicate candidates side-by-side (existing record vs incoming row), with the matching rule shown ("same normalized phone")
- Decisions: **Merge** (field-level pick of survivor values, incoming extras appended — e.g. a new contact), **Not a duplicate** (creates a new company; the pair is remembered so it is not re-flagged), or **Reject row** (junk data, with reason)
- Clean records need only a quick sanity glance — triage of a non-flagged record is one click
- On merge into a previously `DISQUALIFIED` company, the prior timeline is shown immediately — the analyst sees why it was dropped before wasting any effort

Companies leave this stage in `RESEARCH` state (or straight to `QUALIFICATION` if the analyst judges the record already complete enough).

---

### Stage 3 — Enrich & Research (AI + Human)

**Goal:** AI does the reading and drafting — with evidence for every claim; the analyst does the judging.

**How it works:**

*Batch research via the Research Queue (v1.1):*
- Analyst selects companies (typically a filtered batch — "everything from this import with a website") and sends them to the **Research Queue**
- Queued companies are processed asynchronously in AI batches; batch size and daily caps are Admin configuration (cost control)
- Each completed research lands in the **Review Queue** as an unverified draft

*What AI research produces, per company:*
- A structured draft: what they make, likely cluster, indicative size signals, certifications mentioned, manufacturer-vs-trader signal, and a plain-language summary
- **Every claim carries evidence** (Section 3.5): source URL + excerpt + capture date. Claims the AI cannot evidence are reported as "not found," not asserted
- Clearly marked unverified until an analyst acts — same principle as CustSoul Level 1 classification

*Analyst review:*
- Works the Review Queue: checks claims against their evidence (click-and-verify), corrects errors, adds field research as activities (call notes, visit notes — with attachments such as brochures or catalogue PDFs), and marks the record **Verified** (record-level, with timestamp and user)
- All of it lands on the company timeline, permanently

**v1 scope:** website understanding + summary + trader/manufacturer signal, with evidence. Attachments are stored and viewable; **AI extraction from attached documents (catalogue intelligence) is deferred** — it plugs in later as a new named AI capability against files that will already be in the system. Social-media enrichment and registry lookups are deferred.

---

### Stage 4 — ICP Qualification (Rules + AI Suggest → Human Confirm)

**Goal:** Every company gets an explainable, evidence-backed tier decision against a versioned ICP.

**How it works:**
- The active ICP profile evaluates the record: hard filters first (manufacturing? trader-excluded? location in scope?), then criteria assessment
- Where a criterion needs judgment (owner-driven? MD accessible?), AI suggests an answer from the research record — **citing the evidence items that support it** — and the analyst confirms or corrects, exactly like CustSoul Stage 2
- Output: **Tier A / Tier B / Tier C / Disqualified**, with the per-criterion breakdown and linked evidence stored
- Analyst (or Sales Lead) can **override** the computed tier — override requires a mandatory reason and is visibly marked
- Disqualification always records a reason from a configurable list (trader, out of cluster, too large/corporate, no MD path, dead company…) plus free text
- The qualification record stores the **ICP profile + version** that produced it. Re-qualification (after ICP change or new research) creates a new record; history is never overwritten and both appear on the timeline

Qualified companies move to `READY`. Disqualified companies move to `DISQUALIFIED` — still fully searchable, forever.

---

### Stage 5 — Export & Handoff

**Goal:** Only clean, qualified, verified leads leave the building.

**How it works:**
- Sales Lead filters the `READY` pool (tier, cluster, city, completeness) and creates an **export**
- v1 export = CSV in a configurable column layout matching the CRM's import format
- The export is recorded: which companies, which contacts, who exported, when, destination label. Companies move to `EXPORTED`
- ProspectSoul's responsibility ends here. What happens in the CRM is out of scope — but the export log means a future CRM sync-back ("this lead converted / bounced") has a key to join on, which is what will eventually complete the Source Analytics funnel down to customer conversion

---

## 5. User Flows

### Research Analyst — daily core loop
Upload file → review batch report → work triage queue (merge / new / reject) → send new companies to Research Queue → work Review Queue (verify evidence-backed drafts, log activities, attach documents) → confirm ICP qualification → done

### Research Analyst — single company
Manual entry form → auto normalize/dedup check → research → qualify

### Research Analyst — returning company
Search → open timeline → see full prior history (research, qualification, disqualification reason) in seconds → decide whether anything has changed before spending effort

### Sales Lead
Open READY pool → filter Tier A, target cluster → spot-check records and evidence → export to CSV → hand to sales / import to CRM. Override a tier where field knowledge says otherwise — with reason.

### Admin
Configure ICP criteria and tier thresholds (new version) → configure import templates → manage disqualification reason list → set Research Queue batch caps → manage users/roles in Keycloak

### COO
Weekly dashboard — imported / triaged / qualified / exported counts, source funnel comparison, quality metrics, aging. No record-level work.

---

## 6. Data Quality

Three measured dimensions, all visible on every record and aggregated on the dashboard:

| Dimension | v1 Definition |
|---|---|
| **Completeness** | Weighted percentage of core fields present (name, mobile phone, website, city, cluster/industry, ≥1 contact, MD identified). Weights are Admin configuration. |
| **Verification** | Record-level: `UNVERIFIED` / `VERIFIED (by, at)`. Any edit to a core field after verification drops the record back to `UNVERIFIED`. |
| **Staleness** | Days since last verification (or last meaningful update if never verified). Records older than a configurable threshold (default 180 days) are flagged **Stale** in search, qualification, and export screens. Evidence items display their capture date — a 2-year-old website excerpt is visibly old. Export of stale records triggers a warning, not a block. |

---

## 7. Search

Fast operational search is a first-class requirement — the 10-second "have we seen this company?" answer.

- One search box: company name (normalized + fuzzy trigram matching), phone (any format — normalized before lookup), email, website/domain, contact name
- Filters: pipeline state, tier, cluster, city/state, verification status, staleness, tags, import source
- Activity and note text is searchable — "no ERP" written in a call note two years ago is findable
- Tags are free-form, Admin-curatable labels on companies
- Search covers **all** states including `DISQUALIFIED` and `ARCHIVED` — history is the point
- Implementation: PostgreSQL full-text + `pg_trgm` indexes. No external search engine in v1. Natural-language search ("foundries in Coimbatore with no ERP, Tier A") is deferred — the structured filters answer these queries today; NL becomes a convenience layer later

---

## 8. Reports

Operational reports, not CRM analytics:

- **Source Analytics** *(promoted in v1.1)* — per-source, per-batch funnel: imported → survived dedup → qualified (by tier) → exported. Qualification yield per source answers "which list is worth buying again." Example: Source X yields 22% qualified, Source Y yields 80% — management now knows where to invest. The final funnel step (became customers) requires CRM sync-back and is deferred; the export log holds the join key.
- **Pipeline throughput** — companies per state, movement per week, aging per stage (what's stuck in triage? how deep is the Review Queue?)
- **Import health** — per batch: created / merged / rejected, error breakdown, per source over time
- **Data quality** — completeness distribution, verified %, stale count, by cluster/city
- **Qualification outcomes** — tier distribution, disqualification reasons ranked (market feedback: if 40% of a source is "trader", stop buying that list), override frequency
- **Export log** — what left, when, to where, by whom

All reports are on-screen tables/charts with CSV download. No scheduled email reports in v1.

---

## 9. Technical Stack

| Layer | Technology | Reason |
|---|---|---|
| Frontend | React | Same as core Vyoog product and CustSoul |
| Backend | Spring Boot | Same as core Vyoog product and CustSoul |
| Database | PostgreSQL (+ `pg_trgm` for fuzzy search; pgvector present, unused in v1) | Consistent infrastructure; semantic dedup ready for a later phase without migration |
| File storage | AWS S3 (Mumbai) | Activity attachments (brochures, catalogues, images). Configuration open item — Section 13 |
| AI Layer | Provider-agnostic AI abstraction — Claude API as v1 provider | Website research with evidence extraction, summaries, qualification suggestions, dedup explanations. Same ADR-008 principle as CustSoul: provider and model per capability are configuration, not code |
| Authentication | Keycloak (OIDC/OAuth2), SSO | Standard Identity Provider for all new Vyoog Spring Boot applications. Role claims drive permissions. Service-to-service client-credentials reserved for future API sources |
| Hosting | AWS India (Mumbai) | Consistent with existing infrastructure |

**AI architecture principles (shared with CustSoul):**
- Every AI capability (research, summarization, qualification suggestion; later: document extraction) is a named capability routed through the abstraction layer
- Every AI output is stored with its prompt version, provider/model, rationale, **and evidence** — explainable and auditable
- AI never changes state on its own: no auto-merge, no auto-qualify, no auto-disqualify
- AI claims without evidence are stored as "not found," never asserted as fact

---

## 10. Data Model (High Level)

**companies** — id, canonical_name, normalized_name, website_domain, primary_phone_normalized, city, state, cluster, industry, size_band, tags[], pipeline_state, completeness_score, verification_status, verified_by, verified_at, created_at

**company_aliases** — id, alias_of_company_id, source_company_id — preserved identities of merged records; lookups on a merged ID redirect to the survivor

**company_relationships** *(designed, not built in v1)* — id, parent_company_id, child_company_id, relationship_type (subsidiary/division/plant), valid_from, valid_to

**contacts** — id, company_id, name, designation, is_md_owner (bool), phones[], emails[], is_primary, verification_status, source, association_start, association_end (nullable), created_at

**import_batches** — id, source (enum), template_id, file_reference, imported_by, row_counts (created/merged/rejected), status, created_at

**import_rows** — id, batch_id, row_number, raw_data (JSON), outcome (created/merged/rejected/pending), outcome_reason, resulting_company_id, created_at

**duplicate_candidates** — id, incoming_row_id or company_id, matched_company_id, match_rule, resolution (merged/not_duplicate/pending), resolved_by, resolved_at — "not_duplicate" pairs suppress re-flagging

**merges** — id, survivor_company_id, merged_company_id, field_decisions (JSON), merged_by, merged_at

**activities** *(generalized from research_records in v1.1)* — id, company_id, type (ai_research/manual_note/call/visit/verification), content (JSON/text), ai_provider (nullable), ai_model (nullable), prompt_version (nullable), verified (bool), created_by, created_at

**attachments** — id, activity_id, file_name, file_type, storage_ref (S3 key), uploaded_by, uploaded_at

**evidence** *(new in v1.1)* — id, activity_id, claim, source_type (website/document/call), source_url (nullable), attachment_id (nullable), excerpt, captured_at — referenced by qualification criterion results

**research_queue** — id, company_id, requested_by, status (queued/processing/complete/failed), batch_id, created_at, completed_at

**icp_profiles** — id, name, active (bool) · **icp_profile_versions** — id, profile_id, version_no, criteria (JSON), tier_thresholds (JSON), created_by, created_at

**qualifications** — id, company_id, icp_profile_version_id, computed_tier, final_tier, criterion_results (JSON — including evidence_ids per criterion), overridden (bool), override_reason, disqualification_reason_id (nullable), decided_by, decided_at — append-only; latest record is current

**disqualification_reasons** — id, label, active — Admin-configurable list

**exports** — id, filter_snapshot (JSON), company_ids[], destination_label, exported_by, exported_at

**users** — managed in Keycloak; local table mirrors id, name, role for display/audit

**audit_log** — id, entity_type, entity_id, actor_id, action, previous_state, new_state, timestamp — every state change recorded

**Timeline note:** the Company Timeline is a query-time composition over import_rows, merges, activities, qualifications, state changes (audit_log), and exports — there is no separate timeline/event table to keep in sync.

---

## 11. Non-Functional Requirements

- **Scale (v1 planning envelope):** 100k companies, 300k contacts, batches up to 10k rows — comfortable for a single PostgreSQL instance; no premature optimization
- **Import processing:** a 5k-row batch fully normalized and dedup-checked within minutes, asynchronously, with progress visible
- **Research Queue:** asynchronous with visible progress and per-company failure isolation (one dead website does not fail the batch); Admin-configurable batch size and daily AI-call caps
- **Search:** sub-second for the standard lookups in Section 7
- **Timeline:** company detail view including full timeline loads in under 2 seconds for a company with years of history
- **Auditability:** no state change without an audit record; qualifications and merges are append-only
- **Security:** internal tool behind Keycloak SSO; role-based permissions; no public surface; PII (phones, emails) and attachments stay in AWS Mumbai
- **AI cost control:** AI research is analyst-triggered (single, selected set, or queued batch), never automatic on import — cost scales with intent, not volume

---

## 12. Out of Scope — Version 1

Explicitly excluded to prevent scope creep. Architecture supports each without rework.

**Deferred to a later iteration:**
- Automated scraping/collection engine (SQLE) — plugs in later as an `API` import source
- `POST /api/companies/from-source` programmatic endpoint — designed (CustSoul 4.1 pattern), built when the first real programmatic source exists
- **Document intelligence** — AI extraction of products, machinery, certifications, and markets from attached catalogues/brochures. Attachments exist in v1, so this plugs in later as a new named AI capability against files already in the system
- **Company relationships** (parent/subsidiary/division/plant) — table designed (Section 10), built when group-structured prospects actually appear in volume; tags bridge the gap
- **Contact lifecycle / person-entity model** — closed contact associations (`association_end`) are the seed; full employment-history modeling waits for real need
- **Natural-language search** — structured filters answer the target queries today
- Semantic/AI duplicate detection (pgvector) — after real data volume exists to tune it
- CRM API integration and conversion sync-back — v1 is CSV export with a complete export log to join on later; this is also what completes the Source Analytics funnel to customer conversion
- Field-level verification and numeric contact confidence scores
- Multiple active ICP profiles and ICP A/B comparison — data model supports it; UI configured for one
- Third-party enrichment integrations (email verification services, GST/MCA registry lookups, social media)
- Automated staleness re-research campaigns
- Scheduled/emailed reports
- Call recordings as attachments — storage/consent implications need a decision first

**Not in roadmap:**
- Any CRM/pipeline/opportunity functionality
- Sales outreach activity logging (demos, follow-ups, negotiations) — CRM territory, per the Section 3.6 scope guard
- Calling, WhatsApp, or email outreach from within the tool
- Multi-tenancy or customer-facing access
- Lead marketplace / data resale features

---

## 13. Open Items Before Build Starts

1. **Which CRM is the export target**, and what is its lead-import column format? Needed to configure the v1 export template
2. **Initial ICP profile sign-off** — translate the playbook's Tier A/B/C criteria into concrete configured criteria and thresholds with the Sales Lead; this is configuration work but needs a decision owner
3. **Source file samples** — one real export file each from IndiaMART, TradeIndia, and the current Excel lists, to build the v1 import templates against reality
4. **Who is the Admin** (owns ICP config, templates, reason lists, queue caps) and who are the Research Analysts on Day 1?
5. **Keycloak realm/client configuration** — confirm `prospectsoul` client, audience, and role claims with the team operating Keycloak (same residual as CustSoul Open Item 5)
6. **Disqualification reason starter list** — agree the initial set with sales so reporting is meaningful from the first week
7. **Attachment storage** — confirm the S3 bucket (Mumbai), size limits per file, and allowed file types for activity attachments
8. **Research Queue defaults** — agree initial batch size and daily AI-call cap with whoever owns the AI spend
9. **Name confirmation** — "ProspectSoul" follows the Soul convention; confirm or rename before repositories are created
