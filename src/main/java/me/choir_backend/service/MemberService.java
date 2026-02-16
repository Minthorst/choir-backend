package me.choir_backend.service;

import me.choir_backend.Boundary.AttendanceDTO;
import me.choir_backend.Boundary.CreateMemberRequest;
import me.choir_backend.Boundary.CreateMemberResponse;
import me.choir_backend.Boundary.GetMemberInfoResponse;
import me.choir_backend.model.Attendance;
import me.choir_backend.model.Member;
import me.choir_backend.repository.AttendanceRepository;
import me.choir_backend.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    MemberKeyGeneratorService memberKeyGeneratorService;

    public GetMemberInfoResponse getMemberInfo(String secretKey) {
        Optional<Member> memberOptional = memberRepository.findBySecretKey(secretKey);
        //TODO throw custom exception
        if (memberOptional.isEmpty()) throw new RuntimeException();
        Member member = memberOptional.get();
        List<Attendance> lastFiveAttendance = attendanceRepository.findTop5ByMemberOrderByCheckinTimeDesc(member);
        return new GetMemberInfoResponse(
                member.getName(),
                member.getRegularTickets(),
                member.getCommitTickets(),
                toAttendanceDTOList(lastFiveAttendance)
                );
    }

    private List<AttendanceDTO> toAttendanceDTOList(List<Attendance> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) return null;
        return attendanceList.stream().map(
                attendance -> new AttendanceDTO(attendance.getCheckinTime())).toList();
    }

    public CreateMemberResponse createMember(CreateMemberRequest createMemberRequest) {
        String memberKey = memberKeyGeneratorService.generateUnique();

        Member member = new Member(
                createMemberRequest.name(),
                memberKey,
                createMemberRequest.regularTickets(),
                createMemberRequest.commitTickets());

        memberRepository.save(member);
        return new CreateMemberResponse(memberKey);
    }

}
