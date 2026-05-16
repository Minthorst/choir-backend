package me.choir_backend.Boundary;


import me.choir_backend.model.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceDTO(LocalDateTime attendedOn, AttendanceStatus status) {
}
