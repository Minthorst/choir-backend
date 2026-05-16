package me.choir_backend.Boundary;

import me.choir_backend.model.SessionType;

import java.time.LocalDateTime;

public record GetSessionResponse(
        Long id,
        LocalDateTime startTime,
        Boolean isOpen,
        SessionType sessionType,
        int amountOfAttendees
) {
}
