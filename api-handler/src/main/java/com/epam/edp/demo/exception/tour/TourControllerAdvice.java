package com.epam.edp.demo.exception.tour;

import com.epam.edp.demo.controller.TourController;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice(assignableTypes = TourController.class)
public class TourControllerAdvice {

    private final GlobalExceptionHandler globalExceptionHandler;

    @ExceptionHandler(TourNotFoundException.class)
    public ResponseEntity<String> handleTourNotFound(TourNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex, HandlerMethod handlerMethod) {
        if (ex instanceof TourValidationException tve) {
            return globalExceptionHandler.handleTourValidation(tve);
        }
        String methodName = handlerMethod.getMethod().getName();
        log.warn("Unexpected exception in {}: {}", methodName, ex.toString());
        String body = switch (methodName) {
            case "getAvailableTours" -> "no tours available";
            case "getDestinations" -> "no destinations available";
            default -> "tour not found";
        };
        return ResponseEntity.status(404).body(body);
    }
}
