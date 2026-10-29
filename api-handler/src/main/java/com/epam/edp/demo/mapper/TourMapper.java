package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TourMapper {

    private TourMapper(){}

    public static TourDetailsDTO getTourDetailsFromTour(Tour tour) {
        List<MealSupplement> mealSupplements = tour.getMealSupplements();
        List<String> mealPlans = new ArrayList<>();
        Map<String, String> mealSupplementsPerDay = new HashMap<>();
        mealSupplements.forEach(mealSupplement -> {
            mealPlans.add(mealSupplement.getMealPlan().name());
            mealSupplementsPerDay.put(mealSupplement.getMealPlan().name(), String.format("$%s", mealSupplement.getPricePerDay()));
        });
        return new TourDetailsDTO(
                tour.getTourId(), tour.getName(), tour.getDestination(), tour.getRating(), tour.getImageUrls(),
                tour.getSummary(), tour.getFreeCancelationDaysBefore(), tour.getAccommodation(),
                tour.getHotelName(), tour.getHotelDescription(), mealPlans, tour.getCustomDetails(), tour.getGuestQuantity(),
                tour.getTourPackageOptions(), mealSupplementsPerDay);
        /*
        tourDetailsDTO.setFreeCancelationDaysBefore(tour.getFreeCancelationDaysBefore());
        // should keep this and calculate last day
        */
    }
}
