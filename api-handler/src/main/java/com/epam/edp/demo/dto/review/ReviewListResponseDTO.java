package com.epam.edp.demo.dto.review;

import java.util.List;

public record ReviewListResponseDTO(
        List<ReviewResponseDTO> reviews,
        Integer page,
        Integer pageSize,
        Integer totalPages,
        Integer totalItems
) {
}
