package rszabadi.swiss;

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
class VehicleApiTest {

    private static final String VALID_VEHICLE =
            "{\"brand\":\"Dacia\",\"model\":\"Sandero\",\"year\":2020,\"km\":41000,\"price\":11500}";
    private static final String INVALID_VEHICLE =
            "{\"brand\":\"\",\"model\":\"Clio\",\"year\":2018,\"km\":-5,\"price\":0}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createVehicleReturns201AndTheSavedVehicle() throws Exception {
        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VEHICLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.brand").value("Dacia"));
    }

    @Test
    void createInvalidVehicleReturns400() throws Exception {
        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_VEHICLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithInvalidVehicleReturns400() throws Exception {
        mockMvc.perform(put("/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_VEHICLE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingVehicleReturns404() throws Exception {
        mockMvc.perform(get("/vehicles/999999"))
                .andExpect(status().isNotFound());
    }
    @Test
    void createIgnoresAnIdSentByTheClient() throws Exception {
        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":123456,\"brand\":\"Dacia\",\"model\":\"Sandero\",\"year\":2020,\"km\":41000,\"price\":11500}"))
                .andExpect(status().isCreated());
    }
}