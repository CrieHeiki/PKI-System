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
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "webauthn_recovery_requests")
public class WebAuthnRecoveryRequest {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "webauthn_recovery_request_status")
    private WebAuthnRecoveryRequestStatus status;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "reviewed_by_admin_id")
    private UUID reviewedByAdminId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "recovery_ticket", length = 255)
    private String recoveryTicket;

    @Column(name = "recovery_ticket_issued_at")
    private LocalDateTime recoveryTicketIssuedAt;

    @Column(name = "recovery_ticket_expires_at")
    private LocalDateTime recoveryTicketExpiresAt;

    @Column(name = "completed_registration_id")
    private UUID completedRegistrationId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
