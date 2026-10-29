package com.epam.edp.demo.service.reports;

import com.epam.edp.demo.exception.EventPublisherException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class EventPublisherService {

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisherService(SqsTemplate sqsTemplate) {
        this.objectMapper = new ObjectMapper();
        this.sqsTemplate = sqsTemplate;
    }

    public void publish(Object event) {
        log.info("sending event of type {} to queue", event.getClass());
        try {
            String body = objectMapper.writeValueAsString(event);
            log.info("msg's body: {}", body);
            sqsTemplate.send(queueName, body);
        } catch (Exception e) {
            log.warn("Caught exception while publishing sqs event", e);
            throw new EventPublisherException(e.getMessage());
        }
    }
}
