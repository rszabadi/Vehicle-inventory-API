package rszabadi.swiss;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class VehicleValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private VehicleRequest request(String brand, String model, int year, int km, int price) {
        return new VehicleRequest(brand, model, year, km, price, null);
    }

    private Set<String> invalidFields(VehicleRequest r) {
        return validator.validate(r).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void validVehicleHasNoViolations() {
        assertThat(invalidFields(request("Dacia", "Sandero", 2020, 41000, 11500))).isEmpty();
    }

    @Test
    void blankBrandIsRejected() {
        assertThat(invalidFields(request("", "Sandero", 2020, 41000, 11500)))
                .containsExactly("brand");
    }

    @Test
    void negativeKmIsRejected() {
        assertThat(invalidFields(request("Dacia", "Sandero", 2020, -5, 11500)))
                .containsExactly("km");
    }

    @Test
    void zeroPriceIsRejected() {
        assertThat(invalidFields(request("Dacia", "Sandero", 2020, 41000, 0)))
                .containsExactly("price");
    }
    
    
}