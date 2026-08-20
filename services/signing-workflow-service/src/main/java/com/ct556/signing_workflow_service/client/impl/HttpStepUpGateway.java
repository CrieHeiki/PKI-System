package com.ct556.signing_workflow_service.client.impl;

import com.ct556.signing_workflow_service.client.StepUpGateway;
import com.ct556.signing_workflow_service.client.dto.ConsumeStepUpVerificationRequest;
import com.ct556.signing_workflow_service.client.dto.InternalStepUpVerificationResponse;
import com.ct556.signing_workflow_service.dto.response.ApiResponse;
import com.ct556.signing_workflow_service.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class HttpStepUpGateway implements StepUpGateway {

    private final RestClient restClient;

    public HttpStepUpGateway(@Value("${services.step-up-auth-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public InternalStepUpVerificationResponse consumeVerification(String verificationToken,
                                                                  UUID userId,
                                                                  String action,
                                                                  String targetType,
                                                                  UUID targetId,
                                                                  boolean consume) {
        try {
            ApiResponse<InternalStepUpVerificationResponse> response = restClient.post()
                    .uri("/api/v1/internal/step-up/verifications/consume")
                    .body(new ConsumeStepUpVerificationRequest(
                            verificationToken,
                            userId,
                            action,
                            targetType,
                            targetId,
                            consume
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                throw new BusinessException("Step-up auth service returned empty verification response");
            }

            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Step-up auth service rejected verification proof");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to step-up auth service", ex);
        }
    }
}
