package pl.warsztatpro.service;

import pl.warsztatpro.model.Client;
import pl.warsztatpro.model.ClientType;
import pl.warsztatpro.repository.ClientRepository;
import pl.warsztatpro.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class ClientService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s.]+(?:\\.[^@\\s.]+)*@[^@\\s.]+(?:\\.[^@\\s.]+)+$");
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^\\d{2}-\\d{3}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9 \\-]*$");
    private static final int[] NIP_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};

    private final ClientRepository repository;
    private final VehicleRepository vehicleRepository;

    public ClientService(ClientRepository repository, VehicleRepository vehicleRepository) {
        this.repository = repository;
        this.vehicleRepository = vehicleRepository;
    }

    public Client add(Client client) throws SQLException {
        validate(client);
        return repository.insert(client);
    }

    public List<Client> findAll() throws SQLException {
        return repository.findAll();
    }

    public List<Client> search(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Fraza wyszukiwania nie może być pusta.");
        }
        return repository.search(query.trim());
    }

    public Optional<Client> findById(long id) throws SQLException {
        return repository.findById(id);
    }

    public Client update(Client client) throws SQLException {
        if (client.getId() == null) {
            throw new IllegalArgumentException("Brak identyfikatora klienta.");
        }
        validate(client);
        boolean updated = repository.update(client);
        if (!updated) {
            throw new IllegalArgumentException("Nie znaleziono klienta o ID: " + client.getId());
        }
        return client;
    }

    public void deleteById(long id) throws SQLException {
        if (vehicleRepository.countByClientId(id) > 0) {
            throw new IllegalArgumentException(
                    "Nie można usunąć klienta o ID " + id + ", ponieważ ma przypisane pojazdy.");
        }
        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            throw new IllegalArgumentException("Nie znaleziono klienta o ID: " + id);
        }
    }

    public void validate(Client client) {
        if (client.getType() == null) {
            throw new IllegalArgumentException("Typ klienta jest wymagany (OSOBA_PRYWATNA / FIRMA).");
        }

        if (client.getType() == ClientType.OSOBA_PRYWATNA) {
            requireText(client.getFirstName(), "Imię jest wymagane.");
            requireText(client.getLastName(), "Nazwisko jest wymagane.");
            client.setCompanyName(null);
        } else {
            requireText(client.getCompanyName(), "Nazwa firmy jest wymagana.");
            client.setFirstName(null);
            client.setLastName(null);
            requireText(client.getNip(), "NIP jest wymagany dla firmy.");
            String nip = client.getNip().replaceAll("[\\s-]", "");
            if (!nip.matches("\\d{10}")) throw new IllegalArgumentException("NIP musi zawierać 10 cyfr.");
            int checksum = 0;
            for (int i = 0; i < NIP_WEIGHTS.length; i++) checksum += (nip.charAt(i) - '0') * NIP_WEIGHTS[i];
            if (checksum % 11 != nip.charAt(9) - '0') throw new IllegalArgumentException("Nieprawidłowy numer NIP.");
            client.setNip(nip);
        }
        if (client.getType() == ClientType.OSOBA_PRYWATNA) client.setNip(null);

        requireText(client.getPhone(), "Numer telefonu jest wymagany.");
        String phone = client.getPhone().trim();
        long digitCount = phone.chars().filter(Character::isDigit).count();
        if (!PHONE_PATTERN.matcher(phone).matches() || digitCount < 9 || digitCount > 15) {
            throw new IllegalArgumentException("Niepoprawny numer telefonu.");
        }
        client.setPhone(phone);

        requireText(client.getEmail(), "Adres e-mail jest wymagany.");
        if (!EMAIL_PATTERN.matcher(client.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Niepoprawny adres e-mail.");
        }
        client.setEmail(client.getEmail().trim());

        requireText(client.getPostalCode(), "Kod pocztowy jest wymagany.");
        if (!POSTAL_CODE_PATTERN.matcher(client.getPostalCode().trim()).matches()) {
            throw new IllegalArgumentException("Kod pocztowy musi mieć format XX-XXX, np. 00-001.");
        }
        client.setPostalCode(client.getPostalCode().trim());

        requireText(client.getStreet(), "Ulica jest wymagana.");
        client.setStreet(client.getStreet().trim());

        requireText(client.getCity(), "Miejscowość jest wymagana.");
        client.setCity(client.getCity().trim());
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
