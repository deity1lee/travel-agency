package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.*;
import com.epam.edp.demo.repository.ReportRepository;
import com.epam.edp.demo.repository.SeedReportRepository;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
@Slf4j
public class SeedService {

    private final Faker faker = new Faker(new Locale("en"));
    private final Random random = new Random();

    private final ReportRepository repository;
    private final SeedReportRepository seedReportRepository;

    public SeedService(ReportRepository repository,
                       SeedReportRepository seedReportRepository) {
        this.repository = repository;
        this.seedReportRepository = seedReportRepository;
    }

    /**
     * Generates weekly report data (tour and ta) for date (endDate - 7days) i.e. week starting on (endDate - 14days) and ending on (endDate - 7days).
     * The sk of TravelAgentWeeklyReportData and TourWeeklyReportData will be WEEKLY#(endDate - 7days)
     * <p>
     * Generates daily report data (tour and ta) from date (startDate) to (endDate)
     * @param startDate
     * @param endDate
     */
    public void seedReports(LocalDate startDate, LocalDate endDate, Integer agentCount, Integer tourCount) {
        LocalDate date = endDate.minusDays(7);

        List<TravelAgentSeed> agents = buildTravelAgents(agentCount);
        List<TourSeed> tours = buildTours(tourCount);

        List<TravelAgentWeeklyReportData> taReports = generateWeeklyTaReportData(date, agents);
        List<TourWeeklyReportData> tourReports = generateWeeklyTourReportData(date, tours);
        List<TravelAgentDailyReportData> taDailyReports = generateDailyTaReportData(startDate, endDate, agents);
        List<TourDailyReportData> tourDailyReports = generateDailyTourReportData(startDate, endDate, tours);

        log.info("Saving {} daily tour reports", tourDailyReports.size());
        seedReportRepository.saveDailyTourReports(tourDailyReports);

        log.info("Saving {} daily ta reports", taDailyReports.size());
        seedReportRepository.saveDailyTaReports(taDailyReports);

        log.info("Saving {} weekly tour reports", tourReports.size());
        repository.saveWeeklyTourReports(tourReports);

        log.info("Saving {} weekly ta reports", taReports.size());
        repository.saveWeeklyTaReports(taReports);
    }

    private List<TravelAgentWeeklyReportData> generateWeeklyTaReportData(LocalDate endDate,
                                                                         List<TravelAgentSeed> agents) {
        List<TravelAgentWeeklyReportData> result = new ArrayList<>();

        for (TravelAgentSeed agent : agents) {
            ReportMetrics metrics = generateReportMetrics(
                    10,
                    120,
                    3,
                    1,
                    2,
                    300,
                    3000
            );

            TravelAgentWeeklyReportData report = new TravelAgentWeeklyReportData();
            report.setPk(TravelAgentWeeklyReportData.pk(agent.email()));
            report.setSk(TravelAgentWeeklyReportData.sk(endDate));

            report.setTravelAgentName(agent.name());
            report.setTravelAgentEmail(agent.email());

            setCommonFields(
                    report,
                    metrics.feedbackCount(),
                    metrics.minFeedbackRate(),
                    metrics.confirmedToursRevenue(),
                    metrics.finishedToursRevenue(),
                    metrics.cancelledToursRevenue(),
                    metrics.cancelledToursLoss(),
                    metrics.confirmedToursCount(),
                    metrics.finishedToursCount(),
                    metrics.cancelledToursCount()
            );

            report.setTourDaily(TravelAgentWeeklyReportData.gsiPk());
            report.setTourReportSk(TravelAgentWeeklyReportData.gsiSk(endDate, agent.email()));
            result.add(report);
        }


        return result;
    }

