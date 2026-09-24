<!--
Document: 21-Sales-Intelligence-Technical-Design-Addendum-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Proposed — implementation contract for this scope
Audience: Developers / Claude Code
Companion to: Technical Design Specification v1.0
-->
# ProspectSoul — Sales Intelligence Extension (Technical Design Addendum)

**Version:** 1.0
The PRD Addendum defines *what*; this defines *how*.

---

## 1. Conventions

Everything from Technical Design Spec §2 applies unchanged:
- Base path `/api/v1`, UUID ids, UTC ISO-8601, snake_case JSON
- RFC-7807 errors (`application/problem+json`)
- Standard paginated envelope
- `AuditService.record(...)` on every mutation

---

## 2. New Backend Modules

Two new sibling packages next to the existing `company/`, `contact/`, `imports/`, `admin/`.

```text
com.vyoog.prospectsoul/
  nic/                              -- NEW
    controller/   NicCodeController, NicImportController
    service/      NicCodeService, NicImportService, NicTreeService
    repository/   NicCodeRepository
    entity/       NicCodeEntity
    dto/          NicCodeDto, NicImportResultDto, NicTreeNodeDto
    mapper/
  location/                         -- NEW
    controller/   MapController, PlacesLookupController
    service/      CompanyMapService, PlacesLookupService, PincodeCentroidService
    dto/          MapCompanyDto, ExternalPlaceDto, PincodeCentroidDto
```

And two existing packages extended:

```text
  company/                          -- EXTEND
    (new fields, primary_nic_code_id maintenance, filter extension,
     ContactRoleController for admin-editable role list)
  contact/                          -- EXTEND
    (role field, role list reference, migration of is_md_owner)
  imports/                          -- EXTEND
    (new source: UDYAM_MSME_REGISTRY, template for Kanchipuram-style files,
     Activities JSON parser)
```

---

## 3. NIC Master

### 3.1 Migration `V<next>__nic_master.sql`

```sql
CREATE TABLE nic_codes (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nic_data_id     INTEGER,                            -- source row id, for re-import traceability
  code            VARCHAR(6) NOT NULL UNIQUE,
  description     TEXT NOT NULL,
  industry_type   VARCHAR(20) NOT NULL,               -- 'Service' | 'Manufacturing'
  level           SMALLINT NOT NULL,                  -- 1..5
  parent_id       UUID REFERENCES nic_codes(id),
  is_primary      BOOLEAN NOT NULL DEFAULT false,     -- surfaces in filter pickers first
  active          BOOLEAN NOT NULL DEFAULT true,      -- deactivate never delete
  created_by      UUID,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by      UUID,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nic_parent   ON nic_codes(parent_id);
CREATE INDEX idx_nic_code     ON nic_codes(code);
CREATE INDEX idx_nic_type     ON nic_codes(industry_type);
CREATE INDEX idx_nic_primary  ON nic_codes(is_primary) WHERE is_primary = true;
```

### 3.2 Endpoints

```text
GET    /api/v1/nic-codes                                # list, paginated, filter by q/level/industry_type/is_primary/active
GET    /api/v1/nic-codes/{id}
GET    /api/v1/nic-codes/{id}/children                  # direct children only
GET    /api/v1/nic-codes/tree?root_id=&depth=           # subtree, cached
GET    /api/v1/nic-codes/primary                        # flat list, primary-flagged only
POST   /api/v1/nic-codes                                # create manual — PS_ADMIN
PATCH  /api/v1/nic-codes/{id}                           # edit description/parent/is_primary/active — PS_ADMIN
POST   /api/v1/nic-codes/{id}/toggle-primary            # convenience — PS_ADMIN
POST   /api/v1/admin/nic-codes/import                   # multipart Excel — PS_ADMIN
                                                        # → { rows_read, created, updated, unresolved_parents }
```

Hard-delete is not exposed. Deactivation only.

### 3.3 Parent Resolution Algorithm (import)

1. Parse each row: `code`, `description`, `industry_type`, `nic_data_id`
2. `level` = length of the numeric code (1..5)
3. Sort by `level` ascending — parents always exist before children look them up
4. For each code, find the **longest existing code that is a prefix** of the current code, among rows already in the table. That row's `id` becomes `parent_id`. No match → `parent_id` NULL (root)
5. **Upsert by `code`** — idempotent. Existing `companies.primary_nic_code_id` and `company_nic_codes.nic_code_id` survive because they reference stable UUIDs, not code strings
6. Report `unresolved_parents` — the count of non-root codes whose parent could not be resolved (expected: 0 on a clean master file)

