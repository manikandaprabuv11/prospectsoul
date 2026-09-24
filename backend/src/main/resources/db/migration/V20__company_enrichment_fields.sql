ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS google_place_id           VARCHAR(120),
  ADD COLUMN IF NOT EXISTS google_name               TEXT,
  ADD COLUMN IF NOT EXISTS google_business_category  VARCHAR(120),
  ADD COLUMN IF NOT EXISTS google_business_types     TEXT,
  ADD COLUMN IF NOT EXISTS google_maps_url           TEXT,
  ADD COLUMN IF NOT EXISTS google_lat                NUMERIC(10,7),
  ADD COLUMN IF NOT EXISTS google_lng                NUMERIC(10,7),
  ADD COLUMN IF NOT EXISTS google_business_status    VARCHAR(30),
  ADD COLUMN IF NOT EXISTS google_last_enriched_at   TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS website_reachable         BOOLEAN,
  ADD COLUMN IF NOT EXISTS website_title             TEXT,
  ADD COLUMN IF NOT EXISTS website_description       TEXT,
  ADD COLUMN IF NOT EXISTS website_last_enriched_at  TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS social_linkedin           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_facebook           VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_x                  VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_instagram          VARCHAR(200),
  ADD COLUMN IF NOT EXISTS social_youtube            VARCHAR(200),
  ADD COLUMN IF NOT EXISTS primary_phone_country        VARCHAR(3),
  ADD COLUMN IF NOT EXISTS primary_phone_region         VARCHAR(60),
  ADD COLUMN IF NOT EXISTS primary_phone_carrier        VARCHAR(60),
  ADD COLUMN IF NOT EXISTS primary_phone_type           VARCHAR(20),
  ADD COLUMN IF NOT EXISTS primary_phone_status         VARCHAR(20),
  ADD COLUMN IF NOT EXISTS primary_phone_dnd_registered BOOLEAN,
  ADD COLUMN IF NOT EXISTS primary_phone_last_enriched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_company_google_place ON companies(google_place_id);
CREATE INDEX IF NOT EXISTS idx_company_google_lat_lng ON companies(google_lat, google_lng);
CREATE INDEX IF NOT EXISTS idx_company_phone_status ON companies(primary_phone_status);
