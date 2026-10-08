package rszabadi.swiss;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @Min(1950) int year,
        @PositiveOrZero int km,
        @Positive int price,
        VehicleStatus status) {
}