CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================
-- AUDIT EVENT INDEX
-- =========================
CREATE TABLE IF NOT EXISTS audit_event_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    immudb_tx_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    actor_id UUID,
    actor_role VARCHAR(50),
    target_id UUID,
    resource_type VARCHAR(100),
    resource_id UUID,
    correlation_id VARCHAR(255),
    trace_id VARCHAR(255),
    event_time TIMESTAMP NOT NULL,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_event_type
    ON audit_event_index(event_type);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_actor_id
    ON audit_event_index(actor_id);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_resource
    ON audit_event_index(resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_event_time
    ON audit_event_index(event_time);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_correlation_id
    ON audit_event_index(correlation_id);

-- =========================
-- OPTIONAL: AUDIT EVENT PAYLOAD SNAPSHOT
-- =========================
CREATE TABLE IF NOT EXISTS audit_event_payloads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_event_id UUID NOT NULL REFERENCES audit_event_index(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_event_payloads_audit_event_id
    ON audit_event_payloads(audit_event_id);