package com.epam.edp.demo.dto.tour;

import java.util.List;

public record TourListResponseDTO(
        List<TourListItemDTO> tours,
        Integer page,
        Integer pageSize,
        Integer totalPages,
        Integer totalItems
) {
}
