package com.ct556.certification_service.controller;

import com.ct556.certification_service.dto.request.InternalCertificateSignRequest;
import com.ct556.certification_service.dto.response.InternalCertificateMetadataResponse;
import com.ct556.certification_service.dto.response.InternalCertificateSignResponse;
import com.ct556.certification_service.service.InternalCertificateQueryService;
import com.ct556.certification_service.service.InternalCertificateSigningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/certificates")
public class InternalCertificateController {

    private final InternalCertificateQueryService internalCertificateQueryService;
    private final InternalCertificateSigningService internalCertificateSigningService;

    public InternalCertificateController(InternalCertificateQueryService internalCertificateQueryService,
                                         InternalCertificateSigningService internalCertificateSigningService) {
        this.internalCertificateQueryService = internalCertificateQueryService;
        this.internalCertificateSigningService = internalCertificateSigningService;
    }

    @GetMapping("/{certificateId}")
    public InternalCertificateMetadataResponse getCertificateMetadata(@PathVariable UUID certificateId) {
        return internalCertificateQueryService.getCertificateMetadata(certificateId);
    }

    @GetMapping("/serial/{serialNumber}")
    public InternalCertificateMetadataResponse getCertificateMetadataBySerial(@PathVariable String serialNumber) {
        return internalCertificateQueryService.getCertificateMetadataBySerial(serialNumber);
    }

    @PostMapping("/{certificateId}/sign")
    public InternalCertificateSignResponse sign(@PathVariable UUID certificateId,
                                                @Valid @RequestBody InternalCertificateSignRequest request) {
        return internalCertificateSigningService.sign(certificateId, request);
    }
}
