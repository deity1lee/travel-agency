package com.epam.edp.demo.exception.auth;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}