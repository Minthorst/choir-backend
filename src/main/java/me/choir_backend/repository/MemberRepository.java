package me.choir_backend.repository;

import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySecretKey(String secretKey);

    @Query("SELECT m FROM Member m WHERE m.commitTickets > 0 AND m NOT IN " +
            "(SELECT a.member FROM Attendance a WHERE a.session = :session)")
    List<Member> findAbsenteesWithCommitTicketsBySession(@Param("session") Session session);
}
