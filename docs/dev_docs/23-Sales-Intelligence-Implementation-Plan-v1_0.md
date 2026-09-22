<!--
Document: 23-Sales-Intelligence-Implementation-Plan-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — pending sign-off
Audience: Engineering, QA, Claude Code
Companion to: Implementation Roadmap v1.0, Company Implementation Plan v1.0 (doc 03)
-->
# ProspectSoul — Sales Intelligence Extension (Implementation Plan)

**Version:** 1.0
**Execution model:** vertical slices, five sequential tracks

---

## 0. Sequencing Rule

The Company Management vertical slice (docs 01–18) is the currently active work.

This extension runs as a continuation track after that slice's release gate passes. Per the roadmap's own rule: **never start track N+1 until track N's acceptance criteria all pass.**

```
Docs 01–18 slice (Company Management, existing)
        ↓  release gate passes
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

**Scope**
- Flyway migration `V<next>__nic_master.sql`
- `nic/` module: entity, repository, service, controller, import service
- Longest-prefix parent resolution algorithm
- CRUD endpoints, `PS_ADMIN` guarded
- `is_primary` flag, per-node
- Settings screen: table view, tree view, add/edit dialog, import flow

**Acceptance criteria**
1. Importing the real NIC Excel produces **zero unresolved parents**
2. Re-import is idempotent by `code`; preview reports created vs updated counts
3. Unit tests cover level detection (1–5 digits) and prefix resolution including skipped-level branches
4. `GET /nic-codes/{id}/children` returns correct direct children at every level
5. `POST /nic-codes` validates parent is a prefix; returns 422 otherwise
6. Deactivating a code referenced by any `company_nic_codes` row returns 422 with affected companies listed
7. Non-admin receives 403 on all mutations (tested)
8. Every mutation writes an `audit_log` row
9. Clean migration from empty database succeeds; existing migrations untouched
10. Full-tree endpoint cached in-memory; cache invalidated on mutation

---

## Track C2 — Company Fields + Contact Roles + Multi-NIC Join

**Goal:** Every field the Sales team needs exists on the model, contacts carry role tags, companies can carry multiple NIC codes.

**Scope**
- Three Flyway migrations (all ALTER / additive; `companies` and `contacts` not recreated):
  - `V<next+1>__company_sales_fields.sql`
  - `V<next+2>__contact_roles.sql` (with data backfill from `is_md_owner`)
  - `V<next+3>__company_nic_codes.sql`
- Entity/DTO/mapper updates
- Alias-registry entries for the new importable columns (see §Alias Table below)
- `UDYAM_MSME_REGISTRY` source template + Activities JSON parser
- Contact Roles CRUD + Admin settings screen
- Multi-NIC endpoints (attach, detach, make-primary)
- Company Edit UI: NIC codes section + Contacts tab redesign
- Dedup rule ⓪ from ADR-0004 (source_reference match first)

**Alias registry additions**

| Target field | Aliases |
|---|---|
| `pincode` | Pincode, PIN Code, PIN, Postal Code, Zip, Zip Code |
| `district` | District, Dist, District Name |
| `address_line` | Address, Communication Address, Full Address, Registered Address |
| `region` | Region, Zone, Territory, Sales Region |
| `products` | Products, Product, Product Range, Items, Product Line |
| `turnover` | Turnover, Revenue, Annual Turnover, Sales Turnover |
| `gst_number` | GST, GSTIN, GST No, GST Number, GST Registration |
| `employee_count` | Employees, Employee Count, Headcount, No of Employees, Staff Strength |
| `registration_date` | Registration Date, Reg Date, Date of Registration, Registered On |
| activities JSON | Activities, NIC Activities, NIC Codes |
| contact role | Role, Designation Type, Contact Type, Position Type |

**Acceptance criteria**
1. Existing companies and applied migrations are untouched; every pre-existing row still loads
2. A source file with no NIC column imports with zero rows in `company_nic_codes`, no error
3. A source file with unknown NIC codes does not reject the row; raw values are stored, `nic_code_id` NULL
4. Kanchipuram Excel imports: 123,653 companies created, 6 rejected (blank names), 10 flagged (bad JSON), 175,861 `company_nic_codes` rows created
5. `is_md_owner=true` contacts migrate to `role='MD_OWNER'` with no data loss
6. Setting a new primary NIC atomically demotes the previous one — partial unique index enforces at DB level
7. Setting a new primary contact atomically demotes the previous one
8. Contact Person renders on the Companies list via join from `contacts` — no denormalized column added
9. Dedup rule ⓪ on `source_reference` catches re-imports of the same Kanchipuram file with zero false candidates
10. Non-admin gets 403 on Contact Roles admin endpoints
11. OpenAPI reflects all new endpoints and filters
12. `companies` table row count matches the pre-migration count immediately after migration

---

## Track C3 — Hierarchical NIC Filter + Grouped View

**Goal:** Sales can filter by any NIC node and its descendants, and view results grouped by the hierarchy.

**Scope**
- `GET /companies` filter extension: `nic_code_id`, `nic_parent_id`, `nic_include_descendants`, `view=flat|grouped_by_nic`
- Recursive CTE for descendant expansion
- Grouped-view response shape (nodes + counts, companies lazy-loaded)
- Companies List filter panel: NIC picker with primary-first sort, "include descendants" toggle, view toggle
- `GroupedNicList` component
- Lazy-load per group on expand

**Acceptance criteria**
1. NIC parent filter with `include_descendants=true` returns the node **and all descendants** verified to exact counts against seed data
2. NIC filter with `include_descendants=false` returns companies tagged to that exact node only
3. Grouped view returns tree + counts in <500ms for a subtree of 200 nodes at 100k envelope
4. Expanding a group triggers a paginated `GET /companies?nic_code_id=…` — initial payload does not include company lists
5. "Directly tagged to parent (no sub-code)" bucket appears with correct count
6. A company with 5 NIC codes across 3 branches renders in every matching group (grouped view) and once (flat view)
7. Region, district, pincode, turnover-range, employee-range, GST-present, has-contact-role filters all return correct sets
8. Filter combinations audit: NIC + turnover + region returns intersection, not union

---

## Track C4 — Location Intelligence

**Goal:** Pincode search shows owned companies on a Google Map and live external results in a separate tab; add-to-system routes through manual entry.

**Scope**
- `location/` module: `PlacesLookupService`, `CompanyMapService`, `PincodeCentroidService`
- Pincode centroid reference table + seed migration (from India Post PIN data)
- Endpoints: `/map/pincode/{pincode}`, `/map/companies`, `/external/places-search`
- Daily quota counter + Admin settings entries
- Google Maps JavaScript integration on the frontend (restricted key)
- Company Map screen with Owned/External tabs
- `+ Add` wired to existing manual-entry endpoint with `source=GOOGLE_PLACES`

**Acceptance criteria**
1. Places search writes **zero rows** to `companies` or any other table (integration-asserted)
2. Quota exceeded returns 429/422 with clear problem+json `detail`; UI surfaces it verbatim
3. Provider unavailable returns 502 problem+json — never a silently empty list
4. External results visually distinct from owned records in rendered UI (dashed border, amber pins)
5. `+ Add` produces a batch-of-one with `source=GOOGLE_PLACES`, runs normalization, is caught by existing dedup if the company already exists in that pincode
6. `GOOGLE_PLACES_API_KEY` never appears in any frontend bundle or network response
7. Maps JavaScript key is origin-restricted at Google Cloud Console
8. Map renders <2s for a pincode with ≤200 owned companies
9. Pincode centroid lookup is offline (no Google call) — from the seeded reference table

---

## Track C5 — Companies List Download

**Goal:** Any user who can read the list can download it as CSV/XLSX in their currently-filtered scope, with no pipeline state change.

**Scope**
- `POST /api/v1/companies/download` endpoint
- Streaming CSV + XLSX response
- Column-selection + one-row-per-company vs one-row-per-contact modes
- `COMPANIES_DOWNLOAD_ROW_CAP` setting
- Download modal on Companies List
- Download-mutation audit (who, when, filter snapshot, row count)

**Acceptance criteria**
1. Download does NOT change any company's `pipeline_state` (integration-asserted)
2. Download does NOT create a row in `exports` (integration-asserted)
3. 10k-row CSV streams within 15s
4. Larger than `COMPANIES_DOWNLOAD_ROW_CAP` returns 422 with "narrow your filter" guidance
5. `include_all_contacts=true` produces one row per (company, contact) pair with role visible
6. `include_all_contacts=false` produces one row per company using primary contact only
7. Audit row written per download
8. UI copy says "Download" everywhere in this flow; the word "Export" never appears in this modal
9. Viewer role can download (read-based) — 403 rules do not apply to reads

---

## Release Gate

Do not mark this extension complete until:

- backend build passes
- frontend build passes
- clean migration from empty database passes
- Kanchipuram Excel imports with exact counts per C2 criterion 4
- NIC parent + sub filtering pass
- multi-NIC and multi-contact primary-flag atomicity pass
- pincode search returns results without persisting them
- Add-to-ProspectSoul passes through existing lineage path
- Download does not change pipeline state
- Authorization tests pass (403 paths)
- Audit tests pass
- Unit + integration tests pass
- OpenAPI accurate
- README updated
- ADR folder contains one file per behavior change made (see /adr)

---

## Scope Control

Do not implement:

- automated NIC re-classification of existing companies
- GST validation against a government registry (regex format only)
- scheduled or recurring pincode sweeps
- persistence or caching of raw external API responses
- Udyam number as native identity field (until second registry source confirms format)
- semantic/AI deduplication
- automated contact-movement detection
- cross-company contact merging (person-entity model — deferred)
- CRM, outreach, or company-relationship functionality

unless an existing dependency makes a minimal implementation necessary, or the PRD Addendum is formally changed.
