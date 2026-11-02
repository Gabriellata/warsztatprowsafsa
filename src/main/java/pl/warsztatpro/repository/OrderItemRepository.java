package pl.warsztatpro.repository;

import pl.warsztatpro.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderItemRepository {
    public OrderItem insert(Connection connection, OrderItem item) throws SQLException {
        String sql = "INSERT INTO service_order_items(order_id, service_id, service_name, unit_price, quantity, total_value) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, item); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (keys.next()) item.setId(keys.getLong(1)); }
            return item;
        }
    }

    public List<OrderItem> findByOrderId(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM service_order_items WHERE order_id=? ORDER BY id")) {
            statement.setLong(1, orderId);
            try (ResultSet rs = statement.executeQuery()) { List<OrderItem> items = new ArrayList<>(); while (rs.next()) items.add(map(rs)); return items; }
        }
    }

    public boolean update(Connection connection, OrderItem item) throws SQLException {
        String sql = "UPDATE service_order_items SET unit_price=?, quantity=?, total_value=? WHERE id=? AND order_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.getUnitPrice()); statement.setInt(2, item.getQuantity()); statement.setLong(3, item.getTotalValue());
            statement.setLong(4, item.getId()); statement.setLong(5, item.getOrderId()); return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(Connection connection, long orderId, long itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM service_order_items WHERE order_id=? AND id=?")) {
            statement.setLong(1, orderId); statement.setLong(2, itemId); return statement.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement s, OrderItem i) throws SQLException {
        s.setLong(1, i.getOrderId()); s.setLong(2, i.getServiceId()); s.setString(3, i.getServiceName());
        s.setLong(4, i.getUnitPrice()); s.setInt(5, i.getQuantity()); s.setLong(6, i.getTotalValue());
    }
    private static OrderItem map(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem(); i.setId(rs.getLong("id")); i.setOrderId(rs.getLong("order_id")); i.setServiceId(rs.getLong("service_id"));
        i.setServiceName(rs.getString("service_name")); i.setUnitPrice(rs.getLong("unit_price")); i.setQuantity(rs.getInt("quantity")); i.setTotalValue(rs.getLong("total_value")); return i;
    }
}
