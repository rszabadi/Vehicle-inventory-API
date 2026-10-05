package rszabadi.swiss;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "List all vehicles")
    @GetMapping("/vehicles")
    public List<Vehicle> list() {
        return repository.findAll();
    }
    
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody Vehicle vehicle) {
        vehicle.setId(null); // never trust an id sent by the client on create
        return repository.save(vehicle);
    }

    @Operation(summary = "Get a vehicle by id")
    @GetMapping("/vehicles/{id}")
    public Vehicle get(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    @Operation(summary = "Update a vehicle")
    @PutMapping("/vehicles/{id}")
    	public Vehicle update(@PathVariable Long id, @Valid @RequestBody Vehicle updated) {
        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicle.setBrand(updated.getBrand());
        vehicle.setModel(updated.getModel());
        vehicle.setYear(updated.getYear());
        vehicle.setKm(updated.getKm());
        vehicle.setPrice(updated.getPrice());
        return repository.save(vehicle);
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
}