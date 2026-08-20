package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.CreateMfaRegistrationRequest;
import com.ct556.step_up_auth_service.dto.request.ManageMfaRegistrationRequest;
import com.ct556.step_up_auth_service.dto.response.MfaRegistrationResponse;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.ct556.step_up_auth_service.service.MfaRegistrationService;
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
@RequestMapping("/api/v1/step-up/mfa-registrations")
public class MfaRegistrationController {

    private final MfaRegistrationService mfaRegistrationService;

    public MfaRegistrationController(MfaRegistrationService mfaRegistrationService) {
        this.mfaRegistrationService = mfaRegistrationService;
    }

    @PostMapping
    public ApiResponse<MfaRegistrationResponse> createRegistration(Authentication authentication,
                                                                   @Valid @RequestBody CreateMfaRegistrationRequest request) {
        MfaRegistrationResponse response = mfaRegistrationService.createRegistration(currentUser(authentication), request);
        return new ApiResponse<>(true, response, "MFA registration created");
    }

    @GetMapping("/me")
    public ApiResponse<List<MfaRegistrationResponse>> getMyRegistrations(Authentication authentication) {
        List<MfaRegistrationResponse> response = mfaRegistrationService.getMyRegistrations(currentUser(authentication));
        return new ApiResponse<>(true, response, "Fetched MFA registrations");
    }

    @PostMapping("/{registrationId}/disable")
    public ApiResponse<MfaRegistrationResponse> disableRegistration(Authentication authentication,
                                                                    @PathVariable UUID registrationId,
                                                                    @Valid @RequestBody ManageMfaRegistrationRequest request) {
        MfaRegistrationResponse response = mfaRegistrationService.disableRegistration(currentUser(authentication), registrationId, request);
        return new ApiResponse<>(true, response, "MFA registration disabled");
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
