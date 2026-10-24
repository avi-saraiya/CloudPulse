-- The three simulated services. ON CONFLICT DO NOTHING skips names that already exist (name is UNIQUE).
INSERT INTO services (name, status) VALUES
    ('Payment API', 'HEALTHY'),
    ('Authentication API', 'HEALTHY'),
    ('Order API', 'HEALTHY')
ON CONFLICT DO NOTHING;
