package me.choir_backend.service;

import me.choir_backend.model.Attendance;
import me.choir_backend.model.AttendanceStatus;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }


    public boolean isAlreadyAttending(Member member, Session session) {
        return attendanceRepository.existsAttendanceByMemberAndSession(member, session);
    }

    public void saveAttendance(Member member, Session session) {
        attendanceRepository.save(new Attendance(member, session));
    }

    public List<Member> findMembersBySession(Long id) {
       return attendanceRepository.findMembersBySessionId(id);
    }

    public void saveNoShowAttendance(List<Member> members, Session session) {
        List<Attendance> noShowAttendance = members.stream().map(noShowMember -> new Attendance(noShowMember, session, AttendanceStatus.NO_SHOW)).toList();
        attendanceRepository.saveAll(noShowAttendance);
    }

    public List<Attendance> findAttendances(Member member) {
        return attendanceRepository.findAllByMember(member);
    }
}
