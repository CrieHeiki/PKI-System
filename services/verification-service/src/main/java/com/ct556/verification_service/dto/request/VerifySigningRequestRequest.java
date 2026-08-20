package com.ct556.verification_service.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VerifySigningRequestRequest {

    private UUID verifiedBy;

    @Pattern(regexp = "^(INTERNAL_CA|EXTERNAL_CA)$", message = "trustSource must be INTERNAL_CA or EXTERNAL_CA")
    private String trustSource = "INTERNAL_CA";
}
