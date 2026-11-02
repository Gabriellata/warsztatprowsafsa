package pl.warsztatpro.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

public class DatabaseBackupService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private final Path backupDirectory;

    public DatabaseBackupService() {
        this(Path.of(System.getProperty("warsztatpro.backup.dir", "backups")));
    }

    public DatabaseBackupService(Path backupDirectory) {
        this.backupDirectory = backupDirectory.toAbsolutePath().normalize();
    }

    public Path createBackup() throws SQLException, IOException {
        Files.createDirectories(backupDirectory);
        String base = "warsztatpro-" + LocalDateTime.now().format(TIMESTAMP);
        Path destination = backupDirectory.resolve(base + ".db");
        int suffix = 1;
        while (Files.exists(destination)) destination = backupDirectory.resolve(base + "-" + suffix++ + ".db");

        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, destination.toString());
            statement.execute();
        }

        try {
            verifyBackup(destination);
            return destination;
        } catch (SQLException | IOException e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    public List<Path> listBackups() throws IOException {
        if (!Files.isDirectory(backupDirectory)) return List.of();
        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("warsztatpro-")
                            && path.getFileName().toString().endsWith(".db"))
                    .sorted((left, right) -> right.getFileName().toString().compareTo(left.getFileName().toString()))
                    .toList();
        }
    }

    private static void verifyBackup(Path path) throws SQLException, IOException {
        if (!Files.isRegularFile(path) || Files.size(path) == 0) throw new IOException("Plik kopii zapasowej jest pusty lub nie istnieje.");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            try (var result = statement.executeQuery("PRAGMA integrity_check")) {
            if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                throw new SQLException("Kopia zapasowa nie przeszła kontroli integralności.");
            }
            }
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (result.next()) throw new SQLException("Kopia zapasowa zawiera niepoprawne klucze obce.");
            }
        }
    }
}
