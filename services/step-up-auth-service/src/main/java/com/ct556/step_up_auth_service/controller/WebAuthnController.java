package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.BeginWebAuthnAssertionRequest;
import com.ct556.step_up_auth_service.dto.request.BeginWebAuthnRegistrationRequest;
import com.ct556.step_up_auth_service.dto.request.FinishWebAuthnAssertionRequest;
import com.ct556.step_up_auth_service.dto.request.FinishWebAuthnRegistrationRequest;
import com.ct556.step_up_auth_service.dto.response.MfaRegistrationResponse;
import com.ct556.step_up_auth_service.dto.response.StepUpVerificationResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnAssertionOptionsResponse;
import com.ct556.step_up_auth_service.dto.response.WebAuthnRegistrationOptionsResponse;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.ct556.step_up_auth_service.service.WebAuthnStepUpService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/step-up/webauthn")
public class WebAuthnController {

    private final WebAuthnStepUpService webAuthnStepUpService;

    public WebAuthnController(WebAuthnStepUpService webAuthnStepUpService) {
        this.webAuthnStepUpService = webAuthnStepUpService;
    }

    @PostMapping("/registrations/options")
    public ApiResponse<WebAuthnRegistrationOptionsResponse> beginRegistration(Authentication authentication,
                                                                              @Valid @RequestBody BeginWebAuthnRegistrationRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnStepUpService.beginRegistration(currentUser(authentication), request),
                "WebAuthn registration ceremony started"
        );
    }

    @PostMapping("/registrations/finish")
    public ApiResponse<MfaRegistrationResponse> finishRegistration(Authentication authentication,
                                                                   @Valid @RequestBody FinishWebAuthnRegistrationRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnStepUpService.finishRegistration(currentUser(authentication), request),
                "WebAuthn registration completed"
        );
    }

    @PostMapping("/assertions/options")
    public ApiResponse<WebAuthnAssertionOptionsResponse> beginAssertion(Authentication authentication,
                                                                        @Valid @RequestBody BeginWebAuthnAssertionRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnStepUpService.beginAssertion(currentUser(authentication), request),
                "WebAuthn assertion ceremony started"
        );
    }

    @PostMapping("/assertions/finish")
    public ApiResponse<StepUpVerificationResponse> finishAssertion(Authentication authentication,
                                                                   @Valid @RequestBody FinishWebAuthnAssertionRequest request) {
        return new ApiResponse<>(
                true,
                webAuthnStepUpService.finishAssertion(currentUser(authentication), request),
                "WebAuthn assertion verified"
        );
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
