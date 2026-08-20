package com.ct556.account_service.service;

public record DownloadedSignatureImage(
        String contentType,
        byte[] content
) {
}
