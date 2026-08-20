package com.ct556.signing_workflow_service.client;

import com.ct556.signing_workflow_service.client.dto.CertificateMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateSignResponse;
import com.ct556.signing_workflow_service.client.dto.TimestampTokenResponse;

import java.util.UUID;

public interface CertificateGateway {
    CertificateMetadataResponse getCertificateMetadata(UUID certificateId);
    CertificateSignResponse signPayload(UUID certificateId, String contentBase64);
    TimestampTokenResponse issueTimestampToken(String digestAlgorithm, String messageImprintBase64);
}
