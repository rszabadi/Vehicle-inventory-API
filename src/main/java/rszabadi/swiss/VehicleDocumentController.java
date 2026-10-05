package rszabadi.swiss;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Documents")
@RestController
@RequestMapping("/vehicles/{vehicleId}/documents")
public class VehicleDocumentController {

    private final VehicleRepository vehicles;
    private final VehicleDocumentRepository documents;

    public VehicleDocumentController(VehicleRepository vehicles, VehicleDocumentRepository documents) {
        this.vehicles = vehicles;
        this.documents = documents;
    }

    @Operation(summary = "List a vehicle's documents")
    @GetMapping
    public List<DocumentResponse> list(@PathVariable Long vehicleId) {
        requireVehicle(vehicleId);
        return documents.findByVehicleId(vehicleId).stream()
                .map(d -> DocumentResponse.from(vehicleId, d))
                .toList();
    }

    @Operation(summary = "Add a document to a vehicle")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@PathVariable Long vehicleId,
            @Valid @RequestBody DocumentCreateRequest request) {
        Vehicle vehicle = requireVehicle(vehicleId);
        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        document.setType(request.type());
        document.setExpiresOn(request.expiresOn());
        return DocumentResponse.from(vehicleId, documents.save(document));
    }

    @Operation(summary = "Update a document's status and expiry date")
    @PutMapping("/{documentId}")
    public DocumentResponse update(@PathVariable Long vehicleId, @PathVariable Long documentId,
            @Valid @RequestBody DocumentUpdateRequest request) {
        VehicleDocument document = requireDocument(vehicleId, documentId);
        document.setStatus(request.status());
        document.setExpiresOn(request.expiresOn());
        return DocumentResponse.from(vehicleId, documents.save(document));
    }

    @Operation(summary = "Delete a document")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long vehicleId, @PathVariable Long documentId) {
        documents.delete(requireDocument(vehicleId, documentId));
    }

    private Vehicle requireVehicle(Long vehicleId) {
        return vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    private VehicleDocument requireDocument(Long vehicleId, Long documentId) {
        return documents.findByIdAndVehicleId(documentId, vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }
}