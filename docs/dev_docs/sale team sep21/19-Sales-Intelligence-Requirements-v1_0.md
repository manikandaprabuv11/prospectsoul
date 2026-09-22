<!--
Document: 19-Sales-Intelligence-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Scope: NIC Master · Company Field & Contact Extension · Location Intelligence · List Download
Audience: Product, Engineering, QA, Claude Code
Companion to: PRD v1.1, Domain Model v1.0, Technical Design Spec v1.0, UI/UX Spec v1.0, Implementation Roadmap v1.0, Developer Guide, docs 01–18
-->
# ProspectSoul — Sales Intelligence Extension: Requirements

**Version:** 1.0
**Status:** Ready for implementation
**Owner:** Senthil, COO — Vyoog Information Private Limited

---

## 1. Purpose

This document extends PRD v1.1 to cover the Sales Intelligence needs on top of the existing Company Management vertical slice (docs 01–18). It does not replace anything in the base documentation.

Five capabilities are added:

1. **NIC Code Master** — a full CRUD module for the Government of India NIC classification, with hierarchy, import, and a primary/featured flag per node
2. **Company field extension** — additional fields on the existing `companies` table so Sales can filter and act on real registry data
3. **Multiple contacts per company with role tags** — replacing the implicit "one primary contact + designation" model with an explicit multi-contact, role-tagged model
4. **Company Map (Google Maps)** — pincode-driven map showing owned companies plus live external results, with an "Add to ProspectSoul" action
5. **Companies List Download** — a filter-scoped CSV/Excel download distinct from the existing pipeline Export

Per the Developer Guide rule — *when documents conflict, do not silently choose one; stop and resolve the conflict* — every behavior change to existing flow is called out in §4 and requires an ADR at implementation time (per §17 and doc 22).

---

## 2. Authoritative Project Context

Existing documentation hierarchy that must be respected:

1. PRD v1.1
2. Domain Model v1.0
3. Technical Design Specification v1.0
4. UI/UX Specification v1.0
5. Implementation Roadmap v1.0
6. Developer Guide / CLAUDE.md
7. Docs 01–18 (Company Management vertical slice — already implemented)

This document sits at the same level as PRD v1.1 for its scope. Docs 20 (Technical Specification), 21 (Implementation Plan) and 22 (Claude Kickoff Prompt) sit under it.

---

## 3. Sample Data Reference

This is one representative row-set the system must import through the existing Companies option:

```
LG_ST_Code  State        LG_DT_Code  District      Pincode  RegistrationDate  EnterpriseName
33          TAMIL NADU   574         KANCHIPURAM   600091   17/11/2025        GRACE BLUE METALS & SUPPLIERS
33          TAMIL NADU   574         KANCHIPURAM   600097   17/11/2025        Vijaya enterprises
33          TAMIL NADU   574         KANCHIPURAM   602105   17/11/2025        SHIV SHAKTI ELECTRICALS HARDWARE
33          TAMIL NADU   574         KANCHIPURAM   631604   17/11/2025        Balaji Mobiles
33          TAMIL NADU   574         KANCHIPURAM   600100   17/11/2025        DREAMY DAZZLE EVENT
33          TAMIL NADU   574         KANCHIPURAM   631502   17/11/2025        DEVI THANIKAIVEL
33          TAMIL NADU   574         KANCHIPURAM   603403   17/11/2025        SRI ELUMALAIYAN FLOUR MILL
```

Design rules the sample enforces:

- The importer must accept a file with only these 7 columns and create valid company records (all other fields NULL)
- The importer must also accept richer variants: the full Kanchipuram MSME file has `CommunicationAddress` and `Activities` (JSON array of NIC codes) on top of these seven, and hand-curated lists add Phone / Email / Website / GST / Turnover / Employees / Contact / Role
- Missing columns are never a hard error at the schema layer — validation is per-row (required: `canonical_name` only)
- The 7-column sample lands with `pincode`, `state`, `district`, `registration_date`, `lg_state_code`, `lg_district_code`, and `source_reference` populated; `contacts`, `company_nic_codes`, `address_line` remain empty until enriched

**Real-file baseline** (`Kanchipuram_data.xlsx`, 123,659 rows analyzed):

