package com.ct556.verification_service.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationCheckResponse {
    private String checkName;
    private boolean result;
    private String detailMessage;
    private JsonNode rawData;
}
