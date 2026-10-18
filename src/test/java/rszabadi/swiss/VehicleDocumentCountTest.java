package rszabadi.swiss;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VehicleDocumentCountTest extends SecuredApiTest {

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

    private void saveDocument(Vehicle vehicle, DocumentType type, DocumentStatus status) {
        VehicleDocument d = new VehicleDocument();
        d.setVehicle(vehicle);
        d.setType(type);
        d.setStatus(status);
        documentRepository.save(d);
    }

    @Test
    void vehicleShowsHowManyDocumentsWereReceived() throws Exception {
        Vehicle vehicle = savedVehicle();
        saveDocument(vehicle, DocumentType.ITV_CERTIFICATE, DocumentStatus.RECEIVED);
        saveDocument(vehicle, DocumentType.TECHNICAL_SHEET, DocumentStatus.RECEIVED);
        saveDocument(vehicle, DocumentType.SALES_CONTRACT, DocumentStatus.PENDING);

        mockMvc.perform(get("/vehicles/" + vehicle.getId()))
                .andExpect(jsonPath("$.documentsReceived").value(2))
                .andExpect(jsonPath("$.documentsTotal").value(DocumentType.values().length));
    }

    @Test
    void listShowsTheSameCountsAsTheSingleVehicle() throws Exception {
        Vehicle vehicle = savedVehicle();
        saveDocument(vehicle, DocumentType.ITV_CERTIFICATE, DocumentStatus.RECEIVED);

        mockMvc.perform(get("/vehicles"))
                .andExpect(jsonPath("$[?(@.id == " + vehicle.getId() + ")].documentsReceived").value(1));
    }

    @Test
    void vehicleWithoutDocumentsHasZeroReceived() throws Exception {
        Vehicle vehicle = savedVehicle();

        mockMvc.perform(get("/vehicles/" + vehicle.getId()))
                .andExpect(jsonPath("$.documentsReceived").value(0));
    }
}