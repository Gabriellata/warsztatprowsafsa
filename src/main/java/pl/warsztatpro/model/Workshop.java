package pl.warsztatpro.model;

public class Workshop {
    private Long id;
    private String name;
    private String description;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() {
        return "ID: " + id + " | " + name + " | " + (active ? "Aktywne" : "Nieaktywne")
                + " | " + (description == null || description.isBlank() ? "-" : description);
    }
}
