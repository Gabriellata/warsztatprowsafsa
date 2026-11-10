package pl.warsztatpro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceOrder {
    private Long id;
    private String orderNumber;
    private Long clientId;
    private long vehicleId;
    private LocalDate receivedDate;
    private LocalDate plannedCompletionDate;
    private LocalDate actualCompletionDate;
    private LocalDateTime scheduledAt;
    private int plannedDurationMinutes = 60;
    private Long workshopId;
    private String faultDescription;
    private String workDescription;
    private ServiceOrderStatus status = ServiceOrderStatus.NOWE;
    private long totalCost;
    private String notes;
    private List<OrderItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public LocalDate getPlannedCompletionDate() { return plannedCompletionDate; }
    public void setPlannedCompletionDate(LocalDate plannedCompletionDate) { this.plannedCompletionDate = plannedCompletionDate; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; }
    public void setActualCompletionDate(LocalDate actualCompletionDate) { this.actualCompletionDate = actualCompletionDate; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public int getPlannedDurationMinutes() { return plannedDurationMinutes; }
    public void setPlannedDurationMinutes(int plannedDurationMinutes) { this.plannedDurationMinutes = plannedDurationMinutes; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public String getFaultDescription() { return faultDescription; }
    public void setFaultDescription(String faultDescription) { this.faultDescription = faultDescription; }
    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }
    public ServiceOrderStatus getStatus() { return status; }
    public void setStatus(ServiceOrderStatus status) { this.status = status; }
    public long getTotalCost() { return totalCost; }
    public void setTotalCost(long totalCost) { this.totalCost = totalCost; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items == null ? new ArrayList<>() : items; }

    @Override public String toString() {
        return orderNumber + " | klient " + clientId + " | pojazd " + vehicleId + " | " + status
                + " | przyjęto " + receivedDate + " | koszt " + java.math.BigDecimal.valueOf(totalCost, 2).toPlainString() + " zł";
    }
}
