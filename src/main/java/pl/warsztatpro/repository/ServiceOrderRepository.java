package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.OrderItem;
import pl.warsztatpro.model.ServiceOrder;
import pl.warsztatpro.model.ServiceOrderStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceOrderRepository {
    private final OrderItemRepository itemRepository = new OrderItemRepository();

    public ServiceOrder insert(ServiceOrder order) throws SQLException {
        String sql = "INSERT INTO service_orders(order_number,client_id,vehicle_id,received_date,planned_completion_date,actual_completion_date,fault_description,work_description,status,total_cost,notes) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(s, order); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) order.setId(keys.getLong(1)); }
            }
            try {
                for (OrderItem item : order.getItems()) { item.setOrderId(order.getId()); itemRepository.insert(c, item); }
                refreshTotal(c, order.getId());
                c.commit();
                order.setItems(itemRepository.findByOrderId(c, order.getId()));
                order.setTotalCost(sum(order.getItems()));
                return order;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public List<ServiceOrder> findAll() throws SQLException {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM service_orders ORDER BY received_date DESC,id DESC"); ResultSet rs=s.executeQuery()) { return mapAll(rs); }
    }
    public Optional<ServiceOrder> findById(long id) throws SQLException {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM service_orders WHERE id=?")) {
            s.setLong(1,id); try(ResultSet rs=s.executeQuery()){ return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        }
    }
    public Optional<ServiceOrder> findByNumber(String number) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM service_orders WHERE LOWER(order_number)=LOWER(?)")){
            s.setString(1,number);try(ResultSet rs=s.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}
        }
    }
    public List<ServiceOrder> findByClient(long clientId) throws SQLException { return findByForeignKey("client_id",clientId); }
    public List<ServiceOrder> findByVehicle(long vehicleId) throws SQLException { return findByForeignKey("vehicle_id",vehicleId); }
    public boolean update(ServiceOrder o) throws SQLException {
        String sql="UPDATE service_orders SET client_id=?,vehicle_id=?,received_date=?,planned_completion_date=?,actual_completion_date=?,fault_description=?,work_description=?,notes=? WHERE id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){
            s.setLong(1,o.getClientId());s.setLong(2,o.getVehicleId());s.setString(3,o.getReceivedDate().toString());setDate(s,4,o.getPlannedCompletionDate());setDate(s,5,o.getActualCompletionDate());s.setString(6,o.getFaultDescription());s.setString(7,o.getWorkDescription());s.setString(8,o.getNotes());s.setLong(9,o.getId());return s.executeUpdate()>0;
        }
    }
    public boolean updateStatus(long id, ServiceOrderStatus status, LocalDate actualDate) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE service_orders SET status=?,actual_completion_date=? WHERE id=?")){
            s.setString(1,status.name());setDate(s,2,actualDate);s.setLong(3,id);return s.executeUpdate()>0;
        }
    }
    public boolean deleteIfEmpty(long id) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM service_orders WHERE id=? AND NOT EXISTS (SELECT 1 FROM service_order_items WHERE order_id=?)")){
            s.setLong(1,id);s.setLong(2,id);return s.executeUpdate()>0;
        }
    }
    public void addItem(long orderId, OrderItem item) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) { c.setAutoCommit(false); try {
            item.setOrderId(orderId);itemRepository.insert(c,item);refreshTotal(c,orderId);c.commit();
        }catch(SQLException|RuntimeException e){c.rollback();throw e;} }
    }
    public boolean editItem(long orderId, OrderItem item) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{boolean changed=itemRepository.update(c,item);if(changed)refreshTotal(c,orderId);c.commit();return changed;}catch(SQLException|RuntimeException e){c.rollback();throw e;}}
    }
    public boolean removeItem(long orderId,long itemId) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{boolean changed=itemRepository.delete(c,orderId,itemId);if(changed)refreshTotal(c,orderId);c.commit();return changed;}catch(SQLException|RuntimeException e){c.rollback();throw e;}}
    }
    public List<OrderItem> findItems(long orderId) throws SQLException { try(Connection c=DatabaseConnection.getConnection()){return itemRepository.findByOrderId(c,orderId);} }

    private List<ServiceOrder> findByForeignKey(String column,long id)throws SQLException{
        // Column is selected only by the two internal callers above.
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM service_orders WHERE "+column+"=? ORDER BY received_date DESC,id DESC")){
            s.setLong(1,id);try(ResultSet rs=s.executeQuery()){return mapAll(rs);}
        }
    }
    private List<ServiceOrder> mapAll(ResultSet rs)throws SQLException{List<ServiceOrder> out=new ArrayList<>();while(rs.next())out.add(map(rs));return out;}
    private ServiceOrder map(ResultSet rs)throws SQLException{
        ServiceOrder o=new ServiceOrder();o.setId(rs.getLong("id"));o.setOrderNumber(rs.getString("order_number"));o.setClientId(rs.getLong("client_id"));o.setVehicleId(rs.getLong("vehicle_id"));
        o.setReceivedDate(LocalDate.parse(rs.getString("received_date")));o.setPlannedCompletionDate(parseDate(rs.getString("planned_completion_date")));o.setActualCompletionDate(parseDate(rs.getString("actual_completion_date")));
        o.setFaultDescription(rs.getString("fault_description"));o.setWorkDescription(rs.getString("work_description"));o.setStatus(ServiceOrderStatus.valueOf(rs.getString("status")));o.setTotalCost(rs.getLong("total_cost"));o.setNotes(rs.getString("notes"));
        o.setItems(itemRepository.findByOrderId(rs.getStatement().getConnection(),o.getId()));return o;
    }
    private static void bind(PreparedStatement s,ServiceOrder o)throws SQLException{
        s.setString(1,o.getOrderNumber());s.setLong(2,o.getClientId());s.setLong(3,o.getVehicleId());s.setString(4,o.getReceivedDate().toString());setDate(s,5,o.getPlannedCompletionDate());setDate(s,6,o.getActualCompletionDate());
        s.setString(7,o.getFaultDescription());s.setString(8,o.getWorkDescription());s.setString(9,o.getStatus().name());s.setLong(10,o.getTotalCost());s.setString(11,o.getNotes());
    }
    private static void setDate(PreparedStatement s,int index,LocalDate d)throws SQLException{if(d==null)s.setNull(index,Types.VARCHAR);else s.setString(index,d.toString());}
    private static LocalDate parseDate(String s){return s==null?null:LocalDate.parse(s);}
    private void refreshTotal(Connection c,long id)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("UPDATE service_orders SET total_cost=(SELECT COALESCE(SUM(total_value),0) FROM service_order_items WHERE order_id=?) WHERE id=?")){s.setLong(1,id);s.setLong(2,id);s.executeUpdate();}
    }
    private static long sum(List<OrderItem> items){long total=0;for(OrderItem i:items)total=Math.addExact(total,i.getTotalValue());return total;}
}
