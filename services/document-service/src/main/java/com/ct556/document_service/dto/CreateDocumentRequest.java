package com.ct556.document_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateDocumentRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 255, message = "title must be <= 255 characters")
    private String title;

    private String description;

    @NotNull(message = "file is required")
    private MultipartFile file;
}