package rszabadi.swiss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VehicleDocumentRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    private Vehicle savedVehicle() {
        Vehicle v = new Vehicle();
        v.setBrand("Seat");
        v.setModel("Ibiza");
        v.setYear(2016);
        v.setKm(98000);
        v.setPrice(7500);
        return vehicleRepository.save(v);
    }

    private VehicleDocument document(Vehicle vehicle, DocumentType type) {
        VehicleDocument d = new VehicleDocument();
        d.setVehicle(vehicle);
        d.setType(type);
        return d;
    }

    @Test
    void documentsCanBeFoundByVehicle() {
        Vehicle vehicle = savedVehicle();
        documentRepository.save(document(vehicle, DocumentType.ITV_CERTIFICATE));
        documentRepository.save(document(vehicle, DocumentType.TECHNICAL_SHEET));

        assertThat(documentRepository.findByVehicleId(vehicle.getId())).hasSize(2);
    }

    @Test
    void sameDocumentTypeTwiceForOneVehicleIsRejected() {
        Vehicle vehicle = savedVehicle();
        documentRepository.saveAndFlush(document(vehicle, DocumentType.ITV_CERTIFICATE));

        assertThrows(DataIntegrityViolationException.class,
                () -> documentRepository.saveAndFlush(document(vehicle, DocumentType.ITV_CERTIFICATE)));
    }
}