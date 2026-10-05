package rszabadi.swiss;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleDocumentApiTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String ITV = "{\"type\":\"ITV_CERTIFICATE\",\"expiresOn\":\"2027-03-15\"}";

    @Autowired
    private MockMvc mockMvc;
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

    private VehicleDocument savedDocument(Vehicle vehicle) {
        VehicleDocument d = new VehicleDocument();
        d.setVehicle(vehicle);
        d.setType(DocumentType.TECHNICAL_SHEET);
        return documentRepository.save(d);
    }

    @Test
    void createDocumentReturns201() throws Exception {
        Vehicle vehicle = savedVehicle();

        mockMvc.perform(post("/vehicles/" + vehicle.getId() + "/documents")
                        .contentType(JSON).content(ITV))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ITV_CERTIFICATE"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void duplicateDocumentTypeReturns409() throws Exception {
        String url = "/vehicles/" + savedVehicle().getId() + "/documents";

        mockMvc.perform(post(url).contentType(JSON).content(ITV))
                .andExpect(status().isCreated());
        mockMvc.perform(post(url).contentType(JSON).content(ITV))
                .andExpect(status().isConflict());
    }

    @Test
    void createWithoutTypeReturns400() throws Exception {
        mockMvc.perform(post("/vehicles/" + savedVehicle().getId() + "/documents")
                        .contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void documentsOfMissingVehicleReturn404() throws Exception {
        mockMvc.perform(get("/vehicles/999999/documents"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocumentChangesStatus() throws Exception {
        Vehicle vehicle = savedVehicle();
        VehicleDocument document = savedDocument(vehicle);

        mockMvc.perform(put("/vehicles/" + vehicle.getId() + "/documents/" + document.getId())
                        .contentType(JSON).content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void deleteDocumentUnderAnotherVehicleReturns404() throws Exception {
        Vehicle owner = savedVehicle();
        Vehicle other = savedVehicle();
        VehicleDocument document = savedDocument(owner);

        mockMvc.perform(delete("/vehicles/" + other.getId() + "/documents/" + document.getId()))
                .andExpect(status().isNotFound());
    }
}