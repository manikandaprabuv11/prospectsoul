# ProspectSoul — Implementation Roadmap
**Version:** 1.0 · **Date:** July 2026 · **Companion to:** PRD v1.1, Domain Model v1.0, Tech Spec v1.0, UI/UX Spec v1.0
**Audience:** Claude Code (and whoever reviews its output).

---

## 0. Working Rules for Claude Code

1. **Never start Sprint N+1 until Sprint N's acceptance criteria all pass.** No exceptions, no "I'll just scaffold it."
2. Build **vertical slices**: each sprint delivers working backend + frontend + tests for its scope, demoable end-to-end.
3. Follow the API contract in the Tech Spec **exactly as named**. If a needed endpoint is missing, propose it — don't invent silently.
4. Enforce the Domain Model invariants **in code** (state machine, append-only qualifications, no-evidence-no-claim, reasons on terminal actions). A UI check alone is not enforcement.
5. Migrations are append-only from the moment they're applied.
6. Ask when ambiguous; assume nothing about scope not written in these four documents + PRD v1.1.
7. **Not-blocked-by-open-items policy:** build against the shipped defaults (default ICP, default export template, generic CSV mapping). Real values arrive later as configuration, not code changes.

**Definition of Done (every sprint):**
- All acceptance criteria demonstrably pass against the seed data
- Unit tests for business logic; integration test (Testcontainers) for the sprint's main flow
- Audit log rows written for every mutation introduced
- Role restrictions enforced and tested (403 paths)
- OpenAPI reflects reality; README updated (how to run, what works now)
- No TODOs referencing this sprint's scope

---

## Sprint 1 — Skeleton: Auth, Company, Import, Search, Dashboard

**Goal:** A logged-in analyst imports a CSV and finds companies by search. The tool is *already useful* — a shared, searchable company list beats scattered Excels on day one (PRD: "MVP must already provide value").

**Scope**
- Repo scaffold per Tech Spec §1; Docker Compose (Postgres, Keycloak with realm import, MinIO); Flyway `V1__baseline` (companies, contacts, import_batches, import_rows, import_templates, audit_log, users mirror) + `V2__seed`
- Keycloak integration end-to-end (SPA login, JWT validation, role guards, 5 seed users)
- Companies: GET list w/ q + filters, GET by id, POST manual entry (batch-of-one), PATCH; Contacts CRUD
- Imports: upload → async processing (rows persisted raw; **normalization stubbed to pass-through**; every row → created), Batch Report screen with polling
- Import Wizard (generic CSV mapping, step 2 functional), Companies search screen, minimal Dashboard, app shell + nav
- Search: Postgres full-text + pg_trgm on name; phone/domain/email exact

**Acceptance criteria**
1. Each seed role logs in; Viewer gets 403 on POST /companies (tested)
2. Uploading `sample_generic.csv` (25 rows) creates a COMPLETE batch with 25 created rows; all 25 companies searchable
3. "abc pumps", "ABC PUMPS", and "9843012345" all find ABC Pumps in <1s
4. Manual entry creates a company with a visible batch-of-one lineage
5. Rejecting a malformed row (missing name) shows in Batch Report → Rejected with raw data visible
6. Every create/update has an audit_log row with the correct actor

---

## Sprint 2 — Data Integrity: Normalization, Dedup, Triage, Activities, Timeline

**Goal:** The "one company, once, forever" promise becomes real.

**Scope**
- Normalization per Tech Spec §5 (phone, domain, name stop-suffixes, city reference) — exhaustively unit-tested
- Deterministic dedup on import (3 rules, ordered) + suppressed-pairs; duplicate_candidates, merges, company_aliases tables
- Triage Queue screen (side-by-side, field-level merge radios, keyboard shortcuts, DISQUALIFIED-history banner)
- Pipeline state machine (all states + allowed transitions enforced server-side); state endpoint with ReasonModal
- Activities (MANUAL_NOTE, CALL, VISIT, VERIFICATION) + record-level verify/unverify-on-core-edit
- **Company Timeline** (composed endpoint + default tab): import, merge, activity, state-change, verification events
- Activity text included in search

