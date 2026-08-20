CREATE TABLE documents (
    id UUID PRIMARY KEY,
    owner_account_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    current_version INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_documents_current_version CHECK (current_version >= 1)
);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT NOT NULL,
    sha256_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_document_versions_document_version
        UNIQUE (document_id, version_number),
    CONSTRAINT uk_document_versions_object_key
        UNIQUE (object_key),
    CONSTRAINT chk_document_versions_version_number CHECK (version_number >= 1),
    CONSTRAINT chk_document_versions_file_size CHECK (file_size >= 0)
);

CREATE INDEX idx_documents_owner_account_id
    ON documents(owner_account_id);

CREATE INDEX idx_documents_created_at
    ON documents(created_at DESC);

CREATE INDEX idx_document_versions_document_id
    ON document_versions(document_id);

CREATE INDEX idx_document_versions_created_at
    ON document_versions(created_at DESC);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();