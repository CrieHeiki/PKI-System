package com.ct556.step_up_auth_service.service;

import com.ct556.step_up_auth_service.client.AuditGateway;
import com.ct556.step_up_auth_service.dto.request.CreateMfaRegistrationRequest;
import com.ct556.step_up_auth_service.dto.request.ManageMfaRegistrationRequest;
import com.ct556.step_up_auth_service.dto.response.MfaRegistrationResponse;
import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.MfaRegistrationStatus;
import com.ct556.step_up_auth_service.entity.MfaType;
import com.ct556.step_up_auth_service.exception.BusinessException;
import com.ct556.step_up_auth_service.exception.ResourceNotFoundException;
import com.ct556.step_up_auth_service.model.StepUpVerificationProofState;
import com.ct556.step_up_auth_service.repository.MfaRegistrationRepository;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class MfaRegistrationService {

    private final MfaRegistrationRepository mfaRegistrationRepository;
    private final StepUpRedisStore stepUpRedisStore;
    private final ObjectMapper objectMapper;
    private final AuditGateway auditGateway;
    private final WebAuthnRecoveryService webAuthnRecoveryService;

    public MfaRegistrationService(MfaRegistrationRepository mfaRegistrationRepository,
                                  StepUpRedisStore stepUpRedisStore,
                                  ObjectMapper objectMapper,
                                  AuditGateway auditGateway,
                                  WebAuthnRecoveryService webAuthnRecoveryService) {
        this.mfaRegistrationRepository = mfaRegistrationRepository;
        this.stepUpRedisStore = stepUpRedisStore;
        this.objectMapper = objectMapper;
        this.auditGateway = auditGateway;
        this.webAuthnRecoveryService = webAuthnRecoveryService;
    }

    @Transactional
    public MfaRegistrationResponse createRegistration(CurrentUser currentUser, CreateMfaRegistrationRequest request) {
        throw new BusinessException("Shared-secret step-up methods are disabled. Use WebAuthn registration endpoints.");
    }

    @Transactional(readOnly = true)
    public List<MfaRegistrationResponse> getMyRegistrations(CurrentUser currentUser) {
        return mfaRegistrationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.accountId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MfaRegistrationResponse disableRegistration(CurrentUser currentUser,
                                                       UUID registrationId,
                                                       ManageMfaRegistrationRequest request) {
        webAuthnRecoveryService.assertNoOpenRecoveryRequest(currentUser.accountId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        MfaRegistration registration = getActiveRegistration(currentUser.accountId(), registrationId);
        if (registration.getMfaType() != MfaType.WEBAUTHN) {
            throw new BusinessException("Only WebAuthn authenticators can be self-disabled");
        }

        List<MfaRegistration> activeWebAuthnRegistrations = mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                currentUser.accountId(),
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        );
        if (activeWebAuthnRegistrations.size() <= 1) {
            throw new BusinessException("You cannot self-disable the last active WebAuthn authenticator. Contact an administrator for recovery.");
        }

        StepUpVerificationProofState proof = consumeManagementProof(
                currentUser,
                registration,
                request.authorizationVerificationToken(),
                "DISABLE_AUTHENTICATOR",
                "A fresh WebAuthn verification from the authenticator being disabled is required"
        );

        registration.setRegistrationStatus(MfaRegistrationStatus.DISABLED);
        registration.setDisabledAt(now);
        registration.setUpdatedAt(now);
        mfaRegistrationRepository.save(registration);

        publishAudit(
                "MFA_REGISTRATION_DISABLED",
                currentUser,
                registration.getId(),
                "MFA registration disabled",
                payload(
                        "registrationId", registration.getId(),
                        "mfaType", registration.getMfaType().name(),
                        "label", registration.getLabel(),
                        "disabledAt", registration.getDisabledAt(),
                        "proofChallengeId", proof.challengeId(),
                        "proofVerifiedAt", proof.verifiedAt()
                )
        );

        return toResponse(registration);
    }

    private StepUpVerificationProofState consumeManagementProof(CurrentUser currentUser,
                                                                MfaRegistration targetRegistration,
                                                                String authorizationVerificationToken,
                                                                String expectedAction,
                                                                String missingProofMessage) {
        if (!StringUtils.hasText(authorizationVerificationToken)) {
            throw new BusinessException(missingProofMessage);
        }

        String trimmedToken = authorizationVerificationToken.trim();
        StepUpVerificationProofState proof = stepUpRedisStore.findProof(trimmedToken)
                .orElseThrow(() -> new BusinessException("Authorization proof token not found or already expired"));

        if (!proof.userId().equals(currentUser.accountId())) {
            throw new BusinessException("Authorization proof token does not belong to current user");
        }
        if (!Instant.now().isBefore(proof.expiresAt())) {
            stepUpRedisStore.deleteProof(trimmedToken);
            throw new BusinessException("Authorization proof token has expired");
        }
        if (!expectedAction.equalsIgnoreCase(proof.action())) {
            throw new BusinessException("Authorization proof token action is invalid");
        }
        if (!"MFA_REGISTRATION".equalsIgnoreCase(String.valueOf(proof.targetType()))) {
            throw new BusinessException("Authorization proof token target type is invalid");
        }
        if (!targetRegistration.getId().equals(proof.targetId())) {
            throw new BusinessException("Authorization proof token target does not match the requested authenticator");
        }
        if (!targetRegistration.getId().equals(proof.registrationId())) {
            throw new BusinessException("You must verify using the same authenticator that is being modified");
        }

        MfaRegistration proofRegistration = getActiveRegistration(currentUser.accountId(), proof.registrationId());
        if (proofRegistration.getMfaType() != MfaType.WEBAUTHN) {
            throw new BusinessException("Authorization proof token must come from an active WebAuthn authenticator");
        }

        stepUpRedisStore.deleteProof(trimmedToken);
        return proof;
    }

    private MfaRegistration getActiveRegistration(UUID userId, UUID registrationId) {
        MfaRegistration registration = mfaRegistrationRepository.findByIdAndUserId(registrationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA registration not found"));

        if (registration.getRegistrationStatus() != MfaRegistrationStatus.ACTIVE) {
            throw new BusinessException("MFA registration is not active");
        }

        return registration;
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize MFA registration payload", ex);
        }
    }

    private void publishAudit(String eventType,
                              CurrentUser currentUser,
                              UUID targetId,
                              String summary,
                              LinkedHashMap<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                targetId,
                "STEP_UP_MFA_REGISTRATION",
                targetId,
                targetId.toString(),
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