### 3.4 Manual Create/Edit Rules

- `code` must be numeric, 1–5 digits, unique
- `parent_id` must reference an existing code that is a prefix of this code (validate server-side)
- Deactivating a code that is referenced by any `company_nic_codes` row returns 422 with the list of affected companies (Admin can force-deactivate with a query flag; if forced, the reference remains but the picker hides the code)

---

## 4. Company Field Extension

### 4.1 Migration `V<next+1>__company_sales_fields.sql`

```sql
-- companies table NOT recreated
ALTER TABLE companies
  ADD COLUMN pincode              VARCHAR(6),
  ADD COLUMN district             VARCHAR(120),
  ADD COLUMN address_line         TEXT,
  ADD COLUMN region               VARCHAR(120),
  ADD COLUMN products             TEXT,
  ADD COLUMN turnover             NUMERIC(18,2),
  ADD COLUMN gst_number           VARCHAR(15),
  ADD COLUMN employee_count       INTEGER,
  ADD COLUMN registration_date    DATE,
  ADD COLUMN source_reference     VARCHAR(120),
  ADD COLUMN lg_state_code        SMALLINT,
  ADD COLUMN lg_district_code     INTEGER,
  ADD COLUMN primary_nic_code_id  UUID REFERENCES nic_codes(id);

CREATE INDEX idx_company_nic         ON companies(primary_nic_code_id);
CREATE INDEX idx_company_pincode     ON companies(pincode);
CREATE INDEX idx_company_district    ON companies(district);
CREATE INDEX idx_company_region      ON companies(region);
CREATE INDEX idx_company_turnover    ON companies(turnover);
CREATE INDEX idx_company_source_ref  ON companies(source_reference);

-- Extend the import source enum
ALTER TYPE import_source ADD VALUE 'GOOGLE_PLACES';
ALTER TYPE import_source ADD VALUE 'UDYAM_MSME_REGISTRY';
```

### 4.2 Company API — filter extension

```text
GET /api/v1/companies
  ?nic_code_id=<uuid>                # exact node
  &nic_parent_id=<uuid>              # this node + every descendant (recursive CTE)
  &nic_include_descendants=true      # default when nic_parent_id present
  &region=<string>
  &district=<string>
  &pincode=<string>
  &turnover_min=&turnover_max=
  &employee_min=&employee_max=
  &gst_present=true|false
  &has_contact_role=<role>           # e.g. filter to companies with an MD contact
  &view=flat|grouped_by_nic          # default flat
  # existing q, state, city, cluster, tier, source, verification, stale, tag filters unchanged
```

Recursive CTE for the parent filter:

```sql
WITH RECURSIVE subtree AS (
  SELECT id FROM nic_codes WHERE id = :parent
  UNION ALL
  SELECT n.id FROM nic_codes n JOIN subtree s ON n.parent_id = s.id
)
SELECT DISTINCT c.*
FROM companies c
JOIN company_nic_codes cnc ON cnc.company_id = c.id
WHERE cnc.nic_code_id IN (SELECT id FROM subtree);
```

### 4.3 Grouped-by-NIC response

```json
{
  "view": "grouped_by_nic",
  "root_node": { "id": "…", "code": "22", "description": "Manufacture of rubber…" },
  "groups": [
    { "node": { "code": "221", "description": "Rubber products" }, "count": 18, "companies": [] },
    { "node": { "code": "222", "description": "Plastics products" }, "count": 29, "companies": [] },
    { "node": null, "label": "Directly tagged to 22 (no sub-code)", "count": 0, "companies": [] }
  ],
  "total_companies": 47
}
```

Companies within each group are lazy-loaded via `GET /companies?nic_code_id=<uuid>&page=…`.

---

## 5. Contact Extension

### 5.1 Migration `V<next+2>__contact_roles.sql`

