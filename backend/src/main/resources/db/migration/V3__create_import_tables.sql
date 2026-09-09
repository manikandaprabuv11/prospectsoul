-- Import batches: one row per upload event
CREATE TABLE import_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name       VARCHAR(500)  NOT NULL,
    file_type       VARCHAR(20)   NOT NULL,
    source          VARCHAR(50)   NOT NULL,
    status          VARCHAR(50)   NOT NULL DEFAULT 'UPLOADED',
    total_rows      INTEGER       NOT NULL DEFAULT 0,
    processed_rows  INTEGER       NOT NULL DEFAULT 0,
    created_rows    INTEGER       NOT NULL DEFAULT 0,
    duplicate_rows  INTEGER       NOT NULL DEFAULT 0,
    rejected_rows   INTEGER       NOT NULL DEFAULT 0,
    error_message   TEXT,
    column_mappings JSONB,
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_import_batches_status ON import_batches (status);
CREATE INDEX idx_import_batches_created_at ON import_batches (created_at);

-- Import rows: every source row persisted before processing
CREATE TABLE import_rows (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id                UUID          NOT NULL REFERENCES import_batches(id),
    row_number              INTEGER       NOT NULL,
    raw_data                JSONB         NOT NULL,
    mapped_data             JSONB,
    status                  VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    error_message           TEXT,
    company_id              UUID          REFERENCES companies(id),
    duplicate_of_company_id UUID          REFERENCES companies(id),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_import_rows_batch ON import_rows (batch_id);
CREATE INDEX idx_import_rows_status ON import_rows (status);

-- Import templates: saved column mapping configurations
CREATE TABLE import_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200)  NOT NULL,
    source      VARCHAR(50)   NOT NULL,
    is_default  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by  VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Import template mappings: individual column mappings within a template
CREATE TABLE import_template_mappings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID          NOT NULL REFERENCES import_templates(id) ON DELETE CASCADE,
    source_header   VARCHAR(500)  NOT NULL,
    target_field    VARCHAR(200)  NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_template_mappings_template ON import_template_mappings (template_id);
