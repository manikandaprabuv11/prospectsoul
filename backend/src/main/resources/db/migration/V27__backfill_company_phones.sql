-- v1.2: Backfill existing primary_phone_normalized into company_phones.
-- Safe: only inserts where no matching company_phone already exists.

INSERT INTO company_phones (
  company_id,
  number_raw,
  number_normalized,
  phone_type,
  number_source,
  confidence,
  confidence_mode,
  is_primary,
  created_at
)
SELECT
  c.id,
  c.primary_phone_normalized,
  c.primary_phone_normalized,
  CASE
    WHEN c.primary_phone_normalized ~ '^[6-9]' THEN 'MOBILE'::phone_type
    WHEN length(c.primary_phone_normalized) = 10 THEN 'LANDLINE'::phone_type
    ELSE 'INVALID'::phone_type
  END,
  'IMPORT_DEFAULT'::number_source_type,
  'MEDIUM'::confidence_level,
  'AUTO'::confidence_mode,
  TRUE,
  c.created_at
FROM companies c
WHERE c.primary_phone_normalized IS NOT NULL
  AND c.primary_phone_normalized != ''
  AND length(c.primary_phone_normalized) = 10
  AND NOT EXISTS (
    SELECT 1 FROM company_phones cp
    WHERE cp.company_id = c.id
      AND cp.number_normalized = c.primary_phone_normalized
  );
