CREATE UNIQUE INDEX IF NOT EXISTS uq_mfa_registrations_user_active_webauthn
    ON mfa_registrations(user_id)
    WHERE registration_status = 'ACTIVE' AND mfa_type = 'WEBAUTHN';

CREATE INDEX IF NOT EXISTS idx_webauthn_recovery_requests_user_status_requested_at
    ON webauthn_recovery_requests(user_id, status, requested_at DESC);
