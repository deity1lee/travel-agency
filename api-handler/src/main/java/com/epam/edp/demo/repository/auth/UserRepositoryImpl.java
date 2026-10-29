package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.auth.AuthValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final MongoTemplate mongoTemplate;

    public UserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public User save(User user) {
        log.info("Saving user with id {}", user.getId());
        mongoTemplate.save(user);
        log.info("Saved user with id {}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        log.info("Searching for user with id: {}", id);

        User user = mongoTemplate.findById(id, User.class);

        log.info("Found user: {}", user);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Searching for user with email: {}", email);

        Query query = Query.query(Criteria.where("email").is(email));
        User user = mongoTemplate.findOne(query, User.class);

        log.info("Found user : {}", user);
        return Optional.ofNullable(user);
    }

    @Override
    public String findEmailById(String userId) {
        log.info("Finding email by userId: {}", userId);

        User user = mongoTemplate.findById(userId, User.class);

        if (user == null) {
            log.warn("User not found for id: {}", userId);
            throw new AuthValidationException("User not found");
        }

        return user.getEmail();
    }

    @Override
    public List<User> getAll() {
        return mongoTemplate.findAll(User.class);
    }

    @Override
    public boolean existsByEmail(String email) {
        log.info("Checking if user exists with email {}", email);

        Query query = Query.query(Criteria.where("email").is(email));
        return mongoTemplate.exists(query, User.class);
    }

    @Override
    public void updateNames(String userId, String firstName, String lastName) {
        log.info("Updating first/last names for user {} to {} {}", userId, firstName, lastName);

        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update().set("firstName", firstName).set("lastName", lastName);

        mongoTemplate.updateFirst(query, update, User.class);
        log.info("Updated first/last names for user {}", userId);
    }

    @Override
    public void updatePassword(String userId, String newPassword) {
        log.info("Updating password for user {}", userId);

        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update().set("password", newPassword);

        mongoTemplate.updateFirst(query, update, User.class);
        log.info("Updated password for user {}", userId);
    }

    @Override
    public void updateEmail(String userId, String newEmail) {
        log.info("Updating email for user {}", userId);

        Query query = Query.query(Criteria.where("_id").is(userId));
        Update update = new Update().set("email", newEmail);

        mongoTemplate.updateFirst(query, update, User.class);
        log.info("Updated email for user {}", userId);
    }

}
