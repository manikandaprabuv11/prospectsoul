-- Sales-Intelligence company field extension (docs/dev_docs/21 §4, ADR-0003).
--
-- Additive only. `companies` is NOT recreated. Every pre-existing row remains
-- valid — all new columns are nullable and have no defaults that could
-- silently overwrite data.
--
-- The docs' original snippet ends with two `ALTER TYPE import_source
-- ADD VALUE` statements. There is no such PostgreSQL enum type in this
-- schema: `companies.source` and `import_batches.source` are plain
-- VARCHAR(50). Those two lines are deliberately omitted — the source string
-- is set at import time (`GOOGLE_PLACES`, `UDYAM_MSME_REGISTRY`) with no
-- type-level change needed.
ALTER TABLE companies
  ADD COLUMN pincode             VARCHAR(6),
  ADD COLUMN district            VARCHAR(120),
  ADD COLUMN address_line        TEXT,
  ADD COLUMN region              VARCHAR(120),
  ADD COLUMN products            TEXT,
  ADD COLUMN turnover            NUMERIC(18,2),
  ADD COLUMN gst_number          VARCHAR(15),
  ADD COLUMN employee_count      INTEGER,
  ADD COLUMN registration_date   DATE,
  ADD COLUMN source_reference    VARCHAR(120),
  ADD COLUMN lg_state_code       SMALLINT,
  ADD COLUMN lg_district_code    INTEGER,
  ADD COLUMN primary_nic_code_id UUID REFERENCES nic_codes(id),
  -- ADR-0009: owned companies need coordinates to plot on a map with a
  -- meaningful `radius_km`. Adding here as two nullable columns keeps the
  -- migration additive; geocoding is a follow-up C4 concern.
  ADD COLUMN latitude            NUMERIC(9,6),
  ADD COLUMN longitude           NUMERIC(9,6);

CREATE INDEX idx_company_nic         ON companies(primary_nic_code_id);
CREATE INDEX idx_company_pincode     ON companies(pincode);
CREATE INDEX idx_company_district    ON companies(district);
CREATE INDEX idx_company_region      ON companies(region);
CREATE INDEX idx_company_turnover    ON companies(turnover);
CREATE INDEX idx_company_source_ref  ON companies(source_reference);
-- Non-unique because two identical (source, ref) pairs must be detectable
-- as duplicates rather than rejected by the DB. Uniqueness is enforced by
-- the source-aware dedup rule in the service layer (ADR-0004).
CREATE INDEX idx_company_source_ref_source ON companies(source, source_reference);
-- Radius queries benefit from a compound index on the plot columns.
CREATE INDEX idx_company_lat_lng ON companies(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
