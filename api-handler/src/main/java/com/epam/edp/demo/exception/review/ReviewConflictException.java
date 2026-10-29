package com.epam.edp.demo.exception.review;

public class ReviewConflictException extends RuntimeException {
    public ReviewConflictException(String message) {
        super(message);
    }

    public ReviewConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
