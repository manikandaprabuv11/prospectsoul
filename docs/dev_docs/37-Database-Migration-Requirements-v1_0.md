<!--
Document: 37-Database-Migration-Requirements-v1_0.md
Project: ProspectSoul
Version: 1.0
Status: Ready for Implementation
Scope: Flyway migration scripts for v1.2 schema changes
Audience: Engineering, Claude Code
Depends on: 29, 31
-->
# ProspectSoul — Database Migration Requirements

**Version:** 1.0
**Date:** 30 September 2026

---

## 1. Migration Strategy

All changes are delivered as Flyway SQL migration files. Existing migrations are NEVER modified. New migrations are additive and non-destructive. The migration must be reversible (a rollback script is provided but not applied automatically).

---

## 2. Migration: V__phone_confidence_enums.sql

Creates the new enum types.

```sql
-- Phone-related enums
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

CREATE TYPE confidence_level AS ENUM ('HIGH', 'MEDIUM', 'LOW');

CREATE TYPE confidence_mode AS ENUM ('AUTO', 'MANUAL');

CREATE TYPE phone_type AS ENUM ('MOBILE', 'LANDLINE', 'INVALID');
```

---

## 3. Migration: V__company_phones_table.sql

Creates the company_phones table and all indexes/constraints.

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

-- Performance indexes
CREATE INDEX idx_company_phones_company_id
  ON company_phones(company_id);

CREATE INDEX idx_company_phones_normalized
  ON company_phones(number_normalized)
  WHERE number_normalized IS NOT NULL;

CREATE INDEX idx_company_phones_contact_id
  ON company_phones(contact_id)
  WHERE contact_id IS NOT NULL;

CREATE INDEX idx_company_phones_batch_id
  ON company_phones(batch_id)
  WHERE batch_id IS NOT NULL;

-- At most one primary phone per company
CREATE UNIQUE INDEX idx_company_phones_primary
  ON company_phones(company_id)
  WHERE is_primary = TRUE;

-- Normalized phone length check
ALTER TABLE company_phones ADD CONSTRAINT chk_phone_normalized_length
  CHECK (number_normalized IS NULL OR length(number_normalized) = 10);

