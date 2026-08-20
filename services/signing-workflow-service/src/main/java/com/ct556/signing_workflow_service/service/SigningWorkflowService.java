package com.ct556.signing_workflow_service.service;

import com.ct556.signing_workflow_service.client.AccountGateway;
import com.ct556.signing_workflow_service.client.AuditGateway;
import com.ct556.signing_workflow_service.client.CertificateGateway;
import com.ct556.signing_workflow_service.client.DocumentGateway;
import com.ct556.signing_workflow_service.client.dto.AccountMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentPayloadResponse;
import com.ct556.signing_workflow_service.domain.SigningEventType;
import com.ct556.signing_workflow_service.domain.SigningRequestStatus;
import com.ct556.signing_workflow_service.dto.request.ApproveSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.request.CreateSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.request.RejectSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.response.SigningRequestResponse;
import com.ct556.signing_workflow_service.entity.SigningArtifact;
import com.ct556.signing_workflow_service.entity.SigningEvent;
import com.ct556.signing_workflow_service.entity.SigningRequest;
import com.ct556.signing_workflow_service.exception.BusinessException;
import com.ct556.signing_workflow_service.exception.ForbiddenException;
import com.ct556.signing_workflow_service.exception.ResourceNotFoundException;
import com.ct556.signing_workflow_service.mapper.SigningWorkflowMapper;
import com.ct556.signing_workflow_service.repository.SigningArtifactRepository;
import com.ct556.signing_workflow_service.repository.SigningEventRepository;
import com.ct556.signing_workflow_service.repository.SigningRequestRepository;
import com.ct556.signing_workflow_service.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class SigningWorkflowService {

    private final SigningRequestRepository signingRequestRepository;
    private final SigningEventRepository signingEventRepository;
    private final SigningArtifactRepository signingArtifactRepository;
    private final AccountGateway accountGateway;
    private final DocumentGateway documentGateway;
    private final CertificateGateway certificateGateway;
    private final AuditGateway auditGateway;
    private final SigningWorkflowMapper mapper;

    public SigningWorkflowService(SigningRequestRepository signingRequestRepository,
                                  SigningEventRepository signingEventRepository,
                                  SigningArtifactRepository signingArtifactRepository,
                                  AccountGateway accountGateway,
                                  DocumentGateway documentGateway,
                                  CertificateGateway certificateGateway,
                                  AuditGateway auditGateway,
                                  SigningWorkflowMapper mapper) {
        this.signingRequestRepository = signingRequestRepository;
        this.signingEventRepository = signingEventRepository;
        this.signingArtifactRepository = signingArtifactRepository;
        this.accountGateway = accountGateway;
        this.documentGateway = documentGateway;
        this.certificateGateway = certificateGateway;
        this.auditGateway = auditGateway;
        this.mapper = mapper;
    }

    @Transactional
    public SigningRequestResponse createRequest(CreateSigningRequestRequest request, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            throw new BusinessException("Admin accounts cannot create signing requests");
        }

        if (currentUser.getAccountId().equals(request.getSignerAccountId())) {
            throw new BusinessException("Requester is not allowed to create a signing request for themself in strict mode");
        }

        validateRequesterDocumentAccess(request.getDocumentId(), currentUser);
        validateSignerAccount(request.getSignerAccountId());

        SigningRequest signingRequest = SigningRequest.builder()
                .id(UUID.randomUUID())
                .documentId(request.getDocumentId())
                .requesterAccountId(currentUser.getAccountId())
                .signerAccountId(request.getSignerAccountId())
                .status(SigningRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        signingRequestRepository.save(signingRequest);

        addEvent(signingRequest, SigningEventType.REQUEST_CREATED, currentUser.getAccountId(), request.getNote());
        publishAudit(
                "SIGNING_REQUEST_CREATED",
                currentUser.getAccountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signing request created",
                Map.of(
                        "documentId", signingRequest.getDocumentId(),
                        "requesterAccountId", signingRequest.getRequesterAccountId(),
                        "signerAccountId", signingRequest.getSignerAccountId(),
                        "status", signingRequest.getStatus().name()
                )
        );
        return getById(signingRequest.getId(), currentUser);
    }

    public List<SigningRequestResponse> getRequestedByMe(CurrentUser currentUser) {
        return signingRequestRepository.findByRequesterAccountIdOrderByCreatedAtDesc(currentUser.getAccountId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<SigningRequestResponse> getPendingForMe(CurrentUser currentUser) {
        return signingRequestRepository.findBySignerAccountIdAndStatusInOrderByCreatedAtDesc(
                        currentUser.getAccountId(),
                        EnumSet.of(SigningRequestStatus.PENDING, SigningRequestStatus.APPROVED)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SigningRequestResponse getById(UUID requestId, CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        boolean relatedUser = signingRequest.getRequesterAccountId().equals(currentUser.getAccountId())
                || signingRequest.getSignerAccountId().equals(currentUser.getAccountId());

        if (!relatedUser && !currentUser.isAdmin()) {
            throw new ForbiddenException("You are not allowed to view this signing request");
        }

        return toResponse(signingRequest);
    }

    @Transactional
    public SigningRequestResponse approve(UUID requestId,
                                          ApproveSigningRequestRequest request,
                                          CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        if (!signingRequest.getSignerAccountId().equals(currentUser.getAccountId())) {
            throw new ForbiddenException("Only assigned signer can approve this request");
        }

        ensurePending(signingRequest);
        validateCertificateAssignment(signingRequest, request.getCertificateId());

        signingRequest.setCertificateId(request.getCertificateId());
        signingRequest.setStatus(SigningRequestStatus.APPROVED);
        signingRequest.setDecidedAt(LocalDateTime.now());
        signingRequest.setUpdatedAt(LocalDateTime.now());

        signingRequestRepository.save(signingRequest);

        addEvent(signingRequest, SigningEventType.REQUEST_APPROVED, currentUser.getAccountId(), request.getNote());
        publishAudit(
                "SIGNING_REQUEST_APPROVED",
                currentUser.getAccountId(),
                "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signing request approved",
                Map.of(
                        "certificateId", signingRequest.getCertificateId(),
                        "documentId", signingRequest.getDocumentId(),
                        "signerAccountId", signingRequest.getSignerAccountId(),
                        "status", signingRequest.getStatus().name()
                )
        );
        return toResponse(signingRequest);
    }

    @Transactional
    public SigningRequestResponse reject(UUID requestId,
                                         RejectSigningRequestRequest request,
                                         CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        if (!signingRequest.getSignerAccountId().equals(currentUser.getAccountId())) {
            throw new ForbiddenException("Only assigned signer can reject this request");
        }

        ensurePending(signingRequest);

        signingRequest.setStatus(SigningRequestStatus.REJECTED);
        signingRequest.setDecidedAt(LocalDateTime.now());
        signingRequest.setUpdatedAt(LocalDateTime.now());

        signingRequestRepository.save(signingRequest);

        addEvent(signingRequest, SigningEventType.REQUEST_REJECTED, currentUser.getAccountId(), request.getNote());
        publishAudit(
                "SIGNING_REQUEST_REJECTED",
                currentUser.getAccountId(),
                "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signing request rejected",
                Map.of(
                        "documentId", signingRequest.getDocumentId(),
                        "signerAccountId", signingRequest.getSignerAccountId(),
                        "status", signingRequest.getStatus().name()
                )
        );
        return toResponse(signingRequest);
    }

    @Transactional
    public SigningRequestResponse cancel(UUID requestId, CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        if (!signingRequest.getRequesterAccountId().equals(currentUser.getAccountId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Only requester or admin can cancel this request");
        }

        ensurePending(signingRequest);

        signingRequest.setStatus(SigningRequestStatus.CANCELLED);
        signingRequest.setUpdatedAt(LocalDateTime.now());

        signingRequestRepository.save(signingRequest);

        addEvent(signingRequest, SigningEventType.REQUEST_CANCELLED, currentUser.getAccountId(), "Request cancelled");
        publishAudit(
                "SIGNING_REQUEST_CANCELLED",
                currentUser.getAccountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signing request cancelled",
                Map.of(
                        "documentId", signingRequest.getDocumentId(),
                        "requesterAccountId", signingRequest.getRequesterAccountId(),
                        "status", signingRequest.getStatus().name()
                )
        );
        return toResponse(signingRequest);
    }

    public List<SigningRequestResponse> getAllForAdmin() {
        return signingRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DownloadedSignedOutput downloadSignedOutput(UUID requestId, CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        boolean relatedUser = signingRequest.getRequesterAccountId().equals(currentUser.getAccountId())
                || signingRequest.getSignerAccountId().equals(currentUser.getAccountId());

        if (!relatedUser && !currentUser.isAdmin()) {
            throw new ForbiddenException("You are not allowed to download this signed output");
        }

        SigningArtifact artifact = signingArtifactRepository.findBySigningRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing artifact not found for request: " + requestId));

        if (artifact.getOutputObjectKey() == null || artifact.getOutputObjectKey().isBlank()) {
            throw new BusinessException("Signed output is not available for request: " + requestId);
        }

        byte[] content = documentGateway.getSignedOutput(artifact.getOutputObjectKey());

        String resolvedOutputFileName = resolveStoredSignedOutputFileName(
                artifact.getOutputObjectKey(),
                artifact.getOutputFileName(),
                artifact.getFileName()
        );

        publishAudit(
                "SIGNED_OUTPUT_DOWNLOADED",
                currentUser.getAccountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signed output downloaded",
                Map.of(
                        "documentId", signingRequest.getDocumentId(),
                        "signingRequestId", signingRequest.getId(),
                        "outputObjectKey", artifact.getOutputObjectKey(),
                        "outputFileName", resolvedOutputFileName,
                        "outputContentType", artifact.getOutputContentType()
                )
        );

        return new DownloadedSignedOutput(
                resolvedOutputFileName,
                firstNonBlank(artifact.getOutputContentType(), artifact.getContentType(), "application/octet-stream"),
                artifact.getOutputFileSize() == null ? content.length : artifact.getOutputFileSize(),
                content
        );
    }

    public DownloadedSourceDocument downloadSourceDocument(UUID requestId, CurrentUser currentUser) {
        SigningRequest signingRequest = getRequestOrThrow(requestId);

        boolean relatedUser = signingRequest.getRequesterAccountId().equals(currentUser.getAccountId())
                || signingRequest.getSignerAccountId().equals(currentUser.getAccountId());

        if (!relatedUser && !currentUser.isAdmin()) {
            throw new ForbiddenException("You are not allowed to preview this source document");
        }

        DocumentPayloadResponse document = documentGateway.getDocumentForSigning(signingRequest.getDocumentId());
        if (document == null || document.getContentBase64() == null || document.getContentBase64().isBlank()) {
            throw new BusinessException("Source document content is not available");
        }

        byte[] content;
        try {
            content = java.util.Base64.getDecoder().decode(document.getContentBase64());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Source document content is not valid Base64");
        }

        Map<String, Object> previewAuditPayload = new LinkedHashMap<>();
        previewAuditPayload.put("documentId", signingRequest.getDocumentId());
        previewAuditPayload.put("signingRequestId", signingRequest.getId());
        previewAuditPayload.put("fileName", document.getFileName());
        previewAuditPayload.put("contentType", document.getContentType());

        publishAudit(
                "SIGNING_SOURCE_DOCUMENT_PREVIEWED",
                currentUser.getAccountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Source document downloaded for signing preview",
                previewAuditPayload
        );

        return new DownloadedSourceDocument(
                firstNonBlank(document.getFileName(), "source-document.bin"),
                firstNonBlank(document.getContentType(), "application/octet-stream"),
                document.getSize() == null ? content.length : document.getSize(),
                content
        );
    }

    private void validateRequesterDocumentAccess(UUID documentId, CurrentUser currentUser) {
        DocumentMetadataResponse documentMetadata = documentGateway.getDocumentMetadata(documentId);

        if (documentMetadata == null || documentMetadata.getOwnerAccountId() == null) {
            throw new BusinessException("Document metadata is incomplete");
        }

        if (!documentMetadata.getOwnerAccountId().equals(currentUser.getAccountId())) {
            throw new ForbiddenException("You are not allowed to create signing request for this document");
        }

        if (!isActiveStatus(documentMetadata.getStatus())) {
            throw new BusinessException("Document is not active for signing workflow");
        }
    }

    private void validateSignerAccount(UUID signerAccountId) {
        AccountMetadataResponse signerAccount = accountGateway.getAccountMetadata(signerAccountId);

        if (signerAccount == null) {
            throw new BusinessException("Signer account not found");
        }

        if (!"USER".equalsIgnoreCase(signerAccount.getRole())) {
            throw new BusinessException("Signer must have USER role");
        }

        if (!"ACTIVE".equalsIgnoreCase(signerAccount.getStatus())) {
            throw new BusinessException("Signer account must be ACTIVE");
        }
    }

    private void validateCertificateAssignment(SigningRequest signingRequest, UUID certificateId) {
        CertificateMetadataResponse certificate = certificateGateway.getCertificateMetadata(certificateId);

        if (certificate == null) {
            throw new BusinessException("Certificate not found");
        }

        if (certificate.getOwnerAccountId() == null
                || !certificate.getOwnerAccountId().equals(signingRequest.getSignerAccountId())) {
            throw new BusinessException("Certificate does not belong to assigned signer");
        }

        if (certificate.isRevoked()) {
            throw new BusinessException("Certificate has been revoked");
        }

        if (!isActiveStatus(certificate.getStatus())) {
            throw new BusinessException("Certificate is not active");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (certificate.getValidFrom() != null && now.isBefore(certificate.getValidFrom())) {
            throw new BusinessException("Certificate is not yet valid");
        }
        if (certificate.getValidTo() != null && now.isAfter(certificate.getValidTo())) {
            throw new BusinessException("Certificate has expired");
        }
    }

    private SigningRequestResponse toResponse(SigningRequest request) {
        SigningArtifact artifact = signingArtifactRepository.findBySigningRequestId(request.getId()).orElse(null);
        return mapper.toResponse(
                request,
                signingEventRepository.findBySigningRequestIdOrderByEventTimeAsc(request.getId()),
                artifact
        );
    }

    private SigningRequest getRequestOrThrow(UUID requestId) {
        return signingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing request not found: " + requestId));
    }

    private void ensurePending(SigningRequest signingRequest) {
        if (signingRequest.getStatus() != SigningRequestStatus.PENDING) {
            throw new BusinessException("Only PENDING request can be approved, rejected, or cancelled");
        }
    }

    private boolean isActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }

        String normalized = Normalizer.normalize(status, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();

        return "ACTIVE".equals(normalized) || "CON HIEU LUC".equals(normalized);
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

    private void addEvent(SigningRequest signingRequest,
                          SigningEventType eventType,
                          UUID actorAccountId,
                          String detail) {
        SigningEvent event = SigningEvent.builder()
                .id(UUID.randomUUID())
                .signingRequest(signingRequest)
                .eventType(eventType)
                .actorAccountId(actorAccountId)
                .eventTime(LocalDateTime.now())
                .detail(detail)
                .build();

        signingEventRepository.save(event);
    }

    private void publishAudit(String eventType,
                              UUID actorId,
                              String actorRole,
                              UUID targetId,
                              String resourceType,
                              UUID resourceId,
                              String summary,
                              Map<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                actorId,
                actorRole == null ? null : actorRole.toUpperCase(Locale.ROOT),
                targetId,
                resourceType,
                resourceId,
                resourceId == null ? null : resourceId.toString(),
                summary,
                new LinkedHashMap<>(payload)
        );
    }
}
