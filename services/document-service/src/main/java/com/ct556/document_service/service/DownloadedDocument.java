package com.ct556.document_service.service;

import java.io.InputStream;

public record DownloadedDocument(
        String fileName,
        String contentType,
        long contentLength,
        InputStream inputStream
) {
}