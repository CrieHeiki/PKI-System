package com.ct556.verification_service.client;

import com.ct556.verification_service.client.dto.InternalSigningArtifactResponse;
import com.ct556.verification_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class SigningWorkflowClient {

    private final RestClient restClient;

    public SigningWorkflowClient(@Value("${services.signing-workflow-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public InternalSigningArtifactResponse getSigningArtifact(UUID requestId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/signing-requests/{requestId}/artifact", requestId)
                    .retrieve()
                    .body(InternalSigningArtifactResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Signing artifact not found for request: " + requestId);
            }
            throw new IllegalStateException("Signing workflow service rejected artifact lookup for request: " + requestId, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to signing workflow service", ex);
        }
    }
}
