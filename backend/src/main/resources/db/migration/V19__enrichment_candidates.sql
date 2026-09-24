CREATE TABLE enrichment_candidates (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  contact_id        UUID REFERENCES contacts(id) ON DELETE CASCADE,
  enrichment_job_id UUID NOT NULL REFERENCES enrichment_jobs(id) ON DELETE CASCADE,
  candidate_type    VARCHAR(30) NOT NULL,
  field_name        VARCHAR(60),
  proposed_value    TEXT,
  current_value     TEXT,
  provider_key      VARCHAR(40) NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  resolved_by       UUID,
  resolved_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cand_company   ON enrichment_candidates(company_id);
CREATE INDEX idx_cand_status    ON enrichment_candidates(status);
CREATE INDEX idx_cand_type      ON enrichment_candidates(candidate_type);
CREATE INDEX idx_cand_job       ON enrichment_candidates(enrichment_job_id);
