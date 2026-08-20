ALTER TABLE certificate_key_materials
    DROP COLUMN IF EXISTS created_at,
    DROP COLUMN IF EXISTS updated_at;

ALTER TABLE trusted_timestamp_authority_key_materials
    DROP COLUMN IF EXISTS created_at,
    DROP COLUMN IF EXISTS updated_at;
