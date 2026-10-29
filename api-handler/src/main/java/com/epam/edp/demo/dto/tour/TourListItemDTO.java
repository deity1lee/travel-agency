package com.epam.edp.demo.dto.tour;

import java.time.LocalDate;
import java.util.List;

public record TourListItemDTO(
        String id,
        String name,
        String destination,
        LocalDate startDate,
        List<String> durations,
        List<String> mealPlans,
        String price,
        Double rating,
        Integer reviews,
        LocalDate freeCancelation,
        String imageUrl
) {
}
