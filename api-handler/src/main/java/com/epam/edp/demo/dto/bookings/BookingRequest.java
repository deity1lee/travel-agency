package com.epam.edp.demo.dto.bookings;

import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.booking.PersonalDetails;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.validation.booking.ValidBookingGuests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

@ValidBookingGuests
public record BookingRequest(
        @Schema(description = "Id of tour to be booked", example = "tour-002f")
        @NotBlank(message = "Tour id must not be empty")
        String tourId,

        @Schema(description = "Start Date of the tour", example = "2026-09-01")
        @NotNull(message = "Tour start date must be specified")
        @FutureOrPresent(message = "Tour date must be in the present or future")
        LocalDate date,

        @Schema(description = "Duration of the tour in days", example = "12 days")
        @NotNull(message = "Tour duration must be specified")
        @Pattern(
            regexp = "^\\d+\\sdays?$",
            message = "Duration must be in format 'xxx days'"
        )
        String duration,

        @Schema(description = "Meal Plan of the tour", example = "BB")
        @NotNull(message = "Meal Plan must be provided")
        MealPlan mealPlan,

        @NotNull(message = "Guests quantity must be provided")
        @Valid
        Guests guests,

        @NotNull(message = "Personal details must be provided")
        List<@Valid PersonalDetails> personalDetails
) {
}
