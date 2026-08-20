DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_webauthn_recovery_completed_registration'
    ) THEN
        ALTER TABLE webauthn_recovery_requests
            ADD CONSTRAINT fk_webauthn_recovery_completed_registration
            FOREIGN KEY (completed_registration_id)
            REFERENCES mfa_registrations(id)
            ON DELETE SET NULL
            NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_webauthn_recovery_completion'
    ) THEN
        ALTER TABLE webauthn_recovery_requests
            ADD CONSTRAINT chk_webauthn_recovery_completion
            CHECK (
                (status = 'COMPLETED' AND completed_at IS NOT NULL AND completed_registration_id IS NOT NULL)
                OR (status <> 'COMPLETED' AND completed_at IS NULL AND completed_registration_id IS NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_webauthn_recovery_review'
    ) THEN
        ALTER TABLE webauthn_recovery_requests
            ADD CONSTRAINT chk_webauthn_recovery_review
            CHECK (
                (status IN ('APPROVED', 'REJECTED') AND reviewed_at IS NOT NULL AND reviewed_by_admin_id IS NOT NULL)
                OR (status NOT IN ('APPROVED', 'REJECTED'))
            ) NOT VALID;
    END IF;
END $$;
