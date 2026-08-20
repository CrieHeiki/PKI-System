ALTER TABLE certificate_requests
    DROP CONSTRAINT IF EXISTS chk_certificate_requests_completion_certificate;

ALTER TABLE certificate_requests
    ADD CONSTRAINT chk_certificate_requests_completion_certificate
    CHECK (
        (status IN ('PENDING', 'REJECTED') AND certificate_id IS NULL)
        OR (status = 'ISSUED' AND certificate_id IS NOT NULL)
        OR (status = 'REVOKED' AND request_type = 'REVOKE' AND source_certificate_id IS NOT NULL AND certificate_id IS NULL)
    ) NOT VALID;
