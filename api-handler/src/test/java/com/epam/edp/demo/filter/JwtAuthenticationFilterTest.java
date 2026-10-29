package com.epam.edp.demo.filter;

import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.repository.auth.JwtBlacklistRepository;
import com.epam.edp.demo.service.auth.CustomUserDetailsService;
import com.epam.edp.demo.service.auth.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userService = mock(CustomUserDetailsService.class);
    private final JwtBlacklistRepository blacklistRepository = mock(JwtBlacklistRepository.class);

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtService, userService, blacklistRepository);

    @BeforeEach
    void clearContextBefore() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContextAfter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_callsChainAndLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(jwtService, userService, blacklistRepository);
    }

    @Test
    void doFilterInternal_headerNotBearer_callsChainAndLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc.def");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService, userService, blacklistRepository);
    }

    @Test
    void doFilterInternal_blacklistedToken_returns401AndDoesNotCallChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer blacklisted.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(blacklistRepository.exists("blacklisted.jwt")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired token.");
        verify(chain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_validToken_populatesSecurityContextAndCallsChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        User user = new User(
                "user-123",
                "foo@bar.com",
                "pass-123",
                "John",
                "Doe",
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(blacklistRepository.exists("valid.jwt")).thenReturn(false);
        when(jwtService.extractSubject("valid.jwt")).thenReturn("user-123");
        when(userService.loadUserById("user-123")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_userNotFound_returns401AndDoesNotCallChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(blacklistRepository.exists("valid.jwt")).thenReturn(false);
        when(jwtService.extractSubject("valid.jwt")).thenReturn("missing-id");
        when(userService.loadUserById("missing-id"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired token.");
        verify(chain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_malformedJwt_returns401AndDoesNotCallChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(blacklistRepository.exists("malformed.jwt")).thenReturn(false);
        when(jwtService.extractSubject("malformed.jwt"))
                .thenThrow(new MalformedJwtException("bad token"));

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired token.");
        verify(chain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotFilter_optionsRequest_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/anything");

        assertThat(invokeShouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_signInPath_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/auth/sign-in");

        assertThat(invokeShouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_signUpPath_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/auth/sign-up");

        assertThat(invokeShouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_toursSubPath_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/tours/123");

        assertThat(invokeShouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_swaggerUiPath_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/swagger-ui/index.html");

        assertThat(invokeShouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_protectedPath_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/bookings/abc");

        assertThat(invokeShouldNotFilter(request)).isFalse();
    }

    private boolean invokeShouldNotFilter(MockHttpServletRequest request) {
        Boolean result = ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request);
        return Boolean.TRUE.equals(result);
    }
}
