package com.epam.edp.demo.dto.document;

import java.util.List;

public record GenerateUploadUrlsResponseDTO(
        List<GenerateUploadUrlResponseDTO> documents
) {
}
