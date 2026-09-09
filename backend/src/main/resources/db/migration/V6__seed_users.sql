INSERT INTO users (id, username, full_name, email, role, active) VALUES
('a1000000-0000-0000-0000-000000000001', 'analyst',   'Priya Sharma',     'priya@vyoog.com',   'PS_ANALYST',     true),
('a1000000-0000-0000-0000-000000000002', 'analyst2',  'Deepa Krishnan',   'deepa@vyoog.com',   'PS_ANALYST',     true),
('b2000000-0000-0000-0000-000000000001', 'saleslead', 'Kumar Rajan',      'kumar@vyoog.com',   'PS_SALES_LEAD',  true),
('c3000000-0000-0000-0000-000000000001', 'admin',     'Ravi Chandran',    'ravi@vyoog.com',    'PS_ADMIN',       true),
('d4000000-0000-0000-0000-000000000001', 'viewer',    'Meera Natarajan',  'meera@vyoog.com',   'PS_VIEWER',      true),
('d4000000-0000-0000-0000-000000000002', 'viewer2',   'Arun Prakash',     'arun@vyoog.com',    'PS_VIEWER',      true),
('e5000000-0000-0000-0000-000000000001', 'coo',       'Senthil Kumar',    'senthil@vyoog.com', 'PS_COO',         true);

INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES
('a1000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', now()),
('a1000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', now()),
('b2000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', now()),
('c3000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', now()),
('d4000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', now()),
('d4000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004', now()),
('e5000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', now());
