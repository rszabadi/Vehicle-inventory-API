package rszabadi.swiss;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Vehicles")
@RestController
public class VehicleController {

    private final VehicleRepository repository;

    public VehicleController(VehicleRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "List vehicles, optionally filtered by status")
    @GetMapping("/vehicles")
    public List<VehicleResponse> list(@RequestParam(required = false) VehicleStatus status) {
        List<Vehicle> vehicles = (status == null) ? repository.findAll() : repository.findByStatus(status);
        return vehicles.stream().map(VehicleResponse::from).toList();
    }

    @Operation(summary = "Create a vehicle")
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        return VehicleResponse.from(repository.save(vehicle));
    }

    @Operation(summary = "Get a vehicle by id")
    @GetMapping("/vehicles/{id}")
    public VehicleResponse get(@PathVariable Long id) {
        return VehicleResponse.from(find(id));
    }

    @Operation(summary = "Update a vehicle")
    @PutMapping("/vehicles/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = find(id);
        apply(vehicle, request);
        return VehicleResponse.from(repository.save(vehicle));
    }

    @Operation(summary = "Delete a vehicle")
    @DeleteMapping("/vehicles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    private Vehicle find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setKm(request.km());
        vehicle.setPrice(request.price());
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
    }
}