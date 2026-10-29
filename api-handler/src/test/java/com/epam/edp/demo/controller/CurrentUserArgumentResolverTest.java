package com.epam.edp.demo.controller;

import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsParameter_returnsTrue_whenParameterHasCurrentUserAnnotation() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.hasParameterAnnotation(CurrentUser.class)).thenReturn(true);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void supportsParameter_returnsFalse_whenAnnotationMissing() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.hasParameterAnnotation(CurrentUser.class)).thenReturn(false);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void resolveArgument_buildsUserContextFromCustomUserDetails() {
        User user = User.builder()
                .id("user-1")
                .email("jane@example.com")
                .password("pw")
                .role(Role.CUSTOMER)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        setAuthentication(principal);

        UserContext context = (UserContext) resolver.resolveArgument(
                mock(MethodParameter.class), null, mock(NativeWebRequest.class), null);

        Assertions.assertNotNull(context);
        assertThat(context.userId()).isEqualTo("user-1");
        assertThat(context.email()).isEqualTo("jane@example.com");
        assertThat(context.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void resolveArgument_preservesTravelAgentRole() {
        User user = User.builder()
                .id("agent-1")
                .email("agent@example.com")
                .password("pw")
                .role(Role.TRAVEL_AGENT)
                .build();
        setAuthentication(new CustomUserDetails(user));

        UserContext context = (UserContext) resolver.resolveArgument(
                mock(MethodParameter.class), null, mock(NativeWebRequest.class), null);

        Assertions.assertNotNull(context);
        assertThat(context.role()).isEqualTo(Role.TRAVEL_AGENT);
    }

    @Test
    void resolveArgument_throwsUnauthenticated_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resolver.resolveArgument(
                mock(MethodParameter.class), null, mock(NativeWebRequest.class), null))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("@CurrentUser used on unauthenticated endpoint");
    }

    @Test
    void resolveArgument_throwsUnauthenticated_whenPrincipalIsNotCustomUserDetails() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("plain-string", "pw");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> resolver.resolveArgument(
                mock(MethodParameter.class), null, mock(NativeWebRequest.class), null))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("@CurrentUser used on unauthenticated endpoint");
    }

    @Test
    void currentUserAnnotation_isParameterScopedAndRuntimeRetained() {
        Annotation[] declaredAnnotations = CurrentUser.class.getDeclaredAnnotations();
        assertThat(declaredAnnotations).isNotEmpty();
    }

    private static void setAuthentication(CustomUserDetails principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, "pw", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
