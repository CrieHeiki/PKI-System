ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS common_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS subject_dn VARCHAR(500),
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS vault_transit_key_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS certificate_pem TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS issued_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE trusted_timestamp_authorities
    ADD COLUMN IF NOT EXISTS common_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS subject_dn VARCHAR(500),
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS vault_transit_key_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS certificate_pem TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS issued_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS policy_oid VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ordering_enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS accuracy_seconds INTEGER;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'managed_x509_credentials'
    ) THEN
        UPDATE certificates c
        SET common_name = mx.common_name,
            subject_dn = mx.subject_dn,
            serial_number = mx.serial_number,
            vault_transit_key_name = mx.vault_transit_key_name,
            certificate_pem = mx.certificate_pem,
            status = mx.lifecycle_status,
            issued_at = mx.issued_at,
            expires_at = mx.expires_at,
            revoked_at = mx.revoked_at,
            created_at = mx.created_at,
            updated_at = mx.updated_at
        FROM managed_x509_credentials mx
        WHERE mx.id = c.id
          AND (c.serial_number IS NULL OR c.certificate_pem IS NULL);

        UPDATE trusted_timestamp_authorities t
        SET common_name = mx.common_name,
            subject_dn = mx.subject_dn,
            serial_number = mx.serial_number,
            vault_transit_key_name = mx.vault_transit_key_name,
            certificate_pem = mx.certificate_pem,
            status = mx.lifecycle_status,
            issued_at = mx.issued_at,
            expires_at = mx.expires_at,
            revoked_at = mx.revoked_at,
            created_at = mx.created_at,
            updated_at = mx.updated_at
        FROM managed_x509_credentials mx
        WHERE mx.id = t.id
          AND (t.serial_number IS NULL OR t.certificate_pem IS NULL);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'trusted_timestamp_authority_key_materials'
    ) THEN
        UPDATE trusted_timestamp_authorities t
        SET vault_transit_key_name = km.vault_transit_key_name
        FROM trusted_timestamp_authority_key_materials km
        WHERE km.authority_id = t.id
          AND (t.vault_transit_key_name IS NULL OR t.vault_transit_key_name = '');
    END IF;
END $$;

UPDATE trusted_timestamp_authorities
SET policy_oid = COALESCE(policy_oid, '1.3.6.1.4.1.55556.1.1'),
    ordering_enabled = COALESCE(ordering_enabled, TRUE),
    accuracy_seconds = COALESCE(accuracy_seconds, 1),
    vault_transit_key_name = COALESCE(vault_transit_key_name, 'legacy-tsa-' || id::text)
WHERE policy_oid IS NULL
   OR ordering_enabled IS NULL
   OR accuracy_seconds IS NULL
   OR vault_transit_key_name IS NULL;

ALTER TABLE certificates
    ALTER COLUMN common_name SET NOT NULL,
    ALTER COLUMN subject_dn SET NOT NULL,
    ALTER COLUMN serial_number SET NOT NULL,
    ALTER COLUMN vault_transit_key_name SET NOT NULL,
    ALTER COLUMN certificate_pem SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN issued_at SET NOT NULL,
    ALTER COLUMN expires_at SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE trusted_timestamp_authorities
    ALTER COLUMN common_name SET NOT NULL,
    ALTER COLUMN subject_dn SET NOT NULL,
    ALTER COLUMN serial_number SET NOT NULL,
    ALTER COLUMN vault_transit_key_name SET NOT NULL,
    ALTER COLUMN certificate_pem SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN policy_oid SET NOT NULL,
    ALTER COLUMN ordering_enabled SET NOT NULL,
    ALTER COLUMN accuracy_seconds SET NOT NULL,
    ALTER COLUMN issued_at SET NOT NULL,
    ALTER COLUMN expires_at SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DROP INDEX IF EXISTS idx_managed_x509_credentials_type_status;
DROP INDEX IF EXISTS idx_managed_x509_credentials_issued_at;
DROP INDEX IF EXISTS idx_managed_x509_credentials_expires_at;

ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS fk_certificates_managed_x509_credentials;

ALTER TABLE trusted_timestamp_authorities
    DROP CONSTRAINT IF EXISTS fk_trusted_timestamp_authorities_managed_x509_credentials;

DROP TABLE IF EXISTS managed_x509_credentials;
DROP TABLE IF EXISTS trusted_timestamp_authority_key_materials;

CREATE UNIQUE INDEX IF NOT EXISTS uq_certificates_serial_number
    ON certificates(serial_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_trusted_timestamp_authorities_serial_number
    ON trusted_timestamp_authorities(serial_number);

CREATE INDEX IF NOT EXISTS idx_certificates_status
    ON certificates(status);

CREATE INDEX IF NOT EXISTS idx_certificates_issued_at
    ON certificates(issued_at DESC);

CREATE INDEX IF NOT EXISTS idx_certificates_expires_at
    ON certificates(expires_at);

CREATE INDEX IF NOT EXISTS idx_trusted_timestamp_authorities_status
    ON trusted_timestamp_authorities(status);
