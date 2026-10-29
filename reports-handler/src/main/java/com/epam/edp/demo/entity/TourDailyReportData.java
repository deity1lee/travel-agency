package com.epam.edp.demo.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "reports")
public class TourDailyReportData extends ReportData {
    private String tourId;
    private String tourName;
    private String destination;

    public static String sk(LocalDate date) {
        return String.join("#", "DAILY", date.toString());
    }

    public static String pk(String tourId) {
        return String.join("#", "TOUR", tourId);
    }

    public TourDailyReportData(TourDailyReportData r) {
        super(r);
        tourId = r.getTourId();
        tourName = r.getTourName();
        destination = r.getDestination();
    }
}
