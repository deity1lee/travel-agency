package com.epam.edp.demo.controller;

import com.epam.edp.demo.config.WebMvcConfig;
import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.auth.SignUpRequest;
import com.epam.edp.demo.dto.auth.VerifySignUpRequest;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.auth.CaptchaValidationException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
@Import({GlobalExceptionHandler.class, WebMvcConfig.class})
class AuthControllerSignUpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void signUp_validBody_returns200WithVerificationMessage() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        when(authService.signUp(any())).thenReturn(new MessageResponse("Verification code sent"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent"));

        verify(authService).signUp(any());
    }

    @Test
    void signUp_invalidEmail_returns400_withoutEnteringService() throws Exception {
        SignUpRequest request = new SignUpRequest("not-an-email", "RawP@ss1", "John", "Doe", "valid-captcha-token");

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signUp(any());
    }

    @Test
    void signUp_missingFirstName_returns400_withoutEnteringService() throws Exception {
        // firstName null violates @NotNull before the request reaches the service.
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", null, "Doe", "valid-captcha-token");

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signUp(any());
    }

    @Test
    void verifySignUp_validBody_returns201WithAccountCreatedMessage() throws Exception {
        VerifySignUpRequest request = new VerifySignUpRequest("foo@bar.com", "483921");
        when(authService.verifySignUp(any())).thenReturn(new MessageResponse("Account created successfully"));

        mockMvc.perform(post("/auth/sign-up/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created successfully"));

        verify(authService).verifySignUp(any());
    }

    @Test
    void verifySignUp_nonSixDigitCode_returns400_withoutEnteringService() throws Exception {
        VerifySignUpRequest request = new VerifySignUpRequest("foo@bar.com", "12ab");

        mockMvc.perform(post("/auth/sign-up/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).verifySignUp(any());
    }

    @Test
    void verifySignUp_invalidEmail_returns400_withoutEnteringService() throws Exception {
        VerifySignUpRequest request = new VerifySignUpRequest("not-an-email", "483921");

        mockMvc.perform(post("/auth/sign-up/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).verifySignUp(any());
    }

    @Test
    void signUp_captchaValidationFails_returns400WithMessage() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "bad-captcha-token");
        when(authService.signUp(any()))
                .thenThrow(new CaptchaValidationException("Captcha verification failed"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Captcha verification failed"));

        verify(authService).signUp(any());
    }

    @Test
    void signUp_captchaScoreTooLow_returns400WithMessage() throws Exception {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "low-score-token");
        when(authService.signUp(any()))
                .thenThrow(new CaptchaValidationException("Captcha score too low, request rejected"));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Captcha score too low, request rejected"));
    }
}
