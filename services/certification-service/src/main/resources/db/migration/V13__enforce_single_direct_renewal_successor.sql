CREATE UNIQUE INDEX IF NOT EXISTS uq_certificates_renewed_from_certificate_id
    ON certificates(renewed_from_certificate_id)
    WHERE renewed_from_certificate_id IS NOT NULL;
