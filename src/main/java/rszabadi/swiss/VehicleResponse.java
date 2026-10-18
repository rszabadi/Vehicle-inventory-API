package rszabadi.swiss;

public record VehicleResponse(
        Long id,
        String brand,
        String model,
        int year,
        int km,
        int price,
        VehicleStatus status,
        int documentsReceived,
        int documentsTotal) {

    public static VehicleResponse from(Vehicle v, int documentsReceived) {
        return new VehicleResponse(v.getId(), v.getBrand(), v.getModel(),
                v.getYear(), v.getKm(), v.getPrice(), v.getStatus(),
                documentsReceived, DocumentType.values().length);
    }
}