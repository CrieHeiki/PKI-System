package com.ct556.certification_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trusted_timestamp_authorities")
public class TrustedTimestampAuthorityEntity {

    @Id
    private UUID id;

    @Column(name = "common_name", nullable = false, length = 255)
    private String commonName;

    @Column(name = "subject_dn", nullable = false, length = 500)
    private String subjectDn;

    @Column(name = "serial_number", nullable = false, unique = true, length = 255)
    private String serialNumber;

    @Column(name = "vault_transit_key_name", nullable = false, length = 255)
    private String vaultTransitKeyName;

    @Column(name = "certificate_pem", nullable = false, columnDefinition = "TEXT")
    private String certificatePem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TrustedTimestampAuthorityStatus status;

    @Column(name = "policy_oid", nullable = false, length = 100)
    private String policyOid;

    @Column(name = "ordering_enabled", nullable = false)
    private boolean orderingEnabled;

    @Column(name = "accuracy_seconds", nullable = false)
    private int accuracySeconds;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getVaultTransitKeyName() {
        return vaultTransitKeyName;
    }

    public void setVaultTransitKeyName(String vaultTransitKeyName) {
        this.vaultTransitKeyName = vaultTransitKeyName;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public TrustedTimestampAuthorityStatus getStatus() {
        return status;
    }

    public void setStatus(TrustedTimestampAuthorityStatus status) {
        this.status = status;
    }

    public String getPolicyOid() {
        return policyOid;
    }

    public void setPolicyOid(String policyOid) {
        this.policyOid = policyOid;
    }

    public boolean isOrderingEnabled() {
        return orderingEnabled;
    }

    public void setOrderingEnabled(boolean orderingEnabled) {
        this.orderingEnabled = orderingEnabled;
    }

    public int getAccuracySeconds() {
        return accuracySeconds;
    }

    public void setAccuracySeconds(int accuracySeconds) {
        this.accuracySeconds = accuracySeconds;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
