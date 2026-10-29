package com.epam.edp.demo.validation.tour;

import com.epam.edp.demo.dto.tour.TourRequestDTO;
import com.epam.edp.demo.entity.tourModel.DurationBucket;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TourSearchValidatorTest {

    @Test
    void validateAndBuild_multiValueDurations_allParsed() {
        MultiValueMap<String, String> multi = new LinkedMultiValueMap<>();
        multi.add("duration", "1-3 days");
        multi.add("duration", "4-7 days");

        TourRequestDTO dto = TourSearchValidator.validateAndBuild(Map.of(), multi);

        assertEquals(List.of(DurationBucket.DAYS_1_3, DurationBucket.DAYS_4_7), dto.durations());
    }

    @Test
    void validateAndBuild_multiValueMealPlans_allParsed() {
        MultiValueMap<String, String> multi = new LinkedMultiValueMap<>();
        multi.add("mealPlan", "BB");
        multi.add("mealPlan", "HB");

        TourRequestDTO dto = TourSearchValidator.validateAndBuild(Map.of(), multi);

        assertEquals(List.of(MealPlan.BB, MealPlan.HB), dto.mealPlans());
    }


    @Test
    void validateAndBuild_nullMultiBehavesLikeEmptyMulti() {
        Map<String, String> params = Map.of("destination", "Paris");

        TourRequestDTO viaNull = TourSearchValidator.validateAndBuild(params, null);
        TourRequestDTO viaEmpty = TourSearchValidator.validateAndBuild(params, new LinkedMultiValueMap<>());

        assertEquals(viaNull, viaEmpty);
    }


    @Test
    void validateAndBuild_blankDurationInMultiValue_isSkipped() {
        MultiValueMap<String, String> multi = new LinkedMultiValueMap<>();
        multi.add("duration", "");
        multi.add("duration", "1-3 days");

        TourRequestDTO dto = TourSearchValidator.validateAndBuild(Map.of(), multi);

        assertEquals(List.of(DurationBucket.DAYS_1_3), dto.durations());
    }

    @Test
    void validateAndBuild_blankMealPlanInMultiValue_isSkipped() {
        MultiValueMap<String, String> multi = new LinkedMultiValueMap<>();
        multi.add("mealPlan", "  ");
        multi.add("mealPlan", "BB");

        TourRequestDTO dto = TourSearchValidator.validateAndBuild(Map.of(), multi);

        assertEquals(List.of(MealPlan.BB), dto.mealPlans());
    }

    @Test
    void validateAndBuild_nullParams_returnsDefaults() {
        TourRequestDTO dto = TourSearchValidator.validateAndBuild(null, null);

        assertEquals(1, dto.page());
        assertEquals(6, dto.pageSize());
        assertNull(dto.destination());
    }
}
