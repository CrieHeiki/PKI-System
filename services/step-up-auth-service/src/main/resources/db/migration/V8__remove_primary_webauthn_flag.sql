DROP INDEX IF EXISTS uq_mfa_registrations_user_primary_active;

ALTER TABLE mfa_registrations
    DROP COLUMN IF EXISTS is_primary;
