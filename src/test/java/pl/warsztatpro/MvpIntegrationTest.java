package pl.warsztatpro;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.warsztatpro.database.DatabaseBackupService;
import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.*;
import pl.warsztatpro.repository.*;
import pl.warsztatpro.service.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MvpIntegrationTest {
    @TempDir static Path temporaryDirectory;

    @BeforeAll
    static void configureIsolatedDatabase() {
        System.setProperty("warsztatpro.db.path", temporaryDirectory.resolve("warsztatpro-test.db").toString());
    }

    @Test
    void modulesSharePersistentDatabaseAndProtectHistory() throws Exception {
        DatabaseConnection.initializeSchema();
        ClientRepository clients = new ClientRepository();
        VehicleRepository vehicles = new VehicleRepository();
        ServiceRepository serviceRepository = new ServiceRepository();
        ClientService clientService = new ClientService(clients, vehicles);
        VehicleService vehicleService = new VehicleService(vehicles, clientService);
        ServiceService serviceService = new ServiceService(serviceRepository);

        Client client = client("MVP", "Klient", "mvp@example.com", "+48123456789");
        clientService.add(client);
        Client invalidPhone = client("Błędny", "Telefon", "invalid-phone@example.com", "+---");
        assertThrows(IllegalArgumentException.class, () -> clientService.add(invalidPhone));
        Client invalidEmail = client("Błędny", "Email", "niepoprawny", "+48111111111");
        assertThrows(IllegalArgumentException.class, () -> clientService.add(invalidEmail));
        Vehicle vehicle = vehicle(client.getId(), "WAW1234", "1HGCM82633A004352");
        vehicleService.add(vehicle);
        assertThrows(IllegalArgumentException.class, () -> vehicleService.add(vehicle(999_999L, "BAD123", null)));
        assertThrows(IllegalArgumentException.class, () -> vehicleService.add(vehicle(client.getId(), "BADVIN", "INVALID")));
        Vehicle futureVehicle = vehicle(client.getId(), "FUTURE1", null);
        futureVehicle.setProductionYear(LocalDate.now().getYear() + 5);
        assertThrows(IllegalArgumentException.class, () -> vehicleService.add(futureVehicle));

        pl.warsztatpro.model.Service oil = service("Wymiana oleju", 1_000, 1_500, 2_000);
        pl.warsztatpro.model.Service brakes = service("Kontrola hamulców", 2_500, 3_000, 3_500);
        serviceService.add(oil);
        serviceService.add(brakes);
        pl.warsztatpro.model.Service invalidPrice = service("Błędna cena", -1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> serviceService.add(invalidPrice));

        ServiceOrderRepository orderRepository = new ServiceOrderRepository();
        ServiceOrderService orders = new ServiceOrderService(orderRepository, new OrderItemRepository(), clients, vehicles, serviceRepository);
        assertThrows(IllegalArgumentException.class, () -> orders.createItem(999_999, VehicleSize.MALY, 1));
        assertThrows(IllegalArgumentException.class, () -> orders.createItem(oil.getId(), VehicleSize.MALY, 0));
        ServiceOrder order = new ServiceOrder();
        order.setClientId(client.getId());
        order.setVehicleId(vehicle.getId());
        order.setReceivedDate(LocalDate.now());
        order.setFaultDescription("Kontrola okresowa");
        order.getItems().add(orders.createItem(oil.getId(), VehicleSize.SREDNI, 2));
        order.getItems().add(orders.createItem(brakes.getId(), VehicleSize.SREDNI, 1));
        orders.create(order);
        assertEquals(6_000, order.getTotalCost());
        assertEquals(2, order.getItems().size());

        oil.setPriceMedium(99_999);
        serviceService.update(oil);
        ServiceOrder loaded = orders.findByNumber(order.getOrderNumber()).orElseThrow();
        assertEquals(6_000, loaded.getTotalCost());
        assertEquals(1_500, loaded.getItems().getFirst().getUnitPrice());

        ServiceOrder otherClientOrder = new ServiceOrder();
        Client secondClient = client("Inny", "Klient", "other@example.com", "+48987654321");
        clientService.add(secondClient);
        otherClientOrder.setClientId(secondClient.getId());
        otherClientOrder.setVehicleId(vehicle.getId());
        otherClientOrder.setReceivedDate(LocalDate.now());
        otherClientOrder.setFaultDescription("Błędne powiązanie");
        assertThrows(IllegalArgumentException.class, () -> orders.create(otherClientOrder));

        try (var connection = DatabaseConnection.getConnection()) {
            try (var pragma = connection.createStatement().executeQuery("PRAGMA foreign_keys")) {
                assertTrue(pragma.next());
                assertEquals(1, pragma.getInt(1));
            }
            assertThrows(SQLException.class, () -> {
                try (var insert = connection.prepareStatement("INSERT INTO vehicles(client_id,type,registration_number,brand,model,production_year) VALUES(?,?,?,?,?,?)")) {
                    insert.setLong(1, 999_999); insert.setString(2, "OSOBOWY"); insert.setString(3, "NOPE");
                    insert.setString(4, "Test"); insert.setString(5, "Test"); insert.setInt(6, 2020); insert.executeUpdate();
                }
            });
            assertThrows(SQLException.class, () -> {
                try (var insert = connection.prepareStatement("INSERT INTO service_orders(order_number,client_id,vehicle_id,received_date,fault_description,status) VALUES(?,?,?,?,?,?)")) {
                    insert.setString(1, "BAD-RELATION"); insert.setLong(2, secondClient.getId()); insert.setLong(3, vehicle.getId());
                    insert.setString(4, LocalDate.now().toString()); insert.setString(5, "invalid"); insert.setString(6, "NOWE"); insert.executeUpdate();
                }
            });
        }

        orders.changeStatus(order.getId(), ServiceOrderStatus.W_TRAKCIE);
        assertEquals(ServiceOrderStatus.W_TRAKCIE, orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(1, orders.historyForVehicle(vehicle.getId()).size());
        assertEquals(1, orders.historyForClient(client.getId()).size());
        assertThrows(IllegalArgumentException.class, () -> vehicleService.deleteById(vehicle.getId()));
        assertThrows(IllegalArgumentException.class, () -> orders.deleteIfEmpty(order.getId()));

        simulateLegacyCascadeSchema();
        DatabaseConnection.initializeSchema();
        assertEquals(1, orders.findAll().size(), "Dane muszą przetrwać ponowną inicjalizację bazy.");
        assertEquals(2, orders.findById(order.getId()).orElseThrow().getItems().size(), "Migracja nie może utracić historii pozycji.");
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT version FROM schema_version WHERE id=1")) {
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
        }
        try (var connection = DatabaseConnection.getConnection();
             var result = connection.createStatement().executeQuery("PRAGMA foreign_key_list(service_order_items)")) {
            boolean protectedHistory = false;
            while (result.next()) {
                if ("service_orders".equals(result.getString("table"))) {
                    protectedHistory = "RESTRICT".equalsIgnoreCase(result.getString("on_delete"));
                }
            }
            assertTrue(protectedHistory, "Pozycje historyczne powinny blokować fizyczne usunięcie zlecenia.");
        }

        DatabaseBackupService backupService = new DatabaseBackupService(temporaryDirectory.resolve("backups"));
        Path backup = backupService.createBackup();
        assertTrue(backupService.listBackups().contains(backup));
        assertTrue(backup.toFile().length() > 0);
        Path secondBackup = backupService.createBackup();
        assertNotEquals(backup, secondBackup, "Kopie muszą mieć unikalne nazwy.");
        assertEquals(2, backupService.listBackups().size());
    }

    private static Client client(String first, String last, String email, String phone) {
        Client c = new Client(); c.setType(ClientType.OSOBA_PRYWATNA); c.setFirstName(first); c.setLastName(last);
        c.setPhone(phone); c.setEmail(email); c.setPostalCode("00-001"); c.setStreet("Testowa 1"); c.setCity("Warszawa"); return c;
    }
    private static Vehicle vehicle(long clientId, String registration, String vin) {
        Vehicle v = new Vehicle(); v.setClientId(clientId); v.setType(VehicleType.OSOBOWY); v.setRegistrationNumber(registration);
        v.setVin(vin); v.setBrand("Test"); v.setModel("MVP"); v.setProductionYear(2020); return v;
    }
    private static pl.warsztatpro.model.Service service(String name, long small, long medium, long large) {
        pl.warsztatpro.model.Service s = new pl.warsztatpro.model.Service(); s.setName(name); s.setCategory(ServiceCategory.MECHANIKA);
        s.setPriceSmall(small); s.setPriceMedium(medium); s.setPriceLarge(large); s.setActive(true); return s;
    }

    private static void simulateLegacyCascadeSchema() throws SQLException {
        try (var connection = DatabaseConnection.getConnection(); var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");
            connection.setAutoCommit(false);
            statement.execute("""
                    CREATE TABLE service_order_items_legacy (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, service_id INTEGER NOT NULL,
                        service_name TEXT NOT NULL, unit_price INTEGER NOT NULL CHECK(unit_price >= 0),
                        quantity INTEGER NOT NULL CHECK(quantity > 0), total_value INTEGER NOT NULL CHECK(total_value >= 0),
                        FOREIGN KEY(order_id) REFERENCES service_orders(id) ON DELETE CASCADE,
                        FOREIGN KEY(service_id) REFERENCES services(id) ON DELETE RESTRICT)
                    """);
            statement.executeUpdate("INSERT INTO service_order_items_legacy SELECT * FROM service_order_items");
            statement.execute("DROP TABLE service_order_items");
            statement.execute("ALTER TABLE service_order_items_legacy RENAME TO service_order_items");
            statement.executeUpdate("UPDATE schema_version SET version=1 WHERE id=1");
            connection.commit();
        }
    }
}
