ALTER TABLE certificate_requests
    ADD COLUMN IF NOT EXISTS request_payload TEXT;

UPDATE certificate_requests
SET request_payload = COALESCE(
        request_payload,
        CASE request_type
            WHEN 'ISSUE' THEN CAST(
                    jsonb_strip_nulls(
                            jsonb_build_object(
                                    'commonName', common_name,
                                    'organization', organization,
                                    'country', country,
                                    'ttl', ttl
                            )
                    ) AS TEXT
                 )
            WHEN 'RENEW' THEN CAST(
                    jsonb_strip_nulls(
                            jsonb_build_object(
                                    'sourceCertificateId', source_certificate_id,
                                    'organization', organization,
                                    'country', country,
                                    'ttl', ttl
                            )
                    ) AS TEXT
                 )
            WHEN 'REVOKE' THEN CAST(
                    jsonb_strip_nulls(
                            jsonb_build_object(
                                    'sourceCertificateId', source_certificate_id
                            )
                    ) AS TEXT
                 )
            ELSE request_payload
        END
    );

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_source_by_type;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_reason_by_type;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_completion_certificate;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_status_by_type;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS fk_certificate_requests_source_certificate;

DROP INDEX IF EXISTS idx_certificate_requests_source_certificate_id;
DROP INDEX IF EXISTS idx_certificate_requests_account_source_type_status;

ALTER TABLE certificate_requests
    DROP COLUMN IF EXISTS common_name,
    DROP COLUMN IF EXISTS organization,
    DROP COLUMN IF EXISTS country,
    DROP COLUMN IF EXISTS ttl,
    DROP COLUMN IF EXISTS source_certificate_id,
    DROP COLUMN IF EXISTS created_at;

ALTER TABLE certificate_requests
    ADD CONSTRAINT chk_certificate_requests_reason_by_type
    CHECK (
        (request_type IN ('ISSUE', 'RENEW'))
        OR (request_type = 'REVOKE' AND request_reason IS NOT NULL)
    ) NOT VALID;

ALTER TABLE certificate_requests
    ADD CONSTRAINT chk_certificate_requests_completion_certificate
    CHECK (
        (status IN ('PENDING', 'REJECTED') AND result_certificate_id IS NULL)
        OR (status = 'ISSUED' AND request_type IN ('ISSUE', 'RENEW') AND result_certificate_id IS NOT NULL)
        OR (status = 'REVOKED' AND request_type = 'REVOKE' AND result_certificate_id IS NULL)
    ) NOT VALID;

CREATE INDEX IF NOT EXISTS idx_certificate_requests_account_type_status
    ON certificate_requests(account_id, request_type, status);
