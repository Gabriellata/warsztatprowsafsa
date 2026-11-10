package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read-only database aggregates used by the reports menu. */
public class ReportRepository {
    public List<WorkReportRow> workReport(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT o.order_number,o.received_date,o.scheduled_at,o.planned_completion_date,"
                + "CASE WHEN c.company_name IS NOT NULL AND TRIM(c.company_name)<>'' THEN c.company_name "
                + "ELSE TRIM(COALESCE(c.first_name,'')||' '||COALESCE(c.last_name,'')) END client,"
                + "v.registration_number,TRIM(v.brand||' '||v.model) vehicle,o.status,w.name workshop,"
                + "o.fault_description,o.work_description,o.total_cost "
                + "FROM service_orders o LEFT JOIN clients c ON c.id=o.client_id "
                + "JOIN vehicles v ON v.id=o.vehicle_id LEFT JOIN workshops w ON w.id=o.workshop_id "
                + "WHERE o.received_date BETWEEN ? AND ? ORDER BY o.received_date,o.order_number";
        List<WorkReportRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bindRange(s, from, to);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) rows.add(new WorkReportRow(
                        rs.getString("order_number"), LocalDate.parse(rs.getString("received_date")),
                        parseDateTime(rs.getString("scheduled_at")), parseDate(rs.getString("planned_completion_date")),
                        blankAs(rs.getString("client"), "Bez przypisania"), rs.getString("registration_number"),
                        rs.getString("vehicle"), ServiceOrderStatus.valueOf(rs.getString("status")).toString(), blankAs(rs.getString("workshop"), "Bez przypisania"),
                        rs.getString("fault_description"), rs.getString("work_description"), rs.getLong("total_cost")));
            }
        }
        return rows;
    }

    private static LocalDate parseDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
    private static java.time.LocalDateTime parseDateTime(String value) { return value == null || value.isBlank() ? null : java.time.LocalDateTime.parse(value); }
    private static String blankAs(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    public long countOrders(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM service_orders WHERE received_date BETWEEN ? AND ?")) {
            bindRange(s, from, to);
            try (ResultSet rs = s.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    public List<OrderStatusReport> ordersByStatus(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT status,COUNT(*) count FROM service_orders WHERE received_date BETWEEN ? AND ? GROUP BY status";
        List<OrderStatusReport> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bindRange(s, from, to);
            try (ResultSet rs = s.executeQuery()) { while (rs.next()) rows.add(new OrderStatusReport(ServiceOrderStatus.valueOf(rs.getString("status")), rs.getLong("count"))); }
        }
        return rows;
    }

    public List<ServiceCategoryReport> servicesByCategory(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COALESCE(s.category,'BRAK_KATEGORII') category,COUNT(*) line_count," +
                "COALESCE(SUM(i.quantity),0) quantity,COALESCE(SUM(i.total_value),0) value " +
                "FROM service_order_items i JOIN service_orders o ON o.id=i.order_id " +
                "LEFT JOIN services s ON s.id=i.service_id WHERE o.received_date BETWEEN ? AND ? GROUP BY s.category ORDER BY category";
        List<ServiceCategoryReport> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bindRange(s, from, to);
            try (ResultSet rs = s.executeQuery()) { while (rs.next()) {
                String raw = rs.getString("category");
                String label;
                try { label = ServiceCategory.valueOf(raw).toString(); } catch (IllegalArgumentException | NullPointerException e) { label = "Brak kategorii"; }
                rows.add(new ServiceCategoryReport(label, rs.getLong("line_count"), rs.getLong("quantity"), rs.getLong("value")));
            }}
        }
        return rows;
    }

    public OrderFinancialReport financialSummary(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COUNT(*) order_count,COALESCE(SUM(total_cost),0) order_value," +
                "COALESCE((SELECT SUM(i.total_value) FROM service_order_items i JOIN service_orders oi ON oi.id=i.order_id WHERE oi.received_date BETWEEN ? AND ?),0) service_value " +
                "FROM service_orders WHERE received_date BETWEEN ? AND ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, from.toString()); s.setString(2, to.toString()); s.setString(3, from.toString()); s.setString(4, to.toString());
            try (ResultSet rs = s.executeQuery()) { rs.next(); return new OrderFinancialReport(rs.getLong("order_count"), rs.getLong("service_value"), rs.getLong("order_value")); }
        }
    }

    public OrderLifecycleReport lifecycleSummary(LocalDate from, LocalDate to, LocalDate asOf) throws SQLException {
        String sql = "SELECT COUNT(*) total," +
                "SUM(CASE WHEN status='NOWE' THEN 1 ELSE 0 END) new_count," +
                "SUM(CASE WHEN status IN ('W_TRAKCIE','OCZEKUJE_NA_CZESCI','GOTOWE_DO_ODBIORU') THEN 1 ELSE 0 END) progress_count," +
                "SUM(CASE WHEN status='ZAKONCZONE' THEN 1 ELSE 0 END) completed_count," +
                "SUM(CASE WHEN status='ANULOWANE' THEN 1 ELSE 0 END) cancelled_count," +
                "SUM(CASE WHEN planned_completion_date IS NOT NULL AND planned_completion_date < ? AND status NOT IN ('ZAKONCZONE','ANULOWANE') THEN 1 ELSE 0 END) overdue_count " +
                "FROM service_orders WHERE received_date BETWEEN ? AND ?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, asOf.toString()); bindRange(s, 2, from, to);
            try (ResultSet rs = s.executeQuery()) { rs.next(); return new OrderLifecycleReport(rs.getLong("total"), rs.getLong("new_count"), rs.getLong("progress_count"), rs.getLong("completed_count"), rs.getLong("overdue_count"), rs.getLong("cancelled_count")); }
        }
    }

    private static void bindRange(PreparedStatement s, LocalDate from, LocalDate to) throws SQLException { bindRange(s, 1, from, to); }
    private static void bindRange(PreparedStatement s, int first, LocalDate from, LocalDate to) throws SQLException {
        s.setString(first, from.toString()); s.setString(first + 1, to.toString());
    }
}
