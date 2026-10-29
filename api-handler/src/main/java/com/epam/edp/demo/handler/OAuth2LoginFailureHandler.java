package com.epam.edp.demo.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 login failed: {}", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        log.error("OAuth2 login failed: {}", exception.getMessage());

        String redirectUri = (String) request.getSession().getAttribute("redirect_uri");

        log.info("Redirect URI from session: {}", redirectUri);


        if (redirectUri == null || !isAllowed(redirectUri)) {
            log.warn("Redirect URI is null or not allowed. Redirecting to default sign-in page.");
            redirectUri = "http://localhost:4200/auth/sign-in";
        }
        String redirect;
        if (redirectUri.endsWith("/oauth2/callback")) {
            redirect = redirectUri.substring(
                    0,
                    redirectUri.indexOf("/oauth2/callback")
            ) + "/sign-in";
        } else {
            redirect = redirectUri;
        }

        response.sendRedirect(redirect);
    }

    private boolean isAllowed(String uri) {
        return allowedOrigins.stream().anyMatch(uri::startsWith);
    }
}
