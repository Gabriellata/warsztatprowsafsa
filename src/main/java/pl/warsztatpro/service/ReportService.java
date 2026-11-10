package pl.warsztatpro.service;

import pl.warsztatpro.model.*;
import pl.warsztatpro.repository.ReportRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReportRepository reports;
    public ReportService(ReportRepository reports) { this.reports = reports; }

    public long countOrders(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.countOrders(from, to); }
    public List<OrderStatusReport> ordersByStatus(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.ordersByStatus(from, to); }
    public List<ServiceCategoryReport> servicesByCategory(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.servicesByCategory(from, to); }
    public OrderFinancialReport financialSummary(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.financialSummary(from, to); }
    public OrderLifecycleReport lifecycleSummary(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.lifecycleSummary(from, to, LocalDate.now()); }
    public List<WorkReportRow> workReport(LocalDate from, LocalDate to) throws SQLException { validateRange(from, to); return reports.workReport(from, to); }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Podaj datę początku i końca okresu.");
        if (to.isBefore(from)) throw new IllegalArgumentException("Data końcowa nie może być wcześniejsza niż data początkowa.");
    }
}
