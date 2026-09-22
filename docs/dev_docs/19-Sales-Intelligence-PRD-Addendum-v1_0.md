<!--
Document: 19-Sales-Intelligence-PRD-Addendum-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — pending sign-off
Scope: NIC Master · Company Field & Contact Extension · Location Intelligence · Export
Audience: Product, Engineering, QA, Claude Code
Companion to: PRD v1.1, Domain Model v1.0, Technical Design Spec v1.0, UI/UX Spec v1.0, Implementation Roadmap v1.0, docs 01–18
-->
# ProspectSoul — Sales Intelligence Extension (PRD Addendum)

**Version:** 1.0
**Status:** Proposed — pending sign-off
**Owner:** Senthil, COO — Vyoog Information Private Limited

---

## 0. Purpose

This document extends PRD v1.1. It does not replace it.

Five additions requested by the Sales team, informed by inspection of a real 123,659-row source file (Kanchipuram MSME registry, `Kanchipuram_data.xlsx`):

1. **NIC Code Master** — full CRUD module with hierarchical classification, import, and a primary/featured flag
2. **Company field extension** — the seven fields already agreed, plus `pincode`, `district`, `address_line`, `registration_date`, `source_reference`
3. **Multiple contacts per company with role tagging** — replacing the implicit "one primary contact" model with an explicit multi-contact, role-tagged model
4. **Company Map (Google Maps)** — pincode search showing owned companies as a map + list, with add-to-system on live results
5. **Export from Companies List** — filter-scoped CSV/Excel download, distinct from the existing pipeline Export

Per the Developer Guide rule — *"when documents conflict, do not silently choose one; stop and resolve the conflict"* — one naming conflict is flagged and resolved in §4 Decision 2, and one existing behavior (contacts model, dedup rules) is deliberately extended in ADR-0001 and ADR-0004.

---

## 1. Changelog Summary

| Status | Item | Specified in |
|---|---|---|
| NEW | NIC Master module — CRUD, hierarchical, importable, primary flag | §2.1 · Tech Addendum §3 · UI Addendum §2 |
| EXTEND | `companies` gains 12 nullable columns | §2.2 · Tech Addendum §4 · ADR-0003 |
| REWORK | `contacts` model extended for multi-contact with role tags | §2.3 · Tech Addendum §5 · ADR-0001 |
| NEW | `company_nic_codes` join table for multi-NIC per company | §2.4 · Tech Addendum §6 · ADR-0002 |
| NEW | Hierarchical NIC-based filter on Companies List | §2.5 · UI Addendum §4 |
| NEW | Company Map (Google Maps) with owned + external results | §2.6 · Tech Addendum §7 · UI Addendum §5 |
| NEW | Companies List Export (filter-scoped download) | §2.7 · Tech Addendum §8 · UI Addendum §6 |
| NEW | ADR folder convention for documenting decisions | See `/adr` |
| DEFERRED | Automated NIC re-classification, GST registry validation, scheduled pincode sweeps, Udyam-number matching | §5 |

---

## 2. What Is Added

### 2.1 Problem Statement (Extension)

The existing model supports company identity, evidence, and qualification. It does not yet support what the **Sales Lead** actually does day to day:

- Filter by a standard industrial classification
- Filter by turnover, employee count, GST presence, region
- Track multiple people at a company with their role (MD, HR, Purchase Head)
- See geographic coverage on a map, and spot nearby businesses not yet in the system
- Pull a filtered CSV to work offline or share

Two additional realities the Kanchipuram file surfaces:

- **Registry sources have no phone, no email, no website, no GST, no turnover, no headcount.** Seven of the twelve ICP fields are empty on import. Records land with very low completeness and require enrichment before ICP qualification is even possible.
- **A single company routinely holds multiple NIC codes.** 25,514 of 123,659 rows (21%) declare more than one activity; the maximum observed is 91. A single `industry` string cannot represent this without silent data loss.

### 2.2 Company Field Extension

All additions are nullable. None changes an existing field's meaning. The `companies` table is not recreated.

