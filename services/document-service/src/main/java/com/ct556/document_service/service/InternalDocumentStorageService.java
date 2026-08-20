package com.ct556.document_service.service;

import com.ct556.document_service.domain.entity.Document;
import com.ct556.document_service.dto.request.StoreSignedOutputRequest;
import com.ct556.document_service.dto.response.StoreSignedOutputResponse;
import com.ct556.document_service.exception.BadRequestException;
import com.ct556.document_service.exception.ResourceNotFoundException;
import com.ct556.document_service.exception.StorageException;
import com.ct556.document_service.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
public class InternalDocumentStorageService {

    private final DocumentRepository documentRepository;
    private final MinioStorageService minioStorageService;

    public InternalDocumentStorageService(DocumentRepository documentRepository,
                                          MinioStorageService minioStorageService) {
        this.documentRepository = documentRepository;
        this.minioStorageService = minioStorageService;
    }

    public StoreSignedOutputResponse storeSignedOutput(UUID documentId, StoreSignedOutputRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        byte[] content = decodeBase64(request.contentBase64());
        String cleanFileName = cleanFileName(request.fileName());
        String contentType = normalizeContentType(request.contentType());
        String objectKey = buildObjectKey(document.getOwnerAccountId(), documentId, request.signingRequestId(), cleanFileName);

        minioStorageService.uploadBytes(content, objectKey, contentType);

        return StoreSignedOutputResponse.builder()
                .documentId(documentId)
                .signingRequestId(request.signingRequestId())
                .fileName(cleanFileName)
                .contentType(contentType)
                .size((long) content.length)
                .objectKey(objectKey)
                .build();
    }

    public byte[] getSignedOutputBytes(String objectKey) {
        String normalizedObjectKey = normalizeSignedOutputObjectKey(objectKey);
        if (!minioStorageService.objectExists(normalizedObjectKey)) {
            throw new ResourceNotFoundException("Signed output not found for objectKey: " + normalizedObjectKey);
        }

        try (var inputStream = minioStorageService.getObject(normalizedObjectKey)) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new StorageException("Failed to read signed output from MinIO", ex);
        }
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("contentBase64 is not valid Base64");
        }
    }

    private String cleanFileName(String fileName) {
        String normalized = StringUtils.cleanPath(fileName);
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim() : "application/octet-stream";
    }

    private String normalizeSignedOutputObjectKey(String objectKey) {
        String normalized = StringUtils.cleanPath(objectKey);
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            throw new BadRequestException("Invalid objectKey");
        }
        if (!normalized.contains("/signed-output/")) {
            throw new BadRequestException("objectKey is not a signed-output path");
        }
        return normalized;
    }

    private String buildObjectKey(UUID ownerAccountId, UUID documentId, UUID signingRequestId, String fileName) {
        return "documents/%s/%s/signed-output/%s/%s".formatted(
                ownerAccountId,
                documentId,
                signingRequestId,
                fileName
        );
    }
}
