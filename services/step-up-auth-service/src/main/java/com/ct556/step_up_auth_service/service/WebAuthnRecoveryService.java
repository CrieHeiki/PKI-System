package com.ct556.step_up_auth_service.service;

import com.ct556.step_up_auth_service.client.AccountGateway;
import com.ct556.step_up_auth_service.client.AuditGateway;
import com.ct556.step_up_auth_service.client.InternalAccountResponse;
import com.ct556.step_up_auth_service.config.StepUpProperties;
import com.ct556.step_up_auth_service.dto.request.CreateWebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.dto.request.ReviewWebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.dto.response.WebAuthnRecoveryRequestResponse;
import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.MfaRegistrationStatus;
import com.ct556.step_up_auth_service.entity.MfaType;
import com.ct556.step_up_auth_service.entity.WebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.entity.WebAuthnRecoveryRequestStatus;
import com.ct556.step_up_auth_service.exception.BusinessException;
import com.ct556.step_up_auth_service.exception.ResourceNotFoundException;
import com.ct556.step_up_auth_service.model.WebAuthnEnrollmentTicketState;
import com.ct556.step_up_auth_service.repository.MfaRegistrationRepository;
import com.ct556.step_up_auth_service.repository.WebAuthnRecoveryRequestRepository;
import com.ct556.step_up_auth_service.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
public class WebAuthnRecoveryService {

    private static final List<WebAuthnRecoveryRequestStatus> OPEN_STATUSES = List.of(
            WebAuthnRecoveryRequestStatus.PENDING,
            WebAuthnRecoveryRequestStatus.APPROVED
    );

    private final WebAuthnRecoveryRequestRepository recoveryRequestRepository;
    private final MfaRegistrationRepository mfaRegistrationRepository;
    private final StepUpRedisStore stepUpRedisStore;
    private final StepUpProperties stepUpProperties;
    private final AccountGateway accountGateway;
    private final AuditGateway auditGateway;

    public WebAuthnRecoveryService(WebAuthnRecoveryRequestRepository recoveryRequestRepository,
                                   MfaRegistrationRepository mfaRegistrationRepository,
                                   StepUpRedisStore stepUpRedisStore,
                                   StepUpProperties stepUpProperties,
                                   AccountGateway accountGateway,
                                   AuditGateway auditGateway) {
        this.recoveryRequestRepository = recoveryRequestRepository;
        this.mfaRegistrationRepository = mfaRegistrationRepository;
        this.stepUpRedisStore = stepUpRedisStore;
        this.stepUpProperties = stepUpProperties;
        this.accountGateway = accountGateway;
        this.auditGateway = auditGateway;
    }

