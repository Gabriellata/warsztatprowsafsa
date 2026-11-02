package pl.warsztatpro.model;

public enum ServiceOrderStatus {
    NOWE("Nowe"), W_TRAKCIE("W trakcie"), OCZEKUJE_NA_CZESCI("Oczekuje na części"),
    GOTOWE_DO_ODBIORU("Gotowe do odbioru"), ZAKONCZONE("Zakończone"), ANULOWANE("Anulowane");

    private final String label;
    ServiceOrderStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
