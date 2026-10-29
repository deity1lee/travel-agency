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
public class TravelAgentWeeklyReportData extends ReportData {
    private String travelAgentName;
    private String travelAgentEmail;

    public static String gsiPk() {
        return "WEEKLY#AGENT";
    }

    public static String gsiSk(LocalDate date, String tourId) {
        return String.join("#", date.toString(), tourId);
    }

    public static String sk(LocalDate date) {
        return String.join("#", "WEEKLY", date.toString());
    }

    public static String pk(String travelAgentEmail) {
        return String.join("#", "AGENT", travelAgentEmail);
    }
}
