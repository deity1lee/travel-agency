package com.epam.edp.demo.listener;

import com.epam.edp.demo.dto.FeedbackEvent;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.entity.BookingStatus;
import com.epam.edp.demo.service.ReportServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ReportServiceImpl reportServiceImpl;

    @Mock
    private JsonNode jsonNode;

    @InjectMocks
    private ReportsHandler reportsHandler;

    @Test
    void test_handleEvent_aggregatesBookingData_whenBookingStatusFieldExists() throws Exception {
        String body = "{\"bookingStatus\":\"CONFIRMED\"}";
        BookingStatusChangedEvent event = BookingStatusChangedEvent.builder()
                .bookingId("b-1")
                .bookingStatus(BookingStatus.CONFIRMED)
                .price(100)
                .cancelCharge(0.0)
                .build();

        when(objectMapper.readTree(body)).thenReturn(jsonNode);
        when(jsonNode.has("bookingStatus")).thenReturn(true);
        when(objectMapper.treeToValue(jsonNode, BookingStatusChangedEvent.class)).thenReturn(event);

        reportsHandler.handleEvent(body);

        verify(reportServiceImpl).aggregateBookingReportData(event);
        verify(reportServiceImpl, never()).aggregateReviewReportData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void test_handleEvent_aggregatesReviewData_whenNewRateFieldExists() throws Exception {
        String body = "{\"newRate\":5}";
        FeedbackEvent event = new FeedbackEvent(
                "2026-07-04T00:00:00Z",
                5,
                null,
                "tour-1",
                "agent@example.com",
                "Agent Name",
                "Paris, France",
                "Tour Name"
        );

        when(objectMapper.readTree(body)).thenReturn(jsonNode);
        when(jsonNode.has("bookingStatus")).thenReturn(false);
        when(jsonNode.has("newRate")).thenReturn(true);
        when(jsonNode.get("newRate")).thenReturn(jsonNode);
        when(objectMapper.treeToValue(jsonNode, FeedbackEvent.class)).thenReturn(event);

        reportsHandler.handleEvent(body);

        verify(reportServiceImpl).aggregateReviewReportData(event);
        verify(reportServiceImpl, never()).aggregateBookingReportData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void test_handleEvent_doesNotAggregate_whenEventTypeIsUnknown() throws Exception {
        String body = "{\"foo\":\"bar\"}";

        when(objectMapper.readTree(body)).thenReturn(jsonNode);
        when(jsonNode.has("bookingStatus")).thenReturn(false);
        when(jsonNode.has("newRate")).thenReturn(false);

        reportsHandler.handleEvent(body);

        verify(reportServiceImpl, never()).aggregateBookingReportData(org.mockito.ArgumentMatchers.any());
        verify(reportServiceImpl, never()).aggregateReviewReportData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void test_handleEvent_throwsRuntimeException_whenReadTreeFails() throws Exception {
        String body = "not-json";

        when(objectMapper.readTree(body)).thenThrow(new IllegalArgumentException("invalid payload"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reportsHandler.handleEvent(body));

        assertEquals("invalid payload", ex.getCause().getMessage());
        verify(reportServiceImpl, never()).aggregateBookingReportData(org.mockito.ArgumentMatchers.any());
        verify(reportServiceImpl, never()).aggregateReviewReportData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void test_handleEvent_throwsRuntimeException_whenBookingEventMappingFails() throws Exception {
        String body = "{\"bookingStatus\":\"CONFIRMED\"}";

        when(objectMapper.readTree(body)).thenReturn(jsonNode);
        when(jsonNode.has("bookingStatus")).thenReturn(true);
        when(objectMapper.treeToValue(jsonNode, BookingStatusChangedEvent.class))
                .thenThrow(new IllegalStateException("mapping failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> reportsHandler.handleEvent(body));

        assertEquals("mapping failed", ex.getCause().getMessage());
        verify(reportServiceImpl, never()).aggregateBookingReportData(org.mockito.ArgumentMatchers.any());
        verify(reportServiceImpl, never()).aggregateReviewReportData(org.mockito.ArgumentMatchers.any());
    }
}

