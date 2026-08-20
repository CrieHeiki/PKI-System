DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'webauthn_recovery_request_status') THEN
        CREATE TYPE webauthn_recovery_request_status AS ENUM (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'CANCELLED',
            'COMPLETED'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS webauthn_recovery_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status webauthn_recovery_request_status NOT NULL,
    reason VARCHAR(1000),
    decision_note VARCHAR(1000),
    reviewed_by_admin_id UUID,
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    completed_at TIMESTAMP,
    recovery_ticket VARCHAR(255),
    recovery_ticket_issued_at TIMESTAMP,
    recovery_ticket_expires_at TIMESTAMP,
    completed_registration_id UUID,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webauthn_recovery_requests_user_id
    ON webauthn_recovery_requests(user_id);

CREATE INDEX IF NOT EXISTS idx_webauthn_recovery_requests_status
    ON webauthn_recovery_requests(status);
