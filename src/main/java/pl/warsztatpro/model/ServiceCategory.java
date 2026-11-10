package pl.warsztatpro.model;

public enum ServiceCategory {
    MECHANIKA("Mechaniczne"),
    OLEJOWE("Olejowe"),
    EKSPLOATACYJNE("Eksploatacyjne"),
    LAKIERNICZE_I_BLACHARSKIE("Lakiernicze i blacharskie"),
    KONSERWACYJNE("Konserwacyjne"),
    ELEKTRYKA("Elektryka"),
    DIAGNOSTYKA("Diagnostyczne"),
    WYMIANA_PLYNOW("Wymiana płynów"),
    SERWIS_KLIMATYZACJI("Serwis klimatyzacji"),
    WULKANIZACJA("Wulkanizacyjne"),
    INNE("Inne");

    private final String label;

    ServiceCategory(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
