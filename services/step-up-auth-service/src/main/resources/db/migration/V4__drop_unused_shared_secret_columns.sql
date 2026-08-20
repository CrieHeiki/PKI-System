ALTER TABLE mfa_registrations
    DROP COLUMN IF EXISTS secret_value,
    DROP COLUMN IF EXISTS recovery_codes;
