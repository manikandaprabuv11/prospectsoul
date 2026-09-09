CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(100)  NOT NULL,
    entity_id       UUID          NOT NULL,
    actor           VARCHAR(255),
    action          VARCHAR(100)  NOT NULL,
    previous_state  JSONB,
    new_state       JSONB,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
