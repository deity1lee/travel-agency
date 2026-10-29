package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.dto.reports.FeedbackEvent;
import com.epam.edp.demo.exception.EventPublisherException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventPublisherServiceTest {

    private static final String QUEUE = "reports-queue";

    private SqsTemplate sqsTemplate;
    private EventPublisherService service;

    @BeforeEach
    void setUp() {
        sqsTemplate = mock(SqsTemplate.class);
        service = new EventPublisherService(sqsTemplate);
        ReflectionTestUtils.setField(service, "queueName", QUEUE);
    }

    @Test
    void publish_sendsEventToConfiguredQueue() {
        FeedbackEvent event = new FeedbackEvent(
                "2027-01-01T00:00:00Z", 5, 3, "tour-1",
                "agent@x.com", "Agent", "France", "Paris");

        service.publish(event);
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedBody = null;
        try {
            expectedBody = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        verify(sqsTemplate).send(QUEUE, expectedBody);
    }

    @Test
    void publish_wrapsSqsExceptionAsEventPublisherException() {
        FeedbackEvent event = new FeedbackEvent(
                "2027-01-01T00:00:00Z", 5, 3, "tour-1",
                "agent@x.com", "Agent", "France", "Paris");
        String body = null;
        try {
            body = new ObjectMapper().writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        doThrow(new RuntimeException("sqs down"))
                .when(sqsTemplate)
                .send(QUEUE, body);
        assertThatThrownBy(() -> service.publish(event))
                .isInstanceOf(EventPublisherException.class)
                .hasMessage("sqs down");
    }

    @Test
    void publish_successfulCall_returnsWithoutThrowing() {
        FeedbackEvent event = new FeedbackEvent(
                "2027-01-01T00:00:00Z", 5, 3, "tour-1",
                "agent@x.com", "Agent", "France", "Paris");

        // Should not throw
        service.publish(event);
        assertThat(true).isTrue();
    }
}
