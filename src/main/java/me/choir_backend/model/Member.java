package me.choir_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer regularTickets = 0;
    private Integer commitTickets = 0;
    private String name;

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

    public Integer getRegularTickets() {
        return regularTickets;
    }

    public Integer getCommitTickets() {
        return commitTickets;
    }

    public String getName() {
        return name;
    }
}
