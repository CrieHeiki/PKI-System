package com.ct556.verification_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "verification_reports")
public class VerificationReport {

    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_version")
    private Integer documentVersion;

    @Column(name = "signing_request_id")
    private UUID signingRequestId;

    @Column(name = "certificate_id")
    private UUID certificateId;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "verification_status", nullable = false, columnDefinition = "verification_status")
    private VerificationStatus verificationStatus;

    @Column(name = "signed_object_key", length = 500)
    private String signedObjectKey;

    @Column(name = "signed_file_name", length = 255)
    private String signedFileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "signer_certificate_serial", length = 255)
    private String signerCertificateSerial;

    @Column(name = "signer_subject_dn", columnDefinition = "TEXT")
    private String signerSubjectDn;

    @Column(name = "issuer_dn", columnDefinition = "TEXT")
    private String issuerDn;

    @Column(name = "trust_source", nullable = false, length = 50)
    private String trustSource;

    @Column(name = "integrity_valid", nullable = false)
    private boolean integrityValid;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "trust_chain_valid", nullable = false)
    private boolean trustChainValid;

    @Column(name = "expiration_valid", nullable = false)
    private boolean expirationValid;

    @Column(name = "revocation_valid", nullable = false)
    private boolean revocationValid;

    @Column(name = "revocation_source", length = 50)
    private String revocationSource;

    @Column(name = "digest_algorithm", length = 100)
    private String digestAlgorithm;

    @Column(name = "signature_algorithm", length = 100)
    private String signatureAlgorithm;

    @Column(name = "overall_message", columnDefinition = "TEXT")
    private String overallMessage;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<VerificationReportDetail> details = new ArrayList<>();

    public void addDetail(VerificationReportDetail detail) {
        detail.setReport(this);
        details.add(detail);
    }
}
