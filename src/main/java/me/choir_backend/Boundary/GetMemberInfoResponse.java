package me.choir_backend.Boundary;

import java.util.List;

public record GetMemberInfoResponse(String name, int regularTickets, int commitTickets, List<AttendanceDTO> lastAttendances){}
