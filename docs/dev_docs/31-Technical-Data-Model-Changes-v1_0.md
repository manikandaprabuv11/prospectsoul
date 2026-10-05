<!--
Document: 31-Technical-Data-Model-Changes-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Database schema changes for Mobile Number Confidence, Multi-Number, GST
Audience: Engineering, Claude Code
Depends on: 29-PRD-Change-Request-v1_2.md, 30-Functional-Requirements-v1_0.md
-->
# ProspectSoul — Technical / Data Model Changes

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. New Enums

### 1.1 number_source_type

```sql
CREATE TYPE number_source_type AS ENUM (
  'BUSINESS_CARD',
  'FIELD_VISIT',
  'REFERENCE',
  'MANUAL_ENTRY',
  'WEBSITE',
  'GOOGLE_API',
  'LINKEDIN',
  'INDIAMART',
  'IMPORT_DEFAULT'
);
```

### 1.2 confidence_level

```sql
CREATE TYPE confidence_level AS ENUM ('HIGH', 'MEDIUM', 'LOW');
```

### 1.3 confidence_mode

```sql
CREATE TYPE confidence_mode AS ENUM ('AUTO', 'MANUAL');
```

### 1.4 phone_type

```sql
CREATE TYPE phone_type AS ENUM ('MOBILE', 'LANDLINE', 'INVALID');
```

---

## 2. New Table: company_phones

```sql
CREATE TABLE company_phones (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id           UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  contact_id           UUID REFERENCES contacts(id) ON DELETE SET NULL,
  number_raw           TEXT NOT NULL,
  number_normalized    VARCHAR(10),
  phone_type           phone_type NOT NULL DEFAULT 'MOBILE',
  number_source        number_source_type NOT NULL,
  confidence           confidence_level NOT NULL DEFAULT 'MEDIUM',
  confidence_mode      confidence_mode NOT NULL DEFAULT 'AUTO',
  designation_override TEXT,
  is_primary           BOOLEAN NOT NULL DEFAULT FALSE,
  override_reason      TEXT,
  batch_id             UUID REFERENCES import_batches(id) ON DELETE SET NULL,
  import_row_id        UUID REFERENCES import_rows(id) ON DELETE SET NULL,
  created_by           UUID,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by           UUID,
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.1 Indexes

```sql
-- Lookup by company
CREATE INDEX idx_company_phones_company_id ON company_phones(company_id);

-- Dedup: find all companies with a given normalized number
CREATE INDEX idx_company_phones_normalized ON company_phones(number_normalized)
  WHERE number_normalized IS NOT NULL;

-- Lookup by contact
CREATE INDEX idx_company_phones_contact_id ON company_phones(contact_id)
  WHERE contact_id IS NOT NULL;

-- Primary number per company (partial unique — at most one primary per company)
CREATE UNIQUE INDEX idx_company_phones_primary ON company_phones(company_id)
  WHERE is_primary = TRUE;

-- Batch lineage
CREATE INDEX idx_company_phones_batch_id ON company_phones(batch_id)
  WHERE batch_id IS NOT NULL;
