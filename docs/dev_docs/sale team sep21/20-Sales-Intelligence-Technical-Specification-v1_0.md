<!--
Document: 20-Sales-Intelligence-Technical-Specification-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Approved for Implementation
Audience: Developers / Claude Code
Companion to: Sales Intelligence Requirements v1.0 (doc 19), Technical Design Spec v1.0
-->
# ProspectSoul — Sales Intelligence Extension: Technical Specification

**Version:** 1.0
**Status:** Implementation contract for this scope

Doc 19 defines *what*; this defines *how*. Conventions from Technical Design Spec v1.0 §2 apply unchanged (base path `/api/v1`, UUIDs, snake_case JSON, RFC-7807 errors, `AuditService.record(...)` on every mutation, paginated envelope).

---

## 1. Architecture

Preserve the existing ProspectSoul modular-monolith architecture:
- Spring Boot backend, feature-first packages
- React frontend with existing app shell
- PostgreSQL with Flyway migrations
- Keycloak / OIDC
- Existing object-storage abstraction

Two new backend packages, two extended:

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
  company/                          -- EXTEND (fields, filters, download,
                                       primary_nic maintenance, ContactRoleController)
  contact/                          -- EXTEND (role field, backfill)
  imports/                          -- EXTEND (UDYAM_MSME_REGISTRY source template,
                                       Activities JSON parser, alias registry)
```

If existing package names differ, use them — do not restructure unrelated modules. Controllers must not accept or return JPA entities. Use DTO-first request/response.

---

## 2. Database — Additive Migrations

**Every migration is new. Never modify an applied migration.**
Migration version numbers below are placeholders — use the next available in the repository.

### 2.1 `V<next>__nic_master.sql`

```sql
CREATE TABLE nic_codes (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nic_data_id     INTEGER,                            -- source row id, for re-import traceability
  code            VARCHAR(6) NOT NULL UNIQUE,
  description     TEXT NOT NULL,
  industry_type   VARCHAR(20) NOT NULL,               -- 'Service' | 'Manufacturing'
  level           SMALLINT NOT NULL,                  -- 1..5, derived from code length
  parent_id       UUID REFERENCES nic_codes(id),
  is_primary      BOOLEAN NOT NULL DEFAULT false,     -- surfaces in filter pickers first
  active          BOOLEAN NOT NULL DEFAULT true,      -- deactivate; never hard-delete
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

### 2.2 `V<next+1>__company_sales_fields.sql`

```sql
-- companies table NOT recreated. Additive only.
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

-- Extend import_source enum
ALTER TYPE import_source ADD VALUE 'GOOGLE_PLACES';
ALTER TYPE import_source ADD VALUE 'UDYAM_MSME_REGISTRY';
```

### 2.3 `V<next+2>__contact_roles.sql`

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
  ADD COLUMN role_id UUID REFERENCES contact_roles(id);

CREATE INDEX idx_contact_role ON contacts(role_id);

-- Seed starter roles
INSERT INTO contact_roles (key, label, sort_order) VALUES
  ('MD_OWNER',      'MD / Owner',     10),
  ('DIRECTOR',      'Director',       20),
  ('CEO',           'CEO',            30),
  ('COO',           'COO',            40),
  ('CFO',           'CFO',            50),
  ('HR_HEAD',       'HR Head',        60),
  ('PURCHASE_HEAD', 'Purchase Head',  70),
  ('SALES_HEAD',    'Sales Head',     80),
  ('PLANT_HEAD',    'Plant Head',     90),
  ('ACCOUNTS',      'Accounts',      100),
  ('ADMIN',         'Admin',         110),
  ('IT',            'IT',            120),
  ('OTHER',         'Other',         999);

-- Backfill from existing is_md_owner
UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'MD_OWNER')
  WHERE is_md_owner = true AND role_id IS NULL;

UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'OTHER')
  WHERE role_id IS NULL;
```

`is_md_owner` is retained (nullable) for backward compatibility. Service layer keeps it in sync when role is set to `MD_OWNER`.

### 2.4 `V<next+3>__company_nic_codes.sql`

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

-- Enforce Invariant 12 at DB level: at most one primary NIC per company
CREATE UNIQUE INDEX idx_cnc_one_primary_per_company
  ON company_nic_codes(company_id) WHERE is_primary = true;
```

---

## 3. NIC Master

### 3.1 Endpoints

