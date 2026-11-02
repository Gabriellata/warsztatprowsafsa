package pl.warsztatpro.model;

public enum ServiceCategory {
    MECHANIKA("Mechanika"),
    ELEKTRYKA("Elektryka"),
    DIAGNOSTYKA("Diagnostyka"),
    WYMIANA_PLYNOW("Wymiana płynów"),
    SERWIS_KLIMATYZACJI("Serwis klimatyzacji"),
    WULKANIZACJA("Wulkanizacja"),
    INNE("Inne");

    private final String label;

    ServiceCategory(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
