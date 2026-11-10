package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.VehicleType;
import pl.warsztatpro.model.VehicleTypeOption;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleTypeRepository {
    public List<VehicleTypeOption> findAll() throws SQLException {
        List<VehicleTypeOption> options = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT code,active FROM vehicle_types ORDER BY code"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) options.add(new VehicleTypeOption(VehicleType.valueOf(rs.getString("code")), rs.getInt("active") == 1));
        }
        return options;
    }
    public boolean isActive(VehicleType type) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT active FROM vehicle_types WHERE code=?")) {
            s.setString(1, type.name()); try (ResultSet rs = s.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }
    public boolean setActive(VehicleType type, boolean active) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE vehicle_types SET active=? WHERE code=?")) {
            s.setInt(1, active ? 1 : 0); s.setString(2, type.name()); return s.executeUpdate() == 1;
        }
    }
}
