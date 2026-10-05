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

    private Vehicle validVehicle() {
        Vehicle v = new Vehicle();
        v.setBrand("Dacia");
        v.setModel("Sandero");
        v.setYear(2020);
        v.setKm(41000);
        v.setPrice(11500);
        return v;
    }

    private Set<String> invalidFields(Vehicle v) {
        return validator.validate(v).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void validVehicleHasNoViolations() {
        assertThat(invalidFields(validVehicle())).isEmpty();
    }

    @Test
    void blankBrandIsRejected() {
        Vehicle v = validVehicle();
        v.setBrand("");

        assertThat(invalidFields(v)).containsExactly("brand");
    }

    @Test
    void negativeKmIsRejected() {
        Vehicle v = validVehicle();
        v.setKm(-5);

        assertThat(invalidFields(v)).containsExactly("km");
    }

    @Test
    void zeroPriceIsRejected() {
        Vehicle v = validVehicle();
        v.setPrice(0);

        assertThat(invalidFields(v)).containsExactly("price");
    }
}