    private List<TourWeeklyReportData> generateWeeklyTourReportData(LocalDate endDate,
                                                                     List<TourSeed> tours) {
        List<TourWeeklyReportData> result = new ArrayList<>();

        for (TourSeed tour : tours) {
            ReportMetrics metrics = generateReportMetrics(
                    8,
                    90,
                    4,
                    1,
                    2,
                    250,
                    3500
            );

            TourWeeklyReportData report = new TourWeeklyReportData();
            report.setPk(TourWeeklyReportData.pk(tour.id()));
            report.setSk(TourWeeklyReportData.sk(endDate));

            report.setTourId(tour.id());
            report.setTourName(tour.name());
            report.setDestination(tour.destination());

            setCommonFields(
                    report,
                    metrics.feedbackCount(),
                    metrics.minFeedbackRate(),
                    metrics.confirmedToursRevenue(),
                    metrics.finishedToursRevenue(),
                    metrics.cancelledToursRevenue(),
                    metrics.cancelledToursLoss(),
                    metrics.confirmedToursCount(),
                    metrics.finishedToursCount(),
                    metrics.cancelledToursCount()
            );

            report.setTourDaily(TourWeeklyReportData.gsiPk());
            report.setTourReportSk(TourWeeklyReportData.gsiSk(endDate, tour.id()));
            result.add(report);
        }


        return result;
    }

    private List<TravelAgentDailyReportData> generateDailyTaReportData(LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       List<TravelAgentSeed> agents) {
        int between = (int) ChronoUnit.DAYS.between(startDate, endDate);
        List<TravelAgentDailyReportData> result = new ArrayList<>(agents.size() * between);

        for (int dayOffset = 0; dayOffset <= between; dayOffset++) {
            LocalDate day = startDate.plusDays(dayOffset);

            for (TravelAgentSeed agent : agents) {
                ReportMetrics metrics = generateReportMetrics(
                        1,
                        25,
                        3,
                        0,
                        1,
                        150,
                        2200
                );

                TravelAgentDailyReportData report = new TravelAgentDailyReportData();
                report.setPk(TravelAgentDailyReportData.pk(agent.email()));
                report.setSk(TravelAgentDailyReportData.sk(day));

                report.setTravelAgentName(agent.name());
                report.setTravelAgentEmail(agent.email());

                setCommonFields(
                        report,
                        metrics.feedbackCount(),
                        metrics.minFeedbackRate(),
                        metrics.confirmedToursRevenue(),
                        metrics.finishedToursRevenue(),
                        metrics.cancelledToursRevenue(),
                        metrics.cancelledToursLoss(),
                        metrics.confirmedToursCount(),
                        metrics.finishedToursCount(),
                        metrics.cancelledToursCount()
                );

                report.setTourDaily("DAILY#AGENT");
                report.setTourReportSk(day + "#" + agent.email());
                result.add(report);
            }
        }

        return result;
    }

    private List<TourDailyReportData> generateDailyTourReportData(LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  List<TourSeed> tours) {
        int between = (int) ChronoUnit.DAYS.between(startDate, endDate);
        List<TourDailyReportData> result = new ArrayList<>(tours.size() * between);

        for (int dayOffset = 0; dayOffset <= between; dayOffset++) {
            LocalDate day = startDate.plusDays(dayOffset);

            for (TourSeed tour : tours) {
                ReportMetrics metrics = generateReportMetrics(
                        1,
                        20,
                        4,
                        0,
                        1,
                        120,
                        2600
                );

                TourDailyReportData report = new TourDailyReportData();
                report.setPk(TourDailyReportData.pk(tour.id()));
                report.setSk(TourDailyReportData.sk(day));

                report.setTourId(tour.id());
                report.setTourName(tour.name());
                report.setDestination(tour.destination());

                setCommonFields(
                        report,
                        metrics.feedbackCount(),
                        metrics.minFeedbackRate(),
                        metrics.confirmedToursRevenue(),
                        metrics.finishedToursRevenue(),
                        metrics.cancelledToursRevenue(),
                        metrics.cancelledToursLoss(),
                        metrics.confirmedToursCount(),
                        metrics.finishedToursCount(),
                        metrics.cancelledToursCount()
                );

                report.setTourDaily("DAILY#TOUR");
                report.setTourReportSk(day + "#" + tour.id());
                result.add(report);
            }
        }

        return result;
    }

