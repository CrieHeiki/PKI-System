CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    vault_certificate_path VARCHAR(500),
    certificate_pem TEXT NOT NULL,
    issuing_ca_pem TEXT,
    private_key_pem TEXT,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_certificates_account_id ON certificates(account_id);
CREATE INDEX idx_certificates_status ON certificates(status);
CREATE INDEX idx_certificates_issued_at ON certificates(issued_at DESC);
CREATE INDEX idx_certificates_expires_at ON certificates(expires_at);

