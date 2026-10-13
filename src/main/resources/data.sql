-- Seeds the three simulated services. Runs on every startup right after schema.sql.
-- ON CONFLICT DO NOTHING: if a service with that (unique) name already exists, skip it,
-- so restarting the app never creates duplicates or resets data.
INSERT INTO services (name, status) VALUES ('Payment API', 'HEALTHY') ON CONFLICT DO NOTHING;
INSERT INTO services (name, status) VALUES ('Authentication API', 'HEALTHY') ON CONFLICT DO NOTHING;
INSERT INTO services (name, status) VALUES ('Order API', 'HEALTHY') ON CONFLICT DO NOTHING;
