package com.ct556.signing_workflow_service.client;

import com.ct556.signing_workflow_service.client.dto.AccountMetadataResponse;

import java.util.UUID;

public interface AccountGateway {
    AccountMetadataResponse getAccountMetadata(UUID accountId);
}
