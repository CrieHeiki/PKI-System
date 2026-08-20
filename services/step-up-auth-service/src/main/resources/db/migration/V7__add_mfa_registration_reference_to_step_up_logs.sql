ALTER TABLE step_up_verification_logs
    ADD COLUMN IF NOT EXISTS mfa_registration_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_step_up_logs_mfa_registration'
    ) THEN
        ALTER TABLE step_up_verification_logs
            ADD CONSTRAINT fk_step_up_logs_mfa_registration
            FOREIGN KEY (mfa_registration_id)
            REFERENCES mfa_registrations(id)
            ON DELETE SET NULL
            NOT VALID;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_step_up_logs_mfa_registration_id
    ON step_up_verification_logs(mfa_registration_id);
