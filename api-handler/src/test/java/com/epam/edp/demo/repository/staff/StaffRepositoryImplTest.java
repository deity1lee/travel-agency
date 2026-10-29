package com.epam.edp.demo.repository.staff;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.staff.Staff;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private StaffRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StaffRepositoryImpl(mongoTemplate);
    }

    @Test
    void travelAgentExists_returnsTrue_whenMongoFindsAgent() {
        when(mongoTemplate.exists(any(Query.class), eq(Staff.class))).thenReturn(true);

        assertThat(repository.staffExists("agent@example.com", Role.TRAVEL_AGENT)).isTrue();
    }

    @Test
    void travelAgentExists_returnsFalse_whenMongoDoesNotFindAgent() {
        when(mongoTemplate.exists(any(Query.class), eq(Staff.class))).thenReturn(false);

        assertThat(repository.staffExists("missing@example.com", Role.TRAVEL_AGENT)).isFalse();
    }

    @Test
    void staffExists_queriesByEmail() {
        when(mongoTemplate.exists(any(Query.class), eq(Staff.class))).thenReturn(false);

        repository.staffExists("agent@example.com", Role.TRAVEL_AGENT);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).exists(captor.capture(), eq(Staff.class));
        Document query = captor.getValue().getQueryObject();
        assertThat(query.get("email")).isEqualTo("agent@example.com");
    }

    @Test
    void saveAll_insertsAllAgentsInBatch() {
        Staff a = new Staff("A", "a@x.com", "1", null, Role.TRAVEL_AGENT);
        Staff b = new Staff("B", "b@x.com", "2", null, Role.TRAVEL_AGENT);
        List<Staff> agents = List.of(a, b);

        repository.saveAll(agents);

        verify(mongoTemplate).insertAll(agents);
    }

    @Test
    void saveAll_withEmptyList_doesNotTouchMongo() {
        repository.saveAll(List.of());

        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void getAnyAvailableTravelAgent_returnsAnAgentFromMongoResults() {
        Staff agent = new Staff("Jane", "jane@x.com", "555", "tg", Role.TRAVEL_AGENT);
        when(mongoTemplate.findAll(Staff.class)).thenReturn(List.of(agent));

        Optional<Staff> result = repository.getAnyAvailableTravelAgent();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(agent);
    }

    @Test
    void getAnyAvailableTravelAgent_picksOneOfTheScannedAgents() {
        Staff a = new Staff("A", "a@x.com", "1", null, Role.TRAVEL_AGENT);
        Staff b = new Staff("B", "b@x.com", "2", null, Role.TRAVEL_AGENT);
        Staff c = new Staff("C", "c@x.com", "3", null, Role.TRAVEL_AGENT);
        when(mongoTemplate.findAll(Staff.class)).thenReturn(List.of(a, b, c));

        Optional<Staff> result = repository.getAnyAvailableTravelAgent();

        assertThat(result).isPresent();
        assertThat(result.get()).isIn(a, b, c);
    }

    @Test
    void getAnyAvailableTravelAgent_emptyResult_throwsBecauseRandomBoundIsZero() {
        when(mongoTemplate.findAll(Staff.class)).thenReturn(List.of());

        assertThatThrownBy(() -> repository.getAnyAvailableTravelAgent())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
