package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.ExportFormat;
import com.epam.edp.demo.dto.FeedbackEvent;
import com.epam.edp.demo.dto.ReportFile;
import com.epam.edp.demo.dto.ReportRequest;
import com.epam.edp.demo.dto.ReportType;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.entity.BookingStatus;
import com.epam.edp.demo.entity.ReportData;
import com.epam.edp.demo.entity.TourDailyReportData;
import com.epam.edp.demo.entity.TravelAgentDailyReportData;
import com.epam.edp.demo.exception.UnsupportedFileFormatException;
import com.epam.edp.demo.exception.UnsupportedReportTypeException;
import com.epam.edp.demo.model.AggregateReport;
import com.epam.edp.demo.model.SalesAggregateReport;
import com.epam.edp.demo.repository.ReportRepository;
import com.epam.edp.demo.repository.TravelAgentListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportsRepository;

    @Mock
    private S3ReportStorageService s3ReportStorageService;

    @Mock
    private SesEmailService sesEmailService;

    @Mock
    private TravelAgentListRepository travelAgentListRepository;

    @InjectMocks
    private ReportServiceImpl reportServiceImpl;

    @Test
    void test_sendWeeklyTourPerformanceReport_throwsException_whenDateRangeIsNotOneWeek() {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reportServiceImpl.sendWeeklyTourPerformanceReport(startDate, endDate)
        );

        assertEquals("Weekly report must be in span of a week", ex.getMessage());
        verify(s3ReportStorageService, never()).uploadCsv(any(), anyString(), any());
    }

    @Test
    void test_sendWeeklyTravelAgentPerformanceReport_throwsException_whenDateRangeIsNotOneWeek() {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 5);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reportServiceImpl.sendWeeklyTravelAgentPerformanceReport(startDate, endDate)
        );

        assertEquals("Weekly report must be in span of a week", ex.getMessage());
        verify(s3ReportStorageService, never()).uploadCsv(any(), anyString(), any());
    }

    @Test
    void test_sendWeeklyTourPerformanceReport_uploadsCsv_whenDateRangeIsValid() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of());
        when(reportsRepository.getTourWeeklyReports(startDate)).thenReturn(List.of());

        reportServiceImpl.sendWeeklyTourPerformanceReport(startDate, endDate);

        verify(reportsRepository).saveWeeklyTourReports(anyList());
        verify(s3ReportStorageService).uploadCsv(any(), eq("tour-perf"), eq(endDate));
    }

    @Test
    void test_sendWeeklyTravelAgentPerformanceReport_uploadsCsv_whenDateRangeIsValid() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        when(reportsRepository.getTravelAgentDailyReportList(startDate, endDate)).thenReturn(List.of());
        when(reportsRepository.getTravelAgentWeeklyReports(startDate)).thenReturn(List.of());

        reportServiceImpl.sendWeeklyTravelAgentPerformanceReport(startDate, endDate);

        verify(reportsRepository).saveWeeklyTaReports(anyList());
        verify(s3ReportStorageService).uploadCsv(any(), eq("travel-agnt-perf"), eq(endDate));
    }

    @Test
    void test_getAggregatePerformanceReport_returnsSalesReportList_whenTypeIsSales() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, null);
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of());

        List<AggregateReport> result = reportServiceImpl.getAggregatePerformanceReport(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void test_getAggregatePerformanceReport_returnsStaffPerformanceList_whenTypeIsStaffPerformance() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.STAFF_PERFORMANCE, startDate, endDate, null);
        when(reportsRepository.getTravelAgentDailyReportList(startDate, endDate)).thenReturn(List.of());

        List<AggregateReport> result = reportServiceImpl.getAggregatePerformanceReport(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void test_getAggregatePerformanceReport_filtersSalesByLocation_whenLocationMatches() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, "Paris, France");

        TourDailyReportData tourDaily = new TourDailyReportData();
        tourDaily.setTourId("tour-1");
        tourDaily.setTourName("City Tour");
        tourDaily.setDestination("Paris, France");
        tourDaily.setConfirmedToursCount(2);
        tourDaily.setFeedbackCount(1);
        tourDaily.setFeedbackRatingSum(5);
        tourDaily.setMinFeedbackRate(5);
        tourDaily.setFinishedToursRevenue(BigDecimal.valueOf(100));
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of(tourDaily));

        List<AggregateReport> result = reportServiceImpl.getAggregatePerformanceReport(request);

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof SalesAggregateReport);
    }

    @Test
    void test_getAggregatePerformanceReport_filtersSalesByLocation_whenLocationDoesNotMatch() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, "London, UK");

        TourDailyReportData tourDaily = new TourDailyReportData();
        tourDaily.setTourId("tour-1");
        tourDaily.setTourName("City Tour");
        tourDaily.setDestination("Paris, France");
        tourDaily.setConfirmedToursCount(2);
        tourDaily.setFeedbackCount(1);
        tourDaily.setFeedbackRatingSum(5);
        tourDaily.setMinFeedbackRate(5);
        tourDaily.setFinishedToursRevenue(BigDecimal.valueOf(100));
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of(tourDaily));

        List<AggregateReport> result = reportServiceImpl.getAggregatePerformanceReport(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void test_getAggregatePerformanceReport_throwsException_whenReportTypeIsUnsupported() {
        ReportRequest request = new ReportRequest(null, LocalDate.of(2026, 6, 27), LocalDate.of(2026, 7, 4), null);

        UnsupportedReportTypeException ex = assertThrows(
                UnsupportedReportTypeException.class,
                () -> reportServiceImpl.getAggregatePerformanceReport(request)
        );

        assertEquals("Report type null not supported", ex.getMessage());
    }

    @Test
    void test_exportAggregatePerformanceReport_returnsCsvFile_whenSalesReportRequested() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, null);
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of());

        ReportFile file = reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.CSV);

        assertNotNull(file);
        assertEquals("sales_report.csv", file.filename());
        assertEquals(MediaType.parseMediaType("text/csv"), file.mediaType());
        assertNotNull(file.bytes());
    }

    @Test
    void test_exportAggregatePerformanceReport_returnsPdfFile_whenSalesReportRequested() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, null);

        TourDailyReportData tourDaily = new TourDailyReportData();
        tourDaily.setTourId("tour-1");
        tourDaily.setTourName("City Tour");
        tourDaily.setDestination("Paris, France");
        tourDaily.setConfirmedToursCount(2);
        tourDaily.setFeedbackCount(1);
        tourDaily.setFeedbackRatingSum(5);
        tourDaily.setMinFeedbackRate(5);
        tourDaily.setFinishedToursRevenue(BigDecimal.valueOf(100));
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of(tourDaily));

        ReportFile file = reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.PDF);

        assertNotNull(file);
        assertEquals("sales_report.pdf", file.filename());
        assertEquals(MediaType.APPLICATION_PDF, file.mediaType());
        assertTrue(file.bytes().length > 0);
    }

    @Test
    void test_exportAggregatePerformanceReport_filtersByLocation_whenSalesLocationIsProvided() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, "London, UK");

        TourDailyReportData tourDaily = new TourDailyReportData();
        tourDaily.setTourId("tour-1");
        tourDaily.setTourName("City Tour");
        tourDaily.setDestination("Paris, France");
        tourDaily.setConfirmedToursCount(2);
        tourDaily.setFeedbackCount(1);
        tourDaily.setFeedbackRatingSum(5);
        tourDaily.setMinFeedbackRate(5);
        tourDaily.setFinishedToursRevenue(BigDecimal.valueOf(100));
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of(tourDaily));

        ReportFile file = reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.CSV);

        String csv = new String(file.bytes(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Tour name"));
        assertFalse(csv.contains("City Tour"));
    }

    @Test
    void test_exportAggregatePerformanceReport_returnsCsvFile_whenStaffPerformanceReportRequested() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.STAFF_PERFORMANCE, startDate, endDate, null);

        TravelAgentDailyReportData agentDaily = new TravelAgentDailyReportData();
        agentDaily.setTravelAgentName("Agent One");
        agentDaily.setTravelAgentEmail("agent@example.com");
        agentDaily.setConfirmedToursCount(3);
        agentDaily.setFeedbackCount(1);
        agentDaily.setFeedbackRatingSum(5);
        agentDaily.setMinFeedbackRate(5);
        agentDaily.setFinishedToursRevenue(BigDecimal.valueOf(150));
        when(reportsRepository.getTravelAgentDailyReportList(startDate, endDate)).thenReturn(List.of(agentDaily));

        ReportFile file = reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.CSV);

        assertNotNull(file);
        assertEquals("performance_report.csv", file.filename());
        assertEquals(MediaType.parseMediaType("text/csv"), file.mediaType());
        assertTrue(new String(file.bytes(), StandardCharsets.UTF_8).contains("Agent One"));
    }

    @Test
    void test_exportAggregatePerformanceReport_returnsPdfFile_whenStaffPerformanceReportRequested() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.STAFF_PERFORMANCE, startDate, endDate, null);

        TravelAgentDailyReportData agentDaily = new TravelAgentDailyReportData();
        agentDaily.setTravelAgentName("Agent One");
        agentDaily.setTravelAgentEmail("agent@example.com");
        agentDaily.setConfirmedToursCount(3);
        agentDaily.setFeedbackCount(1);
        agentDaily.setFeedbackRatingSum(5);
        agentDaily.setMinFeedbackRate(5);
        agentDaily.setFinishedToursRevenue(BigDecimal.valueOf(150));
        when(reportsRepository.getTravelAgentDailyReportList(startDate, endDate)).thenReturn(List.of(agentDaily));

        ReportFile file = reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.PDF);

        assertNotNull(file);
        assertEquals("performance_report.pdf", file.filename());
        assertEquals(MediaType.APPLICATION_PDF, file.mediaType());
        assertTrue(file.bytes().length > 0);
    }

    @Test
    void test_exportAggregatePerformanceReport_throwsException_whenSalesFormatIsNull() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.SALES, startDate, endDate, null);
        when(reportsRepository.getTourDailyReportList(startDate, endDate)).thenReturn(List.of());

        UnsupportedFileFormatException ex = assertThrows(
                UnsupportedFileFormatException.class,
                () -> reportServiceImpl.exportAggregatePerformanceReport(request, null)
        );

        assertTrue(ex.getMessage().contains("File format null not supported for exporting sales report"));
    }

    @Test
    void test_exportAggregatePerformanceReport_throwsException_whenStaffFormatIsNull() {
        LocalDate startDate = LocalDate.of(2026, 6, 27);
        LocalDate endDate = LocalDate.of(2026, 7, 4);
        ReportRequest request = new ReportRequest(ReportType.STAFF_PERFORMANCE, startDate, endDate, null);
        when(reportsRepository.getTravelAgentDailyReportList(startDate, endDate)).thenReturn(List.of());

        UnsupportedFileFormatException ex = assertThrows(
                UnsupportedFileFormatException.class,
                () -> reportServiceImpl.exportAggregatePerformanceReport(request, null)
        );

        assertTrue(ex.getMessage().contains("File format null not supported for exporting performance report"));
    }

    @Test
    void test_exportAggregatePerformanceReport_throwsException_whenReportTypeIsUnsupported() {
        ReportRequest request = new ReportRequest(null, LocalDate.of(2026, 6, 27), LocalDate.of(2026, 7, 4), null);

        UnsupportedReportTypeException ex = assertThrows(
                UnsupportedReportTypeException.class,
                () -> reportServiceImpl.exportAggregatePerformanceReport(request, ExportFormat.CSV)
        );

        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void test_aggregateBookingReportData_updatesTourAndTravelAgentReports_whenEventIsReceived() {
        BookingStatusChangedEvent event = BookingStatusChangedEvent.builder()
                .tourId("tour-1")
                .tourName("Tour Name")
                .destination("Paris, France")
                .travelAgentName("Agent Name")
                .travelAgentEmail("agent@example.com")
                .bookingStatus(BookingStatus.CANCELED)
                .price(200)
                .cancelCharge(50.5)
                .build();

        reportServiceImpl.aggregateBookingReportData(event);

        ArgumentCaptor<BigDecimal> revenueCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> lossCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(reportsRepository).updateTourReportByState(
                any(LocalDate.class),
                eq(TourDailyReportData.pk("tour-1")),
                anyString(),
                eq(BookingStatus.CANCELED),
                revenueCaptor.capture(),
                lossCaptor.capture(),
                eq("tour-1"),
                eq("Paris, France"),
                eq("Tour Name")
        );

        assertEquals(0, revenueCaptor.getValue().compareTo(BigDecimal.valueOf(50.5)));
        assertEquals(0, lossCaptor.getValue().compareTo(BigDecimal.valueOf(149.5)));

        verify(reportsRepository).updateTravelAgentReportByState(
                any(LocalDate.class),
                eq(TravelAgentDailyReportData.pk("agent@example.com")),
                anyString(),
                eq(BookingStatus.CANCELED),
                eq(BigDecimal.valueOf(50.5)),
                eq(BigDecimal.valueOf(149.5)),
                eq("Agent Name"),
                eq("agent@example.com")
        );
    }

    @Test
    void test_aggregateReviewReportData_updatesBothReportsWithIsNewTrue_whenOldRateIsNull() {
        FeedbackEvent event = new FeedbackEvent(
                "2026-07-04T00:00:00Z",
                4,
                null,
                "tour-1",
                "agent@example.com",
                "Agent Name",
                "Paris, France",
                "Tour Name"
        );

        reportServiceImpl.aggregateReviewReportData(event);

        verify(reportsRepository).updateTourReportReview(
                any(LocalDate.class),
                eq(TourDailyReportData.pk("tour-1")),
                anyString(),
                eq(4),
                eq(4),
                eq(true),
                eq("tour-1"),
                eq("Paris, France"),
                eq("Tour Name")
        );

        verify(reportsRepository).updateTravelAgentReportReview(
                any(LocalDate.class),
                eq(TravelAgentDailyReportData.pk("agent@example.com")),
                anyString(),
                eq(4),
                eq(4),
                eq(true),
                eq("Agent Name"),
                eq("agent@example.com")
        );
    }

    @Test
    void test_aggregateReviewReportData_updatesBothReportsWithRateDiff_whenOldRateExists() {
        FeedbackEvent event = new FeedbackEvent(
                "2026-07-04T00:00:00Z",
                5,
                2,
                "tour-1",
                "agent@example.com",
                "Agent Name",
                "Paris, France",
                "Tour Name"
        );

        reportServiceImpl.aggregateReviewReportData(event);

        verify(reportsRepository).updateTourReportReview(
                any(LocalDate.class),
                eq(TourDailyReportData.pk("tour-1")),
                anyString(),
                eq(3),
                eq(5),
                eq(false),
                eq("tour-1"),
                eq("Paris, France"),
                eq("Tour Name")
        );

        verify(reportsRepository).updateTravelAgentReportReview(
                any(LocalDate.class),
                eq(TravelAgentDailyReportData.pk("agent@example.com")),
                anyString(),
                eq(3),
                eq(5),
                eq(false),
                eq("Agent Name"),
                eq("agent@example.com")
        );
    }

    @Test
    void test_setFields_sumsAllCountersAndAmounts_andSetsMinRateFromB_whenAMinRateIsZero() throws Exception {
        TourDailyReportData a = new TourDailyReportData();
        a.setCancelledToursCount(1);
        a.setConfirmedToursCount(2);
        a.setFinishedToursCount(3);
        a.setCancelledToursRevenue(null);
        a.setConfirmedToursRevenue(BigDecimal.valueOf(10));
        a.setFinishedToursRevenue(null);
        a.setCancelledToursLoss(BigDecimal.valueOf(1));
        a.setFeedbackCount(2);
        a.setFeedbackRatingSum(6);
        a.setMinFeedbackRate(0);

        TourDailyReportData b = new TourDailyReportData();
        b.setCancelledToursCount(4);
        b.setConfirmedToursCount(5);
        b.setFinishedToursCount(6);
        b.setCancelledToursRevenue(BigDecimal.valueOf(7));
        b.setConfirmedToursRevenue(null);
        b.setFinishedToursRevenue(BigDecimal.valueOf(8));
        b.setCancelledToursLoss(null);
        b.setFeedbackCount(3);
        b.setFeedbackRatingSum(9);
        b.setMinFeedbackRate(4);

        Method method = ReportServiceImpl.class.getDeclaredMethod("setFields", ReportData.class, ReportData.class);
        method.setAccessible(true);
        method.invoke(reportServiceImpl, a, b);

        assertEquals(5, a.getCancelledToursCount());
        assertEquals(7, a.getConfirmedToursCount());
        assertEquals(9, a.getFinishedToursCount());
        assertEquals(0, a.getCancelledToursRevenue().compareTo(BigDecimal.valueOf(7)));
        assertEquals(0, a.getConfirmedToursRevenue().compareTo(BigDecimal.valueOf(10)));
        assertEquals(0, a.getFinishedToursRevenue().compareTo(BigDecimal.valueOf(8)));
        assertEquals(0, a.getCancelledToursLoss().compareTo(BigDecimal.valueOf(1)));
        assertEquals(5, a.getFeedbackCount());
        assertEquals(15, a.getFeedbackRatingSum());
        assertEquals(4, a.getMinFeedbackRate());
    }

    @Test
    void test_setFields_setsMinimumOfBothMinRates_whenAMinRateIsNonZero() throws Exception {
        TourDailyReportData a = new TourDailyReportData();
        a.setMinFeedbackRate(3);

        TourDailyReportData b = new TourDailyReportData();
        b.setMinFeedbackRate(5);

        Method method = ReportServiceImpl.class.getDeclaredMethod("setFields", ReportData.class, ReportData.class);
        method.setAccessible(true);
        method.invoke(reportServiceImpl, a, b);

        assertEquals(3, a.getMinFeedbackRate());

        a.setMinFeedbackRate(6);
        b.setMinFeedbackRate(2);
        method.invoke(reportServiceImpl, a, b);
        assertEquals(2, a.getMinFeedbackRate());
    }

    @Test
    void test_setFields_throwsException_whenSecondArgumentIsNull() throws Exception {
        TourDailyReportData a = new TourDailyReportData();

        Method method = ReportServiceImpl.class.getDeclaredMethod("setFields", ReportData.class, ReportData.class);
        method.setAccessible(true);

        assertThrows(Exception.class, () -> method.invoke(reportServiceImpl, a, null));
    }
}

