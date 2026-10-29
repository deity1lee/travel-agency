package com.epam.edp.demo.handler;

import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.service.auth.OAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final OAuth2UserService oAuth2UserService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        log.info("OAuth2 login successful. Authentication: {}", authentication);
        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        OAuth2User oauthUser = oauthToken.getPrincipal();

        log.info("Google OAuth user:");
        log.info("Email: {}", Optional.ofNullable(oauthUser.getAttribute("email")));

        log.info("Processing social login for the user");
        SignInResponse signInResponse = oAuth2UserService.socialLogin(oauthUser);
        response.setStatus(HttpServletResponse.SC_FOUND);

        String redirectUri = (String) request.getSession().getAttribute("redirect_uri");

        log.info("Redirect URI from session: {}", redirectUri);

        if (redirectUri == null || !isAllowed(redirectUri)) {
            redirectUri = "http://localhost:4200/auth/oauth2/callback";
        }

        String encodedName = URLEncoder.encode(signInResponse.userName(), StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(signInResponse.email(), StandardCharsets.UTF_8);

        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("token", signInResponse.token())
                .queryParam("role", signInResponse.role())
                .queryParam("userName", encodedName)
                .queryParam("email", encodedEmail)
                .build(true)
                .toUriString();

        log.info("Redirecting OAuth2 user to frontend: {}", targetUrl);

        response.sendRedirect(targetUrl);
    }

    private boolean isAllowed(String uri) {
        return allowedOrigins.stream().anyMatch(uri::startsWith);
    }
}
