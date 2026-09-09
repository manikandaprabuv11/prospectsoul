INSERT INTO roles (id, name, display_name, description, permissions) VALUES
('10000000-0000-0000-0000-000000000001', 'PS_ANALYST', 'Research Analyst',
 'Imports files, resolves duplicates, runs AI research batches, verifies evidence, logs activities, confirms qualifications. The primary daily user.',
 '["import","triage","research","verify","qualify","read","search","reports"]'),

('10000000-0000-0000-0000-000000000002', 'PS_SALES_LEAD', 'Sales Lead',
 'Reviews qualified pool, approves exports, overrides tiers with reason, requests re-research.',
 '["import","triage","research","verify","qualify","export","override","read","search","reports"]'),

('10000000-0000-0000-0000-000000000003', 'PS_ADMIN', 'Administrator',
 'Configures ICP profiles, import templates, disqualification reasons, activity types, user roles. Full system access.',
 '["import","triage","research","verify","qualify","export","override","configure","manage_users","manage_roles","read","search","reports"]'),

('10000000-0000-0000-0000-000000000004', 'PS_VIEWER', 'Viewer',
 'Read-only access to search, timelines, and reports. No editing or workflow actions.',
 '["read","search","reports"]'),

('10000000-0000-0000-0000-000000000005', 'PS_COO', 'COO',
 'Weekly operational dashboard — pipeline throughput, source analytics, data quality. No record-level work.',
 '["read","search","reports","dashboard"]');
