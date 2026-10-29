package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.entity.auth.EmailChangeToken;
import com.epam.edp.demo.entity.auth.EmailVerificationToken;
import com.epam.edp.demo.entity.auth.PasswordResetToken;
import com.epam.edp.demo.entity.auth.VerificationToken;
import com.epam.edp.demo.entity.auth.VerificationType;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.exception.auth.InvalidVerificationCodeException;
import com.epam.edp.demo.exception.auth.VerificationFailedException;
import com.epam.edp.demo.repository.auth.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-30T10:00:00Z");
    private static final ZoneId FIXED_ZONE = ZoneId.of("UTC");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 6, 30, 10, 0);

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Clock clock;

    @InjectMocks
    private VerificationTokenService service;

    @Test
    void createSignupToken_persistsTokenWithStashedDataAndReturnsSixDigitCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String code = service.createSignupToken("John", "Doe", "foo@bar.com", "BCRYPT_HASH");

        assertThat(code).matches("^\\d{6}$");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("foo@bar.com");
        assertThat(saved.getType()).isEqualTo(VerificationType.SIGNUP);
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPassword()).isEqualTo("BCRYPT_HASH");
        assertThat(passwordEncoder.matches(code, saved.getTokenHash())).isTrue();
        assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plusMinutes(15));
    }

    @Test
    void createSignupToken_storesBcryptHashNotPlaintextCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String code = service.createSignupToken("John", "Doe", "foo@bar.com", "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();

        assertThat(saved.getTokenHash()).isNotEqualTo(code);
        assertThat(saved.getTokenHash()).startsWith("HASH(");
        assertThat(passwordEncoder.matches(code, saved.getTokenHash())).isTrue();
    }

    @Test
    void createSignupToken_storedHashVerifiesRealCodeAndRejectsWrongCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String code = service.createSignupToken("John", "Doe", "foo@bar.com", "BCRYPT_HASH");
        String wrongCode = "999999".equals(code) ? "000000" : "999999";

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        String storedHash = captor.getValue().getTokenHash();

        assertThat(passwordEncoder.matches(code, storedHash)).isTrue();
        assertThat(passwordEncoder.matches(wrongCode, storedHash)).isFalse();
    }


    @Test
    void verifyCode_roundTripsWithBcryptHash() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        // The email-change flow shares the bcrypt hashing/verification: a token written with
        // passwordEncoder.encode must read back via passwordEncoder.matches.
        String email = "foo@bar.com";

        String code = service.createEmailChangeToken(email, "new@bar.com");

        ArgumentCaptor<EmailChangeToken> captor = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailChangeToken stored = captor.getValue();
        when(tokenRepository.findByEmailAndType(email, VerificationType.CHANGE_EMAIL)).thenReturn(Optional.of(stored));

        VerificationToken verified = service.verifyCode(email, code, VerificationType.CHANGE_EMAIL);

        assertThat(verified).isSameAs(stored);
    }

    @Test
    void verifyCode_signupHappyPath_marksUsedAndPersists() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String email = "foo@bar.com";
        String code = service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.of(stored));

        VerificationToken verified = service.verifyCode(email, code, VerificationType.SIGNUP);

        assertThat(verified).isSameAs(stored);
        verify(tokenRepository).save(stored);
    }

    @Test
    void verifyCode_wrongCode_throwsInvalidAndDeletesNothing() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String email = "foo@bar.com";
        service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.verifyCode(email, "000000", VerificationType.SIGNUP))
                .isInstanceOf(InvalidVerificationCodeException.class);

        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void verifyCode_wrongCode_incrementsAttemptsAndPersistsWithoutDeleting() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String email = "foo@bar.com";
        service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.verifyCode(email, "000000", VerificationType.SIGNUP))
                .isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(stored.getAttempts()).isEqualTo(1);
        verify(tokenRepository).save((VerificationToken) stored); // counter persisted
        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void verifyCode_failedAttemptsReachingCap_invalidatesTokenWithSameException() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        String email = "foo@bar.com";
        service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.of(stored));

        // First MAX_ATTEMPTS-1 wrong guesses leave the token in place (counter persisted, not deleted).
        for (int i = 0; i < VerificationTokenService.MAX_ATTEMPTS - 1; i++) {
            assertThatThrownBy(() -> service.verifyCode(email, "000000", VerificationType.SIGNUP))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }
        verify(tokenRepository, never()).delete(any(VerificationToken.class));

        // The capping guess invalidates the row — but with the same exception, no lockout signal.
        assertThatThrownBy(() -> service.verifyCode(email, "000000", VerificationType.SIGNUP))
                .isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(captor.getValue().getAttempts()).isEqualTo(VerificationTokenService.MAX_ATTEMPTS);
        verify(tokenRepository).delete((VerificationToken) stored);
    }

    @Test
    void verifyCode_correctCodeAfterCapTripped_isRejected() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        // AC: once the cap deletes the token, even the correct code must fail (re-request required).
        String email = "foo@bar.com";
        String code = service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        stored.setAttempts(VerificationTokenService.MAX_ATTEMPTS); // already locked out
        // Token deleted on lockout, so the email no longer resolves to a row.
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode(email, code, VerificationType.SIGNUP))
                .isInstanceOf(VerificationFailedException.class);
    }

    @Test
    void verifyCode_noTokenForEmail_throwsInvalid() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        when(tokenRepository.findByEmailAndType("nobody@bar.com", VerificationType.SIGNUP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode("nobody@bar.com", "123456", VerificationType.SIGNUP))
                .isInstanceOf(VerificationFailedException.class);
    }

    @Test
    void verifyCode_expiredCode_deletesRowAndThrowsExpired() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        String email = "foo@bar.com";
        String code = service.createSignupToken("John", "Doe", email, "BCRYPT_HASH");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken stored = captor.getValue();
        stored.setExpiresAt(FIXED_NOW.minusMinutes(1)); // already expired
        when(tokenRepository.findByEmailAndType(email, VerificationType.SIGNUP)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.verifyCode(email, code, VerificationType.SIGNUP))
                .isInstanceOf(VerificationFailedException.class);

        verify(tokenRepository).delete((VerificationToken) stored);
    }

    @Test
    void createEmailChangeToken_noExistingToken_persistsNewTokenAndReturnsSixDigitCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.CHANGE_EMAIL))
                .thenReturn(Optional.empty());

        String code = service.createEmailChangeToken("foo@bar.com", "new@bar.com");

        assertThat(code).matches("^\\d{6}$");

        ArgumentCaptor<EmailChangeToken> captor = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailChangeToken saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("foo@bar.com");
        assertThat(saved.getType()).isEqualTo(VerificationType.CHANGE_EMAIL);
        assertThat(saved.getNewEmail()).isEqualTo("new@bar.com");
        assertThat(saved.getTokenHash()).isEqualTo("HASH(" + code + ")");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plusMinutes(15));
    }

    @Test
    void createEmailChangeToken_existingToken_isReusedAndUpdated() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");

        EmailChangeToken existing = new EmailChangeToken();
        existing.setEmail("foo@bar.com");
        existing.setType(VerificationType.CHANGE_EMAIL);
        existing.setNewEmail("stale@bar.com");
        existing.setAttempts(3);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.CHANGE_EMAIL))
                .thenReturn(Optional.of(existing));

        String code = service.createEmailChangeToken("foo@bar.com", "new@bar.com");

        assertThat(code).matches("^\\d{6}$");

        ArgumentCaptor<EmailChangeToken> captor = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailChangeToken saved = captor.getValue();

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getNewEmail()).isEqualTo("new@bar.com");
        assertThat(saved.getTokenHash()).isEqualTo("HASH(" + code + ")");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plusMinutes(15));
    }

    @Test
    void createPasswordResetToken_persistsResetTokenAndReturnsSixDigitCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");

        String code = service.createPasswordResetToken("foo@bar.com");

        assertThat(code).matches("^\\d{6}$");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("foo@bar.com");
        assertThat(saved.getType()).isEqualTo(VerificationType.PASSWORD_RESET);
        assertThat(saved.getTokenHash()).isEqualTo("HASH(" + code + ")");
        assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plusMinutes(15));
    }

    @Test
    void verifyPasswordResetTokenWasValidated_verifiedToken_returnsToken() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);
        token.setExpiresAt(FIXED_NOW.plusMinutes(5));
        token.setVerified(true);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        PasswordResetToken result =
                service.verifyPasswordResetTokenWasValidated("foo@bar.com", VerificationType.PASSWORD_RESET);

        assertThat(result).isSameAs(token);
        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void verifyPasswordResetTokenWasValidated_tokenNotFound_throwsUserNotFound() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.verifyPasswordResetTokenWasValidated("foo@bar.com", VerificationType.PASSWORD_RESET))
                .isInstanceOf(VerificationFailedException.class);
    }

    @Test
    void verifyPasswordResetTokenWasValidated_expiredToken_deletesAndThrowsVerificationFailed() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);
        token.setExpiresAt(FIXED_NOW.minusMinutes(1));
        token.setVerified(true);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.verifyPasswordResetTokenWasValidated("foo@bar.com", VerificationType.PASSWORD_RESET))
                .isInstanceOf(VerificationFailedException.class);

        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyPasswordResetTokenWasValidated_notVerified_throwsVerificationFailed() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);
        token.setExpiresAt(FIXED_NOW.plusMinutes(5));
        token.setVerified(false);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
                service.verifyPasswordResetTokenWasValidated("foo@bar.com", VerificationType.PASSWORD_RESET))
                .isInstanceOf(VerificationFailedException.class)
                .hasMessage("Code has not been verified");

        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void getNewOpt_existingNotExpired_refreshesTokenAndReturnsSixDigitCode() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "HASH(" + invocation.getArgument(0, String.class) + ")");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);
        token.setTokenHash("HASH(oldcode)");
        token.setExpiresAt(FIXED_NOW.plusMinutes(1));
        token.setAttempts(4);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(Optional.of(token));

        String code = service.getNewOpt("foo@bar.com", VerificationType.SIGNUP);

        assertThat(code).matches("^\\d{6}$");
        assertThat(token.getTokenHash()).isEqualTo("HASH(" + code + ")");
        assertThat(token.getExpiresAt()).isEqualTo(FIXED_NOW.plusMinutes(15));
        assertThat(token.getAttempts()).isZero();
        verify(tokenRepository).save((VerificationToken) token);
        verify(tokenRepository, never()).delete(any(VerificationToken.class));
    }

    @Test
    void getNewOpt_tokenNotFound_throwsVerificationFailed() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNewOpt("foo@bar.com", VerificationType.SIGNUP))
                .isInstanceOf(VerificationFailedException.class);

        verify(tokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    void getNewOpt_expiredToken_deletesAndThrowsVerificationFailed() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);
        token.setExpiresAt(FIXED_NOW.minusMinutes(1));
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.getNewOpt("foo@bar.com", VerificationType.SIGNUP))
                .isInstanceOf(VerificationFailedException.class);

        verify(tokenRepository).delete((VerificationToken) token);
        verify(tokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    void verifyCode_passwordReset_marksVerifiedAndPersists() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);
        when(passwordEncoder.matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0, String.class);
                    String encoded = invocation.getArgument(1, String.class);
                    return ("HASH(" + raw + ")").equals(encoded);
                });

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);
        token.setTokenHash("HASH(123456)");
        token.setExpiresAt(FIXED_NOW.plusMinutes(5));
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        VerificationToken verified = service.verifyCode("foo@bar.com", "123456", VerificationType.PASSWORD_RESET);

        assertThat(verified).isSameAs(token);
        assertThat(((PasswordResetToken) verified).isVerified()).isTrue();

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(((PasswordResetToken) captor.getValue()).isVerified()).isTrue();
    }

    @Test
    void findByEmailAndType_delegatesToRepositoryAndReturnsValue() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);
        when(tokenRepository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(Optional.of(token));

        Optional<VerificationToken> result = service.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP);

        assertThat(result).containsSame(token);
        verify(tokenRepository).findByEmailAndType("foo@bar.com", VerificationType.SIGNUP);
    }

    @Test
    void existsByEmailAndType_delegatesToRepositoryAndReturnsValue() {
        when(tokenRepository.existsByEmailAndType("foo@bar.com", VerificationType.SIGNUP))
                .thenReturn(true);

        boolean result = service.existsByEmailAndType("foo@bar.com", VerificationType.SIGNUP);

        assertThat(result).isTrue();
        verify(tokenRepository).existsByEmailAndType("foo@bar.com", VerificationType.SIGNUP);
    }

    @Test
    void delete_delegatesToRepository() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);

        service.delete(token);

        verify(tokenRepository).delete((VerificationToken) token);
    }
}
