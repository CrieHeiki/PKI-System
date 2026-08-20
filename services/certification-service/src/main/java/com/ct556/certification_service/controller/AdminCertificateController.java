package com.ct556.certification_service.controller;

import com.ct556.certification_service.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.dto.response.RevokeCertificateResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/admin/certificates")
public class AdminCertificateController {

    private final CertificateService certificateService;

    public AdminCertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public ResponseEntity<?> listAllCertificates() {
        return ResponseEntity.ok(certificateService.listAllCertificates());
    }
    @PostMapping("/{certificateId}/revoke")
    public ResponseEntity<RevokeCertificateResponse> revokeCertificate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String certificateId
    ) {
        return ResponseEntity.ok(
                certificateService.revokeCertificate(currentUser, java.util.UUID.fromString(certificateId))
        );
    }
}