package pl.warsztatpro.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final int CURRENT_SCHEMA_VERSION = 14;
    private static final Path DATABASE_PATH = Path.of(System.getProperty("warsztatpro.db.path", "data/warsztatpro.db"))
            .toAbsolutePath().normalize();
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH;

    private static final String CREATE_CLIENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL CHECK (type IN ('OSOBA_PRYWATNA', 'FIRMA')),
                first_name TEXT,
                last_name TEXT,
                company_name TEXT,
                phone TEXT NOT NULL,
                email TEXT NOT NULL,
                postal_code TEXT NOT NULL,
                street TEXT NOT NULL,
                city TEXT NOT NULL
            )
            """;

    private static final String CREATE_VEHICLES_TABLE = """
            CREATE TABLE IF NOT EXISTS vehicles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id INTEGER,
                type TEXT NOT NULL CHECK (type IN ('OSOBOWY', 'DOSTAWCZY', 'CIEZAROWY', 'TIR', 'MOTOCYKL', 'QUAD', 'INNY')),
                vin TEXT UNIQUE,
                registration_number TEXT NOT NULL,
                brand TEXT NOT NULL,
                model TEXT NOT NULL,
                production_year INTEGER NOT NULL,
                engine_capacity REAL,
                fuel_type TEXT,
                engine_power REAL,
                drive_type TEXT,
                body_type TEXT,
                FOREIGN KEY (client_id) REFERENCES clients(id)
            )
            """;

    private static final String CREATE_SERVICES_TABLE = """
            CREATE TABLE IF NOT EXISTS services (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                category TEXT NOT NULL CHECK (category IN ('MECHANIKA', 'OLEJOWE', 'EKSPLOATACYJNE',
                    'LAKIERNICZE_I_BLACHARSKIE', 'KONSERWACYJNE', 'ELEKTRYKA', 'DIAGNOSTYKA',
                    'WYMIANA_PLYNOW', 'SERWIS_KLIMATYZACJI', 'WULKANIZACJA', 'INNE')),
                price_small INTEGER NOT NULL CHECK (price_small >= 0),
                price_medium INTEGER NOT NULL CHECK (price_medium >= 0),
                price_large INTEGER NOT NULL CHECK (price_large >= 0),
                active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
            )
            """;

    private static final String CREATE_VEHICLE_TYPES_TABLE = """
            CREATE TABLE IF NOT EXISTS vehicle_types (
                code TEXT PRIMARY KEY,
                active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
            )
            """;

    private static final String CREATE_SERVICE_ORDERS_TABLE = """
            CREATE TABLE IF NOT EXISTS service_orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_number TEXT NOT NULL UNIQUE,
                client_id INTEGER,
                vehicle_id INTEGER NOT NULL,
                received_date TEXT NOT NULL,
                planned_completion_date TEXT,
                actual_completion_date TEXT,
                scheduled_at TEXT,
                planned_duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (planned_duration_minutes > 0),
                workshop_id INTEGER,
                fault_description TEXT NOT NULL,
                work_description TEXT,
                status TEXT NOT NULL CHECK (status IN ('NOWE', 'W_TRAKCIE', 'OCZEKUJE_NA_CZESCI',
                    'GOTOWE_DO_ODBIORU', 'ZAKONCZONE', 'ANULOWANE')),
                total_cost INTEGER NOT NULL DEFAULT 0 CHECK (total_cost >= 0),
                notes TEXT,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
                FOREIGN KEY (workshop_id) REFERENCES workshops(id) ON DELETE RESTRICT
            )
            """;

    private static final String CREATE_SERVICE_ORDER_ITEMS_TABLE = """
            CREATE TABLE IF NOT EXISTS service_order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                service_id INTEGER NOT NULL,
                service_name TEXT NOT NULL,
                unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                total_value INTEGER NOT NULL CHECK (total_value >= 0),
                FOREIGN KEY (order_id) REFERENCES service_orders(id) ON DELETE RESTRICT,
                FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
            )
            """;

    private static final String CREATE_WORKSHOPS_TABLE = """
            CREATE TABLE IF NOT EXISTS workshops (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                description TEXT,
                active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
            )
            """;

    private static final String CREATE_SCHEMA_VERSION_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_version (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                version INTEGER NOT NULL
            )
            """;

    private static final String CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS validate_order_client_vehicle_insert
            BEFORE INSERT ON service_orders
            WHEN NEW.client_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM vehicles WHERE id = NEW.vehicle_id AND client_id = NEW.client_id)
            BEGIN SELECT RAISE(ABORT, 'Pojazd nie jest przypisany do wskazanego klienta'); END
            """;

    private static final String CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS validate_order_client_vehicle_update
            BEFORE UPDATE OF client_id, vehicle_id ON service_orders
            WHEN NEW.client_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM vehicles WHERE id = NEW.vehicle_id AND client_id = NEW.client_id)
            BEGIN SELECT RAISE(ABORT, 'Pojazd nie jest przypisany do wskazanego klienta'); END
            """;

    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(DATABASE_PATH.getParent());
        } catch (Exception e) {
            throw new SQLException("Nie można utworzyć katalogu bazy danych: " + DATABASE_PATH.getParent(), e);
        }
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initializeSchema() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            connection.setAutoCommit(false);
            try {
            statement.execute(CREATE_CLIENTS_TABLE);
            // Ensure this column exists for both older databases and databases created by v13.
            migrateClientNip(statement);
            statement.execute(CREATE_VEHICLES_TABLE);
            statement.execute(CREATE_SERVICES_TABLE);
            statement.execute(CREATE_SERVICE_ORDERS_TABLE);
            statement.execute(CREATE_SERVICE_ORDER_ITEMS_TABLE);
            statement.execute(CREATE_WORKSHOPS_TABLE);
            statement.execute(CREATE_SCHEMA_VERSION_TABLE);
            statement.execute(CREATE_VEHICLE_TYPES_TABLE);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER);
            int version = 0;
            try (var result = statement.executeQuery("SELECT version FROM schema_version WHERE id = 1")) {
                if (result.next()) version = result.getInt(1);
            }
            if (version > CURRENT_SCHEMA_VERSION) {
                throw new SQLException("Baza danych ma nowszą wersję schematu: " + version);
            }
            if (version == 0) migrateVehicleTypes(connection, statement);
            if (version > 0 && version < 2) migrateOrderItemDeleteRestriction(connection, statement);
            boolean hasLegacyAppointments = tableExists(statement, "appointments");
            if (hasLegacyAppointments && version > 0 && version < 4) migrateAppointmentWorkshop(connection, statement);
            if (hasLegacyAppointments && version > 0 && version < 5) migrateAppointmentServiceOrder(connection, statement);
            if (version > 0 && version < 6) migrateVehicleTypes(connection, statement);
            if (version > 0 && version < 7) migrateServiceCategories(connection, statement);
            if (version > 0 && version < 8) migrateClientNip(statement);
            if (version > 0 && version < 9) migrateNullableVehicleOwner(statement);
            if (version > 0 && version < 10) migrateServiceOrderSchedule(statement);
            if (version > 0 && version < 11) migrateNullableScheduleClients(statement);
            if (version > 0 && version < 12) migrateServiceOrderWorkshop(statement);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER);
            if (hasLegacyAppointments) {
                dropLegacyAppointmentTriggers(statement);
                migrateAppointmentRecordsToOrders(connection);
                statement.execute("DROP TABLE appointments");
            }
            if (version == 0) {
                try (var insert = connection.prepareStatement("INSERT INTO schema_version(id, version) VALUES(1, ?)")) {
                    insert.setInt(1, CURRENT_SCHEMA_VERSION);
                    insert.executeUpdate();
                }
            } else if (version < CURRENT_SCHEMA_VERSION) {
                try (var update = connection.prepareStatement("UPDATE schema_version SET version=? WHERE id=1")) {
                    update.setInt(1, CURRENT_SCHEMA_VERSION);
                    update.executeUpdate();
                }
            }
            connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                try (Statement foreignKeys = connection.createStatement()) { foreignKeys.execute("PRAGMA foreign_keys = ON"); }
            }
        }
    }

    public static Path getDatabasePath() { return DATABASE_PATH; }

    private static boolean tableExists(Statement statement, String tableName) throws SQLException {
        try (var result = statement.executeQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return result.next();
        }
    }

    private static void dropLegacyAppointmentTriggers(Statement statement) throws SQLException {
        for (String trigger : new String[]{"validate_appointment_client_vehicle_insert", "validate_appointment_client_vehicle_update",
                "sync_order_calendar_insert", "sync_order_calendar_update", "sync_appointment_order_insert",
                "sync_appointment_order_update", "sync_appointment_order_unlink", "sync_appointment_order_delete",
                "delete_order_calendar_event"}) {
            statement.execute("DROP TRIGGER IF EXISTS " + trigger);
        }
    }

    private static void migrateAppointmentRecordsToOrders(Connection connection) throws SQLException {
        try (var update = connection.prepareStatement("""
                UPDATE service_orders
                SET scheduled_at=(SELECT scheduled_at FROM appointments WHERE appointments.service_order_id=service_orders.id),
                    planned_duration_minutes=(SELECT duration_minutes FROM appointments WHERE appointments.service_order_id=service_orders.id),
                    workshop_id=COALESCE((SELECT workshop_id FROM appointments WHERE appointments.service_order_id=service_orders.id),workshop_id),
                    work_description=CASE WHEN (SELECT description FROM appointments WHERE appointments.service_order_id=service_orders.id) IS NOT NULL
                        THEN (SELECT description FROM appointments WHERE appointments.service_order_id=service_orders.id) ELSE work_description END,
                    notes=COALESCE((SELECT notes FROM appointments WHERE appointments.service_order_id=service_orders.id),notes)
                WHERE EXISTS (SELECT 1 FROM appointments WHERE appointments.service_order_id=service_orders.id)
                """)) {
            update.executeUpdate();
        }
        migrateStandaloneAppointmentsToOrders(connection);
    }

    private static void migrateServiceOrderWorkshop(Statement statement) throws SQLException {
        boolean present = false;
        try (var columns = statement.executeQuery("PRAGMA table_info(service_orders)")) {
            while (columns.next()) if ("workshop_id".equalsIgnoreCase(columns.getString("name"))) present = true;
        }
        if (!present) statement.execute("ALTER TABLE service_orders ADD COLUMN workshop_id INTEGER REFERENCES workshops(id) ON DELETE RESTRICT");
        if (tableExists(statement, "appointments")) {
            statement.executeUpdate("UPDATE service_orders SET workshop_id=(SELECT workshop_id FROM appointments WHERE appointments.service_order_id=service_orders.id) WHERE workshop_id IS NULL AND EXISTS (SELECT 1 FROM appointments WHERE appointments.service_order_id=service_orders.id AND workshop_id IS NOT NULL)");
        }
    }

    /** Promote old standalone calendar entries into the canonical service-order records. */
    private static void migrateStandaloneAppointmentsToOrders(Connection connection) throws SQLException {
        java.util.List<LegacyAppointment> legacy = new java.util.ArrayList<>();
        try (var query = connection.prepareStatement("SELECT id,client_id,vehicle_id,scheduled_at,duration_minutes,description,status,notes,workshop_id FROM appointments WHERE service_order_id IS NULL ORDER BY id");
             var rows = query.executeQuery()) {
            while (rows.next()) {
                long clientValue = rows.getLong("client_id"); Long clientId = rows.wasNull() ? null : clientValue;
                long workshopValue = rows.getLong("workshop_id"); Long workshopId = rows.wasNull() ? null : workshopValue;
                legacy.add(new LegacyAppointment(rows.getLong("id"), clientId, rows.getLong("vehicle_id"), rows.getString("scheduled_at"),
                        rows.getInt("duration_minutes"), rows.getString("description"), rows.getString("status"), rows.getString("notes"), workshopId));
            }
        }
        for (LegacyAppointment appointment : legacy) {
            String orderStatus = switch (appointment.status()) {
                case "ANULOWANA" -> "ANULOWANE";
                case "ZREALIZOWANA" -> "ZAKONCZONE";
                default -> "NOWE";
            };
            String scheduledDate = appointment.scheduledAt().substring(0, 10);
            try (var insert = connection.prepareStatement("INSERT INTO service_orders(order_number,client_id,vehicle_id,received_date,actual_completion_date,scheduled_at,planned_duration_minutes,workshop_id,fault_description,status,total_cost,notes) VALUES(?,?,?,?,?,?,?,?,?,?,0,?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, "ZL-TERM-" + appointment.id());
                if (appointment.clientId() == null) insert.setNull(2, java.sql.Types.INTEGER); else insert.setLong(2, appointment.clientId());
                insert.setLong(3, appointment.vehicleId()); insert.setString(4, scheduledDate);
                if ("ZAKONCZONE".equals(orderStatus)) insert.setString(5, scheduledDate); else insert.setNull(5, java.sql.Types.VARCHAR);
                insert.setString(6, appointment.scheduledAt()); insert.setInt(7, appointment.durationMinutes());
                if (appointment.workshopId() == null) insert.setNull(8, java.sql.Types.INTEGER); else insert.setLong(8, appointment.workshopId());
                insert.setString(9, appointment.description()); insert.setString(10, orderStatus); insert.setString(11, appointment.notes());
                insert.executeUpdate();
                try (var keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Nie udało się utworzyć zlecenia dla wpisu terminarza " + appointment.id() + ".");
                }
            }
        }
    }

    private record LegacyAppointment(long id, Long clientId, long vehicleId, String scheduledAt, int durationMinutes,
                                     String description, String status, String notes, Long workshopId) { }

    private static void migrateClientNip(Statement statement) throws SQLException {
        try (var columns = statement.executeQuery("PRAGMA table_info(clients)")) {
            while (columns.next()) {
                if ("nip".equalsIgnoreCase(columns.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE clients ADD COLUMN nip TEXT");
    }

    private static void migrateNullableScheduleClients(Statement statement) throws SQLException {
        statement.execute("DROP TRIGGER IF EXISTS validate_order_client_vehicle_insert");
        statement.execute("DROP TRIGGER IF EXISTS validate_order_client_vehicle_update");
        statement.execute("DROP TRIGGER IF EXISTS sync_appointment_order_update");
        statement.execute("DROP TRIGGER IF EXISTS sync_appointment_order_unlink");
        statement.execute("DROP TRIGGER IF EXISTS sync_appointment_order_insert");
        statement.execute("DROP TRIGGER IF EXISTS sync_appointment_order_delete");
        statement.execute("""
                CREATE TABLE service_orders_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_number TEXT NOT NULL UNIQUE,
                    client_id INTEGER,
                    vehicle_id INTEGER NOT NULL,
                    received_date TEXT NOT NULL,
                    planned_completion_date TEXT,
                    actual_completion_date TEXT,
                    scheduled_at TEXT,
                    planned_duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (planned_duration_minutes > 0),
                    fault_description TEXT NOT NULL,
                    work_description TEXT,
                    status TEXT NOT NULL CHECK (status IN ('NOWE', 'W_TRAKCIE', 'OCZEKUJE_NA_CZESCI',
                        'GOTOWE_DO_ODBIORU', 'ZAKONCZONE', 'ANULOWANE')),
                    total_cost INTEGER NOT NULL DEFAULT 0 CHECK (total_cost >= 0),
                    notes TEXT,
                    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
                )
                """);
        statement.executeUpdate("""
                INSERT INTO service_orders_new(id,order_number,client_id,vehicle_id,received_date,planned_completion_date,
                    actual_completion_date,scheduled_at,planned_duration_minutes,fault_description,work_description,status,total_cost,notes)
                SELECT id,order_number,client_id,vehicle_id,received_date,planned_completion_date,
                    actual_completion_date,scheduled_at,planned_duration_minutes,fault_description,work_description,status,total_cost,notes
                FROM service_orders
                """);
        statement.execute("DROP TABLE service_orders");
        statement.execute("ALTER TABLE service_orders_new RENAME TO service_orders");
        statement.execute("""
                CREATE TABLE appointments_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_id INTEGER,
                    vehicle_id INTEGER NOT NULL,
                    scheduled_at TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
                    description TEXT NOT NULL,
                    status TEXT NOT NULL CHECK (status IN ('ZAPLANOWANA', 'ZREALIZOWANA', 'ANULOWANA')),
                    notes TEXT,
                    workshop_id INTEGER,
                    service_order_id INTEGER,
                    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
                    FOREIGN KEY (workshop_id) REFERENCES workshops(id) ON DELETE RESTRICT,
                    FOREIGN KEY (service_order_id) REFERENCES service_orders(id) ON DELETE RESTRICT
                )
                """);
        statement.executeUpdate("""
                INSERT INTO appointments_new(id,client_id,vehicle_id,scheduled_at,duration_minutes,description,status,notes,workshop_id,service_order_id)
                SELECT id,client_id,vehicle_id,scheduled_at,duration_minutes,description,status,notes,workshop_id,service_order_id
                FROM appointments
                """);
        statement.execute("DROP TABLE appointments");
        statement.execute("ALTER TABLE appointments_new RENAME TO appointments");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_service_orders_client_id ON service_orders(client_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_appointments_client_id ON appointments(client_id)");
    }

    private static void migrateServiceOrderSchedule(Statement statement) throws SQLException {
        boolean hasScheduledAt = false;
        boolean hasDuration = false;
        try (var columns = statement.executeQuery("PRAGMA table_info(service_orders)")) {
            while (columns.next()) {
                String name = columns.getString("name");
                if ("scheduled_at".equalsIgnoreCase(name)) hasScheduledAt = true;
                if ("planned_duration_minutes".equalsIgnoreCase(name)) hasDuration = true;
            }
        }
        if (!hasScheduledAt) statement.execute("ALTER TABLE service_orders ADD COLUMN scheduled_at TEXT");
        if (!hasDuration) statement.execute("ALTER TABLE service_orders ADD COLUMN planned_duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (planned_duration_minutes > 0)");
        if (tableExists(statement, "appointments")) statement.executeUpdate("""
                UPDATE service_orders
                SET scheduled_at=(SELECT scheduled_at FROM appointments WHERE service_order_id=service_orders.id),
                    planned_duration_minutes=(SELECT duration_minutes FROM appointments WHERE service_order_id=service_orders.id)
                WHERE scheduled_at IS NULL
                  AND (SELECT COUNT(*) FROM appointments WHERE service_order_id=service_orders.id)=1
                """);
    }

    private static void migrateNullableVehicleOwner(Statement statement) throws SQLException {
        statement.execute("DROP TRIGGER IF EXISTS validate_order_client_vehicle_insert");
        statement.execute("DROP TRIGGER IF EXISTS validate_order_client_vehicle_update");
        statement.execute("""
                CREATE TABLE vehicles_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_id INTEGER,
                    type TEXT NOT NULL CHECK (type IN ('OSOBOWY', 'DOSTAWCZY', 'CIEZAROWY', 'TIR', 'MOTOCYKL', 'QUAD', 'INNY')),
                    vin TEXT UNIQUE,
                    registration_number TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    model TEXT NOT NULL,
                    production_year INTEGER NOT NULL,
                    engine_capacity REAL,
                    fuel_type TEXT,
                    engine_power REAL,
                    drive_type TEXT,
                    body_type TEXT,
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
                """);
        statement.execute("""
                INSERT INTO vehicles_new(id, client_id, type, vin, registration_number, brand, model,
                    production_year, engine_capacity, fuel_type, engine_power, drive_type, body_type)
                SELECT id, client_id, type, vin, registration_number, brand, model,
                    production_year, engine_capacity, fuel_type, engine_power, drive_type, body_type FROM vehicles
                """);
        statement.execute("DROP TABLE vehicles");
        statement.execute("ALTER TABLE vehicles_new RENAME TO vehicles");
        statement.execute(CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER);
        statement.execute(CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER);
    }

    private static void migrateVehicleTypes(Connection connection, Statement statement) throws SQLException {
        for (String type : new String[]{"OSOBOWY", "DOSTAWCZY", "CIEZAROWY", "TIR", "MOTOCYKL", "QUAD", "INNY"}) {
            try (var insert = connection.prepareStatement("INSERT OR IGNORE INTO vehicle_types(code, active) VALUES(?, 1)")) {
                insert.setString(1, type); insert.executeUpdate();
            }
        }
    }

    private static void migrateServiceCategories(Connection connection, Statement statement) throws SQLException {
        long previousSequence = 0;
        try (PreparedStatement sequence = connection.prepareStatement("SELECT seq FROM sqlite_sequence WHERE name='services'");
             var result = sequence.executeQuery()) { if (result.next()) previousSequence = result.getLong(1); }
        statement.execute("""
                CREATE TABLE services_migrating (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL CHECK (category IN ('MECHANIKA', 'OLEJOWE', 'EKSPLOATACYJNE',
                        'LAKIERNICZE_I_BLACHARSKIE', 'KONSERWACYJNE', 'ELEKTRYKA', 'DIAGNOSTYKA',
                        'WYMIANA_PLYNOW', 'SERWIS_KLIMATYZACJI', 'WULKANIZACJA', 'INNE')),
                    price_small INTEGER NOT NULL CHECK (price_small >= 0),
                    price_medium INTEGER NOT NULL CHECK (price_medium >= 0),
                    price_large INTEGER NOT NULL CHECK (price_large >= 0),
                    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
                )
                """);
        statement.executeUpdate("INSERT INTO services_migrating(id,name,description,category,price_small,price_medium,price_large,active) SELECT id,name,description,category,price_small,price_medium,price_large,active FROM services");
        statement.execute("DROP TABLE services");
        statement.execute("ALTER TABLE services_migrating RENAME TO services");
        try (PreparedStatement sequence = connection.prepareStatement("UPDATE sqlite_sequence SET seq=MAX(seq,?) WHERE name='services'")) {
            sequence.setLong(1, previousSequence); sequence.executeUpdate();
        }
        try (PreparedStatement sequence = connection.prepareStatement("SELECT 1 FROM sqlite_sequence WHERE name='services'");
             var result = sequence.executeQuery()) {
            if (!result.next() && previousSequence > 0) {
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO sqlite_sequence(name,seq) VALUES('services',?)")) {
                    insert.setLong(1, previousSequence); insert.executeUpdate();
                }
            }
        }
    }

    private static void migrateAppointmentWorkshop(Connection connection, Statement statement) throws SQLException {
        boolean hasWorkshopColumn = false;
        try (var columns = statement.executeQuery("PRAGMA table_info(appointments)")) {
            while (columns.next()) {
                if ("workshop_id".equalsIgnoreCase(columns.getString("name"))) {
                    hasWorkshopColumn = true;
                    break;
                }
            }
        }
        if (!hasWorkshopColumn) {
            statement.execute("ALTER TABLE appointments ADD COLUMN workshop_id INTEGER REFERENCES workshops(id) ON DELETE RESTRICT");
        }
    }

    private static void migrateAppointmentServiceOrder(Connection connection, Statement statement) throws SQLException {
        boolean hasServiceOrderColumn = false;
        try (var columns = statement.executeQuery("PRAGMA table_info(appointments)")) {
            while (columns.next()) {
                if ("service_order_id".equalsIgnoreCase(columns.getString("name"))) {
                    hasServiceOrderColumn = true;
                    break;
                }
            }
        }
        if (!hasServiceOrderColumn) {
            statement.execute("ALTER TABLE appointments ADD COLUMN service_order_id INTEGER REFERENCES service_orders(id) ON DELETE RESTRICT");
        }
    }

    private static void migrateOrderItemDeleteRestriction(Connection connection, Statement statement) throws SQLException {
        boolean cascades = false;
        try (var foreignKeys = statement.executeQuery("PRAGMA foreign_key_list(service_order_items)")) {
            while (foreignKeys.next()) {
                if ("service_orders".equalsIgnoreCase(foreignKeys.getString("table"))
                        && "CASCADE".equalsIgnoreCase(foreignKeys.getString("on_delete"))) {
                    cascades = true;
                    break;
                }
            }
        }
        if (!cascades) return;
        statement.execute("""
                CREATE TABLE service_order_items_migrating (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER NOT NULL,
                    service_id INTEGER NOT NULL,
                    service_name TEXT NOT NULL,
                    unit_price INTEGER NOT NULL CHECK (unit_price >= 0),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    total_value INTEGER NOT NULL CHECK (total_value >= 0),
                    FOREIGN KEY (order_id) REFERENCES service_orders(id) ON DELETE RESTRICT,
                    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT
                )
                """);
        statement.executeUpdate("INSERT INTO service_order_items_migrating SELECT id,order_id,service_id,service_name,unit_price,quantity,total_value FROM service_order_items");
        statement.execute("DROP TABLE service_order_items");
        statement.execute("ALTER TABLE service_order_items_migrating RENAME TO service_order_items");
    }
}
