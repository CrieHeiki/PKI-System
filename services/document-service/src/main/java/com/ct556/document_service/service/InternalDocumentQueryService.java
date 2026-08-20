package com.ct556.document_service.service;

import com.ct556.document_service.domain.entity.Document;
import com.ct556.document_service.domain.entity.DocumentVersion;
import com.ct556.document_service.dto.response.InternalDocumentContentResponse;
import com.ct556.document_service.dto.response.InternalDocumentMetadataResponse;
import com.ct556.document_service.exception.ResourceNotFoundException;
import com.ct556.document_service.repository.DocumentRepository;
import com.ct556.document_service.repository.DocumentVersionRepository;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class InternalDocumentQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MinioStorageService minioStorageService;

    public InternalDocumentQueryService(DocumentRepository documentRepository,
                                        DocumentVersionRepository documentVersionRepository,
                                        MinioStorageService minioStorageService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.minioStorageService = minioStorageService;
    }

    public InternalDocumentMetadataResponse getDocumentMetadata(UUID documentId) {
        Document document = findDocument(documentId);
        DocumentVersion currentVersion = findCurrentVersion(document);

        return InternalDocumentMetadataResponse.builder()
                .documentId(document.getId())
                .ownerAccountId(document.getOwnerAccountId())
                .currentVersion(currentVersion.getVersionNumber())
                .status(document.getStatus().name())
                .build();
    }

    public InternalDocumentContentResponse getDocumentContentForSigning(UUID documentId) {
        Document document = findDocument(documentId);
        DocumentVersion currentVersion = findCurrentVersion(document);

        byte[] content = loadBytesFromMinio(currentVersion.getObjectKey());

        return InternalDocumentContentResponse.builder()
                .documentId(document.getId())
                .fileName(currentVersion.getFileName())
                .contentType(currentVersion.getContentType())
                .contentBase64(Base64.getEncoder().encodeToString(content))
                .size((long) content.length)
                .objectKey(currentVersion.getObjectKey())
                .build();
    }

    private Document findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private DocumentVersion findCurrentVersion(Document document) {
        return documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current document version not found for document: " + document.getId()
                ));
    }

    private byte[] loadBytesFromMinio(String objectKey) {
        try (InputStream inputStream = minioStorageService.getObject(objectKey)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load file from MinIO with objectKey=" + objectKey, e);
        }
    }
}
