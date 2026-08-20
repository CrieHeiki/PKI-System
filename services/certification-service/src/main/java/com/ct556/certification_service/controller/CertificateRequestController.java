package com.ct556.certification_service.controller;

import com.ct556.certification_service.dto.request.CreateCertificateRequestRequest;
import com.ct556.certification_service.dto.response.CertificateRequestResponse;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.service.CertificateRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certificate-requests")
public class CertificateRequestController {

    private final CertificateRequestService certificateRequestService;

    public CertificateRequestController(CertificateRequestService certificateRequestService) {
        this.certificateRequestService = certificateRequestService;
    }

    @PostMapping
    public ResponseEntity<CertificateRequestResponse> createRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateCertificateRequestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateRequestService.createRequest(currentUser, request));
    }

    @GetMapping("/my")
    public ResponseEntity<?> listMyRequests(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(certificateRequestService.listMyRequests(currentUser));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<CertificateRequestResponse> getMyRequestById(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String requestId
    ) {
        return ResponseEntity.ok(
                certificateRequestService.getMyRequestById(currentUser, java.util.UUID.fromString(requestId))
        );
    }
}