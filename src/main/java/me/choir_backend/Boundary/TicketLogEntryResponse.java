package me.choir_backend.Boundary;

import me.choir_backend.model.TicketTransactionType;

import java.time.LocalDateTime;

public record TicketLogEntryResponse(
        LocalDateTime timestamp,
        int regularDelta,
        int commitDelta,
        TicketTransactionType type) {
}
