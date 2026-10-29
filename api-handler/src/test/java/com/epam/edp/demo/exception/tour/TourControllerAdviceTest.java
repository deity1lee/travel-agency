package com.epam.edp.demo.exception.tour;

import com.epam.edp.demo.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourControllerAdviceTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private TourControllerAdvice advice;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = mock(GlobalExceptionHandler.class);
        advice = new TourControllerAdvice(globalExceptionHandler);
    }

    @Test
    void handleTourNotFound_returns404WithMessageBody() {
        ResponseEntity<String> response = advice.handleTourNotFound(
                new TourNotFoundException("tour-1 missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("tour-1 missing");
    }

    @Test
    void handleUnexpected_delegatesToGlobalHandler_forTourValidationException() {
        TourValidationException tve = new TourValidationException(Map.of("k", "v"));
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> expected =
                (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity
                        .badRequest()
                        .body(Map.of("message", "Validation failed"));
        when(globalExceptionHandler.handleTourValidation(tve)).thenReturn(expected);

        ResponseEntity<?> result = advice.handleUnexpected(tve, handlerMethodFor("getAvailableTours"));

        assertThat(result).isSameAs(expected);
        verify(globalExceptionHandler).handleTourValidation(tve);
    }

    @Test
    void handleUnexpected_returnsNoToursAvailableBody_forGetAvailableTours() {
        ResponseEntity<?> response = advice.handleUnexpected(
                new RuntimeException("boom"), handlerMethodFor("getAvailableTours"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("no tours available");
    }

    @Test
    void handleUnexpected_returnsNoDestinationsAvailableBody_forGetDestinations() {
        ResponseEntity<?> response = advice.handleUnexpected(
                new RuntimeException("boom"), handlerMethodFor("getDestinations"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("no destinations available");
    }

    @Test
    void handleUnexpected_returnsTourNotFoundBody_forOtherMethodNames() {
        ResponseEntity<?> response = advice.handleUnexpected(
                new RuntimeException("boom"), handlerMethodFor("anyOtherMethod"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("tour not found");
    }

    private static HandlerMethod handlerMethodFor(String methodName) {
        HandlerMethod handlerMethod = mock(HandlerMethod.class);
        try {
            Method method = Stub.class.getMethod(methodName);
            when(handlerMethod.getMethod()).thenReturn(method);
            return handlerMethod;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unused")
    public static class Stub {
        public void getAvailableTours() {}
        public void getDestinations() {}
        public void anyOtherMethod() {}
    }
}
