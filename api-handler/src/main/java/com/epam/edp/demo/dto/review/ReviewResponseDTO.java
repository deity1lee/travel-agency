package com.epam.edp.demo.dto.review;

public record ReviewResponseDTO(
        String authorName,
        String authorImageUrl,
        String createdAt,
        Integer rate,
        String reviewContent
) {
}
