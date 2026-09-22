<!--
Document: 21-Sales-Intelligence-Implementation-Plan-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Engineering, QA, Claude Code
Companion to: Sales Intelligence Requirements v1.0 (doc 19), Technical Specification v1.0 (doc 20)
-->
# ProspectSoul — Sales Intelligence Extension: Implementation Plan

**Version:** 1.0
**Execution model:** vertical slices, five sequential tracks
**Owner:** Engineering / Claude Code

---

## Phase 0 — Repository and Specification Audit

Before any code changes, produce a **gap assessment**.

Tasks:
- Read all authoritative docs in order (see doc 22 §Authoritative Documents)
- Inspect current `companies` table, entity, all applied Flyway migrations — confirm which of the 12 new fields already exist
- Inspect current `contacts` table, entity — confirm `is_md_owner`, `is_primary`, `designation`
- Inspect current import framework, header normalizer, alias registry
- Inspect current frontend routes and app shell
- Inspect authentication / Keycloak role handling
- Inspect existing tests
- Read `CLAUDE.md`

Exit criteria:
- No uncertainty about existing architecture
- Gap table produced: per planned change → new / partially present / already present
- Any specification conflicts identified and flagged (not silently resolved)
- No duplicate implementation started

---

## Sequencing Rule

**Never start track N+1 until track N's acceptance criteria all pass.**

```
Phase 0 gap assessment
        ↓
C1 — NIC Master module
        ↓
C2 — Company field extension + Contact roles + Multi-NIC join
        ↓
C3 — Hierarchical NIC filter + Grouped view
        ↓
C4 — Location Intelligence (Google Maps + Places)
        ↓
C5 — Companies List Download
```

---

## Track C1 — NIC Master

**Goal:** The NIC classification tree exists, is admin-editable, imports from the master Excel, and is queryable at any level.

**Backend scope:**
- Flyway migration `V<next>__nic_master.sql`
- `nic/` module: entity, repository, service, controller, import service
- Longest-prefix parent resolution algorithm
- CRUD endpoints, `PS_ADMIN` guarded
- `is_primary` flag, per-node
- Idempotent re-import by `code`
- Full-tree endpoint cached, invalidated on mutation

**Frontend scope:**
- `/settings/nic-codes` — Table view (default), Tree view (toggle)
- Add / Edit dialog with parent-is-prefix validation
- Import wizard (Upload → Preview → Confirm)
- Reuse existing app shell — do not duplicate

**Acceptance criteria:**
1. Importing the real NIC Excel produces **zero unresolved parents**
2. Re-import is idempotent by `code`; preview reports created vs updated
3. Unit tests cover level detection (1–5 digits) and prefix resolution including skipped-level branches
4. `GET /nic-codes/{id}/children` returns correct direct children at every level
5. `POST /nic-codes` validates parent is a prefix; returns 422 otherwise
6. Deactivating a code referenced by any `company_nic_codes` row returns 422 with affected companies listed
7. Non-admin gets 403 on all mutations (tested)
8. Every mutation writes an `audit_log` row
9. Clean migration from empty database succeeds; existing migrations untouched
10. Full-tree endpoint cached in-memory; cache invalidated on mutation
11. If Claude Code changes any existing flow or code to accommodate this track, an ADR is written in `/adr` (see doc 22 §12)

---

## Track C2 — Company Fields + Contact Roles + Multi-NIC Join

**Goal:** Every field the Sales team needs exists on the model, contacts carry role tags, companies can carry multiple NIC codes.

**Backend scope:**
- Three Flyway migrations (all additive; `companies` and `contacts` not recreated):
  - `V<next+1>__company_sales_fields.sql`
  - `V<next+2>__contact_roles.sql` (with backfill from `is_md_owner`)
  - `V<next+3>__company_nic_codes.sql`
