CREATE TABLE trusted_timestamp_authorities (
    id UUID PRIMARY KEY,
    common_name VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    certificate_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trusted_timestamp_authorities_status
    ON trusted_timestamp_authorities(status);
