package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Vehicle;
import pl.warsztatpro.model.VehicleType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleRepository {

    public Vehicle insert(Vehicle vehicle) throws SQLException {
        String sql = """
                INSERT INTO vehicles (client_id, type, vin, registration_number, brand, model,
                    production_year, engine_capacity, fuel_type, engine_power, drive_type, body_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindVehicle(statement, vehicle);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    vehicle.setId(keys.getLong(1));
                }
            }
            return vehicle;
        }
    }

    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicles ORDER BY id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        }
    }

    public List<Vehicle> search(String query) throws SQLException {
        String sql = """
                SELECT * FROM vehicles
                WHERE LOWER(IFNULL(vin, '')) LIKE ?
                   OR LOWER(IFNULL(registration_number, '')) LIKE ?
                   OR LOWER(IFNULL(brand, '')) LIKE ?
                   OR LOWER(IFNULL(model, '')) LIKE ?
                ORDER BY id
                """;
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    public Optional<Vehicle> findById(long id) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapVehicle(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<Vehicle> findByClientId(long clientId) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE client_id = ? ORDER BY id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    public int countByClientId(long clientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE client_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }

    public boolean existsVin(String vin, Long excludeId) throws SQLException {
        String sql = excludeId == null
                ? "SELECT 1 FROM vehicles WHERE vin = ? LIMIT 1"
                : "SELECT 1 FROM vehicles WHERE vin = ? AND id <> ? LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vin);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean update(Vehicle vehicle) throws SQLException {
        String sql = """
                UPDATE vehicles
                SET client_id = ?, type = ?, vin = ?, registration_number = ?, brand = ?, model = ?,
                    production_year = ?, engine_capacity = ?, fuel_type = ?, engine_power = ?,
                    drive_type = ?, body_type = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindVehicle(statement, vehicle);
            statement.setLong(13, vehicle.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bindVehicle(PreparedStatement statement, Vehicle vehicle) throws SQLException {
        statement.setLong(1, vehicle.getClientId());
        statement.setString(2, vehicle.getType().name());
        statement.setString(3, emptyToNull(vehicle.getVin()));
        statement.setString(4, vehicle.getRegistrationNumber());
        statement.setString(5, vehicle.getBrand());
        statement.setString(6, vehicle.getModel());
        statement.setInt(7, vehicle.getProductionYear());
        setNullableDouble(statement, 8, vehicle.getEngineCapacity());
        statement.setString(9, emptyToNull(vehicle.getFuelType()));
        setNullableDouble(statement, 10, vehicle.getEnginePower());
        statement.setString(11, emptyToNull(vehicle.getDriveType()));
        statement.setString(12, emptyToNull(vehicle.getBodyType()));
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.REAL);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static List<Vehicle> mapAll(ResultSet resultSet) throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        while (resultSet.next()) {
            vehicles.add(mapVehicle(resultSet));
        }
        return vehicles;
    }

    private static Vehicle mapVehicle(ResultSet resultSet) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(resultSet.getLong("id"));
        vehicle.setClientId(resultSet.getLong("client_id"));
        vehicle.setType(VehicleType.valueOf(resultSet.getString("type")));
        vehicle.setVin(resultSet.getString("vin"));
        vehicle.setRegistrationNumber(resultSet.getString("registration_number"));
        vehicle.setBrand(resultSet.getString("brand"));
        vehicle.setModel(resultSet.getString("model"));
        vehicle.setProductionYear(resultSet.getInt("production_year"));
        vehicle.setEngineCapacity(getNullableDouble(resultSet, "engine_capacity"));
        vehicle.setFuelType(resultSet.getString("fuel_type"));
        vehicle.setEnginePower(getNullableDouble(resultSet, "engine_power"));
        vehicle.setDriveType(resultSet.getString("drive_type"));
        vehicle.setBodyType(resultSet.getString("body_type"));
        return vehicle;
    }

    private static Double getNullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
