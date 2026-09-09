CREATE TABLE companies (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_name            VARCHAR(500)  NOT NULL,
    normalized_name           VARCHAR(500)  NOT NULL,
    website_domain            VARCHAR(500),
    primary_phone_normalized  VARCHAR(20),
    email                     VARCHAR(500),
    city                      VARCHAR(200),
    state                     VARCHAR(200),
    cluster                   VARCHAR(200),
    industry                  VARCHAR(200),
    size_band                 VARCHAR(50),
    tags                      TEXT[],
    source                    VARCHAR(50),
    pipeline_state            VARCHAR(50)   NOT NULL DEFAULT 'IMPORTED',
    completeness_score        INTEGER       NOT NULL DEFAULT 0,
    verification_status       VARCHAR(50)   NOT NULL DEFAULT 'UNVERIFIED',
    verified_by               VARCHAR(255),
    verified_at               TIMESTAMPTZ,
    created_by                VARCHAR(255),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by                VARCHAR(255),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_companies_normalized_name ON companies (normalized_name);
CREATE INDEX idx_companies_website_domain ON companies (website_domain);
CREATE INDEX idx_companies_phone ON companies (primary_phone_normalized);
CREATE INDEX idx_companies_email ON companies (email);
CREATE INDEX idx_companies_city ON companies (city);
CREATE INDEX idx_companies_state ON companies (state);
CREATE INDEX idx_companies_cluster ON companies (cluster);
CREATE INDEX idx_companies_industry ON companies (industry);
CREATE INDEX idx_companies_pipeline_state ON companies (pipeline_state);
CREATE INDEX idx_companies_verification ON companies (verification_status);
CREATE INDEX idx_companies_created_at ON companies (created_at);
CREATE INDEX idx_companies_updated_at ON companies (updated_at);
