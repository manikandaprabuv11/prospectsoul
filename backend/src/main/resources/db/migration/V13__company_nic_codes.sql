-- Company ↔ NIC join (docs/dev_docs/21 §6, ADR-0002).
--
-- Raw code + description are preserved so unmatched codes are never lost
-- (Domain Model Invariant 6). Exactly one row per company may be flagged
-- `is_primary` — enforced at the DB via the partial unique index below
-- so the service-level swap must run inside a single transaction.
CREATE TABLE company_nic_codes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID          NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    nic_code_id      UUID          REFERENCES nic_codes(id),
    nic_code_raw     VARCHAR(6)    NOT NULL,
    description_raw  TEXT,
    is_primary       BOOLEAN       NOT NULL DEFAULT false,
    sequence_no      SMALLINT      NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_cnc_company_raw UNIQUE (company_id, nic_code_raw)
);

CREATE INDEX idx_cnc_company ON company_nic_codes(company_id);
CREATE INDEX idx_cnc_nic     ON company_nic_codes(nic_code_id);

-- Domain Model Addendum Invariant 12.
CREATE UNIQUE INDEX uq_cnc_one_primary_per_company
    ON company_nic_codes(company_id) WHERE is_primary = true;
