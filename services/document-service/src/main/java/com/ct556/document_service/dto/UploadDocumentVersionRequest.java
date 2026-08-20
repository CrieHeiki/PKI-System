package com.ct556.document_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UploadDocumentVersionRequest {

    @NotNull(message = "file is required")
    private MultipartFile file;
}