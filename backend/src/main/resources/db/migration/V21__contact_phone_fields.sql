ALTER TABLE contacts
  ADD COLUMN IF NOT EXISTS phone_normalized       VARCHAR(20),
  ADD COLUMN IF NOT EXISTS phone_country          VARCHAR(3),
  ADD COLUMN IF NOT EXISTS phone_region           VARCHAR(60),
  ADD COLUMN IF NOT EXISTS phone_carrier          VARCHAR(60),
  ADD COLUMN IF NOT EXISTS phone_type             VARCHAR(20),
  ADD COLUMN IF NOT EXISTS phone_status           VARCHAR(20),
  ADD COLUMN IF NOT EXISTS phone_dnd_registered   BOOLEAN,
  ADD COLUMN IF NOT EXISTS phone_last_enriched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_contact_phone_status ON contacts(phone_status);