```sql
CREATE TABLE contact_roles (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  key         VARCHAR(40) NOT NULL UNIQUE,   -- 'MD_OWNER', 'HR_HEAD', ...
  label       VARCHAR(80) NOT NULL,          -- 'MD / Owner'
  sort_order  SMALLINT NOT NULL DEFAULT 100,
  active      BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE contacts
  ADD COLUMN role_id  UUID REFERENCES contact_roles(id);

CREATE INDEX idx_contact_role ON contacts(role_id);

-- Seed starter roles
INSERT INTO contact_roles (key, label, sort_order) VALUES
  ('MD_OWNER',     'MD / Owner',     10),
  ('DIRECTOR',     'Director',       20),
  ('CEO',          'CEO',            30),
  ('COO',          'COO',            40),
  ('CFO',          'CFO',            50),
  ('HR_HEAD',      'HR Head',        60),
  ('PURCHASE_HEAD','Purchase Head',  70),
  ('SALES_HEAD',   'Sales Head',     80),
  ('PLANT_HEAD',   'Plant Head',     90),
  ('ACCOUNTS',     'Accounts',      100),
  ('ADMIN',        'Admin',         110),
  ('IT',           'IT',            120),
  ('OTHER',        'Other',         999);

-- Backfill from existing is_md_owner
UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'MD_OWNER')
  WHERE is_md_owner = true AND role_id IS NULL;

UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'OTHER')
  WHERE role_id IS NULL;
```

`is_md_owner` is retained (nullable, backward-compat). Application code reads role via `role_id` going forward; a service-level helper keeps `is_md_owner` in sync when role is set to `MD_OWNER` for readers still using it.

### 5.2 Endpoints

```text
GET    /api/v1/contact-roles                         # list, paginated
POST   /api/v1/contact-roles                         # PS_ADMIN
PATCH  /api/v1/contact-roles/{id}                    # PS_ADMIN — label/sort/active
                                                     # (deactivate; never delete)

GET    /api/v1/companies/{id}/contacts               # existing, now returns role
POST   /api/v1/companies/{id}/contacts               # role_id required
PATCH  /api/v1/contacts/{id}                         # can change role, primary flag, fields
POST   /api/v1/contacts/{id}/make-primary            # atomic: demotes previous primary
```

### 5.3 Invariants enforced in service

- On create: if role is `MD_OWNER`, also set `is_md_owner = true`
- On update: keep `is_md_owner` and role consistent
- At most one `is_primary = true` per company; setting a new one demotes the old atomically (single transaction)

---

## 6. Multi-NIC Join

### 6.1 Migration `V<next+3>__company_nic_codes.sql`

```sql
CREATE TABLE company_nic_codes (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id       UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  nic_code_id      UUID REFERENCES nic_codes(id),      -- NULL when raw code unmatched
  nic_code_raw     VARCHAR(6) NOT NULL,                -- as supplied by source
  description_raw  TEXT,                               -- as supplied
  is_primary       BOOLEAN NOT NULL DEFAULT false,
  sequence_no      SMALLINT NOT NULL,                  -- order in source array
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (company_id, nic_code_raw)
);

CREATE INDEX idx_cnc_company     ON company_nic_codes(company_id);
CREATE INDEX idx_cnc_nic         ON company_nic_codes(nic_code_id);
CREATE UNIQUE INDEX idx_cnc_one_primary_per_company
  ON company_nic_codes(company_id) WHERE is_primary = true;
```

The partial unique index enforces Invariant 12 at the database level — at most one primary NIC per company.

### 6.2 Endpoints

```text
GET   /api/v1/companies/{id}/nic-codes                  # ordered by sequence_no
POST  /api/v1/companies/{id}/nic-codes                  # attach: { code | nic_code_id }
POST  /api/v1/companies/{id}/nic-codes/{cnc_id}/make-primary
DELETE /api/v1/companies/{id}/nic-codes/{cnc_id}        # detach one — audited
```

Setting a new primary atomically:
1. Update all rows for this company: `is_primary = false`
2. Update selected row: `is_primary = true`
3. Update `companies.primary_nic_code_id` to the resolved `nic_code_id` (may be NULL if raw unmatched)
4. Audit record

---

## 7. Location Intelligence (`location/`)

### 7.1 Endpoints

