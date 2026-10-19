CREATE TABLE audit_event_index (
    id UUID NOT NULL,
    immudb_tx_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    actor_account_id UUID,
    actor_role VARCHAR(50),
    resource_type VARCHAR(100),
    resource_id UUID,
    correlation_id VARCHAR(255),
    trace_id VARCHAR(255),
    event_time TIMESTAMP NOT NULL,
    summary TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE audit_event_payloads (
    id UUID NOT NULL,
    audit_event_id UUID NOT NULL,
    payload JSONB NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (audit_event_id) REFERENCES audit_event_index(id) ON DELETE CASCADE
);
