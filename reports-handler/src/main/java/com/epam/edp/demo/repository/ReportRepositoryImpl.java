package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Repository
public class ReportRepositoryImpl implements ReportRepository {
    private final MongoTemplate mongoTemplate;

    public ReportRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<TourDailyReportData> getTourDailyReportList(LocalDate start, LocalDate end) {
        String startKey = start.toString();
        String endKey = end.toString() + "#\uffff";

        Query query = new Query();

        query.addCriteria(
                Criteria.where("tourDaily")
                        .is("DAILY#TOUR")
                        .and("tourReportSk")
                        .gte(startKey)
                        .lte(endKey)
        );

        return mongoTemplate.find(
                query,
                TourDailyReportData.class,
                "reports"
        );
    }

    @Override
    public List<TravelAgentDailyReportData> getTravelAgentDailyReportList(LocalDate start, LocalDate end) {
        Query query = new Query();

        query.addCriteria(
                Criteria.where("tourDaily")
                        .is("DAILY#AGENT")
                        .and("tourReportSk")
                        .gte(start.toString())
                        .lte(end.toString() + "#\uffff")
        );

        return mongoTemplate.find(
                query,
                TravelAgentDailyReportData.class,
                "reports"
        );
    }

    @Override
    public List<TourWeeklyReportData> getTourWeeklyReports(LocalDate reportDate) {
        log.info("querying db for weekly tour report for items between {} and {}", reportDate.minusDays(7), reportDate);

        Query query = new Query();
        query.addCriteria(
                Criteria.where("tourDaily")
                        .is("WEEKLY#TOUR")
                        .and("tourReportSk")
                        .regex("^" + reportDate)
        );


        List<TourWeeklyReportData> result = mongoTemplate.find(
                query,
                TourWeeklyReportData.class,
                "reports"
        );

        log.info("Received {} items", result.size());
        return result;
    }

    @Override
    public List<TravelAgentWeeklyReportData> getTravelAgentWeeklyReports(LocalDate reportDate) {
        log.info("querying db for weekly travel agent report for items between {} and {}", reportDate.minusDays(7), reportDate);

        Query query = new Query();
        query.addCriteria(
                Criteria.where("tourDaily")
                        .is("WEEKLY#AGENT")
                        .and("tourReportSk")
                        .regex("^" + reportDate)
        );

        List<TravelAgentWeeklyReportData> result = mongoTemplate.find(
                query,
                TravelAgentWeeklyReportData.class,
                "reports"
        );

        log.info("Received {} items", result.size());
        return result;
    }
    @Override
    public void saveWeeklyTourReports(
            List<TourWeeklyReportData> reports) {

        BulkOperations bulk =
                mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        TourWeeklyReportData.class,
                        "reports"
                );


        reports.forEach(bulk::insert);

