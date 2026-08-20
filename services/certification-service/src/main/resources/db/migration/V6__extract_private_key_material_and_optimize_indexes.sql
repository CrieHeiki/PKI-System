CREATE TABLE IF NOT EXISTS certificate_key_materials (
    certificate_id UUID PRIMARY KEY,
    private_key_pem TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_certificate_key_materials_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificates(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trusted_timestamp_authority_key_materials (
    authority_id UUID PRIMARY KEY,
    private_key_pem TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_timestamp_authority_key_materials_authority
        FOREIGN KEY (authority_id) REFERENCES trusted_timestamp_authorities(id)
        ON DELETE CASCADE
);

INSERT INTO certificate_key_materials (certificate_id, private_key_pem, created_at, updated_at)
SELECT id, private_key_pem, created_at, updated_at
FROM certificates
WHERE private_key_pem IS NOT NULL
ON CONFLICT (certificate_id) DO NOTHING;

INSERT INTO trusted_timestamp_authority_key_materials (authority_id, private_key_pem, created_at, updated_at)
SELECT id, private_key_pem, created_at, updated_at
FROM trusted_timestamp_authorities
WHERE private_key_pem IS NOT NULL
ON CONFLICT (authority_id) DO NOTHING;

ALTER TABLE certificates
    DROP COLUMN IF EXISTS private_key_pem;

ALTER TABLE trusted_timestamp_authorities
    DROP COLUMN IF EXISTS private_key_pem;

CREATE INDEX IF NOT EXISTS idx_certificate_requests_type_status_requested_at
    ON certificate_requests(request_type, status, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_certificate_requests_account_source_type_status
    ON certificate_requests(account_id, source_certificate_id, request_type, status);
