package com.ct556.signing_workflow_service.mapper;

import com.ct556.signing_workflow_service.dto.response.SigningArtifactResponse;
import com.ct556.signing_workflow_service.dto.response.SigningEventResponse;
import com.ct556.signing_workflow_service.dto.response.SigningRequestResponse;
import com.ct556.signing_workflow_service.entity.SigningArtifact;
import com.ct556.signing_workflow_service.entity.SigningEvent;
import com.ct556.signing_workflow_service.entity.SigningRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SigningWorkflowMapper {

    public SigningRequestResponse toResponse(SigningRequest request,
                                             List<SigningEvent> events,
                                             SigningArtifact artifact) {
        return SigningRequestResponse.builder()
                .id(request.getId())
                .documentId(request.getDocumentId())
                .requesterAccountId(request.getRequesterAccountId())
                .signerAccountId(request.getSignerAccountId())
                .certificateId(request.getCertificateId())
                .status(request.getStatus().name())
                .requestedAt(request.getRequestedAt())
                .decidedAt(request.getDecidedAt())
                .signedAt(request.getSignedAt())
                .note(request.getNote())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .events(events.stream().map(this::toEventResponse).toList())
                .artifact(toArtifactResponse(request, artifact))
                .build();
    }

    private SigningEventResponse toEventResponse(SigningEvent event) {
        return SigningEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType().name())
                .actorAccountId(event.getActorAccountId())
                .eventTime(event.getEventTime())
                .detail(event.getDetail())
                .build();
    }

    private SigningArtifactResponse toArtifactResponse(SigningRequest request, SigningArtifact artifact) {
        if (artifact == null) {
            return null;
        }

        return SigningArtifactResponse.builder()
                .id(artifact.getId())
                .signingRequestId(artifact.getSigningRequest().getId())
                .signerAccountId(request.getSignerAccountId())
                .certificateId(request.getCertificateId())
                .fileName(artifact.getFileName())
                .contentType(artifact.getContentType())
                .digestAlgorithm(artifact.getDigestAlgorithm())
                .signatureAlgorithm(artifact.getSignatureAlgorithm())
                .documentDigestBase64(artifact.getDocumentDigestBase64())
                .signatureValueBase64(artifact.getSignatureValueBase64())
                .signedPayloadLength(artifact.getSignedPayloadLength())
                .outputObjectKey(artifact.getOutputObjectKey())
                .outputFileName(resolveStoredSignedOutputFileName(
                        artifact.getOutputObjectKey(),
                        artifact.getOutputFileName(),
                        artifact.getFileName()
                ))
                .outputContentType(artifact.getOutputContentType())
                .outputFileSize(artifact.getOutputFileSize())
                .createdAt(artifact.getCreatedAt())
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
