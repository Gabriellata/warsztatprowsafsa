package pl.warsztatpro.model;

public enum AppointmentStatus {
    ZAPLANOWANA("Zaplanowana"), ZREALIZOWANA("Zrealizowana"), ANULOWANA("Anulowana");

    private final String label;

    AppointmentStatus(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
