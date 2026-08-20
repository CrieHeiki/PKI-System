package com.ct556.document_service.controller;

import com.ct556.document_service.dto.DocumentSummaryResponse;
import com.ct556.document_service.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/documents")
public class AdminDocumentController {

    private final DocumentService documentService;

    public AdminDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }
}