package com.epam.edp.demo.dto.reports;

public record FeedbackEvent(
        String createdAt, // Instant
        Integer newRate,
        Integer oldRate,
        String tourId,

        String travelAgentEmail,
        String travelAgentName,
        String destination,
        String tourName
) {
}