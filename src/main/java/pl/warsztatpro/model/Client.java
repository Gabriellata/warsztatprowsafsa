package pl.warsztatpro.model;

public class Client {

    private Long id;
    private ClientType type;
    private String firstName;
    private String lastName;
    private String companyName;
    private String phone;
    private String email;
    private String postalCode;
    private String street;
    private String city;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClientType getType() {
        return type;
    }

    public void setType(ClientType type) {
        this.type = type;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String displayName() {
        if (type == ClientType.FIRMA) {
            return companyName;
        }
        return (nullToEmpty(firstName) + " " + nullToEmpty(lastName)).trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String toString() {
        return "ID: " + id
                + " | Typ: " + type
                + " | Nazwa: " + displayName()
                + " | Tel: " + phone
                + " | E-mail: " + email
                + " | Adres: " + street + ", " + postalCode + " " + city;
    }
}
