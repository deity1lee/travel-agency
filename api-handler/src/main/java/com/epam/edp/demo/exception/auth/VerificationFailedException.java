package com.epam.edp.demo.exception.auth;

public class VerificationFailedException extends RuntimeException {
    public VerificationFailedException(String message) {
        super(message);
    }
}