    private void setCommonFields(ReportData report,
                                 int feedbackCount,
                                 int minFeedbackRate,
                                 BigDecimal confirmedToursRevenue,
                                 BigDecimal finishedToursRevenue,
                                 BigDecimal cancelledToursRevenue,
                                 BigDecimal cancelledToursLoss,
                                 int confirmedToursCount,
                                 int finishedToursCount,
                                 int cancelledToursCount) {
        report.setConfirmedToursCount(confirmedToursCount);
        report.setFinishedToursCount(finishedToursCount);
        report.setCancelledToursCount(cancelledToursCount);

        report.setConfirmedToursRevenue(confirmedToursRevenue);
        report.setFinishedToursRevenue(finishedToursRevenue);
        report.setCancelledToursRevenue(cancelledToursRevenue);
        report.setCancelledToursLoss(cancelledToursLoss);

        report.setFeedbackCount(feedbackCount);
        report.setFeedbackRatingSum(feedbackCount == 0 ? 0 : feedbackCount * randomBetween(minFeedbackRate, 5));
        report.setMinFeedbackRate(feedbackCount == 0 ? 0 : minFeedbackRate);
    }

    private ReportMetrics generateReportMetrics(int minConfirmedToursCount,
                                                int maxConfirmedToursCount,
                                                int cancelledToursDivider,
                                                int minFeedbackCount,
                                                int minFeedbackMax,
                                                double minTourPrice,
                                                double maxTourPrice) {
        int confirmedToursCount = randomBetween(minConfirmedToursCount, maxConfirmedToursCount);
        int finishedToursCount = randomBetween(0, confirmedToursCount);
        int cancelledToursCount = randomBetween(0, Math.max(1, confirmedToursCount / cancelledToursDivider));

        int feedbackCount = randomBetween(minFeedbackCount, Math.max(minFeedbackMax, finishedToursCount));
        int minFeedbackRate = randomBetween(1, 5);

        BigDecimal avgTourPrice = randomMoney(minTourPrice, maxTourPrice);
        BigDecimal confirmedToursRevenue = avgTourPrice.multiply(BigDecimal.valueOf(confirmedToursCount));
        BigDecimal finishedToursRevenue = avgTourPrice.multiply(BigDecimal.valueOf(finishedToursCount));
        BigDecimal cancelledToursRevenue = avgTourPrice.multiply(BigDecimal.valueOf(cancelledToursCount));
        BigDecimal cancelledToursLoss = cancelledToursRevenue.multiply(randomMoney(0.2, 0.8));

        return new ReportMetrics(
                confirmedToursCount,
                finishedToursCount,
                cancelledToursCount,
                feedbackCount,
                minFeedbackRate,
                confirmedToursRevenue,
                finishedToursRevenue,
                cancelledToursRevenue,
                cancelledToursLoss
        );
    }

    private List<TravelAgentSeed> buildTravelAgents(int agentCount) {
        List<TravelAgentSeed> agents = new ArrayList<>(agentCount);

        for (int i = 0; i < agentCount; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String fullName = firstName + " " + lastName;
            String email = (firstName + "." + lastName + "." + (i + 1) + "@example.com")
                    .toLowerCase(Locale.ROOT)
                    .replace("'", "")
                    .replace(" ", "");
            agents.add(new TravelAgentSeed(fullName, email));
        }

        return agents;
    }

    private List<TourSeed> buildTours(int tourCount) {
        List<TourSeed> tours = new ArrayList<>(tourCount);

        for (int i = 0; i < tourCount; i++) {
            String city = faker.address().cityName();
            String country = faker.address().country();
            String destination = city + ", " + country;
            String tourName = faker.company().buzzword() + " Explorer #" + (i + 1);
            String tourId = "tour-" + String.format("%03d", i + 1);
            tours.add(new TourSeed(tourId, tourName, destination));
        }

        return tours;
    }

    private int randomBetween(int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    private BigDecimal randomMoney(double minInclusive, double maxInclusive) {
        double value = minInclusive + (maxInclusive - minInclusive) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record ReportMetrics(int confirmedToursCount,
                                 int finishedToursCount,
                                 int cancelledToursCount,
                                 int feedbackCount,
                                 int minFeedbackRate,
                                 BigDecimal confirmedToursRevenue,
                                 BigDecimal finishedToursRevenue,
                                 BigDecimal cancelledToursRevenue,
                                 BigDecimal cancelledToursLoss) {
    }

    private record TravelAgentSeed(String name, String email) {}

    private record TourSeed(String id, String name, String destination) {}
}