```text
GET    /api/v1/nic-codes                              # list, paginated; q, level, industry_type, is_primary, active filters
GET    /api/v1/nic-codes/{id}
GET    /api/v1/nic-codes/{id}/children                # direct children
GET    /api/v1/nic-codes/tree?root_id=&depth=         # subtree, cached
GET    /api/v1/nic-codes/primary                      # flat list, primary-flagged only
POST   /api/v1/nic-codes                              # manual create — PS_ADMIN
PATCH  /api/v1/nic-codes/{id}                         # edit description/parent/is_primary/active — PS_ADMIN
POST   /api/v1/nic-codes/{id}/toggle-primary          # convenience — PS_ADMIN
POST   /api/v1/admin/nic-codes/import                 # multipart Excel — PS_ADMIN
                                                      # → { rows_read, created, updated, unresolved_parents }
```

Hard-delete is not exposed.

### 3.2 Parent resolution algorithm (import)

1. Parse each row: `code`, `description`, `industry_type`, `nic_data_id`
2. `level` = length of the numeric code (1–5)
3. Sort by `level` ascending — parents exist before children look them up
4. For each code, find the **longest existing code that is a prefix** of the current code. That row's `id` becomes `parent_id`. No match → `parent_id` NULL (root)
5. **Upsert by `code`** — idempotent. Existing references (`companies.primary_nic_code_id`, `company_nic_codes.nic_code_id`) survive because they use stable UUIDs
6. Report `unresolved_parents` (expected 0 on a clean master file)

### 3.3 Manual create/edit rules

- `code` must be numeric, 1–5 digits, unique
- `parent_id` must reference an existing code that is a prefix of this code (validated server-side)
- Deactivating a code referenced by any `company_nic_codes` row returns 422 with the affected companies listed; Admin can force with a query flag (reference remains, picker hides the code)

---

## 4. Company Field Extension

### 4.1 Company filter API (extended)

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
  &has_contact_role=<role_key>       # e.g. companies with an MD_OWNER contact
  &view=flat|grouped_by_nic          # default flat
  # existing q, state, city, cluster, tier, source, verification, stale, tag filters unchanged
```

### 4.2 Recursive CTE for descendant expansion

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

### 4.3 Grouped-by-NIC response shape

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

Groups are lazy-loaded — `GET /companies?nic_code_id=<uuid>&page=…` on expand.

---

## 5. Contacts — Multi with Roles

### 5.1 Endpoints

```text
GET    /api/v1/contact-roles                         # list, paginated
POST   /api/v1/contact-roles                         # PS_ADMIN
PATCH  /api/v1/contact-roles/{id}                    # PS_ADMIN — label/sort/active (deactivate never delete)

GET    /api/v1/companies/{id}/contacts               # existing, now returns role
POST   /api/v1/companies/{id}/contacts               # role_id required for new
PATCH  /api/v1/contacts/{id}                         # can change role, primary, fields
POST   /api/v1/contacts/{id}/make-primary            # atomic: demotes previous primary
```

### 5.2 Service-level invariants

- On create: if role is `MD_OWNER`, also set `is_md_owner = true`
- On update: keep `is_md_owner` and role consistent
- At most one `is_primary = true` per company; setting a new one demotes the old in a single transaction
- At most one `is_primary = true` per company for NIC codes; same rule (also enforced by partial unique index at DB level)

---

## 6. Multi-NIC per Company

### 6.1 Endpoints

```text
GET    /api/v1/companies/{id}/nic-codes                       # ordered by sequence_no
POST   /api/v1/companies/{id}/nic-codes                       # { code | nic_code_id }
POST   /api/v1/companies/{id}/nic-codes/{cnc_id}/make-primary
DELETE /api/v1/companies/{id}/nic-codes/{cnc_id}              # audited
```

### 6.2 Setting a new primary NIC (atomic)

1. Update all rows for this company: `is_primary = false`
2. Update selected row: `is_primary = true`
3. Update `companies.primary_nic_code_id` to the resolved `nic_code_id` (may be NULL if raw unmatched)
4. Audit record

---

## 7. Location Intelligence

### 7.1 Endpoints

```text
GET /api/v1/map/pincode/{pincode}
  → { pincode, centroid: { lat, lng }, area_name, state, district }
  # backed by PincodeCentroidService — an offline pincode reference table
  # (seeded from India Post PIN data). NOT a live Google call.

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
  # NOTHING WRITTEN to any table
  # 502 problem+json (type: upstream_unavailable) if Places down
  # 429/422 problem+json if quota exceeded — surface `detail` verbatim
