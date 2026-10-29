package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.*;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.entity.*;
import com.epam.edp.demo.exception.UnsupportedFileFormatException;
import com.epam.edp.demo.exception.UnsupportedReportTypeException;
import com.epam.edp.demo.export.PerformanceReportColumnOrder;
import com.epam.edp.demo.export.SalesReportColumnOrder;
import com.epam.edp.demo.mapper.ReportAggregateCsvMapper;
import com.epam.edp.demo.mapper.ReportAggregatePdfMapper;
import com.epam.edp.demo.model.AggregateReport;
import com.epam.edp.demo.model.PerformanceAggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import com.epam.edp.demo.mapper.ReportAggregateMapper;
import com.epam.edp.demo.repository.ReportRepository;
import com.epam.edp.demo.repository.TravelAgentListRepository;
import com.epam.edp.demo.export.SalesStatisticsReportCsv;
import com.epam.edp.demo.export.TravelAgentPerformanceReportCsv;
import com.epam.edp.demo.util.Utils;
import com.epam.edp.demo.mapper.WeeklyReportMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportsRepository;
    private final S3ReportStorageService s3ReportStorageService;
    private final SesEmailService sesEmailService;
    private final TravelAgentListRepository travelAgentListRepository;

    public ReportServiceImpl(ReportRepository reportsRepository,
                             S3ReportStorageService s3ReportStorageService,
                             SesEmailService sesEmailService, TravelAgentListRepository travelAgentListRepository) {
        this.reportsRepository = reportsRepository;
        this.s3ReportStorageService = s3ReportStorageService;
        this.sesEmailService = sesEmailService;
        this.travelAgentListRepository = travelAgentListRepository;
    }

    @Override // Location is considered only for tour report
    public List<AggregateReport> getAggregatePerformanceReport(ReportRequest reportRequest) {
        List<AggregateReport> report = new ArrayList<>();
        log.info("Getting reports of type {} from {} to {}.",
                reportRequest.reportType(),
                reportRequest.startDate(),
                reportRequest.endDate());

        if (reportRequest.reportType() == ReportType.SALES) {
            report.addAll(aggregateTourReportData(reportRequest.startDate(), reportRequest.endDate()));
            log.info("report count: {}", report.size());
            if (reportRequest.location() != null) {
                log.info("Filtering by location {}", reportRequest.location());
                report = report.stream()
                        .filter(item -> {
                            if (!(item instanceof SalesAggregateReport child)) {
                                return false;
                            }

                            String[] cityCountry = Utils.getCityCountry(reportRequest.location());

                            return child.getDestinationCity().equals(cityCountry[0]) && child.getDestinationCountry().equals(cityCountry[1]);
                        })
                        .toList();
                log.info("report count after filtering location {} : {}", reportRequest.location(), report.size());
            }
        } else if (reportRequest.reportType() == ReportType.STAFF_PERFORMANCE) {
            report.addAll(aggregateTravelAgentReportData(reportRequest.startDate(), reportRequest.endDate()));
        } else {
            log.warn("Report type {} not supported", reportRequest.reportType());
            throw new UnsupportedReportTypeException(String.format("Report type %s not supported", reportRequest.reportType()));
        }

        return report;
    }

    @Override // Location is not considered
    public ReportFile exportAggregatePerformanceReport(ReportRequest reportRequest, ExportFormat format) {
        log.info("Exporting {} report in format {}", reportRequest.reportType(), format);

        ReportFile file;
        if (reportRequest.reportType() == ReportType.SALES) {
            file = getToursReport(reportRequest, format);
        } else if (reportRequest.reportType() == ReportType.STAFF_PERFORMANCE) {
            file = getTravelAgentReport(reportRequest, format);
        } else {
            log.warn("Report type {} not supported", reportRequest.reportType());
            throw new UnsupportedReportTypeException(String.format("Report type %s not supported", reportRequest.reportType()));
        }

        return file;
    }

    private ReportFile getToursReport(ReportRequest reportRequest, ExportFormat format) {
        List<SalesAggregateReport> report = aggregateTourReportData(reportRequest.startDate(), reportRequest.endDate());

        if (reportRequest.location() != null) {
            log.info("Filtering by location {}", reportRequest.location());
            report = report.stream()
                    .filter(item -> {
                        String[] cityCountry = Utils.getCityCountry(reportRequest.location());

                        return item.getDestinationCity().equals(cityCountry[0]) && item.getDestinationCountry().equals(cityCountry[1]);
                    })
                    .toList();
            log.info("report count after filtering location {} : {}", reportRequest.location(), report.size());
        }

        String fileName = "sales_report";

        if (format == ExportFormat.CSV) {
            List<SalesStatisticsReportCsv> rep = ReportAggregateCsvMapper.toSalesStatisticsReportList(report);
            byte[] csv = ReportAggregateCsvMapper.toCsv(rep, SalesStatisticsReportCsv.class, new SalesReportColumnOrder());

            return new ReportFile(
                    csv,
                    MediaType.parseMediaType("text/csv"),
                    String.format("%s.csv", fileName)
            );
        } else if (format == ExportFormat.PDF) {
            byte[] pdf = ReportAggregatePdfMapper.toPdf(report, SalesAggregateReport.class);

            return new ReportFile(
                    pdf,
                    MediaType.APPLICATION_PDF,
                    String.format("%s.pdf", fileName)
            );
        } else {
            log.warn("File format {} not supported for exporting sales report", format);
            throw new UnsupportedFileFormatException(String.format("File format %s not supported for exporting sales report", format));
        }
    }

    private ReportFile getTravelAgentReport(ReportRequest reportRequest, ExportFormat format) {
        List<PerformanceAggregateReport> report = aggregateTravelAgentReportData(reportRequest.startDate(), reportRequest.endDate());
        String fileName = "performance_report";

        if (format == ExportFormat.CSV) {
            List<TravelAgentPerformanceReportCsv> rep = ReportAggregateCsvMapper.toTravelAgentPerformanceList(report);
            byte[] csv = ReportAggregateCsvMapper.toCsv(rep, TravelAgentPerformanceReportCsv.class, new PerformanceReportColumnOrder());

            return new ReportFile(
                    csv,
                    MediaType.parseMediaType("text/csv"),
                    String.format("%s.csv", fileName)
            );
        } else if (format == ExportFormat.PDF) {
            byte[] pdf = ReportAggregatePdfMapper.toPdf(report, PerformanceAggregateReport.class);

            return new ReportFile(
                    pdf,
                    MediaType.APPLICATION_PDF,
                    String.format("%s.pdf", fileName)
            );
        } else {
            log.warn("File format {} not supported for exporting performance report", format);
            throw new UnsupportedFileFormatException(String.format("File format %s not supported for exporting performance report", format));
        }
    }

    @Override
    public void sendWeeklyTourPerformanceReport(LocalDate startDate, LocalDate endDate) {
        if (!startDate.plusDays(7).isEqual(endDate)) {
            throw new IllegalArgumentException("Weekly report must be in span of a week");
        }

        List<SalesAggregateReport> report = aggregateAndSaveWeeklyTourReportData(startDate, endDate);

        List<SalesStatisticsReportCsv> rep = ReportAggregateCsvMapper.toSalesStatisticsReportList(report);
        log.info("SalesStatistics report size: {}", rep.size());

        byte[] csv = ReportAggregateCsvMapper.toCsv(rep, SalesStatisticsReportCsv.class, new SalesReportColumnOrder());

        s3ReportStorageService.uploadCsv(csv, "tour-perf", endDate);

        List<SalesAggregateReport> aggregateReports = aggregateTourReportData(startDate, endDate);
        sesEmailService.sendReportEmail(aggregateReports, csv, "tour-report", "", new SalesReportColumnOrder());
    }

    @Override
    public void sendWeeklyTravelAgentPerformanceReport(LocalDate startDate, LocalDate endDate) {
        if (!startDate.plusDays(7).isEqual(endDate)) {
            throw new IllegalArgumentException("Weekly report must be in span of a week");
        }

        List<PerformanceAggregateReport> report = aggregateAndSaveWeeklyTravelAgentReportData(startDate, endDate);

        List<TravelAgentPerformanceReportCsv> rep = ReportAggregateCsvMapper.toTravelAgentPerformanceList(report);
        log.info("TravelAgentPerformance report size: {}", rep.size());

        byte[] csv = ReportAggregateCsvMapper.toCsv(rep, TravelAgentPerformanceReportCsv.class, new PerformanceReportColumnOrder());

        s3ReportStorageService.uploadCsv(csv, "travel-agnt-perf", endDate);

        List<PerformanceAggregateReport> aggregateReports = aggregateTravelAgentReportData(startDate, endDate);
        sesEmailService.sendReportEmail(aggregateReports, csv, "agent-report", "", new PerformanceReportColumnOrder());
    }

    @Override
    public void aggregateBookingReportData(BookingStatusChangedEvent dto) {
        LocalDate today = LocalDate.now();

        reportsRepository.updateTourReportByState(
                today,
                TourDailyReportData.pk(dto.tourId()),
                TourDailyReportData.sk(today),
                dto.bookingStatus(),
                Utils.getRevenue(dto),
                Utils.getLoss(dto),
                dto.tourId(),
                dto.destination(),
                dto.tourName()
        );

        reportsRepository.updateTravelAgentReportByState(
                today,
                TravelAgentDailyReportData.pk(dto.travelAgentEmail()),
                TravelAgentDailyReportData.sk(today),
                dto.bookingStatus(),
                Utils.getRevenue(dto),
                Utils.getLoss(dto),
                dto.travelAgentName(),
                dto.travelAgentEmail()
        );
    }

    @Override
    public void aggregateReviewReportData(FeedbackEvent dto) {
        LocalDate today = LocalDate.now();

        boolean isNew = dto.oldRate() == null;

        int rateDiff = dto.newRate();
        if (!isNew) {
            rateDiff -= dto.oldRate();
        }

        reportsRepository.updateTourReportReview(
                today,
                TourDailyReportData.pk(dto.tourId()),
                TourDailyReportData.sk(today),
                rateDiff,
                dto.newRate(),
                isNew,
                dto.tourId(),
                dto.destination(),
                dto.tourName()
        );

        reportsRepository.updateTravelAgentReportReview(
                today,
                TravelAgentDailyReportData.pk(dto.travelAgentEmail()),
                TravelAgentDailyReportData.sk(today),
                rateDiff,
                dto.newRate(),
                isNew,
                dto.travelAgentName(),
                dto.travelAgentEmail()
        );
    }

    private List<SalesAggregateReport> aggregateTourReportData(LocalDate startDate, LocalDate endDate) {
        log.info("Aggregating sales report data for time period {} - {}", startDate, endDate);
        List<TourDailyReportData> toursReportsGroupedByTourId = aggregateDailyTourPerformanceReport(startDate, endDate);
        List<TourWeeklyReportData> tourWeeklyReportsList = WeeklyReportMapper.toTourWeeklyReportList(toursReportsGroupedByTourId, endDate);

        long between = ChronoUnit.DAYS.between(startDate, endDate);
        List<TourDailyReportData> prevToursReportsGroupedByTourId = aggregateDailyTourPerformanceReport(startDate.minusDays(between), startDate);
        List<TourWeeklyReportData> previousWeekReport = WeeklyReportMapper.toTourWeeklyReportList(prevToursReportsGroupedByTourId, startDate);

        log.info("Building SalesAggregateReport where previous period is empty");
        List<SalesAggregateReport> salesAggregateReportList = ReportAggregateMapper.toSalesAggregateReportList(tourWeeklyReportsList, previousWeekReport, startDate, endDate);

        log.info("Returning SalesAggregateReport with {} items", salesAggregateReportList.size());
        return salesAggregateReportList;
    }

    private List<PerformanceAggregateReport> aggregateTravelAgentReportData(LocalDate startDate, LocalDate endDate) {
        log.info("Aggregating performance report data for time period {} - {}", startDate, endDate);
        List<TravelAgentDailyReportData> travelAgentsWeeklyReportsGroupedByTaEmail = aggregateDailyTravelAgentPerformanceReport(startDate, endDate);
        List<TravelAgentWeeklyReportData> travelAgentWeeklyReportsList = WeeklyReportMapper.toTravelAgentWeeklyReportList(travelAgentsWeeklyReportsGroupedByTaEmail, endDate);

        long between = ChronoUnit.DAYS.between(startDate, endDate);
        List<TravelAgentDailyReportData> prevTravelAgentsWeeklyReportsGroupedByTaEmail = aggregateDailyTravelAgentPerformanceReport(startDate.minusDays(between), startDate);
        List<TravelAgentWeeklyReportData> previousWeekReport = WeeklyReportMapper.toTravelAgentWeeklyReportList(prevTravelAgentsWeeklyReportsGroupedByTaEmail, startDate);

        log.info("Building PerformanceAggregateReport where previous period is empty");
        List<PerformanceAggregateReport> performanceAggregateReportList = ReportAggregateMapper.toPerformanceAggregateList(travelAgentWeeklyReportsList, previousWeekReport, startDate, endDate);

        log.info("Returning PerformanceAggregateReport with {} items", performanceAggregateReportList.size());
        return performanceAggregateReportList;
    }

    private List<SalesAggregateReport> aggregateAndSaveWeeklyTourReportData(LocalDate startDate, LocalDate endDate) {
        log.info("Aggregating sales report data for week {} - {}", startDate, endDate);
        List<TourDailyReportData> toursReportsGroupedByTourId = aggregateDailyTourPerformanceReport(startDate, endDate);

        List<TourWeeklyReportData> tourWeeklyReportsList = WeeklyReportMapper.toTourWeeklyReportList(toursReportsGroupedByTourId, endDate);
        reportsRepository.saveWeeklyTourReports(tourWeeklyReportsList);
        log.info("grouped weekly tour reports count = {}", tourWeeklyReportsList.size());

        List<TourWeeklyReportData> previousWeekReport = reportsRepository.getTourWeeklyReports(startDate);

        List<SalesAggregateReport> salesAggregateReportList = ReportAggregateMapper.toSalesAggregateReportList(tourWeeklyReportsList, previousWeekReport, startDate, endDate);
        log.info("got weekly statistics for {} tours", salesAggregateReportList.size());

        return salesAggregateReportList;
    }

    private List<PerformanceAggregateReport> aggregateAndSaveWeeklyTravelAgentReportData(LocalDate startDate, LocalDate endDate) {
        log.info("Aggregating performance report data for week {} - {}", startDate, endDate);
        List<TravelAgentDailyReportData> travelAgentsWeeklyReportsGroupedByTaEmail = aggregateDailyTravelAgentPerformanceReport(startDate, endDate);

        List<TravelAgentWeeklyReportData> travelAgentWeeklyReportsList = WeeklyReportMapper.toTravelAgentWeeklyReportList(travelAgentsWeeklyReportsGroupedByTaEmail, endDate);
        reportsRepository.saveWeeklyTaReports(travelAgentWeeklyReportsList);
        log.info("grouped weekly travel agent reports count = {}", travelAgentWeeklyReportsList.size());

        List<TravelAgentWeeklyReportData> previousWeekReport = reportsRepository.getTravelAgentWeeklyReports(startDate);
        List<PerformanceAggregateReport> performanceAggregateReportList = ReportAggregateMapper.toPerformanceAggregateList(travelAgentWeeklyReportsList, previousWeekReport, startDate, endDate);
        log.info("got weekly statistics for {} travel agent ", performanceAggregateReportList.size());

        return performanceAggregateReportList;
    }

    private List<TourDailyReportData> aggregateDailyTourPerformanceReport(LocalDate startDate, LocalDate endDate) {
        log.info("getting tour data from {} to {}", startDate, endDate);
        List<TourDailyReportData> tourDailyReportList = reportsRepository.getTourDailyReportList(startDate, endDate);

        Map<String, TourDailyReportData> tourReports =
                tourDailyReportList.stream()
                        .collect(Collectors.toMap(
                                TourDailyReportData::getTourId,
                                TourDailyReportData::new,
                                (a, b) -> {
                                    setFields(a, b);
                                    return a;
                                }
                        ));
        List<TourDailyReportData> toursReportsGroupedByTourId = new ArrayList<>(tourReports.values());

        log.info("found {} tour reports groups by tour id", toursReportsGroupedByTourId.size());
        return toursReportsGroupedByTourId;
    }

    private List<TravelAgentDailyReportData> aggregateDailyTravelAgentPerformanceReport(LocalDate startDate, LocalDate endDate) {
        log.info("getting travel agent from {} to {}", startDate, endDate);
        List<TravelAgentDailyReportData> travelAgentDailyReports = reportsRepository.getTravelAgentDailyReportList(startDate, endDate);

        Map<String, TravelAgentDailyReportData> travelAgentReports =
                travelAgentDailyReports.stream()
                        .collect(Collectors.toMap(
                                TravelAgentDailyReportData::getTravelAgentEmail,
                                TravelAgentDailyReportData::new,
                                (a, b) -> {
                                    setFields(a, b);
                                    return a;
                                }
                        ));
        List<TravelAgentDailyReportData> travelAgentsReportsGroupedByTaEmail = new ArrayList<>(travelAgentReports.values());
        log.info("found {} tour reports groups by travel agent email", travelAgentsReportsGroupedByTaEmail.size());
        return travelAgentsReportsGroupedByTaEmail;
    }

    private void setFields(ReportData a, ReportData b) {
        a.setCancelledToursCount(a.getCancelledToursCount() + b.getCancelledToursCount());
        a.setConfirmedToursCount(a.getConfirmedToursCount() + b.getConfirmedToursCount());
        a.setFinishedToursCount(a.getFinishedToursCount() + b.getFinishedToursCount());

        a.setCancelledToursRevenue(Utils.nvl(a.getCancelledToursRevenue()).add(Utils.nvl(b.getCancelledToursRevenue())));
        a.setConfirmedToursRevenue(Utils.nvl(a.getConfirmedToursRevenue()).add(Utils.nvl(b.getConfirmedToursRevenue())));
        a.setFinishedToursRevenue(Utils.nvl(a.getFinishedToursRevenue()).add(Utils.nvl(b.getFinishedToursRevenue())));

        a.setCancelledToursLoss(Utils.nvl(a.getCancelledToursLoss()).add(Utils.nvl(b.getCancelledToursLoss())));

        a.setFeedbackCount(a.getFeedbackCount() + b.getFeedbackCount());
        a.setFeedbackRatingSum(a.getFeedbackRatingSum() + b.getFeedbackRatingSum());

        if (a.getMinFeedbackRate() == 0) {
            a.setMinFeedbackRate(b.getMinFeedbackRate());
        } else {
            a.setMinFeedbackRate(Math.min(a.getMinFeedbackRate(), b.getMinFeedbackRate()));
        }
    }
}
