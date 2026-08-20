ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS vault_transit_key_name VARCHAR(255);

UPDATE certificates c
SET vault_transit_key_name = ckm.vault_transit_key_name
FROM certificate_key_materials ckm
WHERE c.id = ckm.certificate_id
  AND c.vault_transit_key_name IS NULL;

UPDATE certificates
SET vault_transit_key_name = 'legacy-certificate-' || id::text
WHERE vault_transit_key_name IS NULL;

ALTER TABLE certificates
    ALTER COLUMN vault_transit_key_name SET NOT NULL;

DROP TABLE IF EXISTS certificate_key_materials;
