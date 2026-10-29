package com.epam.edp.demo.validation.booking;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.epam.edp.demo.entity.booking.Guests;

public class GuestCountValidator implements ConstraintValidator<ValidGuestCount, Guests> {

    private static final int MAX_GUESTS = 10;

    private String message;

    @Override
    public void initialize(ValidGuestCount annotation) {
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Guests guests, ConstraintValidatorContext context) {
        if (guests == null
                || guests.getAdults() == null
                || guests.getChildren() == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();

        return guests.getTotalGuests() <= MAX_GUESTS;
    }
}
