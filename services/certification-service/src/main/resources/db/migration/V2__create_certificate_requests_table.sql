CREATE TABLE certificate_requests (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    country VARCHAR(10),
    ttl VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    certificate_id UUID UNIQUE,
    requested_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_certificate_requests_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificates(id)
);

CREATE INDEX idx_certificate_requests_account_id
    ON certificate_requests(account_id);

CREATE INDEX idx_certificate_requests_status
    ON certificate_requests(status);

CREATE INDEX idx_certificate_requests_requested_at
    ON certificate_requests(requested_at DESC);