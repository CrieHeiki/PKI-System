DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_signing_artifacts_request_signer_certificate'
    ) THEN
        ALTER TABLE signing_artifacts
            DROP CONSTRAINT fk_signing_artifacts_request_signer_certificate;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_signing_requests_id_signer_certificate'
    ) THEN
        ALTER TABLE signing_requests
            DROP CONSTRAINT uq_signing_requests_id_signer_certificate;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_signing_artifacts_signer_account_id;
DROP INDEX IF EXISTS idx_signing_artifacts_certificate_id;

ALTER TABLE signing_artifacts
    DROP COLUMN IF EXISTS signer_account_id,
    DROP COLUMN IF EXISTS certificate_id;
