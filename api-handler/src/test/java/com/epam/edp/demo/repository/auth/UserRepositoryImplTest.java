package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.auth.AuthValidationException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryImpl(mongoTemplate);
    }

    private static User user(String id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("hashed")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .build();
    }

    @Test
    void save_savesUserToMongoAndReturnsInputUnchanged() {
        User input = user("user-001", "john@example.com");

        User result = repository.save(input);

        assertThat(result).isSameAs(input);
        verify(mongoTemplate).save(input);
    }

    @Test
    void findById_returnsOptionalWithUser_whenMongoReturnsItem() {
        User found = user("user-001", "john@example.com");
        when(mongoTemplate.findById("user-001", User.class)).thenReturn(found);

        Optional<User> result = repository.findById("user-001");

        assertThat(result).contains(found);
        verify(mongoTemplate).findById("user-001", User.class);
    }

    @Test
    void findById_returnsEmpty_whenMongoReturnsNull() {
        when(mongoTemplate.findById("missing-id", User.class)).thenReturn(null);

        Optional<User> result = repository.findById("missing-id");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_returnsOptionalWithUser_andBuildsEqualityQueryOnEmail() {
        User found = user("user-001", "john@example.com");
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(found);

        Optional<User> result = repository.findByEmail("john@example.com");

        assertThat(result).contains(found);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findOne(captor.capture(), eq(User.class));
        assertThat(captor.getValue().getQueryObject().get("email")).isEqualTo("john@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenMongoReturnsNull() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(null);

        Optional<User> result = repository.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findEmailById_returnsEmail_whenMongoReturnsUser() {
        User found = user("user-001", "john@example.com");
        when(mongoTemplate.findById("user-001", User.class)).thenReturn(found);

        String email = repository.findEmailById("user-001");

        assertThat(email).isEqualTo("john@example.com");
    }

    @Test
    void findEmailById_throwsAuthValidationException_whenMongoReturnsNull() {
        when(mongoTemplate.findById("missing-id", User.class)).thenReturn(null);

        assertThatThrownBy(() -> repository.findEmailById("missing-id"))
                .isInstanceOf(AuthValidationException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAll_returnsAllUsersFromMongo() {
        User a = user("user-001", "a@example.com");
        User b = user("user-002", "b@example.com");
        User c = user("user-003", "c@example.com");
        when(mongoTemplate.findAll(User.class)).thenReturn(List.of(a, b, c));

        List<User> result = repository.getAll();

        assertThat(result).containsExactly(a, b, c);
    }

    @Test
    void existsByEmail_returnsTrue_andBuildsEqualityQueryOnEmail_whenUserExists() {
        when(mongoTemplate.exists(any(Query.class), eq(User.class))).thenReturn(true);

        boolean result = repository.existsByEmail("john@example.com");

        assertThat(result).isTrue();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).exists(captor.capture(), eq(User.class));
        assertThat(captor.getValue().getQueryObject().get("email")).isEqualTo("john@example.com");
    }

    @Test
    void existsByEmail_returnsFalse_whenUserDoesNotExist() {
        when(mongoTemplate.exists(any(Query.class), eq(User.class))).thenReturn(false);

        assertThat(repository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void updateNames_targetsIdAndSetsFirstAndLastName() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(acknowledged(1, 1));

        repository.updateNames("user-001", "Jane", "Roe");

        CapturedUpdate captured = captureUpdate();
        Document query = captured.query.getQueryObject();
        Document set = (Document) captured.update.getUpdateObject().get("$set");

        assertThat(query.get("_id")).isEqualTo("user-001");
        assertThat(set.get("firstName")).isEqualTo("Jane");
        assertThat(set.get("lastName")).isEqualTo("Roe");
    }

    @Test
    void updatePassword_targetsIdAndSetsPassword() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(acknowledged(1, 1));

        repository.updatePassword("user-001", "new-hashed");

        CapturedUpdate captured = captureUpdate();
        Document query = captured.query.getQueryObject();
        Document set = (Document) captured.update.getUpdateObject().get("$set");

        assertThat(query.get("_id")).isEqualTo("user-001");
        assertThat(set.get("password")).isEqualTo("new-hashed");
    }

    @Test
    void updateEmail_targetsIdAndSetsEmail() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(acknowledged(1, 1));

        repository.updateEmail("user-001", "new@example.com");

        CapturedUpdate captured = captureUpdate();
        Document query = captured.query.getQueryObject();
        Document set = (Document) captured.update.getUpdateObject().get("$set");

        assertThat(query.get("_id")).isEqualTo("user-001");
        assertThat(set.get("email")).isEqualTo("new@example.com");
    }

    private CapturedUpdate captureUpdate() {
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(User.class));
        return new CapturedUpdate(queryCaptor.getValue(), updateCaptor.getValue());
    }

    private record CapturedUpdate(Query query, Update update) {
    }

    private static UpdateResult acknowledged(long matchedCount, long modifiedCount) {
        return UpdateResult.acknowledged(matchedCount, modifiedCount, null);
    }
}
