package com.ct556.signing_workflow_service.controller;

import com.ct556.signing_workflow_service.dto.request.ApproveSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.request.CreateSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.request.ExecuteSigningRequest;
import com.ct556.signing_workflow_service.dto.request.RejectSigningRequestRequest;
import com.ct556.signing_workflow_service.dto.response.ApiResponse;
import com.ct556.signing_workflow_service.dto.response.SigningRequestResponse;
import com.ct556.signing_workflow_service.security.CurrentUser;
import com.ct556.signing_workflow_service.service.DownloadedSignedOutput;
import com.ct556.signing_workflow_service.service.DownloadedSourceDocument;
import com.ct556.signing_workflow_service.service.SigningExecutionService;
import com.ct556.signing_workflow_service.service.SigningWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/signing-requests")
public class SigningRequestController {

    private final SigningWorkflowService signingWorkflowService;
    private final SigningExecutionService signingExecutionService;

    public SigningRequestController(SigningWorkflowService signingWorkflowService,
                                    SigningExecutionService signingExecutionService) {
        this.signingWorkflowService = signingWorkflowService;
        this.signingExecutionService = signingExecutionService;
    }

    @PostMapping
    public ApiResponse<SigningRequestResponse> create(@Valid @RequestBody CreateSigningRequestRequest request,
                                                      Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.createRequest(request, currentUser),
                "Signing request created successfully");
    }

    @GetMapping("/requested-by-me")
    public ApiResponse<List<SigningRequestResponse>> requestedByMe(Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.getRequestedByMe(currentUser),
                "Fetched signing requests created by current user");
    }

    @GetMapping("/pending-for-me")
    public ApiResponse<List<SigningRequestResponse>> pendingForMe(Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.getPendingForMe(currentUser),
                "Fetched pending signing requests for current user");
    }

    @GetMapping("/{requestId}")
    public ApiResponse<SigningRequestResponse> getOne(@PathVariable UUID requestId,
                                                      Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.getById(requestId, currentUser),
                "Fetched signing request detail");
    }

    @GetMapping("/{requestId}/signed-output")
    public ResponseEntity<byte[]> downloadSignedOutput(@PathVariable UUID requestId,
                                                       Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        DownloadedSignedOutput downloadedSignedOutput = signingWorkflowService.downloadSignedOutput(requestId, currentUser);

        MediaType mediaType = resolveDownloadMediaType(downloadedSignedOutput.fileName(), downloadedSignedOutput.contentType());
        String contentDisposition = buildAttachmentContentDisposition(downloadedSignedOutput.fileName());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(downloadedSignedOutput.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(downloadedSignedOutput.content());
    }

    @GetMapping("/{requestId}/source-document")
    public ResponseEntity<byte[]> downloadSourceDocument(@PathVariable UUID requestId,
                                                         Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        DownloadedSourceDocument sourceDocument = signingWorkflowService.downloadSourceDocument(requestId, currentUser);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(sourceDocument.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(sourceDocument.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(sourceDocument.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(sourceDocument.content());
    }

    @PostMapping("/{requestId}/approve")
    public ApiResponse<SigningRequestResponse> approve(@PathVariable UUID requestId,
                                                       @Valid @RequestBody ApproveSigningRequestRequest request,
                                                       Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.approve(requestId, request, currentUser),
                "Signing request approved successfully");
    }

    @PostMapping("/{requestId}/reject")
    public ApiResponse<SigningRequestResponse> reject(@PathVariable UUID requestId,
                                                      @Valid @RequestBody RejectSigningRequestRequest request,
                                                      Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.reject(requestId, request, currentUser),
                "Signing request rejected successfully");
    }

    @PostMapping("/{requestId}/cancel")
    public ApiResponse<SigningRequestResponse> cancel(@PathVariable UUID requestId,
                                                      Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingWorkflowService.cancel(requestId, currentUser),
                "Signing request cancelled successfully");
    }

    @PostMapping("/{requestId}/execute-signing")
    public ApiResponse<SigningRequestResponse> executeSigning(@PathVariable UUID requestId,
                                                              @Valid @RequestBody ExecuteSigningRequest request,
                                                              Authentication authentication) {
        CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
        return new ApiResponse<>(true,
                signingExecutionService.executeSigning(requestId, request, currentUser),
                "Cryptographic signing executed successfully");
    }

    private String buildAttachmentContentDisposition(String fileName) {
        String normalizedFileName = fileName == null || fileName.isBlank() ? "download.bin" : fileName.trim();
        String asciiFallback = normalizedFileName.replace("\\", "_").replace("\"", "_");
        String encoded = URLEncoder.encode(normalizedFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded;
    }

    private MediaType resolveDownloadMediaType(String fileName, String contentType) {
        String normalizedFileName = fileName == null ? "" : fileName.trim().toLowerCase();
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase();

        if (normalizedFileName.endsWith(".p7m")) {
        return MediaType.parseMediaType("application/pkcs7-mime; smime-type=signed-data");
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
