CREATE TABLE signing_artifacts (
    id UUID PRIMARY KEY,
    signing_request_id UUID NOT NULL UNIQUE,
    signer_account_id UUID NOT NULL,
    certificate_id UUID NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(255),
    digest_algorithm VARCHAR(50) NOT NULL,
    signature_algorithm VARCHAR(100) NOT NULL,
    document_digest_base64 TEXT NOT NULL,
    signature_value_base64 TEXT NOT NULL,
    signed_payload_length BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_signing_artifacts_request
        FOREIGN KEY (signing_request_id) REFERENCES signing_requests(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_signing_artifacts_request_id ON signing_artifacts(signing_request_id);
CREATE INDEX idx_signing_artifacts_signer_account_id ON signing_artifacts(signer_account_id);
CREATE INDEX idx_signing_artifacts_certificate_id ON signing_artifacts(certificate_id);