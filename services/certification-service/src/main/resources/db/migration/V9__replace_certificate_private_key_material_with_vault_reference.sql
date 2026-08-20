ALTER TABLE certificate_key_materials
    ADD COLUMN IF NOT EXISTS vault_transit_key_name VARCHAR(255);

UPDATE certificate_key_materials
SET vault_transit_key_name = 'legacy-certificate-' || certificate_id::text
WHERE vault_transit_key_name IS NULL;

ALTER TABLE certificate_key_materials
    ALTER COLUMN vault_transit_key_name SET NOT NULL;

ALTER TABLE certificate_key_materials
    DROP COLUMN IF EXISTS private_key_pem;
