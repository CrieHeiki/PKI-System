package com.ct556.step_up_auth_service.service;

import com.ct556.step_up_auth_service.client.AuditGateway;
import com.ct556.step_up_auth_service.config.StepUpProperties;
import com.ct556.step_up_auth_service.dto.request.ConsumeStepUpVerificationRequest;
import com.ct556.step_up_auth_service.dto.request.CreateStepUpChallengeRequest;
import com.ct556.step_up_auth_service.dto.request.VerifyStepUpChallengeRequest;
import com.ct556.step_up_auth_service.dto.response.InternalStepUpVerificationResponse;
import com.ct556.step_up_auth_service.dto.response.StepUpChallengeResponse;
import com.ct556.step_up_auth_service.dto.response.StepUpVerificationResponse;
import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.StepUpVerificationLog;
import com.ct556.step_up_auth_service.exception.BusinessException;
import com.ct556.step_up_auth_service.model.StepUpChallengeState;
import com.ct556.step_up_auth_service.model.StepUpVerificationProofState;
import com.ct556.step_up_auth_service.repository.StepUpVerificationLogRepository;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class StepUpChallengeService {

    private final MfaRegistrationService mfaRegistrationService;
    private final StepUpRedisStore stepUpRedisStore;
    private final StepUpVerificationLogRepository verificationLogRepository;
    private final StepUpProperties stepUpProperties;
    private final ObjectMapper objectMapper;
    private final AuditGateway auditGateway;
    private final SecureRandom secureRandom = new SecureRandom();

    public StepUpChallengeService(MfaRegistrationService mfaRegistrationService,
                                  StepUpRedisStore stepUpRedisStore,
                                  StepUpVerificationLogRepository verificationLogRepository,
                                  StepUpProperties stepUpProperties,
                                  ObjectMapper objectMapper,
                                  AuditGateway auditGateway) {
        this.mfaRegistrationService = mfaRegistrationService;
        this.stepUpRedisStore = stepUpRedisStore;
        this.verificationLogRepository = verificationLogRepository;
        this.stepUpProperties = stepUpProperties;
        this.objectMapper = objectMapper;
        this.auditGateway = auditGateway;
    }

    public StepUpChallengeResponse createChallenge(CurrentUser currentUser, CreateStepUpChallengeRequest request) {
        throw new BusinessException("OTP/TOTP step-up challenge flow is disabled. Use WebAuthn verification.");
    }

    @Transactional
    public StepUpVerificationResponse verifyChallenge(CurrentUser currentUser,
                                                      UUID challengeId,
                                                      VerifyStepUpChallengeRequest request) {
        throw new BusinessException("OTP/TOTP verification flow is disabled. Use WebAuthn verification.");
    }

    public InternalStepUpVerificationResponse consumeVerification(ConsumeStepUpVerificationRequest request) {
        Optional<StepUpVerificationProofState> proofOptional = stepUpRedisStore.findProof(request.verificationToken());
        if (proofOptional.isEmpty()) {
            return invalidResponse("Verification token not found or already expired");
        }

        StepUpVerificationProofState proof = proofOptional.get();
        Instant now = Instant.now();
        if (!now.isBefore(proof.expiresAt())) {
            stepUpRedisStore.deleteProof(request.verificationToken());
            return invalidResponse("Verification token has expired");
        }

        if (!proof.userId().equals(request.userId())) {
            return invalidResponse("Verification token does not belong to the requested user");
        }

        if (!proof.action().equalsIgnoreCase(request.action().trim())) {
            return invalidResponse("Verification action does not match");
        }

        if (StringUtils.hasText(request.targetType())
                && !request.targetType().trim().equalsIgnoreCase(String.valueOf(proof.targetType()))) {
            return invalidResponse("Verification targetType does not match");
        }

        if (request.targetId() != null && !request.targetId().equals(proof.targetId())) {
            return invalidResponse("Verification targetId does not match");
        }

        boolean consume = request.consume() == null || request.consume();
        if (consume) {
            stepUpRedisStore.deleteProof(request.verificationToken());
        }

        publishAudit(
                "STEP_UP_VERIFICATION_CONSUMED",
                proof.userId(),
                proof.challengeId(),
                consume
                        ? "Step-up verification token consumed"
                        : "Step-up verification token validated without consuming",
                payload(
                        "action", proof.action(),
                        "targetType", proof.targetType(),
                        "targetId", proof.targetId(),
                        "registrationId", proof.registrationId(),
                        "verifiedAt", proof.verifiedAt(),
                        "expiresAt", proof.expiresAt(),
                        "consumed", consume
                )
        );

        return new InternalStepUpVerificationResponse(
                true,
                consume,
                proof.userId(),
                proof.action(),
                proof.targetType(),
                proof.targetId(),
                proof.registrationId(),
                proof.verifiedAt(),
                proof.expiresAt(),
                consume ? "Verification token is valid and consumed" : "Verification token is valid"
        );
    }

    private void saveVerificationLog(StepUpChallengeState challenge,
                                     boolean verified,
                                     String failureReason,
                                     Map<String, Object> metadata) {

        StepUpVerificationLog log = new StepUpVerificationLog();
        log.setId(UUID.randomUUID());
        log.setChallengeId(challenge.challengeId().toString());
        log.setUserId(challenge.userId());
        log.setMfaRegistrationId(challenge.registrationId());
        log.setAction(challenge.action());
        log.setTargetType(challenge.targetType());
        log.setTargetId(challenge.targetId());
        log.setVerified(verified);
        log.setFailureReason(failureReason);
        log.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        log.setMetadata(toJson(metadata));

        verificationLogRepository.save(log);
    }

    private String buildDeliveryHint(String label, String fallback) {
        if (!StringUtils.hasText(label)) {
            return fallback;
        }

        String trimmed = label.trim();
        if (trimmed.contains("@")) {
            int atIndex = trimmed.indexOf('@');
            if (atIndex > 1) {
                return trimmed.charAt(0) + "***" + trimmed.substring(atIndex - 1);
            }
            return "***" + trimmed.substring(atIndex);
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "***" + digits.substring(digits.length() - 4);
        }

        return trimmed;
    }

    private String generateNumericCode(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private InternalStepUpVerificationResponse invalidResponse(String message) {
        return new InternalStepUpVerificationResponse(
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(metadata));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize step-up verification metadata", ex);
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void publishAudit(String eventType,
                              UUID actorId,
                              UUID targetId,
                              String summary,
                              LinkedHashMap<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                actorId,
                "USER",
                targetId,
                "STEP_UP_CHALLENGE",
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
