package me.choir_backend.Boundary;

import jakarta.validation.constraints.NotNull;
import me.choir_backend.model.SessionType;

public record EndSessionRequest(
        @NotNull Long sessionId,
        @NotNull SessionType sessionType) {
}
