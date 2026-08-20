CREATE INDEX IF NOT EXISTS idx_audit_event_index_actor_event_time
    ON audit_event_index(actor_id, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_resource_event_time
    ON audit_event_index(resource_type, resource_id, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_audit_event_index_correlation_event_time
    ON audit_event_index(correlation_id, event_time DESC);
