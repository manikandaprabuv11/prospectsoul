-- v1.2: Decision-maker designation reference table with seed data.
-- Used by ConfidenceEngine to resolve designation -> is_decision_maker.

CREATE TABLE decision_maker_designations (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  designation TEXT NOT NULL UNIQUE,
  aliases     TEXT[] NOT NULL DEFAULT '{}',
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO decision_maker_designations (designation, aliases) VALUES
  ('MD', ARRAY['Managing Director', 'M.D.', 'Mng Director']),
  ('CEO', ARRAY['Chief Executive Officer', 'C.E.O.']),
  ('COO', ARRAY['Chief Operating Officer', 'C.O.O.']),
  ('Owner', ARRAY['Proprietor', 'Prop.', 'Business Owner']),
  ('Director', ARRAY['Dir.']),
  ('Partner', ARRAY[]::TEXT[]),
  ('Founder', ARRAY['Co-Founder', 'Co Founder']),
  ('Chairman', ARRAY['Chairperson', 'Chair']),
  ('General Manager', ARRAY['GM', 'G.M.']),
  ('Plant Manager', ARRAY['Factory Manager', 'Works Manager']),
  ('Purchase Manager', ARRAY['Procurement Manager', 'Buying Manager']),
  ('Production Manager', ARRAY['Manufacturing Manager', 'Prod. Manager']);
