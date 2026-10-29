package com.epam.edp.demo.filter;

import com.epam.edp.demo.ratelimit.VerifyRateLimiter;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Throttles the OTP verify endpoints by source IP, ahead of the controller. Rejecting here means an
 * over-limit request never reaches {@code VerificationTokenService.verifyCode}, so it consumes no
 * per-token guess — an attacker cannot burn a token's attempt budget (or parallelise across tokens)
 * faster than the window allows. Pairs with the per-token {@code attempts} cap (issue 004).
 */
@Slf4j
@RequiredArgsConstructor
public class VerifyRateLimitFilter extends OncePerRequestFilter {

    // The verify entry points. POST /auth/sign-up/verify (public) and POST /auth/email/verify.
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/auth/sign-up/verify",
            "/auth/email/verify"
    );

    private final VerifyRateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only guard the verify endpoints; everything else passes straight through.
        return !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain)
            throws IOException, ServletException {

        String key = "verify:" + clientIp(request);
        if (!rateLimiter.tryAcquire(key)) {
            log.warn("Rate limit exceeded for verify request from {}", key);
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Too many requests. Please try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    // Prefer the first hop in X-Forwarded-For (the real client behind the load balancer);
    // fall back to the socket address when the header is absent.
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
