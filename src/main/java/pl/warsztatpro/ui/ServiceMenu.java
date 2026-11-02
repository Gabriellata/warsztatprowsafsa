package pl.warsztatpro.ui;

import pl.warsztatpro.model.Service;
import pl.warsztatpro.model.ServiceCategory;
import pl.warsztatpro.service.ServiceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ServiceMenu {
    private final ServiceService service;
    private final Scanner scanner;
    public ServiceMenu(ServiceService service, Scanner scanner) { this.service = service; this.scanner = scanner; }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("--- Zarządzanie usługami i cennikiem ---");
            System.out.println("1. Dodaj usługę\n2. Wyświetl wszystkie usługi\n3. Wyszukaj usługę\n4. Edytuj usługę\n5. Usuń usługę\n6. Zmień status usługi\n7. Wyświetl cennik\n8. Powrót do menu głównego");
            String choice = read("Wybierz opcję: ");
            try {
                switch (choice) {
                    case "1" -> add();
                    case "2" -> print(service.findAll());
                    case "3" -> print(service.search(read("Nazwa usługi: ")));
                    case "4" -> edit();
                    case "5" -> delete();
                    case "6" -> toggle();
                    case "7" -> print(service.findAll().stream().filter(Service::isActive).toList());
                    case "8" -> running = false;
                    default -> System.out.println("Nieznana opcja. Wybierz pozycję 1-8.");
                }
            } catch (IllegalArgumentException e) { System.out.println("Błąd: " + e.getMessage()); }
            catch (InputClosedException e) { running = false; }
            catch (SQLException e) { System.out.println("Błąd bazy danych: " + e.getMessage()); }
            if (running) System.out.println();
        }
    }

    private void add() throws SQLException {
        Service s = new Service(); fill(s, false); service.add(s); System.out.println("Dodano usługę o ID: " + s.getId());
    }
    private void edit() throws SQLException {
        long id = readId("ID usługi do edycji: "); Optional<Service> found = service.findById(id);
        if (found.isEmpty()) { System.out.println("Nie znaleziono usługi o ID: " + id); return; }
        Service s = found.get(); System.out.println("Pozostaw pole puste, aby zachować wartość."); fill(s, true); service.update(s); System.out.println("Zaktualizowano usługę.");
    }
    private void delete() throws SQLException {
        long id = readId("ID usługi do usunięcia: ");
        if (service.findById(id).isEmpty()) { System.out.println("Nie znaleziono usługi."); return; }
        if ("t".equalsIgnoreCase(read("Potwierdź usunięcie (t/n): "))) { service.deleteById(id); System.out.println("Usunięto usługę."); }
        else System.out.println("Anulowano usuwanie.");
    }
    private void toggle() throws SQLException {
        long id = readId("ID usługi: "); Optional<Service> found = service.findById(id);
        if (found.isEmpty()) { System.out.println("Nie znaleziono usługi."); return; }
        boolean active = !found.get().isActive(); service.setActive(id, active); System.out.println(active ? "Usługa aktywowana." : "Usługa dezaktywowana.");
    }
    private void fill(Service s, boolean edit) {
        s.setName(readOrKeep("Nazwa", s.getName(), edit)); s.setDescription(readOrKeep("Opis", s.getDescription(), edit));
        s.setCategory(readCategory(edit ? s.getCategory() : null));
        s.setPriceSmall(readPrice("Cena dla małego pojazdu", s.getPriceSmall(), edit));
        s.setPriceMedium(readPrice("Cena dla średniego pojazdu", s.getPriceMedium(), edit));
        s.setPriceLarge(readPrice("Cena dla dużego pojazdu", s.getPriceLarge(), edit));
    }
    private ServiceCategory readCategory(ServiceCategory current) {
        ServiceCategory[] values = ServiceCategory.values();
        while (true) {
            for (int i=0; i<values.length; i++) System.out.println((i+1) + ". " + values[i]);
            String value = read(current == null ? "Kategoria: " : "Kategoria [" + current + "]: ");
            if (value.isBlank() && current != null) return current;
            try { int n = Integer.parseInt(value); if (n >= 1 && n <= values.length) return values[n-1]; } catch (NumberFormatException ignored) { }
            for (ServiceCategory category : values) if (category.name().equalsIgnoreCase(value)) return category;
            System.out.println("Wybierz kategorię z listy.");
        }
    }
    private long readPrice(String label, long current, boolean edit) {
        while (true) {
            String raw = read(label + (edit ? " [" + money(current) + "]: " : " (zł): "));
            if (raw.isBlank() && edit) return current;
            try {
                BigDecimal amount = new BigDecimal(raw.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
                if (amount.signum() >= 0) return amount.movePointRight(2).longValueExact();
            } catch (NumberFormatException | ArithmeticException ignored) { }
            System.out.println("Podaj nieujemną kwotę z maksymalnie dwoma miejscami po przecinku.");
        }
    }
    private String readOrKeep(String label, String current, boolean edit) {
        String value = read(label + (edit ? " [" + (current == null ? "" : current) + "]: " : ": "));
        return value.isBlank() && edit ? current : value;
    }
    private long readId(String prompt) {
        while (true) { try { long id=Long.parseLong(read(prompt)); if(id>0)return id; } catch(NumberFormatException ignored){} System.out.println("Podaj dodatni identyfikator."); }
    }
    private static String money(long grosze) { return BigDecimal.valueOf(grosze, 2).toPlainString() + " zł"; }
    private void print(List<Service> services) { if (services.isEmpty()) { System.out.println("Brak usług do wyświetlenia."); return; } System.out.println("ID | Usługa | Kategoria | Mały | Średni | Duży | Status"); services.forEach(System.out::println); }
    private String read(String prompt) { System.out.print(prompt); if(!scanner.hasNextLine())throw new InputClosedException();return scanner.nextLine().trim(); }
    private static final class InputClosedException extends RuntimeException { }
}
