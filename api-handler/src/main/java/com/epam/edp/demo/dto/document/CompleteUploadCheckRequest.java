package com.epam.edp.demo.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompleteUploadCheckRequest(
        @NotNull(message = "Documents list is required")
        @Size(min = 1, max = 10, message = "Too many documents")
        List<String> documentIds
) {
}
