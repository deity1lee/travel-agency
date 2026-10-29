package com.epam.edp.demo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    @InjectMocks
    private ApiKeyFilter apiKeyFilter = new ApiKeyFilter("expected-api-key");

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    void test_shouldNotFilter_returnsTrue_whenPathIsHelloEndpoint() {
        when(request.getServletPath()).thenReturn("/api/hello");

        boolean result = apiKeyFilter.shouldNotFilter(request);

        assertTrue(result);
    }

    @Test
    void test_shouldNotFilter_returnsFalse_whenPathIsNotHelloEndpoint() {
        when(request.getServletPath()).thenReturn("/report");

        boolean result = apiKeyFilter.shouldNotFilter(request);

        assertFalse(result);
    }

    @Test
    void test_doFilterInternal_callsFilterChain_whenApiKeyIsValid() throws ServletException, IOException {
        when(request.getHeader("X-API-KEY")).thenReturn("expected-api-key");

        apiKeyFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void test_doFilterInternal_returnsUnauthorizedAndErrorBody_whenApiKeyIsInvalid() throws ServletException, IOException {
        when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        apiKeyFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(responseBody.toString().contains("Invalid API key"));
    }

    @Test
    void test_doFilterInternal_returnsUnauthorized_whenApiKeyHeaderIsMissing() throws ServletException, IOException {
        when(request.getHeader("X-API-KEY")).thenReturn(null);
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        apiKeyFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(responseBody.toString().contains("Invalid API key"));
    }

    @Test
    void test_doFilterInternal_propagatesIOException_whenResponseWriterFails() throws IOException, ServletException {
        when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");
        when(response.getWriter()).thenThrow(new IOException("writer failure"));

        IOException ex = assertThrows(
                IOException.class,
                () -> apiKeyFilter.doFilterInternal(request, response, filterChain)
        );

        assertEquals("writer failure", ex.getMessage());
        verify(filterChain, never()).doFilter(request, response);
    }
}


