<!--
Document: 24-Sales-Intelligence-Claude-Kickoff-Prompt-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — pending sign-off
Audience: Claude Code
-->
# ProspectSoul — Sales Intelligence Extension: Claude Code Kickoff Prompt

You are the senior full-stack engineer implementing the ProspectSoul Sales Intelligence Extension.

## AUTHORITATIVE DOCUMENTS

Before coding, read in order:

1. ProspectSoul PRD v1.1
2. ProspectSoul Domain Model v1.0
3. ProspectSoul Technical Design Specification v1.0
4. ProspectSoul UI/UX Specification v1.0
5. ProspectSoul Implementation Roadmap v1.0
6. Technical-Team Developer Guide
7. Docs 01–18 (Company Management vertical slice — already implemented)
8. **19 — Sales Intelligence PRD Addendum**
9. **20 — Sales Intelligence Domain Model Addendum**
10. **21 — Sales Intelligence Technical Design Addendum**
11. **22 — Sales Intelligence UI/UX Addendum**
12. **23 — Sales Intelligence Implementation Plan**
13. **/adr** — all ADRs, in numeric order

Document hierarchy must be respected. If documents conflict, **do not silently choose one — identify the conflict, stop, and ask.**

---

## FIRST: INSPECT

Before changing any code, produce a **gap assessment**. Do not begin implementation until it is reviewed.

Inspect:
- Repository structure
- Current `companies` table, entity, all applied Flyway migrations — confirm exactly which of the 12 new fields already exist
- Current `contacts` table, entity — confirm `is_md_owner`, `is_primary`, `designation`
- Current import framework, header normalizer, alias registry
- Current frontend routes and app shell
- Authentication / Keycloak role handling
- Existing tests

Report the gap as a table: for each planned change, is it new, partially present, or already present?

---

## OBJECTIVE

Implement five sequential tracks. **Do not start a track until the previous one's acceptance criteria all pass.**

- **C1** — NIC Master module
- **C2** — Company fields + Contact roles + Multi-NIC join
- **C3** — Hierarchical NIC filter + Grouped view
- **C4** — Location Intelligence (Google Maps + Places)
- **C5** — Companies List Download

Full scope, criteria and required tests are in doc 23.

---

## CRITICAL CONSTRAINTS

Break any of these and the work is not accepted:

1. **Do not recreate the `companies` or `contacts` table.** Additive `ALTER TABLE` migrations only. Never modify an applied migration.
2. **NIC is optional everywhere.** Absent → no rows in `company_nic_codes`, no validation error, row never rejected.
3. **A company can carry multiple NIC codes and multiple contacts.** Scalar-column shortcuts are forbidden — use the join table (`company_nic_codes`) and the existing `contacts` table extended with `role_id`. The Kanchipuram file has one row with 91 NIC codes; anything that discards them is broken.
4. **Contact role is required on new contacts** and comes from the configured `contact_roles` list. `is_md_owner` is preserved for backward compat and kept in sync.
5. **Resolve NIC parents at import time by longest existing prefix**, not fixed-length truncation — real data skips levels.
6. **NIC master import is reference-data import.** Must NOT create `import_batches` / `import_rows` rows.
7. **Pincode search must not fetch external results from the `companies` table.** External results come live from the Places API and are **never persisted automatically**.
8. **"+ Add" (from map) routes through the existing manual-entry endpoint** — batch-of-one, normalization, dedup, audit. Do not build a second creation path.
9. The Places API key stays **server-side only**. Never in a frontend bundle. Maps JS API key is separate, origin-restricted.
10. **Do not rename or repurpose "ICP".** NIC filtering is a search facet on `companies`. ICP Qualification (PRD §3.3) stays reserved for the versioned Tier decision engine.
11. **"Download" ≠ "Export".** Download = filter-scoped CSV/XLSX, no state change. Export = pipeline handoff, changes state to `EXPORTED`. Two endpoints, two verbs, one clear meaning each.
12. **Every behavior change to existing code requires an ADR.** File in `/adr` before merging. New code that follows the specs as written does not need an ADR.

---

## BACKEND RULES

- Feature-first modular architecture: `controller / service / repository / entity / dto / mapper`
- Controllers must never expose JPA entities
- UUID ids, `/api/v1`, snake_case JSON, RFC-7807 errors
- Server-side authorization; test 403 paths for Viewer on mutations, non-Admin on `/admin/*`
- `AuditService` / `audit_log` for every mutation — do not build a second audit system
- Flyway migrations, immutable once applied
- Primary-flag atomicity enforced at both service and DB (partial unique index)

---

## TESTS REQUIRED

**Unit**
- NIC level detection, longest-prefix parent resolution (incl. skipped levels)
- Idempotent NIC re-import
- Recursive descendant expansion
- Activities JSON parser: valid, single, `"NA"`, empty, malformed
- Contact role backfill from `is_md_owner`
- GST format, pincode format
- Atomic primary-flag transitions (NIC and contact)

**Integration (Testcontainers)**
- Real Kanchipuram Excel import — exact expected counts
- Company import with/without/unknown NIC codes
- Recursive NIC filter — exact counts against seed
- Places search — assert zero DB writes
- `+ Add` — batch-of-one with `source=GOOGLE_PLACES`, dedup applied
- Download — pipeline state unchanged, no `exports` row created
- 403 authorization paths
- Audit rows written

---

## DEFINITION OF DONE

Do not claim completion until every criterion in doc 23's Release Gate passes, with evidence per criterion.

---

## FINAL RESPONSE FORMAT

1. Files created / changed
2. Database migrations
3. Backend implementation summary per track
4. Frontend implementation summary per track
5. Kanchipuram import results (row counts, rejections, flags, join-table rows)
6. Tests and exact results
7. Acceptance criteria PASS / FAIL / BLOCKED per criterion, per track
8. ADRs written
9. Specification deviations (none expected — flag with reason if any)
10. Remaining work

**Do not claim PASS without evidence.**

---

## SCOPE CONTROL

Do not implement automated re-classification, GST registry lookups, scheduled pincode sweeps, external-response caching, Udyam-number as identity field, AI research changes, ICP qualification changes, CRM outreach, cross-company contact merging, or company relationships — unless strictly required by an existing dependency.

Start by inspecting the repository and the authoritative documents.
