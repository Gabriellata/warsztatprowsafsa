package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Workshop;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkshopRepository {
    public Workshop insert(Workshop workshop) throws SQLException {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(
                "INSERT INTO workshops(name,description,active) VALUES(?,?,1)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,workshop.getName()); s.setString(2,workshop.getDescription()); s.executeUpdate();
            try(ResultSet keys=s.getGeneratedKeys()){if(keys.next())workshop.setId(keys.getLong(1));}
            return workshop;
        }
    }
    public List<Workshop> findAll() throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM workshops ORDER BY active DESC,name COLLATE NOCASE,id"); ResultSet rs=s.executeQuery()) {
            List<Workshop> out=new ArrayList<>(); while(rs.next())out.add(map(rs)); return out;
        }
    }
    public List<Workshop> findActive() throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM workshops WHERE active=1 ORDER BY name COLLATE NOCASE,id"); ResultSet rs=s.executeQuery()) {
            List<Workshop> out=new ArrayList<>(); while(rs.next())out.add(map(rs)); return out;
        }
    }
    public Optional<Workshop> findById(long id) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM workshops WHERE id=?")) {
            s.setLong(1,id); try(ResultSet rs=s.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}
        }
    }
    public boolean existsByName(String name, Long excludeId) throws SQLException {
        String sql=excludeId==null?"SELECT 1 FROM workshops WHERE name=? COLLATE NOCASE":"SELECT 1 FROM workshops WHERE name=? COLLATE NOCASE AND id<>?";
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,name); if(excludeId!=null)s.setLong(2,excludeId); try(ResultSet rs=s.executeQuery()){return rs.next();}
        }
    }
    public boolean update(Workshop workshop) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("UPDATE workshops SET name=?,description=? WHERE id=?")) {
            s.setString(1,workshop.getName()); s.setString(2,workshop.getDescription()); s.setLong(3,workshop.getId()); return s.executeUpdate()>0;
        }
    }
    public boolean deactivate(long id) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("UPDATE workshops SET active=0 WHERE id=? AND active=1")) {
            s.setLong(1,id); return s.executeUpdate()>0;
        }
    }
    public boolean isAssignedToScheduledOrders(long id) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT 1 FROM service_orders WHERE workshop_id=? AND scheduled_at IS NOT NULL LIMIT 1")) {
            s.setLong(1,id); try(ResultSet rs=s.executeQuery()){return rs.next();}
        }
    }
    private static Workshop map(ResultSet rs)throws SQLException {
        Workshop w=new Workshop();w.setId(rs.getLong("id"));w.setName(rs.getString("name"));w.setDescription(rs.getString("description"));w.setActive(rs.getInt("active")==1);return w;
    }
}
