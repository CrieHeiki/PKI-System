package com.ct556.signing_workflow_service.controller;

import com.ct556.signing_workflow_service.dto.response.ApiResponse;
import com.ct556.signing_workflow_service.dto.response.SigningRequestResponse;
import com.ct556.signing_workflow_service.service.SigningWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/signing-requests")
public class AdminSigningRequestController {

    private final SigningWorkflowService signingWorkflowService;

    public AdminSigningRequestController(SigningWorkflowService signingWorkflowService) {
        this.signingWorkflowService = signingWorkflowService;
    }

    @GetMapping
    public ApiResponse<List<SigningRequestResponse>> getAll() {
        return new ApiResponse<>(
                true,
                signingWorkflowService.getAllForAdmin(),
                "Fetched all signing workflows"
        );
    }
}