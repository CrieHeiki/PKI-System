package com.ct556.account_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "signature_image", columnDefinition = "bytea")
    private byte[] signatureImage;

    @Column(name = "signature_image_content_type", length = 100)
    private String signatureImageContentType;

    @Column(name = "signature_image_updated_at")
    private LocalDateTime signatureImageUpdatedAt;

    @Column(name = "signature_image_only", nullable = false)
    private boolean signatureImageOnly;

    @Column(name = "signature_box_x_percent", nullable = false)
    private int signatureBoxXPercent;

    @Column(name = "signature_box_y_percent", nullable = false)
    private int signatureBoxYPercent;

    @Column(name = "signature_box_width_percent", nullable = false)
    private int signatureBoxWidthPercent;

    @Column(name = "signature_box_height_percent", nullable = false)
    private int signatureBoxHeightPercent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
