package me.choir_backend.Boundary;

public record AddTicketsRequest(Long memberId, int commitTickets, int regularTickets) {
}
