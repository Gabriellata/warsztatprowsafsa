package pl.warsztatpro;

import pl.warsztatpro.database.DatabaseBackupService;
import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.repository.*;
import pl.warsztatpro.service.*;
import pl.warsztatpro.ui.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("WARSZTATPRO");
        try {
            DatabaseConnection.initializeSchema();
        } catch (SQLException e) {
            System.out.println("Nie udało się otworzyć bazy danych: " + e.getMessage());
            return;
        }

        ClientRepository clients = new ClientRepository();
        VehicleRepository vehicles = new VehicleRepository();
        ServiceRepository services = new ServiceRepository();
        ClientService clientService = new ClientService(clients, vehicles);
        VehicleService vehicleService = new VehicleService(vehicles, clientService);
        ServiceService serviceService = new ServiceService(services);
        ServiceOrderService orderService = new ServiceOrderService(new ServiceOrderRepository(),
                new OrderItemRepository(), clients, vehicles, services);
        DatabaseBackupService backups = new DatabaseBackupService();

        try (Scanner scanner = new Scanner(System.in)) {
            ClientMenu clientMenu = new ClientMenu(clientService, scanner);
            VehicleMenu vehicleMenu = new VehicleMenu(vehicleService, scanner);
            ServiceMenu serviceMenu = new ServiceMenu(serviceService, scanner);
            ServiceOrderMenu orderMenu = new ServiceOrderMenu(orderService, scanner);
            boolean running = true;
            while (running) {
                printMainMenu();
                String choice = read(scanner, "Wybierz opcję (K = kopie zapasowe): ");
                try {
                    switch (choice.toLowerCase(java.util.Locale.ROOT)) {
                        case "1" -> clientMenu.run();
                        case "2" -> vehicleMenu.run();
                        case "3" -> serviceMenu.run();
                        case "4" -> orderMenu.run();
                        case "5" -> running = false;
                        case "k" -> backupMenu(backups, scanner);
                        default -> System.out.println("Nieznana opcja. Wybierz 1-5 lub K.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Błąd danych: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.out.println("Nie udało się wykonać operacji: " + e.getMessage());
                }
            }
            System.out.println("Do widzenia.");
        }
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("WARSZTATPRO");
        System.out.println("1. Zarządzanie klientami.");
        System.out.println("2. Zarządzanie pojazdami.");
        System.out.println("3. Zarządzanie usługami i cennikiem.");
        System.out.println("4. Zarządzanie zleceniami serwisowymi.");
        System.out.println("5. Wyjście.");
    }

    private static void backupMenu(DatabaseBackupService backups, Scanner scanner) {
        boolean running = true;
        while (running) {
            System.out.println("--- Kopie zapasowe ---");
            System.out.println("1. Utwórz kopię zapasową\n2. Sprawdź dostępne kopie\n3. Powrót");
            switch (read(scanner, "Wybierz opcję: ")) {
                case "1" -> {
                    try {
                        System.out.println("Utworzono i sprawdzono kopię: " + backups.createBackup());
                    } catch (SQLException | IOException e) {
                        System.out.println("Nie udało się utworzyć kopii: " + e.getMessage());
                    }
                }
                case "2" -> {
                    try {
                        var available = backups.listBackups();
                        if (available.isEmpty()) System.out.println("Brak kopii zapasowych.");
                        else available.forEach(System.out::println);
                    } catch (IOException e) {
                        System.out.println("Nie udało się odczytać katalogu kopii: " + e.getMessage());
                    }
                }
                case "3" -> running = false;
                default -> System.out.println("Nieznana opcja. Wybierz 1-3.");
            }
        }
    }

    private static String read(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "5";
    }
}
