DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_signing_requests_id_signer_certificate'
    ) THEN
        ALTER TABLE signing_requests
            ADD CONSTRAINT uq_signing_requests_id_signer_certificate
            UNIQUE (id, signer_account_id, certificate_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_signing_artifacts_request_signer_certificate'
    ) THEN
        ALTER TABLE signing_artifacts
            ADD CONSTRAINT fk_signing_artifacts_request_signer_certificate
            FOREIGN KEY (signing_request_id, signer_account_id, certificate_id)
            REFERENCES signing_requests(id, signer_account_id, certificate_id)
            NOT VALID;
    END IF;
END $$;