- 21% of rows carry more than one NIC code; maximum 91 on a single row
- 15,002 duplicate proprietor-style names
- 6 blank enterprise names → rejected
- 10 rows with malformed Activities JSON → flagged, still persisted
- 106 rows with empty activities array → company created, zero NIC rows
- Seven of the twelve "ICP fields" are entirely absent (no phone, no website, no email, no GST, no turnover, no headcount, no contact)

The design must not fail against this shape of data.

---

## 4. Additions in Detail

### 4.1 NIC Code Master

**Purpose:** A single source of truth for the NIC classification, hierarchical, admin-editable, importable from the government Excel.

**Data shape:**
- Every code has: `code` (1–5 digits), `description`, `industry_type` (Service / Manufacturing), `level` (derived from code length), `parent_id` (self-referencing), `is_primary`, `active`
- `parent_id` is resolved at import time by **longest existing prefix** (some branches skip levels — length-based truncation fails)
- Any node at any level can be marked `is_primary` (surfaces first in filter pickers)
- A company can be classified at any level, and at any number of nodes (see §4.3)

**Operations required:**
- Import from the master Excel (idempotent by `code`)
- Manual add, edit, deactivate (never hard-delete)
- Toggle `is_primary` per node
- Tree browse and search
- Deactivation of a code referenced by any company is blocked unless forced

**Not required in v1:** automatic re-classification of existing companies against an updated master, alternate classification systems (HSN, SIC).

### 4.2 Company Field Extension

Twelve new nullable columns on the existing `companies` table. The table is **not recreated** (ADR-required if it is — doc 22 §12).

| New field | Type | Sales need it answers | Sample-column source |
|---|---|---|---|
| `pincode` | VARCHAR(6) | Location, map lookup | `Pincode` |
| `district` | VARCHAR(120) | Location, region derivation | `District` |
| `address_line` | TEXT | Full postal address | `CommunicationAddress` (when present) |
| `region` | VARCHAR(120) | Sales-territory grouping | derived / manual |
| `products` | TEXT | Products the company makes/sells | manual |
| `turnover` | NUMERIC | Commercial size signal | manual |
| `gst_number` | VARCHAR(15) | Registration identifier | manual |
| `employee_count` | INTEGER | Headcount signal | manual |
| `registration_date` | DATE | Government registration date | `RegistrationDate` (parse `dd/MM/yyyy`) |
| `source_reference` | VARCHAR(120) | Registry identity for dedup + lineage | derived per source |
| `lg_state_code` | SMALLINT | Optional govt lineage | `LG_ST_Code` |
| `lg_district_code` | INTEGER | Optional govt lineage | `LG_DT_Code` |
| `primary_nic_code_id` | UUID FK | Denormalized primary NIC | first activity in source |

Existing columns already cover: `canonical_name`, `primary_phone_normalized`, `email`, `website_domain`, `city`, `state`, `industry`, `cluster`, `source`.

### 4.3 Multi-Contact with Role Tags

**What changes:** the `contacts` model becomes explicitly multi, with a first-class **role** tag replacing free-text `designation` as the filter/report dimension.

- A company has zero-to-many contacts, no upper limit
- Each contact carries a `role` from a configured, admin-editable list
- Multiple contacts can share a role (two Purchase Heads is legal)
- `is_primary` remains the single default-display flag; setting a new primary demotes the previous atomically
- `is_md_owner` is preserved for backward compat and mapped to `MD / Owner` at migration

**Starter role list (admin can add):**
`MD / Owner · Director · CEO · COO · CFO · HR Head · Purchase Head · Sales Head · Plant Head · Accounts · Admin · IT · Other`

### 4.4 Multi-NIC per Company

A single company can carry multiple NIC codes. Storing this on a scalar `industry` column silently discards everything after the first — a direct violation of the "nothing silently dropped" invariant.

Structure:
- Join table `company_nic_codes` — one row per (company, NIC) pair
- Preserves the raw code + description as supplied (unmatched codes never lost)
- Resolved `nic_code_id` links to master when a match exists, NULL otherwise
- Exactly one row per company flagged `is_primary` (partial unique index enforces this at DB level)
- `is_primary` denormalized to `companies.primary_nic_code_id` for fast list rendering

### 4.5 Hierarchical NIC Filter on Companies List

Clicking a parent NIC (like `22` — Rubber and plastics) filters the list to every company under it and its sub-codes. Two views:

