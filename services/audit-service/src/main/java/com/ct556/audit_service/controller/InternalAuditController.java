package com.ct556.audit_service.controller;

import com.ct556.audit_service.dto.request.CreateAuditEventRequest;
import com.ct556.audit_service.dto.response.ApiResponse;
import com.ct556.audit_service.dto.response.AuditEventResponse;
import com.ct556.audit_service.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/audit/events")
public class InternalAuditController {

    private final AuditService auditService;

    public InternalAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ApiResponse<AuditEventResponse> createEvent(@Valid @RequestBody CreateAuditEventRequest request) {
        return new ApiResponse<>(
                true,
                auditService.createEvent(request),
                "Audit event stored successfully"
        );
    }
}
