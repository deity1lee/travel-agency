package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.entity.auth.*;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.exception.auth.InvalidVerificationCodeException;
import com.epam.edp.demo.exception.auth.VerificationFailedException;
import com.epam.edp.demo.repository.auth.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

import java.util.Optional;
@Service
@Slf4j
public class VerificationTokenService {

    private static final int OTP_BOUND = 1_000_000;
    private static final int EXPIRY_MINUTES = 15;
    static final int MAX_ATTEMPTS = 5;
    public static final String INVALID_CODE_ERROR_RESPONSE = "The verification code is invalid or has expired.";

    private final SecureRandom secureRandom = new SecureRandom();
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder tokenEncoder;
    private final Clock clock;

    public VerificationTokenService(VerificationTokenRepository tokenRepository,
                                    PasswordEncoder tokenEncoder,
                                    Clock clock) {
        this.tokenRepository = tokenRepository;
        this.tokenEncoder = tokenEncoder;
        this.clock = clock;
    }

    private String generateNumericOtp() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }

    public String createSignupToken(String firstName, String lastName, String email, String hashedPassword) {
        LocalDateTime now = LocalDateTime.now(clock);

        String code = generateNumericOtp();
        String hashedCode = tokenEncoder.encode(code);

        EmailVerificationToken verificationToken = (EmailVerificationToken) findByEmailAndType(email, VerificationType.SIGNUP)
                .orElseGet(() -> {
                    EmailVerificationToken token = new EmailVerificationToken();
                    token.setEmail(email);
                    token.setType(VerificationType.SIGNUP);
                    return token;
                });

        verificationToken.setFirstName(firstName);
        verificationToken.setLastName(lastName);
        verificationToken.setPassword(hashedPassword);

        verificationToken.setTokenHash(hashedCode);
        verificationToken.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        verificationToken.setAttempts(0);

        tokenRepository.save(verificationToken);
        return code;
    }

    public String createEmailChangeToken(String email, String newEmail) {
        LocalDateTime now = LocalDateTime.now(clock);

        String code = generateNumericOtp();
        String hashedToken = tokenEncoder.encode(code);

        EmailChangeToken verificationToken = (EmailChangeToken) findByEmailAndType(email, VerificationType.CHANGE_EMAIL)
                .orElseGet(() -> {
                    EmailChangeToken token = new EmailChangeToken();
                    token.setEmail(email);
                    token.setType(VerificationType.CHANGE_EMAIL);
                    return token;
                });

        verificationToken.setTokenHash(hashedToken);
        verificationToken.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        verificationToken.setNewEmail(newEmail);
        verificationToken.setAttempts(0);

        tokenRepository.save(verificationToken);
        return code;
    }

    public String createPasswordResetToken(String email) {
        LocalDateTime now = LocalDateTime.now(clock);

        String token = generateNumericOtp();
        String hashedToken = tokenEncoder.encode(token);
        PasswordResetToken verificationToken = new PasswordResetToken();
        verificationToken.setEmail(email);
        verificationToken.setType(VerificationType.PASSWORD_RESET);
        verificationToken.setTokenHash(hashedToken);
        verificationToken.setExpiresAt(now.plusMinutes(15));
        tokenRepository.save(verificationToken);

        return token;
    }

    public PasswordResetToken verifyPasswordResetTokenWasValidated(String email, VerificationType type) {
        LocalDateTime now = LocalDateTime.now(clock);

        PasswordResetToken token = (PasswordResetToken)tokenRepository.findByEmailAndType(email, type)
                .orElseThrow(() -> new VerificationFailedException("No matching verification token for email"));
        if (token.getExpiresAt().isBefore(now)) {
            tokenRepository.delete(token);
            throw new VerificationFailedException(INVALID_CODE_ERROR_RESPONSE);
        }
        if (!token.isVerified()) {
            throw new VerificationFailedException("Code has not been verified");
        }
        return token;
    }

    public VerificationToken verifyCode(String email, String tokenStr, VerificationType type) {
        LocalDateTime now = LocalDateTime.now(clock);

        VerificationToken token = tokenRepository.findByEmailAndType(email, type)
                .orElseThrow(() -> new VerificationFailedException(INVALID_CODE_ERROR_RESPONSE));

        if (token.getExpiresAt().isBefore(now)) {
            tokenRepository.delete(token);
            throw new VerificationFailedException(INVALID_CODE_ERROR_RESPONSE);
        }

        if (!tokenEncoder.matches(tokenStr, token.getTokenHash())) {
            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= MAX_ATTEMPTS) {
                tokenRepository.delete(token);
            } else {
                tokenRepository.save(token);
            }
            throw new InvalidVerificationCodeException("Verification code did not match.", MAX_ATTEMPTS - token.getAttempts());
        }

        if (token instanceof PasswordResetToken) {
            ((PasswordResetToken) token).setVerified(true);
        }

        tokenRepository.save(token);

        return token;
    }

    public void delete(VerificationToken token) {
        tokenRepository.delete(token);
    }

    public Optional<VerificationToken> findByEmailAndType(String email, VerificationType type) {
        return tokenRepository.findByEmailAndType(email, type);
    }

    public boolean existsByEmailAndType(String email, VerificationType type) {
        return tokenRepository.existsByEmailAndType(email, type);
    }

    public String getNewOpt(String email, VerificationType type) {
        LocalDateTime now = LocalDateTime.now(clock);

        VerificationToken token = findByEmailAndType(email, type)
                .orElseThrow(()-> new VerificationFailedException(INVALID_CODE_ERROR_RESPONSE));

        if (token.getExpiresAt().isBefore(now)) {
            tokenRepository.delete(token);
            throw new VerificationFailedException(INVALID_CODE_ERROR_RESPONSE);
        }

        String code = generateNumericOtp();
        String hashedCode = tokenEncoder.encode(code);

        token.setTokenHash(hashedCode);
        token.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        token.setAttempts(0);

        tokenRepository.save(token);

        return code;
    }
}