        bulk.execute();
    }

    @Override
    public void saveWeeklyTaReports(
            List<TravelAgentWeeklyReportData> reports) {

        BulkOperations bulk =
                mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        TravelAgentWeeklyReportData.class,
                        "reports"
                );

        reports.forEach(bulk::insert);

        bulk.execute();
    }

    @Override
    public void updateTourReportByState(
            LocalDate now,
            String pk,
            String sk,
            BookingStatus state,
            BigDecimal revenue,
            BigDecimal canceledLoss,
            String tourId,
            String destination,
            String tourName) {

        Query query = new Query(
                Criteria.where("pk").is(pk)
                        .and("sk").is(sk)
        );
        Update update = new Update();
        update.setOnInsert("tourId", tourId);
        update.setOnInsert("tourName", tourName);
        update.setOnInsert("destination", destination);
        update.setOnInsert(
                "tourDaily",
                "DAILY#TOUR"
        );
        update.setOnInsert(
                "tourReportSk",
                now + "#" + tourId
        );
        switch(state) {
            case CONFIRMED -> {
                update.inc(
                        "confirmedToursCount",
                        1
                );

                update.inc(
                        "confirmedToursRevenue",
                        revenue
                );
            }
            case CANCELED -> {
                update.inc(
                        "cancelledToursCount",
                        1
                );

                update.inc(
                        "cancelledToursRevenue",
                        revenue
                );

                update.inc(
                        "cancelledToursLoss",
                        canceledLoss
                );
            }
            case FINISHED -> {
                update.inc(
                        "finishedToursCount",
                        1
                );

                update.inc(
                        "finishedToursRevenue",
                        revenue
                );
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown state " + state
                    );
        }
        mongoTemplate.upsert(
                query,
                update,
                TourDailyReportData.class,
                "reports"
        );
        log.info("tour report updated (pk={}, sk={}) by state {}", pk, sk, state);
    }

    @Override
    public void updateTravelAgentReportByState(
            LocalDate now,
            String pk,
            String sk,
            BookingStatus state,
            BigDecimal revenue,
            BigDecimal canceledLoss,
            String travelAgentName,
            String travelAgentEmail) {

        log.info("updating travel agent report (pk={}, sk={}) by state {}", pk, sk, state);
        Query query = new Query(
                Criteria.where("pk").is(pk)
                        .and("sk").is(sk)
        );
        Update update = new Update();
        // DynamoDB if_not_exists equivalent
        update.setOnInsert("travelAgentName", travelAgentName);
        update.setOnInsert("travelAgentEmail", travelAgentEmail);
        update.setOnInsert(
                "tourDaily",
                "DAILY#AGENT"
        );
        update.setOnInsert(
                "tourReportSk",
                now + "#" + travelAgentEmail
        );
        switch (state) {
            case CONFIRMED -> {
                update.inc(
                        "confirmedToursCount",
                        1
                );

                update.inc(
                        "confirmedToursRevenue",
                        revenue != null ? revenue : BigDecimal.ZERO
                );
            }
            case CANCELED -> {
                update.inc(
                        "cancelledToursCount",
                        1
                );

                update.inc(
                        "cancelledToursRevenue",
                        revenue != null ? revenue : BigDecimal.ZERO
                );

                update.inc(
                        "cancelledToursLoss",
                        canceledLoss != null ? canceledLoss : BigDecimal.ZERO
                );
            }
            case FINISHED -> {
                update.inc(
                        "finishedToursCount",
                        1
                );

                update.inc(
                        "finishedToursRevenue",
                        revenue != null ? revenue : BigDecimal.ZERO
                );
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown state: " + state
                    );
        }

        mongoTemplate.upsert(
                query,
                update,
                TravelAgentDailyReportData.class,
                "reports"
        );

        log.info("travel agent report updated");
    }

    @Override
    public void updateTourReportReview(
            LocalDate now,
            String pk,
            String sk,
            int rateDiff,
            int newRate,
            boolean isNew,
            String tourId,
            String destination,
            String tourName) {


        Query query = new Query(
                Criteria.where("pk").is(pk)
                        .and("sk").is(sk)
        );


        Update update = new Update();


        update.setOnInsert(
                "tourDaily",
                "DAILY#TOUR"
        );

        update.setOnInsert(
                "tourReportSk",
                now + "#" + tourId
        );


        update.setOnInsert(
                "tourId",
                tourId
        );

        update.setOnInsert(
                "destination",
                destination
        );

        update.setOnInsert(
                "tourName",
                tourName
        );


        updateReportReview(
                update,
                isNew,
                rateDiff,
                newRate
        );


        mongoTemplate.upsert(
                query,
                update,
                TourDailyReportData.class,
                "reports"
        );
    }

    @Override
    public void updateTravelAgentReportReview(
            LocalDate now,
            String pk,
            String sk,
            int rateDiff,
            int newRate,
            boolean isNew,
            String travelAgentName,
            String travelAgentEmail) {


        log.info(
                "updating travel agent report with review (pk={}, sk={})",
                pk,
                sk
        );


        Query query = new Query(
                Criteria.where("pk").is(pk)
                        .and("sk").is(sk)
        );


        Update update = new Update();


        // DynamoDB:
        // tourDaily = if_not_exists(tourDaily, "DAILY#AGENT")
        update.setOnInsert(
                "tourDaily",
                "DAILY#AGENT"
        );


        // DynamoDB:
        // tourReportSk = if_not_exists(tourReportSk, now#email)
        update.setOnInsert(
                "tourReportSk",
                now + "#" + travelAgentEmail
        );


        // DynamoDB:
        // travelAgentName = if_not_exists(...)
        update.setOnInsert(
                "travelAgentName",
                travelAgentName
        );


        // DynamoDB:
        // travelAgentEmail = if_not_exists(...)
        update.setOnInsert(
                "travelAgentEmail",
                travelAgentEmail
        );


        updateReportReview(
                update,
                isNew,
                rateDiff,
                newRate
        );


        mongoTemplate.upsert(
                query,
                update,
                TravelAgentDailyReportData.class,
                "reports"
        );
        log.info(
                "travel agent report updated with review (pk={}, sk={})",
                pk,
                sk
        );
    }

    private void updateReportReview(
            Update update,
            boolean isNew,
            int rateDiff,
            int newRate) {


        if (isNew) {
            update.inc(
                    "feedbackCount",
                    1
            );
        }


        update.inc(
                "feedbackRatingSum",
                rateDiff
        );


        update.min(
                "minFeedbackRate",
                newRate
        );
    }
}

