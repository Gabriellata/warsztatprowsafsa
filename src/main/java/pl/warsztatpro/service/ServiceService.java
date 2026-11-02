package pl.warsztatpro.service;

import pl.warsztatpro.model.Service;
import pl.warsztatpro.model.ServiceCategory;
import pl.warsztatpro.model.VehicleSize;
import pl.warsztatpro.repository.ServiceRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ServiceService {
    private final ServiceRepository repository;
    public ServiceService(ServiceRepository repository) { this.repository = repository; }
    public Service add(Service service) throws SQLException { validate(service); return repository.insert(service); }
    public List<Service> findAll() throws SQLException { return repository.findAll(); }
    public List<Service> search(String name) throws SQLException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Fraza wyszukiwania nie może być pusta.");
        return repository.search(name.trim());
    }
    public Optional<Service> findById(long id) throws SQLException { return repository.findById(id); }
    public Service update(Service service) throws SQLException {
        if (service.getId() == null) throw new IllegalArgumentException("Brak identyfikatora usługi.");
        validate(service); if (!repository.update(service)) throw new IllegalArgumentException("Nie znaleziono usługi o ID: " + service.getId()); return service;
    }
    public void deleteById(long id) throws SQLException {
        try {
            if (!repository.deleteById(id)) throw new IllegalArgumentException("Nie znaleziono usługi o ID: " + id);
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key"))) {
                throw new IllegalArgumentException("Nie można usunąć usługi użytej w historii zleceń.", e);
            }
            throw e;
        }
    }
    public void setActive(long id, boolean active) throws SQLException { if (!repository.setActive(id, active)) throw new IllegalArgumentException("Nie znaleziono usługi o ID: " + id); }
    public long defaultPrice(long id, VehicleSize size) throws SQLException {
        Service service = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Nie znaleziono usługi o ID: " + id));
        if (!service.isActive()) throw new IllegalArgumentException("Usługa jest nieaktywna.");
        return service.priceFor(size);
    }
    private static void validate(Service service) {
        if (service.getName() == null || service.getName().isBlank()) throw new IllegalArgumentException("Nazwa usługi jest wymagana.");
        service.setName(service.getName().trim());
        if (service.getDescription() != null && service.getDescription().isBlank()) service.setDescription(null);
        if (service.getCategory() == null || !List.of(ServiceCategory.values()).contains(service.getCategory())) throw new IllegalArgumentException("Wybierz kategorię usługi.");
        if (service.getPriceSmall() < 0 || service.getPriceMedium() < 0 || service.getPriceLarge() < 0) throw new IllegalArgumentException("Ceny nie mogą być ujemne.");
    }
}
