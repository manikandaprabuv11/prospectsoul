-- Company phone verification (docs/dev_docs/14 §3).
--
-- verification_batches is one "Start Verification" event; verification_batch_items
-- is the DB-backed work queue the worker claims from with FOR UPDATE SKIP LOCKED.
--
-- `requested_by` and `filter_added_by` are VARCHAR(255), not UUID: the actor
-- columns already in this schema (companies.created_by, companies.verified_by,
-- audit_log.actor) are VARCHAR(255) holding the Keycloak subject, and
-- filter_added_by is compared directly against companies.created_by.
--
-- The canonical verification state stays on companies.verification_status /
-- verified_by / verified_at. Nothing here duplicates it.
CREATE TABLE verification_batches (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by       VARCHAR(255)  NOT NULL,
    status             VARCHAR(50)   NOT NULL DEFAULT 'QUEUED',
    filter_added_by    VARCHAR(255),
    filter_date_from   DATE,
    filter_date_to     DATE,
    total_count        INTEGER       NOT NULL DEFAULT 0,
    queued_count       INTEGER       NOT NULL DEFAULT 0,
    processing_count   INTEGER       NOT NULL DEFAULT 0,
    verified_count     INTEGER       NOT NULL DEFAULT 0,
    failed_count       INTEGER       NOT NULL DEFAULT 0,
    skipped_count      INTEGER       NOT NULL DEFAULT 0,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_verification_batches_status ON verification_batches (status);
CREATE INDEX idx_verification_batches_requested_by ON verification_batches (requested_by);
CREATE INDEX idx_verification_batches_created_at ON verification_batches (created_at DESC);

CREATE TABLE verification_batch_items (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id                  UUID          NOT NULL REFERENCES verification_batches(id),
    company_id                UUID          NOT NULL REFERENCES companies(id),
    status                    VARCHAR(50)   NOT NULL DEFAULT 'QUEUED',
    phone_number              VARCHAR(50),
    normalized_phone_number   VARCHAR(50),
    provider                  VARCHAR(50),
    provider_reference        VARCHAR(500),
    phone_valid               BOOLEAN,
    line_type                 VARCHAR(50),
    carrier_name              VARCHAR(255),
    mobile_country_code       VARCHAR(10),
    mobile_network_code       VARCHAR(10),
    failure_code              VARCHAR(50),
    failure_message           TEXT,
    attempt_count             INTEGER       NOT NULL DEFAULT 0,
    started_at                TIMESTAMPTZ,
    completed_at              TIMESTAMPTZ,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_verification_batch_item UNIQUE (batch_id, company_id)
);

CREATE INDEX idx_verification_items_batch ON verification_batch_items (batch_id);
CREATE INDEX idx_verification_items_status ON verification_batch_items (status);
CREATE INDEX idx_verification_items_company ON verification_batch_items (company_id);
CREATE INDEX idx_verification_items_created_at ON verification_batch_items (created_at);

-- The worker claims with: WHERE status = 'QUEUED' ORDER BY created_at
-- FOR UPDATE SKIP LOCKED. This partial index keeps that claim cheap once the
-- table holds a large history of terminal items.
CREATE INDEX idx_verification_items_queued_claim
    ON verification_batch_items (created_at)
    WHERE status = 'QUEUED';
