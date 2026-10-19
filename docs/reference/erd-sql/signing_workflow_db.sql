CREATE TABLE signing_requests (
    id UUID NOT NULL,
    document_id UUID NOT NULL,
    requester_account_id UUID NOT NULL,
    signer_account_id UUID NOT NULL,
    certificate_id UUID,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    signed_at TIMESTAMP,
    note TEXT,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE signing_events (
    id UUID NOT NULL,
    signing_request_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_account_id UUID NOT NULL,
    event_time TIMESTAMP NOT NULL,
    detail TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (signing_request_id) REFERENCES signing_requests(id) ON DELETE CASCADE
);

CREATE TABLE signing_artifacts (
    id UUID NOT NULL,
    signing_request_id UUID NOT NULL,
    digest_algorithm VARCHAR(50) NOT NULL,
    signature_algorithm VARCHAR(100) NOT NULL,
    document_digest_base64 TEXT NOT NULL,
    signature_value_base64 TEXT NOT NULL,
    output_object_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (signing_request_id),
    FOREIGN KEY (signing_request_id) REFERENCES signing_requests(id) ON DELETE CASCADE
);
