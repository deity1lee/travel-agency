package com.epam.edp.demo.dto.tour;

import com.epam.edp.demo.entity.tourModel.DurationBucket;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.TourSortBy;
import com.epam.edp.demo.entity.tourModel.TourType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record TourRequestDTO(
        String destination,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        List<DurationBucket> durations,
        Integer adults,
        Integer children,
        List<MealPlan> mealPlans,
        List<TourType> tourTypes,
        TourSortBy sortBy,
        Integer page,
        Integer pageSize
) {
}
