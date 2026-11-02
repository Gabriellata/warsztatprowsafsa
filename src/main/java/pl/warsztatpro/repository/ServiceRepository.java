package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Service;
import pl.warsztatpro.model.ServiceCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceRepository {
    public Service insert(Service service) throws SQLException {
        String sql = "INSERT INTO services(name, description, category, price_small, price_medium, price_large, active) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, service); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) service.setId(keys.getLong(1)); }
            return service;
        }
    }

    public List<Service> findAll() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT * FROM services ORDER BY name");
             ResultSet rs = s.executeQuery()) {
            return mapAll(rs);
        }
    }
    public List<Service> search(String name) throws SQLException {
        String sql = "SELECT * FROM services WHERE LOWER(name) LIKE ? ORDER BY name";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, "%" + name.toLowerCase(java.util.Locale.ROOT) + "%");
            try (ResultSet rs = s.executeQuery()) { return mapAll(rs); }
        }
    }
    public Optional<Service> findById(long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT * FROM services WHERE id=?")) {
            s.setLong(1, id); try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }
    public boolean update(Service service) throws SQLException {
        String sql = "UPDATE services SET name=?, description=?, category=?, price_small=?, price_medium=?, price_large=?, active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, service); s.setLong(8, service.getId()); return s.executeUpdate() > 0;
        }
    }
    public boolean deleteById(long id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM services WHERE id=?")) {
            s.setLong(1, id); return s.executeUpdate() > 0;
        }
    }
    public boolean setActive(long id, boolean active) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE services SET active=? WHERE id=?")) {
            s.setInt(1, active ? 1 : 0); s.setLong(2, id); return s.executeUpdate() > 0;
        }
    }
    private static void bind(PreparedStatement s, Service v) throws SQLException {
        s.setString(1, v.getName()); s.setString(2, v.getDescription()); s.setString(3, v.getCategory().name());
        s.setLong(4, v.getPriceSmall()); s.setLong(5, v.getPriceMedium()); s.setLong(6, v.getPriceLarge()); s.setInt(7, v.isActive() ? 1 : 0);
    }
    private static List<Service> mapAll(ResultSet rs) throws SQLException {
        List<Service> list = new ArrayList<>(); while (rs.next()) list.add(map(rs)); return list;
    }
    private static Service map(ResultSet rs) throws SQLException {
        Service v = new Service(); v.setId(rs.getLong("id")); v.setName(rs.getString("name")); v.setDescription(rs.getString("description"));
        v.setCategory(ServiceCategory.valueOf(rs.getString("category"))); v.setPriceSmall(rs.getLong("price_small"));
        v.setPriceMedium(rs.getLong("price_medium")); v.setPriceLarge(rs.getLong("price_large")); v.setActive(rs.getInt("active") == 1); return v;
    }
}
