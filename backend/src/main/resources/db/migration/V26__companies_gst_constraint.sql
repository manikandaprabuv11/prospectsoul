-- v1.2: GST number format constraint and index.
-- The gst_number column already exists (V10). This adds validation only.

ALTER TABLE companies ADD CONSTRAINT chk_gst_format
  CHECK (
    gst_number IS NULL
    OR gst_number ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$'
  );

CREATE INDEX idx_companies_gst_number
  ON companies(gst_number)
  WHERE gst_number IS NOT NULL;