    @Transactional(readOnly = true)
    public List<WebAuthnRecoveryRequestResponse> getMyRecoveryRequests(CurrentUser currentUser) {
        return recoveryRequestRepository.findByUserIdOrderByRequestedAtDesc(currentUser.accountId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WebAuthnRecoveryRequestResponse createRecoveryRequest(CurrentUser currentUser,
                                                                 CreateWebAuthnRecoveryRequest request) {
        if (!mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                currentUser.accountId(),
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        ).isEmpty()) {
            // expected path
        } else {
            throw new BusinessException("Recovery requests require at least one active WebAuthn authenticator. Ask an administrator to issue a first-enrollment ticket instead.");
        }

        findOpenRecoveryRequest(currentUser.accountId()).ifPresent(existing -> {
            throw new BusinessException("A WebAuthn recovery request is already open for this account");
        });

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        WebAuthnRecoveryRequest recoveryRequest = new WebAuthnRecoveryRequest();
        recoveryRequest.setId(UUID.randomUUID());
        recoveryRequest.setUserId(currentUser.accountId());
        recoveryRequest.setStatus(WebAuthnRecoveryRequestStatus.PENDING);
        recoveryRequest.setReason(normalizeNullable(request.reason()));
        recoveryRequest.setRequestedAt(now);
        recoveryRequest.setUpdatedAt(now);

        recoveryRequestRepository.save(recoveryRequest);

        publishAudit(
                "WEBAUTHN_RECOVERY_REQUEST_CREATED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                recoveryRequest.getId(),
                "User submitted a WebAuthn recovery request",
                payload(
                        "userId", currentUser.accountId(),
                        "requestedAt", recoveryRequest.getRequestedAt(),
                        "reason", recoveryRequest.getReason()
                )
        );

        return toResponse(recoveryRequest);
    }

    @Transactional
    public WebAuthnRecoveryRequestResponse cancelRecoveryRequest(CurrentUser currentUser, UUID requestId) {
        WebAuthnRecoveryRequest recoveryRequest = recoveryRequestRepository.findByIdAndUserId(requestId, currentUser.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Recovery request not found"));

        if (recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.PENDING
                && recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.APPROVED) {
            throw new BusinessException("Only pending or approved recovery requests can be cancelled");
        }

        deleteTicketIfPresent(recoveryRequest);
        recoveryRequest.setStatus(WebAuthnRecoveryRequestStatus.CANCELLED);
        recoveryRequest.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        recoveryRequestRepository.save(recoveryRequest);

        publishAudit(
                "WEBAUTHN_RECOVERY_REQUEST_CANCELLED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                recoveryRequest.getId(),
                "User cancelled a WebAuthn recovery request",
                payload(
                        "requestId", recoveryRequest.getId(),
                        "status", recoveryRequest.getStatus().name()
                )
        );

        return toResponse(recoveryRequest);
    }

    @Transactional(readOnly = true)
    public List<WebAuthnRecoveryRequestResponse> getRecoveryRequestsForAdmin(CurrentUser currentUser,
                                                                             UUID userId,
                                                                             String status) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("Only administrators can review WebAuthn recovery requests");
        }

        String normalizedStatus = normalizeNullable(status);
        return recoveryRequestRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .filter(request -> normalizedStatus == null || request.getStatus().name().equalsIgnoreCase(normalizedStatus))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WebAuthnRecoveryRequestResponse approveRecoveryRequest(CurrentUser currentUser,
                                                                  UUID requestId,
                                                                  ReviewWebAuthnRecoveryRequest request) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("Only administrators can approve WebAuthn recovery requests");
        }

        WebAuthnRecoveryRequest recoveryRequest = recoveryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery request not found"));

        if (recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.PENDING
                && recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.APPROVED) {
            throw new BusinessException("Only pending or approved recovery requests can be issued a recovery ticket");
        }

        InternalAccountResponse targetAccount = accountGateway.getInternalAccount(recoveryRequest.getUserId());
        if (!"ACTIVE".equalsIgnoreCase(targetAccount.status())) {
            throw new BusinessException("Recovery tickets can only be issued to ACTIVE accounts");
        }

