package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Appointment;
import pl.warsztatpro.model.AppointmentStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentRepository {
    public Appointment insert(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointments(client_id,vehicle_id,scheduled_at,duration_minutes,description,status,notes,workshop_id,service_order_id) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, appointment, false); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) appointment.setId(keys.getLong(1)); }
            return appointment;
        }
    }

    public List<Appointment> findAll() throws SQLException { return query("SELECT * FROM appointments ORDER BY scheduled_at,id", null); }
    public List<Appointment> findByDate(LocalDate date) throws SQLException {
        return query("SELECT * FROM appointments WHERE substr(scheduled_at,1,10)=? ORDER BY scheduled_at,id", date.toString());
    }
    public List<Appointment> findBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT * FROM appointments WHERE substr(scheduled_at,1,10)>=? AND substr(scheduled_at,1,10)<? ORDER BY scheduled_at,id")) {
            s.setString(1,fromInclusive.toLocalDate().toString()); s.setString(2,toExclusive.toLocalDate().toString());
            try(ResultSet rs=s.executeQuery()){List<Appointment> out=new ArrayList<>();while(rs.next())out.add(map(rs));return out;}
        }
    }
    public List<Appointment> findByWorkshopId(long workshopId) throws SQLException {
        return query("SELECT * FROM appointments WHERE workshop_id=? ORDER BY scheduled_at,id", Long.toString(workshopId));
    }
    public List<Appointment> findByServiceOrderId(long serviceOrderId) throws SQLException {
        return query("SELECT * FROM appointments WHERE service_order_id=? ORDER BY scheduled_at,id", Long.toString(serviceOrderId));
    }
    public List<Appointment> findByClientId(long clientId) throws SQLException {
        return query("SELECT * FROM appointments WHERE client_id=? ORDER BY scheduled_at,id", Long.toString(clientId));
    }
    public List<Appointment> findByVehicleId(long vehicleId) throws SQLException {
        return query("SELECT * FROM appointments WHERE vehicle_id=? ORDER BY scheduled_at,id", Long.toString(vehicleId));
    }
    public boolean updateServiceOrderId(long appointmentId,Long serviceOrderId)throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE appointments SET service_order_id=? WHERE id=?")) {
            if(serviceOrderId==null)s.setNull(1,Types.INTEGER);else s.setLong(1,serviceOrderId);
            s.setLong(2,appointmentId);return s.executeUpdate()>0;
        }
    }
    public Optional<Appointment> findById(long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM appointments WHERE id=?")) {
            s.setLong(1, id); try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }
    public boolean update(Appointment appointment) throws SQLException {
        String sql = "UPDATE appointments SET client_id=?,vehicle_id=?,scheduled_at=?,duration_minutes=?,description=?,status=?,notes=?,workshop_id=?,service_order_id=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, appointment, true); return s.executeUpdate() > 0;
        }
    }
    public boolean updateStatus(long id, AppointmentStatus status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE appointments SET status=? WHERE id=?")) {
            s.setString(1, status.name()); s.setLong(2, id); return s.executeUpdate() > 0;
        }
    }
    public boolean delete(long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM appointments WHERE id=?")) {
            s.setLong(1, id); return s.executeUpdate() > 0;
        }
    }
    private List<Appointment> query(String sql, String parameter) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (parameter != null) s.setString(1, parameter);
            try (ResultSet rs = s.executeQuery()) { List<Appointment> out = new ArrayList<>(); while (rs.next()) out.add(map(rs)); return out; }
        }
    }
    private static void bind(PreparedStatement s, Appointment a, boolean includeId) throws SQLException {
        if(a.getClientId()==null)s.setNull(1,Types.INTEGER);else s.setLong(1,a.getClientId()); s.setLong(2,a.getVehicleId()); s.setString(3,a.getScheduledAt().toString());
        s.setInt(4,a.getDurationMinutes()); s.setString(5,a.getDescription()); s.setString(6,a.getStatus().name()); s.setString(7,a.getNotes());
        if(a.getWorkshopId()==null)s.setNull(8,Types.INTEGER);else s.setLong(8,a.getWorkshopId());
        if(a.getServiceOrderId()==null)s.setNull(9,Types.INTEGER);else s.setLong(9,a.getServiceOrderId());
        if (includeId) s.setLong(10,a.getId());
    }
    private static Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment(); a.setId(rs.getLong("id")); long clientId=rs.getLong("client_id"); a.setClientId(rs.wasNull()?null:clientId); a.setVehicleId(rs.getLong("vehicle_id"));
        a.setScheduledAt(java.time.LocalDateTime.parse(rs.getString("scheduled_at"))); a.setDurationMinutes(rs.getInt("duration_minutes"));
        a.setDescription(rs.getString("description")); a.setStatus(AppointmentStatus.valueOf(rs.getString("status"))); a.setNotes(rs.getString("notes"));
        long workshopId=rs.getLong("workshop_id"); a.setWorkshopId(rs.wasNull()?null:workshopId);
        long orderId=rs.getLong("service_order_id"); a.setServiceOrderId(rs.wasNull()?null:orderId); return a;
    }
}
