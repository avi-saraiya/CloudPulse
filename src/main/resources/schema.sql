-- Runs on every startup; IF NOT EXISTS leaves existing tables alone.
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT REFERENCES services(id),
    latency_ms DOUBLE PRECISION,
    error_rate DOUBLE PRECISION,
    recorded_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT REFERENCES services(id),
    rule_name VARCHAR(100),
    status VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT REFERENCES alerts(id),
    status VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE
);
