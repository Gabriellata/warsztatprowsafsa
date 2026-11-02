package pl.warsztatpro.model;

public class OrderItem {
    private Long id;
    private long orderId;
    private long serviceId;
    private String serviceName;
    private long unitPrice;
    private int quantity;
    private long totalValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }
    public long getServiceId() { return serviceId; }
    public void setServiceId(long serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public long getTotalValue() { return totalValue; }
    public void setTotalValue(long totalValue) { this.totalValue = totalValue; }

    @Override public String toString() {
        return "Pozycja " + id + " | " + serviceName + " (usługa " + serviceId + ") | " + quantity
                + " × " + money(unitPrice) + " = " + money(totalValue);
    }
    private static String money(long grosze) { return java.math.BigDecimal.valueOf(grosze, 2).toPlainString() + " zł"; }
}
