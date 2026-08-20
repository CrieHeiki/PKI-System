package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.ConsumeStepUpVerificationRequest;
import com.ct556.step_up_auth_service.dto.response.InternalStepUpVerificationResponse;
import com.ct556.step_up_auth_service.service.StepUpChallengeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/step-up/verifications")
public class InternalStepUpController {

    private final StepUpChallengeService stepUpChallengeService;

    public InternalStepUpController(StepUpChallengeService stepUpChallengeService) {
        this.stepUpChallengeService = stepUpChallengeService;
    }

    @PostMapping("/consume")
    public ApiResponse<InternalStepUpVerificationResponse> consumeVerification(@Valid @RequestBody ConsumeStepUpVerificationRequest request) {
        InternalStepUpVerificationResponse response = stepUpChallengeService.consumeVerification(request);
        return new ApiResponse<>(true, response, "Step-up verification evaluated");
    }
}
