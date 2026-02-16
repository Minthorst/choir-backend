package me.choir_backend.repository;

import me.choir_backend.model.Attendance;
import me.choir_backend.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findTop5ByMemberOrderByCheckinTimeDesc(Member member);
}
