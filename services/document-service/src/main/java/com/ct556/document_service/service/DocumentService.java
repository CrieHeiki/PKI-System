package com.ct556.document_service.service;

import com.ct556.document_service.client.AuditGateway;
import com.ct556.document_service.domain.entity.Document;
import com.ct556.document_service.domain.entity.DocumentVersion;
import com.ct556.document_service.domain.enumtype.DocumentStatus;
import com.ct556.document_service.dto.*;
import com.ct556.document_service.exception.BadRequestException;
import com.ct556.document_service.exception.ForbiddenException;
import com.ct556.document_service.exception.ResourceNotFoundException;
import com.ct556.document_service.repository.DocumentRepository;
import com.ct556.document_service.repository.DocumentVersionRepository;
import com.ct556.document_service.security.CurrentUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MinioStorageService minioStorageService;
    private final Sha256Service sha256Service;
    private final AuditGateway auditGateway;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository documentVersionRepository,
                           MinioStorageService minioStorageService,
                           Sha256Service sha256Service,
                           AuditGateway auditGateway) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.minioStorageService = minioStorageService;
        this.sha256Service = sha256Service;
        this.auditGateway = auditGateway;
    }

    @Transactional
    public DocumentDetailResponse createDocument(CreateDocumentRequest request, CurrentUser currentUser) {
        MultipartFile file = request.getFile();
        validateMultipartFile(file);

        Path tempFile = copyToTempFile(file);
        try {
            UUID documentId = UUID.randomUUID();
            int versionNumber = 1;

            String cleanFileName = cleanFileName(file.getOriginalFilename());
            String contentType = normalizeContentType(file.getContentType());
            long fileSize = safeFileSize(file);
            String sha256Hash = sha256Service.calculate(tempFile);
            String objectKey = buildObjectKey(currentUser.accountId(), documentId, versionNumber, cleanFileName);

            minioStorageService.upload(tempFile, objectKey, contentType);

            Document document = Document.builder()
                    .id(documentId)
                    .ownerAccountId(currentUser.accountId())
                    .title(request.getTitle().trim())
                    .description(normalizeNullableText(request.getDescription()))
                    .status(DocumentStatus.ACTIVE)
                    .build();

            documentRepository.save(document);

            DocumentVersion documentVersion = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .document(document)
                    .versionNumber(versionNumber)
                    .objectKey(objectKey)
                    .fileName(cleanFileName)
                    .contentType(contentType)
                    .fileSize(fileSize)
                    .sha256Hash(sha256Hash)
                    .build();

            documentVersionRepository.save(documentVersion);
            publishAudit(
                    "DOCUMENT_CREATED",
                    currentUser,
                    document.getId(),
                    "Document uploaded successfully",
                    payload(
                            "documentId", document.getId(),
                            "ownerAccountId", document.getOwnerAccountId(),
                            "currentVersion", documentVersion.getVersionNumber(),
                            "fileName", documentVersion.getFileName(),
                            "contentType", documentVersion.getContentType(),
                            "fileSize", documentVersion.getFileSize(),
                            "sha256Hash", documentVersion.getSha256Hash(),
                            "objectKey", documentVersion.getObjectKey()
                    )
            );

            return toDetailResponse(document, documentVersion);
        } finally {
            deleteTempFileQuietly(tempFile);
        }
    }

    public DocumentDetailResponse getDocumentDetail(UUID documentId, CurrentUser currentUser) {
        Document document = findDocumentById(documentId);
        ensureCanAccess(document, currentUser);

        DocumentVersion currentVersion = findCurrentVersion(document);
        return toDetailResponse(document, currentVersion);
    }

    public DownloadedDocument downloadCurrentVersion(UUID documentId, CurrentUser currentUser) {
        Document document = findDocumentById(documentId);
        ensureCanAccess(document, currentUser);

        DocumentVersion currentVersion = findCurrentVersion(document);
        publishAudit(
                "DOCUMENT_DOWNLOADED",
                currentUser,
                document.getId(),
                "Current document version downloaded",
                payload(
                        "documentId", document.getId(),
                        "ownerAccountId", document.getOwnerAccountId(),
                        "currentVersion", currentVersion.getVersionNumber(),
                        "fileName", currentVersion.getFileName(),
                        "contentType", currentVersion.getContentType(),
                        "fileSize", currentVersion.getFileSize()
                )
        );

        return new DownloadedDocument(
                currentVersion.getFileName(),
                normalizeContentType(currentVersion.getContentType()),
                currentVersion.getFileSize(),
                minioStorageService.getObject(currentVersion.getObjectKey())
        );
    }

    @Transactional
    public DocumentDetailResponse uploadNewVersion(UUID documentId,
                                                   UploadDocumentVersionRequest request,
                                                   CurrentUser currentUser) {
        validateMultipartFile(request.getFile());

        Document document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        ensureCanAccess(document, currentUser);

        DocumentVersion latestVersion = findCurrentVersion(document);
        int nextVersionNumber = latestVersion.getVersionNumber() + 1;
        MultipartFile file = request.getFile();
        Path tempFile = copyToTempFile(file);

        try {
            String cleanFileName = cleanFileName(file.getOriginalFilename());
            String contentType = normalizeContentType(file.getContentType());
            long fileSize = safeFileSize(file);
            String sha256Hash = sha256Service.calculate(tempFile);
            String objectKey = buildObjectKey(document.getOwnerAccountId(), document.getId(), nextVersionNumber, cleanFileName);

            minioStorageService.upload(tempFile, objectKey, contentType);

            DocumentVersion documentVersion = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .document(document)
                    .versionNumber(nextVersionNumber)
                    .objectKey(objectKey)
                    .fileName(cleanFileName)
                    .contentType(contentType)
                    .fileSize(fileSize)
                    .sha256Hash(sha256Hash)
                    .build();

            documentVersionRepository.save(documentVersion);

            documentRepository.save(document);
            publishAudit(
                    "DOCUMENT_VERSION_UPLOADED",
                    currentUser,
                    document.getId(),
                    "New document version uploaded successfully",
                    payload(
                            "documentId", document.getId(),
                            "ownerAccountId", document.getOwnerAccountId(),
                            "currentVersion", documentVersion.getVersionNumber(),
                            "uploadedVersion", documentVersion.getVersionNumber(),
                            "fileName", documentVersion.getFileName(),
                            "contentType", documentVersion.getContentType(),
                            "fileSize", documentVersion.getFileSize(),
                            "sha256Hash", documentVersion.getSha256Hash(),
                            "objectKey", documentVersion.getObjectKey()
                    )
            );

            return toDetailResponse(document, documentVersion);
        } finally {
            deleteTempFileQuietly(tempFile);
        }
    }

    public List<DocumentSummaryResponse> getMyDocuments(CurrentUser currentUser) {
        return documentRepository.findByOwnerAccountIdOrderByCreatedAtDesc(currentUser.accountId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<DocumentSummaryResponse> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
    }

    private DocumentVersion findCurrentVersion(Document document) {
        return documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current version metadata not found for document id: " + document.getId()
                ));
    }

    private void ensureCanAccess(Document document, CurrentUser currentUser) {
        boolean isOwner = document.getOwnerAccountId().equals(currentUser.accountId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not allowed to access this document");
        }
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null) {
            throw new BadRequestException("file is required");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
    }

    private Path copyToTempFile(MultipartFile multipartFile) {
        try {
            String cleanName = cleanFileName(multipartFile.getOriginalFilename());
            String suffix = getFileSuffix(cleanName);
            Path tempFile = Files.createTempFile("ct556-upload-", suffix);

            try (var inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return tempFile;
        } catch (IOException ex) {
            throw new BadRequestException("Failed to process uploaded file");
        }
    }

    private void deleteTempFileQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String cleanFileName(String originalFileName) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNullElse(originalFileName, "file.bin"));

        if (fileName.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }

        if (!StringUtils.hasText(fileName)) {
            throw new BadRequestException("Invalid file name");
        }

        return fileName;
    }

    private String getFileSuffix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return ".tmp";
        }
        return fileName.substring(lastDotIndex);
    }

    private long safeFileSize(MultipartFile file) {
        long size = file.getSize();
        if (size < 0) {
            throw new BadRequestException("Invalid uploaded file size");
        }
        return size;
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildObjectKey(UUID ownerAccountId, UUID documentId, int versionNumber, String fileName) {
        String extension = getFileSuffix(fileName);
        String randomFileToken = UUID.randomUUID().toString();
        return "documents/%s/%s/v%d/%s%s".formatted(
                ownerAccountId,
                documentId,
                versionNumber,
                randomFileToken,
                extension
        );
    }

    private DocumentDetailResponse toDetailResponse(Document document, DocumentVersion currentVersion) {
        return new DocumentDetailResponse(
                document.getId(),
                document.getOwnerAccountId(),
                document.getTitle(),
                document.getDescription(),
                currentVersion.getVersionNumber(),
                document.getStatus().name(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                new DocumentVersionResponse(
                        currentVersion.getId(),
                        currentVersion.getVersionNumber(),
                        currentVersion.getFileName(),
                        currentVersion.getContentType(),
                        currentVersion.getFileSize(),
                        currentVersion.getSha256Hash(),
                        currentVersion.getCreatedAt()
                )
        );
    }

    private DocumentSummaryResponse toSummaryResponse(Document document) {
        DocumentVersion currentVersion = findCurrentVersion(document);
        return new DocumentSummaryResponse(
                document.getId(),
                document.getOwnerAccountId(),
                document.getTitle(),
                document.getDescription(),
                currentVersion.getVersionNumber(),
                document.getStatus().name(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private void publishAudit(String eventType,
                              CurrentUser currentUser,
                              UUID documentId,
                              String summary,
                              LinkedHashMap<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                documentId,
                "DOCUMENT",
                documentId,
                documentId.toString(),
                summary,
                payload
        );
    }

    private LinkedHashMap<String, Object> payload(Object... keyValues) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                payload.put(String.valueOf(key), value);
            }
        }
        return payload;
    }
}
