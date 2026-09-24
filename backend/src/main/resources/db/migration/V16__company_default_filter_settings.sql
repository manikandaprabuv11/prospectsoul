-- Admin-managed default filter settings for the Companies list.
--
-- Each column visible on the Companies List has one row here. The admin can:
--   • enable/disable the default filter on that column,
--   • set the default operator (eq / gte / lte / range / between / in / is_present / is_missing),
--   • set the value(s) — stored as JSONB so a value can be a scalar, a
--     [min,max] pair or an array without needing per-column columns.
--
-- The service applies every ACTIVE row when the Companies list is loaded
-- with `?apply_defaults=true`, unless the request already sets a value
-- for that same filter key. So a user can always override a default.
CREATE TABLE company_default_filters (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Filter key on the /companies endpoint (e.g. "employee_min",
    -- "nic_parent_id", "gst_present", "pipeline_state"). Kept as text so
    -- adding a new filter later never needs a schema change.
    filter_key     VARCHAR(80)  NOT NULL,
    label          VARCHAR(120) NOT NULL,
    operator       VARCHAR(30)  NOT NULL DEFAULT 'eq',
    value          JSONB,
    active         BOOLEAN      NOT NULL DEFAULT true,
    sort_order     SMALLINT     NOT NULL DEFAULT 100,
    created_by     VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(255),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_default_filter_key UNIQUE (filter_key)
);

CREATE INDEX idx_default_filters_active ON company_default_filters(active) WHERE active = true;

-- Sensible starter defaults so an admin sees something the moment the
-- new settings page loads. All rows land ACTIVE = true; toggle off any
-- one to disable it without deleting.
INSERT INTO company_default_filters (filter_key, label, operator, value, sort_order, active) VALUES
  ('employee_min',     'Minimum employees',       'gte',        '10',    10, true),
  ('gst_present',      'GST present',             'eq',         'true',  20, true),
  ('pipeline_state',   'Pipeline state',          'eq',         '"READY"', 30, false),
  ('verification_status','Verification status',    'eq',         '"VERIFIED"', 40, false),
  ('has_nic_primary',  'Has primary NIC',         'eq',         'true',  50, true),
  ('turnover_min',     'Minimum turnover',        'gte',         'null', 60, false);
