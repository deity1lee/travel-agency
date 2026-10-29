package com.epam.edp.demo.dto.tour;

import com.epam.edp.demo.entity.tourModel.GuestQuantity;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record TourDetailsDTO(
        String id,
        String name,
        String destination,
        Double rating,
        List<String> imageUrls,
        String summary,
        Integer freeCancelationDaysBefore,
        String accommodation,
        String hotelName,
        String hotelDescription,
        List<String> mealPlans,
        Map<String, String> customDetails,
        GuestQuantity guestQuantity,
        List<TourPackageOption> tourPackageOption,
        Map<String, String> mealSupplementsPerDay
) {
}
