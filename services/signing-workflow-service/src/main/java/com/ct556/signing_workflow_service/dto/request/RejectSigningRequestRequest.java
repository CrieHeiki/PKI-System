package com.ct556.signing_workflow_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectSigningRequestRequest {

    @NotBlank
    private String note;
}