package com.ct556.verification_service.controller;

import com.ct556.verification_service.dto.request.VerifySignedOutputRequest;
import com.ct556.verification_service.dto.request.VerifySigningRequestRequest;
import com.ct556.verification_service.dto.response.ApiResponse;
import com.ct556.verification_service.dto.response.VerificationReportResponse;
import com.ct556.verification_service.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/signed-output")
    public ApiResponse<VerificationReportResponse> verifySignedOutput(@Valid @RequestBody VerifySignedOutputRequest request) {
        return new ApiResponse<>(
                true,
                verificationService.verifySignedOutput(request),
                "Signed output verified successfully"
        );
    }

    @PostMapping("/signing-requests/{requestId}")
    public ApiResponse<VerificationReportResponse> verifySigningRequest(@PathVariable UUID requestId,
                                                                        @Valid @RequestBody(required = false) VerifySigningRequestRequest request) {
        return new ApiResponse<>(
                true,
                verificationService.verifySigningRequest(requestId, request),
                "Signing request verified successfully"
        );
    }

    @PostMapping("/files")
    public ApiResponse<VerificationReportResponse> verifyUploadedFile(@RequestParam("file") MultipartFile file,
                                                                      @RequestParam(value = "verifiedBy", required = false) UUID verifiedBy,
                                                                      @RequestParam(value = "trustSource", required = false) String trustSource,
                                                                      @RequestParam(value = "trustAnchors", required = false) List<MultipartFile> trustAnchors) {
        return new ApiResponse<>(
                true,
                verificationService.verifyUploadedFile(file, verifiedBy, trustSource, trustAnchors),
                "Uploaded file verified successfully"
        );
    }

    @GetMapping("/reports/{reportId}")
    public ApiResponse<VerificationReportResponse> getReport(@PathVariable UUID reportId) {
        return new ApiResponse<>(
                true,
                verificationService.getReport(reportId),
                "Verification report fetched successfully"
        );
    }

    @GetMapping("/signing-requests/{requestId}/latest")
    public ApiResponse<VerificationReportResponse> getLatestReportBySigningRequest(@PathVariable UUID requestId) {
        return new ApiResponse<>(
                true,
                verificationService.getLatestReportBySigningRequest(requestId),
                "Latest verification report fetched successfully"
        );
    }

    @GetMapping("/reports")
    public ApiResponse<List<VerificationReportResponse>> getRecentReports() {
        return new ApiResponse<>(
                true,
                verificationService.getRecentReports(),
                "Recent verification reports fetched successfully"
        );
    }
}
