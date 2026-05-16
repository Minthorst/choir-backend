package me.choir_backend.repository;

import jakarta.persistence.LockModeType;
import me.choir_backend.Boundary.GetSessionResponse;
import me.choir_backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.isOpen = true ORDER BY s.startTime DESC")
    Optional<Session> findFirstByIsOpenWithLock();

    @Query("SELECT new me.choir_backend.Boundary.GetSessionResponse(" +
            "s.id, s.startTime, s.isOpen, s.sessionType, CAST(COUNT(a) AS int)) " +
            "FROM Session s " +
            "LEFT JOIN Attendance a ON a.session = s " +
            "GROUP BY s.id, s.startTime, s.isOpen, s.sessionType "+
            "ORDER BY s.startTime DESC")
    List<GetSessionResponse> findAllSessionsWithAttendeeCount();

    @Query("SELECT s FROM Session s WHERE s.isOpen = true OR s.sessionType = SessionType.AUTO_CLOSE")
    List<Session> getForgottenAndUnfinalizedSessions();
}
