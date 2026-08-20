package com.ct556.signing_workflow_service.service;

public record DownloadedSignedOutput(
        String fileName,
        String contentType,
        long contentLength,
        byte[] content
) {
}
