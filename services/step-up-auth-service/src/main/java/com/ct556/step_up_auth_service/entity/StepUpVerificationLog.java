package com.ct556.step_up_auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "step_up_verification_logs")
public class StepUpVerificationLog {

    @Id
    private UUID id;

    @Column(name = "challenge_id", nullable = false, length = 255)
    private String challengeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mfa_registration_id")
    private UUID mfaRegistrationId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}
