package com.ct556.step_up_auth_service.service;

import com.ct556.step_up_auth_service.model.StepUpChallengeState;
import com.ct556.step_up_auth_service.model.StepUpVerificationProofState;
import com.ct556.step_up_auth_service.model.WebAuthnEnrollmentTicketState;
import com.ct556.step_up_auth_service.model.WebAuthnAssertionCeremonyState;
import com.ct556.step_up_auth_service.model.WebAuthnRegistrationCeremonyState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class StepUpRedisStore {

    private static final String CHALLENGE_KEY_PREFIX = "step-up:challenge:";
    private static final String PROOF_KEY_PREFIX = "step-up:proof:";
    private static final String WEBAUTHN_ENROLLMENT_TICKET_KEY_PREFIX = "step-up:webauthn:enrollment-ticket:";
    private static final String WEBAUTHN_REGISTRATION_KEY_PREFIX = "step-up:webauthn:registration:";
    private static final String WEBAUTHN_ASSERTION_KEY_PREFIX = "step-up:webauthn:assertion:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public StepUpRedisStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveChallenge(StepUpChallengeState state) {
        saveJson(challengeKey(state.challengeId()), state, state.expiresAt());
    }

    public Optional<StepUpChallengeState> findChallenge(UUID challengeId) {
        return readJson(challengeKey(challengeId), StepUpChallengeState.class);
    }

    public void deleteChallenge(UUID challengeId) {
        redisTemplate.delete(challengeKey(challengeId));
    }

    public void saveProof(StepUpVerificationProofState state) {
        saveJson(proofKey(state.verificationToken()), state, state.expiresAt());
    }

    public Optional<StepUpVerificationProofState> findProof(String verificationToken) {
        return readJson(proofKey(verificationToken), StepUpVerificationProofState.class);
    }

    public void deleteProof(String verificationToken) {
        redisTemplate.delete(proofKey(verificationToken));
    }

    public void saveWebAuthnEnrollmentTicket(WebAuthnEnrollmentTicketState state) {
        saveJson(webauthnEnrollmentTicketKey(state.enrollmentTicket()), state, state.expiresAt());
    }

    public Optional<WebAuthnEnrollmentTicketState> findWebAuthnEnrollmentTicket(String enrollmentTicket) {
        return readJson(webauthnEnrollmentTicketKey(enrollmentTicket), WebAuthnEnrollmentTicketState.class);
    }

    public void deleteWebAuthnEnrollmentTicket(String enrollmentTicket) {
        redisTemplate.delete(webauthnEnrollmentTicketKey(enrollmentTicket));
    }

    public void saveWebAuthnRegistrationCeremony(WebAuthnRegistrationCeremonyState state) {
        saveJson(webauthnRegistrationKey(state.ceremonyId()), state, state.expiresAt());
    }

    public Optional<WebAuthnRegistrationCeremonyState> findWebAuthnRegistrationCeremony(UUID ceremonyId) {
        return readJson(webauthnRegistrationKey(ceremonyId), WebAuthnRegistrationCeremonyState.class);
    }

    public void deleteWebAuthnRegistrationCeremony(UUID ceremonyId) {
        redisTemplate.delete(webauthnRegistrationKey(ceremonyId));
    }

    public void saveWebAuthnAssertionCeremony(WebAuthnAssertionCeremonyState state) {
        saveJson(webauthnAssertionKey(state.ceremonyId()), state, state.expiresAt());
    }

    public Optional<WebAuthnAssertionCeremonyState> findWebAuthnAssertionCeremony(UUID ceremonyId) {
        return readJson(webauthnAssertionKey(ceremonyId), WebAuthnAssertionCeremonyState.class);
    }

    public void deleteWebAuthnAssertionCeremony(UUID ceremonyId) {
        redisTemplate.delete(webauthnAssertionKey(ceremonyId));
    }

    private void saveJson(String key, Object value, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            redisTemplate.delete(key);
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to persist step-up state in Redis", ex);
        }
    }

    private <T> Optional<T> readJson(String key, Class<T> type) {
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(payload, type));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to read step-up state from Redis", ex);
        }
    }

    private String challengeKey(UUID challengeId) {
        return CHALLENGE_KEY_PREFIX + challengeId;
    }

    private String proofKey(String verificationToken) {
        return PROOF_KEY_PREFIX + verificationToken;
    }

    private String webauthnEnrollmentTicketKey(String enrollmentTicket) {
        return WEBAUTHN_ENROLLMENT_TICKET_KEY_PREFIX + enrollmentTicket;
    }

    private String webauthnRegistrationKey(UUID ceremonyId) {
        return WEBAUTHN_REGISTRATION_KEY_PREFIX + ceremonyId;
    }

    private String webauthnAssertionKey(UUID ceremonyId) {
        return WEBAUTHN_ASSERTION_KEY_PREFIX + ceremonyId;
    }
}
