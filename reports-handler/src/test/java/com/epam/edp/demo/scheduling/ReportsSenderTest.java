package com.epam.edp.demo.scheduling;

import com.epam.edp.demo.service.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsSenderTest {

    @Mock
    private ReportServiceImpl reportServiceImpl;

    @Mock
    private Clock clock;

    @InjectMocks
    private ReportsSender reportsSender;

    @Test
    void test_generateWeeklyReports_callsBothWeeklyReportMethods_withExpectedDateRange() {
        when(clock.instant()).thenReturn(Instant.parse("2026-07-04T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        reportsSender.generateWeeklyReports();

        LocalDate endDate = LocalDate.of(2026, 7, 4);
        LocalDate startDate = LocalDate.of(2026, 6, 27);

        verify(reportServiceImpl).sendWeeklyTourPerformanceReport(startDate, endDate);
        verify(reportServiceImpl).sendWeeklyTravelAgentPerformanceReport(startDate, endDate);
    }

    @Test
    void test_generateWeeklyReports_propagatesException_andSkipsSecondCall_whenTourReportFails() {
        when(clock.instant()).thenReturn(Instant.parse("2026-07-04T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        LocalDate endDate = LocalDate.of(2026, 7, 4);
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        doThrow(new IllegalStateException("tour report failed"))
                .when(reportServiceImpl)
                .sendWeeklyTourPerformanceReport(startDate, endDate);

        IllegalStateException ex = assertThrows(IllegalStateException.class, reportsSender::generateWeeklyReports);

        assertEquals("tour report failed", ex.getMessage());
        verify(reportServiceImpl).sendWeeklyTourPerformanceReport(startDate, endDate);
        verify(reportServiceImpl, never()).sendWeeklyTravelAgentPerformanceReport(any(), any());
    }

    @Test
    void test_generateWeeklyReports_propagatesException_afterFirstCall_whenTravelAgentReportFails() {
        when(clock.instant()).thenReturn(Instant.parse("2026-07-04T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        LocalDate endDate = LocalDate.of(2026, 7, 4);
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        doThrow(new RuntimeException("travel agent report failed"))
                .when(reportServiceImpl)
                .sendWeeklyTravelAgentPerformanceReport(startDate, endDate);

        RuntimeException ex = assertThrows(RuntimeException.class, reportsSender::generateWeeklyReports);

        assertEquals("travel agent report failed", ex.getMessage());
        verify(reportServiceImpl).sendWeeklyTourPerformanceReport(startDate, endDate);
        verify(reportServiceImpl).sendWeeklyTravelAgentPerformanceReport(startDate, endDate);
    }
}

