CREATE INDEX IF NOT EXISTS idx_verification_reports_signing_request_verified_at
    ON verification_reports(signing_request_id, verified_at DESC);

CREATE INDEX IF NOT EXISTS idx_verification_reports_verified_by_verified_at
    ON verification_reports(verified_by, verified_at DESC);
