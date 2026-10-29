package com.epam.edp.demo.exception;

public class EmailProcessingException extends RuntimeException {
    public EmailProcessingException(String message, Exception cause) {
        super(message, cause);
    }
}

