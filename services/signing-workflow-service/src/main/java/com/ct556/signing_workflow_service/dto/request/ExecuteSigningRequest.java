package com.ct556.signing_workflow_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class ExecuteSigningRequest {

    @NotBlank(message = "stepUpVerificationToken is required")
    private String stepUpVerificationToken;

    @Valid
    private SignatureAppearanceOverrideRequest signatureAppearance;

    public String getStepUpVerificationToken() {
        return stepUpVerificationToken;
    }

    public void setStepUpVerificationToken(String stepUpVerificationToken) {
        this.stepUpVerificationToken = stepUpVerificationToken;
    }

    public SignatureAppearanceOverrideRequest getSignatureAppearance() {
        return signatureAppearance;
    }

    public void setSignatureAppearance(SignatureAppearanceOverrideRequest signatureAppearance) {
        this.signatureAppearance = signatureAppearance;
    }
}
