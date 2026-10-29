package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.MessageResponse;
import com.epam.edp.demo.dto.auth.AuthenticatedOptVerifyRequest;
import com.epam.edp.demo.dto.auth.EditEmailRequest;
import com.epam.edp.demo.dto.auth.EditPasswordRequest;
import com.epam.edp.demo.dto.auth.EditUserNamesRequest;
import com.epam.edp.demo.dto.auth.LogoutResponse;
import com.epam.edp.demo.dto.auth.ResendCodeRequest;
import com.epam.edp.demo.dto.auth.SendVerificationCodeRequest;
import com.epam.edp.demo.dto.auth.SignInRequest;
import com.epam.edp.demo.dto.auth.SignInResponse;
import com.epam.edp.demo.dto.auth.SignUpRequest;
import com.epam.edp.demo.dto.auth.VerifySignUpRequest;
import com.epam.edp.demo.dto.document.EditProfileResponse;
import com.epam.edp.demo.dto.document.PhotoUpload;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.EmailChangeToken;
import com.epam.edp.demo.entity.auth.EmailVerificationToken;
import com.epam.edp.demo.entity.auth.JwtBlacklist;
import com.epam.edp.demo.entity.auth.PasswordResetToken;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.auth.VerificationType;
import com.epam.edp.demo.entity.document.FileFormat;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.exception.auth.*;
import com.epam.edp.demo.repository.auth.JwtBlacklistRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.captcha.CaptchaService;
import com.epam.edp.demo.service.document.S3PresignedUrlService;
import com.epam.edp.demo.service.email.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private JwtBlacklistRepository jwtBlacklistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    void signUp_happyPath_normalizesEmailEncodesPasswordAndSendsVerificationCode() {
        SignUpRequest request = new SignUpRequest("  Foo@Bar.COM  ", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("RawP@ss1")).thenReturn("ENCODED");
        when(verificationTokenService.createSignupToken("John", "Doe", "foo@bar.com", "ENCODED"))
                .thenReturn("123456");

        MessageResponse response = service.signUp(request);

        // The user row is not persisted until the emailed code is verified.
        verify(userRepository, never()).save(any());
        verify(verificationTokenService).createSignupToken("John", "Doe", "foo@bar.com", "ENCODED");
        verify(emailService).sendSignupVerificationEmail("foo@bar.com", "123456");
        assertThat(response.getMessage()).isEqualTo("Verification code sent");
    }

    @Test
    void signUp_emailAlreadyExists_throwsConflictAndDoesNotSave() {
        SignUpRequest request = new SignUpRequest("foo@bar.com", "RawP@ss1", "John", "Doe", "valid-captcha-token");
        User existing = User.builder().email("foo@bar.com").build();
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .satisfies(thrown -> {
                    EmailAlreadyExistsException rse = (EmailAlreadyExistsException) thrown;
                    assertThat(rse.getMessage()).contains("already exists");
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifySignUp_travelAgentEmail_assignsTravelAgentRoleAndPersistsUser() {
        VerifySignUpRequest request = new VerifySignUpRequest("  Agent@Bar.COM  ", "123456");
        EmailVerificationToken token = stashedToken("Jane", "Doe", "ENCODED");
        when(verificationTokenService.verifyCode("agent@bar.com", "123456", VerificationType.SIGNUP))
                .thenReturn(token);
        when(staffRepository.staffExists("agent@bar.com", Role.TRAVEL_AGENT)).thenReturn(true);

        service.verifySignUp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getRole()).isEqualTo(Role.TRAVEL_AGENT);
        assertThat(saved.getEmail()).isEqualTo("agent@bar.com");
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        verify(verificationTokenService).delete(token);
    }

    @Test
    void verifySignUp_nonAgentEmail_assignsCustomerRole() {
        VerifySignUpRequest request = new VerifySignUpRequest("cust@bar.com", "123456");
        EmailVerificationToken token = stashedToken("Jane", "Doe", "ENCODED");
        when(verificationTokenService.verifyCode("cust@bar.com", "123456", VerificationType.SIGNUP))
                .thenReturn(token);
        when(staffRepository.staffExists("cust@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);

        service.verifySignUp(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    private static EmailVerificationToken stashedToken(String firstName, String lastName, String hashedPassword) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setFirstName(firstName);
        token.setLastName(lastName);
        token.setPassword(hashedPassword);
        return token;
    }

    @Test
    void signIn_happyPath_authenticatesWithNormalizedEmailAndReturnsMappedResponse() {
        SignInRequest request = new SignInRequest("  Foo@Bar.COM  ", "RawP@ss1");
        String firstName = "John";
        String lastName = "Doe";
        User user = new User(
                "user-123",
                "foo@bar.com",
                "ENCODED",
                firstName,
                lastName,
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        SignInResponse expected = SignInResponse.builder()
                .token("jwt-token")
                .role("CUSTOMER")
                .userName("foo@bar.com")
                .email("foo@bar.com")
                .build();

        when(s3PresignedUrlService.getDocumentsBucketName()).thenReturn("a");
        when(s3PresignedUrlService.verifyFileExistsByKey(eq("a"), any())).thenReturn(false);
        SignInResponse response = service.signIn(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        UsernamePasswordAuthenticationToken authToken = tokenCaptor.getValue();
        assertThat(authToken.getPrincipal()).isEqualTo("foo@bar.com");
        assertThat(authToken.getCredentials()).isEqualTo("RawP@ss1");
        assertThat(response.token()).isEqualTo(expected.token());
        assertThat(response.role()).isEqualTo(expected.role());
        assertThat(response.userName()).isEqualTo(firstName + " " + lastName);
        assertThat(response.email()).isEqualTo(expected.email());
    }

    @Test
    void signIn_normalizesEmailFromMixedCaseWithWhitespace() {
        SignInRequest request = new SignInRequest("  Foo@Bar.COM  ", "RawP@ss1");
        User user = new User(
                "user-123",
                "foo@bar.com",
                "ENCODED",
                "John",
                "Doe",
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        when(s3PresignedUrlService.getDocumentsBucketName()).thenReturn("a");
        when(s3PresignedUrlService.verifyFileExistsByKey(eq("a"), any())).thenReturn(false);
        service.signIn(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("foo@bar.com");
    }

    @Test
    void signIn_badCredentials_authenticationManagerThrows_rewrappedAsBadCredentials() {
        SignInRequest request = new SignInRequest("foo@bar.com", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationException("provider-failure") {});

        assertThatThrownBy(() -> service.signIn(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void logout_happyPath_persistsBlacklistEntryWithExpirationAndReturnsMessage() {
        String token = "abc.def.ghi";
        Date expiration = new Date(1_700_000_000_000L);
        when(jwtService.extractExpiration(token)).thenReturn(expiration);

        LogoutResponse response = service.logout(token);

        verify(jwtService).extractExpiration(token);
        ArgumentCaptor<JwtBlacklist> captor = ArgumentCaptor.forClass(JwtBlacklist.class);
        verify(jwtBlacklistRepository).save(captor.capture());
        JwtBlacklist saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiresAt()).isEqualTo(expiration.toInstant());
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    void logout_nullToken_throwsBadCredentialsAndSkipsJwtAndRepo() {
        assertThatThrownBy(() -> service.logout(null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Missing or invalid Authorization token");

        verify(jwtService, never()).extractExpiration(any());
        verify(jwtBlacklistRepository, never()).save(any());
    }

    @Test
    void logout_emptyStringToken_persistsToBlacklistLockingInCurrentBehavior() {
        Date expiration = new Date(1_700_000_000_000L);
        when(jwtService.extractExpiration("")).thenReturn(expiration);

        LogoutResponse response = service.logout("");

        ArgumentCaptor<JwtBlacklist> captor = ArgumentCaptor.forClass(JwtBlacklist.class);
        verify(jwtBlacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("");
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    void findRole_travelAgentExists_returnsTravelAgent() {
        when(staffRepository.staffExists("agent@bar.com", Role.TRAVEL_AGENT)).thenReturn(true);

        assertThat(service.findRole("agent@bar.com")).isEqualTo(Role.TRAVEL_AGENT);
    }

    @Test
    void findRole_travelAgentDoesNotExist_returnsCustomer() {
        when(staffRepository.staffExists("cust@bar.com", Role.TRAVEL_AGENT)).thenReturn(false);

        assertThat(service.findRole("cust@bar.com")).isEqualTo(Role.CUSTOMER);
    }

    // ---------------------------------------------------------------------
    // signIn - additional branches
    // ---------------------------------------------------------------------

    @Test
    void signIn_emailNotVerified_throwsAndSkipsAuthentication() {
        SignInRequest request = new SignInRequest("  Foo@Bar.COM  ", "RawP@ss1");
        when(verificationTokenService.existsByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(true);

        assertThatThrownBy(() -> service.signIn(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("verify your email");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void signIn_profilePictureExists_returnsRetrieveUrl() {
        SignInRequest request = new SignInRequest("foo@bar.com", "RawP@ss1");
        User user = new User("user-123", "foo@bar.com", "ENCODED", "John", "Doe", Role.CUSTOMER);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        String expectedKey = String.format(S3PresignedUrlService.PICTURE_UPLOAD_URL_BASE, "user-123");
        when(s3PresignedUrlService.getDocumentsBucketName()).thenReturn("bucket");
        when(s3PresignedUrlService.verifyFileExistsByKey("bucket", expectedKey)).thenReturn(true);
        when(s3PresignedUrlService.generateRetrieveUrl("bucket", expectedKey)).thenReturn("pic-url");

        SignInResponse response = service.signIn(request);

        assertThat(response.imageUrl()).isEqualTo("pic-url");
    }

    // ---------------------------------------------------------------------
    // editNames
    // ---------------------------------------------------------------------

    @Test
    void editNames_withPhoto_generatesUploadUrlAndUpdatesNames() {
        UserContext ctx = new UserContext("user-123", "foo@bar.com", Role.CUSTOMER);
        PhotoUpload photo = new PhotoUpload(FileFormat.png, 100);
        EditUserNamesRequest request = new EditUserNamesRequest("John", "Doe", photo);

        String expectedKey = String.format(S3PresignedUrlService.PICTURE_UPLOAD_URL_BASE, "user-123");
        when(s3PresignedUrlService.getDocumentsBucketName()).thenReturn("bucket");
        when(s3PresignedUrlService.generatePictureUploadUrl("bucket", expectedKey, FileFormat.png))
                .thenReturn("upload-url");

        EditProfileResponse response = service.editNames(ctx, request);

        verify(userRepository).updateNames("user-123", "John", "Doe");
        assertThat(response.message()).isEqualTo("Profile edited successfully");
        assertThat(response.uploadUrl()).isEqualTo("upload-url");
    }

    @Test
    void editNames_withoutPhoto_updatesNamesWithNullUploadUrl() {
        UserContext ctx = new UserContext("user-123", "foo@bar.com", Role.CUSTOMER);
        EditUserNamesRequest request = new EditUserNamesRequest("John", "Doe", null);

        EditProfileResponse response = service.editNames(ctx, request);

        verify(userRepository).updateNames("user-123", "John", "Doe");
        verify(s3PresignedUrlService, never()).generatePictureUploadUrl(any(), any(), any());
        assertThat(response.message()).isEqualTo("Profile edited successfully");
        assertThat(response.uploadUrl()).isNull();
    }

    // ---------------------------------------------------------------------
    // editEmail
    // ---------------------------------------------------------------------

    @Test
    void editEmail_happyPath_createsChangeTokenAndSendsEmail() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        EditEmailRequest request = new EditEmailRequest("new@bar.com", "correct-password");
        User user = new User("user-123", "old@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@bar.com")).thenReturn(false);
        setSecurityPrincipal(new CustomUserDetails(user));
        when(passwordEncoder.matches("correct-password", "HASH")).thenReturn(true);
        when(verificationTokenService.createEmailChangeToken("old@bar.com", "new@bar.com")).thenReturn("tok");

        MessageResponse response = service.editEmail(ctx, request);

        verify(emailService).sendChangeEmailEmail("new@bar.com", "tok");
        assertThat(response.getMessage()).isEqualTo("Email verification sent");
    }

    @Test
    void editEmail_userNotFound_throwsUserNotFoundException() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        EditEmailRequest request = new EditEmailRequest("new@bar.com", "correct-password");
        when(userRepository.findById("user-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editEmail(ctx, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("not found");

        verify(verificationTokenService, never()).createEmailChangeToken(any(), any());
        verify(emailService, never()).sendChangeEmailEmail(any(), any());
    }

    @Test
    void editEmail_newEmailAlreadyExists_throwsConflict() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        EditEmailRequest request = new EditEmailRequest("taken@bar.com", "correct-password");
        User user = new User("user-123", "old@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@bar.com")).thenReturn(true);

        assertThatThrownBy(() -> service.editEmail(ctx, request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(verificationTokenService, never()).createEmailChangeToken(any(), any());
        verify(emailService, never()).sendChangeEmailEmail(any(), any());
    }

    @Test
    void editEmail_incorrectPassword_throwsIncorrectPasswordException() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        EditEmailRequest request = new EditEmailRequest("new@bar.com", "wrong-password");
        User user = new User("user-123", "old@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@bar.com")).thenReturn(false);
        setSecurityPrincipal(new CustomUserDetails(user));
        when(passwordEncoder.matches("wrong-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.editEmail(ctx, request))
                .isInstanceOf(IncorrectPasswordException.class)
                .hasMessageContaining("Invalid password");

        verify(verificationTokenService, never()).createEmailChangeToken(any(), any());
        verify(emailService, never()).sendChangeEmailEmail(any(), any());
    }

    @Test
    void editEmail_googleAccountWithNoPassword_throwsEditProfileException() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        EditEmailRequest request = new EditEmailRequest("new@bar.com", "whatever");
        User user = new User("user-123", "old@bar.com", null, "John", "Doe", Role.CUSTOMER);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@bar.com")).thenReturn(false);
        setSecurityPrincipal(new CustomUserDetails(user));

        assertThatThrownBy(() -> service.editEmail(ctx, request))
                .isInstanceOf(EditProfileException.class);

        verify(verificationTokenService, never()).createEmailChangeToken(any(), any());
    }

    // ---------------------------------------------------------------------
    // editPassword
    // ---------------------------------------------------------------------

    @Test
    void editPassword_happyPath_encodesAndUpdatesPassword() {
        UserContext ctx = new UserContext("user-123", "foo@bar.com", Role.CUSTOMER);
        EditPasswordRequest request = new EditPasswordRequest("new-password", "old-password");
        User user = new User("user-123", "foo@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        setSecurityPrincipal(new CustomUserDetails(user));
        when(passwordEncoder.matches("old-password", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("NEW_HASH");

        MessageResponse response = service.editPassword(ctx, request);

        verify(userRepository).updatePassword("user-123", "NEW_HASH");
        assertThat(response.getMessage()).isEqualTo("Password edited successfully");
    }

    @Test
    void editPassword_incorrectOldPassword_throwsAndDoesNotUpdate() {
        UserContext ctx = new UserContext("user-123", "foo@bar.com", Role.CUSTOMER);
        EditPasswordRequest request = new EditPasswordRequest("new-password", "wrong-old-password");
        User user = new User("user-123", "foo@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        setSecurityPrincipal(new CustomUserDetails(user));
        when(passwordEncoder.matches("wrong-old-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.editPassword(ctx, request))
                .isInstanceOf(IncorrectPasswordException.class)
                .hasMessageContaining("Invalid password");

        verify(userRepository, never()).updatePassword(any(), any());
    }

    // ---------------------------------------------------------------------
    // sendVerificationCodeForPasswordReset
    // ---------------------------------------------------------------------

    @Test
    void sendVerificationCodeForPasswordReset_happyPath_createsTokenAndSendsEmail() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest("foo@bar.com");
        User user = new User("user-123", "foo@bar.com", "HASH", "John", "Doe", Role.CUSTOMER);
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(user));
        when(verificationTokenService.createPasswordResetToken("foo@bar.com")).thenReturn("tok");

        MessageResponse response = service.sendVerificationCodeForPasswordReset(request);

        verify(emailService).sendPasswordResetVerificationEmail("foo@bar.com", "tok");
        assertThat(response.getMessage()).isEqualTo("Verification code was sent");
    }

    @Test
    void sendVerificationCodeForPasswordReset_userNotFound_throwsBadCredentials() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest("nobody@bar.com");
        when(userRepository.findByEmail("nobody@bar.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendVerificationCodeForPasswordReset(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not found");

        verify(verificationTokenService, never()).createPasswordResetToken(any());
        verify(emailService, never()).sendPasswordResetVerificationEmail(any(), any());
    }

    @Test
    void sendVerificationCodeForPasswordReset_googleAccountWithNoPassword_throwsEditProfileException() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest("google@bar.com");
        User user = new User("user-123", "google@bar.com", null, "John", "Doe", Role.CUSTOMER);
        when(userRepository.findByEmail("google@bar.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.sendVerificationCodeForPasswordReset(request))
                .isInstanceOf(EditProfileException.class)
                .hasMessageContaining("Google account");

        verify(verificationTokenService, never()).createPasswordResetToken(any());
    }

    // ---------------------------------------------------------------------
    // verifyEmailChangeToken
    // ---------------------------------------------------------------------

    @Test
    void verifyEmailChangeToken_validCode_updatesEmailAndDeletesToken() {
        UserContext ctx = new UserContext("user-123", "old@bar.com", Role.CUSTOMER);
        AuthenticatedOptVerifyRequest request = new AuthenticatedOptVerifyRequest("123456");
        EmailChangeToken token = new EmailChangeToken();
        token.setNewEmail("new@bar.com");
        when(verificationTokenService.verifyCode("old@bar.com", "123456", VerificationType.CHANGE_EMAIL))
                .thenReturn(token);

        service.verifyEmailChangeToken(ctx, request);

        verify(userRepository).updateEmail("user-123", "new@bar.com");
        verify(verificationTokenService).delete(token);
    }

    // ---------------------------------------------------------------------
    // verifyPasswordResetToken
    // ---------------------------------------------------------------------

    @Test
    void verifyPasswordResetToken_delegatesToVerificationTokenService() {
        service.verifyPasswordResetToken("foo@bar.com", "123456");

        verify(verificationTokenService).verifyCode("foo@bar.com", "123456", VerificationType.PASSWORD_RESET);
    }

    // ---------------------------------------------------------------------
    // resendOtpAuth
    // ---------------------------------------------------------------------

    @Test
    void resendOtpAuth_changeEmailType_resendsChangeEmailCode() {
        ResendCodeRequest request = new ResendCodeRequest("foo@bar.com", VerificationType.CHANGE_EMAIL);
        when(verificationTokenService.getNewOpt("foo@bar.com", VerificationType.CHANGE_EMAIL)).thenReturn("code");

        MessageResponse response = service.resendOtpAuth(request);

        verify(emailService).sendChangeEmailEmail("foo@bar.com", "code");
        assertThat(response.getMessage()).isEqualTo("Verification code sent");
    }

    @Test
    void resendOtpAuth_unsupportedType_throwsBadCredentials() {
        ResendCodeRequest request = new ResendCodeRequest("foo@bar.com", VerificationType.SIGNUP);
        when(verificationTokenService.getNewOpt("foo@bar.com", VerificationType.SIGNUP)).thenReturn("code");

        assertThatThrownBy(() -> service.resendOtpAuth(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Unsupported verification type");

        verify(emailService, never()).sendChangeEmailEmail(any(), any());
    }

    // ---------------------------------------------------------------------
    // resendOtpNotAuth
    // ---------------------------------------------------------------------

    @Test
    void resendOtpNotAuth_signupType_resendsSignupCode() {
        ResendCodeRequest request = new ResendCodeRequest("foo@bar.com", VerificationType.SIGNUP);
        when(verificationTokenService.getNewOpt("foo@bar.com", VerificationType.SIGNUP)).thenReturn("code");

        MessageResponse response = service.resendOtpNotAuth(request);

        verify(emailService).sendSignupVerificationEmail("foo@bar.com", "code");
        assertThat(response.getMessage()).isEqualTo("Verification code sent");
    }

    @Test
    void resendOtpNotAuth_passwordResetType_resendsPasswordResetCode() {
        ResendCodeRequest request = new ResendCodeRequest("foo@bar.com", VerificationType.PASSWORD_RESET);
        when(verificationTokenService.getNewOpt("foo@bar.com", VerificationType.PASSWORD_RESET)).thenReturn("code");

        MessageResponse response = service.resendOtpNotAuth(request);

        verify(emailService).sendPasswordResetVerificationEmail("foo@bar.com", "code");
        assertThat(response.getMessage()).isEqualTo("Verification code sent");
    }

    @Test
    void resendOtpNotAuth_unsupportedType_throwsBadCredentials() {
        ResendCodeRequest request = new ResendCodeRequest("foo@bar.com", VerificationType.CHANGE_EMAIL);
        when(verificationTokenService.getNewOpt("foo@bar.com", VerificationType.CHANGE_EMAIL)).thenReturn("code");

        assertThatThrownBy(() -> service.resendOtpNotAuth(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Unsupported verification type");

        verify(emailService, never()).sendSignupVerificationEmail(any(), any());
        verify(emailService, never()).sendPasswordResetVerificationEmail(any(), any());
    }

    // ---------------------------------------------------------------------
    // resetPassword
    // ---------------------------------------------------------------------

    @Test
    void resetPassword_happyPath_encodesUpdatesAndDeletesToken() {
        User user = new User("user-123", "foo@bar.com", "OLD_HASH", "John", "Doe", Role.CUSTOMER);
        PasswordResetToken token = new PasswordResetToken();
        when(userRepository.findByEmail("foo@bar.com")).thenReturn(Optional.of(user));
        when(verificationTokenService.verifyPasswordResetTokenWasValidated("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(token);
        when(passwordEncoder.encode("new-password")).thenReturn("NEW_HASH");

        MessageResponse response = service.resetPassword("foo@bar.com", "new-password");

        verify(userRepository).updatePassword("user-123", "NEW_HASH");
        verify(verificationTokenService).delete(token);
        assertThat(response.getMessage()).isEqualTo("Password edited successfully");
    }

    @Test
    void resetPassword_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByEmail("nobody@bar.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("nobody@bar.com", "new-password"))
                .isInstanceOf(VerificationFailedException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).updatePassword(any(), any());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityPrincipal(UserDetails principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
