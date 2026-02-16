package me.choir_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private Boolean isOpen = true;
    private Boolean wasAutoClosed = false;

    @Enumerated(EnumType.STRING)
    private SessionType sessionType = SessionType.NONE;

    // Standard getters and setters
}

