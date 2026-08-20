ALTER TABLE verification_reports
    ADD COLUMN IF NOT EXISTS signing_request_id UUID,
    ADD COLUMN IF NOT EXISTS certificate_id UUID,
    ADD COLUMN IF NOT EXISTS signed_object_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS signed_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS digest_algorithm VARCHAR(100),
    ADD COLUMN IF NOT EXISTS signature_algorithm VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_verification_reports_signing_request_id
    ON verification_reports(signing_request_id);

CREATE INDEX IF NOT EXISTS idx_verification_reports_certificate_id
    ON verification_reports(certificate_id);

CREATE INDEX IF NOT EXISTS idx_verification_reports_signed_object_key
    ON verification_reports(signed_object_key);