```

### 2.2 Constraints

- `number_normalized` must be exactly 10 digits when not NULL (CHECK constraint).
- `override_reason` is required when `confidence_mode = 'MANUAL'` (enforced in application layer, not DB constraint, to allow migration flexibility).
- `designation_override` is mutually preferred with `contact_id` — if `contact_id` is set, the contact's designation takes precedence over `designation_override` at the application layer.

### 2.3 Trigger

```sql
-- Auto-update updated_at
CREATE TRIGGER trg_company_phones_updated
  BEFORE UPDATE ON company_phones
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
```

---

## 3. New Reference Table: decision_maker_designations

```sql
CREATE TABLE decision_maker_designations (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  designation TEXT NOT NULL UNIQUE,
  aliases     TEXT[] NOT NULL DEFAULT '{}',
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Seed data:

| designation | aliases |
|---|---|
| MD | {Managing Director, M.D., Mng Director} |
| CEO | {Chief Executive Officer, C.E.O.} |
| COO | {Chief Operating Officer, C.O.O.} |
| Owner | {Proprietor, Prop., Business Owner} |
| Director | {Dir.} |
| Partner | {} |
| Founder | {Co-Founder, Co Founder} |
| Chairman | {Chairperson, Chair} |
| General Manager | {GM, G.M.} |
| Plant Manager | {Factory Manager, Works Manager} |
| Purchase Manager | {Procurement Manager, Buying Manager} |
| Production Manager | {Manufacturing Manager, Prod. Manager} |

---

## 4. Modified Table: companies

### 4.1 New column

```sql
ALTER TABLE companies ADD COLUMN gst_number VARCHAR(15);
```

### 4.2 GST validation constraint

```sql
ALTER TABLE companies ADD CONSTRAINT chk_gst_format
  CHECK (gst_number IS NULL OR gst_number ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$');
```

### 4.3 primary_phone_normalized — kept as denormalized

`companies.primary_phone_normalized` is retained. It is updated by a service-layer method `CompanyPhoneService.recomputePrimary(companyId)` whenever:
- A phone is added, removed, or updated
- A phone's confidence changes
- A phone's primary flag changes
- A linked contact's designation changes

This preserves backward compatibility with existing dedup logic, search, and the companies list query.

---

## 5. Modified Table: contacts

### 5.1 phones[] deprecation path

The existing `contacts.phones[]` array is NOT removed in this migration to avoid breaking existing code. Instead:
- New phone numbers are stored in `company_phones` with a `contact_id` reference.
- A backfill migration copies existing `contacts.phones[]` entries into `company_phones`.
- After backfill verification, `contacts.phones[]` is marked deprecated in the entity/DTO (excluded from API responses, ignored on input).
- A future migration drops the column.

---

## 6. Entity / DTO Changes

### 6.1 New Entity: CompanyPhone

```java
@Entity
@Table(name = "company_phones")
public class CompanyPhone {
    @Id private UUID id;
    @ManyToOne(fetch = LAZY) private Company company;
    @ManyToOne(fetch = LAZY) private Contact contact;
    private String numberRaw;
    private String numberNormalized;
    @Enumerated(STRING) private PhoneType phoneType;
    @Enumerated(STRING) private NumberSourceType numberSource;
    @Enumerated(STRING) private ConfidenceLevel confidence;
    @Enumerated(STRING) private ConfidenceMode confidenceMode;
    private String designationOverride;
    private Boolean isPrimary;
    private String overrideReason;
    @ManyToOne(fetch = LAZY) private ImportBatch batch;
    @ManyToOne(fetch = LAZY) private ImportRow importRow;
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;
}
```

### 6.2 New DTOs

```java
// Request
public record CompanyPhoneRequest(
    String numberRaw,
    NumberSourceType numberSource,
    ConfidenceLevel confidence,     // only honored when numberSource = MANUAL_ENTRY
    UUID contactId,                  // nullable
    String designationOverride,      // nullable
    Boolean isPrimary
) {}

// Response
public record CompanyPhoneResponse(
    UUID id,
    String numberRaw,
    String numberNormalized,
    PhoneType phoneType,
    NumberSourceType numberSource,
    ConfidenceLevel confidence,
    ConfidenceMode confidenceMode,
    String designation,              // resolved: contact.designation or designationOverride
    Boolean isDecisionMaker,
    Boolean isPrimary,
    UUID contactId,
    String contactName,
    Instant createdAt
) {}
```

### 6.3 Modified: CompanyCreateRequest

Add:
```java
List<CompanyPhoneRequest> phones;  // replaces the single primaryPhone field
String gstNumber;                   // new
```

### 6.4 Modified: CompanyUpdateRequest (PATCH)

Add:
```java
List<CompanyPhoneRequest> phones;  // full replacement of phone list on PATCH
String gstNumber;
```

### 6.5 Modified: CompanyListResponse (list item)

Add:
```java
String primaryPhone;                // denormalized, same as before
ConfidenceLevel primaryPhoneConfidence;
String primaryPhoneDesignation;
Integer additionalPhoneCount;
```

### 6.6 Modified: CompanyDetailResponse

Add:
```java
List<CompanyPhoneResponse> phones;
String gstNumber;
```

---

## 7. Service Layer Changes

### 7.1 ConfidenceEngine

New service: `ConfidenceEngine`

```java
public class ConfidenceEngine {
    /**
     * Compute confidence for a phone number based on source and designation.
     * Rules:
     * 1. If confidence_mode is MANUAL, return existing confidence (never auto-override manual).
     * 2. Look up designation against decision_maker_designations.
     * 3. If decision-maker AND source != INDIAMART → HIGH
     * 4. If decision-maker AND source == INDIAMART → MEDIUM
     * 5. Otherwise, apply source default (FR-1.2 table)
     * 6. Apply never-downgrade: if existing confidence > computed, keep existing
     */
    public ConfidenceLevel compute(NumberSourceType source, String designation, 
                                    ConfidenceLevel existingConfidence,
                                    ConfidenceMode existingMode);
}
```

### 7.2 CompanyPhoneService

New service handling all phone CRUD:
- `addPhone(companyId, CompanyPhoneRequest)` — normalize, classify, compute confidence, check dedup, persist
- `updatePhone(phoneId, CompanyPhoneRequest)` — re-compute confidence if source/contact changed
- `removePhone(phoneId)` — soft considerations (audit), recompute primary
- `recomputePrimary(companyId)` — select best number, update `companies.primary_phone_normalized`
- `recomputeConfidence(phoneId)` — triggered when linked contact's designation changes

### 7.3 NormalizationService — Extended

Add to existing phone normalization:
- After normalizing, classify as MOBILE (starts with 9/8/7/6), LANDLINE, or INVALID.
- Return a `PhoneNormalizationResult` with `normalized`, `phoneType`, `isValid`.

### 7.4 DuplicateDetectionService — Extended

Dedup rule ① now queries `company_phones.number_normalized` instead of (or in addition to) `companies.primary_phone_normalized`. Return value includes `matchedPhoneId` and `matchedCompanyId`.

---

## 8. Import Column Mapping — Extensions

### 8.1 New target fields for import mapping

| Target Field | Aliases |
|---|---|
| number_source | Number Source, Phone Source, Mobile Source, Source of Number |
| designation | Designation, Title, Position, Role, Contact Title, Person Title |
| gst_number | GST, GSTIN, GST Number, GST No, GST No., Tax ID, Tax Number |
| alternate_phone_1 | Alternate Phone, Alt Phone, Phone 2, Mobile 2, Second Phone, Other Phone |
| alternate_phone_2 | Phone 3, Mobile 3, Third Phone |
| owner_phone | Owner Phone, Owner Mobile, MD Phone, MD Mobile, Director Phone |

### 8.2 Multi-phone column handling

When an import row maps to multiple phone columns (primary_phone + alternate_phone_1 + owner_phone), each creates a separate `company_phones` record:
- `primary_phone` → is_primary=true, number_source from row or IMPORT_DEFAULT
- `alternate_phone_1` → is_primary=false, number_source from row or IMPORT_DEFAULT
- `owner_phone` → is_primary=false, number_source from row or IMPORT_DEFAULT, designation_override="Owner"

---

## 9. Relationship Diagram

```
companies
  ├── company_phones[] ──→ contacts (optional)
  │     ├── number_source (enum)
  │     ├── confidence (enum)
  │     ├── phone_type (enum)
  │     └── batch/row lineage
  ├── gst_number
  ├── primary_phone_normalized (denormalized from best company_phone)
  └── (existing fields unchanged)

decision_maker_designations (reference/config table)
  └── used by ConfidenceEngine to resolve designation → is_decision_maker
```
