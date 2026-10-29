package com.epam.edp.demo.dto.reports;

import java.util.List;

public record ReportResponse(
        List<AggregateReport> report
) {
}
