ALTER TABLE certificate_requests
    ADD COLUMN IF NOT EXISTS request_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS result_certificate_id UUID,
    ADD COLUMN IF NOT EXISTS reviewed_by_account_id UUID;

UPDATE certificate_requests
SET request_reason = COALESCE(request_reason, renewal_reason, revocation_reason),
    result_certificate_id = COALESCE(result_certificate_id, certificate_id),
    reviewed_by_account_id = COALESCE(reviewed_by_account_id, reviewed_by);

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_reason_by_type;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_completion_certificate;

ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS fk_certificate_requests_certificate;

DROP INDEX IF EXISTS uq_certificate_requests_result_certificate_id;

ALTER TABLE certificate_requests
    DROP COLUMN IF EXISTS renewal_reason,
    DROP COLUMN IF EXISTS revocation_reason,
    DROP COLUMN IF EXISTS certificate_id,
    DROP COLUMN IF EXISTS reviewed_by;

ALTER TABLE certificate_requests
    ADD CONSTRAINT fk_certificate_requests_result_certificate
        FOREIGN KEY (result_certificate_id) REFERENCES certificates(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_certificate_requests_result_certificate_id
    ON certificate_requests(result_certificate_id)
    WHERE result_certificate_id IS NOT NULL;

ALTER TABLE certificate_requests
    ADD CONSTRAINT chk_certificate_requests_reason_by_type
    CHECK (
        (request_type = 'ISSUE' AND request_reason IS NULL)
        OR (request_type = 'RENEW')
        OR (request_type = 'REVOKE' AND ttl IS NULL AND request_reason IS NOT NULL)
    ) NOT VALID;

ALTER TABLE certificate_requests
    ADD CONSTRAINT chk_certificate_requests_completion_certificate
    CHECK (
        (status IN ('PENDING', 'REJECTED') AND result_certificate_id IS NULL)
        OR (status = 'ISSUED' AND result_certificate_id IS NOT NULL)
        OR (status = 'REVOKED' AND request_type = 'REVOKE' AND source_certificate_id IS NOT NULL AND result_certificate_id IS NULL)
    ) NOT VALID;
