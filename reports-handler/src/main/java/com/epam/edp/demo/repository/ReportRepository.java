package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportRepository {
    List<TourDailyReportData> getTourDailyReportList(LocalDate start,
                                                     LocalDate end);

    List<TravelAgentDailyReportData> getTravelAgentDailyReportList(LocalDate start,
                                                                   LocalDate end);

    List<TourWeeklyReportData> getTourWeeklyReports(LocalDate reportDate);

    List<TravelAgentWeeklyReportData> getTravelAgentWeeklyReports(LocalDate reportDate);

    void saveWeeklyTourReports(List<TourWeeklyReportData> reports);

    void saveWeeklyTaReports(List<TravelAgentWeeklyReportData> reports);


    void updateTourReportByState(LocalDate now,
                                 String pk,
                                 String sk,
                                 BookingStatus state,
                                 BigDecimal revenue,
                                 BigDecimal canceledLoss,
                                 String tourId,
                                 String destination,
                                 String tourName);

    void updateTravelAgentReportByState(LocalDate now,
                                        String pk,
                                        String sk,
                                        BookingStatus state,
                                        BigDecimal revenue,
                                        BigDecimal canceledLoss,
                                        String travelAgentName,
                                        String travelAgentEmail);

    void updateTourReportReview(LocalDate now,
                                String pk,
                                String sk,
                                int rateDiff,
                                int newRate,
                                boolean isNew,
                                String tourId,
                                String destination,
                                String tourName);

    void updateTravelAgentReportReview(LocalDate now,
                                       String pk,
                                       String sk,
                                       int rateDiff,
                                       int newRate,
                                       boolean isNew,
                                       String travelAgentName,
                                       String travelAgentEmail);
}
