-- Move phone enrichment data from companies to company_phones.
-- primary_phone_normalized stays on companies as a denormalized dedup key.

-- Step 1: Add enrichment columns to company_phones
ALTER TABLE company_phones
  ADD COLUMN IF NOT EXISTS enriched_country      VARCHAR(3),
  ADD COLUMN IF NOT EXISTS enriched_region       VARCHAR(60),
  ADD COLUMN IF NOT EXISTS enriched_carrier      VARCHAR(60),
  ADD COLUMN IF NOT EXISTS enriched_line_type    VARCHAR(20),
  ADD COLUMN IF NOT EXISTS enriched_status       VARCHAR(20),
  ADD COLUMN IF NOT EXISTS enriched_dnd          BOOLEAN,
  ADD COLUMN IF NOT EXISTS enriched_at           TIMESTAMPTZ;

-- Step 2: Backfill enrichment data from companies to primary phones
UPDATE company_phones cp
SET enriched_country   = c.primary_phone_country,
    enriched_region    = c.primary_phone_region,
    enriched_carrier   = c.primary_phone_carrier,
    enriched_line_type = c.primary_phone_type,
    enriched_status    = c.primary_phone_status,
    enriched_dnd       = c.primary_phone_dnd_registered,
    enriched_at        = c.primary_phone_last_enriched_at
FROM companies c
WHERE cp.company_id = c.id
  AND cp.is_primary = TRUE
  AND c.primary_phone_last_enriched_at IS NOT NULL;

-- Step 3: Drop enrichment columns from companies
ALTER TABLE companies
  DROP COLUMN IF EXISTS primary_phone_country,
  DROP COLUMN IF EXISTS primary_phone_region,
  DROP COLUMN IF EXISTS primary_phone_carrier,
  DROP COLUMN IF EXISTS primary_phone_type,
  DROP COLUMN IF EXISTS primary_phone_status,
  DROP COLUMN IF EXISTS primary_phone_dnd_registered,
  DROP COLUMN IF EXISTS primary_phone_last_enriched_at;

-- Step 4: Drop the now-orphaned index
DROP INDEX IF EXISTS idx_company_phone_status;
