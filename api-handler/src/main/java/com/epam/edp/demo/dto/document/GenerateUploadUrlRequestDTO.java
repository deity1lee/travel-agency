package com.epam.edp.demo.dto.document;

import com.epam.edp.demo.entity.document.DocumentType;
import com.epam.edp.demo.entity.document.FileFormat;
import com.epam.edp.demo.validation.documents.ValidDocumentHolder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ValidDocumentHolder
public record GenerateUploadUrlRequestDTO(
        @Schema(description = "File format", example = "pdf")
        @NotNull(message = "File format is required")
        FileFormat fileFormat,

        @Schema(description = "First name", example = "John")
        String guestFirstName,

        @Schema(description = "Last name", example = "Doe")
        String guestLastName,

        @Schema(description = "Document type", example = "PASSPORT")
        @NotNull(message = "Document type name is required")
        DocumentType documentType,

        @Schema(description = "File size", example = "100")
        @Min(value = 10, message = "File can be minimum 10KB")
        @Max(value = 10000, message = "File can be maximum 10.000KB")
        @NotNull(message = "File size is required")
        Integer fileSize
) {
}
