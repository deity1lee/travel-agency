package com.epam.edp.demo.mapper;

import com.epam.edp.demo.entity.TourDailyReportData;
import com.epam.edp.demo.entity.TourWeeklyReportData;
import com.epam.edp.demo.entity.TravelAgentDailyReportData;
import com.epam.edp.demo.entity.TravelAgentWeeklyReportData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyReportMapperTest {

    @Test
    void test_toTourWeeklyReport_mapsFieldsAndKeys_whenDailyReportIsValid() {
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        TourDailyReportData daily = new TourDailyReportData();
        daily.setTourId("tour-1");
        daily.setTourName("City Tour");
        daily.setDestination("Paris, France");
        daily.setFinishedToursCount(3);
        daily.setConfirmedToursCount(10);
        daily.setCancelledToursCount(1);
        daily.setFinishedToursRevenue(BigDecimal.valueOf(1000));
        daily.setConfirmedToursRevenue(BigDecimal.valueOf(1200));
        daily.setCancelledToursRevenue(BigDecimal.valueOf(100));
        daily.setCancelledToursLoss(BigDecimal.valueOf(50));
        daily.setFeedbackCount(4);
        daily.setFeedbackRatingSum(15);
        daily.setMinFeedbackRate(2);

        TourWeeklyReportData result = WeeklyReportMapper.toTourWeeklyReport(daily, endDate);

        assertEquals("TOUR#tour-1", result.getPk());
        assertEquals("WEEKLY#2026-07-04", result.getSk());
        assertEquals("tour-1", result.getTourId());
        assertEquals("City Tour", result.getTourName());
        assertEquals("Paris, France", result.getDestination());
        assertEquals(10, result.getConfirmedToursCount());
        assertEquals("WEEKLY#TOUR", result.getTourDaily());
        assertEquals("2026-07-04#tour-1", result.getTourReportSk());
    }

    @Test
    void test_toTravelAgentWeeklyReport_mapsFieldsAndKeys_whenDailyReportIsValid() {
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        TravelAgentDailyReportData daily = new TravelAgentDailyReportData();
        daily.setTravelAgentName("Agent One");
        daily.setTravelAgentEmail("agent@example.com");
        daily.setFinishedToursCount(3);
        daily.setConfirmedToursCount(10);
        daily.setCancelledToursCount(1);
        daily.setFinishedToursRevenue(BigDecimal.valueOf(1000));
        daily.setConfirmedToursRevenue(BigDecimal.valueOf(1200));
        daily.setCancelledToursRevenue(BigDecimal.valueOf(100));
        daily.setCancelledToursLoss(BigDecimal.valueOf(50));
        daily.setFeedbackCount(4);
        daily.setFeedbackRatingSum(15);
        daily.setMinFeedbackRate(2);

        TravelAgentWeeklyReportData result = WeeklyReportMapper.toTravelAgentWeeklyReport(daily, endDate);

        assertEquals("AGENT#agent@example.com", result.getPk());
        assertEquals("WEEKLY#2026-07-04", result.getSk());
        assertEquals("Agent One", result.getTravelAgentName());
        assertEquals("agent@example.com", result.getTravelAgentEmail());
        assertEquals(10, result.getConfirmedToursCount());
        assertEquals("WEEKLY#AGENT", result.getTourDaily());
        assertEquals("2026-07-04#agent@example.com", result.getTourReportSk());
    }

    @Test
    void test_toTourWeeklyReportList_mapsAllItems_whenDailyListContainsMultipleItems() {
        LocalDate endDate = LocalDate.of(2026, 7, 4);

        TourDailyReportData first = new TourDailyReportData();
        first.setTourId("tour-1");

        TourDailyReportData second = new TourDailyReportData();
        second.setTourId("tour-2");

        List<TourWeeklyReportData> result = WeeklyReportMapper.toTourWeeklyReportList(List.of(first, second), endDate);

        assertEquals(2, result.size());
        assertEquals("tour-1", result.get(0).getTourId());
        assertEquals("tour-2", result.get(1).getTourId());
    }

    @Test
    void test_toTravelAgentWeeklyReportList_mapsAllItems_whenDailyListContainsMultipleItems() {
        LocalDate endDate = LocalDate.of(2026, 7, 4);

        TravelAgentDailyReportData first = new TravelAgentDailyReportData();
        first.setTravelAgentEmail("first@example.com");

        TravelAgentDailyReportData second = new TravelAgentDailyReportData();
        second.setTravelAgentEmail("second@example.com");

        List<TravelAgentWeeklyReportData> result = WeeklyReportMapper.toTravelAgentWeeklyReportList(List.of(first, second), endDate);

        assertEquals(2, result.size());
        assertEquals("first@example.com", result.get(0).getTravelAgentEmail());
        assertEquals("second@example.com", result.get(1).getTravelAgentEmail());
    }

    @Test
    void test_toTourWeeklyReport_throwsException_whenInputDailyReportIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> WeeklyReportMapper.toTourWeeklyReport(null, LocalDate.of(2026, 7, 4))
        );
    }

    @Test
    void test_toTravelAgentWeeklyReport_throwsException_whenInputDailyReportIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> WeeklyReportMapper.toTravelAgentWeeklyReport(null, LocalDate.of(2026, 7, 4))
        );
    }
}

