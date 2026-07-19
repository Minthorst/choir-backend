package me.choir_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "members")
public class Member {

    final static Integer MINIMUM_TICKET_COUNT = -3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int regularTickets = 0;
    private int commitTickets = 0;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean archived = false;

    @Column(unique = true, nullable = false)
    private String name;

    public String getSecretKey() {
        return secretKey;
    }

    @Column(unique = true, nullable = false)
    private String secretKey;

    protected Member() {
    }

    public Member(String name, String secretKey, Integer regularTickets, Integer commitTickets) {
        this.name = name;
        this.secretKey = secretKey;
        this.regularTickets = regularTickets;
        this.commitTickets = commitTickets;
    }

    public boolean hasEnoughTicketsForCheckin(){
        return  (this.commitTickets + this.regularTickets > MINIMUM_TICKET_COUNT);
    }

    public Integer getRegularTickets() {
        return regularTickets;
    }

    public Integer getCommitTickets() {
        return commitTickets;
    }

    public String getName() {
        return name;
    }

    public void setRegularTickets(int regularTickets) {
        this.regularTickets = regularTickets;
    }

    public void setCommitTickets(int commitTickets) {
        this.commitTickets = commitTickets;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }
}
