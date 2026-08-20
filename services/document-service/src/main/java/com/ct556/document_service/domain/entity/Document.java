package com.ct556.document_service.domain.entity;

import com.ct556.document_service.domain.enumtype.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_documents_owner_account_id", columnList = "owner_account_id"),
                @Index(name = "idx_documents_created_at", columnList = "created_at")
        }
)
public class Document extends BaseAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_account_id", nullable = false)
    private UUID ownerAccountId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentStatus status;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentVersion> versions = new ArrayList<>();
}
