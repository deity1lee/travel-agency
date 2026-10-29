package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.travelagent.TravelAgent;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelAgentListRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private TravelAgentListRepository travelAgentListRepository;

    @BeforeEach
    void setUp() {
        travelAgentListRepository = new TravelAgentListRepository(mongoTemplate);
    }

    @Test
    void getAllTravelAgentEmails_returnsEmails_whenCollectionHasItems() {
        when(mongoTemplate.find(any(Query.class), eq(TravelAgent.class), eq("travel_agents")))
                .thenReturn(List.of(
                        travelAgent("agent1@example.com"),
                        travelAgent("agent2@example.com")
                ));

        List<String> emails = travelAgentListRepository.getAllTravelAgentEmails();

        assertIterableEquals(List.of("agent1@example.com", "agent2@example.com"), emails);
    }

    @Test
    void getAllTravelAgentEmails_returnsEmptyList_whenCollectionIsEmpty() {
        when(mongoTemplate.find(any(Query.class), eq(TravelAgent.class), eq("travel_agents")))
                .thenReturn(List.of());

        List<String> emails = travelAgentListRepository.getAllTravelAgentEmails();

        assertTrue(emails.isEmpty());
    }

    @Test
    void getAllTravelAgentEmails_queriesTravelAgentsCollection_andProjectsOnlyEmail() {
        when(mongoTemplate.find(any(Query.class), eq(TravelAgent.class), eq("travel_agents")))
                .thenReturn(List.of());

        travelAgentListRepository.getAllTravelAgentEmails();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(TravelAgent.class), eq("travel_agents"));

        Query query = captor.getValue();
        Document fields = query.getFieldsObject();
        assertEquals(1, fields.get("email"));
        assertTrue(query.getQueryObject().isEmpty());
    }

    private static TravelAgent travelAgent(String email) {
        return new TravelAgent("Agent", email, "555", "tg");
    }
}

