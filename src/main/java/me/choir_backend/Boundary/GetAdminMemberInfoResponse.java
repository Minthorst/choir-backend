package me.choir_backend.Boundary;

public record GetAdminMemberInfoResponse(Long id, String name, int regularTickets, int commitTickets, String secretKey, boolean checkedIn, boolean archived) {
}