- Entity / DTO / mapper updates for new fields
- Alias-registry additions per doc 20 §9.1
- `UDYAM_MSME_REGISTRY` source template + Activities JSON parser + `dd/MM/yyyy` date parser
- Contact Roles CRUD + service invariants (role↔`is_md_owner` sync, atomic primary)
- Multi-NIC endpoints (attach, detach, make-primary, atomic)
- Dedup rule ⓪ (`source + source_reference`) added ahead of existing rules ①-③

**Frontend scope:**
- Company Edit UI: NIC codes section with star / Make primary / Detach / Attach
- Contacts tab redesigned: multi-card layout with role badge, primary star
- Add-contact dialog with role picker (required)
- `/settings/contact-roles` admin screen

**Acceptance criteria:**
1. Existing companies and applied migrations are untouched; every pre-existing row still loads
2. A source file with **only the 7-column sample from doc 19 §3** imports cleanly: all 7 fields populated, `address_line` and `company_nic_codes` empty, zero rejections
3. A source file with no NIC column imports with zero rows in `company_nic_codes`, no error
4. A source file with unknown NIC codes does not reject the row; raw values stored, `nic_code_id` NULL
5. Kanchipuram Excel imports: **123,653 companies created · 6 rejected (blank names) · 10 flagged (bad JSON) · 175,861 `company_nic_codes` rows created**
6. `is_md_owner=true` contacts migrate to `role='MD_OWNER'` with no data loss; `is_md_owner=false` or NULL migrate to `Other`
7. Setting a new primary NIC atomically demotes the previous — partial unique index enforces at DB level
8. Setting a new primary contact atomically demotes the previous
9. Contact Person renders on the Companies list via join from `contacts` — no denormalized column added
10. Dedup rule ⓪ on `source_reference` catches re-imports of the Kanchipuram file with zero false candidates
11. Registration date parser handles `dd/MM/yyyy` strings (like `17/11/2025`) correctly
12. Non-admin gets 403 on Contact Roles admin endpoints
13. OpenAPI reflects all new endpoints and filters
14. Companies row count matches pre-migration count immediately after migration
15. ADR written for any existing-flow change

---

## Track C3 — Hierarchical NIC Filter + Grouped View

**Goal:** Sales can filter by any NIC node and its descendants, and view results grouped by the hierarchy.

**Backend scope:**
- `GET /companies` filter extension: `nic_code_id`, `nic_parent_id`, `nic_include_descendants`, `view=flat|grouped_by_nic`, `region`, `district`, `pincode`, `turnover_min/max`, `employee_min/max`, `gst_present`, `has_contact_role`
- Recursive CTE for descendant expansion
- Grouped-view response shape (nodes + counts, companies lazy-loaded)

**Frontend scope:**
- Companies List filter panel: NIC picker with primary-first sort, "include descendants" toggle, view toggle
- `GroupedNicList` component with lazy-load per group
- New default columns: Primary NIC, Contact (Primary) with role, Region, Pincode
- Column toggling preserved

**Acceptance criteria:**
1. NIC parent filter with `include_descendants=true` returns the node **and all descendants**; exact counts verified against seed data
2. NIC filter with `include_descendants=false` returns only companies tagged to that exact node
3. Grouped view returns tree + counts in <500ms for a subtree of 200 nodes at 100k envelope
4. Expanding a group triggers a paginated `GET /companies?nic_code_id=…` — initial payload does not include company lists
5. "Directly tagged to parent (no sub-code)" bucket appears with correct count
6. A company with 5 NIC codes across 3 branches renders in every matching group (grouped view) and once (flat view)
7. Region, district, pincode, turnover-range, employee-range, GST-present, has-contact-role filters all return correct sets
8. Filter combinations audit: NIC + turnover + region returns intersection, not union
9. ADR written for any existing-flow change

---

## Track C4 — Location Intelligence

**Goal:** Pincode search shows owned companies on a Google Map and live external results in a separate tab; add-to-system routes through the existing manual-entry path.

