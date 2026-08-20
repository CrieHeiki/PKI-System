package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.CreateWebAuthnEnrollmentTicketRequest;
import com.ct556.step_up_auth_service.dto.request.ReviewWebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.dto.response.WebAuthnEnrollmentTicketResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnRecoveryRequestResponse;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.ct556.step_up_auth_service.service.WebAuthnRecoveryService;
import com.ct556.step_up_auth_service.service.WebAuthnStepUpService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/step-up/webauthn")
public class AdminStepUpController {

    private final WebAuthnStepUpService webAuthnStepUpService;
    private final WebAuthnRecoveryService webAuthnRecoveryService;

    public AdminStepUpController(WebAuthnStepUpService webAuthnStepUpService,
                                 WebAuthnRecoveryService webAuthnRecoveryService) {
        this.webAuthnStepUpService = webAuthnStepUpService;
        this.webAuthnRecoveryService = webAuthnRecoveryService;
    }

    @PostMapping("/enrollment-tickets")
    public ApiResponse<WebAuthnEnrollmentTicketResponse> createEnrollmentTicket(Authentication authentication,
                                                                                @Valid @RequestBody CreateWebAuthnEnrollmentTicketRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnStepUpService.issueFirstEnrollmentTicket(currentUser(authentication), request),
                "First-authenticator enrollment ticket issued"
        );
    }

    @GetMapping("/recovery-requests")
    public ApiResponse<List<WebAuthnRecoveryRequestResponse>> getRecoveryRequests(Authentication authentication,
                                                                                  @RequestParam UUID userId,
                                                                                  @RequestParam(required = false) String status) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.getRecoveryRequestsForAdmin(currentUser(authentication), userId, status),
                "Fetched WebAuthn recovery requests"
        );
    }

    @PostMapping("/recovery-requests/{requestId}/approve")
    public ApiResponse<WebAuthnRecoveryRequestResponse> approveRecoveryRequest(Authentication authentication,
                                                                               @PathVariable UUID requestId,
                                                                               @Valid @RequestBody ReviewWebAuthnRecoveryRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.approveRecoveryRequest(currentUser(authentication), requestId, request),
                "WebAuthn recovery request approved"
        );
    }

    @PostMapping("/recovery-requests/{requestId}/reject")
    public ApiResponse<WebAuthnRecoveryRequestResponse> rejectRecoveryRequest(Authentication authentication,
                                                                              @PathVariable UUID requestId,
                                                                              @Valid @RequestBody ReviewWebAuthnRecoveryRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.rejectRecoveryRequest(currentUser(authentication), requestId, request),
                "WebAuthn recovery request rejected"
        );
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