-- Updated_at trigger (reuses existing function)
CREATE TRIGGER trg_company_phones_updated
  BEFORE UPDATE ON company_phones
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
```

---

## 4. Migration: V__decision_maker_designations.sql

Creates the reference table and seeds it.

```sql
CREATE TABLE decision_maker_designations (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  designation TEXT NOT NULL UNIQUE,
  aliases     TEXT[] NOT NULL DEFAULT '{}',
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed data
INSERT INTO decision_maker_designations (designation, aliases) VALUES
  ('MD', ARRAY['Managing Director', 'M.D.', 'Mng Director']),
  ('CEO', ARRAY['Chief Executive Officer', 'C.E.O.']),
  ('COO', ARRAY['Chief Operating Officer', 'C.O.O.']),
  ('Owner', ARRAY['Proprietor', 'Prop.', 'Business Owner']),
  ('Director', ARRAY['Dir.']),
  ('Partner', ARRAY[]::TEXT[]),
  ('Founder', ARRAY['Co-Founder', 'Co Founder']),
  ('Chairman', ARRAY['Chairperson', 'Chair']),
  ('General Manager', ARRAY['GM', 'G.M.']),
  ('Plant Manager', ARRAY['Factory Manager', 'Works Manager']),
  ('Purchase Manager', ARRAY['Procurement Manager', 'Buying Manager']),
  ('Production Manager', ARRAY['Manufacturing Manager', 'Prod. Manager']);
```

---

## 5. Migration: V__companies_gst_number.sql

Adds GST number to companies.

```sql
ALTER TABLE companies
  ADD COLUMN gst_number VARCHAR(15);

ALTER TABLE companies ADD CONSTRAINT chk_gst_format
  CHECK (
    gst_number IS NULL
    OR gst_number ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$'
  );

CREATE INDEX idx_companies_gst_number
  ON companies(gst_number)
  WHERE gst_number IS NOT NULL;
```

---

## 6. Migration: V__backfill_company_phones.sql

Backfills existing phone data into company_phones.

```sql
-- Backfill primary_phone_normalized from companies table
INSERT INTO company_phones (
  company_id,
  number_raw,
  number_normalized,
  phone_type,
  number_source,
  confidence,
  confidence_mode,
  is_primary,
  created_by,
  created_at
)
SELECT
  c.id,
  c.primary_phone_normalized,           -- raw = normalized (we don't have original)
  c.primary_phone_normalized,
  CASE
    WHEN c.primary_phone_normalized ~ '^[6-9]' THEN 'MOBILE'::phone_type
    WHEN length(c.primary_phone_normalized) = 10 THEN 'LANDLINE'::phone_type
    ELSE 'INVALID'::phone_type
  END,
  'IMPORT_DEFAULT'::number_source_type,  -- we don't know the original source
  'MEDIUM'::confidence_level,            -- conservative default
  'AUTO'::confidence_mode,
  c.created_by,
  c.created_at
FROM companies c
WHERE c.primary_phone_normalized IS NOT NULL
  AND c.primary_phone_normalized != ''
  AND NOT EXISTS (
    SELECT 1 FROM company_phones cp
    WHERE cp.company_id = c.id
      AND cp.number_normalized = c.primary_phone_normalized
  );

-- Backfill phones from contacts.phones[] array
-- NOTE: This depends on the contacts table having a phones[] column.
-- If contacts.phones[] does not exist in the current schema, skip this block.
/*
INSERT INTO company_phones (
  company_id,
  contact_id,
  number_raw,
  number_normalized,
  phone_type,
  number_source,
  confidence,
  confidence_mode,
  is_primary,
  designation_override,
  created_by,
  created_at
)
SELECT
  ct.company_id,
  ct.id,
  unnest(ct.phones),
  -- normalization would need to be done in application code, not SQL
  -- this insert uses raw values; a post-migration job normalizes them
  NULL,
  'MOBILE'::phone_type,
  'IMPORT_DEFAULT'::number_source_type,
  'MEDIUM'::confidence_level,
  'AUTO'::confidence_mode,
  FALSE,
  ct.designation,
  ct.created_by,
  ct.created_at
FROM contacts ct
WHERE ct.phones IS NOT NULL
  AND array_length(ct.phones, 1) > 0;
*/
```

### 6.1 Post-Migration Normalization Job

After the backfill migration, run a one-time application job that:
1. Iterates all company_phones where number_normalized IS NULL
2. Normalizes number_raw → number_normalized using NormalizationService
3. Classifies phone_type
4. Runs ConfidenceEngine (checks for decision-maker designation via linked contact)
5. Logs results

This is an application-layer job, not a SQL migration, because phone normalization rules are in Java.

---

## 7. Migration: V__import_mapping_aliases.sql

Adds new mapping aliases for the import framework.

```sql
-- Add new target fields to the import mapping system
-- The exact SQL depends on how import_template_mappings is structured.
-- If it's a config table:

INSERT INTO import_column_aliases (target_field, alias, normalized_alias) VALUES
  -- Alternate phones
  ('alternate_phone_1', 'Alternate Phone', 'alternatephone'),
  ('alternate_phone_1', 'Alt Phone', 'altphone'),
  ('alternate_phone_1', 'Phone 2', 'phone2'),
  ('alternate_phone_1', 'Mobile 2', 'mobile2'),
  ('alternate_phone_1', 'Second Phone', 'secondphone'),
  ('alternate_phone_1', 'Other Phone', 'otherphone'),
  ('alternate_phone_1', 'Alt Mobile', 'altmobile'),
  ('alternate_phone_2', 'Phone 3', 'phone3'),
  ('alternate_phone_2', 'Mobile 3', 'mobile3'),
  ('alternate_phone_2', 'Third Phone', 'thirdphone'),
  -- Owner phone
  ('owner_phone', 'Owner Phone', 'ownerphone'),
  ('owner_phone', 'Owner Mobile', 'ownermobile'),
  ('owner_phone', 'MD Phone', 'mdphone'),
  ('owner_phone', 'MD Mobile', 'mdmobile'),
  ('owner_phone', 'Director Phone', 'directorphone'),
  ('owner_phone', 'Director Mobile', 'directormobile'),
  ('owner_phone', 'Proprietor Phone', 'proprietorphone'),
  -- Number source
  ('number_source', 'Number Source', 'numbersource'),
  ('number_source', 'Phone Source', 'phonesource'),
  ('number_source', 'Mobile Source', 'mobilesource'),
  ('number_source', 'Source of Number', 'sourceofnumber'),
  -- Designation
  ('designation', 'Designation', 'designation'),
  ('designation', 'Title', 'title'),
  ('designation', 'Position', 'position'),
  ('designation', 'Role', 'role'),
  ('designation', 'Contact Title', 'contacttitle'),
  ('designation', 'Person Title', 'persontitle'),
  -- GST
  ('gst_number', 'GST', 'gst'),
  ('gst_number', 'GSTIN', 'gstin'),
  ('gst_number', 'GST Number', 'gstnumber'),
  ('gst_number', 'GST No', 'gstno'),
  ('gst_number', 'GST No.', 'gstno'),
  ('gst_number', 'Tax ID', 'taxid'),
  ('gst_number', 'Tax Number', 'taxnumber'),
  ('gst_number', 'GSTIN No', 'gstinno')
ON CONFLICT DO NOTHING;
```

---

## 8. Rollback Scripts

Each migration should have a corresponding rollback (not auto-applied):

```sql
-- Rollback V__phone_confidence_enums.sql
DROP TYPE IF EXISTS phone_type CASCADE;
DROP TYPE IF EXISTS confidence_mode CASCADE;
DROP TYPE IF EXISTS confidence_level CASCADE;
DROP TYPE IF EXISTS number_source_type CASCADE;

-- Rollback V__company_phones_table.sql
DROP TABLE IF EXISTS company_phones CASCADE;

-- Rollback V__decision_maker_designations.sql
DROP TABLE IF EXISTS decision_maker_designations CASCADE;

-- Rollback V__companies_gst_number.sql
ALTER TABLE companies DROP CONSTRAINT IF EXISTS chk_gst_format;
DROP INDEX IF EXISTS idx_companies_gst_number;
ALTER TABLE companies DROP COLUMN IF EXISTS gst_number;

-- Rollback V__backfill_company_phones.sql
DELETE FROM company_phones WHERE number_source = 'IMPORT_DEFAULT';
-- Or: TRUNCATE company_phones; (if no other data exists yet)
```

---

## 9. Migration Order

1. V__phone_confidence_enums.sql (enums first — referenced by later tables)
2. V__company_phones_table.sql (depends on enums + companies + contacts + import tables)
3. V__decision_maker_designations.sql (independent reference table)
4. V__companies_gst_number.sql (independent column addition)
5. V__import_mapping_aliases.sql (depends on import mapping table existing)
6. V__backfill_company_phones.sql (depends on company_phones table)

---

## 10. Pre-Migration Checklist

- [ ] Verify `touch_updated_at()` function exists (created in earlier migration)
- [ ] Verify `companies` table exists with `primary_phone_normalized` column
- [ ] Verify `contacts` table exists (for FK reference)
- [ ] Verify `import_batches` and `import_rows` tables exist (for FK references)
- [ ] Verify import alias table name matches the actual schema (may be `import_column_aliases` or `import_template_mappings`)
- [ ] Run on a copy of production data first to validate backfill results
- [ ] After backfill: verify count of company_phones matches count of non-null primary_phone_normalized in companies
