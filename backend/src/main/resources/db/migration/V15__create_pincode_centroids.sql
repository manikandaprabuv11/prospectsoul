-- Offline pincode centroids (docs/dev_docs/21 §7.1). Populated from India
-- Post PIN data; the Places API is NEVER called for a centroid lookup so
-- the map screen can operate without a Google roundtrip per request.
--
-- Only a small illustrative seed (Coimbatore / Kanchipuram / a few cities)
-- is inserted here so the module is exercisable in tests; the full India
-- data set is loaded by an out-of-band Admin import.
CREATE TABLE pincode_centroids (
    pincode      VARCHAR(6) PRIMARY KEY,
    area_name    VARCHAR(255) NOT NULL,
    district     VARCHAR(120),
    state        VARCHAR(120),
    latitude     NUMERIC(9,6) NOT NULL,
    longitude    NUMERIC(9,6) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pincode_numeric CHECK (pincode ~ '^[0-9]{6}$')
);

INSERT INTO pincode_centroids (pincode, area_name, district, state, latitude, longitude) VALUES
    ('641001', 'Coimbatore H.O.',           'Coimbatore',   'Tamil Nadu', 11.017100, 76.958700),
    ('641004', 'Race Course',               'Coimbatore',   'Tamil Nadu', 10.998400, 76.973300),
    ('631502', 'Kanchipuram H.O.',          'Kanchipuram',  'Tamil Nadu', 12.834200, 79.702400),
    ('600001', 'Chennai G.P.O.',            'Chennai',      'Tamil Nadu', 13.088500, 80.278800),
    ('560001', 'Bangalore G.P.O.',          'Bangalore',    'Karnataka',  12.976700, 77.590100),
    ('400001', 'Mumbai G.P.O.',             'Mumbai',       'Maharashtra',18.940200, 72.835800),
    ('110001', 'Connaught Place',           'New Delhi',    'Delhi',      28.632700, 77.219700);
