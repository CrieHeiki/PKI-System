ALTER TABLE certificates
    ADD COLUMN renewed_from_certificate_id UUID;

ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_renewed_from
        FOREIGN KEY (renewed_from_certificate_id) REFERENCES certificates(id);

CREATE INDEX idx_certificates_renewed_from_certificate_id
    ON certificates(renewed_from_certificate_id);

ALTER TABLE certificate_requests
    ADD COLUMN request_type VARCHAR(30) NOT NULL DEFAULT 'ISSUE';

ALTER TABLE certificate_requests
    ADD COLUMN source_certificate_id UUID;

ALTER TABLE certificate_requests
    ADD COLUMN renewal_reason VARCHAR(1000);

ALTER TABLE certificate_requests
    ADD CONSTRAINT fk_certificate_requests_source_certificate
        FOREIGN KEY (source_certificate_id) REFERENCES certificates(id);

CREATE INDEX idx_certificate_requests_request_type
    ON certificate_requests(request_type);

CREATE INDEX idx_certificate_requests_source_certificate_id
    ON certificate_requests(source_certificate_id);