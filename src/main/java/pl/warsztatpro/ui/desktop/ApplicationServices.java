package pl.warsztatpro.ui.desktop;

import pl.warsztatpro.database.DatabaseBackupService;
import pl.warsztatpro.repository.*;
import pl.warsztatpro.service.*;

/** Shared application services used by the desktop views. */
public record ApplicationServices(
        ClientService clients,
        VehicleService vehicles,
        ServiceService services,
        ServiceOrderService orders,
        WorkshopService workshops,
        ServiceHistoryService history,
        ReportService reports,
        DatabaseBackupService backups) {

    public static ApplicationServices create() {
        ClientRepository clients = new ClientRepository();
        VehicleRepository vehicles = new VehicleRepository();
        ServiceRepository services = new ServiceRepository();
        ServiceOrderRepository orders = new ServiceOrderRepository();
        WorkshopRepository workshops = new WorkshopRepository();
        ClientService clientService = new ClientService(clients, vehicles);
        return new ApplicationServices(
                clientService,
                new VehicleService(vehicles, clientService),
                new ServiceService(services),
                new ServiceOrderService(orders, new OrderItemRepository(), clients, vehicles, services, workshops),
                new WorkshopService(workshops),
                new ServiceHistoryService(orders, clients, vehicles),
                new ReportService(new ReportRepository()),
                new DatabaseBackupService());
    }
}
