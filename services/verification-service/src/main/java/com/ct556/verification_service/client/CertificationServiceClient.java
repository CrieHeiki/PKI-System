package com.ct556.verification_service.client;

import com.ct556.verification_service.client.dto.InternalCertificateMetadataResponse;
import com.ct556.verification_service.client.dto.InternalTimestampAuthorityMetadataResponse;
import com.ct556.verification_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class CertificationServiceClient {

    private final RestClient restClient;

    public CertificationServiceClient(@Value("${services.certification-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public InternalCertificateMetadataResponse getCertificateMetadata(UUID certificateId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/certificates/{certificateId}", certificateId)
                    .retrieve()
                    .body(InternalCertificateMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Certificate not found: " + certificateId);
            }
            throw new IllegalStateException("Certification service rejected certificate lookup for: " + certificateId, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }

    public InternalCertificateMetadataResponse getCertificateMetadataBySerial(String serialNumber) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/certificates/serial/{serialNumber}", serialNumber)
                    .retrieve()
                    .body(InternalCertificateMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Certificate not found with serial: " + serialNumber);
            }
            throw new IllegalStateException("Certification service rejected certificate serial lookup for: " + serialNumber, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }

    public InternalTimestampAuthorityMetadataResponse getTimestampAuthorityMetadataBySerial(String serialNumber) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/timestamps/authority/serial/{serialNumber}", serialNumber)
                    .retrieve()
                    .body(InternalTimestampAuthorityMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Timestamp authority not found with serial: " + serialNumber);
            }
            throw new IllegalStateException("Certification service rejected timestamp authority lookup for: " + serialNumber, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }
}
