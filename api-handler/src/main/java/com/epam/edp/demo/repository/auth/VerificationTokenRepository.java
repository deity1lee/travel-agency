package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
public class VerificationTokenRepository implements TokenRepository {

    private final MongoTemplate mongoTemplate;

    public VerificationTokenRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(EmailChangeToken token) {
        saveToken(token);
    }

    @Override
    public void save(EmailVerificationToken token) {
        saveToken(token);
    }

    @Override
    public void save(PasswordResetToken token) {
        saveToken(token);
    }

    @Override
    public void save(VerificationToken token) {
        saveToken(token);
    }

    private void saveToken(VerificationToken token) {
        String id = compositeId(token.getEmail(), token.getType());
        log.info("Saving {} token for user {}", token.getType(), token.getEmail());
        token.setId(id);
        mongoTemplate.save(token);
        log.info("Token of type {} saved for user: {}", token.getType(), token.getEmail());
    }

    @Override
    public void delete(EmailChangeToken token) {
        deleteToken(token);
    }

    @Override
    public void delete(EmailVerificationToken token) {
        deleteToken(token);
    }

    @Override
    public void delete(VerificationToken token) {
        deleteToken(token);
    }

    private void deleteToken(VerificationToken token) {
        log.info("Deleting {} token for user {}", token.getType(), token.getEmail());
        Query query = Query.query(Criteria.where("_id").is(compositeId(token.getEmail(), token.getType())));
        mongoTemplate.remove(query, VerificationToken.class);
        log.info("Token of type {} deleted for user: {}", token.getType(), token.getEmail());
    }

    @Override
    public Optional<VerificationToken> findByEmailAndType(String email, VerificationType type) {
        log.info("Finding {} token for user {}", type, email);
        VerificationToken token = mongoTemplate.findById(compositeId(email, type), VerificationToken.class);
        log.info("token for user {}: {}", email, token != null ? "found" : "not found");
        return Optional.ofNullable(token);
    }

    @Override
    public boolean existsByEmailAndType(String email, VerificationType type) {
        Query query = Query.query(Criteria.where("_id").is(compositeId(email, type)));
        return mongoTemplate.exists(query, VerificationToken.class);
    }

    private static String compositeId(String email, VerificationType type) {
        return email + "#" + type.name();
    }
}
