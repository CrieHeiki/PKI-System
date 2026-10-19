CREATE TABLE verification_reports (
    id UUID NOT NULL,
    document_id UUID,
    document_version INT,
    signing_request_id UUID,
    certificate_id UUID,
    verified_by_account_id UUID,
    verification_status VARCHAR(30) NOT NULL,
    signer_certificate_serial VARCHAR(255),
    signer_subject_dn TEXT,
    issuer_dn TEXT,
    trust_source VARCHAR(50) NOT NULL,
    integrity_valid BOOLEAN NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    trust_chain_valid BOOLEAN NOT NULL,
    expiration_valid BOOLEAN NOT NULL,
    revocation_valid BOOLEAN NOT NULL,
    overall_message TEXT,
    verified_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE verification_report_details (
    id UUID NOT NULL,
    report_id UUID NOT NULL,
    check_name VARCHAR(100) NOT NULL,
    check_result BOOLEAN NOT NULL,
    detail_message TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (report_id) REFERENCES verification_reports(id) ON DELETE CASCADE
);
