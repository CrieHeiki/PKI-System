package com.ct556.signing_workflow_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signing_artifacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SigningArtifact {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signing_request_id", nullable = false, unique = true)
    private SigningRequest signingRequest;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "digest_algorithm", nullable = false)
    private String digestAlgorithm;

    @Column(name = "signature_algorithm", nullable = false)
    private String signatureAlgorithm;

    @Column(name = "document_digest_base64", nullable = false, columnDefinition = "TEXT")
    private String documentDigestBase64;

    @Column(name = "signature_value_base64", nullable = false, columnDefinition = "TEXT")
    private String signatureValueBase64;

    @Column(name = "signed_payload_length", nullable = false)
    private Long signedPayloadLength;

    @Column(name = "output_object_key")
    private String outputObjectKey;

    @Column(name = "output_file_name")
    private String outputFileName;

    @Column(name = "output_content_type")
    private String outputContentType;

    @Column(name = "output_file_size")
    private Long outputFileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
