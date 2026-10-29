package com.epam.edp.demo.dto.bookings;

import com.epam.edp.demo.entity.tourModel.TourPackageOption;

public record NewOldPackageOptions(
        TourPackageOption newTourPackageOption,
        TourPackageOption assumedOldTourPackageOption
) {
}
