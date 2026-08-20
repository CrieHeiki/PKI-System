package com.ct556.signing_workflow_service.service;

import com.ct556.signing_workflow_service.client.AuditGateway;
import com.ct556.signing_workflow_service.client.AccountGateway;
import com.ct556.signing_workflow_service.client.CertificateGateway;
import com.ct556.signing_workflow_service.client.DocumentGateway;
import com.ct556.signing_workflow_service.client.StepUpGateway;
import com.ct556.signing_workflow_service.client.dto.AccountMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentPayloadResponse;
import com.ct556.signing_workflow_service.client.dto.InternalStepUpVerificationResponse;
import com.ct556.signing_workflow_service.client.dto.StoredSignedOutputResponse;
import com.ct556.signing_workflow_service.domain.SigningEventType;
import com.ct556.signing_workflow_service.domain.SigningRequestStatus;
import com.ct556.signing_workflow_service.dto.request.ExecuteSigningRequest;
import com.ct556.signing_workflow_service.dto.request.SignatureAppearanceOverrideRequest;
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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SigningExecutionService {

    private static final String STEP_UP_ACTION = "SIGN_DOCUMENT";
    private static final String STEP_UP_TARGET_TYPE = "SIGNING_REQUEST";

    private final SigningRequestRepository signingRequestRepository;
    private final SigningEventRepository signingEventRepository;
    private final SigningArtifactRepository signingArtifactRepository;
    private final AccountGateway accountGateway;
    private final DocumentGateway documentGateway;
    private final CertificateGateway certificateGateway;
    private final StepUpGateway stepUpGateway;
    private final AuditGateway auditGateway;
    private final SigningWorkflowMapper mapper;
    private final EmbeddedSignatureOutputService embeddedSignatureOutputService;

    public SigningExecutionService(SigningRequestRepository signingRequestRepository,
                                   SigningEventRepository signingEventRepository,
                                   SigningArtifactRepository signingArtifactRepository,
                                   AccountGateway accountGateway,
                                   DocumentGateway documentGateway,
                                   CertificateGateway certificateGateway,
                                   StepUpGateway stepUpGateway,
                                   AuditGateway auditGateway,
                                    SigningWorkflowMapper mapper,
                                   EmbeddedSignatureOutputService embeddedSignatureOutputService) {
        this.signingRequestRepository = signingRequestRepository;
        this.signingEventRepository = signingEventRepository;
        this.signingArtifactRepository = signingArtifactRepository;
        this.accountGateway = accountGateway;
        this.documentGateway = documentGateway;
        this.certificateGateway = certificateGateway;
        this.stepUpGateway = stepUpGateway;
        this.auditGateway = auditGateway;
        this.mapper = mapper;
        this.embeddedSignatureOutputService = embeddedSignatureOutputService;
    }

    @Transactional
    public SigningRequestResponse executeSigning(UUID requestId,
                                                 ExecuteSigningRequest request,
                                                 CurrentUser currentUser) {
        SigningRequest signingRequest = signingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Signing request not found: " + requestId));

        if (currentUser.isAdmin()) {
            throw new ForbiddenException("Admin accounts are not allowed to execute signing");
        }

        if (!signingRequest.getSignerAccountId().equals(currentUser.getAccountId())) {
            throw new ForbiddenException("Only assigned signer can execute signing");
        }

        if (signingRequest.getStatus() != SigningRequestStatus.APPROVED) {
            throw new BusinessException("Only APPROVED request can be signed");
        }

        if (signingRequest.getCertificateId() == null) {
            throw new BusinessException("Certificate must be selected before execute signing");
        }

        if (signingArtifactRepository.existsBySigningRequestId(signingRequest.getId())) {
            throw new BusinessException("Signing artifact already exists for this request");
        }

        InternalStepUpVerificationResponse stepUpVerification = stepUpGateway.consumeVerification(
                request.getStepUpVerificationToken(),
                currentUser.getAccountId(),
                STEP_UP_ACTION,
                STEP_UP_TARGET_TYPE,
                signingRequest.getId(),
                true
        );
        validateStepUpVerification(stepUpVerification, currentUser.getAccountId(), signingRequest.getId());

        CertificateMetadataResponse certificate = certificateGateway.getCertificateMetadata(signingRequest.getCertificateId());
        validateCertificate(signingRequest, certificate);

        DocumentPayloadResponse document = documentGateway.getDocumentForSigning(signingRequest.getDocumentId());
        if (document == null || document.getContentBase64() == null || document.getContentBase64().isBlank()) {
            throw new BusinessException("Document content is empty");
        }
        AccountMetadataResponse signerAccount = accountGateway.getAccountMetadata(signingRequest.getSignerAccountId());

        EmbeddedSignatureOutputService.EmbeddedSignatureResult signedOutput = embeddedSignatureOutputService.buildSignedOutput(
                signingRequest,
                document,
                certificate,
                signerAccount,
                request.getSignatureAppearance()
        );

        StoredSignedOutputResponse storedOutput = documentGateway.storeSignedOutput(
                signingRequest.getDocumentId(),
                signingRequest.getId(),
                signedOutput.outputFileName(),
                signedOutput.outputContentType(),
                Base64.getEncoder().encodeToString(signedOutput.outputBytes())
        );

        SigningArtifact artifact = SigningArtifact.builder()
                .id(UUID.randomUUID())
                .signingRequest(signingRequest)
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .digestAlgorithm(signedOutput.digestAlgorithm())
                .signatureAlgorithm(signedOutput.signatureAlgorithm())
                .documentDigestBase64(signedOutput.documentDigestBase64())
                .signatureValueBase64(signedOutput.signatureValueBase64())
                .signedPayloadLength(signedOutput.signedPayloadLength())
                .outputObjectKey(storedOutput.getObjectKey())
                .outputFileName(storedOutput.getFileName())
                .outputContentType(storedOutput.getContentType())
                .outputFileSize(storedOutput.getSize())
                .createdAt(LocalDateTime.now())
                .build();

        signingArtifactRepository.save(artifact);

        signingRequest.setStatus(SigningRequestStatus.SIGNED);
        signingRequest.setSignedAt(LocalDateTime.now());
        signingRequest.setUpdatedAt(LocalDateTime.now());
        signingRequestRepository.save(signingRequest);

        addEvent(signingRequest, SigningEventType.REQUEST_SIGNED, currentUser.getAccountId(),
                "Cryptographic signing executed and signed output package stored in MinIO");

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("documentId", signingRequest.getDocumentId());
        auditPayload.put("certificateId", signingRequest.getCertificateId());
        auditPayload.put("artifactId", artifact.getId());
        auditPayload.put("outputObjectKey", artifact.getOutputObjectKey());
        auditPayload.put("status", signingRequest.getStatus().name());
        appendAppearanceAudit(auditPayload, request.getSignatureAppearance());

        publishAudit(
                "SIGNING_REQUEST_EXECUTED",
                currentUser.getAccountId(),
                "USER",
                signingRequest.getId(),
                "SIGNING_REQUEST",
                signingRequest.getId(),
                "Signing executed successfully",
                auditPayload
        );

        return mapper.toResponse(
                signingRequest,
                signingEventRepository.findBySigningRequestIdOrderByEventTimeAsc(signingRequest.getId()),
                artifact
        );
    }

    private void validateStepUpVerification(InternalStepUpVerificationResponse stepUpVerification,
                                            UUID currentUserId,
                                            UUID requestId) {
        if (stepUpVerification == null || !stepUpVerification.isValid()) {
            throw new BusinessException("Step-up verification is missing or invalid");
        }

        if (stepUpVerification.getUserId() == null || !stepUpVerification.getUserId().equals(currentUserId)) {
            throw new BusinessException("Step-up verification does not belong to current signer");
        }

        if (!STEP_UP_ACTION.equalsIgnoreCase(stepUpVerification.getAction())) {
            throw new BusinessException("Step-up verification action does not match SIGN_DOCUMENT");
        }

        if (stepUpVerification.getTargetId() == null || !stepUpVerification.getTargetId().equals(requestId)) {
            throw new BusinessException("Step-up verification target does not match signing request");
        }
    }

    private void validateCertificate(SigningRequest signingRequest, CertificateMetadataResponse certificate) {
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

    private void appendAppearanceAudit(Map<String, Object> payload,
                                       SignatureAppearanceOverrideRequest appearanceOverride) {
        if (appearanceOverride == null) {
            payload.put("signatureAppearanceSource", "PROFILE_DEFAULT");
            return;
        }

        payload.put("signatureAppearanceSource", "REQUEST_OVERRIDE");
        payload.put("signaturePageNumber", appearanceOverride.getPageNumber());
        payload.put("signatureXPercent", appearanceOverride.getXPercent());
        payload.put("signatureYPercent", appearanceOverride.getYPercent());
        payload.put("signatureWidthPercent", appearanceOverride.getWidthPercent());
        payload.put("signatureHeightPercent", appearanceOverride.getHeightPercent());
        payload.put("signatureImageOnly", appearanceOverride.getImageOnly());
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
