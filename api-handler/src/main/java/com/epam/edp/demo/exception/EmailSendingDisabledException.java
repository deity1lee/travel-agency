package com.epam.edp.demo.exception;

public class EmailSendingDisabledException extends RuntimeException {
    public EmailSendingDisabledException(String message) {
        super(message);
    }
}
