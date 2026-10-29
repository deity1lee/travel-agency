package com.epam.edp.demo.mapper;

import com.epam.edp.demo.entity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class WeeklyReportMapper {

    private WeeklyReportMapper() {}

    public static List<TourWeeklyReportData> toTourWeeklyReportList(List<TourDailyReportData> dailyReports, LocalDate repotEndDate) {
        return dailyReports.stream()
                .map(t -> toTourWeeklyReport(t, repotEndDate))
                .collect(Collectors.toList());
    }

    public static TourWeeklyReportData toTourWeeklyReport(TourDailyReportData d, LocalDate reportEndDate) {
        TourWeeklyReportData w = new TourWeeklyReportData();

        w.setPk(TourWeeklyReportData.pk(d.getTourId()));
        w.setSk(TourWeeklyReportData.sk(reportEndDate));

        w.setTourId(d.getTourId());
        w.setTourName(d.getTourName());
        w.setDestination(d.getDestination());

        setCommonFields(w, d);

        w.setTourDaily(TourWeeklyReportData.gsiPk());
        w.setTourReportSk(TourWeeklyReportData.gsiSk(reportEndDate, d.getTourId()));

        return w;
    }

    public static List<TravelAgentWeeklyReportData> toTravelAgentWeeklyReportList(List<TravelAgentDailyReportData> dailyReports, LocalDate reportEndDate) {
        return dailyReports.stream()
                .map(t -> toTravelAgentWeeklyReport(t, reportEndDate))
                .collect(Collectors.toList());
    }

    public static TravelAgentWeeklyReportData toTravelAgentWeeklyReport(TravelAgentDailyReportData d, LocalDate reportEndDate) {
        TravelAgentWeeklyReportData w = new TravelAgentWeeklyReportData();

        w.setPk(TravelAgentWeeklyReportData.pk(d.getTravelAgentEmail()));
        w.setSk(TravelAgentWeeklyReportData.sk(reportEndDate));

        w.setTravelAgentEmail(d.getTravelAgentEmail());
        w.setTravelAgentName(d.getTravelAgentName());

        setCommonFields(w, d);

        w.setTourDaily(TravelAgentWeeklyReportData.gsiPk());
        w.setTourReportSk(TravelAgentWeeklyReportData.gsiSk(reportEndDate, d.getTravelAgentEmail()));

        return w;
    }

    private static void setCommonFields(ReportData w, ReportData d) {
        w.setFinishedToursCount(d.getFinishedToursCount());
        w.setConfirmedToursCount(d.getConfirmedToursCount());
        w.setCancelledToursCount(d.getCancelledToursCount());

        w.setFinishedToursRevenue(d.getFinishedToursRevenue());
        w.setConfirmedToursRevenue(d.getConfirmedToursRevenue());
        w.setCancelledToursRevenue(d.getCancelledToursRevenue());

        w.setCancelledToursLoss(d.getCancelledToursLoss());

        w.setFeedbackCount(d.getFeedbackCount());
        w.setFeedbackRatingSum(d.getFeedbackRatingSum());
        w.setMinFeedbackRate(d.getMinFeedbackRate());
    }
}
