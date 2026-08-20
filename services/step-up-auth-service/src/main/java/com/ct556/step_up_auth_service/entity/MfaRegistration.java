package com.ct556.step_up_auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "mfa_registrations")
public class MfaRegistration {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "mfa_type", nullable = false, columnDefinition = "mfa_type")
    private MfaType mfaType;

    @Column(name = "webauthn_credential_id", columnDefinition = "TEXT")
    private String webauthnCredentialId;

    @Column(name = "webauthn_public_key_cose", columnDefinition = "TEXT")
    private String webauthnPublicKeyCose;

    @Column(name = "webauthn_signature_count")
    private Long webauthnSignatureCount;

    @Column(name = "webauthn_aaguid", length = 64)
    private String webauthnAaguid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webauthn_transports", columnDefinition = "jsonb")
    private String webauthnTransports;

    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "label", length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "registration_status", nullable = false, columnDefinition = "mfa_registration_status")
    private MfaRegistrationStatus registrationStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
