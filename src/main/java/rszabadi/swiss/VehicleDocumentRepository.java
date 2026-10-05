package rszabadi.swiss;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleId(Long vehicleId);
    Optional<VehicleDocument> findByIdAndVehicleId(Long id, Long vehicleId);
}