package com.ct556.certification_service.controller;

import com.ct556.certification_service.dto.request.CreateRenewalRequestRequest;
import com.ct556.certification_service.dto.request.CreateRevocationRequestRequest;
import com.ct556.certification_service.dto.response.CertificateRequestResponse;
import com.ct556.certification_service.dto.response.CertificateResponse;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.service.CertificateRequestService;
import com.ct556.certification_service.service.CertificateService;
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
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateRequestService certificateRequestService;

    public CertificateController(CertificateService certificateService,
                                 CertificateRequestService certificateRequestService) {
        this.certificateService = certificateService;
        this.certificateRequestService = certificateRequestService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> listMyCertificates(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(certificateService.listMyCertificates(currentUser));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<CertificateResponse> getCertificateById(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String certificateId
    ) {
        return ResponseEntity.ok(
                certificateService.getCertificateById(currentUser, java.util.UUID.fromString(certificateId))
        );
    }

    @GetMapping("/serial/{serialNumber}")
    public ResponseEntity<CertificateResponse> getCertificateBySerial(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String serialNumber
    ) {
        return ResponseEntity.ok(certificateService.getCertificateBySerial(currentUser, serialNumber));
    }

    @PostMapping("/{certificateId}/renewal-requests")
    public ResponseEntity<CertificateRequestResponse> createRenewalRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String certificateId,
            @Valid @RequestBody CreateRenewalRequestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        certificateRequestService.createRenewalRequest(
                                currentUser,
                                java.util.UUID.fromString(certificateId),
                                request
                        )
                );
    }

    @PostMapping("/{certificateId}/revocation-requests")
    public ResponseEntity<CertificateRequestResponse> createRevocationRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String certificateId,
            @Valid @RequestBody CreateRevocationRequestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        certificateRequestService.createRevocationRequest(
                                currentUser,
                                java.util.UUID.fromString(certificateId),
                                request
                        )
                );
    }

    @PostMapping("/{certificateId}/revoke")
    public ResponseEntity<CertificateRequestResponse> createRevocationRequestLegacyAlias(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String certificateId,
            @Valid @RequestBody CreateRevocationRequestRequest request
    ) {
        return createRevocationRequest(currentUser, certificateId, request);
    }
}
