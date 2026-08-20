package com.ct556.step_up_auth_service.dto;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
}
