package pl.warsztatpro.model;

public record VehicleTypeOption(VehicleType type, boolean active) {
    public String label() {
        return switch (type) {
            case OSOBOWY -> "Osobowy";
            case DOSTAWCZY -> "Dostawczy";
            case CIEZAROWY -> "Ciężarowy";
            case TIR -> "TIR";
            case MOTOCYKL -> "Motocykl";
            case QUAD -> "Quad";
            case INNY -> "Inny";
        };
    }
}
