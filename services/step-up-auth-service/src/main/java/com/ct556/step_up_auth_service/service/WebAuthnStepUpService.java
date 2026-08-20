package com.ct556.step_up_auth_service.service;

import com.ct556.step_up_auth_service.client.AuditGateway;
import com.ct556.step_up_auth_service.client.AccountGateway;
import com.ct556.step_up_auth_service.client.InternalAccountResponse;
import com.ct556.step_up_auth_service.config.StepUpProperties;
import com.ct556.step_up_auth_service.config.WebAuthnProperties;
import com.ct556.step_up_auth_service.dto.request.BeginWebAuthnAssertionRequest;
import com.ct556.step_up_auth_service.dto.request.BeginWebAuthnRegistrationRequest;
import com.ct556.step_up_auth_service.dto.request.CreateWebAuthnEnrollmentTicketRequest;
import com.ct556.step_up_auth_service.dto.request.FinishWebAuthnAssertionRequest;
import com.ct556.step_up_auth_service.dto.request.FinishWebAuthnRegistrationRequest;
import com.ct556.step_up_auth_service.dto.response.MfaRegistrationResponse;
import com.ct556.step_up_auth_service.dto.response.StepUpVerificationResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnAssertionOptionsResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnEnrollmentTicketResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnRegistrationOptionsResponse;
import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.MfaRegistrationStatus;
import com.ct556.step_up_auth_service.entity.MfaType;
import com.ct556.step_up_auth_service.entity.StepUpVerificationLog;
import com.ct556.step_up_auth_service.exception.BusinessException;
import com.ct556.step_up_auth_service.exception.ResourceNotFoundException;
import com.ct556.step_up_auth_service.model.StepUpVerificationProofState;
import com.ct556.step_up_auth_service.model.WebAuthnEnrollmentTicketState;
import com.ct556.step_up_auth_service.model.WebAuthnAssertionCeremonyState;
import com.ct556.step_up_auth_service.model.WebAuthnRegistrationCeremonyState;
import com.ct556.step_up_auth_service.repository.MfaRegistrationRepository;
import com.ct556.step_up_auth_service.repository.StepUpVerificationLogRepository;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WebAuthnStepUpService {

    private final RelyingParty relyingParty;
    private final StepUpProperties stepUpProperties;
    private final WebAuthnProperties webAuthnProperties;
    private final WebAuthnUserHandleCodec userHandleCodec;
    private final MfaRegistrationRepository mfaRegistrationRepository;
    private final StepUpRedisStore stepUpRedisStore;
    private final StepUpVerificationLogRepository verificationLogRepository;
    private final AccountGateway accountGateway;
    private final AuditGateway auditGateway;
    private final WebAuthnRecoveryService webAuthnRecoveryService;
    private final ObjectMapper objectMapper;

    public WebAuthnStepUpService(RelyingParty relyingParty,
                                 StepUpProperties stepUpProperties,
                                 WebAuthnProperties webAuthnProperties,
                                 WebAuthnUserHandleCodec userHandleCodec,
                                 MfaRegistrationRepository mfaRegistrationRepository,
                                 StepUpRedisStore stepUpRedisStore,
                                 StepUpVerificationLogRepository verificationLogRepository,
                                 AccountGateway accountGateway,
                                 AuditGateway auditGateway,
                                 WebAuthnRecoveryService webAuthnRecoveryService,
                                 ObjectMapper objectMapper) {
        this.relyingParty = relyingParty;
        this.stepUpProperties = stepUpProperties;
        this.webAuthnProperties = webAuthnProperties;
        this.userHandleCodec = userHandleCodec;
        this.mfaRegistrationRepository = mfaRegistrationRepository;
        this.stepUpRedisStore = stepUpRedisStore;
        this.verificationLogRepository = verificationLogRepository;
        this.accountGateway = accountGateway;
        this.auditGateway = auditGateway;
        this.webAuthnRecoveryService = webAuthnRecoveryService;
        this.objectMapper = objectMapper;
    }

    public WebAuthnEnrollmentTicketResponse issueFirstEnrollmentTicket(CurrentUser currentUser,
                                                                       CreateWebAuthnEnrollmentTicketRequest request) {
        if (!currentUser.isAdmin()) {
            throw new BusinessException("Only administrators can issue first-authenticator enrollment tickets");
        }

        InternalAccountResponse targetAccount = accountGateway.getInternalAccount(request.userId());
        if (!"ACTIVE".equalsIgnoreCase(targetAccount.status())) {
            throw new BusinessException("First-authenticator enrollment tickets can only be issued to ACTIVE accounts");
        }

        List<MfaRegistration> activeWebAuthnRegistrations = mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                request.userId(),
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        );
        if (!activeWebAuthnRegistrations.isEmpty()) {
            throw new BusinessException("Target account already has an active WebAuthn authenticator");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(stepUpProperties.enrollmentTicketTtlSeconds());
        String enrollmentTicket = UUID.randomUUID().toString();

        WebAuthnEnrollmentTicketState state = new WebAuthnEnrollmentTicketState(
                enrollmentTicket,
                request.userId(),
                currentUser.accountId(),
                issuedAt,
                expiresAt,
                null
        );
        stepUpRedisStore.saveWebAuthnEnrollmentTicket(state);

        publishAudit(
                "WEBAUTHN_FIRST_ENROLLMENT_TICKET_ISSUED",
                currentUser.accountId(),
                "ADMIN",
                request.userId(),
                "Admin issued a first-authenticator enrollment ticket",
                payload(
                        "userId", request.userId(),
                        "issuedAt", issuedAt,
                        "expiresAt", expiresAt
                )
        );

        return new WebAuthnEnrollmentTicketResponse(
                enrollmentTicket,
                request.userId(),
                currentUser.accountId(),
                issuedAt,
                expiresAt
        );
    }

    public WebAuthnRegistrationOptionsResponse beginRegistration(CurrentUser currentUser,
                                                                 BeginWebAuthnRegistrationRequest request) {
        UUID recoveryRequestId = authorizeRegistrationEnrollment(currentUser, request);

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(webAuthnProperties.timeoutMillis());
        UUID ceremonyId = UUID.randomUUID();

        UserIdentity userIdentity = UserIdentity.builder()
                .name(currentUser.accountId().toString())
                .displayName(StringUtils.hasText(currentUser.username()) ? currentUser.username() : currentUser.accountId().toString())
                .id(userHandleCodec.fromUserId(currentUser.accountId()))
                .build();

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                                .residentKey(ResidentKeyRequirement.PREFERRED)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .timeout(webAuthnProperties.timeoutMillis())
                        .build()
        );

        WebAuthnRegistrationCeremonyState state = new WebAuthnRegistrationCeremonyState(
                ceremonyId,
                currentUser.accountId(),
                currentUser.username(),
                request.label().trim(),
                normalizeIssuer(request.issuer()),
                toJson(options),
                recoveryRequestId,
                now,
                expiresAt
        );
        stepUpRedisStore.saveWebAuthnRegistrationCeremony(state);

        publishAudit(
                "WEBAUTHN_REGISTRATION_STARTED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                ceremonyId,
                "WebAuthn registration ceremony started",
                payload(
                        "label", state.label(),
                        "issuer", state.issuer(),
                        "recoveryRequestId", state.recoveryRequestId(),
                        "expiresAt", state.expiresAt()
                )
        );

        return new WebAuthnRegistrationOptionsResponse(
                ceremonyId,
                toCredentialsCreateJson(options),
                expiresAt
        );
    }

    private UUID authorizeRegistrationEnrollment(CurrentUser currentUser,
                                                 BeginWebAuthnRegistrationRequest request) {
        List<MfaRegistration> activeWebAuthnRegistrations = mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                currentUser.accountId(),
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        );

        if (StringUtils.hasText(request.enrollmentTicket())) {
            String enrollmentTicket = request.enrollmentTicket().trim();
            if (webAuthnRecoveryService.hasRecoveryTicket(enrollmentTicket)) {
                UUID recoveryRequestId = webAuthnRecoveryService.authorizeApprovedRecoveryTicket(currentUser.accountId(), enrollmentTicket);
                publishAudit(
                        "WEBAUTHN_RECOVERY_ENROLLMENT_AUTHORIZED",
                        currentUser.accountId(),
                        "USER",
                        recoveryRequestId,
                        "Recovery ticket authorized replacement WebAuthn enrollment",
                        payload(
                                "recoveryRequestId", recoveryRequestId
                        )
                );
                return recoveryRequestId;
            }
        }

        webAuthnRecoveryService.assertNoOpenRecoveryRequest(currentUser.accountId());

        if (activeWebAuthnRegistrations.isEmpty()) {
            if (!StringUtils.hasText(request.enrollmentTicket())) {
                throw new BusinessException("The first WebAuthn authenticator must be enrolled with an admin-issued enrollment ticket");
            }

            String enrollmentTicket = request.enrollmentTicket().trim();
            WebAuthnEnrollmentTicketState ticket = stepUpRedisStore.findWebAuthnEnrollmentTicket(enrollmentTicket)
                    .orElseThrow(() -> new BusinessException("Enrollment ticket not found or already expired"));
            if (!ticket.userId().equals(currentUser.accountId())) {
                throw new BusinessException("Enrollment ticket does not belong to current user");
            }
            if (ticket.recoveryRequestId() != null) {
                throw new BusinessException("Recovery enrollment tickets cannot be used for first-time onboarding");
            }
            if (!Instant.now().isBefore(ticket.expiresAt())) {
                stepUpRedisStore.deleteWebAuthnEnrollmentTicket(enrollmentTicket);
                throw new BusinessException("Enrollment ticket has expired");
            }

            stepUpRedisStore.deleteWebAuthnEnrollmentTicket(enrollmentTicket);
            publishAudit(
                    "WEBAUTHN_FIRST_ENROLLMENT_AUTHORIZED",
                    currentUser.accountId(),
                    "USER",
                    ticket.userId(),
                    "First WebAuthn enrollment authorized by admin ticket",
                    payload(
                            "issuedByAdminId", ticket.issuedByAdminId(),
                            "issuedAt", ticket.issuedAt(),
                            "expiresAt", ticket.expiresAt()
                    )
            );
            return null;
        }

        throw new BusinessException("Each account can only keep one active WebAuthn authenticator. Use the recovery flow if you need to replace the current device.");
    }

    @Transactional
    public MfaRegistrationResponse finishRegistration(CurrentUser currentUser,
                                                      FinishWebAuthnRegistrationRequest request) {
        WebAuthnRegistrationCeremonyState state = stepUpRedisStore.findWebAuthnRegistrationCeremony(request.ceremonyId())
                .orElseThrow(() -> new BusinessException("WebAuthn registration ceremony not found or expired"));

        if (!state.userId().equals(currentUser.accountId())) {
            throw new BusinessException("WebAuthn registration ceremony does not belong to current user");
        }
        if (!Instant.now().isBefore(state.expiresAt())) {
            stepUpRedisStore.deleteWebAuthnRegistrationCeremony(request.ceremonyId());
            throw new BusinessException("WebAuthn registration ceremony has expired");
        }

        PublicKeyCredentialCreationOptions options = fromCreationOptionsJson(state.optionsJson());
        PublicKeyCredential<?, ?> credential = parseRegistrationCredential(request.credentialJson());

        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(options)
                            .response((PublicKeyCredential) credential)
                            .build()
            );
        } catch (RegistrationFailedException ex) {
            throw new BusinessException("WebAuthn registration failed: " + ex.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<MfaRegistration> activeRegistrations = mfaRegistrationRepository.findByUserIdAndRegistrationStatus(
                currentUser.accountId(),
                MfaRegistrationStatus.ACTIVE
        );
        List<MfaRegistration> activeWebAuthnRegistrations = activeRegistrations.stream()
                .filter(existing -> existing.getMfaType() == MfaType.WEBAUTHN)
                .toList();
        List<UUID> disabledRegistrationIds = List.of();

        if (state.recoveryRequestId() == null && !activeWebAuthnRegistrations.isEmpty()) {
            stepUpRedisStore.deleteWebAuthnRegistrationCeremony(request.ceremonyId());
            throw new BusinessException("Only one active WebAuthn authenticator is allowed per account");
        }

        if (state.recoveryRequestId() != null) {
            activeWebAuthnRegistrations.forEach(existing -> {
                existing.setRegistrationStatus(MfaRegistrationStatus.DISABLED);
                existing.setDisabledAt(now);
                existing.setUpdatedAt(now);
            });
            mfaRegistrationRepository.saveAll(activeWebAuthnRegistrations);
            disabledRegistrationIds = activeWebAuthnRegistrations.stream().map(MfaRegistration::getId).toList();
        }

        MfaRegistration registration = new MfaRegistration();
        registration.setId(UUID.randomUUID());
        registration.setUserId(currentUser.accountId());
        registration.setMfaType(MfaType.WEBAUTHN);
        registration.setIssuer(state.issuer());
        registration.setLabel(state.label());
        registration.setRegistrationStatus(MfaRegistrationStatus.ACTIVE);
        registration.setCreatedAt(now);
        registration.setActivatedAt(now);
        registration.setUpdatedAt(now);
        registration.setWebauthnCredentialId(result.getKeyId().getId().getBase64Url());
        registration.setWebauthnPublicKeyCose(result.getPublicKeyCose().getBase64Url());
        registration.setWebauthnSignatureCount(result.getSignatureCount());
        registration.setWebauthnAaguid(result.getAaguid().getHex());
        registration.setWebauthnTransports(toJson(
                result.getKeyId().getTransports()
                        .map(transports -> transports.stream().map(Object::toString).toList())
                        .orElse(List.of())
        ));
        mfaRegistrationRepository.save(registration);
        stepUpRedisStore.deleteWebAuthnRegistrationCeremony(request.ceremonyId());

        if (state.recoveryRequestId() != null) {
            webAuthnRecoveryService.completeRecovery(
                    currentUser.accountId(),
                    state.recoveryRequestId(),
                    registration.getId(),
                    disabledRegistrationIds
            );
        }

        publishAudit(
                "WEBAUTHN_REGISTRATION_COMPLETED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                registration.getId(),
                "WebAuthn registration completed successfully",
                payload(
                        "registrationId", registration.getId(),
                        "credentialId", registration.getWebauthnCredentialId(),
                        "aaguid", registration.getWebauthnAaguid(),
                        "signCount", registration.getWebauthnSignatureCount(),
                        "recoveryRequestId", state.recoveryRequestId()
                )
        );

        return toResponse(registration);
    }

    public WebAuthnAssertionOptionsResponse beginAssertion(CurrentUser currentUser,
                                                           BeginWebAuthnAssertionRequest request) {
        webAuthnRecoveryService.assertNoOpenRecoveryRequest(currentUser.accountId());
        MfaRegistration registration = getActiveWebAuthnRegistration(currentUser.accountId(), request.registrationId());
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(webAuthnProperties.timeoutMillis());
        UUID ceremonyId = UUID.randomUUID();

        AssertionRequest assertionRequest = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .username(currentUser.accountId().toString())
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .timeout(webAuthnProperties.timeoutMillis())
                        .build()
        );

        WebAuthnAssertionCeremonyState state = new WebAuthnAssertionCeremonyState(
                ceremonyId,
                currentUser.accountId(),
                registration.getId(),
                toJson(assertionRequest),
                request.action().trim(),
                normalizeNullable(request.targetType()),
                request.targetId(),
                now,
                expiresAt
        );
        stepUpRedisStore.saveWebAuthnAssertionCeremony(state);

        publishAudit(
                "WEBAUTHN_ASSERTION_STARTED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                ceremonyId,
                "WebAuthn assertion ceremony started",
                payload(
                        "registrationId", registration.getId(),
                        "action", state.action(),
                        "targetType", state.targetType(),
                        "targetId", state.targetId(),
                        "expiresAt", state.expiresAt()
                )
        );

        return new WebAuthnAssertionOptionsResponse(
                ceremonyId,
                registration.getId(),
                state.action(),
                state.targetType(),
                state.targetId(),
                toCredentialsGetJson(assertionRequest),
                expiresAt
        );
    }

    @Transactional
    public StepUpVerificationResponse finishAssertion(CurrentUser currentUser,
                                                      FinishWebAuthnAssertionRequest request) {
        WebAuthnAssertionCeremonyState state = stepUpRedisStore.findWebAuthnAssertionCeremony(request.ceremonyId())
                .orElseThrow(() -> new BusinessException("WebAuthn assertion ceremony not found or expired"));

        if (!state.userId().equals(currentUser.accountId())) {
            throw new BusinessException("WebAuthn assertion ceremony does not belong to current user");
        }
        if (!Instant.now().isBefore(state.expiresAt())) {
            stepUpRedisStore.deleteWebAuthnAssertionCeremony(request.ceremonyId());
            throw new BusinessException("WebAuthn assertion ceremony has expired");
        }

        AssertionRequest assertionRequest = fromAssertionRequestJson(state.requestJson());
        PublicKeyCredential<?, ?> credential = parseAssertionCredential(request.credentialJson());

        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(assertionRequest)
                            .response((PublicKeyCredential) credential)
                            .build()
            );
        } catch (AssertionFailedException ex) {
            saveVerificationLog(state, false, "WebAuthn assertion failed", Map.of("reason", ex.getMessage()));
            throw new BusinessException("WebAuthn verification failed: " + ex.getMessage());
        }

        if (!result.isSuccess()) {
            saveVerificationLog(state, false, "WebAuthn assertion did not succeed", Map.of());
            throw new BusinessException("WebAuthn verification did not succeed");
        }

        MfaRegistration registration = mfaRegistrationRepository.findByWebauthnCredentialId(result.getCredentialId().getBase64Url())
                .orElseThrow(() -> new ResourceNotFoundException("WebAuthn registration not found for credential"));

        if (!registration.getUserId().equals(currentUser.accountId())) {
            throw new BusinessException("WebAuthn credential does not belong to current user");
        }
        if (state.registrationId() != null && !registration.getId().equals(state.registrationId())) {
            throw new BusinessException("WebAuthn credential does not match selected registration");
        }

        registration.setWebauthnSignatureCount(result.getSignatureCount());
        registration.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mfaRegistrationRepository.save(registration);
        stepUpRedisStore.deleteWebAuthnAssertionCeremony(request.ceremonyId());

        Instant verifiedAt = Instant.now();
        Instant proofExpiresAt = verifiedAt.plusSeconds(stepUpProperties.proofTtlSeconds());
        String verificationToken = UUID.randomUUID().toString();

        StepUpVerificationProofState proofState = new StepUpVerificationProofState(
                verificationToken,
                state.ceremonyId(),
                currentUser.accountId(),
                registration.getId(),
                state.action(),
                state.targetType(),
                state.targetId(),
                verifiedAt,
                proofExpiresAt
        );
        stepUpRedisStore.saveProof(proofState);

        saveVerificationLog(
                state,
                true,
                null,
                Map.of(
                        "registrationId", registration.getId(),
                        "credentialId", registration.getWebauthnCredentialId(),
                        "proofExpiresAt", proofExpiresAt,
                        "signatureCounterValid", result.isSignatureCounterValid()
                )
        );

        publishAudit(
                "WEBAUTHN_ASSERTION_VERIFIED",
                currentUser.accountId(),
                "USER",
                state.ceremonyId(),
                "WebAuthn assertion verified successfully",
                payload(
                        "registrationId", registration.getId(),
                        "credentialId", registration.getWebauthnCredentialId(),
                        "action", state.action(),
                        "targetType", state.targetType(),
                        "targetId", state.targetId(),
                        "verifiedAt", verifiedAt,
                        "proofExpiresAt", proofExpiresAt
                )
        );

        return new StepUpVerificationResponse(
                true,
                state.ceremonyId(),
                verificationToken,
                registration.getId(),
                state.action(),
                state.targetType(),
                state.targetId(),
                verifiedAt,
                proofExpiresAt
        );
    }

    private MfaRegistration getActiveWebAuthnRegistration(UUID userId, UUID registrationId) {
        if (registrationId != null) {
            MfaRegistration registration = mfaRegistrationRepository.findByIdAndUserId(registrationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("WebAuthn registration not found"));
            if (registration.getRegistrationStatus() != MfaRegistrationStatus.ACTIVE || registration.getMfaType() != MfaType.WEBAUTHN) {
                throw new BusinessException("Selected registration is not an active WebAuthn authenticator");
            }
            return registration;
        }

        return mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(userId, MfaRegistrationStatus.ACTIVE, MfaType.WEBAUTHN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("No active WebAuthn registration found for current user"));
    }

    private PublicKeyCredentialCreationOptions fromCreationOptionsJson(String json) {
        try {
            return PublicKeyCredentialCreationOptions.fromJson(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn registration options", ex);
        }
    }

    private AssertionRequest fromAssertionRequestJson(String json) {
        try {
            return AssertionRequest.fromJson(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn assertion request", ex);
        }
    }

    private PublicKeyCredential<?, ?> parseRegistrationCredential(String credentialJson) {
        try {
            return PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
        } catch (IOException ex) {
            throw new BusinessException("Unable to parse WebAuthn registration response");
        }
    }

    private PublicKeyCredential<?, ?> parseAssertionCredential(String credentialJson) {
        try {
            return PublicKeyCredential.parseAssertionResponseJson(credentialJson);
        } catch (IOException ex) {
            throw new BusinessException("Unable to parse WebAuthn assertion response");
        }
    }

    private String toCredentialsCreateJson(PublicKeyCredentialCreationOptions options) {
        try {
            return options.toCredentialsCreateJson();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn registration options", ex);
        }
    }

    private String toCredentialsGetJson(AssertionRequest assertionRequest) {
        try {
            return assertionRequest.toCredentialsGetJson();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn assertion options", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn state", ex);
        }
    }

    private String normalizeIssuer(String issuer) {
        return StringUtils.hasText(issuer) ? issuer.trim() : "CT556 PKI";
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void saveVerificationLog(WebAuthnAssertionCeremonyState state,
                                     boolean verified,
                                     String failureReason,
                                     Map<String, Object> metadata) {
        StepUpVerificationLog log = new StepUpVerificationLog();
        log.setId(UUID.randomUUID());
        log.setChallengeId(state.ceremonyId().toString());
        log.setUserId(state.userId());
        log.setMfaRegistrationId(state.registrationId());
        log.setAction(state.action());
        log.setTargetType(state.targetType());
        log.setTargetId(state.targetId());
        log.setVerified(verified);
        log.setFailureReason(failureReason);
        log.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        log.setMetadata(toJson(new LinkedHashMap<>(metadata)));
        verificationLogRepository.save(log);
    }

    private MfaRegistrationResponse toResponse(MfaRegistration registration) {
        return new MfaRegistrationResponse(
                registration.getId(),
                registration.getUserId(),
                registration.getMfaType().name(),
                registration.getIssuer(),
                registration.getLabel(),
                registration.getRegistrationStatus().name(),
                registration.getCreatedAt(),
                registration.getActivatedAt(),
                registration.getDisabledAt()
        );
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
                "STEP_UP_WEBAUTHN",
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
