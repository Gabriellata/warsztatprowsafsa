package pl.warsztatpro.model;

public class Service {
    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private long priceSmall;
    private long priceMedium;
    private long priceLarge;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ServiceCategory getCategory() { return category; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public long getPriceSmall() { return priceSmall; }
    public void setPriceSmall(long priceSmall) { this.priceSmall = priceSmall; }
    public long getPriceMedium() { return priceMedium; }
    public void setPriceMedium(long priceMedium) { this.priceMedium = priceMedium; }
    public long getPriceLarge() { return priceLarge; }
    public void setPriceLarge(long priceLarge) { this.priceLarge = priceLarge; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long priceFor(VehicleSize size) {
        return switch (size) {
            case MALY -> priceSmall;
            case SREDNI -> priceMedium;
            case DUZY -> priceLarge;
        };
    }

    @Override public String toString() {
        return "ID: " + id + " | " + name + " | " + category + " | mały: " + money(priceSmall)
                + " | średni: " + money(priceMedium) + " | duży: " + money(priceLarge)
                + " | " + (active ? "aktywna" : "nieaktywna")
                + (description == null || description.isBlank() ? "" : " | " + description);
    }

    private static String money(long grosze) {
        return java.math.BigDecimal.valueOf(grosze, 2).toPlainString() + " zł";
    }
}
