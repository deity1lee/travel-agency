package com.epam.edp.demo.dto.document;

import com.epam.edp.demo.validation.documents.ValidDocuments;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@ValidDocuments
public record GenerateUploadUrlsRequestDTO(
        @NotNull(message = "Documents list is required")
        @NotEmpty(message = "Documents list is required")
        List<@Valid GenerateUploadUrlRequestDTO> documents
) {
}