| New field | Type | Sales need it answers | Source column example |
|---|---|---|---|
| `pincode` | VARCHAR(6) | Location, map lookup key | `Pincode` |
| `district` | VARCHAR(120) | Location, region derivation | `District` |
| `address_line` | TEXT | Full postal address | `CommunicationAddress` |
| `region` | VARCHAR(120) | Sales-territory grouping | derived / manual |
| `products` | TEXT | Products the company makes/sells | manual |
| `turnover` | NUMERIC | Commercial size signal | manual |
| `gst_number` | VARCHAR(15) | Registration identifier | manual |
| `employee_count` | INTEGER | Headcount signal | manual |
| `registration_date` | DATE | Government registration date | `RegistrationDate` |
| `source_reference` | VARCHAR(120) | Registry identity, for dedup + lineage | derived per source |
| `lg_state_code` | SMALLINT | Optional govt lineage | `LG_ST_Code` |
| `lg_district_code` | INTEGER | Optional govt lineage | `LG_DT_Code` |
| `primary_nic_code_id` | UUID FK | Denormalized primary NIC, for fast filter/display | first activity in source |

Existing columns cover: `canonical_name`, `primary_phone_normalized`, `email`, `website_domain`, `city`, `state`, `industry`, `cluster`.

**Contact Person is not a `companies` column.** See §2.3.

### 2.3 Multi-Contact with Role Tags

Today `contacts` carries a `name`, `designation`, `is_primary` and an MD flag. This proved insufficient once Sales asked to record "who this contact is" — an MD, an HR head, a purchase manager, an accountant — because designation is free text and cannot be filtered or reported on cleanly.

**Extension:**

- A company has zero-to-many contacts, no upper limit
- Each contact carries an explicit `role` from a configured, extensible list (Admin-editable)
- Multiple contacts can share a role (two purchase managers is legal); the `is_primary` flag remains the single "default contact" for list/export displays
- `is_md_owner` is preserved for backward compatibility and mapped to the new role tag `MD` at migration time

**Starter role list** (Admin can add):
`MD / Owner`, `Director`, `CEO`, `COO`, `CFO`, `HR Head`, `Purchase Head`, `Sales Head`, `Plant Head`, `Accounts`, `Admin`, `IT`, `Other`.

See ADR-0001 for why role becomes a first-class field rather than being inferred from `designation`.

### 2.4 Multi-NIC per Company

A single company can carry multiple NIC codes. The `companies` table cannot hold this on a scalar column without losing data.

**Structure** (specified in Tech Addendum §6, decided in ADR-0002):

- A join table `company_nic_codes` — one row per (company, NIC code) pair
- Each row keeps the raw code and description as supplied (so unmatched codes are never lost), plus the resolved `nic_code_id` when a match exists in the master
- One row per company may be flagged `is_primary`, denormalized onto `companies.primary_nic_code_id` for fast list/filter rendering
- On import the first activity becomes primary by default; the user can change it in the Company Edit screen

### 2.5 Hierarchical NIC Filter

The Sales team wants to click a parent NIC (like `22` — Rubber and plastics) and see every company under it and its sub-codes, grouped:

```
▾ 22   Manufacture of rubber and plastics products         (47 companies)
   ▾ 221  Manufacture of rubber products                   (18)
   ▾ 222  Manufacture of plastics products                 (29)
   ▸ Directly tagged to 22 (no sub-code)                    (0)
```

**Two views, one screen:**

- Flat table (default) — every company as a row, existing behavior
- Grouped tree (toggle) — companies grouped by their NIC hierarchy, lazy-loaded per node

The "Directly tagged to parent" bucket is essential: companies may carry a 2-digit or 3-digit code with no sub-classification, and they must remain visible.

### 2.6 Company Map (Google Maps)

Enter a pincode. See owned companies on the map. See external results (live from Google Places) in a separate tab. Add any external result to the system with one click, routed through the existing manual-entry path.

Detailed screens in UI Addendum §5. External results are **never persisted automatically**.

### 2.7 Companies List Export

The existing pipeline `Export` (PRD Stage 5, `POST /api/v1/exports`) exists to hand qualified leads off to a CRM — it moves companies to state `EXPORTED` and logs the event.

The Sales team also wants a plain **filter-scoped download** — "give me a CSV of the 340 companies I'm currently viewing" — that must **not** change pipeline state.

These are two distinct operations. Tech Addendum §8 specifies both, and ADR-0005 records the decision to keep them separate rather than overload the existing endpoint.

---

## 3. What This Is NOT

