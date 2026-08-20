package com.ct556.certification_service.controller;

import com.ct556.certification_service.dto.request.RejectCertificateRequestRequest;
import com.ct556.certification_service.dto.response.ApproveCertificateRequestResponse;
import com.ct556.certification_service.dto.response.RejectCertificateRequestResponse;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.service.CertificateRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/certificate-requests")
public class AdminCertificateRequestController {

    private final CertificateRequestService certificateRequestService;

    public AdminCertificateRequestController(CertificateRequestService certificateRequestService) {
        this.certificateRequestService = certificateRequestService;
    }

    @GetMapping
    public ResponseEntity<?> listAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestType
    ) {
        return ResponseEntity.ok(certificateRequestService.listAllRequests(status, requestType));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApproveCertificateRequestResponse> approveRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String requestId
    ) {
        return ResponseEntity.ok(
                certificateRequestService.approveRequest(currentUser, java.util.UUID.fromString(requestId))
        );
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<RejectCertificateRequestResponse> rejectRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String requestId,
            @Valid @RequestBody RejectCertificateRequestRequest request
    ) {
        return ResponseEntity.ok(
                certificateRequestService.rejectRequest(currentUser, java.util.UUID.fromString(requestId), request)
        );
    }
}