package me.choir_backend.Boundary;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String message,
        String page,
        String memberKey,
        String userAgent,
        String viewport,
        String clientTimestamp) {
}
