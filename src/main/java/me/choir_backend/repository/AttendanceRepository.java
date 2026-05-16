package me.choir_backend.repository;

import me.choir_backend.model.Attendance;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllByMember(Member member);
    
    boolean existsAttendanceByMemberAndSession(Member member, Session currentSession);

    @Query("SELECT a.member FROM Attendance a WHERE a.session.id = :sessionId")
    List<Member> findMembersBySessionId(@Param("sessionId") Long sessionId);
}
