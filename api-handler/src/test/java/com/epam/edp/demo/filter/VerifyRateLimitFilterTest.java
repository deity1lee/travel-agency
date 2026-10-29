package com.epam.edp.demo.filter;

import com.epam.edp.demo.ratelimit.VerifyRateLimiter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifyRateLimitFilterTest {

    private final VerifyRateLimiter limiter = mock(VerifyRateLimiter.class);
    private final VerifyRateLimitFilter filter = new VerifyRateLimitFilter(limiter);

    @Test
    void underLimit_passesRequestDownTheChain() throws Exception {
        when(limiter.tryAcquire(anyString())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/sign-up/verify");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void overLimit_rejectsWith429AndDoesNotReachTheController() throws Exception {
        when(limiter.tryAcquire(anyString())).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/sign-up/verify");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        // Short-circuited before the controller, so no token guess is consumed.
        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void keyPrefersForwardedClientIpOverRemoteAddr() throws Exception {
        when(limiter.tryAcquire(anyString())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/sign-up/verify");
        request.setRemoteAddr("10.0.0.1"); // load balancer hop
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1"); // real client first
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture());
        assertThat(key.getValue()).contains("203.0.113.7");
    }

    @Test
    void keyFallsBackToRemoteAddr_whenForwardedHeaderIsBlank() throws Exception {
        when(limiter.tryAcquire(anyString())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/sign-up/verify");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "   "); // present but blank -> ignored
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(key.capture());
        assertThat(key.getValue()).isEqualTo("verify:10.0.0.1");
    }

    @Test
    void shouldNotFilter_isTrue_forNonRateLimitedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/sign-in");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_isFalse_forRateLimitedPaths() {
        MockHttpServletRequest signUpVerify = new MockHttpServletRequest("POST", "/auth/sign-up/verify");
        MockHttpServletRequest emailVerify = new MockHttpServletRequest("POST", "/auth/email/verify");

        assertThat(filter.shouldNotFilter(signUpVerify)).isFalse();
        assertThat(filter.shouldNotFilter(emailVerify)).isFalse();
    }
}