```

### 7.2 Keys and quota

- `GOOGLE_PLACES_API_KEY` — server-side only; never in frontend bundle
- `MAPS_JS_API_KEY` — frontend; origin-restricted at Google Cloud Console
- `PLACES_DAILY_QUOTA` — admin-configurable; same counter pattern as `AI_DAILY_CAP`
- `PLACES_SEARCH_RADIUS_M` — default radius per pincode search

### 7.3 "+ Add" — one door in

Clicking "+ Add" on an external result opens `/companies/new` pre-filled from the Places payload. Submits through the existing `POST /api/v1/companies` path:
- batch-of-one lineage
- normalization
- deterministic dedup (existing rules ①-③, plus new ⓪ from §9)
- audit

Import source set to `GOOGLE_PLACES`.

---

## 8. Companies List Download

### 8.1 New endpoint (distinct from pipeline Export)

```text
POST /api/v1/companies/download
  Body: {
    filter: { …current list filter… },
    format: "csv" | "xlsx",
    columns: [ "canonical_name", "primary_phone", "primary_contact_role", … ],
    include_all_contacts: false
  }
  → 200 file stream + Content-Disposition: attachment
  # DOES NOT change pipeline state
  # DOES NOT create an exports row
  # DOES audit: who, when, filter snapshot, row count
```

- Available to any role that can read the list (Viewer included)
- `COMPANIES_DOWNLOAD_ROW_CAP` setting; over-cap returns 422 with narrow-filter guidance
- `include_all_contacts=true` produces one row per (company, contact) pair; otherwise one row per company using primary contact

### 8.2 Existing pipeline Export

```text
POST /api/v1/exports                       # UNCHANGED
                                           # Sales Lead+ only
                                           # State → EXPORTED, logged
```

---

## 9. Kanchipuram / Registry Import

### 9.1 Header alias registry (add to the existing registry)

| Target field | Aliases |
|---|---|
| `canonical_name` | Enterprise Name, EnterpriseName, Company, Company Name, Firm, Business Name, Legal Name |
| `pincode` | Pincode, PIN Code, PIN, Postal Code, Zip, Zip Code |
| `district` | District, Dist, District Name |
| `state` | State, State Name, Province, Region |
| `address_line` | Address, Communication Address, CommunicationAddress, Full Address, Registered Address |
| `region` | Region, Zone, Territory, Sales Region |
| `products` | Products, Product, Product Range, Items, Product Line |
| `turnover` | Turnover, Revenue, Annual Turnover, Sales Turnover |
| `gst_number` | GST, GSTIN, GST No, GST Number, GST Registration |
| `employee_count` | Employees, Employee Count, Headcount, No of Employees, Staff Strength |
| `registration_date` | Registration Date, RegistrationDate, Reg Date, Date of Registration, Registered On |
| `lg_state_code` | LG_ST_Code, LG State Code |
| `lg_district_code` | LG_DT_Code, LG District Code |
| activities JSON | Activities, NIC Activities, NIC Codes |
| contact role | Role, Designation Type, Contact Type, Position Type |

Aliases must be extensible / configurable.

### 9.2 Activities JSON parser

- Parse the string as JSON
- If `"NA"` or empty → 0 activities, row still imported
- Parse failure → row still persisted, `outcome_reason = 'activities_json_invalid'`, batch does not fail
- For each element: `NIC5DigitId` → `nic_code_raw`, `Description` → `description_raw`, index → `sequence_no`
- First element → `is_primary = true`

### 9.3 Source reference (dedup rule ⓪)

For Udyam imports, `source_reference` is composed as `LG_ST_Code-LG_DT_Code-pincode-<hash(name+regdate)>`.

**Extended dedup order:**
```
⓪  source + source_reference   (new, for registry sources)
①  normalized phone            (existing)
②  website domain              (existing)
③  normalized name + city      (existing, tightened for registry sources
                                to normalized name + pincode + address similarity)
```

Rule ⓪ prevents re-import of the same Kanchipuram file from generating tens of thousands of false candidates.

### 9.4 Registration date parsing

The sample column `RegistrationDate` is a string in `dd/MM/yyyy`. Parse explicitly with that format — do not rely on Excel date auto-detection (some rows arrive as strings, others as dates).

### 9.5 Handling the 7-column sample from doc 19 §3

Every column maps successfully via §9.1 aliases. `CommunicationAddress` (absent) → `address_line = NULL`. `Activities` (absent) → zero rows in `company_nic_codes`. Row is created in state `TRIAGE` with the seven fields populated. No error, no rejection.

---

## 10. Frontend Additions

Use the existing project stack (React, TypeScript, existing component library), existing app shell (Header / Sidebar / Footer), existing routing.

### 10.1 Navigation additions

```
Sidebar:
  Companies                 (existing, extended filters + Download button)
     └ Map                  ← NEW  /companies/map
  Settings                  (Admin only)
     ├ NIC Codes            ← NEW  /settings/nic-codes
     └ Contact Roles        ← NEW  /settings/contact-roles
