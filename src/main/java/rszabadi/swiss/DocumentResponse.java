package rszabadi.swiss;

import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        Long vehicleId,
        DocumentType type,
        DocumentStatus status,
        LocalDate expiresOn) {

    public static DocumentResponse from(Long vehicleId, VehicleDocument document) {
        return new DocumentResponse(document.getId(), vehicleId, document.getType(),
                document.getStatus(), document.getExpiresOn());
    }
}