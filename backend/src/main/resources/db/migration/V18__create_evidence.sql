CREATE TABLE evidence (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id          UUID REFERENCES companies(id) ON DELETE CASCADE,
  contact_id          UUID REFERENCES contacts(id) ON DELETE CASCADE,
  observation_type    VARCHAR(30) NOT NULL,
  provider_key        VARCHAR(40),
  enrichment_job_id   UUID REFERENCES enrichment_jobs(id) ON DELETE SET NULL,
  source_url          TEXT,
  excerpt             TEXT,
  ai_model            VARCHAR(100),
  raw_payload_ref     TEXT,
  raw_payload_inline  JSONB,
  observed_at         TIMESTAMPTZ,
  capture_date        TIMESTAMPTZ,
  created_by          VARCHAR(255),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ev_company    ON evidence(company_id);
CREATE INDEX idx_ev_contact    ON evidence(contact_id);
CREATE INDEX idx_ev_provider   ON evidence(provider_key);
CREATE INDEX idx_ev_job        ON evidence(enrichment_job_id);
CREATE INDEX idx_ev_observed   ON evidence(observed_at DESC);
CREATE INDEX idx_ev_type       ON evidence(observation_type);
