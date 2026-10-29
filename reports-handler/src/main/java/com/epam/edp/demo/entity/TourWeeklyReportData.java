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
public class TourWeeklyReportData extends ReportData {
    private String tourId;
    private String tourName;
    private String destination;

    public static String gsiPk() {
        return "WEEKLY#TOUR";
    }

    public static String gsiSk(LocalDate date, String email) {
        return String.join("#", date.toString(), email);
    }

    public static String sk(LocalDate date) {
        return String.join("#", "WEEKLY", date.toString());
    }

    public static String pk(String tourId) {
        return String.join("#", "TOUR", tourId);
    }
}
