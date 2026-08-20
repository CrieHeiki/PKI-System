ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS chk_documents_current_version,
    DROP COLUMN IF EXISTS current_version;
