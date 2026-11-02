package pl.warsztatpro.ui;

import pl.warsztatpro.model.Vehicle;
import pl.warsztatpro.model.VehicleType;
import pl.warsztatpro.service.VehicleService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class VehicleMenu {

    private final VehicleService vehicleService;
    private final Scanner scanner;

    public VehicleMenu(VehicleService vehicleService, Scanner scanner) {
        this.vehicleService = vehicleService;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Wybierz opcję: ");
            try {
                switch (choice) {
                    case "1" -> addVehicle();
                    case "2" -> listVehicles();
                    case "3" -> searchVehicles();
                    case "4" -> editVehicle();
                    case "5" -> deleteVehicle();
                    case "6" -> listVehiclesByClient();
                    case "7" -> running = false;
                    default -> System.out.println("Nieznana opcja. Wybierz pozycję z menu (1-7).");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Błąd: " + e.getMessage());
            } catch (InputClosedException e) {
                running = false;
            } catch (SQLException e) {
                System.out.println("Błąd bazy danych: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Wystąpił nieoczekiwany błąd: " + e.getMessage());
            }
            if (running) {
                System.out.println();
            }
        }
    }

    private void printMenu() {
        System.out.println("---------- Pojazdy ----------");
        System.out.println("1. Dodaj pojazd");
        System.out.println("2. Wyświetl wszystkie pojazdy");
        System.out.println("3. Wyszukaj pojazd");
        System.out.println("4. Edytuj pojazd");
        System.out.println("5. Usuń pojazd");
        System.out.println("6. Wyświetl pojazdy klienta");
        System.out.println("7. Powrót");
        System.out.println("-----------------------------");
    }

    private void addVehicle() throws SQLException {
        Vehicle vehicle = new Vehicle();
        fillVehicleData(vehicle, false);
        Vehicle saved = vehicleService.add(vehicle);
        System.out.println("Dodano pojazd o ID: " + saved.getId());
    }

    private void listVehicles() throws SQLException {
        printVehicles(vehicleService.findAll());
    }

    private void searchVehicles() throws SQLException {
        String query = readLine("VIN, numer rejestracyjny, marka lub model: ");
        printVehicles(vehicleService.search(query));
    }

    private void editVehicle() throws SQLException {
        long id = readRequiredId("ID pojazdu do edycji: ");
        Optional<Vehicle> existing = vehicleService.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Nie znaleziono pojazdu o ID: " + id);
            return;
        }
        Vehicle vehicle = existing.get();
        System.out.println("Aktualne dane: " + vehicle);
        System.out.println("Pozostaw pole puste, aby zachować dotychczasową wartość.");
        fillVehicleData(vehicle, true);
        vehicleService.update(vehicle);
        System.out.println("Zaktualizowano pojazd o ID: " + id);
    }

    private void deleteVehicle() throws SQLException {
        long id = readRequiredId("ID pojazdu do usunięcia: ");
        Optional<Vehicle> existing = vehicleService.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Nie znaleziono pojazdu o ID: " + id);
            return;
        }
        System.out.println("Pojazd: " + existing.get());
        String confirmation = readLine("Czy na pewno usunąć? (t/n): ");
        if (!"t".equalsIgnoreCase(confirmation) && !"tak".equalsIgnoreCase(confirmation)) {
            System.out.println("Anulowano usuwanie.");
            return;
        }
        vehicleService.deleteById(id);
        System.out.println("Usunięto pojazd o ID: " + id);
    }

    private void listVehiclesByClient() throws SQLException {
        long clientId = readRequiredId("ID klienta: ");
        printVehicles(vehicleService.findByClientId(clientId));
    }

    private void fillVehicleData(Vehicle vehicle, boolean keepExisting) {
        vehicle.setClientId(readLong("ID właściciela (klienta)", keepExisting ? vehicle.getClientId() : null, keepExisting));
        vehicle.setType(readType(keepExisting ? vehicle.getType() : null));
        vehicle.setVin(readValue("VIN (opcjonalnie)", keepExisting ? vehicle.getVin() : null, keepExisting));
        vehicle.setRegistrationNumber(readValue("Numer rejestracyjny", keepExisting ? vehicle.getRegistrationNumber() : null, keepExisting));
        vehicle.setBrand(readValue("Marka", keepExisting ? vehicle.getBrand() : null, keepExisting));
        vehicle.setModel(readValue("Model", keepExisting ? vehicle.getModel() : null, keepExisting));
        vehicle.setProductionYear(readInteger("Rok produkcji", keepExisting ? vehicle.getProductionYear() : null, keepExisting));
        vehicle.setEngineCapacity(readDouble("Pojemność silnika (l, opcjonalnie)", keepExisting ? vehicle.getEngineCapacity() : null, keepExisting));
        vehicle.setFuelType(readValue("Rodzaj paliwa (opcjonalnie)", keepExisting ? vehicle.getFuelType() : null, keepExisting));
        vehicle.setEnginePower(readDouble("Moc silnika (KM, opcjonalnie)", keepExisting ? vehicle.getEnginePower() : null, keepExisting));
        vehicle.setDriveType(readValue("Rodzaj napędu (opcjonalnie)", keepExisting ? vehicle.getDriveType() : null, keepExisting));
        vehicle.setBodyType(readValue("Typ nadwozia (opcjonalnie)", keepExisting ? vehicle.getBodyType() : null, keepExisting));
    }

    private VehicleType readType(VehicleType current) {
        while (true) {
            System.out.println("Typ pojazdu: 1=osobowy, 2=dostawczy, 3=ciężarowy, 4=TIR, 5=motocykl, 6=quad, 7=inny");
            String prompt = current == null ? "Wybierz typ: " : "Wybierz typ [" + current + "]: ";
            String value = readLine(prompt);
            if (value.isBlank() && current != null) {
                return current;
            }
            VehicleType parsed = parseType(value);
            if (parsed != null) {
                return parsed;
            }
            System.out.println("Niepoprawny typ pojazdu.");
        }
    }

    private static VehicleType parseType(String value) {
        return switch (value.toUpperCase()) {
            case "1", "OSOBOWY" -> VehicleType.OSOBOWY;
            case "2", "DOSTAWCZY" -> VehicleType.DOSTAWCZY;
            case "3", "CIEZAROWY", "CIĘŻAROWY" -> VehicleType.CIEZAROWY;
            case "4", "TIR" -> VehicleType.TIR;
            case "5", "MOTOCYKL" -> VehicleType.MOTOCYKL;
            case "6", "QUAD" -> VehicleType.QUAD;
            case "7", "INNY" -> VehicleType.INNY;
            default -> null;
        };
    }

    private String readValue(String label, String current, boolean keepExisting) {
        String prompt = keepExisting && current != null && !current.isBlank()
                ? label + " [" + current + "]: "
                : label + ": ";
        String value = readLine(prompt);
        if (value.isBlank() && keepExisting) {
            return current;
        }
        return value;
    }

    private Long readLong(String label, Long current, boolean keepExisting) {
        while (true) {
            String prompt = keepExisting && current != null
                    ? label + " [" + current + "]: "
                    : label + ": ";
            String value = readLine(prompt);
            if (value.isBlank() && keepExisting && current != null) {
                return current;
            }
            try {
                long parsed = Long.parseLong(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
            System.out.println("Podaj poprawny identyfikator liczbowy.");
        }
    }

    private Integer readInteger(String label, Integer current, boolean keepExisting) {
        while (true) {
            String prompt = keepExisting && current != null
                    ? label + " [" + current + "]: "
                    : label + ": ";
            String value = readLine(prompt);
            if (value.isBlank() && keepExisting) {
                return current;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Podaj poprawną liczbę całkowitą.");
            }
        }
    }

    private Double readDouble(String label, Double current, boolean keepExisting) {
        while (true) {
            String prompt = keepExisting && current != null
                    ? label + " [" + current + "]: "
                    : label + ": ";
            String value = readLine(prompt);
            if (value.isBlank()) {
                return keepExisting ? current : null;
            }
            try {
                return Double.parseDouble(value.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("Podaj poprawną liczbę (możesz zostawić puste).");
            }
        }
    }

    private long readRequiredId(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
            System.out.println("Podaj poprawny identyfikator liczbowy.");
        }
    }

    private void printVehicles(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            System.out.println("Brak pojazdów do wyświetlenia.");
            return;
        }
        System.out.println("ID | Klient | Typ | VIN | Rejestracja | Pojazd | Rok");
        for (Vehicle vehicle : vehicles) {
            System.out.println(vehicle);
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new InputClosedException();
        return scanner.nextLine().trim();
    }

    private static final class InputClosedException extends RuntimeException { }
}
