package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.JwtBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private JwtBlacklistRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JwtBlacklistRepository(mongoTemplate);
    }

    @Test
    void save_persistsEntityToMongo() {
        JwtBlacklist entity = JwtBlacklist.builder()
                .token("abc.def.ghi")
                .expiresAt(Instant.parse("2026-12-31T00:00:00Z"))
                .build();

        repository.save(entity);

        verify(mongoTemplate).save(entity);
    }

    @Test
    void exists_returnsTrue_andBuildsIdQuery_whenMongoReportsExisting() {
        when(mongoTemplate.exists(any(Query.class), eq(JwtBlacklist.class))).thenReturn(true);

        boolean result = repository.exists("abc.def.ghi");

        assertThat(result).isTrue();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).exists(captor.capture(), eq(JwtBlacklist.class));
        assertThat(captor.getValue().getQueryObject().get("_id")).isEqualTo("abc.def.ghi");
    }

    @Test
    void exists_returnsFalse_whenMongoReportsAbsent() {
        when(mongoTemplate.exists(any(Query.class), eq(JwtBlacklist.class))).thenReturn(false);

        boolean result = repository.exists("nonexistent-token");

        assertThat(result).isFalse();
    }
}
