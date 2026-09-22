-- NIC Code master (docs/dev_docs/21 §3, ADR-0002 backing table).
--
-- Reference data, admin-editable. Import is a separate operation that does
-- NOT create import_batches / import_rows rows (Kickoff constraint 6).
--
-- The tree is materialised by parent_id (resolved once at import time via
-- longest existing prefix — Kickoff constraint 5), so descendant queries
-- run as a recursive CTE against a real relationship instead of substring
-- arithmetic on the code string.
CREATE TABLE nic_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nic_data_id     INTEGER,
    code            VARCHAR(6)    NOT NULL UNIQUE,
    description     TEXT          NOT NULL,
    industry_type   VARCHAR(20)   NOT NULL,
    level           SMALLINT      NOT NULL,
    parent_id       UUID          REFERENCES nic_codes(id),
    is_primary      BOOLEAN       NOT NULL DEFAULT false,
    active          BOOLEAN       NOT NULL DEFAULT true,
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_nic_level        CHECK (level BETWEEN 1 AND 5),
    CONSTRAINT chk_nic_code_numeric CHECK (code ~ '^[0-9]{1,5}$'),
    CONSTRAINT chk_nic_industry     CHECK (industry_type IN ('Service','Manufacturing','Unknown'))
);

CREATE INDEX idx_nic_parent  ON nic_codes(parent_id);
CREATE INDEX idx_nic_code    ON nic_codes(code);
CREATE INDEX idx_nic_type    ON nic_codes(industry_type);
CREATE INDEX idx_nic_primary ON nic_codes(is_primary) WHERE is_primary = true;
CREATE INDEX idx_nic_active  ON nic_codes(active)     WHERE active     = true;
