package rszabadi.swiss;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final VehicleDocumentRepository documents;

    public VehicleController(VehicleRepository repository, VehicleDocumentRepository documents) {
        this.repository = repository;
        this.documents = documents;
    }

    @Operation(summary = "List vehicles, optionally filtered by status")
    @GetMapping("/vehicles")
    public List<VehicleResponse> list(@RequestParam(required = false) VehicleStatus status) {
        List<Vehicle> found = (status == null) ? repository.findAll() : repository.findByStatus(status);
        Map<Long, Integer> received = receivedCounts();
        return found.stream()
                .map(v -> VehicleResponse.from(v, received.getOrDefault(v.getId(), 0)))
                .toList();
    }

    @Operation(summary = "Create a vehicle")
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        return response(repository.save(vehicle));
    }

    @Operation(summary = "Get a vehicle by id")
    @GetMapping("/vehicles/{id}")
    public VehicleResponse get(@PathVariable Long id) {
        return response(find(id));
    }

    @Operation(summary = "Update a vehicle")
    @PutMapping("/vehicles/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = find(id);
        apply(vehicle, request);
        return response(repository.save(vehicle));
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

    private VehicleResponse response(Vehicle vehicle) {
        int received = (int) documents.countByVehicleIdAndStatus(vehicle.getId(), DocumentStatus.RECEIVED);
        return VehicleResponse.from(vehicle, received);
    }

    private Map<Long, Integer> receivedCounts() {
        return documents.countPerVehicle(DocumentStatus.RECEIVED).stream()
                .collect(Collectors.toMap(
                        VehicleDocumentRepository.ReceivedCount::getVehicleId,
                        c -> c.getReceived().intValue()));
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