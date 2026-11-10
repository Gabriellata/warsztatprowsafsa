package pl.warsztatpro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkReportRow(
        String orderNumber,
        LocalDate receivedDate,
        LocalDateTime scheduledAt,
        LocalDate plannedCompletionDate,
        String client,
        String registrationNumber,
        String vehicle,
        String status,
        String workshop,
        String faultDescription,
        String workDescription,
        long totalCostGrosze) {
}
