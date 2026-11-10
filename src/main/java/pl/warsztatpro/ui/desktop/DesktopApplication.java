package pl.warsztatpro.ui.desktop;

import pl.warsztatpro.database.DatabaseConnection;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public final class DesktopApplication {
    private DesktopApplication() { }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) { }
            new SwingWorker<ApplicationServices, Void>() {
                @Override protected ApplicationServices doInBackground() throws Exception {
                    DatabaseConnection.initializeSchema();
                    return ApplicationServices.create();
                }
                @Override protected void done() {
                    try { new DesktopFrame(get()).setVisible(true); }
                    catch (Exception e) {
                        Throwable cause=e.getCause()==null?e:e.getCause();
                        JOptionPane.showMessageDialog(null,
                                cause instanceof java.sql.SQLException ? "Nie udało się otworzyć bazy danych. Sprawdź plik i uprawnienia." : "Nie udało się uruchomić aplikacji.",
                                "WarsztatPro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
    }
}