**Acceptance criteria**
1. Normalization table-driven tests pass: `+91 98430-12345`→`9843012345` (mobile), `044-2345678` flagged landline, `www.abc-pumps.com/products`→`abc-pumps.com`, `ABC Pumps Pvt. Ltd.`≡`abc pumps`
2. Importing `sample_indiamart.csv` (30 rows, 8 engineered duplicates) flags exactly 8 candidates; zero auto-merges
3. Merging preserves the survivor, appends the incoming contact, and the old ID (alias) still resolves via GET with `resolved_from`
4. NOT_DUPLICATE pair is never re-flagged on re-import of the same row
5. Illegal state transition (EXPORTED→TRIAGE) returns 409; DISQUALIFY without reason returns 422
6. Timeline for a merged company shows import, merge, note, and state change in order, <2s
7. Searching "no ERP" finds the company whose call note contains it, with snippet

---

## Sprint 3 — Intelligence: Research Queue, AI + Evidence, Attachments, Review

**Goal:** AI reads websites; every claim carries evidence; humans verify fast.

**Scope**
- AI abstraction (capability interface, provider registry, Anthropic provider, **MockAiProvider** for dev/CI, call logging)
- WebsiteFetchService (Jsoup, page allowlist, timeouts, robots.txt)
- CompanyResearchCapability → AI_RESEARCH activity + evidence rows; no-evidence claims stored `not_found` (enforced in code + tested)
- research_queue table + scheduled worker (SKIP LOCKED claim, per-company failure isolation, daily cap counter, 422 over cap)
- Bulk "Send to Research" from Companies screen; Research Queue screen (statuses, cap bar, retry)
- Review Queue screen (claim-by-claim accept/edit/reject with reason, verify gate)
- Attachments: upload to S3/MinIO, pre-signed downloads, type/size limits; Evidence tab on Company Detail

**Acceptance criteria**
1. Queuing 10 companies (MockAiProvider) processes them in batches of `RESEARCH_BATCH_SIZE`; one seeded dead-website company → FAILED with error; other 9 COMPLETE
2. Daily cap set to 5 → 6th request returns 422 with clear message; cap bar reflects usage
3. Every persisted AI claim has source_url + excerpt + captured_at; a mock claim without evidence is stored as not_found (integration-tested)
4. Review flow: reject one claim (reason required), accept rest, Verified gate disabled until all resolved, verify marks record VERIFIED and lands on timeline
5. PDF attaches, appears on timeline, downloads via pre-signed URL; 25 MB file rejected with clean error
6. AI call log records capability, model, prompt_version per call

---

## Sprint 4 — Decisions: ICP Engine, Qualification, Reports

**Goal:** Explainable, evidence-backed tier decisions and management-grade reporting.

**Scope**
- ICP profiles + versions (edit = new version); Settings ICP editor; **seeded default ICP v1** (Section: Seed Data)
- Qualification draft endpoint: hard filters + QualificationSuggestCapability (per-criterion answer + rationale + evidence_ids) + computed tier
- Qualification POST: append-only, override-reason and disqualification-reason enforcement (422 paths); Qualifications tab (history)
- Qualification screen per UI spec (evidence side panel)
- Reports: source-funnel, pipeline, quality, qualification-outcomes (endpoints + screens + CSV download)
- Role-aware Dashboard completed (real tiles)

**Acceptance criteria**
1. A verified seed company drafts computed Tier A with per-criterion evidence chips; confirming persists and shows on timeline with ICP version
2. Changing final tier without override_reason → 422; with reason → persisted and flagged `overridden`
3. Editing the ICP creates v2; the old qualification still displays v1; re-qualification under v2 appends (both visible in history)
4. Disqualify requires a configured reason; reasons report ranks them correctly against seed data
5. Source-funnel report shows imported→deduped→qualified(by tier)→exported per source matching seed-data ground truth exactly
6. Trader company fails hard filters → only Disqualify offered

---

## Sprint 5 — Handoff: Ready Pool, Export, Admin, Hardening

