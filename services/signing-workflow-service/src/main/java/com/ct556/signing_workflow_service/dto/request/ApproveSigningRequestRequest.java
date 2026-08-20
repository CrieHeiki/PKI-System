package com.ct556.signing_workflow_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ApproveSigningRequestRequest {

    @NotNull
    private UUID certificateId;

    private String note;
}