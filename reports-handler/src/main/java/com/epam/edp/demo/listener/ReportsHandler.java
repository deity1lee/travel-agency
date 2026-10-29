package com.epam.edp.demo.listener;

import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.FeedbackEvent;
import com.epam.edp.demo.exception.SqsProcessingException;
import com.epam.edp.demo.service.ReportServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportsHandler {

    private final ObjectMapper objectMapper;
    private final ReportServiceImpl reportServiceImpl;

    @SqsListener("${booking-events.queue-name}")
    public void handleEvent(String body) {

        log.info("Received SQS event with body: {}", body);
        try {
            JsonNode json = objectMapper.readTree(body);

            if (json.has("bookingStatus")) {
                BookingStatusChangedEvent dto =
                        objectMapper.treeToValue(json, BookingStatusChangedEvent.class);

                log.info("state of booking with id {} was changed", dto.bookingId());
                reportServiceImpl.aggregateBookingReportData(dto);

            } else if (json.has("newRate")) {

                log.info("Received feedback event for booking with rate {}", json.get("newRate"));
                FeedbackEvent dto =
                        objectMapper.treeToValue(json, FeedbackEvent.class);

                reportServiceImpl.aggregateReviewReportData(dto);

            } else {
                log.warn("Unknown event {}", body);
            }

        } catch (JsonProcessingException e) {
            log.error("Unrecoverable message format error, discarding: {}", body, e);
        } catch (Exception e) {
            log.error("Failed to process message. Will retry", e);
            throw new SqsProcessingException("Transient failure", e);
        }
    }
}
