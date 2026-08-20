package com.ct556.document_service.controller;

import com.ct556.document_service.dto.*;
import com.ct556.document_service.security.CurrentUser;
import com.ct556.document_service.security.SecurityUtils;
import com.ct556.document_service.service.DocumentService;
import com.ct556.document_service.service.DownloadedDocument;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final SecurityUtils securityUtils;

    public DocumentController(DocumentService documentService, SecurityUtils securityUtils) {
        this.documentService = documentService;
        this.securityUtils = securityUtils;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailResponse> uploadNewDocument(@Valid @ModelAttribute CreateDocumentRequest request) {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        DocumentDetailResponse response = documentService.createDocument(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(@PathVariable UUID documentId) {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        DocumentDetailResponse response = documentService.getDocumentDetail(documentId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadCurrentVersion(@PathVariable UUID documentId) {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        DownloadedDocument downloadedDocument = documentService.downloadCurrentVersion(documentId, currentUser);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(downloadedDocument.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(downloadedDocument.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(downloadedDocument.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(downloadedDocument.inputStream()));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailResponse> uploadNewVersion(@PathVariable UUID documentId,
                                                                   @Valid @ModelAttribute UploadDocumentVersionRequest request) {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        DocumentDetailResponse response = documentService.uploadNewVersion(documentId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DocumentSummaryResponse>> getMyDocuments() {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        List<DocumentSummaryResponse> response = documentService.getMyDocuments(currentUser);
        return ResponseEntity.ok(response);
    }
}