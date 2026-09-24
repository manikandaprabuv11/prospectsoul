CREATE TABLE provider_configs (
  provider_key             VARCHAR(40) PRIMARY KEY,
  enabled                  BOOLEAN NOT NULL DEFAULT true,
  rate_limit_per_sec       INTEGER,
  rate_limit_per_day       INTEGER,
  timeout_ms               INTEGER NOT NULL DEFAULT 30000,
  max_retries              SMALLINT NOT NULL DEFAULT 3,
  idempotency_window_hours SMALLINT NOT NULL DEFAULT 24,
  cost_per_call_usd        NUMERIC(10,6) NOT NULL DEFAULT 0,
  options                  JSONB,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by               UUID
);

INSERT INTO provider_configs (provider_key, rate_limit_per_sec, rate_limit_per_day, cost_per_call_usd) VALUES
  ('GOOGLE_PLACES', 10, 5000, 0.017),
  ('WEBSITE',        5, 2000, 0.002),
  ('PHONE',         20, 10000, 0.005);
