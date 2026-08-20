package com.ct556.audit_service.controller;

import com.ct556.audit_service.dto.response.ApiResponse;
import com.ct556.audit_service.dto.response.AuditEventResponse;
import com.ct556.audit_service.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit/events")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<AuditEventResponse>> searchEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return new ApiResponse<>(
                true,
                auditService.searchEvents(eventType, actorId, resourceType, resourceId, correlationId, from, to, limit),
                "Audit events fetched successfully"
        );
    }

    @GetMapping("/{eventId}")
    public ApiResponse<AuditEventResponse> getEvent(@PathVariable UUID eventId) {
        return new ApiResponse<>(
                true,
                auditService.getEvent(eventId),
                "Audit event fetched successfully"
        );
    }
}
