package pl.warsztatpro.service;

import pl.warsztatpro.model.*;
import pl.warsztatpro.repository.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServiceOrderService {
    private final ServiceOrderRepository orders;
    private final OrderItemRepository items;
    private final ClientRepository clients;
    private final VehicleRepository vehicles;
    private final ServiceRepository services;

    public ServiceOrderService(ServiceOrderRepository orders, OrderItemRepository items, ClientRepository clients,
                               VehicleRepository vehicles, ServiceRepository services) {
        this.orders=orders;this.items=items;this.clients=clients;this.vehicles=vehicles;this.services=services;
    }
    public ServiceOrder create(ServiceOrder order)throws SQLException{
        validateOrder(order);
        if(clients.findById(order.getClientId()).isEmpty())throw new IllegalArgumentException("Nie znaleziono klienta.");
        var vehicle=vehicles.findById(order.getVehicleId()).orElseThrow(()->new IllegalArgumentException("Nie znaleziono pojazdu."));
        if(vehicle.getClientId()!=order.getClientId())throw new IllegalArgumentException("Wybrany pojazd nie jest przypisany do wskazanego klienta.");
        for (OrderItem item : order.getItems()) {
            pl.warsztatpro.model.Service catalog = services.findById(item.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono usługi o ID: " + item.getServiceId()));
            if (!catalog.isActive()) throw new IllegalArgumentException("Do zlecenia można dodawać wyłącznie aktywne usługi.");
            if (item.getQuantity() <= 0) throw new IllegalArgumentException("Ilość usługi musi być większa od zera.");
            if (item.getUnitPrice() < 0) throw new IllegalArgumentException("Cena nie może być ujemna.");
            item.setServiceName(catalog.getName());
            item.setTotalValue(Math.multiplyExact(item.getUnitPrice(), item.getQuantity()));
        }
        order.setOrderNumber("ZL-"+YearMonth.now()+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());
        order.setStatus(ServiceOrderStatus.NOWE);order.setTotalCost(0);
        return orders.insert(order);
    }
    public List<ServiceOrder> findAll()throws SQLException{return orders.findAll();}
    public Optional<ServiceOrder> findByNumber(String number)throws SQLException{
        if(number==null||number.isBlank())throw new IllegalArgumentException("Numer zlecenia jest wymagany.");return orders.findByNumber(number.trim());
    }
    public Optional<ServiceOrder> findById(long id)throws SQLException{return orders.findById(id);}
    public List<ServiceOrder> historyForClient(long id)throws SQLException{
        if(clients.findById(id).isEmpty())throw new IllegalArgumentException("Nie znaleziono klienta.");return orders.findByClient(id);
    }
    public List<ServiceOrder> historyForVehicle(long id)throws SQLException{
        if(vehicles.findById(id).isEmpty())throw new IllegalArgumentException("Nie znaleziono pojazdu.");return orders.findByVehicle(id);
    }
    public ServiceOrder update(ServiceOrder order)throws SQLException{
        if(order.getId()==null||orders.findById(order.getId()).isEmpty())throw new IllegalArgumentException("Nie znaleziono zlecenia.");validateOrder(order);
        if(clients.findById(order.getClientId()).isEmpty())throw new IllegalArgumentException("Nie znaleziono klienta.");
        var vehicle=vehicles.findById(order.getVehicleId()).orElseThrow(()->new IllegalArgumentException("Nie znaleziono pojazdu."));
        if(vehicle.getClientId()!=order.getClientId())throw new IllegalArgumentException("Pojazd nie jest przypisany do wskazanego klienta.");
        if(!orders.update(order))throw new IllegalArgumentException("Nie udało się zaktualizować zlecenia.");return orders.findById(order.getId()).orElse(order);
    }
    public void changeStatus(long id,ServiceOrderStatus status)throws SQLException{
        ServiceOrder existing=orders.findById(id).orElseThrow(()->new IllegalArgumentException("Nie znaleziono zlecenia."));
        if(existing.getStatus()==ServiceOrderStatus.ZAKONCZONE||existing.getStatus()==ServiceOrderStatus.ANULOWANE)throw new IllegalArgumentException("Zlecenie zakończone lub anulowane nie może zmienić statusu.");
        LocalDate actual=status==ServiceOrderStatus.ZAKONCZONE?LocalDate.now():existing.getActualCompletionDate();
        if(!orders.updateStatus(id,status,actual))throw new IllegalArgumentException("Nie udało się zmienić statusu.");
    }
    public void deleteIfEmpty(long id)throws SQLException{
        if(!orders.deleteIfEmpty(id))throw new IllegalArgumentException("Zlecenie nie istnieje albo zawiera historię usług. Użyj statusu ANULOWANE.");
    }
    public List<OrderItem> items(long orderId)throws SQLException{requireOrder(orderId);return orders.findItems(orderId);}
    public OrderItem addItem(long orderId,long serviceId,VehicleSize size,int quantity)throws SQLException{
        ServiceOrder order=requireOrder(orderId);if(order.getStatus()==ServiceOrderStatus.ZAKONCZONE||order.getStatus()==ServiceOrderStatus.ANULOWANE)throw new IllegalArgumentException("Nie można zmieniać pozycji zakończonego lub anulowanego zlecenia.");
        if(quantity<=0)throw new IllegalArgumentException("Ilość musi być większa od zera.");
        OrderItem item=createItem(serviceId,size,quantity);
        item.setOrderId(orderId);
        orders.addItem(orderId,item);return item;
    }
    public OrderItem createItem(long serviceId,VehicleSize size,int quantity)throws SQLException{
        if(quantity<=0)throw new IllegalArgumentException("Ilość musi być większa od zera.");
        pl.warsztatpro.model.Service catalog=services.findById(serviceId).orElseThrow(()->new IllegalArgumentException("Nie znaleziono usługi."));
        if(!catalog.isActive())throw new IllegalArgumentException("Do zlecenia można dodawać wyłącznie aktywne usługi.");
        OrderItem item=new OrderItem();item.setServiceId(serviceId);item.setServiceName(catalog.getName());item.setUnitPrice(catalog.priceFor(size));item.setQuantity(quantity);item.setTotalValue(Math.multiplyExact(item.getUnitPrice(),quantity));return item;
    }
    public void editItem(long orderId,OrderItem item)throws SQLException{
        ServiceOrder order=requireOrder(orderId);if(order.getStatus()==ServiceOrderStatus.ZAKONCZONE||order.getStatus()==ServiceOrderStatus.ANULOWANE)throw new IllegalArgumentException("Nie można zmieniać pozycji zlecenia.");
        if(item.getQuantity()<=0||item.getUnitPrice()<0)throw new IllegalArgumentException("Cena nie może być ujemna, a ilość musi być większa od zera.");
        item.setOrderId(orderId);item.setTotalValue(Math.multiplyExact(item.getUnitPrice(),item.getQuantity()));
        if(!orders.editItem(orderId,item))throw new IllegalArgumentException("Nie znaleziono pozycji w tym zleceniu.");
    }
    public void removeItem(long orderId,long itemId)throws SQLException{
        ServiceOrder order=requireOrder(orderId);if(order.getStatus()==ServiceOrderStatus.ZAKONCZONE||order.getStatus()==ServiceOrderStatus.ANULOWANE)throw new IllegalArgumentException("Nie można zmieniać pozycji zlecenia.");
        if(!orders.removeItem(orderId,itemId))throw new IllegalArgumentException("Nie znaleziono pozycji.");
    }
    private ServiceOrder requireOrder(long id)throws SQLException{return orders.findById(id).orElseThrow(()->new IllegalArgumentException("Nie znaleziono zlecenia."));}
    private static void validateOrder(ServiceOrder o){
        if(o.getClientId()<=0||o.getVehicleId()<=0)throw new IllegalArgumentException("Wskaż klienta i pojazd.");
        if(o.getReceivedDate()==null)throw new IllegalArgumentException("Data przyjęcia jest wymagana.");
        if(o.getPlannedCompletionDate()!=null&&o.getPlannedCompletionDate().isBefore(o.getReceivedDate()))throw new IllegalArgumentException("Planowany termin nie może być wcześniejszy niż data przyjęcia.");
        if(o.getFaultDescription()==null||o.getFaultDescription().isBlank())throw new IllegalArgumentException("Opis usterki jest wymagany.");
        if(o.getStatus()==null)throw new IllegalArgumentException("Status zlecenia jest wymagany.");
    }
}
