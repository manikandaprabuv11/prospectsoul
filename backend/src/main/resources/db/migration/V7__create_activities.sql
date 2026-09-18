-- Activities: every human or machine touch on a company (PRD v1.1 §3.6, §10).
--
-- The Verification module (docs/dev_docs/14 §12) must create VERIFICATION
-- activities so that verification composes into the query-time Company
-- Timeline (Domain Model Invariant 7) instead of sitting in a second event
-- store. This migration therefore creates only the `activities` table exactly
-- as PRD v1.1 §10 specifies it. Attachments and evidence are separate tables
-- in the PRD and are deliberately NOT created here — they belong to the
-- research module, not to this vertical slice.
CREATE TABLE activities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID          NOT NULL REFERENCES companies(id),
    type            VARCHAR(50)   NOT NULL,
    content         JSONB,
    ai_provider     VARCHAR(100),
    ai_model        VARCHAR(100),
    prompt_version  VARCHAR(50),
    verified        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_activities_company ON activities (company_id);
CREATE INDEX idx_activities_type ON activities (type);
CREATE INDEX idx_activities_created_at ON activities (created_at);
CREATE INDEX idx_activities_company_created_at ON activities (company_id, created_at DESC);
