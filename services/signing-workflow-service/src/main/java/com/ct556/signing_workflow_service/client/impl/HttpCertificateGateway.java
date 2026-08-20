package com.ct556.signing_workflow_service.client.impl;

import com.ct556.signing_workflow_service.client.CertificateGateway;
import com.ct556.signing_workflow_service.client.dto.CertificateMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateSignRequest;
import com.ct556.signing_workflow_service.client.dto.CertificateSignResponse;
import com.ct556.signing_workflow_service.client.dto.TimestampTokenRequest;
import com.ct556.signing_workflow_service.client.dto.TimestampTokenResponse;
import com.ct556.signing_workflow_service.exception.BusinessException;
import com.ct556.signing_workflow_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class HttpCertificateGateway implements CertificateGateway {

    private final RestClient restClient;

    public HttpCertificateGateway(@Value("${services.certification-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public CertificateMetadataResponse getCertificateMetadata(UUID certificateId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/certificates/{certificateId}", certificateId)
                    .retrieve()
                    .body(CertificateMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Certificate not found: " + certificateId);
            }
            throw new BusinessException("Certification service rejected metadata lookup for certificate: " + certificateId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }

    @Override
    public CertificateSignResponse signPayload(UUID certificateId, String contentBase64) {
        try {
            return restClient.post()
                    .uri("/api/v1/internal/certificates/{certificateId}/sign", certificateId)
                    .body(new CertificateSignRequest(contentBase64))
                    .retrieve()
                    .body(CertificateSignResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Certificate not found: " + certificateId);
            }
            throw new BusinessException("Certification service rejected signing request for certificate: " + certificateId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }

    @Override
    public TimestampTokenResponse issueTimestampToken(String digestAlgorithm, String messageImprintBase64) {
        try {
            return restClient.post()
                    .uri("/api/v1/internal/timestamps/token")
                    .body(new TimestampTokenRequest(messageImprintBase64, digestAlgorithm))
                    .retrieve()
                    .body(TimestampTokenResponse.class);
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Certification service rejected timestamp token request");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to certification service", ex);
        }
    }
}
