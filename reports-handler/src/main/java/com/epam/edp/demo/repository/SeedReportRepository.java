package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class SeedReportRepository {

    private final MongoTemplate mongoTemplate;

    public SeedReportRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void saveDailyTourReports(List<TourDailyReportData> reports) {
        log.info("saving daily tour reports, total={}", reports.size());
        BulkOperations bulk =
                mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        TourDailyReportData.class,
                        "reports"
                );
        for (TourDailyReportData report : reports) {

            Query query = new Query(
                    Criteria.where("pk")
                            .is(report.getPk())
                            .and("sk")
                            .is(report.getSk())
            );
            bulk.replaceOne(
                    query,
                    report,
                    FindAndReplaceOptions.options()
                            .upsert()
            );
        }
        bulk.execute();
    }

    public void saveDailyTaReports(List<TravelAgentDailyReportData> reports) {
        log.info("saving daily travel agent reports, total={}", reports.size());
        BulkOperations bulk =
                mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        TravelAgentDailyReportData.class,
                        "reports"
                );

        for (TravelAgentDailyReportData report : reports) {

            Query query = new Query(
                    Criteria.where("pk")
                            .is(report.getPk())
                            .and("sk")
                            .is(report.getSk())
            );
            bulk.replaceOne(
                    query,
                    report,
                    FindAndReplaceOptions.options()
                            .upsert()
            );
        }
        bulk.execute();
    }
}