- **Flat table** (default) — every company as a row, existing behavior extended
- **Grouped tree** (toggle) — companies grouped by NIC hierarchy, lazy-loaded per node, with a "Directly tagged to parent (no sub-code)" bucket for companies carrying only the parent

### 4.6 Company Map — Google Maps

- Enter a pincode → map centered on that pincode
- **Owned tab:** companies in the system in that pincode (with radius)
- **External tab:** live results from Google Places, visually distinct (dashed border, amber pins)
- **"+ Add"** on any external result opens the manual-entry form pre-filled — routes through the existing creation path (batch-of-one, normalization, dedup, audit)
- External results are **never persisted automatically**
- Daily quota counter surfaced in the UI; exceeded → clear 429/422 with detail

### 4.7 Companies List Download

The existing pipeline `POST /api/v1/exports` moves companies to state `EXPORTED` and logs the event. Sales also needs a filter-scoped download that does not do either.

Two operations, two verbs:

- **Export** — pipeline Stage 5 handoff. State → `EXPORTED`. Logged. Sales Lead+. Unchanged.
- **Download** — filter-scoped CSV/Excel from the Companies List. No state change. No `exports` row. Audit only. Any read role.

The words are not interchangeable in labels, tooltips, or error messages.

---

## 5. Users and Roles

No new roles. Existing roles gain new capabilities:

| Capability | ANALYST | SALES_LEAD | ADMIN | VIEWER | COO |
|---|---|---|---|---|---|
| NIC master read | ✓ | ✓ | ✓ | ✓ | ✓ |
| NIC master mutate | – | – | ✓ | – | – |
| NIC master import | – | – | ✓ | – | – |
| Contact roles read | ✓ | ✓ | ✓ | ✓ | ✓ |
| Contact roles mutate | – | – | ✓ | – | – |
| Multi-NIC attach/detach on company | ✓ | ✓ | ✓ | – | – |
| Multi-contact CRUD on company | ✓ | ✓ | ✓ | – | – |
| Companies List (with new filters) | ✓ | ✓ | ✓ | ✓ | ✓ |
| Company Map — owned tab | ✓ | ✓ | ✓ | ✓ | ✓ |
| Company Map — external tab | ✓ | ✓ | ✓ | – | – |
| "+ Add" from external | ✓ | ✓ | ✓ | – | – |
| Companies List Download | ✓ | ✓ | ✓ | ✓ | – |
| Pipeline Export (existing) | – | ✓ | ✓ | – | – |

Authorization enforced server-side.

---

## 6. Domain Concepts (Vocabulary)

Say the same words everywhere.

| Term | Means | Never means |
|---|---|---|
| NIC Code | A node in the Government of India NIC classification tree | An ICP criterion, a tier, or a cluster |
| NIC Filter | A taxonomy filter on the Companies List | ICP Qualification |
| Primary NIC | The default NIC for a company, shown on lists | The only NIC — a company can have many |
| Contact Role | A first-class tag describing what a contact is at the company | `designation` — that stays as free text |
| Primary Contact | The default-display contact | The only contact |
| External Result | A live record from Google Places, not in ProspectSoul | A Company record |
| Region | A sales-territory grouping | A state, district, or cluster |
| Download | A filter-scoped CSV/Excel — no state change | Pipeline Export |
| Export | Pipeline Stage 5 handoff — logged, state → `EXPORTED` | A casual download |

---

## 7. Business Invariants (Additions)

Extending Domain Model v1.0 §5:

10. **NIC classification never blocks company creation.** Present → linked or raw-only. Absent → no rows in `company_nic_codes`. No validation error either way.
11. **NIC codes are reference data**, not per-record free text. Analysts pick from the master; they don't type into a classification field.
12. **Every company has zero or one primary NIC.** Setting a new primary demotes the previous atomically. Enforced by partial unique index at DB level.
13. **Every company has zero or one primary contact.** Same atomic rule.
14. **Contact role is a controlled vocabulary.** New roles are added via Admin only; nothing else creates a role value.
15. **External location results are transient.** No id, no persistence, until a human explicitly adds one via the manual-entry flow.
16. **Region ≠ State ≠ District ≠ Cluster.** Four distinct facets.
17. **Companies List Download does not change pipeline state.** Only pipeline Export does.
18. **Everything nullable is genuinely nullable.** A row from a 7-column import file (§3) must succeed with 11 of the 12 new fields NULL.

---

## 8. Key Design Decisions

