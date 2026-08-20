package com.ct556.verification_service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "verification_report_details")
public class VerificationReportDetail {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private VerificationReport report;

    @Column(name = "check_name", nullable = false, length = 100)
    private String checkName;

    @Column(name = "check_result", nullable = false)
    private boolean checkResult;

    @Column(name = "detail_message", columnDefinition = "TEXT")
    private String detailMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private JsonNode rawData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
