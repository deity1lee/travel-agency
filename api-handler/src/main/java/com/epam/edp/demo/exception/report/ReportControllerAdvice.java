package com.epam.edp.demo.exception.report;

import com.epam.edp.demo.controller.ReportController;
import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice(assignableTypes = ReportController.class)
public class ReportControllerAdvice {

    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleResponseError(WebClientResponseException ex) {
        log.warn("Returning 502 BAD GATEWAY: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler(WebClientRequestException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<String> handleRequestError(WebClientRequestException ex) {
        HttpStatus status = (ex.getCause() instanceof TimeoutException)
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;
        log.warn("Returning {} {}: {}", status, status.name(), ex.getMessage());
        return ResponseEntity
                .status(status)
                .body(ex.getMessage());
    }
}
