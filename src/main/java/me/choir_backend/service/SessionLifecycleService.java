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
import me.choir_backend.model.TicketTransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleService.class);

    private final MemberService memberService;
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final TicketLogService ticketLogService;

    public SessionLifecycleService(MemberService memberService, SessionService sessionService, AttendanceService attendanceService, TicketLogService ticketLogService) {
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
        this.ticketLogService = ticketLogService;
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
        TicketDelta delta = memberService.reduceMembersTicket(member);
        memberService.saveMember(member);
        ticketLogService.record(member, delta, TicketTransactionType.CHECK_IN, activeSession);
        attendanceService.saveAttendance(member, activeSession);
        log.info("Checked in member '{}' to session {} ({} regular / {} commit tickets left)",
                member.getName(), activeSession.getId(), member.getRegularTickets(), member.getCommitTickets());
    }


    @Transactional
    public EndSessionResponse finalizeSession(EndSessionRequest endSessionRequest) {
        Session sessionToFinalize = sessionService.findMandatorySession(endSessionRequest.sessionId());
        SessionType currentSessionType = sessionToFinalize.getSessionType();
        SessionType requestedSessionType = endSessionRequest.sessionType();
        if (currentSessionType == SessionType.COMMIT || currentSessionType == SessionType.REGULAR_ONLY
                || currentSessionType == SessionType.FREE) {
            throw new WrongSessionTypeException(String.format("Invalid current Session Type for ending session %s!", currentSessionType));
        }
            EndSessionResponse response = switch (requestedSessionType) {
                case AUTO_CLOSE, NONE ->
                        throw new WrongSessionTypeException(String.format("Invalid requested Session Type for ending session %s!", requestedSessionType));
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
                    for (Member absentee : absentMembersWithCommitTickets) {
                        TicketDelta delta = memberService.reduceMembersTicket(absentee);
                        ticketLogService.record(absentee, delta, TicketTransactionType.NO_SHOW_CHARGE, sessionToFinalize);
                    }
                    memberService.saveMembers(absentMembersWithCommitTickets);
                    yield new EndSessionResponse(attendedMemberList.size(), absentMembersWithCommitTickets.size());
                }
                case FREE -> {
                    sessionToFinalize.setOpen(false);
                    sessionToFinalize.setSessionType(SessionType.FREE);
                    sessionService.saveSession(sessionToFinalize);
                    List<Member> attendedMemberList = attendanceService.findMembersBySession(sessionToFinalize.getId());
                    for (Member attendee : attendedMemberList) {
                        TicketDelta delta = memberService.refundRegularTicket(attendee);
                        ticketLogService.record(attendee, delta, TicketTransactionType.FREE_SESSION_REFUND, sessionToFinalize);
                    }
                    memberService.saveMembers(attendedMemberList);
                    yield new EndSessionResponse(attendedMemberList.size(), 0);
                }
            };
            log.info("Finalized session {} as {}: {} attendees, {} absent commit members charged",
                    sessionToFinalize.getId(), requestedSessionType, response.presentMembers(), response.absentCommitMembers());
            return response;
        }
    }
