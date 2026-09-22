-- Contacts (ADR-0006).
--
-- Docs 21 §5.1 and ADR-0001 both assume `contacts` already exists (the
-- Company Management vertical slice, docs 01–18, deliberately deferred
-- "full contact lifecycle" per doc 03 §Scope Control). It never did — this
-- migration therefore CREATES the table, carrying exactly the columns the
-- addenda reference. Backfill from `is_md_owner` in the following migration
-- is a legal no-op on an empty table; the invariant we care about is that
-- once rows exist, `role_id` and `is_md_owner` remain in sync via the
-- service layer.
CREATE TABLE contacts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID          NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                 VARCHAR(255)  NOT NULL,
    designation          VARCHAR(255),
    phone                VARCHAR(20),
    email                VARCHAR(255),
    is_primary           BOOLEAN       NOT NULL DEFAULT false,
    is_md_owner          BOOLEAN       NOT NULL DEFAULT false,
    association_start    DATE,
    association_end      DATE,
    verification_status  VARCHAR(50)   NOT NULL DEFAULT 'UNVERIFIED',
    created_by           VARCHAR(255),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by           VARCHAR(255),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_contacts_company    ON contacts(company_id);
CREATE INDEX idx_contacts_name       ON contacts(name);
CREATE INDEX idx_contacts_email      ON contacts(email);
CREATE INDEX idx_contacts_phone      ON contacts(phone);
CREATE INDEX idx_contacts_is_primary ON contacts(is_primary) WHERE is_primary = true;

-- Domain Model Addendum Invariant 13: at most one primary contact per company.
CREATE UNIQUE INDEX uq_contacts_one_primary_per_company
    ON contacts(company_id) WHERE is_primary = true;
