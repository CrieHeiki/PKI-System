ALTER TABLE trusted_timestamp_authority_key_materials
    ADD COLUMN IF NOT EXISTS vault_transit_key_name VARCHAR(255);

UPDATE trusted_timestamp_authority_key_materials
SET vault_transit_key_name = 'legacy-tsa-' || authority_id::text
WHERE vault_transit_key_name IS NULL;

ALTER TABLE trusted_timestamp_authority_key_materials
    ALTER COLUMN vault_transit_key_name SET NOT NULL;

ALTER TABLE trusted_timestamp_authority_key_materials
    DROP COLUMN IF EXISTS private_key_pem;

UPDATE trusted_timestamp_authorities
SET status = 'ROTATED',
    updated_at = NOW()
WHERE status = 'ACTIVE'
  AND id IN (
      SELECT authority_id
      FROM trusted_timestamp_authority_key_materials
      WHERE vault_transit_key_name LIKE 'legacy-tsa-%'
  );
