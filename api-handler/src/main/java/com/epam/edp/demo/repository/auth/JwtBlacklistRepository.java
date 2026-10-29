package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.JwtBlacklist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class JwtBlacklistRepository {
    private final MongoTemplate mongoTemplate;

    public JwtBlacklistRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void save(JwtBlacklist entity) {
        log.info("saving token in blacklist");
        mongoTemplate.save(entity);
        log.info("token saved in blacklist");
    }

    public boolean exists(String token) {
        log.info("Checking if token {} exists in blacklist", token);

        Query query = Query.query(Criteria.where("_id").is(token));
        boolean exists = mongoTemplate.exists(query, JwtBlacklist.class);

        log.info("Token exists in blacklist: {}", exists);

        return exists;
    }
}
