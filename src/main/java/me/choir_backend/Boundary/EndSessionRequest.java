package me.choir_backend.Boundary;

import me.choir_backend.model.SessionType;

public record EndSessionRequest(Long sessionId, SessionType sessionType) {
}
