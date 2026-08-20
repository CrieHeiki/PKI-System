package com.ct556.document_service.controller;

import com.ct556.document_service.dto.request.StoreSignedOutputRequest;
import com.ct556.document_service.dto.response.InternalDocumentContentResponse;
import com.ct556.document_service.dto.response.InternalDocumentMetadataResponse;
import com.ct556.document_service.dto.response.StoreSignedOutputResponse;
import com.ct556.document_service.service.InternalDocumentQueryService;
import com.ct556.document_service.service.InternalDocumentStorageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/documents")
public class InternalDocumentController {

    private final InternalDocumentQueryService internalDocumentQueryService;
    private final InternalDocumentStorageService internalDocumentStorageService;

    public InternalDocumentController(InternalDocumentQueryService internalDocumentQueryService,
                                      InternalDocumentStorageService internalDocumentStorageService) {
        this.internalDocumentQueryService = internalDocumentQueryService;
        this.internalDocumentStorageService = internalDocumentStorageService;
    }

    @GetMapping("/{documentId}/metadata")
    public InternalDocumentMetadataResponse getMetadata(@PathVariable UUID documentId) {
        return internalDocumentQueryService.getDocumentMetadata(documentId);
    }

    @GetMapping("/{documentId}/content-for-signing")
    public InternalDocumentContentResponse getContentForSigning(@PathVariable UUID documentId) {
        return internalDocumentQueryService.getDocumentContentForSigning(documentId);
    }

    @GetMapping("/signed-output")
    public byte[] getSignedOutput(@RequestParam String objectKey) {
        return internalDocumentStorageService.getSignedOutputBytes(objectKey);
    }

    @PostMapping("/{documentId}/signed-output")
    public StoreSignedOutputResponse storeSignedOutput(@PathVariable UUID documentId,
                                                       @Valid @RequestBody StoreSignedOutputRequest request) {
        return internalDocumentStorageService.storeSignedOutput(documentId, request);
    }
}
