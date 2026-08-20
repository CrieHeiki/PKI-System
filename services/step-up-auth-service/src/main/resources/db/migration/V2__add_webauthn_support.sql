ALTER TYPE mfa_type ADD VALUE IF NOT EXISTS 'WEBAUTHN';

ALTER TABLE mfa_registrations
    ADD COLUMN IF NOT EXISTS webauthn_credential_id TEXT,
    ADD COLUMN IF NOT EXISTS webauthn_public_key_cose TEXT,
    ADD COLUMN IF NOT EXISTS webauthn_signature_count BIGINT,
    ADD COLUMN IF NOT EXISTS webauthn_aaguid VARCHAR(64),
    ADD COLUMN IF NOT EXISTS webauthn_transports JSONB;

CREATE UNIQUE INDEX IF NOT EXISTS uq_mfa_registrations_webauthn_credential_id
    ON mfa_registrations(webauthn_credential_id)
    WHERE webauthn_credential_id IS NOT NULL;
