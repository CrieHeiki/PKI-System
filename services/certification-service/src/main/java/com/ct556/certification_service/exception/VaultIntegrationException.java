package com.ct556.certification_service.exception;

public class VaultIntegrationException extends RuntimeException {

    public VaultIntegrationException(String message) {
        super(message);
    }

    public VaultIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}