package com.epam.edp.demo.entity;

import com.epam.edp.demo.util.Utils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

// tour is confirmed: confirmedToursCount++, confirmedToursRevenue += rev, bookingsCount++
// tour is finished: finishedToursCount++, finishedToursRevenue += rev
// tour is canceled: cancelledToursCount++, cancelledToursRevenue += rev (optional)
@Getter
@Setter
@NoArgsConstructor
public abstract class ReportData {

    @Field("pk")
    private String pk; // TOUR#tourId OR AGENT#email

    @Field("sk")
    private String sk; // DAILY#date OR WEEKLY#date
    protected int finishedToursCount;
    protected int confirmedToursCount;
    protected int cancelledToursCount;

    protected BigDecimal finishedToursRevenue;
    protected BigDecimal confirmedToursRevenue;
    protected BigDecimal cancelledToursRevenue;

    protected BigDecimal cancelledToursLoss;

    protected int feedbackCount;
    protected int feedbackRatingSum;
    protected int minFeedbackRate;

    protected String tourDaily;       // DAILY#TOUR OR DAILY#AGENT      OR          WEEKLY#TOUR OR WEEKLY#AGENT
    protected String tourReportSk;    // (eventDate#pk) eventDate#tourId OR eventDate#email

    public ReportData(ReportData r) {
        finishedToursCount = r.getFinishedToursCount();
        confirmedToursCount = r.getConfirmedToursCount();
        cancelledToursCount = r.getCancelledToursCount();

        finishedToursRevenue = Utils.nvl(r.getFinishedToursRevenue());
        confirmedToursRevenue = Utils.nvl(r.getConfirmedToursRevenue());
        cancelledToursRevenue = Utils.nvl(r.getCancelledToursRevenue());

        cancelledToursLoss = Utils.nvl(r.getCancelledToursLoss());

        feedbackCount = r.getFeedbackCount();
        feedbackRatingSum = r.getFeedbackRatingSum();
        minFeedbackRate = r.getMinFeedbackRate();
    }
}
