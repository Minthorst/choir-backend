package me.choir_backend.model;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;


@Entity
@Table(name = "sessions")
public class Session {
    private static final Duration MAX_SESSION_AGE = Duration.ofHours(12);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private Boolean isOpen;

    @Enumerated(EnumType.STRING)
    private SessionType sessionType = SessionType.NONE;

    public Session() {
        this.id = null;
        this.startTime = LocalDateTime.now();
        this.isOpen = true;
    }

    public Boolean isExpired(){
        return LocalDateTime.now().isAfter(this.getStartTime().plus(MAX_SESSION_AGE));
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Boolean getOpen() {
        return isOpen;
    }

    public void setOpen(Boolean open) {
        isOpen = open;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }
}

