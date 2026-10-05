package rszabadi.swiss;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DocumentCreateRequest(
        @NotNull DocumentType type,
        LocalDate expiresOn) {
}