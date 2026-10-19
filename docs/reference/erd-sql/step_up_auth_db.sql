CREATE TABLE mfa_registrations (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    mfa_type VARCHAR(30) NOT NULL,
    webauthn_credential_id TEXT,
    webauthn_public_key_cose TEXT,
    webauthn_signature_count BIGINT,
    registration_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    activated_at TIMESTAMP,
    disabled_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE step_up_verification_logs (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    mfa_registration_id UUID,
    action VARCHAR(100) NOT NULL,
    verified BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    verified_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (mfa_registration_id) REFERENCES mfa_registrations(id) ON DELETE SET NULL
);

CREATE TABLE webauthn_recovery_requests (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    reviewed_by_admin_account_id UUID,
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    completed_at TIMESTAMP,
    recovery_ticket VARCHAR(255),
    recovery_ticket_issued_at TIMESTAMP,
    recovery_ticket_expires_at TIMESTAMP,
    completed_mfa_registration_id UUID,
    PRIMARY KEY (id),
    FOREIGN KEY (completed_mfa_registration_id) REFERENCES mfa_registrations(id) ON DELETE SET NULL
);
