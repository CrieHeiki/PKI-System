CREATE TABLE managed_x509_credentials (
    id UUID PRIMARY KEY,
    credential_type VARCHAR(50) NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    vault_transit_key_name VARCHAR(255) NOT NULL,
    certificate_pem TEXT NOT NULL,
    lifecycle_status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_managed_x509_credentials_type_status
    ON managed_x509_credentials(credential_type, lifecycle_status);

CREATE INDEX idx_managed_x509_credentials_issued_at
    ON managed_x509_credentials(issued_at DESC);

CREATE INDEX idx_managed_x509_credentials_expires_at
    ON managed_x509_credentials(expires_at);

INSERT INTO managed_x509_credentials (
    id,
    credential_type,
    common_name,
    subject_dn,
    serial_number,
    vault_transit_key_name,
    certificate_pem,
    lifecycle_status,
    issued_at,
    expires_at,
    revoked_at,
    created_at,
    updated_at
)
SELECT
    c.id,
    'USER_CERTIFICATE',
    c.common_name,
    c.subject_dn,
    c.serial_number,
    c.vault_transit_key_name,
    c.certificate_pem,
    c.status,
    c.issued_at,
    c.expires_at,
    c.revoked_at,
    c.created_at,
    c.updated_at
FROM certificates c;

INSERT INTO managed_x509_credentials (
    id,
    credential_type,
    common_name,
    subject_dn,
    serial_number,
    vault_transit_key_name,
    certificate_pem,
    lifecycle_status,
    issued_at,
    expires_at,
    revoked_at,
    created_at,
    updated_at
)
SELECT
    t.id,
    'TIMESTAMP_AUTHORITY',
    t.common_name,
    t.subject_dn,
    t.serial_number,
    COALESCE(km.vault_transit_key_name, 'legacy-tsa-' || t.id::text),
    t.certificate_pem,
    t.status,
    t.issued_at,
    t.expires_at,
    t.revoked_at,
    t.created_at,
    t.updated_at
FROM trusted_timestamp_authorities t
LEFT JOIN trusted_timestamp_authority_key_materials km
    ON km.authority_id = t.id;

ALTER TABLE trusted_timestamp_authorities
    ADD COLUMN policy_oid VARCHAR(100),
    ADD COLUMN ordering_enabled BOOLEAN,
    ADD COLUMN accuracy_seconds INTEGER;

UPDATE trusted_timestamp_authorities
SET policy_oid = '1.3.6.1.4.1.55556.1.1',
    ordering_enabled = TRUE,
    accuracy_seconds = 1
WHERE policy_oid IS NULL
   OR ordering_enabled IS NULL
   OR accuracy_seconds IS NULL;

ALTER TABLE trusted_timestamp_authorities
    ALTER COLUMN policy_oid SET NOT NULL,
    ALTER COLUMN ordering_enabled SET NOT NULL,
    ALTER COLUMN accuracy_seconds SET NOT NULL;

DROP INDEX IF EXISTS idx_certificates_status;
DROP INDEX IF EXISTS idx_certificates_issued_at;
DROP INDEX IF EXISTS idx_certificates_expires_at;
DROP INDEX IF EXISTS idx_trusted_timestamp_authorities_status;

ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS certificates_serial_number_key;

ALTER TABLE trusted_timestamp_authorities
    DROP CONSTRAINT IF EXISTS trusted_timestamp_authorities_serial_number_key;

ALTER TABLE certificates
    DROP COLUMN common_name,
    DROP COLUMN subject_dn,
    DROP COLUMN serial_number,
    DROP COLUMN vault_transit_key_name,
    DROP COLUMN certificate_pem,
    DROP COLUMN status,
    DROP COLUMN issued_at,
    DROP COLUMN expires_at,
    DROP COLUMN revoked_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

ALTER TABLE trusted_timestamp_authorities
    DROP COLUMN common_name,
    DROP COLUMN subject_dn,
    DROP COLUMN serial_number,
    DROP COLUMN certificate_pem,
    DROP COLUMN status,
    DROP COLUMN issued_at,
    DROP COLUMN expires_at,
    DROP COLUMN revoked_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_managed_x509_credentials
        FOREIGN KEY (id)
        REFERENCES managed_x509_credentials(id)
        ON DELETE CASCADE;

ALTER TABLE trusted_timestamp_authorities
    ADD CONSTRAINT fk_trusted_timestamp_authorities_managed_x509_credentials
        FOREIGN KEY (id)
        REFERENCES managed_x509_credentials(id)
        ON DELETE CASCADE;

DROP TABLE trusted_timestamp_authority_key_materials;
