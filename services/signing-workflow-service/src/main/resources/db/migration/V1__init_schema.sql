CREATE TABLE signing_requests (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    requester_account_id UUID NOT NULL,
    signer_account_id UUID NOT NULL,
    certificate_id UUID NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NULL,
    signed_at TIMESTAMP NULL,
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE signing_events (
    id UUID PRIMARY KEY,
    signing_request_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_account_id UUID NOT NULL,
    event_time TIMESTAMP NOT NULL,
    detail TEXT NULL,
    CONSTRAINT fk_signing_events_request
        FOREIGN KEY (signing_request_id) REFERENCES signing_requests(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_signing_requests_requester ON signing_requests(requester_account_id);
CREATE INDEX idx_signing_requests_signer ON signing_requests(signer_account_id);
CREATE INDEX idx_signing_requests_status ON signing_requests(status);
CREATE INDEX idx_signing_requests_document ON signing_requests(document_id);

CREATE INDEX idx_signing_events_request_id ON signing_events(signing_request_id);
CREATE INDEX idx_signing_events_actor ON signing_events(actor_account_id);
CREATE INDEX idx_signing_events_type ON signing_events(event_type);