**Backend scope:**
- `location/` module: `PlacesLookupService`, `CompanyMapService`, `PincodeCentroidService`
- Pincode centroid reference table + seed migration (from India Post PIN data)
- Endpoints: `/map/pincode/{pincode}`, `/map/companies`, `/external/places-search`
- Daily quota counter + Admin settings entries

**Frontend scope:**
- Google Maps JavaScript integration (restricted key, origin-locked)
- `/companies/map` screen with Owned / External tabs
- Distinct pin styles (solid teal vs dashed amber)
- "+ Add" wired to `/companies/new` pre-filled from Places payload

**Acceptance criteria:**
1. Places search writes **zero rows** to `companies` or any other table (integration-asserted)
2. Quota exceeded returns 429/422 with clear problem+json `detail`; UI surfaces it verbatim
3. Provider unavailable returns 502 problem+json — never a silently empty list
4. External results visually distinct from owned records (dashed border, amber pins)
5. "+ Add" produces a batch-of-one with `source=GOOGLE_PLACES`, runs normalization, is caught by existing dedup if the company already exists in that pincode
6. `GOOGLE_PLACES_API_KEY` never appears in any frontend bundle or network response
7. Maps JavaScript key is origin-restricted at Google Cloud Console
8. Map renders <2s for a pincode with ≤200 owned companies
9. Pincode centroid lookup is offline (no Google call) — from seeded reference table
10. Viewer role blocked from `/external/places-search` (403 tested)
11. ADR written for any existing-flow change

---

## Track C5 — Companies List Download

**Goal:** Any user who can read the list can download it as CSV/XLSX in their currently-filtered scope, with no pipeline state change.

**Backend scope:**
- `POST /api/v1/companies/download` endpoint
- Streaming CSV + XLSX response
- Column selection + one-row-per-company vs one-row-per-contact modes
- `COMPANIES_DOWNLOAD_ROW_CAP` setting
- Audit row per download (who, when, filter snapshot, row count)

**Frontend scope:**
- Download button top-right of Companies List
- Download modal: format, column selection, "All contacts" toggle, filter-match count, explicit no-state-change note
- UI copy: the word "Export" never appears in this flow

**Acceptance criteria:**
1. Download does NOT change any company's `pipeline_state` (integration-asserted)
2. Download does NOT create a row in `exports` (integration-asserted)
3. 10k-row CSV streams within 15s
4. Larger than `COMPANIES_DOWNLOAD_ROW_CAP` returns 422 with "narrow your filter" guidance
5. `include_all_contacts=true` produces one row per (company, contact) pair with role visible
6. `include_all_contacts=false` produces one row per company using primary contact only
7. Audit row written per download
8. UI copy says "Download" everywhere in this flow; the word "Export" never appears in this modal
9. Viewer role can download (read-based); 403 rules do not apply to reads
10. ADR written for any existing-flow change

---

## Release Gate

Do not mark this extension complete until:

- backend build passes
- frontend build passes
- clean migration from empty database passes
- 7-column sample from doc 19 §3 imports cleanly
- Kanchipuram Excel imports with exact counts per C2 criterion 5
- NIC parent + sub filtering pass with exact counts
- Multi-NIC and multi-contact primary-flag atomicity pass
- Pincode search returns results without persisting them
- "+ Add" passes through existing lineage path
- Download does not change pipeline state
- Authorization tests pass (403 paths for every mutation)
- Audit tests pass for every mutation
- Unit + integration tests pass
- OpenAPI accurate
- README updated
- `/adr` folder contains one file per existing-flow change made (empty if none)

---

## Scope Control

Do not implement:

- Automated NIC re-classification of existing companies
- GST validation against a government registry (regex format only)
- Scheduled or recurring pincode sweeps
- Persistence or caching of raw external API responses
- Udyam registration number as native identity field
- Semantic / AI deduplication
- Automated contact-movement detection
- Cross-company contact merging (person-entity model — deferred)
- CRM, outreach, or company-relationship functionality
- AI research or ICP qualification changes

unless an existing dependency makes a minimal implementation necessary, or the requirements doc is formally changed.
