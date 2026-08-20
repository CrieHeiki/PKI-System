package com.ct556.certification_service.controller;

import com.ct556.certification_service.dto.request.InternalTimestampTokenRequest;
import com.ct556.certification_service.dto.response.InternalTimestampAuthorityMetadataResponse;
import com.ct556.certification_service.dto.response.InternalTimestampTokenResponse;
import com.ct556.certification_service.service.InternalTimestampAuthorityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/timestamps")
public class InternalTimestampController {

    private final InternalTimestampAuthorityService internalTimestampAuthorityService;

    public InternalTimestampController(InternalTimestampAuthorityService internalTimestampAuthorityService) {
        this.internalTimestampAuthorityService = internalTimestampAuthorityService;
    }

    @PostMapping("/token")
    public InternalTimestampTokenResponse issueTimestampToken(@Valid @RequestBody InternalTimestampTokenRequest request) {
        return internalTimestampAuthorityService.issueTimestampToken(request);
    }

    @GetMapping("/authority")
    public InternalTimestampAuthorityMetadataResponse getActiveAuthority() {
        return internalTimestampAuthorityService.getActiveAuthorityMetadata();
    }

    @GetMapping("/authority/serial/{serialNumber}")
    public InternalTimestampAuthorityMetadataResponse getAuthorityBySerial(@PathVariable String serialNumber) {
        return internalTimestampAuthorityService.getAuthorityMetadataBySerial(serialNumber);
    }
}
