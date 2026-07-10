package me.choir_backend.service;

import jakarta.transaction.Transactional;
import me.choir_backend.Boundary.*;
import me.choir_backend.Exception.ResourceNotFoundException;
import me.choir_backend.model.Attendance;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    final MemberRepository memberRepository;


    final MemberKeyGeneratorService memberKeyGeneratorService;
    private final AttendanceService attendanceService;
    private final SessionService sessionService;


    public MemberService(MemberRepository memberRepository, MemberKeyGeneratorService memberKeyGeneratorService, AttendanceService attendanceService, SessionService sessionService) {
        this.memberRepository = memberRepository;
        this.memberKeyGeneratorService = memberKeyGeneratorService;
        this.attendanceService = attendanceService;
        this.sessionService = sessionService;
    }

    public Member getMandatoryMember(String secretKey) {
        return memberRepository.findBySecretKey(secretKey)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("No member with given secret key %s found", secretKey)));
    }

    public Member getMandatoryMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("No member with given ID %s found", id)));
    }

    public List<GetMemberNameResponse> getAllMemberNames() {
        return memberRepository.findAll().stream()
                .map(member -> new GetMemberNameResponse(member.getId(), member.getName()))
                .toList();
    }

    public GetMemberInfoResponse getMemberInfo(String secretKey) {
        Member member = getMandatoryMember(secretKey);
        List<Attendance> pastAttendances = attendanceService.findAttendances(member);
        Session activeSession = sessionService.getActiveSession();
        boolean checkedIn = activeSession != null && pastAttendances.stream()
                .anyMatch(a -> a.getSession().getId().equals(activeSession.getId()));
        return new GetMemberInfoResponse(
                member.getName(),
                member.getRegularTickets(),
                member.getCommitTickets(),
                toAttendanceDTOList(pastAttendances),
                checkedIn
        );
    }

    //TODO move to mapper
    private List<AttendanceDTO> toAttendanceDTOList(List<Attendance> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) return null;
        return attendanceList.stream().map(
                attendance -> new AttendanceDTO(attendance.getCheckinTime(), attendance.getStatus())).toList();
    }

    @Transactional
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

    public void reduceMembersTicket(Member member) {
        int availableCommitTickets = member.getCommitTickets();
        int availableRegularTickets = member.getRegularTickets();
        if (availableCommitTickets > 0) {
            member.setCommitTickets(availableCommitTickets - 1);
        } else {
            member.setRegularTickets(availableRegularTickets - 1);
        }
    }

    public List<GetAdminMemberInfoResponse> getAllMembersWithSecret() {
        List<Member> allMembers = memberRepository.findAll();
        Session activeSession = sessionService.getActiveSession();
        return allMembers.stream().map(m -> toAdminMemberInfoResponse(m, activeSession)).toList();
    }

    private GetAdminMemberInfoResponse toAdminMemberInfoResponse(Member member, Session activeSession) {
        boolean checkedIn = activeSession != null && attendanceService.isAlreadyAttending(member, activeSession);
        return new GetAdminMemberInfoResponse(member.getId(), member.getName(), member.getRegularTickets(), member.getCommitTickets(), member.getSecretKey(), checkedIn);
    }

    @Transactional
    public void addTickets(AddTicketsRequest addTicketsRequest) {
        Member member = memberRepository.findById(addTicketsRequest.memberId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Member with ID: %s not found", addTicketsRequest.memberId())));
        member.setRegularTickets(member.getRegularTickets() + addTicketsRequest.regularTickets());
        member.setCommitTickets(member.getCommitTickets() + addTicketsRequest.commitTickets());
        memberRepository.save(member);
    }

    public List<GetAdminMemberInfoResponse> getAllMembersOfSession(Long sessionId) {
        List<Member> attendingMembers = attendanceService.findMembersBySession(sessionId);
        Session activeSession = sessionService.getActiveSession();
        return attendingMembers.stream().map(m -> toAdminMemberInfoResponse(m, activeSession)).toList();
    }

    public void saveMembers(List<Member> members) {
        memberRepository.saveAll(members);
    }

    public List<Member> findAbsenteesWithCommitTickets(Session sessionToFinalize) {
        return memberRepository.findAbsenteesWithCommitTicketsBySession(sessionToFinalize);
    }


    public void reduceMembersTicketsAndSaveMembers(List<Member> members) {
        members.forEach(this::reduceMembersTicket);
        saveMembers(members);
    }

    public void saveMember(Member member) {
        memberRepository.save(member);
    }
}
