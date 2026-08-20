package com.ct556.signing_workflow_service.service;

import com.ct556.signing_workflow_service.dto.response.InternalSigningArtifactResponse;
import com.ct556.signing_workflow_service.entity.SigningArtifact;
import com.ct556.signing_workflow_service.entity.SigningRequest;
import com.ct556.signing_workflow_service.exception.BusinessException;
import com.ct556.signing_workflow_service.exception.ResourceNotFoundException;
import com.ct556.signing_workflow_service.repository.SigningArtifactRepository;
import com.ct556.signing_workflow_service.repository.SigningRequestRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InternalSigningArtifactQueryService {

    private final SigningRequestRepository signingRequestRepository;
    private final SigningArtifactRepository signingArtifactRepository;

    public InternalSigningArtifactQueryService(SigningRequestRepository signingRequestRepository,
                                               SigningArtifactRepository signingArtifactRepository) {
        this.signingRequestRepository = signingRequestRepository;
        this.signingArtifactRepository = signingArtifactRepository;
    }

    public InternalSigningArtifactResponse getArtifactByRequestId(UUID requestId) {
        SigningRequest request = signingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing request not found: " + requestId));

        SigningArtifact artifact = signingArtifactRepository.findBySigningRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing artifact not found for request: " + requestId));

        if (artifact.getOutputObjectKey() == null || artifact.getOutputObjectKey().isBlank()) {
            throw new BusinessException("Signed output is not available for request: " + requestId);
        }

        return InternalSigningArtifactResponse.builder()
                .signingRequestId(request.getId())
                .documentId(request.getDocumentId())
                .status(request.getStatus().name())
                .certificateId(request.getCertificateId())
                .fileName(artifact.getFileName())
                .contentType(artifact.getContentType())
                .digestAlgorithm(artifact.getDigestAlgorithm())
                .signatureAlgorithm(artifact.getSignatureAlgorithm())
                .outputObjectKey(artifact.getOutputObjectKey())
                .outputFileName(resolveStoredSignedOutputFileName(
                        artifact.getOutputObjectKey(),
                        artifact.getOutputFileName(),
                        artifact.getFileName()
                ))
                .outputContentType(artifact.getOutputContentType())
                .outputFileSize(artifact.getOutputFileSize())
                .build();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String resolveStoredSignedOutputFileName(String outputObjectKey,
                                                     String outputFileName,
                                                     String fallbackFileName) {
        return firstNonBlank(extractLeafName(outputObjectKey), outputFileName, fallbackFileName, "signed-output.bin");
    }

    private String extractLeafName(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace("\\", "/").trim();
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
