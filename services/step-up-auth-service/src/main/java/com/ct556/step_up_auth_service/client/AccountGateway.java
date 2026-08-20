package com.ct556.step_up_auth_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AccountGateway {

    private final RestClient restClient;

    public AccountGateway(@Value("${services.account-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public InternalAccountResponse getInternalAccount(UUID accountId) {
        return restClient.get()
                .uri("/api/v1/internal/accounts/{accountId}", accountId)
                .retrieve()
                .body(InternalAccountResponse.class);
    }
}
