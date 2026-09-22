-- Contact roles (docs/dev_docs/21 §5, ADR-0001).
--
-- Admin-editable, deactivate-never-delete. The starter list matches the
-- one specified in the PRD Addendum §2.3.
CREATE TABLE contact_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(40)  NOT NULL UNIQUE,
    label       VARCHAR(80)  NOT NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 100,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE contacts
    ADD COLUMN role_id UUID REFERENCES contact_roles(id);

CREATE INDEX idx_contact_role ON contacts(role_id);

INSERT INTO contact_roles (key, label, sort_order) VALUES
    ('MD_OWNER',      'MD / Owner',      10),
    ('DIRECTOR',      'Director',        20),
    ('CEO',           'CEO',             30),
    ('COO',           'COO',             40),
    ('CFO',           'CFO',             50),
    ('HR_HEAD',       'HR Head',         60),
    ('PURCHASE_HEAD', 'Purchase Head',   70),
    ('SALES_HEAD',    'Sales Head',      80),
    ('PLANT_HEAD',    'Plant Head',      90),
    ('ACCOUNTS',      'Accounts',       100),
    ('ADMIN',         'Admin',          110),
    ('IT',            'IT',             120),
    ('OTHER',         'Other',          999);

-- Backfill (no-op on an empty table today; safe on any future re-run).
UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'MD_OWNER')
    WHERE is_md_owner = true AND role_id IS NULL;

UPDATE contacts SET role_id = (SELECT id FROM contact_roles WHERE key = 'OTHER')
    WHERE role_id IS NULL;
