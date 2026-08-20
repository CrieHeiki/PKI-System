package com.ct556.signing_workflow_service.entity;

import com.ct556.signing_workflow_service.domain.SigningEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signing_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SigningEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signing_request_id", nullable = false)
    private SigningRequest signingRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SigningEventType eventType;

    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (eventTime == null) eventTime = LocalDateTime.now();
    }
}