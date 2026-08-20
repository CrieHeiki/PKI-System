DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_certificate_requests_source_by_type'
    ) THEN
        ALTER TABLE certificate_requests
            ADD CONSTRAINT chk_certificate_requests_source_by_type
            CHECK (
                (request_type = 'ISSUE' AND source_certificate_id IS NULL)
                OR (request_type IN ('RENEW', 'REVOKE') AND source_certificate_id IS NOT NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_certificate_requests_reason_by_type'
    ) THEN
        ALTER TABLE certificate_requests
            ADD CONSTRAINT chk_certificate_requests_reason_by_type
            CHECK (
                (request_type = 'ISSUE' AND renewal_reason IS NULL AND revocation_reason IS NULL)
                OR (request_type = 'RENEW' AND revocation_reason IS NULL)
                OR (request_type = 'REVOKE' AND ttl IS NULL AND renewal_reason IS NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_certificate_requests_completion_certificate'
    ) THEN
        ALTER TABLE certificate_requests
            ADD CONSTRAINT chk_certificate_requests_completion_certificate
            CHECK (
                (status IN ('PENDING', 'REJECTED') AND certificate_id IS NULL)
                OR (status IN ('ISSUED', 'REVOKED') AND certificate_id IS NOT NULL)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_certificate_requests_status_by_type'
    ) THEN
        ALTER TABLE certificate_requests
            ADD CONSTRAINT chk_certificate_requests_status_by_type
            CHECK (
                (request_type IN ('ISSUE', 'RENEW') AND status IN ('PENDING', 'REJECTED', 'ISSUED'))
                OR (request_type = 'REVOKE' AND status IN ('PENDING', 'REJECTED', 'REVOKED'))
            ) NOT VALID;
    END IF;
END $$;
