package com.epam.edp.demo.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class AuthResolver {
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository repo) {

        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {

            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request,
                                                         HttpServletRequest servletRequest) {

                if (request == null) {return null;}

                String redirectUri = servletRequest.getParameter("redirect_uri");
                if (redirectUri != null) {
                    log.info("Storing redirect_uri in session: {}", redirectUri);
                    servletRequest.getSession().setAttribute("redirect_uri", redirectUri);
                }

                Map<String, Object> additionalParameters = new HashMap<>(request.getAdditionalParameters());
                additionalParameters.put("prompt", "select_account");

                return OAuth2AuthorizationRequest.from(request).additionalParameters(additionalParameters).build();
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(defaultResolver.resolve(request), request);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {

                return customize(defaultResolver.resolve(request, clientRegistrationId), request);
            }
        };
    }
}
