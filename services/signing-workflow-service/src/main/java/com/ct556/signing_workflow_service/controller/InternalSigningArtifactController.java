package com.ct556.signing_workflow_service.controller;

import com.ct556.signing_workflow_service.dto.response.InternalSigningArtifactResponse;
import com.ct556.signing_workflow_service.service.InternalSigningArtifactQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/signing-requests")
public class InternalSigningArtifactController {

    private final InternalSigningArtifactQueryService internalSigningArtifactQueryService;

    public InternalSigningArtifactController(InternalSigningArtifactQueryService internalSigningArtifactQueryService) {
        this.internalSigningArtifactQueryService = internalSigningArtifactQueryService;
    }

    @GetMapping("/{requestId}/artifact")
    public InternalSigningArtifactResponse getArtifact(@PathVariable UUID requestId) {
        return internalSigningArtifactQueryService.getArtifactByRequestId(requestId);
    }
}
