package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.CreateWebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.dto.response.WebAuthnRecoveryRequestResponse;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.ct556.step_up_auth_service.service.WebAuthnRecoveryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/step-up/recovery-requests")
public class WebAuthnRecoveryController {

    private final WebAuthnRecoveryService webAuthnRecoveryService;

    public WebAuthnRecoveryController(WebAuthnRecoveryService webAuthnRecoveryService) {
        this.webAuthnRecoveryService = webAuthnRecoveryService;
    }

    @GetMapping("/me")
    public ApiResponse<List<WebAuthnRecoveryRequestResponse>> getMyRecoveryRequests(Authentication authentication) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.getMyRecoveryRequests(currentUser(authentication)),
                "Fetched WebAuthn recovery requests"
        );
    }

    @PostMapping
    public ApiResponse<WebAuthnRecoveryRequestResponse> createRecoveryRequest(Authentication authentication,
                                                                              @Valid @RequestBody CreateWebAuthnRecoveryRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.createRecoveryRequest(currentUser(authentication), request),
                "WebAuthn recovery request created"
        );
    }

    @PostMapping("/{requestId}/cancel")
    public ApiResponse<WebAuthnRecoveryRequestResponse> cancelRecoveryRequest(Authentication authentication,
                                                                              @PathVariable UUID requestId) {
        return new ApiResponse<>(
                true,
                webAuthnRecoveryService.cancelRecoveryRequest(currentUser(authentication), requestId),
                "WebAuthn recovery request cancelled"
        );
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
