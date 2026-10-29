package com.epam.edp.demo.exception;

public class SqsProcessingException extends RuntimeException {
    public SqsProcessingException(String message, Exception e) {
        super(message, e);
    }
}
