-- v1.2: Phone confidence enums for multi-phone support.
-- Must run before V24 (company_phones table references these types).

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

-- Reusable trigger function for auto-updating updated_at timestamps.
-- Referenced by company_phones (V24) and potentially other future tables.
CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
