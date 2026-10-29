package com.epam.edp.demo.dto.document;

import lombok.Builder;

@Builder
public record GenerateUploadUrlResponseDTO(
        String documentId,
        String uploadUrl
) {
}
