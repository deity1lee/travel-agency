package com.epam.edp.demo.entity;

import com.epam.edp.demo.util.Utils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@Document(collection = "reports")
public class TravelAgentDailyReportData extends ReportData {
    private String travelAgentName;
    private String travelAgentEmail;

    public static String sk(LocalDate date) {
        return String.join("#", "DAILY", date.toString());
    }

    public static String pk(String travelAgentEmail) {
        return String.join("#", "AGENT", travelAgentEmail);
    }

    public TravelAgentDailyReportData(TravelAgentDailyReportData r) {
        super(r);
        travelAgentName = r.getTravelAgentName();
        travelAgentEmail = r.getTravelAgentEmail();
    }
}
