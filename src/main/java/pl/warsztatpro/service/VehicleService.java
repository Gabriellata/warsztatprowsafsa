package pl.warsztatpro.service;

import pl.warsztatpro.model.Vehicle;
import pl.warsztatpro.model.VehicleType;
import pl.warsztatpro.repository.VehicleRepository;
import pl.warsztatpro.model.VehicleTypeOption;
import pl.warsztatpro.repository.VehicleTypeRepository;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class VehicleService {

    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");
    private static final int MIN_YEAR = 1900;

    private final VehicleRepository vehicleRepository;
    private final ClientService clientService;
    private final VehicleTypeRepository types = new VehicleTypeRepository();

    public VehicleService(VehicleRepository vehicleRepository, ClientService clientService) {
        this.vehicleRepository = vehicleRepository;
        this.clientService = clientService;
    }

    public Vehicle add(Vehicle vehicle) throws SQLException {
        validate(vehicle);
        return vehicleRepository.insert(vehicle);
    }

    public List<Vehicle> findAll() throws SQLException {
        return vehicleRepository.findAll();
    }

    public List<VehicleTypeOption> vehicleTypes() throws SQLException { return types.findAll(); }

    public void setVehicleTypeActive(VehicleType type, boolean active) throws SQLException {
        if (!types.setActive(type, active)) throw new IllegalArgumentException("Nie znaleziono typu pojazdu.");
    }

    public List<Vehicle> search(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Fraza wyszukiwania nie może być pusta.");
        }
        return vehicleRepository.search(query.trim());
    }

    public Optional<Vehicle> findById(long id) throws SQLException {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> findByClientId(long clientId) throws SQLException {
        if (clientService.findById(clientId).isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono klienta o ID: " + clientId);
        }
        return vehicleRepository.findByClientId(clientId);
    }

    public Vehicle update(Vehicle vehicle) throws SQLException {
        if (vehicle.getId() == null) {
            throw new IllegalArgumentException("Brak identyfikatora pojazdu.");
        }
        validate(vehicle);
        boolean updated = vehicleRepository.update(vehicle);
        if (!updated) {
            throw new IllegalArgumentException("Nie znaleziono pojazdu o ID: " + vehicle.getId());
        }
        return vehicle;
    }

    public void deleteById(long id) throws SQLException {
        boolean deleted;
        try {
            deleted = vehicleRepository.deleteById(id);
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key"))) {
                throw new IllegalArgumentException("Nie można usunąć pojazdu, ponieważ ma powiązane zlecenia serwisowe.", e);
            }
            throw e;
        }
        if (!deleted) {
            throw new IllegalArgumentException("Nie znaleziono pojazdu o ID: " + id);
        }
    }

    public void validate(Vehicle vehicle) throws SQLException {
        if (vehicle.getClientId() != null && vehicle.getClientId() <= 0) {
            throw new IllegalArgumentException("Wybierz istniejącego klienta albo pozostaw pole puste.");
        }
        if (vehicle.getClientId() != null && clientService.findById(vehicle.getClientId()).isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono wybranego klienta.");
        }
        if (vehicle.getType() == null) {
            throw new IllegalArgumentException("Typ pojazdu jest wymagany.");
        }
        boolean activeType = types.isActive(vehicle.getType());
        boolean unchangedExistingType = vehicle.getId() != null
                && vehicleRepository.findById(vehicle.getId()).map(existing -> existing.getType() == vehicle.getType()).orElse(false);
        if (!activeType && !unchangedExistingType) throw new IllegalArgumentException("Ten typ pojazdu jest nieaktywny i nie można go przypisać.");

        requireText(vehicle.getRegistrationNumber(), "Numer rejestracyjny jest wymagany.");
        vehicle.setRegistrationNumber(normalizeRegistration(vehicle.getRegistrationNumber()));

        requireText(vehicle.getBrand(), "Marka jest wymagana.");
        vehicle.setBrand(vehicle.getBrand().trim());

        requireText(vehicle.getModel(), "Model jest wymagany.");
        vehicle.setModel(vehicle.getModel().trim());

        if (vehicle.getProductionYear() == null) {
            throw new IllegalArgumentException("Rok produkcji jest wymagany.");
        }
        int maxYear = Year.now().getValue() + 1;
        if (vehicle.getProductionYear() < MIN_YEAR || vehicle.getProductionYear() > maxYear) {
            throw new IllegalArgumentException("Rok produkcji musi być w zakresie " + MIN_YEAR + "-" + maxYear + ".");
        }

        if (vehicle.getVin() != null && !vehicle.getVin().isBlank()) {
            String vin = vehicle.getVin().trim().toUpperCase();
            if (!VIN_PATTERN.matcher(vin).matches()) {
                throw new IllegalArgumentException("VIN musi mieć 17 znaków (bez liter I, O, Q).");
            }
            if (vehicleRepository.existsVin(vin, vehicle.getId())) {
                throw new IllegalArgumentException("Pojazd o podanym VIN już istnieje.");
            }
            vehicle.setVin(vin);
        } else {
            vehicle.setVin(null);
        }

        if (vehicle.getEngineCapacity() != null && (!Double.isFinite(vehicle.getEngineCapacity()) || vehicle.getEngineCapacity() <= 0)) {
            throw new IllegalArgumentException("Pojemność silnika musi być większa od zera.");
        }
        if (vehicle.getEnginePower() != null && (!Double.isFinite(vehicle.getEnginePower()) || vehicle.getEnginePower() <= 0)) {
            throw new IllegalArgumentException("Moc silnika musi być większa od zera.");
        }

        vehicle.setFuelType(blankToNull(vehicle.getFuelType()));
        vehicle.setDriveType(blankToNull(vehicle.getDriveType()));
        vehicle.setBodyType(blankToNull(vehicle.getBodyType()));
    }

    private static String normalizeRegistration(String value) {
        return value.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
