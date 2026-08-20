package com.ct556.signing_workflow_service.client.impl;

import com.ct556.signing_workflow_service.client.AccountGateway;
import com.ct556.signing_workflow_service.client.dto.AccountMetadataResponse;
import com.ct556.signing_workflow_service.exception.BusinessException;
import com.ct556.signing_workflow_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class HttpAccountGateway implements AccountGateway {

    private final RestClient restClient;

    public HttpAccountGateway(@Value("${services.account-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public AccountMetadataResponse getAccountMetadata(UUID accountId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/accounts/{accountId}", accountId)
                    .retrieve()
                    .body(AccountMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Account not found: " + accountId);
            }
            throw new BusinessException("Account service rejected metadata lookup for account: " + accountId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to account service", ex);
        }
    }
}
