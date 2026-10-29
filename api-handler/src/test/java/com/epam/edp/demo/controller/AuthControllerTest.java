package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.auth.LogoutResponse;
import com.epam.edp.demo.dto.auth.SignInRequest;
import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.dto.auth.SignUpRequest;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.auth.AuthValidationException;
import com.epam.edp.demo.exception.auth.EmailAlreadyExistsException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void signUp_returns200_whenServiceReturnsMessage() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "Foo", "Bar", "valid-captcha-token");
        when(authService.signUp(any())).thenReturn(new MessageResponse("Verification code sent"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent"));
    }

    @Test
    void signUp_returns400_whenServiceThrowsAuthValidationException() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "Foo", "Bar", "valid-captcha-token");
        when(authService.signUp(any()))
                .thenThrow(new AuthValidationException("firstName: First name is required"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Auth validation error: firstName: First name is required"));
    }

    @Test
    void signUp_returns409_whenServiceThrowsConflictEmailAlreadyExistsException() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "Foo", "Bar", "valid-captcha-token");
        when(authService.signUp(any()))
                .thenThrow(new EmailAlreadyExistsException("User with this email already exists"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("User with this email already exists"));
    }

    @Test
    void signIn_returns200_withFullResponseBody() throws Exception {
        SignInRequest request = new SignInRequest("foo@bar.com", "RawP@ss1");
        SignInResponse svcResponse = SignInResponse.builder()
                .token("jwt-token")
                .role("CUSTOMER")
                .userName("foo@bar.com")
                .email("foo@bar.com")
                .build();
        when(authService.signIn(any())).thenReturn(svcResponse);

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.userName").value("foo@bar.com"))
                .andExpect(jsonPath("$.email").value("foo@bar.com"));
    }

    @Test
    void signIn_returns400_whenServiceThrowsAuthValidationException() throws Exception {
        SignInRequest request = new SignInRequest("foo@bar.com", "RawP@ss1");
        when(authService.signIn(any()))
                .thenThrow(new AuthValidationException("email: Invalid email address"));

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Auth validation error: email: Invalid email address"));
    }

    @Test
    void signIn_returns401_whenServiceThrowsBadCredentials() throws Exception {
        SignInRequest request = new SignInRequest("foo@bar.com", "wrong");
        when(authService.signIn(any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void logout_stripsBearerPrefixBeforeCallingService() throws Exception {
        when(authService.logout(any())).thenReturn(new LogoutResponse("Logged out successfully"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo("abc.def.ghi");
    }

    @Test
    void logout_forwardsNullWhenHeaderMissing() throws Exception {
        when(authService.logout(any())).thenReturn(new LogoutResponse("Logged out successfully"));

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isNull();
    }

    @Test
    void logout_forwardsFullHeaderWhenNoBearerPrefix_locksInCurrentBehavior() throws Exception {
        when(authService.logout(any())).thenReturn(new LogoutResponse("Logged out successfully"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "abc.def.ghi"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).logout(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo("abc.def.ghi");
    }
}
