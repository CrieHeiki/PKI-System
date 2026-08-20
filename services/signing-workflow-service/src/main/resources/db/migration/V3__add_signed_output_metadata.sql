ALTER TABLE signing_artifacts
    ADD COLUMN output_object_key VARCHAR(500);

ALTER TABLE signing_artifacts
    ADD COLUMN output_file_name VARCHAR(255);

ALTER TABLE signing_artifacts
    ADD COLUMN output_content_type VARCHAR(255);

ALTER TABLE signing_artifacts
    ADD COLUMN output_file_size BIGINT;

CREATE INDEX idx_signing_artifacts_output_object_key
    ON signing_artifacts(output_object_key);
