package pl.warsztatpro;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.warsztatpro.database.DatabaseBackupService;
import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Appointment;
import pl.warsztatpro.model.AppointmentStatus;
import pl.warsztatpro.model.*;
import pl.warsztatpro.repository.*;
import pl.warsztatpro.service.AppointmentService;
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
        assertNull(clients.findById(client.getId()).orElseThrow().getNip());
        Client invalidPhone = client("Błędny", "Telefon", "invalid-phone@example.com", "+---");
        assertThrows(IllegalArgumentException.class, () -> clientService.add(invalidPhone));
        Client invalidEmail = client("Błędny", "Email", "niepoprawny", "+48111111111");
        assertThrows(IllegalArgumentException.class, () -> clientService.add(invalidEmail));
        Vehicle vehicle = vehicle(client.getId(), "WAW1234", "1HGCM82633A004352");
        vehicleService.add(vehicle);
        Vehicle unassignedVehicle = vehicle(null, "UNASSIGNED1", null);
        vehicleService.add(unassignedVehicle);
        assertNull(vehicleService.findById(unassignedVehicle.getId()).orElseThrow().getClientId());
        Client company = new Client(); company.setType(ClientType.FIRMA); company.setCompanyName("Warsztat Testowy");
        company.setNip("5260250274"); company.setPhone("+48123456789"); company.setEmail("firma@example.com");
        company.setPostalCode("00-001"); company.setStreet("Testowa 1"); company.setCity("Warszawa");
        clientService.add(company);
        assertEquals("5260250274", clients.findById(company.getId()).orElseThrow().getNip());
        company.setCompanyName("Warsztat Testowy po edycji"); clientService.update(company);
        assertEquals("5260250274", clients.findById(company.getId()).orElseThrow().getNip());
        Client invalidNip = new Client(); invalidNip.setType(ClientType.FIRMA); invalidNip.setCompanyName("Błędny NIP");
        invalidNip.setNip("1234567890"); invalidNip.setPhone("+48123456789"); invalidNip.setEmail("nip@example.com");
        invalidNip.setPostalCode("00-001"); invalidNip.setStreet("Testowa 1"); invalidNip.setCity("Warszawa");
        assertThrows(IllegalArgumentException.class, () -> clientService.add(invalidNip));
        AppointmentService appointmentService = new AppointmentService(new AppointmentRepository(), clients, vehicles);
        Appointment appointment = new Appointment();
        appointment.setClientId(client.getId()); appointment.setVehicleId(vehicle.getId());
        appointment.setScheduledAt(LocalDate.now().plusDays(2).atTime(10, 30)); appointment.setDescription("Przegląd");
        appointmentService.create(appointment);
        assertEquals(1, appointmentService.findByDate(appointment.getScheduledAt().toLocalDate()).size());
        Appointment invalidAppointment = new Appointment(); invalidAppointment.setClientId(client.getId());
        invalidAppointment.setVehicleId(999_999); invalidAppointment.setScheduledAt(LocalDate.now().plusDays(2).atTime(11, 30));
        invalidAppointment.setDescription("Nieprawidłowe powiązanie");
        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(invalidAppointment));
        appointmentService.changeStatus(appointment.getId(), AppointmentStatus.ZREALIZOWANA);
        assertEquals(AppointmentStatus.ZREALIZOWANA, appointmentService.findById(appointment.getId()).orElseThrow().getStatus());
        Appointment rescheduled = appointmentService.findById(appointment.getId()).orElseThrow();
        rescheduled.setScheduledAt(rescheduled.getScheduledAt().withHour(12).withMinute(15));
        rescheduled.setDescription("Przegląd po edycji");
        appointmentService.update(rescheduled);
        Appointment persistedAppointment = appointmentService.findById(appointment.getId()).orElseThrow();
        assertEquals(12, persistedAppointment.getScheduledAt().getHour());
        assertEquals("Przegląd po edycji", persistedAppointment.getDescription());
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
        order.setScheduledAt(LocalDate.now().plusDays(5).atTime(8, 30));
        order.setPlannedDurationMinutes(120);
        order.setFaultDescription("Kontrola okresowa");
        order.getItems().add(orders.createItem(oil.getId(), VehicleSize.SREDNI, 2));
        order.getItems().add(orders.createItem(brakes.getId(), VehicleSize.SREDNI, 1));
        orders.create(order);
        assertEquals(6_000, order.getTotalCost());
        assertEquals(2, order.getItems().size());
        assertEquals(1, orders.appointmentsForOrder(order.getId()).size());
        assertEquals(order.getScheduledAt(), orders.appointmentsForOrder(order.getId()).getFirst().getScheduledAt());
        assertThrows(IllegalArgumentException.class, () -> appointmentService.linkToServiceOrder(appointment.getId(), order.getId()));

        order.setScheduledAt(order.getScheduledAt().plusDays(1));
        orders.update(order);
        assertEquals(1, orders.appointmentsForOrder(order.getId()).size(), "Zmiana terminu nie może tworzyć kolejnego zdarzenia.");
        Appointment linkedEvent = orders.appointmentsForOrder(order.getId()).getFirst();
        linkedEvent.setScheduledAt(linkedEvent.getScheduledAt().plusHours(2));
        appointmentService.update(linkedEvent);
        assertEquals(linkedEvent.getScheduledAt(), orders.findById(order.getId()).orElseThrow().getScheduledAt());

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
        Appointment removable = orders.appointmentsForOrder(order.getId()).getFirst();
        appointmentService.delete(removable.getId());
        assertTrue(appointmentService.findById(removable.getId()).isEmpty());
        assertTrue(orders.findById(order.getId()).isPresent(), "Usunięcie wpisu nie może usuwać powiązanego zlecenia.");
        assertNull(orders.findById(order.getId()).orElseThrow().getScheduledAt(), "Usunięcie wpisu powinno wyczyścić termin zlecenia.");

        ServiceOrder withoutSchedule = new ServiceOrder(); withoutSchedule.setClientId(client.getId()); withoutSchedule.setVehicleId(vehicle.getId());
        withoutSchedule.setReceivedDate(LocalDate.now()); withoutSchedule.setFaultDescription("Bez ustalonego terminu"); orders.create(withoutSchedule);
        assertNull(orders.findById(withoutSchedule.getId()).orElseThrow().getScheduledAt());
        assertTrue(orders.appointmentsForOrder(withoutSchedule.getId()).isEmpty());
        ServiceOrder scheduledEmpty = new ServiceOrder(); scheduledEmpty.setClientId(client.getId()); scheduledEmpty.setVehicleId(vehicle.getId());
        scheduledEmpty.setReceivedDate(LocalDate.now()); scheduledEmpty.setFaultDescription("Puste zlecenie z terminem");
        scheduledEmpty.setScheduledAt(LocalDate.now().plusDays(7).atTime(9, 0)); orders.create(scheduledEmpty);
        Long scheduledEmptyEvent = orders.appointmentsForOrder(scheduledEmpty.getId()).getFirst().getId();
        orders.changeStatus(scheduledEmpty.getId(), ServiceOrderStatus.ANULOWANE);
        assertEquals(AppointmentStatus.ANULOWANA, appointmentService.findById(scheduledEmptyEvent).orElseThrow().getStatus());
        orders.deleteIfEmpty(scheduledEmpty.getId());
        assertTrue(appointmentService.findById(scheduledEmptyEvent).isEmpty(), "Usunięcie zlecenia nie może zostawić zdarzenia.");

        simulateLegacyCascadeSchema();
        DatabaseConnection.initializeSchema();
        assertEquals(2, orders.findAll().size(), "Dane muszą przetrwać ponowną inicjalizację bazy.");
        assertEquals(1, appointmentService.findAll().size(), "Wizyty muszą przetrwać ponowną inicjalizację bazy.");
        assertEquals(AppointmentStatus.ZREALIZOWANA, appointmentService.findById(appointment.getId()).orElseThrow().getStatus());
        assertEquals(2, orders.findById(order.getId()).orElseThrow().getItems().size(), "Migracja nie może utracić historii pozycji.");
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT version FROM schema_version WHERE id=1")) {
            assertTrue(result.next());
            assertEquals(11, result.getInt(1));
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
    private static Vehicle vehicle(Long clientId, String registration, String vin) {
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
