CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'mfa_type') THEN
        CREATE TYPE mfa_type AS ENUM (
            'TOTP',
            'EMAIL_OTP',
            'SMS_OTP'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'mfa_registration_status') THEN
        CREATE TYPE mfa_registration_status AS ENUM (
            'PENDING',
            'ACTIVE',
            'DISABLED'
        );
    END IF;
END $$;

-- =========================
-- MFA REGISTRATIONS
-- =========================
CREATE TABLE IF NOT EXISTS mfa_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    mfa_type mfa_type NOT NULL,
    secret_value TEXT,
    issuer VARCHAR(255),
    label VARCHAR(255),
    recovery_codes JSONB,
    registration_status mfa_registration_status NOT NULL DEFAULT 'PENDING',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    activated_at TIMESTAMP,
    disabled_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mfa_registrations_user_id
    ON mfa_registrations(user_id);

CREATE INDEX IF NOT EXISTS idx_mfa_registrations_status
    ON mfa_registrations(registration_status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_mfa_registrations_user_primary_active
    ON mfa_registrations(user_id, is_primary)
    WHERE registration_status = 'ACTIVE' AND is_primary = TRUE;

-- =========================
-- STEP-UP VERIFICATION LOG
-- =========================
CREATE TABLE IF NOT EXISTS step_up_verification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id UUID,
    verified BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    verified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_step_up_logs_user_id
    ON step_up_verification_logs(user_id);

CREATE INDEX IF NOT EXISTS idx_step_up_logs_challenge_id
    ON step_up_verification_logs(challenge_id);

CREATE INDEX IF NOT EXISTS idx_step_up_logs_verified_at
    ON step_up_verification_logs(verified_at);