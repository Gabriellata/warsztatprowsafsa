package pl.warsztatpro.model;

import java.time.LocalDateTime;

public class Appointment {
    private Long id;
    private Long clientId;
    private long vehicleId;
    private Long workshopId;
    private Long serviceOrderId;
    private LocalDateTime scheduledAt;
    private int durationMinutes = 60;
    private String description;
    private AppointmentStatus status = AppointmentStatus.ZAPLANOWANA;
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public Long getServiceOrderId() { return serviceOrderId; }
    public void setServiceOrderId(Long serviceOrderId) { this.serviceOrderId = serviceOrderId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override public String toString() {
        return "ID: " + id + " | " + scheduledAt.toString().replace('T', ' ') + " | klient " + clientId
                + " | pojazd " + vehicleId + " | " + durationMinutes + " min | " + status + " | " + description;
    }
}