Ten decisions taken deliberately. Recorded so they are challenged with context, not re-litigated.

| # | Decision | Rationale |
|---|---|---|
| 1 | Contact role becomes a first-class enum, not free-text `designation` | Sales must filter and report by role — impossible against free text. Backward-compatible: `is_md_owner=true` migrates to `role='MD_OWNER'`. |
| 2 | NIC attaches via a **join table**, not a scalar column | 21% of registry rows carry multiple NIC codes; max observed 91. A scalar column silently discards everything after the first. |
| 3 | New company fields are **additive** via `ALTER TABLE`; the table is not recreated | Preserve every existing row and applied migration. Extend, don't replace. |
| 4 | Dedup gets a new **rule ⓪** for registry sources: match on `source + source_reference` first | Registry data has no phone/website (existing rules ①-② never fire) and 15,002 duplicated proprietor names in one file. Without rule ⓪, re-import would swamp the Triage Queue. |
| 5 | **Download** and **Export** are distinct operations, distinct endpoints, distinct words | Overloading pipeline Export would silently push companies to `EXPORTED` state on every casual analysis download. |
| 6 | External Places results are **never persisted automatically** | Mirrors Invariant 2 — no external system changes state on its own. |
| 7 | NIC parent links resolved by **longest existing prefix** at import, stored as `parent_id` | Truncation-by-length fails on real data — some branches skip levels. |
| 8 | NIC master import is **reference-data import**, separate from `import_batches` / `import_rows` | Those tables model prospect lineage. A classification list has no lineage story. |
| 9 | "ICP fields" in Sales terminology refers to the 12 filter fields; **ICP Qualification** stays reserved for the versioned Tier decision engine | Two mechanisms sharing one name would blur what "ICP" means in code, reports, and the timeline. |
| 10 | NIC master is admin-editable with a **primary/featured flag per node** | Filter pickers otherwise force scrolling 2,000+ codes; primary flag surfaces the codes Sales actually uses first. |

---

## 9. Explicitly Out of Scope

- Automated NIC re-classification of existing companies
- GST validation against a live government registry (regex format only in v1)
- Scheduled or recurring pincode sweeps — every lookup is user-initiated
- Persistence or caching of raw external API responses beyond the browser session
- Udyam registration number as a native identity field
- Semantic / AI duplicate detection (pgvector) — deferred pending data volume
- Automated contact-movement detection when a person's role changes
- Cross-company contact merging (person-entity model — deferred)
- CRM outreach, calling, WhatsApp, or email from within this tool
- Company relationships (parent/subsidiary/plant) — table designed, not built
- Multiple active ICP profiles and A/B comparison

---

## 10. Acceptance Criteria (Summary)

Full per-track criteria in doc 21. This is the high-level:

- NIC master imports from the government Excel with **zero unresolved parents**
- The 7-column sample in §3 imports without error, creating valid company records
- The Kanchipuram Excel imports: **123,653 created · 6 rejected · 10 flagged · 175,861 NIC join rows**
- `is_md_owner=true` contacts migrate to `role='MD_OWNER'` with no data loss
- Existing companies and applied migrations are untouched
- A company can carry many NIC codes and many contacts, each with a primary
- Recursive NIC filter returns exact counts against seed
- Places search writes zero rows to any table
- Download does not change pipeline state
- Every 403 path tested (Viewer on mutations, non-Admin on `/admin/*`)
- Every mutation audited

---

## 11. Open Items Before Build

| # | Item | Decision needed |
|---|---|---|
| 1 | Turnover format | Exact `NUMERIC` or a band like `size_band`? |
| 2 | Employee count | Exact integer, or is `size_band` sufficient? |
| 3 | GST validation depth | Regex only, or check-digit validation? No live lookup either way. |
| 4 | Region taxonomy | Free text, or an admin-editable list? |
| 5 | Google Cloud billing owner | Who owns `GOOGLE_PLACES_API_KEY` and sets `PLACES_DAILY_QUOTA`? |
| 6 | Unmatched NIC on import | NULL + flag, or auto-create a placeholder for review? |
| 7 | Contact role list ownership | Admin-configurable (recommended) or fixed enum? |
| 8 | Low-completeness registry state | Land in `TRIAGE` alongside prospects, or a separate holding state so 100k+ don't swamp the list? |
| 9 | Multi-NIC grouped-view row | One row per company (badged with matches) or one row per matching NIC? |
