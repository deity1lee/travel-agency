package com.epam.edp.demo.validation.booking;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.epam.edp.demo.dto.bookings.BookingRequest;

public class BookingGuestsValidator implements ConstraintValidator<ValidBookingGuests, BookingRequest> {

    private String message;

    @Override
    public void initialize(ValidBookingGuests annotation) {
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(BookingRequest request, ConstraintValidatorContext context) {
        if (request == null
                || request.guests() == null
                || request.personalDetails() == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("personalDetails")
                .addConstraintViolation();

        return request.guests().getTotalGuests() == request.personalDetails().size();
    }
}
