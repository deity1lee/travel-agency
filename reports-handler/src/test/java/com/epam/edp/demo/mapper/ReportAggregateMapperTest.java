package com.epam.edp.demo.mapper;

import com.epam.edp.demo.entity.TourWeeklyReportData;
import com.epam.edp.demo.entity.TravelAgentWeeklyReportData;
import com.epam.edp.demo.model.PerformanceAggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportAggregateMapperTest {

    @Test
    void test_toSalesAggregateReportList_mapsAllFieldsAndDeltas_whenPreviousDataExists() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TourWeeklyReportData current = tourWeekly(
                "tour-1",
                "City Tour",
                "Paris, France",
                10,
                4,
                15,
                2,
                BigDecimal.valueOf(500)
        );
        TourWeeklyReportData previous = tourWeekly(
                "tour-1",
                "City Tour",
                "Paris, France",
                5,
                4,
                8,
                1,
                BigDecimal.valueOf(400)
        );

        List<SalesAggregateReport> result = ReportAggregateMapper.toSalesAggregateReportList(
                List.of(current),
                List.of(previous),
                start,
                end
        );

        assertEquals(1, result.size());
        SalesAggregateReport report = result.get(0);

        assertEquals("City Tour", report.getTourName());
        assertEquals("Paris", report.getDestinationCity());
        assertEquals("France", report.getDestinationCountry());
        assertEquals(start, report.getReportStartDate());
        assertEquals(end, report.getReportEndDate());
        assertEquals(10, report.getToursSold());
        assertEquals(0, report.getTourSoldDelta().compareTo(BigDecimal.valueOf(100.0000)));
        assertEquals(0, report.getFeedbackRateAvg().compareTo(BigDecimal.valueOf(3.7500)));
        assertEquals(2, report.getFeedbackRateMin());
        assertEquals(0, report.getFeedbackRateAvgDelta().compareTo(BigDecimal.valueOf(87.5000)));
        assertEquals(0, report.getFinishedToursRevenue().compareTo(BigDecimal.valueOf(500)));
        assertEquals(0, report.getFinishedToursRevenueDelta().compareTo(BigDecimal.valueOf(25.0000)));
    }

    @Test
    void test_toSalesAggregateReportList_setsNullDeltas_whenPreviousDataMissing() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TourWeeklyReportData current = tourWeekly(
                "tour-1",
                "City Tour",
                "Paris, France",
                10,
                4,
                15,
                2,
                BigDecimal.valueOf(500)
        );

        List<SalesAggregateReport> result = ReportAggregateMapper.toSalesAggregateReportList(
                List.of(current),
                List.of(),
                start,
                end
        );

        assertEquals(1, result.size());
        SalesAggregateReport report = result.get(0);
        assertNull(report.getTourSoldDelta());
        assertNull(report.getFeedbackRateAvgDelta());
        assertNull(report.getFinishedToursRevenueDelta());
    }

    @Test
    void test_toSalesAggregateReportList_throwsException_whenPreviousContainsDuplicateTourIds() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TourWeeklyReportData current = tourWeekly("tour-1", "City Tour", "Paris, France", 10, 4, 15, 2, BigDecimal.valueOf(500));
        TourWeeklyReportData previous1 = tourWeekly("tour-1", "City Tour", "Paris, France", 5, 4, 8, 1, BigDecimal.valueOf(400));
        TourWeeklyReportData previous2 = tourWeekly("tour-1", "City Tour", "Paris, France", 4, 2, 6, 1, BigDecimal.valueOf(300));

        assertThrows(
                IllegalStateException.class,
                () -> ReportAggregateMapper.toSalesAggregateReportList(
                        List.of(current),
                        List.of(previous1, previous2),
                        start,
                        end
                )
        );
    }

    @Test
    void test_toPerformanceAggregateList_mapsAllFieldsAndDeltas_whenPreviousDataExists() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TravelAgentWeeklyReportData current = travelAgentWeekly(
                "agent@example.com",
                "Agent One",
                10,
                4,
                15,
                2,
                BigDecimal.valueOf(500)
        );
        TravelAgentWeeklyReportData previous = travelAgentWeekly(
                "agent@example.com",
                "Agent One",
                5,
                4,
                8,
                1,
                BigDecimal.valueOf(400)
        );

        List<PerformanceAggregateReport> result = ReportAggregateMapper.toPerformanceAggregateList(
                List.of(current),
                List.of(previous),
                start,
                end
        );

        assertEquals(1, result.size());
        PerformanceAggregateReport report = result.get(0);

        assertEquals("Agent One", report.getTravelAgentName());
        assertEquals("agent@example.com", report.getTravelAgentEmail());
        assertEquals(start, report.getReportStartDate());
        assertEquals(end, report.getReportEndDate());
        assertEquals(10, report.getToursSold());
        assertEquals(0, report.getTourSoldDelta().compareTo(BigDecimal.valueOf(100.0000)));
        assertEquals(0, report.getFeedbackRateAvg().compareTo(BigDecimal.valueOf(3.7500)));
        assertEquals(2, report.getFeedbackRateMin());
        assertEquals(0, report.getFeedbackRateAvgDelta().compareTo(BigDecimal.valueOf(87.5000)));
        assertEquals(0, report.getFinishedToursRevenue().compareTo(BigDecimal.valueOf(500)));
        assertEquals(0, report.getFinishedToursRevenueDelta().compareTo(BigDecimal.valueOf(25.0000)));
    }

    @Test
    void test_toPerformanceAggregateList_setsNullDeltas_whenPreviousDataMissing() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TravelAgentWeeklyReportData current = travelAgentWeekly(
                "agent@example.com",
                "Agent One",
                10,
                4,
                15,
                2,
                BigDecimal.valueOf(500)
        );

        List<PerformanceAggregateReport> result = ReportAggregateMapper.toPerformanceAggregateList(
                List.of(current),
                List.of(),
                start,
                end
        );

        assertEquals(1, result.size());
        PerformanceAggregateReport report = result.get(0);
        assertNull(report.getTourSoldDelta());
        assertNull(report.getFeedbackRateAvgDelta());
        assertNull(report.getFinishedToursRevenueDelta());
    }

    @Test
    void test_toPerformanceAggregateList_throwsException_whenPreviousContainsDuplicateEmails() {
        LocalDate start = LocalDate.of(2026, 6, 27);
        LocalDate end = LocalDate.of(2026, 7, 4);

        TravelAgentWeeklyReportData current = travelAgentWeekly("agent@example.com", "Agent One", 10, 4, 15, 2, BigDecimal.valueOf(500));
        TravelAgentWeeklyReportData previous1 = travelAgentWeekly("agent@example.com", "Agent One", 5, 4, 8, 1, BigDecimal.valueOf(400));
        TravelAgentWeeklyReportData previous2 = travelAgentWeekly("agent@example.com", "Agent One", 4, 2, 6, 1, BigDecimal.valueOf(300));

        assertThrows(
                IllegalStateException.class,
                () -> ReportAggregateMapper.toPerformanceAggregateList(
                        List.of(current),
                        List.of(previous1, previous2),
                        start,
                        end
                )
        );
    }

    private TourWeeklyReportData tourWeekly(String tourId,
                                            String tourName,
                                            String destination,
                                            int confirmedToursCount,
                                            int feedbackCount,
                                            int feedbackRatingSum,
                                            int minFeedbackRate,
                                            BigDecimal finishedToursRevenue) {
        TourWeeklyReportData data = new TourWeeklyReportData();
        data.setTourId(tourId);
        data.setTourName(tourName);
        data.setDestination(destination);
        data.setConfirmedToursCount(confirmedToursCount);
        data.setFeedbackCount(feedbackCount);
        data.setFeedbackRatingSum(feedbackRatingSum);
        data.setMinFeedbackRate(minFeedbackRate);
        data.setFinishedToursRevenue(finishedToursRevenue);
        assertNotNull(data);
        return data;
    }

    private TravelAgentWeeklyReportData travelAgentWeekly(String travelAgentEmail,
                                                          String travelAgentName,
                                                          int confirmedToursCount,
                                                          int feedbackCount,
                                                          int feedbackRatingSum,
                                                          int minFeedbackRate,
                                                          BigDecimal finishedToursRevenue) {
        TravelAgentWeeklyReportData data = new TravelAgentWeeklyReportData();
        data.setTravelAgentEmail(travelAgentEmail);
        data.setTravelAgentName(travelAgentName);
        data.setConfirmedToursCount(confirmedToursCount);
        data.setFeedbackCount(feedbackCount);
        data.setFeedbackRatingSum(feedbackRatingSum);
        data.setMinFeedbackRate(minFeedbackRate);
        data.setFinishedToursRevenue(finishedToursRevenue);
        assertNotNull(data);
        return data;
    }
}

