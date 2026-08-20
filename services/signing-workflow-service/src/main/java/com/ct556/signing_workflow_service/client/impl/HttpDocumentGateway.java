package com.ct556.signing_workflow_service.client.impl;

import com.ct556.signing_workflow_service.client.DocumentGateway;
import com.ct556.signing_workflow_service.client.dto.DocumentMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentPayloadResponse;
import com.ct556.signing_workflow_service.client.dto.StoreSignedOutputRequest;
import com.ct556.signing_workflow_service.client.dto.StoredSignedOutputResponse;
import com.ct556.signing_workflow_service.exception.BusinessException;
import com.ct556.signing_workflow_service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class HttpDocumentGateway implements DocumentGateway {

    private final RestClient restClient;

    public HttpDocumentGateway(@Value("${services.document-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public DocumentMetadataResponse getDocumentMetadata(UUID documentId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/documents/{documentId}/metadata", documentId)
                    .retrieve()
                    .body(DocumentMetadataResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Document not found: " + documentId);
            }
            throw new BusinessException("Document service rejected metadata lookup for document: " + documentId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to document service", ex);
        }
    }

    @Override
    public DocumentPayloadResponse getDocumentForSigning(UUID documentId) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/documents/{documentId}/content-for-signing", documentId)
                    .retrieve()
                    .body(DocumentPayloadResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Document not found: " + documentId);
            }
            throw new BusinessException("Document service rejected content lookup for document: " + documentId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to document service", ex);
        }
    }

    @Override
    public byte[] getSignedOutput(String objectKey) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/internal/documents/signed-output")
                            .queryParam("objectKey", objectKey)
                            .build())
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Signed output not found for objectKey: " + objectKey);
            }
            throw new BusinessException("Document service rejected signed output download for objectKey: " + objectKey);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to document service", ex);
        }
    }

    @Override
    public StoredSignedOutputResponse storeSignedOutput(UUID documentId,
                                                        UUID signingRequestId,
                                                        String fileName,
                                                        String contentType,
                                                        String contentBase64) {
        try {
            return restClient.post()
                    .uri("/api/v1/internal/documents/{documentId}/signed-output", documentId)
                    .body(new StoreSignedOutputRequest(
                            signingRequestId,
                            fileName,
                            contentType,
                            contentBase64
                    ))
                    .retrieve()
                    .body(StoredSignedOutputResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Document not found: " + documentId);
            }
            throw new BusinessException("Document service rejected signed output storage for document: " + documentId);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Cannot connect to document service", ex);
        }
    }
}
