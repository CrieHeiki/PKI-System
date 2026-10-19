CREATE TABLE ref_accounts (
    id UUID NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE certificates (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(255) NOT NULL,
    vault_transit_key_name VARCHAR(255) NOT NULL,
    certificate_pem TEXT NOT NULL,
    issuing_ca_pem TEXT,
    status VARCHAR(30) NOT NULL,
    renewed_from_certificate_id UUID,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (serial_number),
    UNIQUE (renewed_from_certificate_id),
    FOREIGN KEY (account_id) REFERENCES ref_accounts(id),
    FOREIGN KEY (renewed_from_certificate_id) REFERENCES certificates(id)
);

CREATE TABLE trusted_timestamp_authorities (
    id UUID NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(255) NOT NULL,
    vault_transit_key_name VARCHAR(255) NOT NULL,
    certificate_pem TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    policy_oid VARCHAR(100) NOT NULL,
    ordering_enabled BOOLEAN NOT NULL,
    accuracy_seconds INTEGER NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (serial_number)
);

CREATE TABLE certificate_requests (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    request_payload TEXT,
    request_reason VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    result_certificate_id UUID,
    requested_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by_account_id UUID,
    rejection_reason VARCHAR(1000),
    PRIMARY KEY (id),
    FOREIGN KEY (account_id) REFERENCES ref_accounts(id),
    FOREIGN KEY (reviewed_by_account_id) REFERENCES ref_accounts(id),
    FOREIGN KEY (result_certificate_id) REFERENCES certificates(id)
);
