package pl.warsztatpro.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final int CURRENT_SCHEMA_VERSION = 2;
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
                client_id INTEGER NOT NULL,
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
                category TEXT NOT NULL CHECK (category IN ('MECHANIKA', 'ELEKTRYKA', 'DIAGNOSTYKA',
                    'WYMIANA_PLYNOW', 'SERWIS_KLIMATYZACJI', 'WULKANIZACJA', 'INNE')),
                price_small INTEGER NOT NULL CHECK (price_small >= 0),
                price_medium INTEGER NOT NULL CHECK (price_medium >= 0),
                price_large INTEGER NOT NULL CHECK (price_large >= 0),
                active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
            )
            """;

    private static final String CREATE_SERVICE_ORDERS_TABLE = """
            CREATE TABLE IF NOT EXISTS service_orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_number TEXT NOT NULL UNIQUE,
                client_id INTEGER NOT NULL,
                vehicle_id INTEGER NOT NULL,
                received_date TEXT NOT NULL,
                planned_completion_date TEXT,
                actual_completion_date TEXT,
                fault_description TEXT NOT NULL,
                work_description TEXT,
                status TEXT NOT NULL CHECK (status IN ('NOWE', 'W_TRAKCIE', 'OCZEKUJE_NA_CZESCI',
                    'GOTOWE_DO_ODBIORU', 'ZAKONCZONE', 'ANULOWANE')),
                total_cost INTEGER NOT NULL DEFAULT 0 CHECK (total_cost >= 0),
                notes TEXT,
                FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
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

    private static final String CREATE_SCHEMA_VERSION_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_version (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                version INTEGER NOT NULL
            )
            """;

    private static final String CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS validate_order_client_vehicle_insert
            BEFORE INSERT ON service_orders
            WHEN NOT EXISTS (SELECT 1 FROM vehicles WHERE id = NEW.vehicle_id AND client_id = NEW.client_id)
            BEGIN SELECT RAISE(ABORT, 'Pojazd nie jest przypisany do wskazanego klienta'); END
            """;

    private static final String CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER = """
            CREATE TRIGGER IF NOT EXISTS validate_order_client_vehicle_update
            BEFORE UPDATE OF client_id, vehicle_id ON service_orders
            WHEN NOT EXISTS (SELECT 1 FROM vehicles WHERE id = NEW.vehicle_id AND client_id = NEW.client_id)
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
            connection.setAutoCommit(false);
            try {
            statement.execute(CREATE_CLIENTS_TABLE);
            statement.execute(CREATE_VEHICLES_TABLE);
            statement.execute(CREATE_SERVICES_TABLE);
            statement.execute(CREATE_SERVICE_ORDERS_TABLE);
            statement.execute(CREATE_SERVICE_ORDER_ITEMS_TABLE);
            statement.execute(CREATE_SCHEMA_VERSION_TABLE);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_INSERT_TRIGGER);
            statement.execute(CREATE_ORDER_CLIENT_VEHICLE_UPDATE_TRIGGER);
            int version = 0;
            try (var result = statement.executeQuery("SELECT version FROM schema_version WHERE id = 1")) {
                if (result.next()) version = result.getInt(1);
            }
            if (version > CURRENT_SCHEMA_VERSION) {
                throw new SQLException("Baza danych ma nowszą wersję schematu: " + version);
            }
            if (version < 2) migrateOrderItemDeleteRestriction(connection, statement);
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
            }
        }
    }

    public static Path getDatabasePath() { return DATABASE_PATH; }

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
