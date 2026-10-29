package com.epam.edp.demo.export;

import java.util.List;

public class PerformanceReportColumnOrder implements CsvColumnOrder {
    @Override
    public List<String> columns() {
        return List.of(
                "Travel Agent",
                "TA e-mail",
                "Report period start",
                "Report period end",
                "Tours sold",
                "Delta of tours sold to previous period %",
                "Average Feedback rate for Travel experience (1 to 5)",
                "Minimum Feedback rate for Travel experience (1 to 5)",
                "Delta Average Feedback rate for Travel experience to previous period %",
                "Revenue for tours finished within reported period (USD)",
                "Delta for revenue for tours finished to previous period %"
        );
    }
}
