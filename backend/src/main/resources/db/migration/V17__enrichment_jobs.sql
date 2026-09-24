CREATE TABLE enrichment_jobs (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_id          UUID,
  company_id        UUID REFERENCES companies(id) ON DELETE CASCADE,
  contact_id        UUID REFERENCES contacts(id) ON DELETE CASCADE,
  provider_key      VARCHAR(40) NOT NULL,
  input_hash        VARCHAR(64) NOT NULL,
  status            VARCHAR(20) NOT NULL,
  attempt           SMALLINT NOT NULL DEFAULT 1,
  max_attempts      SMALLINT NOT NULL DEFAULT 3,
  scheduled_at      TIMESTAMPTZ,
  started_at        TIMESTAMPTZ,
  completed_at      TIMESTAMPTZ,
  facts_added       SMALLINT NOT NULL DEFAULT 0,
  facts_updated     SMALLINT NOT NULL DEFAULT 0,
  candidates_added  SMALLINT NOT NULL DEFAULT 0,
  cost_usd          NUMERIC(10,6) NOT NULL DEFAULT 0,
  error_code        VARCHAR(60),
  error_message     TEXT,
  triggered_by      UUID,
  triggered_via     VARCHAR(20) NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (company_id IS NOT NULL OR contact_id IS NOT NULL)
);

CREATE INDEX idx_ej_company     ON enrichment_jobs(company_id);
CREATE INDEX idx_ej_contact     ON enrichment_jobs(contact_id);
CREATE INDEX idx_ej_batch       ON enrichment_jobs(batch_id);
CREATE INDEX idx_ej_provider    ON enrichment_jobs(provider_key);
CREATE INDEX idx_ej_status      ON enrichment_jobs(status);
CREATE INDEX idx_ej_created     ON enrichment_jobs(created_at DESC);
CREATE UNIQUE INDEX idx_ej_idempotency
  ON enrichment_jobs(provider_key, input_hash, COALESCE(company_id, contact_id))
  WHERE status IN ('QUEUED', 'RUNNING', 'SUCCESS');
