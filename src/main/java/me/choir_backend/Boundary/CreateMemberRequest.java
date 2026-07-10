package me.choir_backend.Boundary;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank String name,
        Integer regularTickets,
        Integer commitTickets) {}