        List<MfaRegistration> activeRegistrations = mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                recoveryRequest.getUserId(),
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        );
        if (activeRegistrations.isEmpty()) {
            throw new BusinessException("Recovery tickets require at least one active WebAuthn authenticator on record");
        }

        deleteTicketIfPresent(recoveryRequest);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(stepUpProperties.enrollmentTicketTtlSeconds());
        String recoveryTicket = UUID.randomUUID().toString();

        stepUpRedisStore.saveWebAuthnEnrollmentTicket(new WebAuthnEnrollmentTicketState(
                recoveryTicket,
                recoveryRequest.getUserId(),
                currentUser.accountId(),
                issuedAt,
                expiresAt,
                recoveryRequest.getId()
        ));

        LocalDateTime reviewedAt = LocalDateTime.now(ZoneOffset.UTC);
        recoveryRequest.setStatus(WebAuthnRecoveryRequestStatus.APPROVED);
        recoveryRequest.setDecisionNote(normalizeNullable(request.note()));
        recoveryRequest.setReviewedByAdminId(currentUser.accountId());
        recoveryRequest.setReviewedAt(reviewedAt);
        recoveryRequest.setRecoveryTicket(recoveryTicket);
        recoveryRequest.setRecoveryTicketIssuedAt(LocalDateTime.ofInstant(issuedAt, ZoneOffset.UTC));
        recoveryRequest.setRecoveryTicketExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
        recoveryRequest.setUpdatedAt(reviewedAt);
        recoveryRequestRepository.save(recoveryRequest);

        publishAudit(
                "WEBAUTHN_RECOVERY_REQUEST_APPROVED",
                currentUser.accountId(),
                "ADMIN",
                recoveryRequest.getId(),
                "Admin approved a WebAuthn recovery request",
                payload(
                        "requestId", recoveryRequest.getId(),
                        "userId", recoveryRequest.getUserId(),
                        "issuedAt", issuedAt,
                        "expiresAt", expiresAt,
                        "note", recoveryRequest.getDecisionNote()
                )
        );

        return toResponse(recoveryRequest);
    }

    @Transactional
    public WebAuthnRecoveryRequestResponse rejectRecoveryRequest(CurrentUser currentUser,
                                                                 UUID requestId,
                                                                 ReviewWebAuthnRecoveryRequest request) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("Only administrators can reject WebAuthn recovery requests");
        }

        WebAuthnRecoveryRequest recoveryRequest = recoveryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery request not found"));

        if (recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.PENDING
                && recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.APPROVED) {
            throw new BusinessException("Only pending or approved recovery requests can be rejected");
        }

        deleteTicketIfPresent(recoveryRequest);

        LocalDateTime reviewedAt = LocalDateTime.now(ZoneOffset.UTC);
        recoveryRequest.setStatus(WebAuthnRecoveryRequestStatus.REJECTED);
        recoveryRequest.setDecisionNote(normalizeNullable(request.note()));
        recoveryRequest.setReviewedByAdminId(currentUser.accountId());
        recoveryRequest.setReviewedAt(reviewedAt);
        recoveryRequest.setUpdatedAt(reviewedAt);
        recoveryRequestRepository.save(recoveryRequest);

        publishAudit(
                "WEBAUTHN_RECOVERY_REQUEST_REJECTED",
                currentUser.accountId(),
                "ADMIN",
                recoveryRequest.getId(),
                "Admin rejected a WebAuthn recovery request",
                payload(
                        "requestId", recoveryRequest.getId(),
                        "userId", recoveryRequest.getUserId(),
                        "note", recoveryRequest.getDecisionNote()
                )
        );

        return toResponse(recoveryRequest);
    }

    @Transactional(readOnly = true)
    public void assertNoOpenRecoveryRequest(UUID userId) {
        findOpenRecoveryRequest(userId).ifPresent(recoveryRequest -> {
            throw new BusinessException("A WebAuthn recovery request is in progress. Normal WebAuthn operations are blocked until the request is cancelled or completed.");
        });
    }

    @Transactional(readOnly = true)
    public WebAuthnRecoveryRequest findApprovedRecoveryRequestByTicket(UUID userId, String recoveryTicket) {
        WebAuthnRecoveryRequest recoveryRequest = recoveryRequestRepository.findByRecoveryTicket(recoveryTicket)
                .orElseThrow(() -> new BusinessException("Recovery enrollment ticket was not found"));

        if (!recoveryRequest.getUserId().equals(userId)) {
            throw new BusinessException("Recovery enrollment ticket does not belong to current user");
        }
        if (recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.APPROVED) {
            throw new BusinessException("Recovery enrollment ticket is no longer approved");
        }
        if (recoveryRequest.getRecoveryTicketExpiresAt() == null
                || !LocalDateTime.now(ZoneOffset.UTC).isBefore(recoveryRequest.getRecoveryTicketExpiresAt())) {
            stepUpRedisStore.deleteWebAuthnEnrollmentTicket(recoveryTicket);
            throw new BusinessException("Recovery enrollment ticket has expired");
        }

        Instant issuedAt = toInstant(recoveryRequest.getRecoveryTicketIssuedAt(), recoveryRequest.getReviewedAt());
        Instant expiresAt = recoveryRequest.getRecoveryTicketExpiresAt().toInstant(ZoneOffset.UTC);
        WebAuthnEnrollmentTicketState ticketState = stepUpRedisStore.findWebAuthnEnrollmentTicket(recoveryTicket)
                .filter(state -> state.userId().equals(userId))
                .filter(state -> recoveryRequest.getId().equals(state.recoveryRequestId()))
                .filter(state -> Instant.now().isBefore(state.expiresAt()))
                .orElseGet(() -> {
                    WebAuthnEnrollmentTicketState repairedState = new WebAuthnEnrollmentTicketState(
                            recoveryTicket,
                            recoveryRequest.getUserId(),
                            recoveryRequest.getReviewedByAdminId(),
                            issuedAt,
                            expiresAt,
                            recoveryRequest.getId()
                    );
                    stepUpRedisStore.saveWebAuthnEnrollmentTicket(repairedState);
                    return repairedState;
                });
        if (!ticketState.userId().equals(userId)) {
            throw new BusinessException("Recovery enrollment ticket does not belong to current user");
        }

        return recoveryRequest;
    }

    @Transactional
    public void completeRecovery(UUID userId,
                                 UUID requestId,
                                 UUID completedRegistrationId,
                                 List<UUID> disabledRegistrationIds) {
        WebAuthnRecoveryRequest recoveryRequest = recoveryRequestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery request not found"));

        if (recoveryRequest.getStatus() != WebAuthnRecoveryRequestStatus.APPROVED) {
            throw new BusinessException("Recovery request is not approved");
        }

        deleteTicketIfPresent(recoveryRequest);
        recoveryRequest.setStatus(WebAuthnRecoveryRequestStatus.COMPLETED);
        recoveryRequest.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        recoveryRequest.setCompletedRegistrationId(completedRegistrationId);
        recoveryRequest.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        recoveryRequestRepository.save(recoveryRequest);

        publishAudit(
                "WEBAUTHN_RECOVERY_COMPLETED",
                userId,
                "USER",
                recoveryRequest.getId(),
                "WebAuthn recovery completed with a replacement authenticator",
                payload(
                        "requestId", recoveryRequest.getId(),
                        "completedRegistrationId", completedRegistrationId,
                        "disabledRegistrationIds", disabledRegistrationIds
                )
        );
    }

    @Transactional
    public UUID authorizeApprovedRecoveryTicket(UUID userId, String recoveryTicket) {
        WebAuthnRecoveryRequest recoveryRequest = findApprovedRecoveryRequestByTicket(userId, recoveryTicket);
        return recoveryRequest.getId();
    }

    @Transactional(readOnly = true)
    public boolean hasRecoveryTicket(String recoveryTicket) {
        return recoveryRequestRepository.findByRecoveryTicket(recoveryTicket).isPresent();
    }

    private java.util.Optional<WebAuthnRecoveryRequest> findOpenRecoveryRequest(UUID userId) {
        return recoveryRequestRepository.findFirstByUserIdAndStatusInOrderByRequestedAtDesc(userId, OPEN_STATUSES);
    }

    private void deleteTicketIfPresent(WebAuthnRecoveryRequest recoveryRequest) {
        if (StringUtils.hasText(recoveryRequest.getRecoveryTicket())) {
            stepUpRedisStore.deleteWebAuthnEnrollmentTicket(recoveryRequest.getRecoveryTicket());
        }
        recoveryRequest.setRecoveryTicket(null);
        recoveryRequest.setRecoveryTicketIssuedAt(null);
        recoveryRequest.setRecoveryTicketExpiresAt(null);
    }

    private WebAuthnRecoveryRequestResponse toResponse(WebAuthnRecoveryRequest recoveryRequest) {
        return new WebAuthnRecoveryRequestResponse(
                recoveryRequest.getId(),
                recoveryRequest.getUserId(),
                recoveryRequest.getStatus().name(),
                recoveryRequest.getReason(),
                recoveryRequest.getDecisionNote(),
                recoveryRequest.getReviewedByAdminId(),
                recoveryRequest.getRequestedAt(),
                recoveryRequest.getReviewedAt(),
                recoveryRequest.getCompletedAt(),
                recoveryRequest.getRecoveryTicket(),
                recoveryRequest.getRecoveryTicketIssuedAt(),
                recoveryRequest.getRecoveryTicketExpiresAt(),
                recoveryRequest.getCompletedRegistrationId()
        );
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Instant toInstant(LocalDateTime primary, LocalDateTime fallback) {
        if (primary != null) {
            return primary.toInstant(ZoneOffset.UTC);
        }
        if (fallback != null) {
            return fallback.toInstant(ZoneOffset.UTC);
        }
        return Instant.now();
    }

    private void publishAudit(String eventType,
                              UUID actorId,
                              String actorRole,
                              UUID targetId,
                              String summary,
                              LinkedHashMap<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                actorId,
                actorRole,
                targetId,
                "STEP_UP_WEBAUTHN_RECOVERY",
                targetId,
                targetId == null ? null : targetId.toString(),
                summary,
                payload
        );
    }

    private LinkedHashMap<String, Object> payload(Object... keyValues) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                payload.put(String.valueOf(key), value);
            }
        }
        return payload;
    }
}
