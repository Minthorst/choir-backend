package me.choir_backend.service;

import jakarta.transaction.Transactional;
import me.choir_backend.Boundary.DoormanCheckInResponse;
import me.choir_backend.Boundary.EndSessionRequest;
import me.choir_backend.Boundary.EndSessionResponse;
import me.choir_backend.Exception.InsufficientTicketsException;
import me.choir_backend.Exception.MemberAlreadyCheckedInException;
import me.choir_backend.Exception.WrongSessionTypeException;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.model.SessionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionLifecycleService {
    private final MemberService memberService;
    private final SessionService sessionService;
    private final AttendanceService attendanceService;

    public SessionLifecycleService(MemberService memberService, SessionService sessionService, AttendanceService attendanceService) {
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
    }

    @Transactional
    public void checkInMember(String secretKey) {
        checkInMember(memberService.getMandatoryMember(secretKey));
    }

    @Transactional
    public DoormanCheckInResponse checkInMemberById(Long memberId) {
        Member member = memberService.getMandatoryMemberById(memberId);
        boolean alreadyCheckedIn = false;
        try {
            checkInMember(member);
        } catch (MemberAlreadyCheckedInException e) {
            alreadyCheckedIn = true;
        }
        return new DoormanCheckInResponse(member.getName(), member.getRegularTickets(), member.getCommitTickets(), alreadyCheckedIn);
    }

    private void checkInMember(Member member) {
        sessionService.closeStaleSessions();
        Session activeSession = sessionService.getActiveSessionForCheckIn();
        if (attendanceService.isAlreadyAttending(member, activeSession))
            throw new MemberAlreadyCheckedInException(activeSession);
        if (!member.hasEnoughTicketsForCheckin())
            throw new InsufficientTicketsException();
        memberService.reduceMembersTicket(member);
        memberService.saveMember(member);
        attendanceService.saveAttendance(member, activeSession);
    }


    @Transactional
    public EndSessionResponse finalizeSession(EndSessionRequest endSessionRequest) {
        Session sessionToFinalize = sessionService.findMandatorySession(endSessionRequest.sessionId());
        SessionType requestedSessionType = endSessionRequest.sessionType();
        return switch (requestedSessionType) {
            case AUTO_CLOSE, NONE ->
                    throw new WrongSessionTypeException(String.format("Invalid Session Type for ending session %s!", requestedSessionType));
            case REGULAR_ONLY -> {
                sessionToFinalize.setOpen(false);
                sessionToFinalize.setSessionType(SessionType.REGULAR_ONLY);
                sessionService.saveSession(sessionToFinalize);
                List<Member> attendedMemberList = attendanceService.findMembersBySession(sessionToFinalize.getId());
                yield new EndSessionResponse(attendedMemberList.size(), 0);
            }
            case COMMIT -> {
                sessionToFinalize.setOpen(false);
                sessionToFinalize.setSessionType(SessionType.COMMIT);
                sessionService.saveSession(sessionToFinalize);
                List<Member> attendedMemberList = attendanceService.findMembersBySession(sessionToFinalize.getId());
                List<Member> absentMembersWithCommitTickets = memberService.findAbsenteesWithCommitTickets(sessionToFinalize);
                attendanceService.saveNoShowAttendance(absentMembersWithCommitTickets, sessionToFinalize);
                memberService.reduceMembersTicketsAndSaveMembers(absentMembersWithCommitTickets);
                yield new EndSessionResponse(attendedMemberList.size(), absentMembersWithCommitTickets.size());
            }
        };
    }
}
