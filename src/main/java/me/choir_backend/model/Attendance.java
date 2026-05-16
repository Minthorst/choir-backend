package me.choir_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    private LocalDateTime checkinTime;

    public Attendance() {}

    public Attendance(Member member, Session session) {
        this.member = member;
        this.session = session;
        this.checkinTime = LocalDateTime.now();
        this.status = AttendanceStatus.PRESENT;
    }

    public Attendance(Member member, Session session, AttendanceStatus status) {
        this.member = member;
        this.session = session;
        this.checkinTime = LocalDateTime.now();
        this.status = status;
    }


    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public AttendanceStatus getStatus() {
        return status;
    }
}