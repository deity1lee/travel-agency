package com.epam.edp.demo.filter;

import com.epam.edp.demo.repository.auth.JwtBlacklistRepository;
import com.epam.edp.demo.service.auth.CustomUserDetailsService;
import com.epam.edp.demo.service.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userService;
    private final JwtBlacklistRepository blacklistRepository;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/auth/sign-in",
            "/auth/sign-up",
            "/tours",
            "/swagger-ui",
//            "/healthz",
//            "/ping",
//            "/actuator/health",
            "/v3/api-docs",
            "/dev/seed",
            "/api/hello",
            "/oauth2",
            "/login",
            "/api/hello",
            "/auth/forgot-password",
            "/auth/forgot-password/verify",
            "/auth/reset-password",
            "/auth/non/resend"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")) {
            log.info("Processing authentication for request: {} {}", request.getMethod(), request.getRequestURI());
            log.info("Authorization header: {}", header);

            String token = header.substring(7);

            try {
                if (blacklistRepository.exists(token)) {
                    log.info("Blacklisted token rejected");
                    throw new IllegalArgumentException();
                }

                String userId = jwtService.extractSubject(token);
                log.info("Extracted id from token: {}", userId);
                UserDetails user = userService.loadUserById(userId);

                log.info("User details loaded: {}", user.getUsername());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("Authentication successful for user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
                log.debug("Invalid or expired token: ", e);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\": \"Invalid or expired token.\"}");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(request,response);
    }
}
