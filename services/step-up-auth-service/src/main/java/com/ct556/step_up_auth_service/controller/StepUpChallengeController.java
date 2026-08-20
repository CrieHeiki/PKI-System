package com.ct556.step_up_auth_service.controller;

import com.ct556.step_up_auth_service.dto.ApiResponse;
import com.ct556.step_up_auth_service.dto.request.CreateStepUpChallengeRequest;
import com.ct556.step_up_auth_service.dto.request.VerifyStepUpChallengeRequest;
import com.ct556.step_up_auth_service.dto.response.StepUpChallengeResponse;
import com.ct556.step_up_auth_service.dto.response.StepUpVerificationResponse;
import com.ct556.step_up_auth_service.security.CurrentUser;
import com.ct556.step_up_auth_service.service.StepUpChallengeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/step-up/challenges")
public class StepUpChallengeController {

    private final StepUpChallengeService stepUpChallengeService;

    public StepUpChallengeController(StepUpChallengeService stepUpChallengeService) {
        this.stepUpChallengeService = stepUpChallengeService;
    }

    @PostMapping
    public ApiResponse<StepUpChallengeResponse> createChallenge(Authentication authentication,
                                                                @Valid @RequestBody CreateStepUpChallengeRequest request) {
        StepUpChallengeResponse response = stepUpChallengeService.createChallenge(currentUser(authentication), request);
        return new ApiResponse<>(true, response, "Step-up challenge created");
    }

    @PostMapping("/{challengeId}/verify")
    public ApiResponse<StepUpVerificationResponse> verifyChallenge(Authentication authentication,
                                                                   @PathVariable UUID challengeId,
                                                                   @Valid @RequestBody VerifyStepUpChallengeRequest request) {
        StepUpVerificationResponse response = stepUpChallengeService.verifyChallenge(currentUser(authentication), challengeId, request);
        return new ApiResponse<>(true, response, "Step-up challenge verified");
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}