```

### 10.2 Screens

**`/settings/nic-codes`** — NIC master, two views (Table default, Tree). Table columns: Code, Description, Level, Type, Parent, Primary flag. Add/Edit dialog validates parent is a prefix of code. Import wizard: Upload → Preview (rows read / created / updated / unresolved parents) → Confirm. Re-import is idempotent.

**`/settings/contact-roles`** — Sort, Label, Active, Used-by count. Add / rename / activate / deactivate. Deactivated roles hidden for new contacts, still render on existing.

**`/companies` — extended** — Filter panel gains: NIC (with "include descendants" toggle), Region, District, Pincode, Turnover range, Employees range, GST presence, "has contact role" multi-select. View toggle: Flat / Grouped by NIC. Grouped view lazy-loads per group. New default columns: Primary NIC, Contact (Primary) with role, Region, Pincode. Download button top-right → Download modal.

**`/companies/map`** — Pincode search → map centered on pincode. Tabs: In ProspectSoul (owned) / Found on Google (external). Owned pins: solid teal; External pins: dashed amber. External list has "+ Add" per result → opens `/companies/new` pre-filled. Quota indicator persistent under external tab.

**`/companies/{id}` — Contacts tab** — Multi-contact cards: name, role badge, primary star (★), contact details, Edit / Make primary / Re-link buttons. "+ Add contact" opens dialog with role picker (required).

**`/companies/{id}` — Overview tab (NIC section)** — List of attached NIC codes with primary star. Attach code / Make primary / Detach controls.

**Download modal** — Format (CSV / Excel), column selection, "All contacts" toggle, filter-match count, explicit note that this is not a pipeline Export.

### 10.3 New shared components

| Component | Where used | Behavior |
|---|---|---|
| `NicPicker` | Filters, Company edit, Import mapping | Tree-typeahead; primary-flagged first; level chip; "include descendants" toggle |
| `NicBadge` | List, Detail | Code + short description; full path on hover |
| `ContactRoleBadge` | Contact card, List column | Compact tag |
| `ContactRolePicker` | Contact add/edit | Sorted by `sort_order`; deactivated hidden for new, visible for existing |
| `PincodeSearchBox` | Map | Debounced; validates 6-digit before firing |
| `ExternalResultCard` | Map | Dashed border, distinct accent, always "not yet in ProspectSoul" label |
| `MapPin` | Map | Two variants: solid teal (owned) / dashed amber (external) |
| `GroupedNicList` | List grouped view | Collapsible tree of groups, lazy-loaded on expand |
| `DownloadModal` | List | Column selection, format, filter-match count |

---

## 11. Configuration Additions

| Setting | Purpose |
|---|---|
| `GOOGLE_PLACES_API_KEY` | Server-side Places calls |
| `MAPS_JS_API_KEY` | Frontend map rendering; origin-restricted |
| `PLACES_DAILY_QUOTA` | Admin-configurable; 429/422 with detail when exceeded |
| `PLACES_SEARCH_RADIUS_M` | Default radius per pincode search |
| `COMPANIES_DOWNLOAD_ROW_CAP` | Hard cap per download; larger returns 422 |

---

## 12. Testing Baseline

### Unit
- NIC level detection (1–5 digits) and longest-prefix parent resolution — including skipped-level branches
- Idempotent NIC re-import by `code`
- Recursive descendant expansion
- Activities JSON parser: valid array, single element, `"NA"`, empty array, malformed
- Registration date parser: `dd/MM/yyyy` string vs Excel date variants
- Contact role backfill: `is_md_owner=true` → `role='MD_OWNER'`
- GST format validation (regex)
- Pincode format validation (6-digit)
- Atomic primary-flag transitions (NIC and contact)
- Header alias resolution for all new aliases
- Dedup rule ⓪ (`source + source_reference`) before rules ①-③

### Integration (Testcontainers)
- The 7-column sample from doc 19 §3: all rows import, all 7 fields populated, address/activities empty, no error
- Real Kanchipuram Excel import: 123,653 created / 6 rejected / 10 flagged / 175,861 join rows / 0 unresolved parents
- Company with unknown NIC codes: raw kept, `nic_code_id` NULL, no rejection
- Recursive NIC filter against seed: exact counts
- Places search → assert zero DB writes
- "+ Add" → batch-of-one with `source=GOOGLE_PLACES`, dedup applied
- Companies download → CSV/XLSX byte content matches filter, pipeline state unchanged, no `exports` row
- 403 paths: Viewer on mutations, non-Admin on `/admin/*`, non-Sales Lead on pipeline Export
- Audit rows written on every mutation

### AI
- No change to existing AI capabilities or MockAiProvider

---

## 13. Non-Functional Additions

- **Companies List grouped view:** subtree count query <500ms for a subtree of 200 nodes at the 100k envelope
- **Company Map:** renders <2s for a pincode with ≤200 owned companies
- **Companies List Download:** 10k-row CSV streams within 15s; larger returns 422
- **NIC master tree endpoint:** cached in-memory, invalidated on mutation
- **Pincode centroid lookup:** offline (no Google call) — seeded reference table
