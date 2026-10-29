package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.EmailChangeToken;
import com.epam.edp.demo.entity.auth.EmailVerificationToken;
import com.epam.edp.demo.entity.auth.PasswordResetToken;
import com.epam.edp.demo.entity.auth.VerificationToken;
import com.epam.edp.demo.entity.auth.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationTokenRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private VerificationTokenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new VerificationTokenRepository(mongoTemplate);
    }

    // ---- save ----

    @Test
    void saveEmailVerificationToken_setsCompositeIdAndPersists() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);

        repository.save(token);

        assertThat(token.getId()).isEqualTo("foo@bar.com#SIGNUP");
        verify(mongoTemplate).save(token);
    }

    @Test
    void saveEmailChangeToken_setsCompositeIdAndPersists() {
        EmailChangeToken token = new EmailChangeToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.CHANGE_EMAIL);

        repository.save(token);

        assertThat(token.getId()).isEqualTo("foo@bar.com#CHANGE_EMAIL");
        verify(mongoTemplate).save(token);
    }

    @Test
    void savePasswordResetToken_setsCompositeIdAndPersists() {
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);

        repository.save(token);

        assertThat(token.getId()).isEqualTo("foo@bar.com#PASSWORD_RESET");
        verify(mongoTemplate).save(token);
    }

    @Test
    void saveViaBaseType_setsCompositeIdAndPersistsConcreteInstance() {
        VerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);

        repository.save(token);

        assertThat(token.getId()).isEqualTo("foo@bar.com#SIGNUP");
        verify(mongoTemplate).save(token);
    }

    // ---- delete ----

    @Test
    void deleteSignupToken_removesByCompositeId() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.SIGNUP);

        repository.delete((VerificationToken) token);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(captor.capture(), eq(VerificationToken.class));
        assertThat(captor.getValue().getQueryObject().get("_id")).isEqualTo("foo@bar.com#SIGNUP");
    }

    @Test
    void deleteChangeToken_removesByCompositeId() {
        EmailChangeToken token = new EmailChangeToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.CHANGE_EMAIL);

        repository.delete((VerificationToken) token);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(captor.capture(), eq(VerificationToken.class));
        assertThat(captor.getValue().getQueryObject().get("_id")).isEqualTo("foo@bar.com#CHANGE_EMAIL");
    }

    @Test
    void deleteViaBaseType_passwordReset_removesByCompositeId() {
        // Unlike the DynamoDB implementation (where base-type delete silently
        // skipped PASSWORD_RESET), the composite-id delete is uniform across types.
        VerificationToken token = new PasswordResetToken();
        token.setEmail("foo@bar.com");
        token.setType(VerificationType.PASSWORD_RESET);

        repository.delete(token);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(captor.capture(), eq(VerificationToken.class));
        assertThat(captor.getValue().getQueryObject().get("_id")).isEqualTo("foo@bar.com#PASSWORD_RESET");
    }

    // ---- findByEmailAndType ----

    @Test
    void findByEmailAndType_signup_returnsTokenLookedUpByCompositeId() {
        EmailVerificationToken signupTok = new EmailVerificationToken();
        signupTok.setEmail("foo@bar.com");
        signupTok.setType(VerificationType.SIGNUP);
        when(mongoTemplate.findById("foo@bar.com#SIGNUP", VerificationToken.class)).thenReturn(signupTok);

        Optional<VerificationToken> result =
                repository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP);

        assertThat(result).containsSame(signupTok);
        verify(mongoTemplate).findById("foo@bar.com#SIGNUP", VerificationToken.class);
    }

    @Test
    void findByEmailAndType_changeEmail_returnsTokenLookedUpByCompositeId() {
        EmailChangeToken changeTok = new EmailChangeToken();
        changeTok.setEmail("foo@bar.com");
        changeTok.setType(VerificationType.CHANGE_EMAIL);
        when(mongoTemplate.findById("foo@bar.com#CHANGE_EMAIL", VerificationToken.class)).thenReturn(changeTok);

        Optional<VerificationToken> result =
                repository.findByEmailAndType("foo@bar.com", VerificationType.CHANGE_EMAIL);

        assertThat(result).containsSame(changeTok);
        verify(mongoTemplate).findById("foo@bar.com#CHANGE_EMAIL", VerificationToken.class);
    }

    @Test
    void findByEmailAndType_passwordReset_returnsTokenLookedUpByCompositeId() {
        PasswordResetToken resetTok = new PasswordResetToken();
        resetTok.setEmail("foo@bar.com");
        resetTok.setType(VerificationType.PASSWORD_RESET);
        when(mongoTemplate.findById("foo@bar.com#PASSWORD_RESET", VerificationToken.class)).thenReturn(resetTok);

        Optional<VerificationToken> result =
                repository.findByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET);

        assertThat(result).containsSame(resetTok);
        verify(mongoTemplate).findById("foo@bar.com#PASSWORD_RESET", VerificationToken.class);
    }

    @Test
    void findByEmailAndType_missingRow_returnsEmpty() {
        when(mongoTemplate.findById("foo@bar.com#SIGNUP", VerificationToken.class)).thenReturn(null);

        Optional<VerificationToken> result =
                repository.findByEmailAndType("foo@bar.com", VerificationType.SIGNUP);

        assertThat(result).isEmpty();
    }

    // ---- existsByEmailAndType ----

    @Test
    void existsByEmailAndType_returnsTrue_andBuildsCompositeIdQuery() {
        when(mongoTemplate.exists(any(Query.class), eq(VerificationToken.class))).thenReturn(true);

        boolean result = repository.existsByEmailAndType("foo@bar.com", VerificationType.SIGNUP);

        assertThat(result).isTrue();
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).exists(captor.capture(), eq(VerificationToken.class));
        assertThat(captor.getValue().getQueryObject().get("_id")).isEqualTo("foo@bar.com#SIGNUP");
    }

    @Test
    void existsByEmailAndType_returnsFalse_whenAbsent() {
        when(mongoTemplate.exists(any(Query.class), eq(VerificationToken.class))).thenReturn(false);

        boolean result = repository.existsByEmailAndType("foo@bar.com", VerificationType.PASSWORD_RESET);

        assertThat(result).isFalse();
    }
}
