package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.auth.SignUpRequest;
import com.epam.edp.demo.dto.auth.VerifySignUpRequest;
import com.epam.edp.demo.entity.auth.EmailVerificationToken;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.auth.VerificationType;
import com.epam.edp.demo.exception.auth.CaptchaValidationException;
import com.epam.edp.demo.exception.auth.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.auth.InvalidVerificationCodeException;
import com.epam.edp.demo.repository.auth.JwtBlacklistRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.captcha.CaptchaService;
import com.epam.edp.demo.service.document.S3PresignedUrlService;
import com.epam.edp.demo.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplSignUpTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final StaffRepository staffRepository = mock(StaffRepository.class);
    private final JwtBlacklistRepository jwtBlacklistRepository = mock(JwtBlacklistRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final VerificationTokenService verificationTokenService = mock(VerificationTokenService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final CaptchaService captchaService = mock(CaptchaService.class);
    private final S3PresignedUrlService presignedUrlService = mock(S3PresignedUrlService.class);

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(
                userRepository,
                staffRepository,
                jwtBlacklistRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                verificationTokenService,
                emailService,
                captchaService,
                presignedUrlService);
    }

    @Test
    void signUp_stashesTokenAndEmailsCode_withoutCreatingUser() {
        SignUpRequest request = new SignUpRequest("  Foo@Bar.COM  ", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("RawP@ss1")).thenReturn("ENCODED");
        when(verificationTokenService.createSignupToken("John", "Doe", "foo@bar.com", "ENCODED"))
                .thenReturn("483921");

        MessageResponse response = service.signUp(request);

        assertThat(response.getMessage()).isEqualTo("Verification code sent");
        verify(verificationTokenService).createSignupToken("John", "Doe", "foo@bar.com", "ENCODED");
        verify(emailService).sendSignupVerificationEmail("foo@bar.com", "483921");
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_neverPassesPlaintextPasswordToTokenService() {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("RawP@ss1")).thenReturn("ENCODED");
        when(verificationTokenService.createSignupToken(any(), any(), any(), any())).thenReturn("483921");

        service.signUp(request);

        verify(verificationTokenService, never())
                .createSignupToken(any(), any(), any(), eq("RawP@ss1"));
        verify(verificationTokenService).createSignupToken("John", "Doe", "foo@bar.com", "ENCODED");
    }

    @Test
    void signUp_emailAlreadyExists_throwsConflict_andWritesNothing() {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(User.builder().email("foo@bar.com").build()));

        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(verificationTokenService, never()).createSignupToken(any(), any(), any(), any());
        verify(emailService, never()).sendSignupVerificationEmail(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifySignUp_validCode_createsUserFromStashedTokenAndDeletesToken() {
        VerifySignUpRequest request = new VerifySignUpRequest("foo@bar.com", "483921");
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);
        token.setFirstName("John");
        token.setLastName("Doe");
        token.setPassword("BCRYPT_HASH");
        when(verificationTokenService.verifyCode("foo@bar.com", "483921", VerificationType.SIGNUP))
                .thenReturn(token);
        when(staffRepository.staffExists("foo@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);

        MessageResponse response = service.verifySignUp(request);

        assertThat(response.getMessage()).isEqualTo("Account created successfully");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getEmail()).isEqualTo("foo@bar.com");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPassword()).isEqualTo("BCRYPT_HASH");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);

        verify(verificationTokenService).delete(token);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void verifySignUp_registeredTravelAgentEmail_assignsTravelAgentRole() {
        VerifySignUpRequest request = new VerifySignUpRequest("agent@bar.com", "483921");
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("agent@bar.com");
        token.setType(VerificationType.SIGNUP);
        token.setFirstName("Jane");
        token.setLastName("Smith");
        token.setPassword("BCRYPT_HASH");
        when(verificationTokenService.verifyCode("agent@bar.com", "483921", VerificationType.SIGNUP))
                .thenReturn(token);
        when(staffRepository.staffExists("agent@bar.com", Role.TRAVEL_AGENT)).thenReturn(true);

        service.verifySignUp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.TRAVEL_AGENT);
    }

    @Test
    void verifySignUp_verificationFails_propagatesAndCreatesNothing() {
        VerifySignUpRequest request = new VerifySignUpRequest("foo@bar.com", "000000");
        when(verificationTokenService.verifyCode("foo@bar.com", "000000", VerificationType.SIGNUP))
                .thenThrow(new InvalidVerificationCodeException("nope", 4));

        assertThatThrownBy(() -> service.verifySignUp(request))
                .isInstanceOf(InvalidVerificationCodeException.class);

        verify(userRepository, never()).save(any());
        verify(verificationTokenService, never()).delete(any(EmailVerificationToken.class));
    }

    @Test
    void signUp_captchaValidationFails_throwsAndSkipsAllDownstreamCalls() {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "bad-token");
        doThrow(new CaptchaValidationException("Captcha verification failed"))
                .when(captchaService).validate("bad-token");

        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha verification failed");

        verify(userRepository, never()).findByEmail(any());
        verify(passwordEncoder, never()).encode(any());
        verify(verificationTokenService, never()).createSignupToken(any(), any(), any(), any());
        verify(emailService, never()).sendSignupVerificationEmail(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_captchaTokenIsNull_throwsCaptchaValidationException() {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", null);
        doThrow(new CaptchaValidationException("Captcha token is required"))
                .when(captchaService).validate(null);

        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(CaptchaValidationException.class)
                .hasMessage("Captcha token is required");

        verify(userRepository, never()).findByEmail(any());
        verify(emailService, never()).sendSignupVerificationEmail(any(), any());
    }
}
