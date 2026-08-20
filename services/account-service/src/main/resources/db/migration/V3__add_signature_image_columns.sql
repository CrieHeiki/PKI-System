ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS signature_image BYTEA,
    ADD COLUMN IF NOT EXISTS signature_image_content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS signature_image_updated_at TIMESTAMP;
