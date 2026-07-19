package me.choir_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_transactions")
public class TicketTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private int regularDelta;
    private int commitDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketTransactionType type;

    @ManyToOne
    private Session session;

    protected TicketTransaction() {
    }

    public TicketTransaction(Member member, int regularDelta, int commitDelta,
                             TicketTransactionType type, Session session) {
        this.member = member;
        this.timestamp = LocalDateTime.now();
        this.regularDelta = regularDelta;
        this.commitDelta = commitDelta;
        this.type = type;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getRegularDelta() {
        return regularDelta;
    }

    public int getCommitDelta() {
        return commitDelta;
    }

    public TicketTransactionType getType() {
        return type;
    }

    public Session getSession() {
        return session;
    }
}
