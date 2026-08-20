CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'verification_status') THEN
        CREATE TYPE verification_status AS ENUM (
            'PASSED',
            'FAILED',
            'PARTIAL'
        );
    END IF;
END $$;

-- =========================
-- VERIFICATION REPORTS
-- =========================
CREATE TABLE IF NOT EXISTS verification_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID,
    document_version INT,
    verified_by UUID,
    verification_status verification_status NOT NULL,
    signer_certificate_serial VARCHAR(255),
    signer_subject_dn TEXT,
    issuer_dn TEXT,
    trust_source VARCHAR(50) NOT NULL, -- INTERNAL_CA / EXTERNAL_CA
    integrity_valid BOOLEAN NOT NULL DEFAULT FALSE,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    trust_chain_valid BOOLEAN NOT NULL DEFAULT FALSE,
    expiration_valid BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_valid BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_source VARCHAR(50), -- VAULT_CRL / OCSP / CRL
    overall_message TEXT,
    verified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verification_reports_document_id
    ON verification_reports(document_id);

CREATE INDEX IF NOT EXISTS idx_verification_reports_status
    ON verification_reports(verification_status);

CREATE INDEX IF NOT EXISTS idx_verification_reports_verified_at
    ON verification_reports(verified_at);

-- =========================
-- VERIFICATION REPORT DETAILS
-- =========================
CREATE TABLE IF NOT EXISTS verification_report_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES verification_reports(id) ON DELETE CASCADE,
    check_name VARCHAR(100) NOT NULL,
    check_result BOOLEAN NOT NULL,
    detail_message TEXT,
    raw_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verification_report_details_report_id
    ON verification_report_details(report_id);