```text
GET /api/v1/map/pincode/{pincode}
  → { pincode, centroid: { lat, lng }, area_name, state, district }
  # backed by PincodeCentroidService — an offline pincode reference table
  # (seeded from India Post PIN data), NOT a live Google call

GET /api/v1/map/companies?pincode=641001&radius_km=5
  → { center: {lat,lng}, content: [ { id, canonical_name, lat, lng, tier,
                                       pipeline_state, primary_nic } ] }
  # owned companies — normal authenticated read

GET /api/v1/external/places-search?pincode=641001&radius_m=3000&keyword=&type=
  → { results: [ { place_id, name, formatted_address, phone, lat, lng,
                   business_status, types[] } ],
      source: "google_places",
      persisted: false,
      quota_remaining: 4382 }
  # nothing written to any table
  # 502 problem+json (type: upstream_unavailable) if Places is down
  # 429/422 problem+json if daily quota exceeded — surface `detail` verbatim
```

### 7.2 Google Maps integration

- **Server-side** for Places API calls (`GOOGLE_PLACES_API_KEY` never in the frontend)
- **Client-side** for map rendering (Maps JavaScript API with a `MAPS_JS_API_KEY` restricted to the ProspectSoul origin via Google Cloud Console)
- Daily quota counter on Places calls, same pattern as `AI_DAILY_CAP`

### 7.3 Add-to-ProspectSoul — one door in

Selecting an external result opens the existing manual-entry form pre-filled from the payload. It submits through the same `POST /api/v1/companies` path already in use:
- batch-of-one lineage
- normalization
- deterministic dedup (existing rules ①–③, plus new ⓪ from ADR-0004 checking `source + source_reference`)
- audit

Import source is set to `GOOGLE_PLACES`.

### 7.4 Implementation Note (2026-09-24) — NIC filter on the map, ANDed with pincode

`GET /api/v1/map/companies` gained two additive optional params, `nic_parent_id` (UUID) and `nic_include_descendants` (boolean) — existing callers without them are unaffected:

```text
GET /api/v1/map/companies?pincode=641001&radius_km=5&nic_parent_id=<uuid>&nic_include_descendants=true
```

`CompanyMapService` resolves the NIC id set via the existing `NicCodeRepository.findDescendantIds` recursive CTE (the same one `CompanySpecification` uses for the Companies List NIC filter), then **intersects** that id set with the pincode/radius match — a company must satisfy both filters, not either. Matching is done through a new `CompanyNicCodeRepository.findCompanyIdsByNicCodeIdIn(Collection<UUID> nicCodeIds)` (JPQL over the `company_nic_codes` join table, primary and secondary codes both count) so the map's NIC semantics stay identical to the list's.

`MapCompanyResponse` also gained:
- `content[].nicCodes` — each company's full, unfiltered NIC list (id, code, description, primary flag) — always returned regardless of filter.
- `matchedNicCodeIds` (top-level `List<UUID>`) — the resolved parent-plus-descendants id set when a NIC filter is active, `null` when it is not. This lets the frontend show only the chips that matched without baking a display decision into the endpoint.

No new endpoint was added for the manual NIC-code lookup on the map page (§5.2 of the UI/UX Addendum) — it reuses the existing `nic` list endpoint with a `q` param and an exact-code match client-side; see the UI/UX Addendum for the frontend contract.

---

## 8. Export — Two Distinct Operations

### 8.1 Existing pipeline Export (unchanged)

```text
POST /api/v1/exports                       # Sales Lead+
                                           # moves companies to state = EXPORTED
                                           # logs to exports table
                                           # existing behavior, do not modify
```

### 8.2 NEW — Companies List Download

```text
POST /api/v1/companies/download
  Body: { filter: {...current list filter...},
          format: "csv" | "xlsx",
          columns: [ "canonical_name", "primary_phone", "primary_contact_role", ... ],
          include_all_contacts: false }
  → 200 with file stream + Content-Disposition: attachment
  # DOES NOT change pipeline state
  # DOES NOT create an exports row
  # DOES audit the download (who, when, filter snapshot, row count)
```

Available to any role that can read the Companies List. Rate-limited per user to prevent abuse. Column list is configurable per-user (frontend state) with a sensible default.

`include_all_contacts=true` produces one row per (company, contact) pair — useful for outreach lists that need every contact, not just primary. Otherwise one row per company using primary contact only.

---

## 9. Kanchipuram-style Registry Import

### 9.1 New template

Header mapping for `UDYAM_MSME_REGISTRY` source:

| Source column | Target field |
|---|---|
| `LG_ST_Code` | `lg_state_code` |
| `State` | `state` |
| `LG_DT_Code` | `lg_district_code` |
| `District` | `district` |
| `Pincode` | `pincode` |
| `RegistrationDate` | `registration_date` (parse dd/MM/yyyy explicitly) |
| `EnterpriseName` | `canonical_name` |
| `CommunicationAddress` | `address_line` |
| `Activities` | parse JSON → many rows in `company_nic_codes` |

