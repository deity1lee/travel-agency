package com.epam.edp.demo.mapper;

import com.epam.edp.demo.entity.TourWeeklyReportData;
import com.epam.edp.demo.entity.TravelAgentWeeklyReportData;
import com.epam.edp.demo.model.PerformanceAggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import com.epam.edp.demo.util.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportAggregateMapper {

    private ReportAggregateMapper() {}

    public static List<SalesAggregateReport> toSalesAggregateReportList(List<TourWeeklyReportData> tourWeeklyReports,
                                                                        List<TourWeeklyReportData> previousTourWeeklyReports,
                                                                        LocalDate reportStartDate,
                                                                        LocalDate reportEndDate
    ) {
        Map<String, TourWeeklyReportData> previousByTourId =
                previousTourWeeklyReports.stream()
                        .collect(Collectors.toMap(
                                TourWeeklyReportData::getTourId,
                                Function.identity())
                        );

        return tourWeeklyReports.stream()
                .map(current -> toSalesStatisticsReport(
                        current,
                        previousByTourId.get(current.getTourId()),
                        reportStartDate,
                        reportEndDate)
                )
                .collect(Collectors.toList());
    }

    private static SalesAggregateReport toSalesStatisticsReport(TourWeeklyReportData tourWeeklyReport,
                                                                TourWeeklyReportData previousWeeklyReport,
                                                                LocalDate reportStartDate,
                                                                LocalDate reportEndDate) {
        String[] cityCountry = Utils.getCityCountry(tourWeeklyReport.getDestination());

        BigDecimal feedbackRateAverage = tourWeeklyReport.getFeedbackCount() != 0 ? BigDecimal.valueOf(tourWeeklyReport.getFeedbackRatingSum())
                                                                                    .divide(BigDecimal.valueOf(tourWeeklyReport.getFeedbackCount()), 4, RoundingMode.HALF_UP) : null;

        BigDecimal prevFeedbackRateAverage = previousWeeklyReport != null && previousWeeklyReport.getFeedbackCount() != 0 ? BigDecimal.valueOf(previousWeeklyReport.getFeedbackRatingSum())
                                                                            .divide(BigDecimal.valueOf(previousWeeklyReport.getFeedbackCount()), 4, RoundingMode.HALF_UP) : null;

        BigDecimal prevFinishedToursRevenue = previousWeeklyReport != null ? previousWeeklyReport.getFinishedToursRevenue() : null;

        Integer prevConfirmedToursCount = previousWeeklyReport != null ? previousWeeklyReport.getConfirmedToursCount() : null;

        SalesAggregateReport report = new SalesAggregateReport();
        report.setTourName(tourWeeklyReport.getTourName());
        report.setDestinationCity(cityCountry[0]);
        report.setDestinationCountry(cityCountry[1]);

        report.setReportStartDate(reportStartDate);
        report.setReportEndDate(reportEndDate);
        report.setToursSold(tourWeeklyReport.getConfirmedToursCount());  // tours sold... (maybe should be "booked")

        BigDecimal tourSoldDelta = Utils.delta(tourWeeklyReport.getConfirmedToursCount(), prevConfirmedToursCount);
        report.setTourSoldDelta(tourSoldDelta);

        report.setFeedbackRateAvg(feedbackRateAverage);
        report.setFeedbackRateMin(tourWeeklyReport.getMinFeedbackRate());

        BigDecimal feedbackRateAvgDelta = Utils.delta(feedbackRateAverage, prevFeedbackRateAverage);
        report.setFeedbackRateAvgDelta(feedbackRateAvgDelta);

        report.setFinishedToursRevenue(tourWeeklyReport.getFinishedToursRevenue());

        BigDecimal finishedToursRevenueDelta = Utils.delta(tourWeeklyReport.getFinishedToursRevenue(), prevFinishedToursRevenue);
        report.setFinishedToursRevenueDelta(finishedToursRevenueDelta);

        return report;
    }

    public static List<PerformanceAggregateReport> toPerformanceAggregateList(List<TravelAgentWeeklyReportData> travelAgentWeeklyReports,
                                                                              List<TravelAgentWeeklyReportData> previousTravelAgentWeeklyReports,
                                                                              LocalDate reportStartDate,
                                                                              LocalDate reportEndDate
    ) {
        Map<String, TravelAgentWeeklyReportData> previousByTourId =
                previousTravelAgentWeeklyReports.stream()
                        .collect(Collectors.toMap(
                                TravelAgentWeeklyReportData::getTravelAgentEmail,
                                Function.identity())
                        );

        return travelAgentWeeklyReports.stream()
                .map(current -> toPerformanceAggregate(
                        current,
                        previousByTourId.get(current.getTravelAgentEmail()),
                        reportStartDate,
                        reportEndDate)
                )
                .collect(Collectors.toList());
    }

    private static PerformanceAggregateReport toPerformanceAggregate(TravelAgentWeeklyReportData travelAgentWeeklyReport,
                                                                     TravelAgentWeeklyReportData previousWeeklyReport,
                                                                     LocalDate reportStartDate,
                                                                     LocalDate reportEndDate
    ) {

        BigDecimal feedbackRateAverage = travelAgentWeeklyReport.getFeedbackCount() != 0 ? BigDecimal.valueOf(travelAgentWeeklyReport.getFeedbackRatingSum())
                                                                                           .divide(BigDecimal.valueOf(travelAgentWeeklyReport.getFeedbackCount()), 4, RoundingMode.HALF_UP) : null;

        BigDecimal prevFeedbackRateAverage = previousWeeklyReport != null && previousWeeklyReport.getFeedbackCount() != 0 ? BigDecimal.valueOf(previousWeeklyReport.getFeedbackRatingSum())
                                                                            .divide(BigDecimal.valueOf(previousWeeklyReport.getFeedbackCount()), 4, RoundingMode.HALF_UP) : null;

        BigDecimal prevFinishedToursRevenue = previousWeeklyReport != null ? previousWeeklyReport.getFinishedToursRevenue() : null;

        Integer prevConfirmedToursCount = previousWeeklyReport != null ? previousWeeklyReport.getConfirmedToursCount() : null;

        PerformanceAggregateReport report = new PerformanceAggregateReport();
        report.setTravelAgentName(travelAgentWeeklyReport.getTravelAgentName());
        report.setTravelAgentEmail(travelAgentWeeklyReport.getTravelAgentEmail());

        report.setReportStartDate(reportStartDate);
        report.setReportEndDate(reportEndDate);
        report.setToursSold(travelAgentWeeklyReport.getConfirmedToursCount());  // tours sold... (maybe should be "booked")

        BigDecimal tourSoldDelta = Utils.delta(travelAgentWeeklyReport.getConfirmedToursCount(), prevConfirmedToursCount);
        report.setTourSoldDelta(tourSoldDelta);

        report.setFeedbackRateAvg(feedbackRateAverage);
        report.setFeedbackRateMin(travelAgentWeeklyReport.getMinFeedbackRate());

        BigDecimal feedbackRateAvgDelta = Utils.delta(feedbackRateAverage, prevFeedbackRateAverage);
        report.setFeedbackRateAvgDelta(feedbackRateAvgDelta);

        report.setFinishedToursRevenue(travelAgentWeeklyReport.getFinishedToursRevenue());

        BigDecimal finishedToursRevenueDelta = Utils.delta(travelAgentWeeklyReport.getFinishedToursRevenue(), prevFinishedToursRevenue);
        report.setFinishedToursRevenueDelta(finishedToursRevenueDelta);

        return report;
    }

}
