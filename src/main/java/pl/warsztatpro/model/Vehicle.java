package pl.warsztatpro.model;

public class Vehicle {

    private Long id;
    private Long clientId;
    private VehicleType type;
    private String vin;
    private String registrationNumber;
    private String brand;
    private String model;
    private Integer productionYear;
    private Double engineCapacity;
    private String fuelType;
    private Double enginePower;
    private String driveType;
    private String bodyType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    }

    public Double getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(Double engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Double getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(Double enginePower) {
        this.enginePower = enginePower;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    @Override
    public String toString() {
        return "ID: " + id
                + " | Klient: " + (clientId == null ? "brak" : clientId)
                + " | Typ: " + type
                + " | VIN: " + display(vin)
                + " | Rej.: " + registrationNumber
                + " | " + brand + " " + model
                + " | Rok: " + productionYear
                + " | Poj.: " + display(engineCapacity)
                + " | Paliwo: " + display(fuelType)
                + " | Moc: " + display(enginePower)
                + " | Napęd: " + display(driveType)
                + " | Nadwozie: " + display(bodyType);
    }

    private static String display(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value.toString();
    }
}
