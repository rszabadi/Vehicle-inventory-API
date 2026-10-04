package rszabadi.swiss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class VehicleControllerTest {

    private final VehicleRepository repository = mock(VehicleRepository.class);
    private final VehicleController controller = new VehicleController(repository);

    @Test
    void getReturnsVehicleWhenItExists() {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand("Seat");
        when(repository.findById(1L)).thenReturn(Optional.of(vehicle));

        Vehicle result = controller.get(1L);

        assertThat(result.getBrand()).isEqualTo("Seat");
    }

    @Test
    void getThrowsNotFoundWhenVehicleIsMissing() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.get(999L));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}