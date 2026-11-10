package pl.warsztatpro.service;

import pl.warsztatpro.model.ServiceHistoryEntry;
import pl.warsztatpro.model.ServiceOrder;
import pl.warsztatpro.repository.ClientRepository;
import pl.warsztatpro.repository.ServiceOrderRepository;
import pl.warsztatpro.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServiceHistoryService {
    private final ServiceOrderRepository orders;
    private final ClientRepository clients;
    private final VehicleRepository vehicles;

    public ServiceHistoryService(ServiceOrderRepository orders, ClientRepository clients, VehicleRepository vehicles) {
        this.orders=orders; this.clients=clients; this.vehicles=vehicles;
    }

    public List<ServiceHistoryEntry> forClient(long clientId)throws SQLException {
        if(clients.findById(clientId).isEmpty())throw new IllegalArgumentException("Nie znaleziono klienta.");
        List<ServiceHistoryEntry> history=new ArrayList<>();
        for(ServiceOrder order:orders.findByClient(clientId))history.add(orderEntry(order));
        return sorted(history);
    }

    public List<ServiceHistoryEntry> forVehicle(long vehicleId)throws SQLException {
        if(vehicles.findById(vehicleId).isEmpty())throw new IllegalArgumentException("Nie znaleziono pojazdu.");
        List<ServiceHistoryEntry> history=new ArrayList<>();
        for(ServiceOrder order:orders.findByVehicle(vehicleId))history.add(orderEntry(order));
        return sorted(history);
    }

    private static ServiceHistoryEntry orderEntry(ServiceOrder order) {
        return new ServiceHistoryEntry(order.getReceivedDate().atStartOfDay(),"Zlecenie",order.getOrderNumber(),
                order.getStatus().toString(),order.getFaultDescription());
    }
    private static List<ServiceHistoryEntry> sorted(List<ServiceHistoryEntry> history) {
        history.sort(Comparator.comparing(ServiceHistoryEntry::occurredAt).thenComparing(ServiceHistoryEntry::type));
        return history;
    }
}
