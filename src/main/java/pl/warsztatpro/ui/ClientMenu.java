package pl.warsztatpro.ui;

import pl.warsztatpro.model.Client;
import pl.warsztatpro.model.ClientType;
import pl.warsztatpro.service.ClientService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ClientMenu {

    private final ClientService clientService;
    private final Scanner scanner;

    public ClientMenu(ClientService clientService, Scanner scanner) {
        this.clientService = clientService;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Wybierz opcję: ");
            try {
                switch (choice) {
                    case "1" -> addClient();
                    case "2" -> listClients();
                    case "3" -> searchClients();
                    case "4" -> editClient();
                    case "5" -> deleteClient();
                    case "6" -> running = false;
                    default -> System.out.println("Nieznana opcja. Wybierz pozycję z menu (1-6).");
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
        System.out.println("Do widzenia.");
    }

    private void printMenu() {
        System.out.println("----------------------------------------");
        System.out.println("1. Dodaj klienta");
        System.out.println("2. Wyświetl klientów");
        System.out.println("3. Wyszukaj klienta");
        System.out.println("4. Edytuj klienta");
        System.out.println("5. Usuń klienta");
        System.out.println("6. Powrót do menu głównego");
        System.out.println("----------------------------------------");
    }

    private void addClient() throws SQLException {
        Client client = new Client();
        fillClientData(client, false);
        Client saved = clientService.add(client);
        System.out.println("Dodano klienta o ID: " + saved.getId());
    }

    private void listClients() throws SQLException {
        printClients(clientService.findAll());
    }

    private void searchClients() throws SQLException {
        String query = readLine("Imię, nazwisko lub nazwa firmy: ");
        printClients(clientService.search(query));
    }

    private void editClient() throws SQLException {
        long id = readRequiredId("ID klienta do edycji: ");
        Optional<Client> existing = clientService.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Nie znaleziono klienta o ID: " + id);
            return;
        }
        Client client = existing.get();
        System.out.println("Aktualne dane: " + client);
        System.out.println("Pozostaw pole puste, aby zachować dotychczasową wartość.");
        fillClientData(client, true);
        clientService.update(client);
        System.out.println("Zaktualizowano klienta o ID: " + id);
    }

    private void deleteClient() throws SQLException {
        long id = readRequiredId("ID klienta do usunięcia: ");
        Optional<Client> existing = clientService.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Nie znaleziono klienta o ID: " + id);
            return;
        }
        System.out.println("Klient: " + existing.get());
        String confirmation = readLine("Czy na pewno usunąć? (t/n): ");
        if (!"t".equalsIgnoreCase(confirmation) && !"tak".equalsIgnoreCase(confirmation)) {
            System.out.println("Anulowano usuwanie.");
            return;
        }
        clientService.deleteById(id);
        System.out.println("Usunięto klienta o ID: " + id);
    }

    private void fillClientData(Client client, boolean keepExisting) {
        client.setType(readType(keepExisting ? client.getType() : null));
        if (client.getType() == ClientType.OSOBA_PRYWATNA) {
            client.setFirstName(readValue("Imię", keepExisting ? client.getFirstName() : null, keepExisting));
            client.setLastName(readValue("Nazwisko", keepExisting ? client.getLastName() : null, keepExisting));
            client.setCompanyName(null);
        } else {
            client.setCompanyName(readValue("Nazwa firmy", keepExisting ? client.getCompanyName() : null, keepExisting));
            client.setFirstName(null);
            client.setLastName(null);
        }
        client.setPhone(readValue("Telefon", keepExisting ? client.getPhone() : null, keepExisting));
        client.setEmail(readValue("E-mail", keepExisting ? client.getEmail() : null, keepExisting));
        client.setPostalCode(readValue("Kod pocztowy (XX-XXX)", keepExisting ? client.getPostalCode() : null, keepExisting));
        client.setStreet(readValue("Ulica", keepExisting ? client.getStreet() : null, keepExisting));
        client.setCity(readValue("Miejscowość", keepExisting ? client.getCity() : null, keepExisting));
    }

    private ClientType readType(ClientType current) {
        while (true) {
            String prompt = current == null
                    ? "Typ klienta (1 = OSOBA_PRYWATNA, 2 = FIRMA): "
                    : "Typ klienta (1 = OSOBA_PRYWATNA, 2 = FIRMA) [" + current + "]: ";
            String value = readLine(prompt);
            if (value.isBlank() && current != null) {
                return current;
            }
            if ("1".equals(value) || "OSOBA_PRYWATNA".equalsIgnoreCase(value)) {
                return ClientType.OSOBA_PRYWATNA;
            }
            if ("2".equals(value) || "FIRMA".equalsIgnoreCase(value)) {
                return ClientType.FIRMA;
            }
            System.out.println("Niepoprawny typ. Wpisz 1 lub 2.");
        }
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

    private long readRequiredId(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                long id = Long.parseLong(value.trim());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
            System.out.println("Podaj poprawny identyfikator liczbowy.");
        }
    }

    private void printClients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Brak klientów do wyświetlenia.");
            return;
        }
        System.out.println("ID | Typ | Klient | Telefon | E-mail | Adres");
        for (Client client : clients) {
            System.out.println(client);
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new InputClosedException();
        return scanner.nextLine().trim();
    }

    private static final class InputClosedException extends RuntimeException { }
}
