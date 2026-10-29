package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.entity.tourModel.GuestQuantity;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourMapperTest {

    @Test
    void getTourDetailsFromTour_passesScalarFieldsThrough() {
        Tour tour = baseTour();

        TourDetailsDTO dto = TourMapper.getTourDetailsFromTour(tour);

        assertThat(dto.id()).isEqualTo("tour-001");
        assertThat(dto.name()).isEqualTo("Paris weekender");
        assertThat(dto.destination()).isEqualTo("Paris");
        assertThat(dto.rating()).isEqualTo(4.7);
        assertThat(dto.summary()).isEqualTo("Lovely tour");
        assertThat(dto.freeCancelationDaysBefore()).isEqualTo(14);
        assertThat(dto.accommodation()).isEqualTo("Hotel double room");
    }

    @Test
    void getTourDetailsFromTour_collectsMealPlanNamesInOrder() {
        Tour tour = baseTour();
        tour.setMealSupplements(List.of(
                supplement(MealPlan.BB, 0),
                supplement(MealPlan.HB, 25),
                supplement(MealPlan.AI, 60)));

        TourDetailsDTO dto = TourMapper.getTourDetailsFromTour(tour);

        assertThat(dto.mealPlans()).containsExactly("BB", "HB", "AI");
    }

    @Test
    void getTourDetailsFromTour_buildsSupplementsPerDayMapWithDollarPrefix() {
        Tour tour = baseTour();
        tour.setMealSupplements(List.of(
                supplement(MealPlan.BB, 0),
                supplement(MealPlan.HB, 25)));

        TourDetailsDTO dto = TourMapper.getTourDetailsFromTour(tour);

        assertThat(dto.mealSupplementsPerDay())
                .containsEntry("BB", "$0")
                .containsEntry("HB", "$25");
    }

    @Test
    void getTourDetailsFromTour_passesGuestQuantityAndPackageOptions() {
        Tour tour = baseTour();
        GuestQuantity gq = new GuestQuantity();
        gq.setAdultsMaxValue(2);
        gq.setChildrenMaxValue(4);
        gq.setTotalMaxValue(6);
        tour.setGuestQuantity(gq);

        TourPackageOption option = new TourPackageOption();
        option.setStartDate(LocalDate.parse("2026-07-10"));
        option.setDuration("7 days");
        option.setPrice(1299);
        option.setAvailableAdultSpots(2);
        option.setAvailableChildSpots(1);
        tour.setTourPackageOptions(List.of(option));

        TourDetailsDTO dto = TourMapper.getTourDetailsFromTour(tour);

        assertThat(dto.guestQuantity()).isSameAs(gq);
        assertThat(dto.tourPackageOption()).containsExactly(option);
    }

    @Test
    void getTourDetailsFromTour_throwsOnNullMealSupplements() {
        Tour tour = baseTour();
        tour.setMealSupplements(null);

        assertThatThrownBy(() -> TourMapper.getTourDetailsFromTour(tour))
                .isInstanceOf(NullPointerException.class);
    }

    private static Tour baseTour() {
        return Tour.builder()
                .tourId("tour-001")
                .name("Paris weekender")
                .destination("Paris")
                .rating(4.7)
                .imageUrls(List.of("https://img/one.jpg"))
                .summary("Lovely tour")
                .freeCancelationDaysBefore(14)
                .accommodation("Hotel double room")
                .customDetails(Map.of("wifi", "free"))
                .mealSupplements(List.of(supplement(MealPlan.BB, 0)))
                .build();
    }

    private static MealSupplement supplement(MealPlan plan, int pricePerDay) {
        MealSupplement s = new MealSupplement();
        s.setMealPlan(plan);
        s.setPricePerDay(pricePerDay);
        return s;
    }
}
