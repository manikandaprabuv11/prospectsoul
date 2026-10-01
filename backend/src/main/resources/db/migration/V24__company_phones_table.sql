-- v1.2: Multi-phone support — company_phones table.
-- Depends on V23 (enums), V2 (companies), V11 (contacts), V3 (import tables).

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

-- At most one primary phone per company (partial unique index).
CREATE UNIQUE INDEX idx_company_phones_primary
  ON company_phones(company_id)
  WHERE is_primary = TRUE;

-- Normalized phone must be exactly 10 digits when present.
ALTER TABLE company_phones ADD CONSTRAINT chk_phone_normalized_length
  CHECK (number_normalized IS NULL OR length(number_normalized) = 10);

CREATE TRIGGER trg_company_phones_updated
  BEFORE UPDATE ON company_phones
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
