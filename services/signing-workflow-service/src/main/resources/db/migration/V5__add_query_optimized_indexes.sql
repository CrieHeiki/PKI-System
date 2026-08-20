CREATE INDEX IF NOT EXISTS idx_signing_requests_requester_created_at
    ON signing_requests(requester_account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_signing_requests_signer_status_created_at
    ON signing_requests(signer_account_id, status, created_at DESC);