- **Not a change to ICP Qualification** (PRD §3.3). NIC filtering is a search facet on `companies` — no tier, no version, no qualification record.
- **Not an automated scraping engine.** PRD §1.3 and §12 already rule that out. The Places lookup is human-triggered, session-scoped, and never persisted.
- **Not a second creation path.** External results and "Add to ProspectSoul" both route through the existing `POST /api/v1/companies` manual-entry flow — one door in.
- **Not a rewrite of the existing `contacts` table.** ADR-0001 records what changes and why; existing rows migrate cleanly.

---

## 4. Key Design Decisions

| # | Decision | Rationale | ADR |
|---|---|---|---|
| 1 | Contact **role** becomes a first-class enum/reference, not free-text `designation` | Sales must filter and report by role — impossible against free text. Backward-compatible: `is_md_owner=true` migrates to `role='MD / Owner'`. | ADR-0001 |
| 2 | NIC codes attach to companies via a **join table**, not a scalar column | 21% of the Kanchipuram source has multiple NIC codes; max observed is 91. A scalar column silently discards everything after the first — a direct violation of the "nothing silently dropped" invariant. | ADR-0002 |
| 3 | New company fields are **additive**; the table is not recreated | Preserve every existing row and applied migration. Extend, do not replace. | ADR-0003 |
| 4 | Dedup rules are **extended for registry sources** (name + pincode + address similarity, plus `source_reference` first) — not left at the existing rule ③ | Real registry data has no phone/website (rules ① and ② never fire) and heavily repeated proprietor names (15,002 duplicated names in one file). Existing rules would generate tens of thousands of false candidates and bury the Triage Queue. | ADR-0004 |
| 5 | Companies List **Download** is a distinct operation from the pipeline **Export** | Overloading the pipeline Export would silently push companies to `EXPORTED` state every time an analyst downloads a filter. Two verbs, two endpoints, one clear meaning each. | ADR-0005 |
| 6 | External Places results are **never persisted automatically**; add-to-system routes through manual entry | Mirrors Domain Model Invariant 2 — no external system changes state on its own. Human decision gate stays in place. | — |
| 7 | NIC parent/child links are **resolved at import time** by longest existing prefix, stored as `parent_id` | Truncation-by-length fails on real data — some branches skip levels. Storing an explicit `parent_id` is correct once and cheap to query forever. | — |
| 8 | NIC master import is **reference-data import**, separate from `import_batches` | Those tables model prospect lineage. A classification list has no lineage story. | — |
| 9 | **"ICP fields"** in casual sales terminology refers to the 12 filter fields; the term **ICP Qualification** stays reserved for the versioned Tier decision engine (PRD §3.3) | Two mechanisms sharing one name would blur what "ICP" means in code, reports, and the timeline. | — |
| 10 | The NIC master is admin-editable with a **primary/featured flag** at both parent and sub-code level | Filter pickers otherwise force scrolling 2,000+ codes; primary flag surfaces the codes Sales actually uses first. Setting is per-node — a Division can be primary without its children being. | — |

---

## 5. Explicitly Out of Scope for This Addendum

- Automated re-classification or bulk re-tagging of existing companies against NIC
- GST validation against a live government registry (format-only in v1)
- Scheduled or recurring pincode sweeps — every lookup is user-initiated
- Caching or storing raw external API responses beyond the browser session
- Udyam registration number as a native identity field (deferred pending confirmation that the number is universally present in future sources)
- Automated contact-movement detection when a person's role changes
- Merging duplicate contacts across companies (belongs to the deferred person-entity model)

---

## 6. Open Items Before Build

| # | Item | Decision needed |
|---|---|---|
| 1 | Turnover format | Exact figure (`NUMERIC`) or a band like `size_band`? |
| 2 | Employee count | Exact integer, or is `size_band` sufficient — making this field redundant? |
| 3 | GST validation depth | Regex only, or check-digit validate? No live lookup in v1 either way. |
| 4 | Region taxonomy | Free text, or a configured Admin list like `disqualification_reasons`? |
| 5 | Google Cloud billing owner | Who sets `PLACES_DAILY_QUOTA` and holds the API key? |
| 6 | Unmatched NIC on import | NULL + flag, or auto-create a placeholder for review? |
| 7 | Contact role list ownership | Admin-configurable (recommended) or fixed enum? |
| 8 | Low-completeness registry imports | Land in `TRIAGE` alongside qualified prospects, or a separate holding state so 100k+ unqualifiable records don't swamp the list? |
| 9 | Multi-NIC filter behavior | When a company has 5 NIC codes and 2 match the filter, is it one row (with all matches badged) or two rows in the grouped tree? |
