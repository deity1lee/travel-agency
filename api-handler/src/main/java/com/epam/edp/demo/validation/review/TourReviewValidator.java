package com.epam.edp.demo.validation.review;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.exception.tour.TourValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TourReviewValidator {

    public static void validateCreateRequest(String callerUserId, Booking booking, CreateReviewRequestDTO body, LocalDate today) {
        Map<String, String> errors = new HashMap<>();
        if (!booking.getUserId().equals(callerUserId)) {
            errors.put("caller", "must be booking owner");
        }

        checkBookingStatus(booking, today, errors);

        if (!errors.isEmpty()) {
            throw new TourValidationException(errors);
        }
    }

    public static void validateUpdateRequest(String callerUserId, Review review, Booking booking, UpdateReviewRequestDTO body, LocalDate today) {
        Map<String, String> errors = new HashMap<>();
        if (body.rate() == null && body.reviewContent() == null) {
            errors.put("body", "at least one of rate or reviewContent required");
            throw new TourValidationException(errors);
        }
        if (!review.getUserId().equals(callerUserId)) {
            errors.put("caller", "must be review author");
        }

        checkBookingStatus(booking, today, errors);

        Integer rate = body.rate();
        int effectiveRate = rate != null ? rate : review.getRate();
        String effectiveContent = body.reviewContent() != null ? body.reviewContent() : review.getReviewContent();
        if (!errors.containsKey("rate") && effectiveRate <= 3
                && (effectiveContent == null || effectiveContent.isBlank())) {
            errors.put("reviewContent", "required when rate is 1-3");
        }
        if (!errors.isEmpty()) {
            throw new TourValidationException(errors);
        }
    }

    private static void checkBookingStatus(Booking booking, LocalDate today, Map<String, String> errors) {
        BookingStatus effective = booking.effectiveStatus(today);
        if (effective != BookingStatus.STARTED && effective != BookingStatus.FINISHED) {
            errors.put("booking", "must be STARTED or FINISHED");
        }
    }
}
