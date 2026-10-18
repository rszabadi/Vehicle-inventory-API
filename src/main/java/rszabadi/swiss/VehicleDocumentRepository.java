package rszabadi.swiss;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleId(Long vehicleId);
    Optional<VehicleDocument> findByIdAndVehicleId(Long id, Long vehicleId);
    boolean existsByVehicleIdAndType(Long vehicleId, DocumentType type);
    
    long countByVehicleIdAndStatus(Long vehicleId, DocumentStatus status);

    interface ReceivedCount {
        Long getVehicleId();
        Long getReceived();
    }

    @Query("select d.vehicle.id as vehicleId, count(d) as received "
            + "from VehicleDocument d where d.status = :status group by d.vehicle.id")
    List<ReceivedCount> countPerVehicle(@Param("status") DocumentStatus status);
}