### 9.2 Activities parser

- Parse the string as JSON
- If the string is `"NA"` or empty → 0 activities, row still imported
- If parse fails → import row still persisted, `outcome_reason` = `activities_json_invalid`, batch does not fail
- For each element: extract `NIC5DigitId` → `nic_code_raw`, `Description` → `description_raw`, index → `sequence_no`
- First element → `is_primary = true`

### 9.3 Source reference

For Udyam imports, `source_reference` is composed as `LG_ST_Code-LG_DT_Code-pincode-<hash(name+regdate)>` and used as dedup key ⓪ per ADR-0004.

### 9.4 Observed data-quality issues (from Kanchipuram_data.xlsx, 123,659 rows)

| Issue | Rows | Handling |
|---|---|---|
| Malformed Activities JSON (literal `"NA"`) | 10 | Row persisted, flagged |
| Empty activities array | 106 | Company created with zero NIC rows |
| Blank `EnterpriseName` | 6 | Row rejected — missing required name |
| Distinct NIC codes seen | 1,162 | Some may not exist in the master → NULL `nic_code_id`, raw kept |
| Companies with >1 activity | 25,514 (21%) | Handled via join table |
| Max activities on one company | 91 | Handled — no upper limit |

### 9.5 Implementation Note (2026-09-24) — batch completion status bug

Batches that finished with `processed_rows == total_rows` and `created_rows > 0` were sometimes displayed as `FAILED`. Root cause and fix are recorded in `docs/dev_docs/adr/ADR-0007-streaming-registry-imports.md` ("Update — 2026-09-24"): the completion audit call had no transaction to join under `processBatch`'s intentionally non-transactional design, threw, and the async catch block regressed the just-set `COMPLETED` status. Fixed with a `REQUIRES_NEW` `recordBatchCompletion(...)` helper (called via `self`) plus a `markBatchFailed(...)` guard that never overwrites a terminal (`COMPLETED`/`FAILED`) status.

---

## 10. Configuration Additions

| Setting | Purpose |
|---|---|
| `GOOGLE_PLACES_API_KEY` | Server-side only |
| `MAPS_JS_API_KEY` | Frontend only; restricted to origin at Google Cloud Console |
| `PLACES_DAILY_QUOTA` | Admin-configurable, 429/422 with clear detail when exceeded |
| `PLACES_SEARCH_RADIUS_M` | Default radius per pincode search |
| `COMPANIES_DOWNLOAD_ROW_CAP` | Hard cap per download; larger requests return 422 asking user to narrow filter |

---

## 11. Testing Baseline

### Unit
- NIC level detection (1–5 digits)
- Longest-prefix parent resolution, including skipped-level branches
- Idempotent re-import by code
- Recursive descendant expansion for `nic_parent_id`
- Activities JSON parser: valid array, single-element, `"NA"`, empty, malformed
- Contact role migration: `is_md_owner=true` → `role='MD_OWNER'`
- GST format validation (regex)
- Pincode format validation (6-digit)
- Atomic primary-flag updates for both NIC and contact

### Integration (Testcontainers)
- Import the real Kanchipuram Excel → zero unresolved parents, 6 rejected (blank names), 10 flagged (bad JSON)
- Company import with multiple NIC codes → join rows created, first is primary, denormalized
- Recursive NIC filter against seed data → exact counts
- Places search → **assert zero rows written to `companies`**
- Add-to-ProspectSoul → batch-of-one with `source=GOOGLE_PLACES`, dedup applied
- Companies download → CSV/XLSX byte content matches filter, does NOT change pipeline state
- Non-admin on NIC/role admin endpoints → 403
- Viewer on downloads → 403

### AI
- No changes; existing MockAiProvider baseline unchanged

---

## 12. Non-Functional Additions

- **Companies List with grouped-by-NIC view**: subtree count query returns in <500ms for a subtree of 200 nodes on the 100k-envelope
- **Map view**: renders <2s for a pincode with ≤200 owned companies
- **Download**: 10k-row CSV streams within 15s; larger returns 422 with narrow-filter guidance
- **NIC master**: full-tree endpoint cached in-memory, invalidated on any mutation
