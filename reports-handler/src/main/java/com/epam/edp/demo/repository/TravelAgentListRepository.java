package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.travelagent.TravelAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TravelAgentListRepository {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public TravelAgentListRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<String> getAllTravelAgentEmails() {
        log.info("Fetching all travel agent emails");
        Query query = new Query();
        query.fields()
                .include("email");
        return mongoTemplate
                .find(
                        query,
                        TravelAgent.class,
                        "travel_agents"
                )
                .stream()
                .map(TravelAgent::getEmail)
                .collect(Collectors.toList());
    }
}
