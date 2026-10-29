package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.*;

import java.util.Optional;

public interface TokenRepository {
    void save(EmailChangeToken token);

    void save(EmailVerificationToken token);

    void save(PasswordResetToken token);

    void save(VerificationToken token);

    void delete(EmailChangeToken token);

    void delete(EmailVerificationToken token);

    void delete(VerificationToken token);

    Optional<VerificationToken> findByEmailAndType(String email, VerificationType type);

    boolean existsByEmailAndType(String email, VerificationType type);

}
