package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.BookingStatus;
import com.epam.edp.demo.entity.TourDailyReportData;
import com.epam.edp.demo.entity.TourWeeklyReportData;
import com.epam.edp.demo.entity.TravelAgentDailyReportData;
import com.epam.edp.demo.entity.TravelAgentWeeklyReportData;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private BulkOperations tourBulkOperations;

    @Mock
    private BulkOperations travelAgentBulkOperations;

    private ReportRepositoryImpl reportRepository;

    @BeforeEach
    void setUp() {
        reportRepository = new ReportRepositoryImpl(mongoTemplate);
    }

    @Test
    void getTourDailyReportList_queriesMongoByDailyTourAndDateRange() {
        TourDailyReportData item = new TourDailyReportData();
        item.setTourId("tour-1");
        when(mongoTemplate.find(any(Query.class), eq(TourDailyReportData.class), eq("reports")))
                .thenReturn(List.of(item));

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 4);

        List<TourDailyReportData> result = reportRepository.getTourDailyReportList(start, end);

        assertIterableEquals(List.of(item), result);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(TourDailyReportData.class), eq("reports"));
        Document query = captor.getValue().getQueryObject();
        assertEquals("DAILY#TOUR", query.get("tourDaily"));
        Document range = (Document) query.get("tourReportSk");
        assertEquals(start.toString(), range.get("$gte"));
        assertEquals(end + "#\uffff", range.get("$lte"));
    }

    @Test
    void getTravelAgentDailyReportList_queriesMongoByDailyAgentAndDateRange() {
        TravelAgentDailyReportData item = new TravelAgentDailyReportData();
        item.setTravelAgentEmail("agent@example.com");
        when(mongoTemplate.find(any(Query.class), eq(TravelAgentDailyReportData.class), eq("reports")))
                .thenReturn(List.of(item));

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 4);

        List<TravelAgentDailyReportData> result = reportRepository.getTravelAgentDailyReportList(start, end);

        assertIterableEquals(List.of(item), result);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(TravelAgentDailyReportData.class), eq("reports"));
        Document query = captor.getValue().getQueryObject();
        assertEquals("DAILY#AGENT", query.get("tourDaily"));
        Document range = (Document) query.get("tourReportSk");
        assertEquals(start.toString(), range.get("$gte"));
        assertEquals(end + "#\uffff", range.get("$lte"));
    }

    @Test
    void getTourWeeklyReports_queriesMongoByWeeklyTourRegex() {
        TourWeeklyReportData item = new TourWeeklyReportData();
        when(mongoTemplate.find(any(Query.class), eq(TourWeeklyReportData.class), eq("reports")))
                .thenReturn(List.of(item));

        LocalDate reportDate = LocalDate.of(2026, 7, 4);

        List<TourWeeklyReportData> result = reportRepository.getTourWeeklyReports(reportDate);

        assertIterableEquals(List.of(item), result);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(TourWeeklyReportData.class), eq("reports"));
        Document query = captor.getValue().getQueryObject();
        assertEquals("WEEKLY#TOUR", query.get("tourDaily"));
        Pattern regex = (Pattern) query.get("tourReportSk");
        assertEquals("^" + reportDate, regex.pattern());
    }

    @Test
    void getTravelAgentWeeklyReports_queriesMongoByWeeklyAgentRegex() {
        TravelAgentWeeklyReportData item = new TravelAgentWeeklyReportData();
        when(mongoTemplate.find(any(Query.class), eq(TravelAgentWeeklyReportData.class), eq("reports")))
                .thenReturn(List.of(item));

        LocalDate reportDate = LocalDate.of(2026, 7, 4);

        List<TravelAgentWeeklyReportData> result = reportRepository.getTravelAgentWeeklyReports(reportDate);

        assertIterableEquals(List.of(item), result);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(TravelAgentWeeklyReportData.class), eq("reports"));
        Document query = captor.getValue().getQueryObject();
        assertEquals("WEEKLY#AGENT", query.get("tourDaily"));
        Pattern regex = (Pattern) query.get("tourReportSk");
        assertEquals("^" + reportDate, regex.pattern());
    }

    @Test
    void saveWeeklyTourReports_usesMongoBulkInsertAndExecute() {
        List<TourWeeklyReportData> reports = List.of(new TourWeeklyReportData(), new TourWeeklyReportData());
        when(mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TourWeeklyReportData.class, "reports"))
                .thenReturn(tourBulkOperations);

        reportRepository.saveWeeklyTourReports(reports);

        verify(tourBulkOperations, times(2)).insert(any(TourWeeklyReportData.class));
        verify(tourBulkOperations).execute();
    }

    @Test
    void saveWeeklyTaReports_usesMongoBulkInsertAndExecute() {
        List<TravelAgentWeeklyReportData> reports = List.of(new TravelAgentWeeklyReportData(), new TravelAgentWeeklyReportData());
        when(mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TravelAgentWeeklyReportData.class, "reports"))
                .thenReturn(travelAgentBulkOperations);

        reportRepository.saveWeeklyTaReports(reports);

        verify(travelAgentBulkOperations, times(2)).insert(any(TravelAgentWeeklyReportData.class));
        verify(travelAgentBulkOperations).execute();
    }

    @Test
    void updateTourReportByState_upsertsConfirmedCountersAndInsertFields() {
        reportRepository.updateTourReportByState(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                BookingStatus.CONFIRMED,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                "tour-1",
                "Paris, France",
                "Tour Name"
        );

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(TourDailyReportData.class), eq("reports"));

        assertPkAndSk(queryCaptor.getValue(), "pk-1", "sk-1");
        Document setOnInsert = (Document) updateCaptor.getValue().getUpdateObject().get("$setOnInsert");
        assertEquals("tour-1", setOnInsert.get("tourId"));
        assertEquals("Tour Name", setOnInsert.get("tourName"));
        assertEquals("Paris, France", setOnInsert.get("destination"));
        assertEquals("DAILY#TOUR", setOnInsert.get("tourDaily"));
        assertEquals("2026-07-04#tour-1", setOnInsert.get("tourReportSk"));

        Document inc = (Document) updateCaptor.getValue().getUpdateObject().get("$inc");
        assertEquals(1, inc.get("confirmedToursCount"));
        assertEquals(BigDecimal.TEN, inc.get("confirmedToursRevenue"));
    }

    @Test
    void updateTourReportByState_upsertsCancelledCountersAndLoss() {
        reportRepository.updateTourReportByState(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                BookingStatus.CANCELED,
                BigDecimal.TEN,
                BigDecimal.valueOf(3),
                "tour-1",
                "Paris, France",
                "Tour Name"
        );

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), updateCaptor.capture(), eq(TourDailyReportData.class), eq("reports"));

        Document inc = (Document) updateCaptor.getValue().getUpdateObject().get("$inc");
        assertEquals(1, inc.get("cancelledToursCount"));
        assertEquals(BigDecimal.TEN, inc.get("cancelledToursRevenue"));
        assertEquals(BigDecimal.valueOf(3), inc.get("cancelledToursLoss"));
    }

    @Test
    void updateTourReportByState_throwsException_whenStateIsUnsupported() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reportRepository.updateTourReportByState(
                        LocalDate.of(2026, 7, 4),
                        "pk-1",
                        "sk-1",
                        BookingStatus.STARTED,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        "tour-1",
                        "Paris, France",
                        "Tour Name"
                )
        );

        assertTrue(ex.getMessage().contains("Unknown state"));
    }

    @Test
    void updateTravelAgentReportByState_upsertsFinishedCountersAndInsertFields() {
        reportRepository.updateTravelAgentReportByState(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                BookingStatus.FINISHED,
                BigDecimal.valueOf(8),
                BigDecimal.ZERO,
                "Agent Name",
                "agent@example.com"
        );

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(TravelAgentDailyReportData.class), eq("reports"));

        assertPkAndSk(queryCaptor.getValue(), "pk-1", "sk-1");
        Document setOnInsert = (Document) updateCaptor.getValue().getUpdateObject().get("$setOnInsert");
        assertEquals("Agent Name", setOnInsert.get("travelAgentName"));
        assertEquals("agent@example.com", setOnInsert.get("travelAgentEmail"));
        assertEquals("DAILY#AGENT", setOnInsert.get("tourDaily"));
        assertEquals("2026-07-04#agent@example.com", setOnInsert.get("tourReportSk"));

        Document inc = (Document) updateCaptor.getValue().getUpdateObject().get("$inc");
        assertEquals(1, inc.get("finishedToursCount"));
        assertEquals(BigDecimal.valueOf(8), inc.get("finishedToursRevenue"));
    }

    @Test
    void updateTravelAgentReportByState_throwsException_whenStateIsUnsupported() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reportRepository.updateTravelAgentReportByState(
                        LocalDate.of(2026, 7, 4),
                        "pk-1",
                        "sk-1",
                        BookingStatus.BOOKED,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        "Agent Name",
                        "agent@example.com"
                )
        );

        assertTrue(ex.getMessage().contains("Unknown state"));
    }

    @Test
    void updateTourReportReview_includesFeedbackCount_whenReviewIsNew() {
        reportRepository.updateTourReportReview(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                2,
                4,
                true,
                "tour-1",
                "Paris, France",
                "Tour Name"
        );

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), updateCaptor.capture(), eq(TourDailyReportData.class), eq("reports"));

        Document update = updateCaptor.getValue().getUpdateObject();
        Document inc = (Document) update.get("$inc");
        assertEquals(1, inc.get("feedbackCount"));
        assertEquals(2, inc.get("feedbackRatingSum"));
        Document min = (Document) update.get("$min");
        assertEquals(4, min.get("minFeedbackRate"));
    }

    @Test
    void updateTourReportReview_excludesFeedbackCount_whenReviewIsNotNew() {
        reportRepository.updateTourReportReview(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                -1,
                2,
                false,
                "tour-1",
                "Paris, France",
                "Tour Name"
        );

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(Query.class), updateCaptor.capture(), eq(TourDailyReportData.class), eq("reports"));

        Document update = updateCaptor.getValue().getUpdateObject();
        Document inc = (Document) update.get("$inc");
        assertEquals(-1, inc.get("feedbackRatingSum"));
        assertTrue(!inc.containsKey("feedbackCount"));
    }

    @Test
    void updateTravelAgentReportReview_upsertsAgentFieldsAndReviewAggregates() {
        reportRepository.updateTravelAgentReportReview(
                LocalDate.of(2026, 7, 4),
                "pk-1",
                "sk-1",
                1,
                3,
                true,
                "Agent Name",
                "agent@example.com"
        );

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(TravelAgentDailyReportData.class), eq("reports"));

        assertPkAndSk(queryCaptor.getValue(), "pk-1", "sk-1");
        Document setOnInsert = (Document) updateCaptor.getValue().getUpdateObject().get("$setOnInsert");
        assertEquals("Agent Name", setOnInsert.get("travelAgentName"));
        assertEquals("agent@example.com", setOnInsert.get("travelAgentEmail"));
        assertEquals("DAILY#AGENT", setOnInsert.get("tourDaily"));
        assertEquals("2026-07-04#agent@example.com", setOnInsert.get("tourReportSk"));

        Document inc = (Document) updateCaptor.getValue().getUpdateObject().get("$inc");
        assertEquals(1, inc.get("feedbackCount"));
        assertEquals(1, inc.get("feedbackRatingSum"));
        Document min = (Document) updateCaptor.getValue().getUpdateObject().get("$min");
        assertEquals(3, min.get("minFeedbackRate"));
    }

    private static void assertPkAndSk(Query query, String expectedPk, String expectedSk) {
        Document document = query.getQueryObject();
        assertEquals(expectedPk, document.get("pk"));
        assertEquals(expectedSk, document.get("sk"));
    }
}




