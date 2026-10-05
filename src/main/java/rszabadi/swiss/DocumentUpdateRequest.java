package rszabadi.swiss;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DocumentUpdateRequest(
        @NotNull DocumentStatus status,
        LocalDate expiresOn) {
}