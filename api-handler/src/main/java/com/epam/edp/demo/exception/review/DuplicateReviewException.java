package com.epam.edp.demo.exception.review;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String message) {
        super(message);
    }

    public DuplicateReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
