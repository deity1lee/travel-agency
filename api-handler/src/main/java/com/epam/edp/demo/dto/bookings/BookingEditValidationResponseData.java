package com.epam.edp.demo.dto.bookings;

import com.epam.edp.demo.entity.tourModel.MealPlan;

public record BookingEditValidationResponseData(
        NewOldPackageOptions packageOptions,
        MealPlan mealPlan
) {
}
