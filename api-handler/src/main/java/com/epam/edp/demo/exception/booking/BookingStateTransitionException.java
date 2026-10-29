package com.epam.edp.demo.exception.booking;

public class BookingStateTransitionException extends RuntimeException {
    public BookingStateTransitionException(String message) {
        super(message);
    }
}
