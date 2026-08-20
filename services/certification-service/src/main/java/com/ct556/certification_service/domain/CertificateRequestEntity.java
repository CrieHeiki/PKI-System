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
@Table(name = "certificate_requests")
public class CertificateRequestEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private CertificateRequestType requestType;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "request_reason", length = 1000)
    private String requestReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CertificateRequestStatus status;

    @Column(name = "result_certificate_id")
    private UUID resultCertificateId;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by_account_id")
    private UUID reviewedByAccountId;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CertificateRequestEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public CertificateRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(CertificateRequestType requestType) {
        this.requestType = requestType;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public CertificateRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CertificateRequestStatus status) {
        this.status = status;
    }

    public UUID getResultCertificateId() {
        return resultCertificateId;
    }

    public void setResultCertificateId(UUID resultCertificateId) {
        this.resultCertificateId = resultCertificateId;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    public void setReviewedByAccountId(UUID reviewedByAccountId) {
        this.reviewedByAccountId = reviewedByAccountId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