**Goal:** Qualified leads leave cleanly; Admin controls everything configurable; v1 ships.

**Scope**
- Ready Pool screen (filters, stale ⏱ flags); export creation (CSV via **default export template**, snapshot, state→EXPORTED, re-download); Exports log; export-templates CRUD
- Staleness: nightly flagger + threshold setting + export-time warning for stale rows
- Settings completed: import/export templates, reasons (deactivate-only), system caps, users mirror
- Hardening: empty states, problem+json details surfaced, OpenAPI complete, seeded demo walkthrough in README
- Full-pipeline integration test: import → triage → research (mock) → qualify → export, asserting timeline + source-funnel end state

**Acceptance criteria**
1. Sales Lead exports 3 Tier-A companies → CSV matches default template exactly; companies EXPORTED; Analyst gets 403 on export
2. Export containing a stale record shows warning but does not block; export re-downloadable from log with identical content
3. Deactivated reason disappears from new dropdowns but still renders in historical records
4. Changing staleness threshold in Settings changes flags after the nightly job (manually triggerable in dev)
5. Full-pipeline integration test passes in CI
6. A new user following the README goes from `docker compose up` to a completed demo walkthrough without asking a human

---

## Seed Data (ships in `V2__seed.sql` + `/samples`)

**Default ICP Profile v1 — "Manufacturing SME — Owner-Driven"** (from the sales playbook; placeholder weights pending sign-off — configuration, not code):
```json
{ "hard_filters": [
    {"key":"industry_manufacturing","label":"Is a manufacturer"},
    {"key":"not_trader","label":"Not a trader"},
    {"key":"location_in_scope","label":"Location in target geography"} ],
  "criteria": [
    {"key":"md_access","label":"Direct MD/Owner access","weight":30,"evidence_required":true},
    {"key":"owner_driven","label":"Owner involved in daily operations","weight":25,"evidence_required":true},
    {"key":"org_size","label":"Under ~300 employees, minimal layers","weight":20,"evidence_required":false},
    {"key":"erp_maturity_low","label":"No ERP or poorly used ERP","weight":15,"evidence_required":true},
    {"key":"visible_inefficiency","label":"Visible cost leakage / manual processes","weight":10,"evidence_required":false} ],
  "tier_thresholds": {"A":80,"B":55,"C":30} }
```
**Default export template:** `company_name, website, phone, city, state, cluster, tier, md_name, md_phone, primary_contact_name, primary_contact_phone, primary_contact_email, prospectsoul_id`

**Disqualification reasons (starter):** Trader / Out of target cluster / Too large / corporate / No MD path / Company inactive or dead / Duplicate — junk data / Out of geography / Other (note required)

**Sample files in `/samples`:**
- `sample_generic.csv` — 25 clean rows (name, phone, email, website, city, industry, contact_name, designation)
- `sample_indiamart.csv` — 30 rows with IndiaMART-style headers and 8 engineered duplicates: `+91` prefixed phones, `Pvt Ltd` vs `Private Limited`, `www.` vs bare domain, `Madras` vs `Chennai`, plus 3 malformed rows (missing name / 6-digit phone / landline)
- `sample_dirty.csv` — stress file: blank rows, duplicate headers, unicode names, 15-digit numbers
- Seeded demo companies spanning all pipeline states (incl. one DISQUALIFIED trader and one dead-website company) so timelines, reports, and the returning-company banner are demonstrable from first launch

## Test Strategy Summary

| Layer | What | When |
|---|---|---|
| Unit | Normalization, dedup matcher, tier computation, evidence enforcement, state machine | Every sprint — these ARE the product |
| Integration (Testcontainers) | Sprint's main flow; Sprint 5 adds the full-pipeline test | Every sprint |
| API | Role 403 paths, 409/422 business-rule paths, problem+json shape | Every sprint |
| AI | Capabilities against recorded fixtures; MockAiProvider in CI — no live AI calls in CI, ever | Sprints 3–4 |
| Manual demo script | README walkthrough per sprint, extended each sprint | Every sprint |
