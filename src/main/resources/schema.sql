-- Runs on every startup (spring.sql.init.mode=always), so every statement uses IF NOT EXISTS
-- to leave existing tables and data untouched.
-- services table - service id as the primary key, contains service name, status and time of creation
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- metrics table - metric id as primary key, service_id foreign key, latency, error (fractional) and timestamp
CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    latency_ms DOUBLE PRECISION NOT NULL CHECK (latency_ms >= 0),
    error_rate DOUBLE PRECISION NOT NULL CHECK (error_rate >= 0.0 AND error_rate <= 1.0),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- alerts table -alert id primary key, service id foreign key, rule, status, creation and resolution time stamps
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    rule_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    observed_value DOUBLE PRECISION NOT NULL,
    threshold_value DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- incident id as primary key, has alert_id as foreign key instead of service id because an incident is created in respones to an alert
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Indexes being used for faster lookups
-- This one optimizes for latest metrics for a given service
CREATE INDEX IF NOT EXISTS idx_metrics_service_recorded ON metrics(service_id, recorded_at DESC);
-- This one optimizes for an OPEN as opposed to a RESOLVED alert status
CREATE INDEX IF NOT EXISTS idx_alerts_status_created ON alerts(status, created_at DESC);
-- This one optimizes for ACKNOWLEDGED and OPEN status incidents as opposed to already RESOLVED ones
CREATE INDEX IF NOT EXISTS idx_incidents_status_created ON incidents(status, created_at DESC);