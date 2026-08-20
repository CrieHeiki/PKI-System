package com.ct556.document_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "document_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_document_versions_document_version", columnNames = {"document_id", "version_number"}),
                @UniqueConstraint(name = "uk_document_versions_object_key", columnNames = {"object_key"})
        },
        indexes = {
                @Index(name = "idx_document_versions_document_id", columnList = "document_id"),
                @Index(name = "idx_document_versions_created_at", columnList = "created_at")
        }
)
public class DocumentVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256_hash", nullable = false, length = 128)
    private String sha256Hash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}